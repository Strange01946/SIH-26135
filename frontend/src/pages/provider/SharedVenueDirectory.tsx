import { useState, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { addVenueClosure, createSharedVenue, getSharedVenues, getVenueAvailability, releaseVenueClosure,
  type BusySlot, type SharedVenue } from '@/api/venueBookingApi';
import type { RegistrationReferenceData } from '@/api/registrationApi';
import type { ApiError } from '@/api/types';
import { Modal } from '@/components/common/Modal';
import { StateDistrictFields } from '@/components/common/StateDistrictFields';
import { ErrorState } from '@/components/feedback/ErrorState';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { useAuth } from '@/context/AuthContext';
import { bookingError, formatVenueTime, indiaDate } from './VenueBookingForm';

function CreateSharedVenueForm({ geography, onClose, onSaved }: {
  geography: RegistrationReferenceData;
  onClose: () => void;
  onSaved: () => void;
}) {
  const { user } = useAuth();
  const [form, setForm] = useState({ code: '', name: '', address: '', pincode: '', capacity: 0,
    stateId: user?.stateId ?? 0, districtId: user?.districtId ?? 0 });
  const save = useMutation<SharedVenue, ApiError>({
    mutationFn: () => createSharedVenue({ ...form, code: form.code.trim(), name: form.name.trim(),
      address: form.address.trim() }),
    onSuccess: onSaved,
  });

  return (
    <Modal open title="Register shared center" onClose={() => { if (!save.isPending) onClose(); }}>
      <form className="space-y-4 p-5" onSubmit={event => { event.preventDefault(); if (!save.isPending) save.mutate(); }}>
        <fieldset className="grid grid-cols-1 gap-3 sm:grid-cols-2" disabled={save.isPending}>
          <label className="form-label">Center code *<input className="form-input" required maxLength={32}
            value={form.code} onChange={e => setForm({ ...form, code: e.target.value })} /></label>
          <label className="form-label">Center name *<input className="form-input" required maxLength={200}
            value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} /></label>
          <StateDistrictFields prefix="shared-venue" reference={geography} required stateId={form.stateId}
            districtId={form.districtId} onChange={(stateId, districtId) => setForm({ ...form, stateId, districtId })} />
          <label className="form-label sm:col-span-2">Address *<input className="form-input" required maxLength={200}
            value={form.address} onChange={e => setForm({ ...form, address: e.target.value })} /></label>
          <label className="form-label">Seating capacity *<input className="form-input" type="number" required min={1} max={2147483647}
            value={form.capacity || ''} onChange={e => setForm({ ...form, capacity: Number(e.target.value) })} /></label>
          <label className="form-label">PIN code *<input className="form-input" required pattern="[1-9][0-9]{5}" maxLength={6} inputMode="numeric"
            value={form.pincode} onChange={e => setForm({ ...form, pincode: e.target.value })} /></label>
        </fieldset>
        {save.isError && <p className="text-sm text-red-700" role="alert">{bookingError(save.error)}</p>}
        <div className="flex justify-end gap-2">
          <button className="btn-outline" type="button" disabled={save.isPending} onClick={onClose}>Cancel</button>
          <button className="btn-primary" type="submit" disabled={save.isPending}>Save shared center</button>
        </div>
      </form>
    </Modal>
  );
}

