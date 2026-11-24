import { useState, type FormEvent } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { getBatchProgramOptions, getCourses } from '@/api/providerPortalApi';
import type { RegistrationReferenceData } from '@/api/registrationApi';
import type { ApiError } from '@/api/types';
import { requestVenue, rescheduleBooking, type BookingDetails, type SessionInput, type SharedVenue } from '@/api/venueBookingApi';
import { Modal } from '@/components/common/Modal';
import { StateDistrictFields } from '@/components/common/StateDistrictFields';

const formatDate = new Intl.DateTimeFormat('en-IN', {
  dateStyle: 'medium', timeStyle: 'short', timeZone: 'Asia/Kolkata',
});
export const indiaDate = (date = new Date()) => new Intl.DateTimeFormat('sv-SE', {
  year: 'numeric', month: '2-digit', day: '2-digit', timeZone: 'Asia/Kolkata',
}).format(date);
export const formatVenueTime = (time: string) => formatDate.format(new Date(time.slice(0, 19) + '+05:30'));
export const bookingError = (error?: ApiError | Error | null) =>
  (error && 'fieldErrors' in error ? error.fieldErrors?.[0]?.message : undefined) || error?.message || 'The request could not be saved.';

type SessionDraft = SessionInput & { key: number };
let nextDraftKey = 0;
const makeDraft = (startsAt = '', endsAt = ''): SessionDraft => ({ key: ++nextDraftKey, startsAt, endsAt });

function ScheduleFields({ sessions, onChange }: { sessions: SessionDraft[]; onChange: (sessions: SessionDraft[]) => void }) {
  const [first, setFirst] = useState('');
  const [last, setLast] = useState('');
  const [startTime, setStartTime] = useState('09:00');
  const [endTime, setEndTime] = useState('13:00');
  const [weekdays, setWeekdays] = useState(true);
  const [patternError, setPatternError] = useState('');

  function generate() {
    setPatternError('');
    if (!first || !last || first > last || !startTime || !endTime || startTime >= endTime) {
      setPatternError('Choose a date range and a finish time after the start time.');
      return;
    }
    const begin = new Date(first + 'T00:00:00Z');
    const end = new Date(last + 'T00:00:00Z');
    if ((end.getTime() - begin.getTime()) / 86_400_000 >= 366) {
      setPatternError('Generate at most 366 calendar days at a time.');
      return;
    }
    const requested = sessions.filter(({ startsAt, endsAt }) => startsAt || endsAt);
    const generated: SessionDraft[] = [];
    for (const day = begin; day <= end; day.setUTCDate(day.getUTCDate() + 1)) {
      if (!weekdays || (day.getUTCDay() > 0 && day.getUTCDay() < 6)) {
        const date = day.toISOString().slice(0, 10);
        generated.push(makeDraft(date + 'T' + startTime, date + 'T' + endTime));
      }
    }
    if (generated.length === 0 || generated.length + requested.length > 366) {
      setPatternError('Choose days that add between 1 and 366 sessions in total.');
      return;
    }
    onChange([...requested, ...generated]);
  }

  return (
    <fieldset className="space-y-3 rounded-md border border-slate-200 p-3">
      <legend className="px-1 font-semibold">Training sessions (IST)</legend>
      <details className="rounded bg-slate-50 p-3">
        <summary className="cursor-pointer font-medium text-navy">Generate sessions across dates</summary>
        <div className="mt-3 grid grid-cols-2 gap-2">
          <label className="form-label">First date<input className="form-input" type="date" value={first} onChange={e => setFirst(e.target.value)} /></label>
          <label className="form-label">Last date<input className="form-input" type="date" value={last} onChange={e => setLast(e.target.value)} /></label>
          <label className="form-label">Starts at<input className="form-input" type="time" value={startTime} onChange={e => setStartTime(e.target.value)} /></label>
          <label className="form-label">Ends at<input className="form-input" type="time" value={endTime} onChange={e => setEndTime(e.target.value)} /></label>
        </div>
        <label className="mt-2 flex gap-2"><input type="checkbox" checked={weekdays} onChange={e => setWeekdays(e.target.checked)} />Weekdays only</label>
        {patternError && <p role="alert" className="mt-2 text-sm text-red-700">{patternError}</p>}
        <button className="btn-outline mt-2" type="button" onClick={generate}>Generate sessions</button>
      </details>
      {sessions.map((session, index) => (
        <div key={session.key} className="grid items-end gap-2 sm:grid-cols-[1fr_1fr_auto]">
          <label className="form-label">Session {index + 1} start *
            <input className="form-input" type="datetime-local" required value={session.startsAt}
              onChange={e => onChange(sessions.map(row => row.key === session.key ? { ...row, startsAt: e.target.value } : row))} />
          </label>
          <label className="form-label">Session {index + 1} end *
            <input className="form-input" type="datetime-local" required value={session.endsAt}
              onChange={e => onChange(sessions.map(row => row.key === session.key ? { ...row, endsAt: e.target.value } : row))} />
          </label>
          <button type="button" className="btn-outline" disabled={sessions.length === 1} aria-label={`Remove session ${index + 1}`}
            onClick={() => onChange(sessions.filter(row => row.key !== session.key))}>Remove</button>
        </div>
      ))}
      <button type="button" className="btn-outline" disabled={sessions.length >= 366}
        onClick={() => onChange([...sessions, makeDraft()])}>Add session</button>
    </fieldset>
  );
}

