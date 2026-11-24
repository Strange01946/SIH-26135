import { useState, type ReactNode } from 'react';
import { useQuery, type UseQueryResult } from '@tanstack/react-query';
import { Link, useSearchParams } from 'react-router-dom';
import { getMisOptions, type MisFilters } from '@/api/misApi';
import {
  getCleanlinessScore, getIncidentTrend, getSpecializedCohort,
  getSpecializedOutcomes, getSpecializedOutreach, getSystemHealth,
} from '@/api/specializedAnalyticsApi';
import { ErrorState } from '@/components/feedback/ErrorState';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { MisExports } from './MisExports';
import { FilterStrip, loadFilters } from './MisFilters';
import { CleanlinessPanel, IncidentPanel, RiskPanels, SystemHealthPanel, WageDistributionsPanel } from './SpecializedCharts';
import { OutcomePanels, OutreachPanels } from './SpecializedQueryPanels';

const views = {
  outcomes: 'Retention, skills and attrition',
  outreach: 'Outreach and verification',
  cohorts: 'Wages and early warnings',
  operations: 'Data quality and operations',
} as const;
type View = keyof typeof views;

export default function SpecializedAnalyticsPage() {
  const [params, setParams] = useSearchParams();
  const selected = params.get('view');
  const view = selected && Object.prototype.hasOwnProperty.call(views, selected) ? selected as View : 'outcomes';
  const options = useQuery({
    queryKey: ['government-mis', 'filters'], queryFn: ({ signal }) => getMisOptions(signal),
    staleTime: 300_000, retry: false,
  });
  const filters = options.data ? loadFilters(params, options.data) : {};
  const dashboardParams = new URLSearchParams(params);
  dashboardParams.delete('view');
  const dashboardSearch = dashboardParams.toString();

  const update = (key: keyof MisFilters | 'view', value: string) => {
    const updated = new URLSearchParams(params);
    if (value) updated.set(key, value);
    else updated.delete(key);
    if (key === 'stateId') updated.delete('districtId');
    if (key === 'districtId' && value) {
      const district = options.data?.districts.find((item) => item.id === Number(value));
      if (district) updated.set('stateId', String(district.stateId));
    }
    setParams(updated, { replace: true });
  };

  return (
    <div className="mx-auto max-w-screen-2xl space-y-5">
      <header className="flex flex-wrap items-center justify-between gap-4 rounded-xl bg-navy px-5 py-6 text-white sm:px-7">
        <div>
          <Link to={{ pathname: '/analytics', search: dashboardSearch }} className="text-sm text-blue-100 underline">Executive analytics</Link>
          <h1 className="mt-1 text-2xl font-semibold sm:text-3xl">Specialized analytics &amp; MIS exports</h1>
        </div>
      </header>
      {options.isError && <ErrorState error={options.error} title="Unable to load MIS report options" onRetry={() => void options.refetch()} />}
      <MisExports filters={filters} options={options.data} />
      <nav className="flex flex-wrap gap-2" aria-label="Specialized analytics views">
        {Object.entries(views).map(([key, label]) => (
          <button key={key} type="button" className={view === key ? 'btn-primary' : 'btn-secondary'}
            aria-pressed={view === key} onClick={() => update('view', key)}>{label}</button>
        ))}
      </nav>
      {(view === 'outcomes' || view === 'outreach') && (
        <p className="text-sm text-slate-600">Nationwide, all-time data. Reporting filters in Wages and early warnings apply to those charts and exports.</p>
      )}
      {view === 'outcomes' && <Outcomes />}
      {view === 'outreach' && <Outreach />}
      {view === 'cohorts' && (options.data ? (
        <>
          <FilterStrip options={options.data} filters={filters} setFilter={update} reset={() => setParams({ view }, { replace: true })} />
          <CohortReports filters={filters} />
        </>
      ) : options.isPending && <LoadingSpinner label="Loading reporting filters" />)}
      {view === 'operations' && <Operations />}
    </div>
  );
}

function QueryContent<T>({ query, label, children }: {
  query: UseQueryResult<T>; label: string; children: (data: T) => ReactNode;
}) {
  if (query.isError) return <ErrorState error={query.error} title={`Unable to load ${label}`} onRetry={() => void query.refetch()} />;
  return query.isPending ? <LoadingSpinner label={`Loading ${label}`} /> : <div className="space-y-4">{children(query.data)}</div>;
}

function Outcomes() {
  const query = useQuery({ queryKey: ['specialized-analytics', 'outcomes'],
    queryFn: ({ signal }) => getSpecializedOutcomes(signal), retry: false });
  return <QueryContent query={query} label="retention and attrition">{(data) => <OutcomePanels {...data} />}</QueryContent>;
}

function Outreach() {
  const query = useQuery({ queryKey: ['specialized-analytics', 'outreach'],
    queryFn: ({ signal }) => getSpecializedOutreach(signal), retry: false });
  return <QueryContent query={query} label="outreach and verification">{(data) => <OutreachPanels {...data} />}</QueryContent>;
}

function CohortReports({ filters }: { filters: MisFilters }) {
  const query = useQuery({ queryKey: ['specialized-analytics', 'cohorts', filters],
    queryFn: ({ signal }) => getSpecializedCohort(filters, signal), retry: false });
  return (
    <QueryContent query={query} label="wages and early warnings">{({ wages, exits, signals }) => (
      <>
        <WageDistributionsPanel wages={wages} />
        <RiskPanels {...exits} signals={signals} />
      </>
    )}</QueryContent>
  );
}

function Operations() {
  const [days, setDays] = useState(30);
  const score = useQuery({ queryKey: ['specialized-analytics', 'data-cleanliness'],
    queryFn: ({ signal }) => getCleanlinessScore(signal), retry: false });
  const incidents = useQuery({ queryKey: ['specialized-analytics', 'incidents', days],
    queryFn: ({ signal }) => getIncidentTrend(days, signal), retry: false });
  const health = useQuery({ queryKey: ['specialized-analytics', 'system-health'],
    queryFn: ({ signal }) => getSystemHealth(signal), retry: false });
  return (
    <>
      <QueryContent query={score} label="data cleanliness">{(data) => <CleanlinessPanel score={data} />}</QueryContent>
      <QueryContent query={incidents} label="operational incidents">{(data) => <IncidentPanel data={data} days={days} onDays={setDays} />}</QueryContent>
      <QueryContent query={health} label="system health">{(data) => <SystemHealthPanel data={data} />}</QueryContent>
    </>
  );
}
