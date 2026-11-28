import { useState } from 'react';
import type { Category, Course, CourseWages, Employer, MisOptions, MisComparisons, Provider, SchemeProgram } from '@/api/misApi';
import { MisTable, Panel, Value, type TableColumn } from './MisUi';
import { matchKeywords } from '@/lib/keywordSearch';

const administration = { CENTRAL: 'Central administration', STATE: 'State administration', OTHER: 'Other administration' };
const placementDefinition = 'Placement counts a joined placement or a self-reported post-training wage job once per enrollment.';
type Program = SchemeProgram & { administration: keyof typeof administration };

const programColumns: TableColumn<Program>[] = [
  { key: 'program', title: 'Program', value: (row) => row.programName, sortable: true },
  { key: 'scheme', title: 'Scheme', value: (row) => row.schemeName, sortable: true },
  { key: 'administration', title: 'Administered by', value: (row) => administration[row.administration] },
  { key: 'enrolled', title: 'Enrollments', value: (row) => row.enrollmentCount, sortable: true, format: 'count' },
  { key: 'certified', title: 'Certification rate', value: (row) => row.certificationRatePct, sortable: true, format: 'percent', noData: (row) => row.enrollmentCount === 0 },
  { key: 'placed', title: 'Placement rate', value: (row) => row.placementRatePct, sortable: true, format: 'percent', noData: (row) => row.enrollmentCount === 0 },
];

export function ProgramMatrix({ programs, schemes }: { programs: MisComparisons['programs']; schemes: MisOptions['schemes'] }) {
  const [metric, setMetric] = useState<'placementRatePct' | 'certificationRatePct'>('placementRatePct');
  const byScheme = new Map(schemes.map((scheme) => [scheme.id, scheme.administration]));
  const rows: Program[] = programs.map((row) => ({ ...row, administration: byScheme.get(row.schemeId ?? -1) ?? 'OTHER' }));
  return (
    <Panel code="Q03" title="Scheme and program comparison">
      <p className="text-sm text-slate-600">Groups reflect the scheme’s administering organization. The scheme catalog has no funding split.</p>
      {metric === 'certificationRatePct' && <p className="text-sm text-slate-600">Certification rates count each trainee once per program.</p>}
      {metric === 'placementRatePct' && <p className="text-sm text-slate-600">{placementDefinition}</p>}
      <div className="max-w-60">
        <label className="form-label" htmlFor="mis-program-metric">Comparison metric</label>
        <select id="mis-program-metric" className="form-input" value={metric} onChange={(event) => setMetric(event.target.value as typeof metric)}>
          <option value="placementRatePct">Placement rate</option>
          <option value="certificationRatePct">Certification rate</option>
        </select>
      </div>
      <div className="grid gap-3 md:grid-cols-2">
        {(['CENTRAL', 'STATE'] as const).map((group) => {
          const items = rows.filter((row) => row.administration === group)
            .sort((a, b) => (b[metric] ?? -1) - (a[metric] ?? -1) || (a.programName ?? '').localeCompare(b.programName ?? ''))
            .slice(0, 5);
          return (
            <figure key={group} className="space-y-3 rounded-lg border border-slate-200 bg-slate-50 p-3">
              <figcaption className="font-semibold text-navy">{administration[group]}</figcaption>
              {items.length === 0 ? <p className="text-sm text-slate-600">No matching programs.</p> : (
                <ul className="space-y-2">
                  {items.map((row) => (
                    <li key={row.programId} className="space-y-1">
                      <div className="flex flex-wrap justify-between gap-1 text-sm"><span>{row.programName}</span>
                        <Value value={row[metric]} format="percent" noData /></div>
                      {row[metric] != null && <div className="h-2 rounded bg-slate-200" aria-hidden="true">
                        <div className={`h-2 rounded ${group === 'CENTRAL' ? 'bg-cyan-800' : 'bg-indigo-700'}`}
                          style={{ width: `${Math.min(100, Math.max(0, row[metric]!))}%` }} />
                      </div>}
                    </li>
                  ))}
                </ul>
              )}
            </figure>
          );
        })}
      </div>
      <MisTable title="Program outcomes" rows={rows} columns={programColumns} rowKey={(row) => row.programId} initialSort="placed" missingAsNoData />
    </Panel>
  );
}

type ScoredProvider = Provider & { qualityValue?: number | null };
const providerColumns: TableColumn<ScoredProvider>[] = [
  { key: 'provider', title: 'Training provider', value: (row) => row.providerName, sortable: true },
  { key: 'enrolled', title: 'Enrollments', value: (row) => row.enrollmentCount, format: 'count', sortable: true },
  { key: 'placed', title: 'Placement efficiency', value: (row) => row.placementRatePct, format: 'percent', sortable: true, noData: (row) => row.enrollmentCount === 0 },
  { key: 'quality', title: 'Enrollment DQI', value: (row) => row.qualityValue, format: 'percent', sortable: true, noData: (row) => row.qualityValue === undefined },
];

