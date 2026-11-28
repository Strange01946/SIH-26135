import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getStateOutcomes, type DistrictSummary, type MisFilters, type MisOptions, type MisOverview } from '@/api/misApi';
import { Modal } from '@/components/common/Modal';
import { ErrorState } from '@/components/feedback/ErrorState';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { formatMetric, MeasureValue, MisTable, Panel, Value, type TableColumn } from './MisUi';
import india from './indiaStates.json';
import { ExecutiveKpiCards } from './ExecutiveKpiCards';

export function ExecutiveKpis({ headline }: Pick<MisOverview, 'headline'>) {
  return (
    <Panel code="Q01" title="Executive indicators">
      <ExecutiveKpiCards headline={headline} />
      <p className="text-sm text-slate-600">Financial years group candidates by enrollment date. Results include subsequent outcomes.
        {!headline.previousFinancialYear && ' Select a financial year to compare it with the previous year.'}</p>
    </Panel>
  );
}

const stages: Record<string, { label: string; detail: string }> = {
  registered: { label: 'Registered', detail: 'Includes candidates registered in the period and candidates with matching enrollments who registered earlier.' },
  enrolled: { label: 'Enrolled', detail: 'Distinct candidates with an enrollment in the selected cohort.' },
  assessed: { label: 'Assessed', detail: 'Candidates who appeared for a held assessment or have an issued certificate linked to a qualifying enrollment.' },
  certified: { label: 'Certified', detail: 'Distinct candidates with an issued certificate linked to a qualifying enrollment. A pass can still be awaiting a certificate.' },
  placed: { label: 'Placed', detail: 'Candidates with a joined placement or a self-reported post-training wage job linked to a matching enrollment.' },
  retained: { label: 'Retained', detail: 'Placed candidates with a wage job linked to the same enrollment that began on or after training completion and lasted at least six months.' },
};

export function TrainingFunnel({ pipeline }: Pick<MisOverview, 'pipeline'>) {
  const [selected, setSelected] = useState('enrolled');
  const maximum = Math.max(1, ...pipeline.filter((stage) => stage.candidates.availability === 'RELEASED')
    .map((stage) => stage.candidates.value ?? 0));
  return (
    <Panel code="Q02" title="Training pipeline">
      <p className="text-sm text-slate-600">Each stage counts distinct candidates. The enrollment and placement cards count enrollments. Select a stage for its definition.</p>
      <ol className="grid gap-2" aria-label="Candidate lifecycle stages">
        {pipeline.map(({ key, candidates }, index) => (
          <li key={key}>
            <button type="button" aria-pressed={key === selected} onClick={() => setSelected(key)}
              className="grid w-full gap-2 rounded-md border border-slate-200 bg-white p-2.5 text-left text-sm hover:bg-slate-50 aria-pressed:border-navy aria-pressed:bg-navy-50 sm:grid-cols-[7.5rem_minmax(0,1fr)_minmax(6rem,auto)] sm:items-center">
              <span className="font-semibold">{stages[key]?.label ?? key}</span>
              <span className="flex h-4 justify-center" aria-hidden="true">
                {candidates.availability === 'RELEASED' && candidates.value != null && (
                  <span className="rounded-sm bg-cyan-800" style={{ width: `${candidates.value / maximum * 100}%`, opacity: 1 - 0.09 * index }} />
                )}
              </span>
              <span className="text-right tabular-nums"><MeasureValue measure={candidates} focusable={false} /></span>
            </button>
          </li>
        ))}
      </ol>
      <p className="min-h-10 rounded-md bg-slate-100 p-3 text-sm text-slate-700" aria-live="polite">{stages[selected]?.detail}</p>
    </Panel>
  );
}

const codeAliases: Record<string, string> = { CG: 'CT', OD: 'OR', TS: 'TG', UK: 'UT', DN: 'DH', DD: 'DH' };
const canonicalCode = (code = '') => codeAliases[code.toUpperCase()] ?? code.toUpperCase();
const metrics = { placementRatePct: 'Placement rate', employmentRatePct: 'Employment rate', certificationRatePct: 'Certification rate' } as const;
export type MapMetric = keyof typeof metrics;
const metricDefinitions: Record<MapMetric, string> = {
  placementRatePct: 'Share of enrollments with a joined placement or a self-reported post-training wage job.',
  employmentRatePct: 'Enrollments with a linked employment record as a share of matching enrollments.',
  certificationRatePct: 'Distinct trainees with an issued certificate as a share of distinct trainees with a matching enrollment.',
};
const scale = ['#e0f2fe', '#bae6fd', '#7dd3fc', '#38bdf8', '#0369a1'];

