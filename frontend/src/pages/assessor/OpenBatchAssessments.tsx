import { useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { claimBatchAssessment, getOpenBatchAssessments, getPublishedBatch } from '@/api/batchAssessmentApi';
import { BatchAssessmentSummary } from '@/components/assessment/BatchAssessmentSummary';
import { TrainingAttendance } from '@/components/assessment/TrainingAttendance';
import { Modal } from '@/components/common/Modal';
import { ErrorState } from '@/components/feedback/ErrorState';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';

export function OpenBatchAssessments() {
  const [attendanceId, setAttendanceId] = useState<number>();
  const navigate = useNavigate();
  const queue = useQuery({ queryKey: ['open-batch-assessments'], queryFn: getOpenBatchAssessments, refetchInterval: 15_000 });
  const claim = useMutation({
    mutationFn: claimBatchAssessment,
    onSuccess: (assignment) => navigate(`/assessor/assessments/${assignment.assessmentId}`),
    onSettled: () => void queue.refetch(),
  });
  return <section className="card p-5 space-y-3" aria-labelledby="open-assessments-title">
    <h2 id="open-assessments-title" className="text-xl font-semibold text-navy">Open batch assessments</h2>
    {queue.isLoading && <LoadingSpinner label="Loading available assessments" />}
    {queue.isError && <ErrorState title="Could not load open assessments" error={queue.error} onRetry={() => void queue.refetch()} />}
    {queue.data?.length === 0 && <p className="text-sm text-gray-700">No completed batches are open for assessment.</p>}
    {claim.isError && <p role="alert" className="text-red-800">{claim.error.message}</p>}
    <div className="grid gap-3 lg:grid-cols-2">{queue.data?.map((item) => <article key={item.assessmentId} className="rounded border p-4 space-y-2">
      <BatchAssessmentSummary appointment={item} />
      <p className="text-sm">{item.provider} · {item.candidates} registered candidates</p>
      <p className="text-sm">Training venue: {[item.trainingVenue, item.district, item.state].filter(Boolean).join(', ')}</p>
      {item.skills && <p className="text-sm">Skills taught: {item.skills}</p>}
      <button type="button" className="btn-outline" onClick={() => setAttendanceId(item.assessmentId)}>Review training attendance</button>
      <form className="space-y-2" onSubmit={(event) => {
        event.preventDefault();
        const form = new FormData(event.currentTarget);
        claim.mutate({ id: item.assessmentId, startsAt: String(form.get('startsAt')), venue: String(form.get('venue')) });
      }}>
        <label className="block max-w-sm text-sm">Assessment date and time (IST)
          <input className="input-field" type="datetime-local" name="startsAt" required disabled={claim.isPending} />
        </label>
        <label className="block max-w-sm text-sm">Assessment venue
          <input className="input-field" name="venue" required maxLength={200} disabled={claim.isPending} />
        </label>
        <button className="btn-primary" type="submit" disabled={claim.isPending}>Claim assessment</button>
      </form>
    </article>)}</div>
    {attendanceId != null && queue.data?.some(item => item.assessmentId === attendanceId) &&
      <AttendanceReview key={attendanceId} id={attendanceId} onClose={() => setAttendanceId(undefined)} />}
  </section>;
}

function AttendanceReview({ id, onClose }: { id: number; onClose: () => void }) {
  const review = useQuery({ queryKey: ['open-batch-assessment-attendance', id], queryFn: () => getPublishedBatch(id), retry: false });
  return <Modal open onClose={onClose} title="Locked training attendance">
    <div className="space-y-3 p-5">
      {review.isPending ? <LoadingSpinner label="Loading saved attendance" /> : review.isError ? (
        <ErrorState error={review.error} title="Could not load saved attendance" onRetry={() => void review.refetch()} />
      ) : (
        <ul className="divide-y divide-slate-200">
          {review.data.candidates.map(candidate => <li key={candidate.traineeId} className="space-y-1 py-3 text-sm">
            <p><strong>{candidate.name}</strong> · {candidate.registrationNumber}</p>
            <TrainingAttendance candidate={candidate} />
          </li>)}
        </ul>
      )}
    </div>
  </Modal>;
}
