import { useState, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import {
  batchAttendanceKey, getAttendanceTimetable, getSessionRoster, saveSessionRoster,
  type AttendanceRosterRow, type AttendanceSessionRoster, type SessionAttendanceMark,
} from '@/api/batchSessionAttendanceApi';
import type { ApiError } from '@/api/types';
import { updateTrainingBatchStatus } from '@/api/providerPortalApi';
import { careerDate } from '@/components/career/CareerInsights';
import { ErrorState } from '@/components/feedback/ErrorState';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { ProviderBatchRoster } from './ProviderBatchRoster';

type SessionDraft = Record<number, SessionAttendanceMark>;
const markLabels: Record<string, string> = { PRESENT: 'Present', ABSENT: 'Absent', LATE: 'Late', EXCUSED: 'Excused' };

export function ProviderBatchAttendancePage() {
  const id = Number(useParams().batchId);

  if (!Number.isSafeInteger(id) || id < 1) return <p className="card p-5">Choose a batch from Training Batches.</p>;
  return <BatchAttendance key={id} batchId={id} />;
}

function BatchAttendance({ batchId }: { batchId: number }) {
  const client = useQueryClient();
  const [chosenSession, setChosenSession] = useState<number>();
  const [drafts, setDrafts] = useState<Record<number, SessionDraft>>({});
  const [lockConfirmed, setLockConfirmed] = useState(false);
  const key = batchAttendanceKey(batchId);
  const timetable = useQuery({
    queryKey: [...key, 'timetable'],
    queryFn: ({ signal }) => getAttendanceTimetable(batchId, signal),
    retry: false, refetchInterval: 15_000, refetchOnWindowFocus: 'always',
  });
  const sessions = timetable.data?.sessions ?? [];
  const session = sessions.find(item => item.id === chosenSession) ?? sessions.find(item => item.canMark) ?? sessions[0];
  const sessionId = session?.id;
  const roster = useQuery({
    queryKey: [...key, 'roster', sessionId],
    queryFn: ({ signal }) => getSessionRoster(batchId, sessionId!, signal),
    enabled: Boolean(sessionId), retry: false, refetchInterval: 15_000, refetchOnWindowFocus: 'always',
  });
  const start = useMutation({
    mutationFn: () => updateTrainingBatchStatus(batchId, { batchStatusId: 3 }),
    onSuccess: async () => {
      await Promise.all([client.invalidateQueries({ queryKey: key }), client.invalidateQueries({ queryKey: ['provider', 'batches'] })]);
    },
  });
  const save = useMutation<AttendanceSessionRoster, ApiError, { sessionId: number; marks: SessionAttendanceMark[] }>({
    mutationFn: input => saveSessionRoster(batchId, input.sessionId, input.marks),
    onSuccess: async (data, input) => {
      const rosterKey = [...key, 'roster', input.sessionId] as const;
      await client.cancelQueries({ queryKey: rosterKey });
      client.setQueryData(rosterKey, data);
      setDrafts(previous => ({ ...previous, [input.sessionId]: {} }));
      await client.invalidateQueries({ queryKey: ['trainee', 'attendance'] });
    },
    onError: async (error) => {
      if (error.code === 'ASSESSMENT_ATTENDANCE_LOCKED') {
        setLockConfirmed(true);
        setDrafts({});
        await client.invalidateQueries({ queryKey: key });
      }
    },
  });
  const attendanceLocked = timetable.data?.attendanceLocked || lockConfirmed;
  const canMark = !attendanceLocked && session?.canMark && roster.data?.session.canMark;
  const draft = canMark && session ? drafts[session.id] ?? {} : {};
  const changes = Object.values(draft);

  function edit(row: AttendanceRosterRow, status: string, remarks: string) {
    if (!session || !canMark) return;
    const sessionId = session.id;
    setDrafts(previous => {
      const next = { ...previous[sessionId] };
      const prior = next[row.enrollmentId];
      const expectedVersion = prior ? prior.expectedVersion : row.version;
      if (!status || (status === row.status && remarks === (row.remarks ?? '') && expectedVersion === row.version)) {
        delete next[row.enrollmentId];
      } else {
        next[row.enrollmentId] = { enrollmentId: row.enrollmentId, status, remarks, expectedVersion };
      }
      return { ...previous, [sessionId]: next };
    });
    if (save.isSuccess) save.reset();
  }

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (session && canMark && changes.length && !save.isPending) save.mutate({ sessionId: session.id, marks: changes });
  }

  return (
    <div className="space-y-5">
      <Link to="/provider/batches" className="text-sm text-navy underline">Training Batches</Link>
      {timetable.isPending ? <LoadingSpinner label="Loading the batch timetable" /> : timetable.isError ? (
        <ErrorState title="Could not load batch attendance" error={timetable.error} onRetry={() => void timetable.refetch()} />
      ) : (
        <>
          <header className="card flex flex-wrap items-center justify-between gap-4 p-5">
            <div>
              <p className="text-sm text-slate-600">{timetable.data.batchCode}</p>
              <h1 className="page-title">{timetable.data.title}</h1>
            </div>
            {timetable.data.batchStatus === 'OPEN' && (
              <button type="button" className="btn-primary" disabled={start.isPending || !sessions.some(item => item.started)}
                onClick={() => start.mutate()}>Start training</button>
            )}
            {start.isError && <p role="alert" className="text-sm text-red-800">{start.error.message}</p>}
          </header>
          <section className="card space-y-4 p-5" aria-labelledby="session-attendance">
            <h2 id="session-attendance" className="section-title">Session attendance</h2>
            {attendanceLocked && <p role="status" className="text-sm text-slate-700">
              Attendance is read-only because this batch is open for assessment.
            </p>}
            {sessions.length === 0 ? <p className="text-sm text-slate-600">This batch has no approved sessions on its timetable.</p> : (
              <>
                <div className="max-w-lg">
                  <label htmlFor="attendance-session-picker" className="form-label">Scheduled session (IST)</label>
                  <select id="attendance-session-picker" className="form-input" value={sessionId} disabled={save.isPending}
                    onChange={event => { setChosenSession(Number(event.target.value)); save.reset(); }}>
                    {sessions.map(item => {
                      const sameDay = item.startsAt.slice(0, 10) === item.endsAt.slice(0, 10);
                      const time = `${item.startsAt.slice(11, 16)}–${sameDay ? '' : careerDate(item.endsAt.slice(0, 10)) + ', '}${item.endsAt.slice(11, 16)}`;
                      return <option key={item.id} value={item.id}>{careerDate(item.startsAt.slice(0, 10))} · {time}</option>;
                    })}
                  </select>
                </div>
                {!attendanceLocked && session && !session.canMark && <p className="text-sm text-slate-600">
                  {!session.started ? 'Attendance opens at the scheduled start time after training begins.'
                    : timetable.data.batchStatus === 'CANCELLED' ? 'This cancelled batch is available for review.'
                    : 'Start training to mark attendance for this session.'}
                </p>}
                {roster.isPending ? <LoadingSpinner label="Loading session roster" /> : roster.isError ? (
                  <ErrorState title="Could not load the session roster" error={roster.error} onRetry={() => void roster.refetch()} />
                ) : roster.data.rows.length === 0 ? <p className="text-sm text-slate-600">No trainees are enrolled for this session.</p> : (
                  <form className="space-y-4" onSubmit={submit}>
                    <div className="overflow-x-auto">
                      <table className="w-full min-w-[36rem] text-left text-sm" aria-label="Session trainee roster">
                        <thead className="border-b border-slate-200">
                          <tr><th scope="col" className="p-2">Trainee</th><th scope="col" className="p-2">Attendance</th>
                            <th scope="col" className="p-2">Remark (optional)</th></tr>
                        </thead>
                        <tbody className="divide-y divide-slate-200">
                          {roster.data.rows.map(row => {
                            const status = draft[row.enrollmentId]?.status ?? row.status ?? '';
                            const remarks = draft[row.enrollmentId]?.remarks ?? row.remarks ?? '';
                            const disabled = !canMark || !row.eligible || save.isPending;
                            return <tr key={row.enrollmentId}>
                              <th scope="row" className="p-2 font-semibold">{row.name}
                                <span className="block font-normal text-slate-600">{row.registrationNumber}</span>
                              </th>
                              <td className="p-2">
                                <select className="form-input" aria-label={`Attendance for ${row.name} (${row.registrationNumber})`}
                                  value={status} disabled={disabled} onChange={event => edit(row, event.target.value, remarks)}>
                                  {!row.status && <option value="">Unmarked</option>}
                                  {row.status && !['PRESENT', 'ABSENT'].includes(row.status) &&
                                    <option value={row.status}>{markLabels[row.status] ?? row.status}</option>}
                                  <option value="PRESENT">Present</option><option value="ABSENT">Absent</option>
                                </select>
                              </td>
                              <td className="p-2">
                                <input className="form-input" aria-label={`Remark for ${row.name} (${row.registrationNumber})`}
                                  maxLength={255} value={remarks} disabled={disabled || !status}
                                  onChange={event => edit(row, status, event.target.value)} />
                              </td>
                            </tr>;
                          })}
                        </tbody>
                      </table>
                    </div>
                    {canMark && <div className="flex flex-wrap items-center gap-3">
                      <button className="btn-primary" type="submit" disabled={!changes.length || save.isPending}>
                        {save.isPending ? 'Saving…' : 'Save attendance'}
                      </button>
                      <span className="text-sm text-slate-600">{changes.length ? `Unsaved marks (${changes.length})` : 'Unmarked trainees stay unrecorded.'}</span>
                    </div>}
                    {save.isSuccess && <p role="status" className="text-sm text-emerald-800">Attendance saved.</p>}
                    {save.isError && <div role="alert" className="space-y-2 text-sm text-red-800">
                      <p>{save.error.message}</p>
                      {['ATTENDANCE_CHANGED', 'INVALID_ATTENDANCE_ENROLLMENT'].includes(save.error.code ?? '') && (
                        <button type="button" className="btn-secondary" onClick={async () => {
                          const result = await roster.refetch();
                          if (result.isSuccess && sessionId) {
                            setDrafts(previous => ({ ...previous, [sessionId]: {} }));
                            save.reset();
                          }
                        }}>Reload saved marks</button>
                      )}
                    </div>}
                  </form>
                )}
              </>
            )}
          </section>
          <ProviderBatchRoster batchId={batchId} />
        </>
      )}
    </div>
  );
}
