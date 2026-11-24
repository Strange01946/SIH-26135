import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { existsSync, readFileSync } from 'node:fs';

const [, , flag, catalog, mode] = process.argv;
assert(flag === '--catalog' && ['SIH26135', 'SIH26135_TEST'].includes(catalog)
  && (mode === undefined || mode === '--verify') && process.argv.length <= 5,
'Use node database/install-india-geography.mjs --catalog SIH26135_TEST [--verify]');

const testCatalog = catalog === 'SIH26135_TEST';
const user = process.env[testCatalog ? 'SIH_TEST_DB_USERNAME' : 'DB_USERNAME'];
const password = process.env[testCatalog ? 'SIH_TEST_DB_PASSWORD' : 'DB_PASSWORD'];
assert(user && password, testCatalog ? 'Set SIH_TEST_DB_USERNAME and SIH_TEST_DB_PASSWORD.' : 'Set DB_USERNAME and DB_PASSWORD.');

const { source, states, districts } = JSON.parse(readFileSync(new URL('./reference/india-lgd.json', import.meta.url), 'utf8'));
const validCode = (code) => typeof code === 'string' && /^[1-9]\d{0,14}$/.test(code);
const validName = (name) => typeof name === 'string' && name.trim() === name && name.length <= 100 && name.length > 0;
const unique = (values) => new Set(values).size === values.length;
assert.equal(states.length, 36);
assert.equal(states.filter(({ kind }) => kind === 'STATE').length, 28);
assert.equal(states.filter(({ kind }) => kind === 'UT').length, 8);
assert(states.every(({ lgdCode, code, name }) => validCode(lgdCode) && /^[A-Z]{2}$/.test(code) && validName(name)));
assert(unique(states.map(({ lgdCode }) => lgdCode)) && unique(states.map(({ code }) => code)));
assert(districts.length > 700 && districts.every(({ lgdCode, stateLgdCode, name }) =>
  validCode(lgdCode) && states.some((state) => state.lgdCode === stateLgdCode) && validName(name)));
assert(unique(districts.map(({ lgdCode }) => lgdCode)));
assert(states.every(({ lgdCode }) => districts.some(({ stateLgdCode }) => stateLgdCode === lgdCode)));

const quote = (value) => "'" + value.replaceAll("'", "''") + "'";
const values = (rows) => rows.map((row) => '(' + row.map(quote).join(', ') + ')').join(',\n');
const legacyCode = { CT: 'CG', OR: 'OD', TG: 'TS', UT: 'UK' };
const mysql = process.env.SIH_E2E_MYSQL ?? [
  'C:/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe',
  'C:/Program Files/MySQL/MySQL Server 8.4/bin/mysql.exe',
].find(existsSync) ?? 'mysql';

const reconcile = `
  -- Resolve existing reference rows before inserting. Unique keys reject ambiguous matches.
  CREATE TEMPORARY TABLE matched_states(lgd_code VARCHAR(16) PRIMARY KEY, state_id BIGINT UNSIGNED NOT NULL UNIQUE);
  INSERT INTO matched_states
  SELECT i.lgd_code, s.state_id FROM incoming_states i JOIN states s
    ON s.lgd_code = i.lgd_code OR s.state_code IN (i.code, i.legacy_code) OR s.state_name = i.name;
  INSERT INTO lgd_checks SELECT 'state identity and lifecycle', NOT EXISTS (
    SELECT 1 FROM matched_states m JOIN states s USING (state_id)
    WHERE (s.lgd_code IS NOT NULL AND s.lgd_code <> m.lgd_code)
       OR s.lifecycle_status_id <> @active OR s.deleted_at IS NOT NULL);
  UPDATE states s JOIN matched_states m USING (state_id)
    SET s.lgd_code = m.lgd_code WHERE s.lgd_code IS NULL;
  SET @linked_states = ROW_COUNT();
  INSERT INTO states (state_code, state_name, lgd_code, lifecycle_status_id)
  SELECT i.code, i.name, i.lgd_code, @active FROM incoming_states i
  LEFT JOIN matched_states m USING (lgd_code) WHERE m.state_id IS NULL ORDER BY i.lgd_code;
  SET @new_states = ROW_COUNT();

  CREATE TEMPORARY TABLE matched_districts(
    lgd_code VARCHAR(16) PRIMARY KEY, district_id BIGINT UNSIGNED NOT NULL UNIQUE, state_id BIGINT UNSIGNED NOT NULL);
  INSERT INTO matched_districts
  SELECT i.lgd_code, d.district_id, s.state_id FROM incoming_districts i
  JOIN states s ON s.lgd_code = i.state_lgd_code
  JOIN districts d ON d.lgd_code = i.lgd_code
    OR (d.state_id = s.state_id AND (d.district_code = i.lgd_code OR d.district_name = i.name));
  INSERT INTO lgd_checks SELECT 'district identity, parent and lifecycle', NOT EXISTS (
    SELECT 1 FROM matched_districts m JOIN districts d USING (district_id)
    WHERE d.state_id <> m.state_id OR (d.lgd_code IS NOT NULL AND d.lgd_code <> m.lgd_code)
       OR d.lifecycle_status_id <> @active OR d.deleted_at IS NOT NULL);
  UPDATE districts d JOIN matched_districts m USING (district_id)
    SET d.lgd_code = m.lgd_code WHERE d.lgd_code IS NULL;
  SET @linked_districts = ROW_COUNT();
  INSERT INTO districts (state_id, district_code, district_name, lgd_code, lifecycle_status_id)
  SELECT s.state_id, i.lgd_code, i.name, i.lgd_code, @active FROM incoming_districts i
  JOIN states s ON s.lgd_code = i.state_lgd_code
  LEFT JOIN matched_districts m ON m.lgd_code = i.lgd_code
  WHERE m.district_id IS NULL ORDER BY i.lgd_code;
  SET @new_districts = ROW_COUNT();
`;