export function ProviderLeaderboard({ providers, quality }: Pick<MisComparisons, 'providers' | 'quality'>) {
  const scores = new Map(quality.map(({ providerId, scorePct }) => [providerId,
    scorePct.availability === 'RELEASED' ? scorePct.value : null]));
  const rows = providers.map((provider) => ({ ...provider, qualityValue: scores.get(provider.providerId ?? -1) }));
  return (
    <Panel code="Q04" title="Training provider leaderboard">
      <p className="text-sm text-slate-600">Enrollment DQI is the share of enrollment records without an unresolved data quality issue.</p>
      <p className="text-sm text-slate-600">{placementDefinition}</p>
      <MisTable title="Training provider leaderboard" rows={rows} columns={providerColumns} rowKey={(row) => row.providerId} initialSort="placed" missingAsNoData />
    </Panel>
  );
}

const courseColumns: TableColumn<Course>[] = [
  { key: 'course', title: 'Course', value: (row) => row.courseName, sortable: true },
  { key: 'qualification', title: 'Qualification', value: (row) => row.qualificationLevelName },
  { key: 'enrolled', title: 'Enrollments', value: (row) => row.enrollmentCount, format: 'count', sortable: true },
  { key: 'placed', title: 'Placement rate', value: (row) => row.placementRatePct, format: 'percent', sortable: true, noData: (row) => row.enrollmentCount === 0 },
  { key: 'wage', title: 'Avg. starting wage (₹/month)', value: (row) => row.avgStartingWage, format: 'rupees', sortable: true, ambiguous: true, noData: (row) => row.enrollmentCount === 0 },
];

export function CourseOutcomes({ courses }: Pick<MisComparisons, 'courses'>) {
  const [search, setSearch] = useState('');
  const filtered = matchKeywords(courses, search, row => String(courseColumns[0].value(row) ?? ''));
  return (
    <Panel code="Q05" title="Course outcomes">
      <p className="text-sm text-slate-600">Starting wage uses the first recorded monthly pay in INR.</p>
      <label className="form-label max-w-xs">Search courses
        <input type="search" className="form-input" value={search} maxLength={120} onChange={event => setSearch(event.target.value)} />
      </label>
      <MisTable title="Course outcomes" rows={filtered} columns={courseColumns} rowKey={(row) => row.courseId} initialSort="placed" missingAsNoData />
    </Panel>
  );
}

const employerColumns: TableColumn<Employer>[] = [
  { key: 'employer', title: 'Employer', value: (row) => row.employerName, sortable: true },
  { key: 'placements', title: 'Placement records', value: (row) => row.placementCount, format: 'count', sortable: true },
  { key: 'joined', title: 'Joined', value: (row) => row.joinedCount, format: 'count', sortable: true },
  { key: 'rate', title: 'Joining rate', value: (row) => row.joinRatePct, format: 'percent', sortable: true, noData: (row) => row.placementCount === 0 },
  { key: 'salary', title: 'Avg. joining pay (₹/month)', value: (row) => row.avgJoiningSalary, format: 'rupees', sortable: true, ambiguous: true },
];

export function EmployerHiring({ employers }: Pick<MisComparisons, 'employers'>) {
  return <Panel code="Q07" title="Employer hiring"><MisTable title="Employer hiring" rows={employers} columns={employerColumns} rowKey={(row) => row.employerId} initialSort="joined" missingAsNoData /></Panel>;
}

