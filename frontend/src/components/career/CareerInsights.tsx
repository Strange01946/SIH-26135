import { useQuery } from '@tanstack/react-query';
import { useId, useState } from 'react';
import { getEmploymentRetention, type WageObservation } from '@/api/careerApi';
import { ErrorState } from '@/components/feedback/ErrorState';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import type { TraineeSkillGapProfile } from '@/types/trainee';

const retentionLabels = {
  NOT_YET_DUE: ['Not yet due', 'bg-slate-100 text-slate-700'],
  RETAINED: ['Retained', 'bg-emerald-50 text-emerald-800'],
  ENDED_BEFORE_MILESTONE: ['Employment ended before milestone', 'bg-amber-50 text-amber-900'],
};

export const careerDate = (date?: string | null) => date
  ? new Intl.DateTimeFormat('en-IN', { dateStyle: 'medium', timeZone: 'UTC' }).format(new Date(date))
  : '—';
const currencyFormat = (amount: number, currency: string) => currency + ' ' + amount.toLocaleString('en-IN');
const payUnit = (wage: WageObservation) => [wage.currency, wage.frequency?.toLowerCase()].filter(Boolean).join(' / ');
const recordStatus = (status?: string) => status?.toLowerCase().replace(/_/g, ' ') ?? '—';

