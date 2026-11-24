import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useSearchParams } from 'react-router-dom';
import { getRegistrationReferenceData } from '@/api/registrationApi';
import { updateTrainingBatchStatus } from '@/api/providerPortalApi';
import type { ApiError } from '@/api/types';
import { closeBooking, getBookingDetails, getBookings, reviewBooking,
  type BookingDetails, type BookingStatus, type SharedVenue } from '@/api/venueBookingApi';
import { Modal } from '@/components/common/Modal';
import { ErrorState } from '@/components/feedback/ErrorState';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { useAuth } from '@/context/AuthContext';
import { simulationActive } from '@/lib/demoEnvironment';
import { bookingError, formatVenueTime, VenueBookingForm, type BookingDraft } from './VenueBookingForm';
import { SharedVenueDirectory } from './SharedVenueDirectory';

const statuses: Record<BookingStatus, string> = {
  PENDING: 'Awaiting review', APPROVED: 'Approved', REJECTED: 'Declined',
  CANCELLED: 'Cancelled', COMPLETED: 'Completed', SUPERSEDED: 'Rescheduled',
};
const pageSize = 20;
type Decision = 'approve' | 'reject' | 'cancel' | 'complete';
const actionLabels: Record<Decision, string> = {
  approve: 'Approve booking', reject: 'Decline booking', cancel: 'Cancel booking', complete: 'Complete training',
};

function BookingDecision({ details, action, onClose, onSaved }: {
  details: BookingDetails;
  action: Decision;
  onClose: () => void;
  onSaved: (details: BookingDetails) => void;
}) {
  const [reason, setReason] = useState('');
  const [invalid, setInvalid] = useState(false);
  const mutation = useMutation<BookingDetails, ApiError>({
    mutationFn: () => action === 'approve' || action === 'reject'
      ? reviewBooking(details.booking.id, action === 'approve', reason.trim() || undefined)
      : closeBooking(details.booking.id, action === 'complete' ? 'COMPLETE' : 'CANCEL', reason.trim() || undefined),
    onSuccess: onSaved,
  });
  return (
    <Modal open title={actionLabels[action]} onClose={() => { if (!mutation.isPending) onClose(); }}>
      <form className="space-y-4 p-5" onSubmit={event => {
        event.preventDefault();
        if (action === 'reject' && !reason.trim()) {
          setInvalid(true);
          return;
        }
        setInvalid(false);
        if (!mutation.isPending) mutation.mutate();
      }}>
        <p className="text-sm">{details.booking.batchCode} · {details.booking.venueName}</p>
        {action === 'approve' && <p className="text-sm">Approval reserves the listed sessions{details.booking.replacesBookingId ? ' and releases the replaced future sessions.' : ' and creates the training batch.'}</p>}
        {(action === 'cancel' || action === 'complete') && <p className="text-sm">Any unused future reservations for this booking will be released.</p>}
        <label className="form-label">Decision note {action === 'reject' ? '*' : '(optional)'}
          <textarea className="form-input min-h-20" maxLength={500} required={action === 'reject'} disabled={mutation.isPending}
            value={reason} onChange={event => setReason(event.target.value)} />
        </label>
        {(invalid || mutation.isError) && <p role="alert" className="text-sm text-red-700">{invalid ? 'Enter a reason for declining.' : bookingError(mutation.error)}</p>}
        <div className="flex justify-end gap-2">
          <button type="button" className="btn-outline" disabled={mutation.isPending} onClick={onClose}>Back</button>
          <button type="submit" className="btn-primary" disabled={mutation.isPending}>Confirm {action === 'approve' ? 'approval'
            : action === 'reject' ? 'decline' : action === 'cancel' ? 'cancellation' : 'completion'}</button>
        </div>
      </form>
    </Modal>
  );
}

