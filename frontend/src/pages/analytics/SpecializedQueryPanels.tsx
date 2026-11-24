import { useState, type ReactNode } from 'react';
import type { getSpecializedOutcomes, getSpecializedOutreach, Schema } from '@/api/specializedAnalyticsApi';
import { MisTable, Panel, Value, type Format, type TableColumn } from './MisUi';

type Outcomes = Awaited<ReturnType<typeof getSpecializedOutcomes>>;
type Outreach = Awaited<ReturnType<typeof getSpecializedOutreach>>;

function tableColumns<T>(name: TableColumn<T>, fields: [keyof T, string, Format?][]): TableColumn<T>[] {
  return [name, ...fields.map(([key, title, format]) => ({
    key: String(key), title, sortable: true, format: format ?? 'count',
    value: (row: T) => {
      const value = row[key];
      return typeof value === 'number' ? value : null;
    },
  }))];
}

function QueryTable<T>({ code, title, rows, columns, rowKey, sort, searchLabel }: {
  code: string; title: string; rows: T[]; columns: TableColumn<T>[]; rowKey: (row: T) => string | number | undefined;
  sort?: string; searchLabel?: string;
}) {
  const [search, setSearch] = useState('');
  const term = search.trim().toLocaleLowerCase();
  const filtered = rows.filter((row) => String(columns[0].value(row) ?? '').toLocaleLowerCase().includes(term));
  return (
    <Panel code={code} title={title}>
      {searchLabel && rows.length > 0 && (
        <div className="max-w-65">
          <label className="form-label" htmlFor={`search-${code}`}>{searchLabel}</label>
          <input id={`search-${code}`} type="search" className="form-input" value={search} onChange={(event) => setSearch(event.target.value)} />
        </div>
      )}
      <MisTable key={term} title={title} rows={filtered} columns={columns} rowKey={rowKey} initialSort={sort} />
    </Panel>
  );
}

export function MetricTiles({ metrics }: { metrics: { label: string; value: ReactNode }[] }) {
  return (
    <dl className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
      {metrics.map(({ label, value }) => (
        <div key={label} className="rounded-lg border border-slate-200 bg-slate-50 p-3">
          <dt className="text-sm text-slate-600">{label}</dt>
          <dd className="mt-1.5 text-xl font-semibold tabular-nums text-navy">{value}</dd>
        </div>
      ))}
    </dl>
  );
}

type Gap = Schema['TopSkillGapResponse'];
const gapColumns = tableColumns<Gap>({ key: 'skill', title: 'Skill', sortable: true, value: (row) => row.skillName ?? row.skillCode }, [
  ['currentGapCount', 'Current gaps'], ['traineeCount', 'Candidates'], ['avgGapLevelDelta', 'Mean gap level'],
]);
gapColumns.splice(1, 0, { key: 'severity', title: 'Severity', value: (row) => row.severityCode, sortable: true });

type Severity = Schema['SkillGapSeverityBreakdownResponse'];
const severityColumns = tableColumns<Severity>({ key: 'severity', title: 'Severity', value: (row) => row.severityName ?? row.severityCode }, [
  ['currentGapCount', 'Current gaps'], ['traineeCount', 'Candidates'], ['recommendationCount', 'Recommendations'],
  ['acceptedRecommendationCount', 'Accepted'], ['acceptanceRatePct', 'Acceptance rate', 'percent'],
]);

type Unemployment = Schema['UnemploymentReasonBreakdownResponse'];
const unemploymentColumns = tableColumns<Unemployment>({ key: 'reason', title: 'Reason', value: (row) => row.reasonName, sortable: true }, [
  ['periodCount', 'Unemployment periods'], ['traineeCount', 'Candidates'], ['avgDurationDays', 'Average duration', 'days'],
]);

type Exit = Schema['AttritionReasonBreakdownResponse'];
const exitColumns = tableColumns<Exit>({ key: 'reason', title: 'Exit reason', value: (row) => row.exitReasonName, sortable: true }, [
  ['exitCount', 'Exit events'], ['traineeCount', 'Candidates'],
]);
exitColumns.splice(1, 0, { key: 'nature', title: 'Separation type', value: (row) => row.separationNatureName, sortable: true });

