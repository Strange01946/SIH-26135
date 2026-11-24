"""Refresh the bundled India geography from archived Ministry of Panchayati Raj LGD tables."""

from hashlib import sha256
from io import BytesIO
import json
from pathlib import Path
import re
from urllib.request import urlopen

import pyarrow.parquet as parquet


CATALOG = 'https://ramseraph.github.io/opendata/lgd/data/metadata.json'
# Numeric LGD identity mapped to the application codes used by the India state map.
STATE_CODES = {
    '1': 'JK', '2': 'HP', '3': 'PB', '4': 'CH', '5': 'UT', '6': 'HR', '7': 'DL',
    '8': 'RJ', '9': 'UP', '10': 'BR', '11': 'SK', '12': 'AR', '13': 'NL', '14': 'MN',
    '15': 'MZ', '16': 'TR', '17': 'ML', '18': 'AS', '19': 'WB', '20': 'JH',
    '21': 'OR', '22': 'CT', '23': 'MP', '24': 'GJ', '27': 'MH', '28': 'AP',
    '29': 'KA', '30': 'GA', '31': 'LD', '32': 'KL', '33': 'TN', '34': 'PY',
    '35': 'AN', '36': 'TG', '37': 'LA', '38': 'DH',
}


def download(url):
    with urlopen(url, timeout=30) as response:
        return response.read()


def read_report(index, report):
    edition = index['datasets'][report]['daily']
    source = edition['file']
    body = download(source['url'])
    if sha256(body).hexdigest() != source['sha256']:
        raise ValueError(f'{report} checksum differs from the published catalogue')
    rows = parquet.read_table(BytesIO(body)).to_pylist()
    if len(rows) != edition['rows']:
        raise ValueError(f'{report} row count differs from the published catalogue')
    return rows, {'observedOn': edition['observed_on'], 'url': source['url'], 'sha256': source['sha256']}


def display_name(name):
    return re.sub(r'\bAnd\b', 'and', ' '.join(name.split()))


def build_snapshot():
    index = json.loads(download(CATALOG))
    state_rows, states_source = read_report(index, 'states')
    district_rows, districts_source = read_report(index, 'districts')
    if states_source['observedOn'] != districts_source['observedOn']:
        raise ValueError('Choose matching LGD editions for states and districts')
    if {row['state_code'] for row in state_rows} != STATE_CODES.keys() or len(state_rows) != len(STATE_CODES):
        raise ValueError('LGD state codes changed. Review the application state-code mapping.')
    states = [dict(lgdCode=row['state_code'], code=STATE_CODES[row['state_code']],
                   name=display_name(row['state_name']), kind='STATE' if row['state_or_ut'] == 'S' else 'UT')
              for row in sorted(state_rows, key=lambda row: int(row['state_code']))]
    districts = [dict(lgdCode=row['district_code'], stateLgdCode=row['state_code'], name=display_name(row['district_name']))
                 for row in sorted(district_rows, key=lambda row: (int(row['state_code']), int(row['district_code'])))]
    if {row['stateLgdCode'] for row in districts} != STATE_CODES.keys():
        raise ValueError('Districts must cover every listed state or union territory')
    if len({row['lgdCode'] for row in districts}) != len(districts):
        raise ValueError('District LGD codes must be unique')
    source = dict(authority='Ministry of Panchayati Raj, Government of India',
                  directory='https://lgdirectory.gov.in/', license='Government Open Data License - India',
                  catalog=CATALOG, states=states_source, districts=districts_source)
    encode = lambda value: json.dumps(value, ensure_ascii=False)
    output = '{\n  "source": ' + encode(source)
    for label, rows in [('states', states), ('districts', districts)]:
        output += ',\n  ' + encode(label) + ': [\n    ' + ',\n    '.join(map(encode, rows)) + '\n  ]'
    Path(__file__).with_name('india-lgd.json').write_text(output + '\n}\n', encoding='utf-8', newline='\n')
    print(f"LGD {districts_source['observedOn']}: {len(states)} states and union territories, {len(districts)} districts")


if __name__ == '__main__':
    build_snapshot()