type Economic = MisComparisons['economic'];
export function EmploymentMix({ mix }: { mix?: Economic['employmentMix'] }) {
  const shares = [
    { label: 'Wage employment', value: mix?.wageEmploymentSharePct },
    { label: 'Self-employment', value: mix?.selfEmploymentSharePct },
    { label: 'Apprenticeships', value: mix?.apprenticeshipSharePct },
    { label: 'Internships', value: mix?.internshipSharePct },
  ];
  return (
    <Panel code="Q08" title="Employment mix">
      <dl className="grid gap-3 sm:grid-cols-3">
        <div className="rounded-lg bg-slate-50 p-3"><dt className="text-sm text-slate-600">Employment spells</dt><dd className="mt-1 text-lg font-semibold text-navy"><Value value={mix?.employmentSpellCount} noData /></dd></div>
        <div className="rounded-lg bg-slate-50 p-3"><dt className="text-sm text-slate-600">Current spells</dt><dd className="mt-1 text-lg font-semibold text-navy"><Value value={mix?.currentSpellCount} noData /></dd></div>
        <div className="rounded-lg bg-slate-50 p-3"><dt className="text-sm text-slate-600">Average duration</dt><dd className="mt-1 text-lg font-semibold text-navy"><Value value={mix?.avgDurationDays} format="days" noData /></dd></div>
      </dl>
      <p className="text-sm text-slate-600">Shares use all employment spells as the denominator. Categories can overlap.</p>
      <ul className="space-y-2" aria-label="Employment shares">
        {shares.map(({ label, value }) => (
          <li key={label} className="flex flex-wrap items-center gap-2 text-sm">
            <span className="min-w-36">{label}</span>
            {value != null && <span aria-hidden="true" className="h-2 min-w-24 flex-1 rounded bg-slate-100">
              <span className="block h-2 rounded bg-cyan-800" style={{ width: `${Math.min(100, Math.max(0, value))}%` }} />
            </span>}
            <Value value={value} format="percent" noData />
          </li>
        ))}
      </ul>
    </Panel>
  );
}

const categoryColumns: TableColumn<Category>[] = [
  { key: 'type', title: 'Engagement type', value: (row) => row.engagementTypeName || row.engagementTypeCode, sortable: true },
  { key: 'spells', title: 'Spells', value: (row) => row.spellCount, format: 'count', sortable: true },
  { key: 'current', title: 'Current spells', value: (row) => row.currentSpellCount, format: 'count', sortable: true },
  { key: 'share', title: 'Share of spells', value: (row) => row.sharePct, format: 'percent', sortable: true },
  { key: 'duration', title: 'Average duration', value: (row) => row.avgDurationDays, format: 'days', sortable: true },
];

export function EngagementTypes({ categories }: { categories: Category[] }) {
  return <Panel code="Q09" title="Engagement types"><MisTable title="Engagement types" rows={categories} columns={categoryColumns} rowKey={(row) => row.engagementTypeCode} initialSort="spells" missingAsNoData /></Panel>;
}

export function WageProgression({ wages }: { wages?: Economic['wageProgression'] }) {
  const first = wages?.avgFirstRecordedSalary;
  const last = wages?.avgLatestRecordedSalary;
  const max = Math.max(1, first ?? 0, last ?? 0);
  const points = [
    { label: 'First recorded monthly pay', value: first },
    { label: 'Latest recorded monthly pay', value: last },
  ];
  return (
    <Panel code="Q10" title="Wage progression">
      <div className="space-y-3 rounded-lg border border-slate-200 bg-slate-50 p-4">
        {points.map(({ label, value }) => (
          <div key={label} className="space-y-1 text-sm">
            <div className="flex flex-wrap justify-between gap-2"><span>{label}</span><strong><Value value={value} noData format="rupees" /></strong></div>
            {value != null && <div className="h-3 rounded bg-slate-200" aria-hidden="true">
              <div className="h-3 rounded bg-cyan-800" style={{ width: `${value / max * 100}%` }} />
            </div>}
          </div>
        ))}
      </div>
      <p className="text-sm text-slate-600">Change uses paired monthly INR salary observations from the same employment spell.</p>
      <dl className="grid gap-3 sm:grid-cols-2">
        <div><dt className="text-sm text-slate-600">Average pay change</dt><dd className="font-semibold"><Value value={wages?.avgSalaryChangeAmount} format="rupees" noData /></dd></div>
        <div><dt className="text-sm text-slate-600">Average growth</dt><dd className="font-semibold"><Value value={wages?.avgSalaryChangePct} format="percent" noData /></dd></div>
      </dl>
    </Panel>
  );
}

const wageColumns: TableColumn<CourseWages>[] = [
  { key: 'course', title: 'Course', value: (row) => row.courseName, sortable: true },
  { key: 'spells', title: 'Employment spells', value: (row) => row.spellCount, format: 'count', sortable: true },
  { key: 'first', title: 'First pay (₹/month)', value: (row) => row.avgFirstRecordedSalary, format: 'rupees', sortable: true, ambiguous: true },
  { key: 'last', title: 'Latest pay (₹/month)', value: (row) => row.avgLatestRecordedSalary, format: 'rupees', sortable: true, ambiguous: true },
  { key: 'growth', title: 'Average growth', value: (row) => row.avgSalaryChangePct, format: 'percent', sortable: true, ambiguous: true },
];

export function WageGrowthByCourse({ rows }: { rows: CourseWages[] }) {
  return <Panel code="Q11" title="Wage progression by course"><MisTable title="Wage progression by course" rows={rows} columns={wageColumns} rowKey={(row) => row.courseId} initialSort="growth" missingAsNoData /></Panel>;
}
