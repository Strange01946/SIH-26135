import type { Bucket, DropoutSignal, ExitArea, IncidentDay, Schema, WageDistributions } from '@/api/specializedAnalyticsApi';
import { formatMetric, MisTable, Panel, Value, type TableColumn } from './MisUi';
import { MetricTiles } from './SpecializedQueryPanels';

interface Series {
  name: string;
  color: string;
  values: (number | null)[];
}

function Trend({ label, labels, series }: { label: string; labels: string[]; series: Series[] }) {
  if (!labels.length) return null;
  const maximum = Math.max(1, ...series.flatMap((line) => line.values.filter((value): value is number => value != null)));
  const x = (index: number) => 67 + (index / Math.max(1, labels.length - 1)) * 507;
  const y = (value: number) => 164 - value / maximum * 137;
  const ticks = Array.from(new Set([0, Math.ceil(maximum / 2), maximum])).filter((tick) => tick <= maximum);
  return (
    <figure className="rounded-lg border border-slate-200 bg-slate-50 p-3">
      <svg viewBox="0 0 630 199" className="mx-auto block max-h-64 w-full" role="img" aria-label={label}>
        {ticks.map((tick) => (
          <g key={tick}>
            <line x1="58" x2="586" y1={y(tick)} y2={y(tick)} stroke="#cbd5e1" />
            <text x="52" y={y(tick) + 4} textAnchor="end" fill="#334155" fontSize="12">{formatMetric(tick, 'count')}</text>
          </g>
        ))}
        {series.map((line) => (
          <g key={line.name} stroke={line.color} fill={line.color}>
            {line.values.map((value, index) => {
              if (value == null) return null;
              const prior = line.values[index - 1];
              return (
                <g key={labels[index]}>
                  {prior != null && <line x1={x(index - 1)} y1={y(prior)} x2={x(index)} y2={y(value)} strokeWidth="2.7" />}
                  <circle cx={x(index)} cy={y(value)} r={labels.length > 31 ? 2.2 : 3.8}>
                    <title>{labels[index]} · {line.name} · {formatMetric(value, 'count')}</title>
                  </circle>
                </g>
              );
            })}
          </g>
        ))}
        {labels.map((entry, index) => (labels.length <= 5 || index === 0 || index === labels.length - 1) && (
          <text key={entry} x={x(index)} y="187" fill="#334155" fontSize="11"
            textAnchor={index === 0 ? 'start' : index === labels.length - 1 ? 'end' : 'middle'}>{entry}</text>
        ))}
      </svg>
      <figcaption className="flex flex-wrap justify-center gap-4 text-xs font-semibold text-slate-700">
        {series.map((line) => <span key={line.name} className="inline-flex items-center gap-1.5">
          <span className="size-2.5 rounded-full" style={{ backgroundColor: line.color }} aria-hidden="true" />{line.name}
        </span>)}
      </figcaption>
    </figure>
  );
}

function BracketBars({ rows, label }: { rows: Bucket[]; label: string }) {
  const max = Math.max(1, ...rows.map((row) => row.count ?? 0));
  return (
    <ul aria-label={label} className="space-y-2">
      {rows.map((row) => (
        <li key={row.label} className="space-y-1">
          <div className="flex flex-wrap justify-between gap-2 text-sm"><span>{row.label}</span><strong><Value value={row.count} /></strong></div>
          {row.count != null && <div className="h-2.5 rounded bg-slate-100" aria-hidden="true">
            <div className="h-2.5 rounded bg-cyan-800" style={{ width: `${row.count / max * 100}%` }} />
          </div>}
        </li>
      ))}
    </ul>
  );
}