function BookingRecord({ id, admin, onDraft, onDecision }: {
  id: number;
  admin: boolean;
  onDraft: (draft: BookingDraft) => void;
  onDecision: (details: BookingDetails, action: Decision) => void;
}) {
  const queryClient = useQueryClient();
  const { user } = useAuth();
  const detail = useQuery({ queryKey: ['venue-bookings', 'detail', user?.id, id],
    queryFn: ({ signal }) => getBookingDetails(id, signal) });
  const advance = useMutation<unknown, ApiError, { batchId: number; status: number }>({
    mutationFn: ({ batchId, status }) => updateTrainingBatchStatus(batchId, { batchStatusId: status }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['venue-bookings'] });
      void queryClient.invalidateQueries({ queryKey: ['provider', 'batches'] });
    },
  });
  if (detail.isPending) return <LoadingSpinner label="Loading booking details" />;
  if (detail.isError) return <ErrorState title="Could not load booking details" error={detail.error} onRetry={() => void detail.refetch()} />;
  const { booking, sessions } = detail.data;
  const hasFuture = sessions.some(session => !session.releasedAt && Date.parse(session.startsAt.slice(0, 19) + '+05:30') > Date.now());
  const approved = booking.status === 'APPROVED';
  const batchId = booking.batchId;

  return (
    <section className="card space-y-4 p-5" aria-labelledby="booking-detail-title">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 id="booking-detail-title" className="section-title">{booking.batchCode} · {booking.venueName}</h2>
          <p className="text-sm text-slate-600">{booking.providerName} · {booking.courseName} · {booking.programName}</p>
          <p className="text-sm text-slate-600">{booking.venueAddress} · {booking.seats} requested seats of {booking.venueCapacity}</p>
        </div>
        <div className="text-right text-sm">
          <p className="font-semibold text-navy">{statuses[booking.status]}</p>
          {booking.batchStatus && <p>Batch {booking.batchStatus.toLowerCase()}</p>}
        </div>
      </div>
      {booking.replacesBookingId && <p className="text-sm">Postponement of booking {booking.replacesBookingId}</p>}
      {booking.premisesPermission && <p className="text-sm">Premises permission · {booking.premisesPermission}</p>}
      {booking.decisionNote && <p className="rounded bg-slate-50 p-3 text-sm">{booking.decisionNote}</p>}
      <div>
        <h3 className="font-semibold">Sessions (IST)</h3>
        <ul className="max-h-64 divide-y overflow-y-auto rounded border border-slate-200 text-sm">
          {sessions.map(session => <li key={session.startsAt} className="px-3 py-2">
            {formatVenueTime(session.startsAt)} – {formatVenueTime(session.endsAt)}
            <span className="ml-2 text-slate-600">{session.releasedAt ? 'Released' : session.reservedAt ? 'Reserved' : 'Requested'}</span>
          </li>)}
        </ul>
      </div>
      {advance.isError && <p role="alert" className="text-sm text-red-700">{bookingError(advance.error)}</p>}
      <div className="flex flex-wrap items-center gap-2">
        {admin && booking.status === 'PENDING' && <>
          <button type="button" className="btn-primary" onClick={() => onDecision(detail.data, 'approve')}>Approve booking</button>
          <button type="button" className="btn-outline" onClick={() => onDecision(detail.data, 'reject')}>Decline booking</button>
        </>}
        {!admin && (approved || booking.status === 'PENDING') && <button type="button" className="btn-outline"
          onClick={() => onDecision(detail.data, 'cancel')}>Cancel booking</button>}
        {!admin && approved && hasFuture && <button type="button" className="btn-outline"
          onClick={() => onDraft({ kind: 'reschedule', id: booking.id, name: booking.venueName })}>Postpone training</button>}
        {!admin && approved && batchId && booking.batchStatus === 'PLANNED' && <button type="button" className="btn-primary"
          disabled={advance.isPending} onClick={() => advance.mutate({ batchId, status: 2 })}>Open enrollment</button>}
        {!admin && approved && batchId && booking.batchStatus === 'OPEN' && <button type="button" className="btn-primary"
          disabled={advance.isPending} onClick={() => advance.mutate({ batchId, status: 3 })}>Start training</button>}
        {!admin && approved && booking.batchStatus === 'ONGOING' && <button type="button" className="btn-primary"
          onClick={() => onDecision(detail.data, 'complete')}>Complete training</button>}
        {!admin && bookedForEnrollment(booking.batchStatus) && <Link className="btn-outline" to="/provider/enrollment">Candidate enrollment</Link>}
      </div>
    </section>
  );
}

const bookedForEnrollment = (status?: string | null) => status === 'OPEN' || status === 'ONGOING';

