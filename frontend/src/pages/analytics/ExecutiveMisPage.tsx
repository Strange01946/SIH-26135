import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link, useSearchParams } from 'react-router-dom';
import { getMisComparisons, getMisOptions, getMisOverview, type MisFilters } from '@/api/misApi';
import { ErrorState } from '@/components/feedback/ErrorState';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { useAuth } from '@/context/AuthContext';
import { ExecutiveKpis, GeographicPanel, TrainingFunnel, type MapMetric } from './MisOverview';
import {
  CourseOutcomes, EmployerHiring, EmploymentMix, EngagementTypes,
  ProgramMatrix, ProviderLeaderboard, WageGrowthByCourse, WageProgression,
} from './MisComparisons';
import { FilterStrip, loadFilters } from './MisFilters';

const sections = {
  overview: 'Executive overview',
  programs: 'Programs and providers',
  employment: 'Employment',
  wages: 'Wages',
} as const;
type Section = keyof typeof sections;

export default function ExecutiveMisPage() {
  const { user } = useAuth();
  const scope = { stateId: user?.stateId ?? undefined, districtId: user?.districtId ?? undefined };
  const [params, setParams] = useSearchParams();
  const [mapMetric, setMapMetric] = useState<MapMetric>('placementRatePct');
  const value = params.get('view');
  const view = value && Object.prototype.hasOwnProperty.call(sections, value) ? value as Section : 'overview';
  const options = useQuery({
    queryKey: ['government-mis', 'filters', user?.id],
    queryFn: ({ signal }) => getMisOptions(signal),
    staleTime: 300_000,
    retry: false,
  });
  const filters = options.data ? loadFilters(params, options.data, scope) : {};
  const specializedParams = new URLSearchParams(params);
  specializedParams.delete('view');
  const overview = useQuery({
    queryKey: ['government-mis', 'overview', user?.id, filters],
    queryFn: ({ signal }) => getMisOverview(filters, signal),
    enabled: options.isSuccess && view === 'overview',
    retry: false,
    refetchOnWindowFocus: 'always',
    refetchOnMount: 'always',
  });
  const comparisons = useQuery({
    queryKey: ['government-mis', 'comparisons', user?.id, filters],
    queryFn: ({ signal }) => getMisComparisons(filters, signal),
    enabled: options.isSuccess && view !== 'overview',
    retry: false,
  });
  const report = view === 'overview' ? overview : comparisons;

  const setFilter = (key: keyof MisFilters, next: string) => {
    const updated = new URLSearchParams(params);
    if (next) updated.set(key, next);
    else updated.delete(key);
    if (key === 'stateId') updated.delete('districtId');
    if (key === 'districtId' && next) {
      const district = options.data?.districts.find((item) => item.id === Number(next));
      if (district) updated.set('stateId', String(district.stateId));
    }
    setParams(updated, { replace: true });
  };

  const setView = (section: Section) => {
    const updated = new URLSearchParams(params);
    updated.set('view', section);
    setParams(updated, { replace: true });
  };

  return (
    <div className="mx-auto max-w-screen-2xl space-y-5">
      <header className="flex flex-wrap items-center justify-between gap-4 rounded-xl bg-navy px-5 py-6 text-white sm:px-7">
        <div>
          <h1 className="text-2xl font-semibold sm:text-3xl">Government MIS &amp; executive analytics</h1>
          <p className="mt-1 max-w-2xl text-sm text-blue-100">Track training, placement, retention, and wages across programmes and geographies.</p>
        </div>
        {scope.stateId == null && <Link to={{ pathname: '/analytics/specialized', search: specializedParams.toString() }} className="btn-secondary">
          Specialized analytics &amp; exports
        </Link>}
      </header>
      {options.isPending && <LoadingSpinner label="Loading reporting filters" />}
      {options.isError && <ErrorState error={options.error} title="Unable to open the government dashboard" onRetry={() => void options.refetch()} />}
      {options.data && (
        <>
          <FilterStrip options={options.data} filters={filters} setFilter={setFilter} scope={scope}
            reset={() => setParams(view === 'overview' ? {} : { view }, { replace: true })} />
          <nav aria-label="Executive analytics views" className="flex flex-wrap gap-2">
            {Object.entries(sections).map(([key, label]) => (
              <button key={key} type="button" className={view === key ? 'btn-primary' : 'btn-secondary'}
                onClick={() => setView(key as Section)} aria-pressed={view === key}>{label}</button>
            ))}
          </nav>
          <div aria-busy={report.isFetching} className="space-y-4">
            {report.isPending ? <div className="card p-6"><LoadingSpinner label="Loading the selected reporting cohort" /></div>
              : report.isError ? <ErrorState error={report.error} title="Unable to load the selected analytics cohort" onRetry={() => void report.refetch()} />
              : view === 'overview' ? (
                overview.data && <>
                  <ExecutiveKpis headline={overview.data.headline} />
                  <TrainingFunnel pipeline={overview.data.pipeline} />
                  <GeographicPanel geography={overview.data.geography} options={options.data} filters={filters}
                    metric={mapMetric} onMetricChange={setMapMetric} />
                </>
              ) : comparisons.data && (view === 'programs' ? (
                <>
                  <ProgramMatrix programs={comparisons.data.programs} schemes={options.data.schemes} />
                  <ProviderLeaderboard providers={comparisons.data.providers} quality={comparisons.data.quality} />
                  <CourseOutcomes courses={comparisons.data.courses} />
                </>
              ) : view === 'employment' ? (
                <>
                  <EmployerHiring employers={comparisons.data.employers} />
                  <EmploymentMix mix={comparisons.data.economic.employmentMix} />
                  <EngagementTypes categories={comparisons.data.economic.categoryBreakdown ?? []} />
                </>
              ) : (
                <>
                  <WageProgression wages={comparisons.data.economic.wageProgression} />
                  <WageGrowthByCourse rows={comparisons.data.economic.courseWageProgression ?? []} />
                </>
              ))}
          </div>
        </>
      )}
    </div>
  );
}