export function WageDistributionsPanel({ wages }: { wages: WageDistributions }) {
  const pairs = wages.beforeTraining.map((bucket, index) => ({
    label: bucket.label, before: bucket.count, after: wages.afterTraining[index]?.count,
  }));
  const hasPairs = pairs.some(({ before, after }) => before !== 0 || after !== 0);
  return (
    <div className="space-y-4">
      <Panel code="Wages" title="Current monthly wage distribution">
        <p className="text-sm text-slate-600">One current monthly INR wage observation per candidate with a matching training enrollment.</p>
        <BracketBars rows={wages.currentWages} label="Current wage brackets" />
      </Panel>
      <Panel code="Wage change" title="Pay before and after training">
        <p className="text-sm text-slate-600">Compares monthly INR wages for the same candidates. The baseline is the latest recorded pay before enrollment. The follow-up is the first recorded pay in a linked job after completion.</p>
        {hasPairs ? (
          <>
            <Trend label="Distribution of paired monthly wages before and after training" labels={pairs.map((row) => row.label)}
              series={[
                { name: 'Before training', values: pairs.map((row) => row.before), color: '#0e7490' },
                { name: 'After training', values: pairs.map((row) => row.after ?? null), color: '#4338ca' },
              ]} />
            <MisTable title="Paired wage distribution" rows={pairs} rowKey={(row) => row.label} columns={[
              { key: 'bracket', title: 'Monthly pay', value: (row) => row.label },
              { key: 'before', title: 'Before training', value: (row) => row.before, format: 'count' },
              { key: 'after', title: 'After training', value: (row) => row.after, format: 'count' },
            ]} />
            <h3 className="pt-2 font-semibold text-navy">Change in monthly pay</h3>
            <BracketBars rows={wages.pairedChange} label="Monthly pay change brackets" />
          </>
        ) : <p className="rounded-md bg-slate-50 p-4 text-sm text-slate-600">No comparable before-and-after wage records match this cohort.</p>}
      </Panel>
    </div>
  );
}

const exitAreaColumns: TableColumn<ExitArea>[] = [
  { key: 'name', title: 'Area', value: (row) => row.name, sortable: true },
  { key: 'exits', title: 'Recorded exits', value: (row) => row.exits, format: 'count', sortable: true },
];
const signalColumns: TableColumn<DropoutSignal>[] = [
  { key: 'district', title: 'District', value: (row) => row.district, sortable: true },
  { key: 'provider', title: 'Training provider', value: (row) => row.provider, sortable: true },
  { key: 'attendance', title: 'Attendance dip', value: (row) => row.attendanceDips, format: 'count', sortable: true },
  { key: 'assessment', title: 'Assessment lag', value: (row) => row.assessmentLags, format: 'count', sortable: true },
  { key: 'alerts', title: 'Candidates with a signal', value: (row) => row.candidatesWithSignal, format: 'count', sortable: true },
];

export function RiskPanels({ districts, sectors, signals }: { districts: ExitArea[]; sectors: ExitArea[]; signals: DropoutSignal[] }) {
  return (
    <>
      <Panel code="Early warnings" title="Candidate dropout signals">
        <p className="text-sm text-slate-600">Rule-based alerts for active enrollments. Attendance flags require at least three marked sessions in each 14-day window, a decline of at least 15 percentage points, and recent attendance below 75%. An assessment flag means a scheduled assessment is at least seven days past due with no recorded appearance.</p>
        <MisTable title="Candidate dropout signals" rows={signals} rowKey={(row) => row.key} columns={signalColumns} initialSort="alerts" />
      </Panel>
      <Panel code="Regional exits" title="Attrition by sector and district">
        <p className="text-sm text-slate-600">Sector follows the course linked to the employment spell. District follows the candidate’s recorded location.</p>
        <div className="grid gap-4 lg:grid-cols-2">
          <div className="min-w-0 space-y-2"><h3 className="font-semibold">By sector</h3><MisTable title="Exits by sector"
            rows={sectors} rowKey={(row) => row.key} columns={exitAreaColumns} initialSort="exits" /></div>
          <div className="min-w-0 space-y-2"><h3 className="font-semibold">By district</h3><MisTable title="Exits by district"
            rows={districts} rowKey={(row) => row.key} columns={exitAreaColumns} initialSort="exits" /></div>
        </div>
      </Panel>
    </>
  );
}

const dayFormat = new Intl.DateTimeFormat('en-IN', { day: 'numeric', month: 'short', timeZone: 'UTC' });
const dayLabel = (value: string) => dayFormat.format(new Date(value));

type Dqi = Schema['OperationalDataQualityScorecardResponse'];
type Domain = NonNullable<Dqi['domainBreakdown']>[number];
const dqiColumns: TableColumn<Domain>[] = [
  { key: 'domain', title: 'Entity type', value: (row) => row.entityType?.replace(/_/g, ' ').toLowerCase(), sortable: true },
  { key: 'population', title: 'Eligible records', value: (row) => row.eligibleCount, format: 'count', sortable: true },
  { key: 'affected', title: 'Affected records', value: (row) => row.distinctAffectedCount, format: 'count', sortable: true },
  { key: 'score', title: 'Cleanliness', value: (row) => row.cleanlinessIndexPct, format: 'percent', sortable: true, noData: (row) => row.eligibleCount === 0 },
];

