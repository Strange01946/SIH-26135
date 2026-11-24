import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState, type FormEvent } from 'react';
import {
  confirmCareerUnemployment, enrollCareerRemediation, findCareerCandidates, getCareerExitOptions,
  getCareerHistory, getRemediationBatches, recordCareerExit,
  type CareerHistory, type EmploymentPeriod, type ExitInput,
} from '@/api/careerApi';
import { getTraineeSkillGapProfile } from '@/api/traineeApi';
import { careerDate, RetentionPanel, SkillGapInsights } from '@/components/career/CareerInsights';
import { Modal } from '@/components/common/Modal';
import { ErrorState } from '@/components/feedback/ErrorState';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { useAuth } from '@/context/AuthContext';
import type { TraineeSkillGapProfile } from '@/types/trainee';

type Recommendation = NonNullable<TraineeSkillGapProfile['recommendations']>[number];

export function AdminCareersPage() {
  const [search, setSearch] = useState('');
  const [term, setTerm] = useState('');
  const [traineeId, setTraineeId] = useState<number>();
  const results = useQuery({
    queryKey: ['career', 'search', term],
    queryFn: ({ signal }) => findCareerCandidates(term, signal),
    enabled: term.length >= 2,
    retry: false,
  });

  function submitSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (search.trim().length >= 2) {
      if (term === search.trim()) void results.refetch();
      setTerm(search.trim());
    }
  }

  return (
    <div className="space-y-5">
      <header>
        <h1 className="page-title">Career progression</h1>
        <p className="mt-1 text-sm text-slate-600">Track employment and wages, record exits, and arrange training for assessed skill deficits.</p>
      </header>
      <div className="grid items-start gap-5 xl:grid-cols-[minmax(16rem,20rem)_minmax(0,1fr)]">
        <aside className="card space-y-4 p-4" aria-label="Candidate search">
          <form onSubmit={submitSearch} className="space-y-2">
            <label className="form-label" htmlFor="career-candidate-search">Candidate name or registration number</label>
            <input id="career-candidate-search" className="form-input" value={search}
              onChange={(event) => setSearch(event.target.value)} minLength={2} maxLength={160} required />
            <button type="submit" className="btn-primary" disabled={search.trim().length < 2}>Search candidates</button>
          </form>
          {term && (results.isPending ? <LoadingSpinner label="Searching candidates" /> : results.isError ? (
            <ErrorState error={results.error} title="Unable to search candidates" onRetry={() => void results.refetch()} />
          ) : results.data.length === 0 ? (
            <p className="text-sm text-slate-600" role="status">No candidates match this search.</p>
          ) : (
            <>
              <ul className="divide-y divide-slate-200" aria-label="Matching candidates">
                {results.data.map((candidate) => (
                  <li key={candidate.id}>
                    <button type="button" aria-pressed={traineeId === candidate.id} onClick={() => setTraineeId(candidate.id)}
                      className="block w-full rounded p-2 text-left hover:bg-slate-50 aria-pressed:bg-navy-50 focus-visible:outline-2">
                      <span className="block text-sm font-semibold text-navy">{candidate.name}</span>
                      <span className="block text-xs text-slate-600">{candidate.registrationNumber}</span>
                    </button>
                  </li>
                ))}
              </ul>
              {results.data.length === 25 && <p className="text-xs text-slate-600">Showing 25 matches. Use the registration number to narrow the results.</p>}
            </>
          ))}
        </aside>
        {traineeId ? <CandidateWorkbench key={traineeId} traineeId={traineeId} /> : (
          <p className="rounded-lg border border-dashed border-slate-300 p-6 text-sm text-slate-600">Select a candidate to open their career record.</p>
        )}
      </div>
    </div>
  );
}