export function OutcomePanels({ retention, skills, attrition }: Outcomes) {
  const unemployment = attrition.unemploymentSummary;
  const separation = attrition.separationAnalysis;
  return (
    <>
      <Panel code="Q12" title="Longitudinal retention">
        <p className="text-sm text-slate-600">Rates use the employment spells old enough to reach each milestone.</p>
        <ol className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4" aria-label="Retention by milestone">
          {(retention.milestones ?? []).map((milestone) => (
            <li key={milestone.milestoneMonths} className="space-y-2 rounded-lg border border-slate-200 bg-slate-50 p-4">
              <h3 className="font-semibold text-navy">{milestone.milestoneMonths} months</h3>
              <p className="text-xl font-semibold tabular-nums"><Value value={milestone.retentionRatePct} format="percent" noData={milestone.observableCount === 0} /></p>
              <p className="text-sm text-slate-600">Observable spells <Value value={milestone.observableCount} /></p>
              {milestone.retentionRatePct != null && <div className="h-2 rounded bg-slate-200" aria-hidden="true">
                <div className="h-2 rounded bg-cyan-800" style={{ width: `${milestone.retentionRatePct}%` }} />
              </div>}
            </li>
          ))}
        </ol>
      </Panel>
      <QueryTable code="Q13" title="Top current skill gaps" rows={skills.topSkillGaps ?? []}
        columns={gapColumns} rowKey={(row) => `${row.skillId}-${row.severityCode}`} sort="currentGapCount" searchLabel="Search skills" />
      <QueryTable code="Q14" title="Skill gap severity and recommendation uptake" rows={skills.severityBreakdown ?? []}
        columns={severityColumns} rowKey={(row) => row.severityCode} sort="currentGapCount" />
      <Panel code="Q15" title="Unemployment and re-employment">
        <MetricTiles metrics={[
          { label: 'Unemployment periods', value: <Value value={unemployment?.unemploymentPeriodCount} /> },
          { label: 'Candidates', value: <Value value={unemployment?.traineeCount} /> },
          { label: 'Average duration', value: <Value value={unemployment?.avgDurationDays} format="days" noData={unemployment?.unemploymentPeriodCount === 0} /> },
          { label: 'Re-employment rate', value: <Value value={unemployment?.reemploymentRatePct} format="percent" noData={unemployment?.unemploymentPeriodCount === 0} /> },
        ]} />
      </Panel>
      <QueryTable code="Q16" title="Reasons for unemployment" rows={attrition.unemploymentReasons ?? []}
        columns={unemploymentColumns} rowKey={(row) => row.unemploymentReasonId} sort="periodCount" searchLabel="Search unemployment reasons" />
      <QueryTable code="Q17" title="Employment exit reasons" rows={attrition.exitReasons ?? []}
        columns={exitColumns} rowKey={(row) => `${row.employmentExitReasonId}-${row.separationNatureId}`} sort="exitCount" searchLabel="Search exit reasons" />
      <Panel code="Q18" title="Voluntary and involuntary separations">
        <MetricTiles metrics={[
          { label: 'Voluntary exits', value: <Value value={separation?.voluntaryExitCount} /> },
          { label: 'Involuntary exits', value: <Value value={separation?.involuntaryExitCount} /> },
          { label: 'Other exits', value: <Value value={separation?.otherExitCount} /> },
        ]} />
      </Panel>
    </>
  );
}

type Wave = Schema['FollowupWavePerformanceResponse'];
const waveColumns = tableColumns<Wave>({ key: 'wave', title: 'Wave', value: (row) => row.typeName ?? row.typeCode, sortable: true }, [
  ['taskCount', 'Tasks'], ['successCount', 'Successful contacts'], ['noResponseCount', 'No response'],
  ['unreachableCount', 'Unreachable'], ['successRatePct', 'Contact success', 'percent'],
]);

type Survey = Schema['SurveyPurposePipelineResponse'];
const surveyColumns = tableColumns<Survey>({
  key: 'purpose', title: 'Survey purpose',
  value: (row) => [row.purposeName ?? row.purposeCode, row.followupOffsetMonths != null ? `${row.followupOffsetMonths} months` : ''].filter(Boolean).join(' · '),
  sortable: true,
}, [
  ['responseRowCount', 'Response opportunities'], ['submittedCount', 'Submitted'],
  ['pendingCount', 'Pending'], ['submissionRatePct', 'Submission rate', 'percent'],
]);

type Verification = Schema['VerificationStatusBreakdownResponse'];
const verificationColumns = tableColumns<Verification>({
  key: 'status', title: 'Recorded outcome', value: (row) => row.statusName ?? row.statusCode, sortable: true,
}, [
  ['verificationCount', 'Verification records'], ['traineeCount', 'Candidates'], ['employmentCount', 'Employment spells'],
]);

type Method = Schema['VerificationMethodEfficacyResponse'];
const methodColumns = tableColumns<Method>({ key: 'method', title: 'Method', value: (row) => row.methodName ?? row.methodCode, sortable: true }, [
  ['currentVerificationCount', 'Current records'], ['verifiedCount', 'Verified'], ['pendingCount', 'Pending'],
  ['rejectedCount', 'Rejected'], ['successRatePct', 'Success rate', 'percent'],
]);

export function OutreachPanels({ followups, surveys, verifications }: Outreach) {
  return (
    <>
      <QueryTable code="Q19" title="Follow-up success by wave" rows={followups.wavePerformance ?? []}
        columns={waveColumns} rowKey={(row) => row.typeCode} sort="taskCount" searchLabel="Search follow-up waves" />
      <QueryTable code="Q20" title="Survey response pipeline" rows={surveys.purposePipeline ?? []}
        columns={surveyColumns} rowKey={(row) => `${row.purposeCode}-${row.followupOffsetMonths}`} sort="responseRowCount" searchLabel="Search survey purposes" />
      <QueryTable code="Q21" title="Verification outcomes across all cycles" rows={verifications.statusBreakdown ?? []}
        columns={verificationColumns} rowKey={(row) => row.statusCode} sort="verificationCount" searchLabel="Search verification outcomes" />
      <QueryTable code="Q22" title="Current verification by method" rows={verifications.methodEfficacyBreakdown ?? []}
        columns={methodColumns} rowKey={(row) => row.methodCode} sort="currentVerificationCount" searchLabel="Search verification methods" />
    </>
  );
}