function VenueAvailability({ venue, manager, onRequest }: {
  venue: SharedVenue;
  manager: boolean;
  onRequest?: (venue: SharedVenue) => void;
}) {
  const queryClient = useQueryClient();
  const [from, setFrom] = useState(indiaDate());
  const [to, setTo] = useState(indiaDate(new Date(Date.now() + 31 * 86_400_000)));
  const [startsAt, setStartsAt] = useState('');
  const [endsAt, setEndsAt] = useState('');
  const [reason, setReason] = useState('');
  const [notice, setNotice] = useState('');
  const validDateRange = Boolean(from && to && from < to
    && Date.parse(to + 'T00:00:00Z') - Date.parse(from + 'T00:00:00Z') <= 93 * 86_400_000);
  const slots = useQuery({ queryKey: ['venue-bookings', 'availability', venue.id, from, to],
    queryFn: ({ signal }) => getVenueAvailability(venue.id, from, to, signal), enabled: validDateRange });
  const add = useMutation<BusySlot, ApiError>({
    mutationFn: () => addVenueClosure(venue.id, { startsAt, endsAt, reason: reason.trim() }),
    onSuccess: () => {
      setNotice('Closure added.');
      setStartsAt('');
      setEndsAt('');
      setReason('');
      void queryClient.invalidateQueries({ queryKey: ['venue-bookings', 'availability', venue.id] });
    },
  });
  const remove = useMutation<void, ApiError, number>({
    mutationFn: (id) => releaseVenueClosure(venue.id, id),
    onSuccess: () => {
      setNotice('Closure removed.');
      void queryClient.invalidateQueries({ queryKey: ['venue-bookings', 'availability', venue.id] });
    },
  });
  function block(event: FormEvent) {
    event.preventDefault();
    if (!add.isPending) {
      setNotice('');
      remove.reset();
      add.mutate();
    }
  }

  return (
    <section className="card space-y-4 p-5" aria-labelledby="venue-calendar-heading">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 id="venue-calendar-heading" className="section-title">{venue.name} availability</h2>
          <p className="text-sm text-slate-600">Times shown in IST. Reservations and closures appear here.</p>
        </div>
        {onRequest && <button type="button" className="btn-primary" onClick={() => onRequest(venue)}>Request this center</button>}
      </div>
      <div className="grid grid-cols-2 gap-3">
        <label className="form-label">Calendar from<input className="form-input" type="date" value={from} onChange={e => setFrom(e.target.value)} /></label>
        <label className="form-label">Calendar until (exclusive)<input className="form-input" type="date" value={to} onChange={e => setTo(e.target.value)} /></label>
      </div>
      {!validDateRange ? <p className="text-sm text-slate-700">Choose an increasing date range of up to 93 days.</p>
        : slots.isPending ? <LoadingSpinner label="Loading availability" />
        : slots.isError ? <ErrorState title="Could not load availability" error={slots.error} onRetry={() => void slots.refetch()} />
        : slots.data.length === 0 ? <p className="text-sm text-slate-600">No reservations or closures in this date range.</p>
        : <ul className="divide-y text-sm">
          {slots.data.map(slot => <li className="flex flex-wrap items-center justify-between gap-2 py-2"
            key={slot.startsAt + ':' + slot.endsAt + ':' + (slot.blackoutId ?? '')}>
            <span>{formatVenueTime(slot.startsAt)} – {formatVenueTime(slot.endsAt)} · {slot.reason || 'Unavailable'}</span>
            {manager && slot.blackoutId && <button type="button" className="btn-outline" disabled={remove.isPending}
              onClick={() => { setNotice(''); add.reset(); remove.mutate(slot.blackoutId!); }}>Remove closure</button>}
          </li>)}
        </ul>}
      {notice && <p role="status" className="text-sm text-green-800">{notice}</p>}
      {(add.error || remove.error) && <p role="alert" className="text-sm text-red-800">{bookingError(add.error || remove.error)}</p>}
      {manager && <form className="space-y-3 border-t pt-4" onSubmit={block}>
        <h3 className="font-semibold">Block time for a closure</h3>
        <div className="grid grid-cols-2 gap-3">
          <label className="form-label">Closure start<input className="form-input" type="datetime-local" required value={startsAt} onChange={e => setStartsAt(e.target.value)} /></label>
          <label className="form-label">Closure end<input className="form-input" type="datetime-local" required value={endsAt} onChange={e => setEndsAt(e.target.value)} /></label>
        </div>
        <label className="form-label">Closure reason<input className="form-input" required maxLength={300} value={reason} onChange={e => setReason(e.target.value)} /></label>
        <button type="submit" className="btn-outline" disabled={add.isPending}>Add closure</button>
      </form>}
    </section>
  );
}

export function SharedVenueDirectory({ geography, manager, onRequest }: {
  geography: RegistrationReferenceData;
  manager: boolean;
  onRequest?: (venue: SharedVenue) => void;
}) {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [stateId, setStateId] = useState(user?.stateId ?? 0);
  const [districtId, setDistrictId] = useState(user?.districtId ?? 0);
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState<SharedVenue | null>(null);
  const [create, setCreate] = useState(false);
  const list = useQuery({ queryKey: ['venue-bookings', 'shared', user?.id, stateId, districtId, page],
    queryFn: ({ signal }) => getSharedVenues(stateId, districtId, page, 20, signal) });
  return (
    <div className="space-y-4">
      <section className="card space-y-4 p-5" aria-labelledby="shared-centers-heading">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <h2 id="shared-centers-heading" className="section-title">Shared training centers</h2>
          {manager && <button type="button" className="btn-primary" onClick={() => setCreate(true)}>Register shared center</button>}
        </div>
        <div className="grid gap-3 sm:grid-cols-2">
          <StateDistrictFields prefix="shared-filter" reference={geography} stateId={stateId} districtId={districtId}
            disabled={manager && Boolean(user?.districtId)}
            onChange={(state, district) => { setStateId(state); setDistrictId(district); setPage(0); setSelected(null); }} />
        </div>
        {list.isPending ? <LoadingSpinner label="Loading shared centers" /> : list.isError
          ? <ErrorState title="Could not load shared centers" error={list.error} onRetry={() => void list.refetch()} />
          : list.data.length === 0 ? <p className="text-sm text-slate-600">No bookable centers match these filters.</p>
          : <ul className="divide-y">{list.data.map(venue => <li key={venue.id} className="flex flex-wrap items-center justify-between gap-3 py-3">
            <div><p className="font-semibold">{venue.name}</p>
              <p className="text-sm text-slate-600">{venue.code} · {geography.districts.find(d => d.id === venue.districtId)?.name ?? venue.districtId} · {venue.capacity} seats</p>
              <p className="text-sm text-slate-600">{venue.address}</p>
            </div>
            <button type="button" className="btn-outline" aria-label={`View availability for ${venue.name}`}
              onClick={() => setSelected(venue)}>View availability</button>
          </li>)}</ul>}
        {(page > 0 || list.data?.length === 20) && <nav aria-label="Shared center pages" className="flex gap-3">
          <button className="btn-outline" type="button" disabled={!page} onClick={() => setPage(page - 1)}>Previous page</button>
          <button className="btn-outline" type="button" disabled={list.data?.length !== 20} onClick={() => setPage(page + 1)}>Next page</button>
        </nav>}
      </section>
      {selected && <VenueAvailability key={selected.id} venue={selected} manager={manager} onRequest={onRequest} />}
      {create && <CreateSharedVenueForm geography={geography} onClose={() => setCreate(false)}
        onSaved={() => { setCreate(false); void queryClient.invalidateQueries({ queryKey: ['venue-bookings', 'shared'] }); }} />}
    </div>
  );
}
