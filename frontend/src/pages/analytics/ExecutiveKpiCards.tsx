import type { ExecutiveHeadline } from '@/api/misApi';
import { formatMetric, MeasureValue } from './MisUi';

const kpiMeta = {
  enrolled: ['Total enrolled', 'Enrollment records', 'count'],
  certified: ['Total certified', 'Distinct trainees with an issued certificate', 'count'],
  placed: ['Total placed', 'Enrollments with a joined placement or a self-reported post-training job', 'count'],
  retention6m: ['6-month retention rate', 'Candidates with a linked post-training wage job whose six-month milestone has passed', 'percent'],
  activeCenters: ['Active training centers', 'Currently active centers serving the cohort', 'count'],
} as const;

export function ExecutiveKpiCards({ headline }: { headline: ExecutiveHeadline }) {
  return (
    <ul className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3 2xl:grid-cols-5">
      {headline.kpis.map((kpi) => {
        const meta = kpiMeta[kpi.key as keyof typeof kpiMeta];
        if (!meta) return null;
        const [label, unit, format] = meta;
        return (
          <li key={kpi.key} className="space-y-2 rounded-lg border border-slate-200 border-t-4 border-t-cyan-800 bg-slate-50 p-3">
            <h3 className="text-sm font-semibold text-slate-700">{label}</h3>
            <p className="text-2xl font-semibold tabular-nums text-navy"><MeasureValue measure={kpi.current} format={format} /></p>
            <p className="text-xs text-slate-600">{unit}</p>
            {headline.previousFinancialYear && (
              <p className="text-xs font-medium text-slate-700">
                {kpi.current.availability !== 'RELEASED' || kpi.changePct == null ? 'Comparison unavailable'
                  : `${kpi.changePct > 0 ? '+' : ''}${formatMetric(kpi.changePct, 'percent')} vs ${headline.previousFinancialYear}`}
              </p>
            )}
          </li>
        );
      })}
    </ul>
  );
}