const statement = `
  SELECT IF(LOWER(DATABASE()) = LOWER(${quote(catalog)}), 1, JSON_EXTRACT('Wrong catalogue', '$'));
  SET SESSION sql_mode = CONCAT_WS(',', NULLIF(@@session.sql_mode, ''), 'NO_BACKSLASH_ESCAPES');
  CREATE TEMPORARY TABLE lgd_checks (
    requirement VARCHAR(100) PRIMARY KEY, passed BOOLEAN NOT NULL CHECK (passed = 1));
  CREATE TEMPORARY TABLE incoming_states (
    lgd_code VARCHAR(16) PRIMARY KEY, code CHAR(2) NOT NULL UNIQUE, legacy_code CHAR(2) NOT NULL, name VARCHAR(100) NOT NULL UNIQUE)
    DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
  CREATE TEMPORARY TABLE incoming_districts (
    lgd_code VARCHAR(16) PRIMARY KEY, state_lgd_code VARCHAR(16) NOT NULL, name VARCHAR(100) NOT NULL,
    UNIQUE (state_lgd_code, name)) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
  INSERT INTO incoming_states VALUES ${values(states.map(({ lgdCode, code, name }) => [lgdCode, code, legacyCode[code] ?? code, name]))};
  INSERT INTO incoming_districts VALUES ${values(districts.map(({ lgdCode, stateLgdCode, name }) => [lgdCode, stateLgdCode, name]))};
  INSERT INTO lgd_checks VALUES ('exclusive geography import', GET_LOCK(CONCAT(DATABASE(), ':india-lgd'), 15));
  START TRANSACTION;
  SET @active = (SELECT lifecycle_status_id FROM ref_lifecycle_status WHERE status_code = 'ACTIVE');
  INSERT INTO lgd_checks VALUES ('configured active reference status', @active IS NOT NULL);
  ${mode === '--verify' ? '' : reconcile}
  SET @state_total = (SELECT COUNT(*) FROM incoming_states i JOIN states s USING (lgd_code)
    WHERE s.lifecycle_status_id = @active AND s.deleted_at IS NULL);
  SET @district_total = (SELECT COUNT(*) FROM incoming_districts i JOIN districts d USING (lgd_code)
    JOIN states s ON s.state_id = d.state_id AND s.lgd_code = i.state_lgd_code
    WHERE d.lifecycle_status_id = @active AND d.deleted_at IS NULL);
  INSERT INTO lgd_checks VALUES ('all active LGD states', @state_total = ${states.length}),
    ('all active LGD districts and correct parents', @district_total = ${districts.length});
  COMMIT;
  SELECT @state_total, @district_total, COALESCE(@new_states, 0), COALESCE(@new_districts, 0),
    COALESCE(@linked_states, 0), COALESCE(@linked_districts, 0);
`;

const result = spawnSync(mysql, ['--no-defaults', '--protocol=TCP', '--host=127.0.0.1', '--port=3306',
  '--user=' + user, '--database=' + catalog, '--batch', '--raw', '--skip-column-names', '--default-character-set=utf8mb4',
  '--connect-timeout=5'], { env: { ...process.env, MYSQL_PWD: password }, input: statement, encoding: 'utf8', timeout: 45_000 });
if (result.error || result.status !== 0) {
  throw new Error('LGD operation failed. ' + (result.error?.message ?? result.stderr.trim()));
}
const [proof, totals] = result.stdout.trim().split(/\r?\n/);
assert.equal(proof, '1');
const [stateCount, districtCount, newStates, newDistricts, linkedStates, linkedDistricts] = totals.split('\t').map(Number);
assert.equal(stateCount, states.length);
assert.equal(districtCount, districts.length);
console.log(`${catalog} verified ${stateCount} states and union territories and ${districtCount} districts from LGD ${source.districts.observedOn}.`);
if (mode !== '--verify') console.log(`Inserted ${newStates} states and ${newDistricts} districts. Linked existing IDs to LGD for ${linkedStates} states and ${linkedDistricts} districts.`);
