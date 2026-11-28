import type { FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import {
  getBatchAssessmentReadiness, getProviderAssessmentReview, publishBatchAssessment,
  type BatchAssessmentReview,
} from '@/api/batchAssessmentApi';
import { batchAttendanceKey } from '@/api/batchSessionAttendanceApi';
import { AssessmentDocumentButton, BatchAssessmentSummary } from '@/components/assessment/BatchAssessmentSummary';
import { ErrorState } from '@/components/feedback/ErrorState';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';

export function ProviderBatchAssessmentPage() {
  const id = Number(useParams().batchId);
  const queryClient = useQueryClient();
  const readinessKey = ['batch-assessment', id];
  const reviewKey = ['batch-assessment', id, 'roster'];
  const readiness = useQuery({
    queryKey: readinessKey, queryFn: () => getBatchAssessmentReadiness(id),
    refetchInterval: 10_000,
  });
  const review = useQuery({
    queryKey: reviewKey, queryFn: () => getProviderAssessmentReview(id), enabled: !!readiness.data?.publication,
    refetchInterval: 10_000,
  });
  const publish = useMutation({
    mutationFn: (form: FormData) => publishBatchAssessment(id, {
      brief: String(form.get('brief')),
    }),
    onSuccess: () => Promise.all([
      queryClient.invalidateQueries({ queryKey: readinessKey }),
      queryClient.invalidateQueries({ queryKey: batchAttendanceKey(id) }),
    ]),
  });

  return <main className="space-y-5">
    <section className="card p-6 space-y-3">
      <Link to="/provider/batches" className="text-sm text-navy underline">Training batches</Link>
      <h1 className="text-2xl font-bold text-navy">Batch assessment</h1>

      {readiness.isLoading && <LoadingSpinner label="Loading assessment readiness" />}
      {readiness.isError && <ErrorState error={readiness.error} title="Could not load assessment readiness" onRetry={() => void readiness.refetch()} />}
      {readiness.data && <p className="text-sm text-gray-700">{readiness.data.batchCode} · {readiness.data.candidates} registered candidates · {readiness.data.sessions} training sessions</p>}
      {readiness.data?.publication && <Link to={`/provider/batches/${id}/attendance`} className="text-sm text-navy underline">View locked training attendance</Link>}
      {readiness.data && !readiness.data.publication && <>
        {readiness.data.blocker && <p className="text-amber-900" role="status">{readiness.data.blocker}</p>}
        <Link to={`/provider/batches/${id}/attendance`} className="text-sm text-navy underline">Review training attendance</Link>
        <p className="text-sm text-gray-700">Opening the batch locks training attendance. The assessor sets the date and venue when they claim the batch.</p>
        <form className="space-y-3" onSubmit={(event: FormEvent<HTMLFormElement>) => {
          event.preventDefault();
          publish.mutate(new FormData(event.currentTarget));
        }}>
          <fieldset disabled={!readiness.data.canPublish || publish.isPending} className="space-y-3 disabled:opacity-70">
            <label className="block max-w-xl">Assessment instructions
              <textarea className="input-field" name="brief" required maxLength={1200} rows={3} />
            </label>
            <button className="btn-primary" type="submit">{publish.isPending ? 'Opening…' : 'Open for assessment'}</button>
          </fieldset>
        </form>
        {publish.isError && <p role="alert" className="text-red-800">{publish.error.message}</p>}
      </>}
    </section>
    {review.isLoading && <LoadingSpinner label="Loading assessment roster" />}
    {review.isError && <ErrorState error={review.error} title="Could not load assessment roster" onRetry={() => void review.refetch()} />}
    {review.data && <AssessmentRoster review={review.data} />}
  </main>;
}

function AssessmentRoster({ review }: { review: BatchAssessmentReview }) {
  const { publication, candidates } = review;
  const status = {
    OPEN: 'Open to assessors',
    CLAIMED: publication.completedAt ? 'Assessment completed' : 'Assessor assigned',
    FINALIZED: 'Results published',
  }[publication.status];

  return <section className="card p-6 space-y-4">
    <h2 className="text-xl font-semibold">{status}</h2>
    <BatchAssessmentSummary appointment={publication} />
    <h3 className="font-semibold">Training attendance and assessment results</h3>
    <div className="overflow-x-auto">
      <table className="w-full text-left text-sm divide-y divide-gray-200">
        <thead><tr>
          {['Candidate', 'Training sessions attended', 'Theory / Practical / Viva', 'Total', 'Outcome', 'Document'].map((heading) => <th key={heading} scope="col" className="p-2">{heading}</th>)}
        </tr></thead>
        <tbody className="divide-y divide-gray-200">{candidates.map((candidate) => <tr key={candidate.traineeId}>
          <th scope="row" className="p-2 font-medium">{candidate.name}<br /><span className="text-xs text-gray-600">{candidate.registrationNumber}</span></th>
          <td className="p-2">{candidate.present} / {candidate.trainingSessions}</td>
          <td className="p-2">{candidate.theory == null ? '—' : `${candidate.theory} / ${candidate.practical} / ${candidate.viva}`}</td>
          <td className="p-2">{candidate.total ?? '—'}</td>
          <td className="p-2">{candidate.outcome ?? '—'}</td>
          <td className="p-2">{candidate.resultId && <AssessmentDocumentButton resultId={candidate.resultId} certificate={!!candidate.certificateId} />}</td>
        </tr>)}</tbody>
      </table>
    </div>
  </section>;
}