function CandidateWorkbench({ traineeId }: { traineeId: number }) {
  const [employmentId, setEmploymentId] = useState<number>();
  const [exitMode, setExitMode] = useState<'exit' | 'unemployment'>();
  const [notice, setNotice] = useState('');
  const queryClient = useQueryClient();
  const { hasAuthority } = useAuth();
  const canManageRemediation = hasAuthority('assessment.manage') || hasAuthority('enrollment.manage');
  const history = useQuery({
    queryKey: ['career', 'history', traineeId],
    queryFn: ({ signal }) => getCareerHistory(traineeId, signal),
    retry: false,
  });

  if (history.isPending) return <LoadingSpinner label="Loading career history" />;
  if (history.isError) return <ErrorState error={history.error} title="Unable to load career history" onRetry={() => void history.refetch()} />;

  const { candidate, employments, exitEvents, unemploymentEvents } = history.data;
  const employment = employments.find((item) => item.id === employmentId) ?? employments[0];
  const recordedExit = exitEvents.find((event) => event.employmentId === employment?.id);
  const canLogUnemployment = recordedExit?.separationDate && employment
    && unemploymentCanStart(history.data, employment, recordedExit.separationDate);

  function afterExit(message: string) {
    setExitMode(undefined);
    setNotice(message);
    void queryClient.invalidateQueries({ queryKey: ['career', 'history', traineeId] });
    void queryClient.invalidateQueries({ queryKey: ['career', 'retention', employment?.id] });
  }

  return (
    <div className="min-w-0 space-y-5">
      <header className="card flex flex-wrap items-center justify-between gap-3 border-l-4 border-l-navy p-4">
        <div>
          <p className="text-xs font-medium text-slate-600">{candidate.registrationNumber}</p>
          <h2 className="text-xl font-semibold text-navy">{candidate.name}</h2>
        </div>
        <div className="text-sm">
          <p className="text-xs text-slate-600">Current employment status</p>
          <strong className={candidate.statusCode === 'UNEMPLOYED' ? 'badge-warning' : 'badge bg-navy-50 text-navy'}>{candidate.status}</strong>
        </div>
      </header>
      {notice && <p className="rounded-md bg-emerald-50 p-3 text-sm text-emerald-900" role="status">{notice}</p>}
      <section className="card space-y-4 p-4 sm:p-5" aria-labelledby="career-employment-heading">
        <h2 id="career-employment-heading" className="section-title">Employment and retention</h2>
        {!employment ? <p className="text-sm text-slate-600">No employment periods are linked to this candidate.</p> : (
          <>
            <div className="flex flex-wrap items-end gap-3">
              <div className="min-w-0 flex-1">
                <label className="form-label" htmlFor="career-employment">Employment period</label>
                <select id="career-employment" className="form-input" value={employment.id} onChange={(event) => setEmploymentId(Number(event.target.value))}>
                  {employments.map((item) => (
                    <option key={item.id} value={item.id}>{[item.role, item.employer, careerDate(item.startDate), item.number].filter(Boolean).join(' · ')}</option>
                  ))}
                </select>
              </div>
              {employment.canRecordExit && <button type="button" className="btn-secondary" onClick={() => setExitMode('exit')}>Record employment exit</button>}
              {canLogUnemployment && <button type="button" className="btn-secondary" onClick={() => setExitMode('unemployment')}>Confirm unemployment</button>}
            </div>
            <div className="flex flex-wrap gap-x-4 gap-y-1 text-sm text-slate-700">
              <span>{careerDate(employment.startDate)}{employment.endDate ? ` to ${careerDate(employment.endDate)}` : ''}</span>
              <strong>{employment.status}</strong>
              {recordedExit && <span>{recordedExit.separationNatureName} · {recordedExit.employmentExitReasonName}</span>}
            </div>
            <RetentionPanel employmentId={employment.id} />
          </>
        )}
      </section>
      <section className="card space-y-3 p-4 sm:p-5" aria-labelledby="career-unemployment-heading">
        <h2 id="career-unemployment-heading" className="section-title">Unemployment periods</h2>
        {unemploymentEvents.length === 0 ? <p className="text-sm text-slate-600">No unemployment periods have been recorded.</p> : (
          <ul className="space-y-2">
            {unemploymentEvents.map((event) => (
              <li key={event.id} className="flex flex-wrap justify-between gap-2 rounded-md border p-3 text-sm">
                <div>
                  <p className="font-semibold">{event.unemploymentReasonName}</p>
                  <p className="text-slate-600">{careerDate(event.startDate)}{event.endDate ? ` to ${careerDate(event.endDate)}` : ''}</p>
                </div>
                <div className="text-xs text-slate-600"><strong>{event.isCurrent ? 'Current' : 'Closed'}</strong> · {event.recordVerificationStatusName}</div>
              </li>
            ))}
          </ul>
        )}
      </section>
      {canManageRemediation && <RemediationWorkbench traineeId={traineeId} />}
      {employment && exitMode && (
        <ExitDialog employment={employment} mode={exitMode}
          unemploymentPossible={unemploymentCanStart(history.data, employment)}
          onClose={() => setExitMode(undefined)} onSaved={afterExit} />
      )}
    </div>
  );
}

