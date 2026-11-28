import { formatAssessmentStart, type BatchAssessmentCandidate } from '@/api/batchAssessmentApi';

const labels: Record<string, string> = { PRESENT: 'Present', ABSENT: 'Absent', LATE: 'Late', EXCUSED: 'Excused' };

export function TrainingAttendance({ candidate }: { candidate: BatchAssessmentCandidate }) {
  const summary = `${candidate.present} / ${candidate.trainingSessions} sessions present`;
  if (!candidate.sessionMarks?.length) return <span>{summary}</span>;

  return <details className="space-y-2">
    <summary className="cursor-pointer font-semibold text-navy">{summary}</summary>
    <ul className="space-y-2 text-left text-xs font-normal text-slate-700">
      {candidate.sessionMarks.map((mark) => (
        <li key={mark.startsAt}>
          <p>{formatAssessmentStart(mark.startsAt)} to {mark.startsAt.slice(0, 10) === mark.endsAt.slice(0, 10)
            ? mark.endsAt.slice(11, 16) : formatAssessmentStart(mark.endsAt)}</p>
          <p><strong>{mark.status ? labels[mark.status] ?? mark.status : 'Unmarked'}</strong>{mark.remarks && ` · ${mark.remarks}`}</p>
        </li>
      ))}
    </ul>
  </details>;
}