export function RetentionPanel({ employmentId }: { employmentId: number }) {
  const retention = useQuery({
    queryKey: ['career', 'retention', employmentId],
    queryFn: ({ signal }) => getEmploymentRetention(employmentId, signal),
    retry: false,
  });
  if (retention.isPending) return <LoadingSpinner label="Loading retention milestones" />;
  if (retention.isError) {
    return <ErrorState error={retention.error} title="Unable to load retention" onRetry={() => void retention.refetch()} />;
  }
  const { milestones, wages, asOf } = retention.data;
  const wageSeries = new Map<string, WageObservation[]>();
  for (const wage of wages) {
    const series = wageSeries.get(payUnit(wage)) ?? [];
    series.push(wage);
    wageSeries.set(payUnit(wage), series);
  }
  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <h3 className="font-bold text-navy">Retention timeline</h3>
        <p className="text-xs text-slate-600">As of {careerDate(asOf)}</p>
      </div>
      <ol className="grid gap-2 sm:grid-cols-3" aria-label="Retention milestones">
        {milestones.map((milestone) => {
          const [label, color] = retentionLabels[milestone.status];
          return (
            <li key={milestone.months} className={`rounded-md border border-slate-200 p-3 ${color}`}>
              <p className="font-bold">{milestone.months} months</p>
              <p className="text-xs">{careerDate(milestone.dueOn)}</p>
              <p className="mt-2 text-sm font-medium">{label}</p>
            </li>
          );
        })}
      </ol>
      <div className="space-y-3">
        <h3 className="font-bold text-navy">Wage progression</h3>
        {wages.length === 0 ? <p className="text-sm text-slate-600">No wage observations have been recorded for this employment.</p> : (
          <>
            {[...wageSeries.entries()].map(([unit, series]) => <WageChart key={unit} unit={unit} series={series} />)}
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm" aria-label="Wage observations">
                <thead className="bg-slate-100 text-slate-700">
                  <tr>{['Effective date', 'Wage', 'Observation', 'Verification'].map((label) => <th key={label} scope="col" className="p-2">{label}</th>)}</tr>
                </thead>
                <tbody>
                  {wages.map((wage) => (
                    <tr key={wage.date} className="border-b">
                      <td className="p-2">{careerDate(wage.date)}</td>
                      <td className="p-2">{currencyFormat(wage.amount, wage.currency)}{wage.frequency && ` / ${wage.frequency.toLowerCase()}`}</td>
                      <td className="p-2">{wage.startingWage ? 'Starting wage' : wage.observationMonth != null ? `Month ${wage.observationMonth}` : 'Salary history'}</td>
                      <td className="p-2 capitalize">{recordStatus(wage.verificationStatus)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </>
        )}
      </div>
    </div>
  );
}

function WageChart({ unit, series }: { unit: string; series: WageObservation[] }) {
  const maximum = Math.max(...series.map((wage) => wage.amount), 1);
  const minimum = Math.min(...series.map((wage) => wage.amount));
  const lower = minimum === maximum ? minimum * 0.8 : Math.max(0, minimum - (maximum - minimum) * 0.15);
  const upper = maximum + Math.max(1, (maximum - lower) * 0.12);
  const start = Date.parse(series[0].date);
  const end = Date.parse(series[series.length - 1].date);
  const points = series.map((wage) => ({
    x: end === start ? 310 : 90 + (Date.parse(wage.date) - start) / (end - start) * 440,
    y: 122 - (wage.amount - lower) / (upper - lower) * 100,
    wage,
  }));
  return (
    <figure className="rounded-lg border border-slate-200 bg-slate-50 px-3 pt-2">
      <figcaption className="text-xs font-semibold text-slate-700">{unit}</figcaption>
      <svg viewBox="0 0 600 160" className="block w-full max-h-56" role="img" aria-label={`Recorded wages over time in ${unit}`}>
        {[0, 0.5, 1].map((fraction) => (
          <g key={fraction}>
            <line x1="88" x2="534" y1={122 - fraction * 100} y2={122 - fraction * 100} stroke="#cbd5e1" />
            <text x="82" y={126 - fraction * 100} textAnchor="end" fill="#475569" fontSize="12">
              {Math.round(lower + (upper - lower) * fraction).toLocaleString('en-IN')}
            </text>
          </g>
        ))}
        {points.length > 1 && <polyline fill="none" stroke="#0e7490" strokeWidth="3" points={points.map(({ x, y }) => `${x},${y}`).join(' ')} />}
        {points.map(({ x, y, wage }) => (
          <circle key={wage.date} cx={x} cy={y} r="5" fill="#0e7490" stroke="white" strokeWidth="2">
            <title>{careerDate(wage.date)} · {currencyFormat(wage.amount, wage.currency)}</title>
          </circle>
        ))}
        <text x={points.length === 1 ? 310 : 90} y="146" textAnchor={points.length === 1 ? 'middle' : 'start'} fill="#475569" fontSize="12">{careerDate(series[0].date)}</text>
        {points.length > 1 && <text x="530" y="146" textAnchor="end" fill="#475569" fontSize="12">{careerDate(series[series.length - 1].date)}</text>}
      </svg>
    </figure>
  );
}

type Gap = NonNullable<TraineeSkillGapProfile['currentSkillGaps']>[number];
type Comparison = Gap & { observedSkillLevelRank: number; requiredSkillLevelRank: number };
const hasRanks = (gap: Gap): gap is Comparison => gap.observedSkillLevelRank != null && gap.requiredSkillLevelRank != null;

export function SkillGapInsights({ profile }: { profile: TraineeSkillGapProfile }) {
  const contextId = useId();
  const [selectedAssessment, setSelectedAssessment] = useState<number>();
  const gaps = profile.currentSkillGaps ?? [];
  const assessments = (profile.assessments ?? []).filter((assessment) =>
    assessment.id != null && gaps.some((gap) => gap.skillGapAssessmentId === assessment.id));
  const assessment = assessments.find((item) => item.id === selectedAssessment) ?? assessments[0];
  const contextualGaps = assessment ? gaps.filter((gap) => gap.skillGapAssessmentId === assessment.id) : gaps;
  const compared = contextualGaps.filter(hasRanks).slice(0, 6);
  const target = assessment?.jobPostingTitle || assessment?.jobRoleName;
  if (!gaps.length) return <p className="text-sm text-slate-600">No current skill deficits have been recorded for this candidate.</p>;

  return (
    <div className="space-y-3">
      <h3 className="font-bold text-navy">Assessed skills and required levels</h3>
      {assessments.length > 1 && (
        <div className="max-w-md">
          <label className="block text-sm font-semibold" htmlFor={contextId}>Assessment</label>
          <select id={contextId} className="form-input" value={assessment?.id ?? ''} onChange={(event) => setSelectedAssessment(Number(event.target.value))}>
            {assessments.map((item) => <option key={item.id} value={item.id}>{item.jobPostingTitle || item.jobRoleName || item.assessmentNumber} · {careerDate(item.assessedOn)}</option>)}
          </select>
        </div>
      )}
      <p className="text-sm text-slate-600">{target ? `Industry demand benchmark · ${target}` : 'Required levels recorded in the assessment'}</p>
      {compared.length >= 3 && <SkillRadar comparisons={compared} />}
      {compared.length > 0 && compared.length < 3 && <SkillBars comparisons={compared} />}
      {compared.length === 0 && <p className="text-sm text-slate-600">The assessment has no observed and required levels that can be plotted together.</p>}
      {contextualGaps.filter(hasRanks).length > compared.length && <p className="text-xs text-slate-600">The chart shows six skills. The table lists all recorded deficits for this assessment.</p>}
      <div className="overflow-x-auto">
        <table className="w-full text-left text-sm" aria-label="Skill deficits">
          <thead className="bg-slate-100 text-slate-700">
            <tr>{['Skill', 'Assessed level', 'Required level', 'Gap', 'Status'].map((label) => <th key={label} scope="col" className="p-2">{label}</th>)}</tr>
          </thead>
          <tbody>
            {contextualGaps.map((gap) => (
              <tr key={gap.id} className="border-b">
                <th scope="row" className="p-2 font-medium">{gap.skillName}</th>
                <td className="p-2">{gap.observedSkillLevelName ?? gap.observedSkillLevelRank ?? '—'}</td>
                <td className="p-2">{gap.requiredSkillLevelName ?? gap.requiredSkillLevelRank ?? '—'}</td>
                <td className="p-2">{gap.gapLevelDelta ?? '—'}</td>
                <td className="p-2">{gap.skillGapStatusName ?? '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function SkillRadar({ comparisons }: { comparisons: Comparison[] }) {
  const scale = Math.max(5, ...comparisons.flatMap((gap) => [gap.observedSkillLevelRank, gap.requiredSkillLevelRank]));
  const point = (index: number, radius: number) => {
    const angle = index * Math.PI * 2 / comparisons.length - Math.PI / 2;
    return { x: 205 + Math.cos(angle) * radius, y: 122 + Math.sin(angle) * radius };
  };
  const polygon = (radiusAt: (index: number) => number) => comparisons.map((_, index) => {
    const { x, y } = point(index, radiusAt(index));
    return `${x},${y}`;
  }).join(' ');
  return (
    <figure className="rounded-lg border border-slate-200 bg-slate-50 p-3">
      <svg viewBox="0 0 410 245" className="mx-auto block w-full max-w-lg" role="img" aria-label="Skill deficit radar chart. The table below gives the exact levels.">
        {[1, 2, 3, 4, 5].map((ring) => <polygon key={ring} points={polygon(() => 16 * ring)} fill="none" stroke="#cbd5e1" />)}
        {comparisons.map((gap, index) => {
          const axis = point(index, 80);
          const label = point(index, 101);
          return <g key={gap.id}>
            <line x1="205" y1="122" x2={axis.x} y2={axis.y} stroke="#94a3b8" />
            <text x={label.x} y={label.y + 4} fontSize="12" textAnchor="middle" fill="#334155">
              <title>{gap.skillName}</title>
              {gap.skillName && gap.skillName.length > 21 ? `${gap.skillName.slice(0, 20)}…` : gap.skillName}
            </text>
          </g>;
        })}
        <polygon points={polygon((index) => 80 * comparisons[index].requiredSkillLevelRank / scale)}
          fill="#fde68a" fillOpacity="0.45" stroke="#92400e" strokeWidth="2" />
        <polygon points={polygon((index) => 80 * comparisons[index].observedSkillLevelRank / scale)}
          fill="#67e8f9" fillOpacity="0.55" stroke="#0e7490" strokeWidth="2" />
      </svg>
      <figcaption className="flex flex-wrap justify-center gap-4 text-xs font-semibold">
        <span className="text-cyan-800">● Assessed level</span>
        <span className="text-amber-900">● Required level</span>
      </figcaption>
    </figure>
  );
}

function SkillBars({ comparisons }: { comparisons: Comparison[] }) {
  const scale = Math.max(5, ...comparisons.flatMap((gap) => [gap.observedSkillLevelRank, gap.requiredSkillLevelRank]));
  return (
    <div className="space-y-3 rounded-lg border border-slate-200 bg-slate-50 p-3">
      {comparisons.map((gap) => (
        <div key={gap.id} className="space-y-1">
          <p className="text-sm font-semibold">{gap.skillName}</p>
          <div className="flex items-center gap-2 text-xs text-cyan-900"><span className="w-24">Assessed {gap.observedSkillLevelRank}</span><span className="h-2 rounded bg-cyan-700" style={{ width: `${65 * gap.observedSkillLevelRank / scale}%` }} /></div>
          <div className="flex items-center gap-2 text-xs text-amber-900"><span className="w-24">Required {gap.requiredSkillLevelRank}</span><span className="h-2 rounded bg-amber-600" style={{ width: `${65 * gap.requiredSkillLevelRank / scale}%` }} /></div>
        </div>
      ))}
    </div>
  );
}