export function VenueBookingsPage({ admin = false }: { admin?: boolean }) {
  const { user, hasAuthority } = useAuth();
  const queryClient = useQueryClient();
  const [search] = useSearchParams();
  const [directory, setDirectory] = useState(search.get('view') === 'shared');
  const [status, setStatus] = useState<BookingStatus | ''>(admin ? 'PENDING' : '');
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState<number | null>(null);
  const [draft, setDraft] = useState<BookingDraft | null>(null);
  const [decision, setDecision] = useState<{ details: BookingDetails; action: Decision } | null>(null);
  const [notice, setNotice] = useState('');
  const geography = useQuery({ queryKey: ['registration-reference-data'], queryFn: getRegistrationReferenceData, enabled: !simulationActive });
  const list = useQuery({ queryKey: ['venue-bookings', 'requests', user?.id, status, page],
    queryFn: ({ signal }) => getBookings(status || undefined, page, pageSize, signal), enabled: !directory && !simulationActive });

  function saved(details: BookingDetails, message: string) {
    setDraft(null);
    setDecision(null);
    setDirectory(false);
    setSelected(details.booking.id);
    setNotice(message);
    queryClient.setQueryData(['venue-bookings', 'detail', user?.id, details.booking.id], details);
    void queryClient.invalidateQueries({ queryKey: ['venue-bookings'] });
    void queryClient.invalidateQueries({ queryKey: ['training-centers', 'directory'] });
    void queryClient.invalidateQueries({ queryKey: ['provider', 'centers'] });
    void queryClient.invalidateQueries({ queryKey: ['provider', 'batches'] });
  }

  function requestShared(venue: SharedVenue) {
    setDraft({ kind: 'shared', venue });
  }

  const scopedGeography = geography.data && admin && user?.stateId ? {
    ...geography.data,
    states: geography.data.states.filter(state => state.id === user.stateId),
    districts: geography.data.districts.filter(district => district.parentId === user.stateId
      && (!user.districtId || district.id === user.districtId)),
  } : geography.data;

  return (
    <div className="space-y-5">
      <header className="card flex flex-wrap items-center justify-between gap-4 border-l-4 border-l-navy p-6">
        <div><h1 className="text-2xl font-bold text-navy">Venue Bookings</h1>
          <p className="mt-1 text-sm text-slate-600">{admin ? 'Review requests and manage shared training availability.'
            : 'Reserve sessions at a shared center or request approval for a temporary location.'}</p></div>
        {!admin && !simulationActive && <button type="button" className="btn-primary" disabled={!geography.data}
          onClick={() => setDraft({ kind: 'temporary' })}>Propose temporary location</button>}
      </header>
      {simulationActive ? <p role="status" className="card p-5">Switch to live mode to manage venue bookings.</p> : <>
        <div className="flex flex-wrap gap-2">
          <button type="button" className={directory ? 'btn-outline' : 'btn-primary'} aria-pressed={!directory}
            onClick={() => setDirectory(false)}>{admin ? 'Review queue' : 'My bookings'}</button>
          <button type="button" className={directory ? 'btn-primary' : 'btn-outline'} aria-pressed={directory}
            onClick={() => setDirectory(true)}>Shared centers</button>
        </div>
        {notice && <p role="status" className="rounded border border-green-200 bg-green-50 p-3 text-sm text-green-800">{notice}</p>}
        {geography.isError && <ErrorState title="Could not load location options" error={geography.error} onRetry={() => void geography.refetch()} />}
        {directory ? (scopedGeography ? <SharedVenueDirectory geography={scopedGeography} manager={admin && hasAuthority('venue.shared.manage')}
          onRequest={admin ? undefined : requestShared} /> : <LoadingSpinner label="Loading locations" />) : <>
          <section className="card space-y-4 p-5" aria-label={admin ? 'Venue review queue' : 'Provider venue bookings'}>
            <label className="form-label max-w-xs">Booking status
              <select className="form-input" value={status} onChange={event => { setStatus(event.target.value as BookingStatus | ''); setPage(0); setSelected(null); }}>
                <option value="">All statuses</option>
                {Object.entries(statuses).map(([key, label]) => <option key={key} value={key}>{label}</option>)}
              </select>
            </label>
            {list.isPending ? <LoadingSpinner label="Loading bookings" /> : list.isError ? <ErrorState title="Could not load bookings"
              error={list.error} onRetry={() => void list.refetch()} /> : list.data.length === 0 ? <p className="text-sm text-slate-600">No bookings match this status.</p>
              : <div className="overflow-x-auto"><table className="w-full text-left text-sm">
                <thead className="border-b bg-slate-50"><tr>
                  <th scope="col" className="p-3">Batch</th><th scope="col" className="p-3">Venue</th>
                  {admin && <th scope="col" className="p-3">Provider</th>}
                  <th scope="col" className="p-3">Status</th><th scope="col" className="p-3">Actions</th>
                </tr></thead>
                <tbody className="divide-y">{list.data.map(booking => <tr key={booking.id}>
                  <th scope="row" className="p-3">{booking.batchCode}{booking.replacesBookingId && <span className="block font-normal">Postponement</span>}</th>
                  <td className="p-3">{booking.venueName}<span className="block text-slate-600">{booking.venueType === 'SHARED' ? 'Shared center' : 'Temporary location'}</span></td>
                  {admin && <td className="p-3">{booking.providerName}</td>}
                  <td className="p-3">{statuses[booking.status]}</td>
                  <td className="p-3"><button type="button" className="btn-outline" onClick={() => setSelected(booking.id)}>View booking</button></td>
                </tr>)}</tbody>
              </table></div>}
            {(page > 0 || list.data?.length === pageSize) && <nav aria-label="Booking pages" className="flex gap-2">
              <button type="button" className="btn-outline" disabled={!page} onClick={() => setPage(page - 1)}>Previous bookings</button>
              <button type="button" className="btn-outline" disabled={list.data?.length !== pageSize} onClick={() => setPage(page + 1)}>Next bookings</button>
            </nav>}
          </section>
          {selected && <BookingRecord key={selected} id={selected} admin={admin} onDraft={setDraft}
            onDecision={(details, action) => setDecision({ details, action })} />}
        </>}
        {draft && scopedGeography && <VenueBookingForm draft={draft} geography={scopedGeography} onClose={() => setDraft(null)}
          onSaved={details => saved(details, 'Request submitted for review.')} />}
        {decision && <BookingDecision details={decision.details} action={decision.action} onClose={() => setDecision(null)}
          onSaved={details => saved(details, 'Booking updated.')} />}
      </>}
    </div>
  );
}