export function CleanlinessPanel({ score }: { score: Dqi }) {
  const value = score.dataCleanlinessIndexPct;
  return (
    <Panel code="DQI" title="Nationwide data cleanliness">
      <div className="grid items-start gap-5 lg:grid-cols-[13rem_minmax(0,1fr)]">
        <div className="rounded-lg border border-slate-200 bg-slate-50 p-4 text-center">
          <svg viewBox="0 0 124 124" className="mx-auto w-full max-w-44" role="img"
            aria-label={value != null ? `Nationwide data cleanliness index ${formatMetric(value, 'percent')}` : 'Nationwide data cleanliness index unavailable'}>
            <circle cx="62" cy="62" r="49" fill="none" stroke="#cbd5e1" strokeWidth="10" />
            {value != null && <circle cx="62" cy="62" r="49" fill="none" stroke="#0e7490" strokeWidth="10" strokeLinecap="round"
              pathLength="100" strokeDasharray={`${value} 100`} transform="rotate(-90 62 62)" />}
            <text x="62" y="68" textAnchor="middle" fill="#1a3c6e" fontSize="21" fontWeight="700">
              {value != null ? formatMetric(value, 'percent') : 'No data'}
            </text>
          </svg>
          <p className="mt-2 text-xs font-medium text-slate-600">Current cohort and unresolved issues</p>
        </div>
        <MisTable title="Cleanliness by data domain" rows={score.domainBreakdown ?? []} columns={dqiColumns} rowKey={(row) => row.entityType} initialSort="score" />
      </div>
    </Panel>
  );
}

export function IncidentPanel({ data, days, onDays }: { data: IncidentDay[]; days: number; onDays: (value: number) => void }) {
  const peak = data.reduce<IncidentDay | undefined>((best, day) =>
    day.errors + day.critical > (best ? best.errors + best.critical : 0) ? day : best, undefined);
  return (
    <Panel code="Incidents" title="Operational incident trend">
      <div className="flex flex-wrap items-end justify-between gap-2">
        <p className="text-sm text-slate-600">Persisted error and critical events by day.</p>
        <div>
          <label className="form-label" htmlFor="incident-period">Incident period</label>
          <select id="incident-period" className="form-input" value={days} onChange={(event) => onDays(Number(event.target.value))}>
            <option value="7">Last 7 days</option><option value="30">Last 30 days</option><option value="90">Last 90 days</option>
          </select>
        </div>
      </div>
      <Trend label="Daily errors and critical events" labels={data.map((day) => dayLabel(day.day))} series={[
        { name: 'Errors', color: '#0e7490', values: data.map((day) => day.errors) },
        { name: 'Critical events', color: '#b91c1c', values: data.map((day) => day.critical) },
      ]} />
      <p className="text-sm text-slate-600">{peak ? `Peak day ${dayLabel(peak.day)} · ${peak.errors} errors and ${peak.critical} critical events` : 'No error or critical events were recorded in this period.'}</p>
      <details>
        <summary className="cursor-pointer text-sm font-semibold text-navy">Daily incident counts</summary>
        <div className="mt-2"><MisTable title="Daily incident counts" rows={data} rowKey={(row) => row.day} initialSort="day" columns={[
          { key: 'day', title: 'Day', value: (row) => row.day, sortable: true },
          { key: 'error', title: 'Errors', value: (row) => row.errors, sortable: true, format: 'count' },
          { key: 'critical', title: 'Critical', value: (row) => row.critical, sortable: true, format: 'count' },
        ]} /></div>
      </details>
    </Panel>
  );
}

export function SystemHealthPanel({ data }: { data: Schema['OperationalSystemHealthResponse'] }) {
  return (
    <Panel code="System health" title="Operational health summary">
      <p className="text-sm text-slate-600">Last 30 calendar days. Counts reflect recorded system events.</p>
      <MetricTiles metrics={[
        { label: 'Total incidents', value: <Value value={data.totalIncidents} /> },
        { label: 'Errors', value: <Value value={data.errorEventCount} /> },
        { label: 'Critical events', value: <Value value={data.criticalEventCount} /> },
        { label: 'Audit mutations', value: <Value value={data.auditMutationVolume} /> },
      ]} />
    </Panel>
  );
}
