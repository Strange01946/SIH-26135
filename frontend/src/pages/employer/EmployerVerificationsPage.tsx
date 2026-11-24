import { useState, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  downloadEmployerEvidence, getEmployerVerifications, submitEmployerVerdict,
  type EmployerVerification, type VerificationDecision, type VerificationInput,
} from '@/api/employerApi';
import { Modal } from '@/components/common/Modal';
import { EmptyState } from '@/components/feedback/EmptyState';
import { ErrorState } from '@/components/feedback/ErrorState';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { EmployerFormError, EmployerPageHeading, EmployerPager, EmployerSelect, formatPay, indiaToday, showStatus, useEmployerReferences } from './EmployerUi';

const decisions: { value: VerificationDecision; label: string }[] = [
  { value: 'ATTEST', label: 'Attest employment' },
  { value: 'DISPUTE', label: 'Dispute claim' },
  { value: 'REPORT_EXIT', label: 'Report exit' },
];

export function EmployerVerificationsPage() {
  const [view, setView] = useState<'pending' | 'history'>('pending');
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState<EmployerVerification | null>(null);
  const [receipt, setReceipt] = useState('');
  const [decision, setDecision] = useState<VerificationDecision | ''>('');
  const [evidence, setEvidence] = useState<File>();
  const [evidenceError, setEvidenceError] = useState('');
  const queryClient = useQueryClient();
  const references = useEmployerReferences();
  const cases = useQuery({
    queryKey: ['employer', 'verifications', view, page],
    queryFn: ({ signal }) => getEmployerVerifications(view, page, signal),
    refetchOnMount: 'always',
    refetchOnWindowFocus: true,
  });
  const submit = useMutation({
    mutationFn: ({ id, verdict }: { id: number; verdict: VerificationInput }) => submitEmployerVerdict(id, verdict, evidence),
    onSuccess: async (result) => {
      setReceipt(`${result.number} · ${showStatus(result.verdict)} · Request ${showStatus(result.requestStatus).toLowerCase()}`);
      setSelected(null);
      setEvidence(undefined);
      setView('history');
      setPage(0);
      await queryClient.invalidateQueries({ queryKey: ['employer', 'verifications'] });
    },
  });
  const download = useMutation({
    mutationFn: async (record: EmployerVerification) => {
      if (!record.evidenceId) return;
      const blob = await downloadEmployerEvidence(record.id, record.evidenceId);
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = record.evidenceName || (blob.type === 'image/jpeg' ? 'salary-proof.jpg' : 'salary-proof.pdf');
      document.body.appendChild(link);
      link.click();
      link.remove();
      setTimeout(() => URL.revokeObjectURL(url), 1000);
    },
  });

  function open(record: EmployerVerification) {
    setDecision('');
    setEvidence(undefined);
    setEvidenceError('');
    submit.reset();
    setSelected(record);
  }

  function respond(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selected || !decision || evidenceError || submit.isPending) return;
    const form = new FormData(event.currentTarget);
    const notes = String(form.get('notes') ?? '').trim() || undefined;
    const verdict: VerificationInput = { decision, notes };
    if (decision === 'DISPUTE') verdict.rejectionReasonId = Number(form.get('rejectionReasonId'));
    if (decision === 'REPORT_EXIT') {
      verdict.exitDate = String(form.get('exitDate'));
      verdict.exitReasonId = Number(form.get('exitReasonId'));
      verdict.separationNatureId = Number(form.get('separationNatureId'));
    }
    submit.mutate({ id: selected.id, verdict });
  }

  return (
    <div className="space-y-5">
      <EmployerPageHeading title="Employment verification">
        <button type="button" className="btn-outline" disabled={cases.isFetching} onClick={() => void cases.refetch()}>Refresh requests</button>
      </EmployerPageHeading>
      <div className="flex flex-wrap gap-2" aria-label="Verification views" role="group">
        {(['pending', 'history'] as const).map((tab) => (
          <button key={tab} type="button" aria-pressed={tab === view} className={tab === view ? 'btn-primary' : 'btn-outline'}
            onClick={() => { setView(tab); setPage(0); }}>{tab === 'pending' ? 'Pending requests' : 'Request history'}</button>
        ))}
      </div>
      {receipt && <p role="status" className="rounded border border-emerald-200 bg-emerald-50 p-3 text-sm text-emerald-900">{receipt}</p>}
      {download.isError && <EmployerFormError error={download.error} />}
      {cases.isPending ? <LoadingSpinner label="Loading verification requests" /> : cases.isError ? (
        <ErrorState error={cases.error} title="Unable to load verification requests" onRetry={() => void cases.refetch()} />
      ) : cases.data.content.length === 0 ? (
        <EmptyState title={view === 'pending' ? 'No open requests' : 'No verification requests yet'}
          description="Requests appear when the verification workflow assigns an employment record to your organisation." />
      ) : (
        <>
          <ul className="grid gap-4 lg:grid-cols-2">
            {cases.data.content.map((record) => (
              <li key={record.id} className="card space-y-3">
                <div className="flex flex-wrap items-start justify-between gap-2">
                  <div>
                    <h2 className="text-lg font-semibold text-navy">{record.candidateName}</h2>
                    <p className="text-sm text-slate-600">{record.jobRole || 'Employment record'} · {record.number}</p>
                  </div>
                  <span className="badge-info">{showStatus(record.requestStatus)}</span>
                </div>
                <dl className="grid grid-cols-2 gap-3 text-sm">
                  <div><dt className="text-slate-600">Employment period</dt><dd>{record.startedOn} – {record.endedOn || 'Current'}</dd></div>
                  <div><dt className="text-slate-600">Starting pay{record.payFrequency && ` · ${record.payFrequency}`}</dt>
                    <dd>{formatPay(record.startingPay, record.currency)}</dd></div>
                  <div><dt className="text-slate-600">Due date</dt><dd>{record.dueAt?.slice(0, 10) || '—'}</dd></div>
                  <div><dt className="text-slate-600">{record.verdict ? 'Recorded verdict' : 'Employment verification status'}</dt>
                    <dd>{showStatus(record.verdict || record.employmentVerificationStatus)}</dd></div>
                </dl>
                <div className="flex flex-wrap gap-2">
                  {record.canRespond && <button type="button" className="btn-primary" onClick={() => open(record)}>Respond to request</button>}
                  {record.evidenceId && <button type="button" className="btn-outline" onClick={() => download.mutate(record)} disabled={download.isPending}>Download evidence</button>}
                </div>
              </li>
            ))}
          </ul>
          <EmployerPager data={cases.data} onPage={setPage} />
        </>
      )}
      <Modal open={selected !== null} onClose={() => { if (!submit.isPending) setSelected(null); }} title="Respond to verification request">
        {selected && (references.isPending ? <LoadingSpinner label="Loading verification options" /> : references.isError ? (
          <ErrorState error={references.error} title="Unable to load verification options" onRetry={() => void references.refetch()} />
        ) : (
          <form onSubmit={respond} className="space-y-4 p-6">
            <p className="text-sm text-slate-600">{selected.candidateName} · {selected.employmentNumber}</p>
            <fieldset disabled={submit.isPending} className="space-y-3">
              <legend className="mb-1 font-semibold text-slate-800">Employer response</legend>
              {decisions.filter(({ value }) => value !== 'REPORT_EXIT' || selected.canReportExit).map(({ value, label }) => (
                <label className="flex items-center gap-2 text-sm" key={value}>
                  <input type="radio" name="decision" value={value} checked={decision === value} onChange={() => setDecision(value)} required />
                  {label}
                </label>
              ))}
              {decision === 'DISPUTE' && <EmployerSelect label="Dispute reason" name="rejectionReasonId" options={references.data.rejectionReasons} required />}
              {decision === 'REPORT_EXIT' && (
                <div className="space-y-3 rounded border border-slate-200 p-3">
                  <label className="block space-y-1 text-sm font-medium"><span>Separation date</span>
                    <input className="form-input" type="date" name="exitDate" min={selected.startedOn} max={indiaToday()} required /></label>
                  <EmployerSelect label="Separation type" name="separationNatureId" options={references.data.separationNatures} required />
                  <EmployerSelect label="Exit reason" name="exitReasonId" options={references.data.exitReasons} required />
                </div>
              )}
              <label className="block space-y-1 text-sm font-medium">
                <span>Notes (optional)</span><textarea className="form-input" name="notes" rows={3} maxLength={500} />
              </label>
              <label className="block space-y-1 text-sm font-medium">
                <span>Payslip or salary proof (optional)</span>
                <input className="form-input" type="file" accept=".pdf,.jpg,.jpeg,application/pdf,image/jpeg"
                  aria-describedby="evidence-file-note" onChange={(event) => {
                    const file = event.target.files?.[0];
                    const invalid = file && (!/\.(pdf|jpe?g)$/i.test(file.name) || !file.size || file.size > 5 * 1024 * 1024);
                    setEvidence(invalid ? undefined : file);
                    setEvidenceError(invalid ? 'Choose a PDF or JPEG file up to 5 MB.' : '');
                  }} />
                <span id="evidence-file-note" className="block text-xs font-normal text-slate-600">PDF or JPEG, up to 5 MB.</span>
              </label>
              {evidenceError && <p role="alert" className="text-sm text-red-800">{evidenceError}</p>}
            </fieldset>
            {submit.isError && <EmployerFormError error={submit.error} />}
            <button type="submit" className="btn-primary" disabled={!decision || !!evidenceError || submit.isPending}>
              {submit.isPending ? 'Submitting…' : 'Submit response'}
            </button>
          </form>
        ))}
      </Modal>
    </div>
  );
}