function unemploymentCanStart(history: CareerHistory, employment: EmploymentPeriod, separationDate?: string) {
  return !history.unemploymentEvents.some((event) => event.isCurrent || event.precedingEmploymentId === employment.id
    || (separationDate && event.startDate && event.startDate >= separationDate))
    && !history.employments.some((item) => item.id !== employment.id
      && (!item.endDate || (separationDate && item.endDate > separationDate)));
}

function ExitDialog({ employment, mode, unemploymentPossible, onClose, onSaved }: {
  employment: EmploymentPeriod;
  mode: 'exit' | 'unemployment';
  unemploymentPossible: boolean;
  onClose: () => void;
  onSaved: (message: string) => void;
}) {
  const [confirmUnemployment, setConfirmUnemployment] = useState(unemploymentPossible);
  const [exitType, setExitType] = useState<ExitInput['exitType']>('VOLUNTARY');
  const existingExit = mode === 'unemployment';
  const wantsUnemployment = existingExit || confirmUnemployment;
  const options = useQuery({ queryKey: ['career', 'exit-options'], queryFn: ({ signal }) => getCareerExitOptions(signal), retry: false });
  const save = useMutation({
    mutationFn: async (data: FormData) => {
      const remarks = String(data.get('remarks') ?? '').trim() || undefined;
      const unemploymentReasonId = Number(data.get('unemploymentReasonId')) || undefined;
      if (existingExit && unemploymentReasonId) {
        await confirmCareerUnemployment(employment.id, unemploymentReasonId, remarks);
        return 'Unemployment confirmed for the recorded exit.';
      }
      const response = await recordCareerExit(employment.id, {
        separationDate: String(data.get('separationDate')),
        exitType,
        exitReasonId: Number(data.get('exitReasonId')) || undefined,
        confirmUnemployment,
        unemploymentReasonId,
        remarks,
      });
      return response.unemploymentEvent ? 'Employment exit recorded and unemployment started.' : 'Employment exit recorded.';
    },
    onSuccess: onSaved,
  });
  const today = new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Kolkata' }).format(new Date());
  return (
    <Modal open onClose={() => { if (!save.isPending) onClose(); }} title={existingExit ? 'Confirm unemployment' : 'Record employment exit'}>
      <form className="space-y-4 p-6" onSubmit={(event) => { event.preventDefault(); save.mutate(new FormData(event.currentTarget)); }}>
        <p className="text-sm text-slate-700">{[employment.role, employment.employer, employment.number].filter(Boolean).join(' · ')}</p>
        {options.isPending && <LoadingSpinner label="Loading exit options" />}
        {options.isError && <ErrorState error={options.error} title="Unable to load exit options" onRetry={() => void options.refetch()} />}
        {options.data && (
          <>
            {!existingExit && (
              <>
                <label className="form-label">Separation date
                  <input name="separationDate" type="date" className="form-input" min={employment.startDate} max={today} required />
                </label>
                <label className="form-label">Exit type
                  <select name="exitType" className="form-input" value={exitType} onChange={(event) => setExitType(event.target.value as ExitInput['exitType'])}>
                    <option value="VOLUNTARY">Voluntary resignation</option>
                    <option value="CONTRACT_END">Contract end</option>
                    <option value="LAY_OFF">Lay-off</option>
                  </select>
                </label>
                {exitType === 'VOLUNTARY' && <label className="form-label">Exit reason
                  <select name="exitReasonId" className="form-input" defaultValue="">
                    <option value="">Other or unspecified</option>
                    {options.data.exitReasons.map((reason) => <option key={reason.id} value={reason.id}>{reason.name}</option>)}
                  </select>
                </label>}
                <label className="flex items-center gap-2 text-sm font-medium">
                  <input type="checkbox" checked={confirmUnemployment} disabled={!unemploymentPossible}
                    onChange={(event) => setConfirmUnemployment(event.target.checked)} />
                  Confirm candidate is unemployed after this exit
                </label>
                {!unemploymentPossible && <p className="text-xs text-slate-600">Review the candidate’s other employment and unemployment periods before confirming their status.</p>}
              </>
            )}
            {wantsUnemployment && <label className="form-label">Unemployment reason
              <select name="unemploymentReasonId" className="form-input" defaultValue="" required>
                <option value="">Select a reason</option>
                {options.data.unemploymentReasons.map((reason) => <option key={reason.id} value={reason.id}>{reason.name}</option>)}
              </select>
            </label>}
            <label className="form-label">Remarks (optional)
              <textarea name="remarks" className="form-input" rows={3} maxLength={500} />
            </label>
          </>
        )}
        {save.isError && <ErrorState error={save.error} title="Unable to save the career update" />}
        <div className="flex flex-wrap justify-end gap-2">
          <button type="button" className="btn-secondary" onClick={onClose} disabled={save.isPending}>Cancel</button>
          <button type="submit" className="btn-primary" disabled={!options.data || save.isPending}>
            {save.isPending ? 'Saving…' : existingExit ? 'Start unemployment period' : 'Save employment exit'}
          </button>
        </div>
      </form>
    </Modal>
  );
}