export function GeographicPanel({ geography, filters, options, metric, onMetricChange }: {
  geography: MisOverview['geography']; filters: MisFilters; options: MisOptions;
  metric: MapMetric; onMetricChange: (metric: MapMetric) => void;
}) {
  const [selectedId, setSelectedId] = useState<number>();
  const summaries = new Map((geography.stateSummaries ?? []).map((row) => [row.stateId, row]));
  const states = options.states.filter((state) => !filters.stateId || filters.stateId === state.id);
  const statesByCode = new Map(states.map((state) => [canonicalCode(state.code), state]));
  const selected = states.find((state) => state.id === selectedId);

  return (
    <Panel code="Q06" title="State and district outcomes">
      <p className="text-sm text-slate-600">{metricDefinitions[metric]} The map uses the trainee’s recorded state. District results require a recorded district in that state.</p>
      <div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_minmax(16rem,0.8fr)]">
        <div className="space-y-3">
          <div className="max-w-60">
            <label className="form-label" htmlFor="mis-map-metric">Map metric</label>
            <select id="mis-map-metric" className="form-input" value={metric} onChange={(event) => onMetricChange(event.target.value as MapMetric)}>
              {Object.entries(metrics).map(([key, label]) => <option key={key} value={key}>{label}</option>)}
            </select>
          </div>
          <svg viewBox="0 0 452 480" aria-label={`India state choropleth of ${metrics[metric].toLowerCase()}`}
            className="mx-auto block w-full max-w-lg rounded-lg bg-slate-50 p-2">
            {india.map((shape) => {
              const state = statesByCode.get(shape.code);
              const row = state ? summaries.get(state.id) : undefined;
              const value = row?.[metric];
              const outside = Boolean(filters.stateId && !state);
              const title = `${state?.name ?? shape.name} · ${value != null ? formatMetric(value, 'percent')
                : outside ? 'Outside selected area' : 'No data'}`;
              const fill = value != null ? scale[Math.min(4, Math.max(0, Math.floor(value / 20)))] : '#e2e8f0';
              return (
                <path key={shape.code} d={shape.path} fill={fill} fillRule="evenodd"
                  opacity={outside ? 0.38 : undefined}
                  stroke={selected?.id === state?.id && selected ? '#0f172a' : '#f8fafc'} strokeWidth="1.3"
                  role={state ? 'button' : undefined} aria-label={title}
                  className={state ? 'cursor-pointer hover:stroke-navy focus:stroke-navy focus:outline-none' : undefined}
                  tabIndex={state ? 0 : undefined}
                  onClick={state ? () => setSelectedId(state.id) : undefined}
                  onKeyDown={state ? (event) => {
                    if (event.key === 'Enter' || event.key === ' ') { event.preventDefault(); setSelectedId(state.id); }
                  } : undefined}>
                  <title>{title}</title>
                </path>
              );
            })}
          </svg>
          <ul className="flex flex-wrap justify-center gap-2 text-xs text-slate-700" aria-label="Map shading legend">
            {[...scale, '#e2e8f0'].map((fill, index) => (
              <li key={fill} className="flex items-center gap-1">
                <span className="size-3 rounded-sm border border-slate-300" style={{ backgroundColor: fill }} aria-hidden="true" />
                {['0–<20%', '20–<40%', '40–<60%', '60–<80%', '80–100%', 'No data'][index]}
              </li>
            ))}
          </ul>
          <p className="text-xs text-slate-600">Boundaries adapted from DataMeet and Election Commission of India via geoBoundaries (<a href="https://creativecommons.org/licenses/by/2.5/in/" className="underline">CC BY 2.5 India</a>).</p>
        </div>
        <div className="min-w-0 space-y-2">
          <h3 className="font-semibold text-navy">Select a state to view districts</h3>
          {states.length === 0 ? <p className="text-sm text-slate-600">No states are available in the location directory.</p> : (
            <ul className="max-h-100 divide-y overflow-auto rounded-md border border-slate-200">
              {states.map((state) => {
                const row = summaries.get(state.id);
                return <li key={state.id} className="flex items-center justify-between gap-2 p-3 text-sm">
                  <button type="button" className="rounded text-left font-semibold text-navy hover:underline focus-visible:outline-2"
                    onClick={() => setSelectedId(state.id)}>{state.name}</button>
                  <Value value={row?.[metric]} format="percent" noData />
                </li>;
              })}
            </ul>
          )}
        </div>
      </div>
      {selected && <StateDialog stateId={selected.id} name={selected.name} filters={filters} onClose={() => setSelectedId(undefined)} />}
    </Panel>
  );
}

const districtColumns: TableColumn<DistrictSummary>[] = [
  { key: 'district', title: 'District', value: (row) => row.districtName, sortable: true },
  { key: 'enrolled', title: 'Enrollments', value: (row) => row.enrollmentCount, format: 'count', sortable: true },
  { key: 'certified', title: 'Certification rate', value: (row) => row.certificationRatePct, format: 'percent', sortable: true, noData: (row) => row.enrollmentCount === 0 },
  { key: 'placed', title: 'Placement rate', value: (row) => row.placementRatePct, format: 'percent', sortable: true, noData: (row) => row.enrollmentCount === 0 },
  { key: 'employment', title: 'Employment rate', value: (row) => row.employmentRatePct, format: 'percent', sortable: true, noData: (row) => row.enrollmentCount === 0 },
];

function StateDialog({ stateId, name, filters, onClose }: { stateId: number; name: string; filters: MisFilters; onClose: () => void }) {
  const districts = useQuery({
    queryKey: ['government-mis', 'state', stateId, filters],
    queryFn: ({ signal }) => getStateOutcomes(stateId, filters, signal),
    retry: false,
    refetchOnMount: 'always',
    refetchOnWindowFocus: 'always',
  });
  return (
    <Modal open onClose={onClose} title={`${name} district outcomes`} className="max-w-[52rem]">
      <div className="p-4 sm:p-5">
        {districts.isPending ? <LoadingSpinner label="Loading district outcomes" /> : districts.isError
          ? <ErrorState error={districts.error} title="Unable to load district outcomes" onRetry={() => void districts.refetch()} />
          : <MisTable title="District outcomes" rows={(districts.data.districts ?? [])
              .filter(({ districtId }) => filters.districtId == null || districtId === filters.districtId)}
              columns={districtColumns} rowKey={(row) => row.districtId} initialSort="district" missingAsNoData />}
      </div>
    </Modal>
  );
}