export type BookingDraft = { kind: 'temporary' } | { kind: 'shared'; venue: SharedVenue } | { kind: 'reschedule'; id: number; name: string };

export function VenueBookingForm({ draft, geography, onClose, onSaved }: {
  draft: BookingDraft;
  geography: RegistrationReferenceData;
  onClose: () => void;
  onSaved: (detail: BookingDetails) => void;
}) {
  const [sessions, setSessions] = useState(() => [makeDraft()]);
  const [coursePage, setCoursePage] = useState(0);
  const [courseId, setCourseId] = useState(0);
  const [programId, setProgramId] = useState(0);
  const [batchCode, setBatchCode] = useState('');
  const [seats, setSeats] = useState('');
  const [temporary, setTemporary] = useState({ name: '', address: '', stateId: 0, districtId: 0, capacity: 0, permissionToUse: '' });
  const [validation, setValidation] = useState('');
  const courses = useQuery({ queryKey: ['venue-bookings', 'courses', coursePage],
    queryFn: () => getCourses({ page: coursePage, size: 25, sort: 'name', direction: 'ASC' }),
    enabled: draft.kind !== 'reschedule' });
  const programs = useQuery({ queryKey: ['provider', 'batch-programs', courseId],
    queryFn: ({ signal }) => getBatchProgramOptions(courseId, signal),
    enabled: draft.kind !== 'reschedule' && courseId > 0 });
  const save = useMutation<BookingDetails, ApiError>({
    mutationFn: () => {
      const slots = sessions.map(({ startsAt, endsAt }) => ({ startsAt, endsAt }));
      if (draft.kind === 'reschedule') return rescheduleBooking(draft.id, slots);
      return requestVenue({
        centerId: draft.kind === 'shared' ? draft.venue.id : undefined,
        temporaryVenue: draft.kind === 'temporary' ? temporary : undefined,
        batchCode: batchCode.trim(), courseId, programId, seats: Number(seats), sessions: slots,
      });
    },
    onSuccess: onSaved,
  });
  const title = draft.kind === 'reschedule' ? 'Postpone training'
    : draft.kind === 'temporary' ? 'Propose temporary location' : 'Request shared center';

  function submit(event: FormEvent) {
    event.preventDefault();
    if (save.isPending) return;
    if (draft.kind !== 'reschedule' && (!courseId || !programId || !batchCode.trim())) {
      setValidation('Enter a batch code and choose a course and program.');
      return;
    }
    if (draft.kind === 'temporary' && (!temporary.stateId || !temporary.districtId)) {
      setValidation('Choose a state or union territory and district.');
      return;
    }
    setValidation('');
    save.mutate();
  }

  return (
    <Modal open title={title} className="max-w-3xl" onClose={() => { if (!save.isPending) onClose(); }}>
      <form onSubmit={submit} className="space-y-4 p-5">
        <fieldset className="space-y-4" disabled={save.isPending}>
          {draft.kind === 'shared' && <p>{draft.venue.name} · {draft.venue.capacity} seats</p>}
          {draft.kind === 'reschedule' && <p className="text-sm text-slate-600">
            {draft.name}. Enter the replacement schedule for future sessions. Existing reservations remain held during review.
          </p>}
          {draft.kind === 'temporary' && (
            <div className="grid gap-3 sm:grid-cols-2">
              <label className="form-label">Venue name *<input className="form-input" required maxLength={200} value={temporary.name}
                onChange={e => setTemporary({ ...temporary, name: e.target.value })} /></label>
              <label className="form-label">Address *<input className="form-input" required maxLength={200} value={temporary.address}
                onChange={e => setTemporary({ ...temporary, address: e.target.value })} /></label>
              <StateDistrictFields prefix="temporary-venue" reference={geography} required stateId={temporary.stateId}
                districtId={temporary.districtId} onChange={(stateId, districtId) => setTemporary({ ...temporary, stateId, districtId })} />
              <label className="form-label">Venue seating capacity *<input type="number" min={1} max={2147483647} required
                className="form-input" value={temporary.capacity || ''} onChange={e => setTemporary({ ...temporary, capacity: Number(e.target.value) })} /></label>
              <label className="form-label">Premises permission reference *<input className="form-input" required maxLength={400}
                value={temporary.permissionToUse} onChange={e => setTemporary({ ...temporary, permissionToUse: e.target.value })} /></label>
            </div>
          )}
          {draft.kind !== 'reschedule' && (
            <div className="grid gap-3 sm:grid-cols-2">
              <label className="form-label">Batch code *<input className="form-input" required maxLength={32} value={batchCode}
                onChange={e => setBatchCode(e.target.value)} /></label>
              <label className="form-label">Requested seats *<input className="form-input" type="number" required min={1}
                max={draft.kind === 'shared' ? draft.venue.capacity : temporary.capacity || 2147483647}
                value={seats} onChange={e => setSeats(e.target.value)} /></label>
              <div>
                <label className="form-label" htmlFor="venue-booking-course">Course *</label>
                <select id="venue-booking-course" className="form-input" required value={courseId || ''}
                  onChange={e => { setCourseId(Number(e.target.value)); setProgramId(0); }}>
                  <option value="">Select a course</option>
                  {courses.data?.content.map(course => <option key={course.id} value={course.id}>{course.courseName}</option>)}
                </select>
                {(courses.data?.hasPrevious || courses.data?.hasNext) && <div className="mt-1 flex gap-3 text-sm">
                  <button type="button" className="underline disabled:opacity-50" disabled={!courses.data?.hasPrevious}
                    onClick={() => { setCoursePage(coursePage - 1); setCourseId(0); setProgramId(0); }}>Previous courses</button>
                  <button type="button" className="underline disabled:opacity-50" disabled={!courses.data?.hasNext}
                    onClick={() => { setCoursePage(coursePage + 1); setCourseId(0); setProgramId(0); }}>Next courses</button>
                </div>}
                {courses.isError && <button type="button" className="underline" onClick={() => void courses.refetch()}>Retry courses</button>}
              </div>
              <div>
                <label className="form-label" htmlFor="venue-booking-program">Program *</label>
                <select id="venue-booking-program" className="form-input" required disabled={!courseId || programs.isPending} value={programId || ''}
                  onChange={e => setProgramId(Number(e.target.value))}>
                  <option value="">Select a linked program</option>
                  {programs.data?.map(program => <option key={program.id} value={program.id}>{program.name}</option>)}
                </select>
                {programs.isError && <button type="button" className="underline" onClick={() => void programs.refetch()}>Retry programs</button>}
              </div>
            </div>
          )}
          <ScheduleFields sessions={sessions} onChange={setSessions} />
        </fieldset>
        {(validation || save.isError) && <p role="alert" className="rounded bg-red-50 p-3 text-sm text-red-800">{validation || bookingError(save.error)}</p>}
        <div className="flex justify-end gap-2">
          <button type="button" className="btn-outline" disabled={save.isPending} onClick={onClose}>Cancel</button>
          <button type="submit" className="btn-primary" disabled={save.isPending}>Submit request</button>
        </div>
      </form>
    </Modal>
  );
}