function RemediationWorkbench({ traineeId }: { traineeId: number }) {
  const [selected, setSelected] = useState<Recommendation>();
  const [enrollmentNumber, setEnrollmentNumber] = useState<string>();
  const queryClient = useQueryClient();
  const profileKey = ['trainee', 'skillgap', traineeId];
  const profile = useQuery({
    queryKey: profileKey,
    queryFn: () => getTraineeSkillGapProfile(traineeId),
    retry: false,
  });

  return (
    <section className="card space-y-4 p-4 sm:p-5" aria-labelledby="career-remediation-heading">
      <h2 id="career-remediation-heading" className="section-title">Skill gap remediation</h2>
      {enrollmentNumber && <p className="rounded-md bg-emerald-50 p-3 text-sm text-emerald-900" role="status">Remediation enrollment {enrollmentNumber} created.</p>}
      {profile.isPending ? <LoadingSpinner label="Loading skill gap profile" /> : profile.isError ? (
        <ErrorState error={profile.error} title="Unable to load skill gap profile" onRetry={() => void profile.refetch()} />
      ) : (
        <>
          <SkillGapInsights profile={profile.data} />
          <h3 className="font-bold text-navy">Personalized recommendations</h3>
          {!profile.data.recommendations?.length ? <p className="text-sm text-slate-600">There are no recommendations for this candidate’s recorded skill gaps.</p> : (
            <ul className="grid gap-3 md:grid-cols-2">
              {profile.data.recommendations.map((recommendation) => {
                const gap = profile.data.currentSkillGaps?.find((item) => item.id === recommendation.skillGapId);
                const inRemediation = gap?.skillGapStatusCode === 'IN_REMEDIATION';
                return (
                  <li key={recommendation.id} className="flex flex-col items-start gap-2 rounded-lg border border-slate-200 p-4">
                    <h4 className="font-semibold text-navy">{recommendation.recommendedCourseName || recommendation.skillGapActionTypeName || 'Skill recommendation'}</h4>
                    {(recommendation.recommendedSkillName || gap?.skillName) && <p className="text-sm text-slate-700">{recommendation.recommendedSkillName || gap?.skillName}</p>}
                    <p className={inRemediation ? 'badge-success' : 'badge bg-slate-100 text-slate-700'}>
                      {inRemediation ? 'Skill gap in remediation' : recommendation.isAcceptedFlag ? `Accepted ${careerDate(recommendation.acceptedOn)}` : 'Candidate acceptance required'}
                    </p>
                    {recommendation.remarks && <p className="text-sm text-slate-600">{recommendation.remarks}</p>}
                    {recommendation.isAcceptedFlag && recommendation.recommendedCourseId && gap?.isCurrent && gap.skillGapStatusCode === 'OPEN'
                      && <button type="button" className="btn-secondary mt-auto" onClick={() => setSelected(recommendation)}>Choose remediation batch</button>}
                  </li>
                );
              })}
            </ul>
          )}
        </>
      )}
      {selected && <EnrollDialog recommendation={selected} onClose={() => setSelected(undefined)} onSaved={(number) => {
        setSelected(undefined);
        setEnrollmentNumber(number);
        void queryClient.invalidateQueries({ queryKey: profileKey });
      }} />}
    </section>
  );
}

