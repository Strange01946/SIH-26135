import { useMutation } from '@tanstack/react-query';
import {
  downloadAssessmentDocument, formatAssessmentStart, type BatchAssessmentAppointment,
} from '@/api/batchAssessmentApi';

export function BatchAssessmentSummary({ appointment }: { appointment: BatchAssessmentAppointment }) {
  return <div className="space-y-1 text-sm text-gray-700">
    <p><strong>{appointment.title}</strong> · {appointment.batchCode}</p>
    <p>{appointment.status !== 'OPEN' && appointment.startsAt && appointment.venue
      ? `${formatAssessmentStart(appointment.startsAt)} · ${appointment.venue}`
      : 'Assessment date and venue pending'}</p>
    {appointment.assessor && <p>Assessor: {appointment.assessor}</p>}
    <p className="whitespace-pre-wrap break-words">{appointment.brief}</p>
  </div>;
}

export function AssessmentDocumentButton({ resultId, certificate }: { resultId: number; certificate?: boolean }) {
  const download = useMutation({ mutationFn: () => downloadAssessmentDocument(resultId) });
  return <div>
    <button type="button" className="btn-outline text-xs" disabled={download.isPending} onClick={() => download.mutate()}>
      {download.isPending ? 'Downloading…' : certificate ? 'Download certificate PDF' : 'Download result PDF'}
    </button>
    {download.isError && <p role="alert" className="text-red-800 text-xs mt-1">{download.error.message}</p>}
  </div>;
}