function EnrollDialog({ recommendation, onClose, onSaved }: {
  recommendation: Recommendation;
  onClose: () => void;
  onSaved: (enrollmentNumber: string) => void;
}) {
  const [batchId, setBatchId] = useState<number>();
  const batches = useQuery({
    queryKey: ['career', 'remediation-batches', recommendation.id],
    queryFn: ({ signal }) => getRemediationBatches(recommendation.id, signal),
    retry: false,
  });
  const save = useMutation({
    mutationFn: (id: number) => enrollCareerRemediation(recommendation.id, id),
    onSuccess: (result) => onSaved(result.createdEnrollment?.enrollmentNumber ?? ''),
    onError: () => { void batches.refetch(); },
  });
  const batch = batches.data?.find((item) => item.id === batchId);
  return (
    <Modal open title="Enroll in remediation course" onClose={() => { if (!save.isPending) onClose(); }}>
      <form className="space-y-4 p-6" onSubmit={(event) => { event.preventDefault(); if (batch) save.mutate(batch.id); }}>
        <p className="font-semibold text-navy">{recommendation.recommendedCourseName}</p>
        {batches.isPending ? <LoadingSpinner label="Loading training batches" /> : batches.isError ? (
          <ErrorState error={batches.error} title="Unable to load training batches" onRetry={() => void batches.refetch()} />
        ) : batches.data.length === 0 ? (
          <p className="text-sm text-slate-600">No open batch with an available seat was found for this course.</p>
        ) : (
          <label className="form-label">Training batch
            <select className="form-input" value={batch?.id ?? ''} onChange={(event) => setBatchId(Number(event.target.value))} required>
              <option value="">Select a batch</option>
              {batches.data.map((item) => <option key={item.id} value={item.id}>{item.code} · {item.center} · {careerDate(item.startsOn)}</option>)}
            </select>
          </label>
        )}
        {batch && <p className="text-sm text-slate-600">{batch.provider} · {batch.availableSeats} {batch.availableSeats === 1 ? 'seat' : 'seats'} available</p>}
        {save.isError && <ErrorState error={save.error} title="Unable to enroll candidate" />}
        <div className="flex justify-end gap-2">
          <button type="button" className="btn-secondary" onClick={onClose} disabled={save.isPending}>Cancel</button>
          <button type="submit" className="btn-primary" disabled={!batch || save.isPending}>{save.isPending ? 'Enrolling…' : 'Confirm enrollment'}</button>
        </div>
      </form>
    </Modal>
  );
}
