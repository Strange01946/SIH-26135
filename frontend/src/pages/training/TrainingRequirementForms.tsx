import { useState, type FormEvent } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import type { RegistrationReferenceData } from '@/api/registrationApi';
import {
  applyForTrainingRequirement, publishTrainingRequirement, setTrainingCentre, type TrainingRequirement, type TrainingWindow,
} from '@/api/trainingRequirementApi';
import { getSharedVenues } from '@/api/venueBookingApi';
import { Modal } from '@/components/common/Modal';
import { StateDistrictFields } from '@/components/common/StateDistrictFields';
import { bookingError, indiaDate } from '@/pages/provider/VenueBookingForm';

const field = (form: FormData, name: string) => String(form.get(name) ?? '').trim();
const centersPerPage = 40;

function TrainingCentreField({ stateId, districtId }: { stateId: number; districtId: number }) {
  const [page, setPage] = useState(0);
  const [center, setCenter] = useState('');
  const enabled = stateId > 0 && districtId > 0;
  const venues = useQuery({
    queryKey: ['venue-bookings', 'requirement-centres', stateId, districtId, page],
    queryFn: ({ signal }) => getSharedVenues(stateId, districtId, page, centersPerPage, signal),
    enabled,
  });
  const placeholder = !enabled ? 'Select a district first'
    : venues.isPending ? 'Loading shared centers…'
      : venues.isError ? 'Could not load shared centers'
        : venues.data.length === 0 ? 'No active shared centers on this page' : 'Select a shared center';
  return (
    <div className="space-y-2">
      <label className="form-label">Training center *
        <select className="form-input" name="centerId" required value={center} onChange={event => setCenter(event.target.value)}>
          <option value="">{placeholder}</option>
          {venues.data?.map(venue => <option key={venue.id} value={venue.id}>{venue.name} ({venue.capacity} seats)</option>)}
        </select>
      </label>
      {venues.isError && <button type="button" className="btn-outline" onClick={() => void venues.refetch()}>Retry centers</button>}
      {(page > 0 || venues.data?.length === centersPerPage) && <div className="flex gap-2">
        <button type="button" className="btn-outline" disabled={!page} onClick={() => { setPage(page - 1); setCenter(''); }}>Previous centers</button>
        <button type="button" className="btn-outline" disabled={venues.data?.length !== centersPerPage}
          onClick={() => { setPage(page + 1); setCenter(''); }}>Next centers</button>
      </div>}
    </div>
  );
}

const readWindow = (form: FormData): TrainingWindow => ({
  startDate: field(form, 'startDate'), endDate: field(form, 'endDate'),
  dailyStartTime: field(form, 'dailyStartTime'), dailyEndTime: field(form, 'dailyEndTime'),
});

function WindowFields({ bounds }: { bounds?: TrainingRequirement }) {
  const first = bounds && bounds.earliestDate > indiaDate() ? bounds.earliestDate : indiaDate();
  return (
    <div className="grid gap-3 sm:grid-cols-2">
      <label className="form-label">{bounds ? 'Proposed first day' : 'Earliest training date'} *
        <input className="form-input" type="date" name="startDate" required min={first} max={bounds?.latestDate} />
      </label>
      <label className="form-label">{bounds ? 'Proposed last day' : 'Latest training date'} *
        <input className="form-input" type="date" name="endDate" required min={first} max={bounds?.latestDate} />
      </label>
      <label className="form-label">Daily start time (IST) *
        <input className="form-input" type="time" name="dailyStartTime" required min={bounds?.dailyStartTime.slice(0, 5)}
          max={bounds?.dailyEndTime.slice(0, 5)} />
      </label>
      <label className="form-label">Daily end time (IST) *
        <input className="form-input" type="time" name="dailyEndTime" required min={bounds?.dailyStartTime.slice(0, 5)}
          max={bounds?.dailyEndTime.slice(0, 5)} />
      </label>
    </div>
  );
}

export function PublishRequirementDialog({ reference, state, district, onClose, onSaved }: {
  reference: RegistrationReferenceData; state: number; district: number;
  onClose: () => void; onSaved: (id: number) => void;
}) {
  const [stateId, setStateId] = useState(state);
  const [districtId, setDistrictId] = useState(district);
  const save = useMutation({ mutationFn: publishTrainingRequirement, onSuccess: value => onSaved(value.id) });
  const scopedReference = state ? { ...reference, states: reference.states.filter(item => item.id === state),
    districts: reference.districts.filter(item => !district || item.id === district) } : reference;

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (save.isPending) return;
    const form = new FormData(event.currentTarget);
    const { startDate, endDate, dailyStartTime, dailyEndTime } = readWindow(form);
    save.mutate({
      title: field(form, 'title'), skillsToTeach: field(form, 'skills'), description: field(form, 'description') || undefined,
      stateId, districtId, centerId: Number(field(form, 'centerId')), seats: Number(field(form, 'seats')),
      earliestDate: startDate, latestDate: endDate, dailyStartTime, dailyEndTime,
    });
  }

  return (
    <Modal open title="Publish training requirement" className="max-w-3xl" onClose={() => { if (!save.isPending) onClose(); }}>
      <form className="space-y-4 p-5" onSubmit={submit}>
        <fieldset className="space-y-4" disabled={save.isPending}>
          <label className="form-label">Requirement title *
            <input className="form-input" name="title" required maxLength={150} />
          </label>
          <div className="grid gap-3 sm:grid-cols-[1fr_8rem]">
            <label className="form-label">Skills to teach *
              <textarea className="form-input" name="skills" rows={2} required maxLength={600} />
            </label>
            <label className="form-label">Seats *
              <input className="form-input" name="seats" type="number" min={1} max={2147483647} required />
            </label>
          </div>
          <div className="grid gap-3 sm:grid-cols-2">
            <StateDistrictFields prefix="requirement" reference={scopedReference} stateId={stateId} districtId={districtId}
              required disabled={Boolean(district)} onChange={(nextState, nextDistrict) => { setStateId(nextState); setDistrictId(nextDistrict); }} />
          </div>
          <TrainingCentreField key={`${stateId}-${districtId}`} stateId={stateId} districtId={districtId} />
          <WindowFields />
          <label className="form-label">Delivery requirements
            <textarea className="form-input" name="description" rows={3} maxLength={1500} />
          </label>
        </fieldset>
        {save.error && <p role="alert" className="text-sm text-red-800">{bookingError(save.error)}</p>}
        <div className="flex justify-end gap-2">
          <button type="button" className="btn-outline" disabled={save.isPending} onClick={onClose}>Cancel</button>
          <button type="submit" className="btn-primary" disabled={save.isPending}>Publish requirement</button>
        </div>
      </form>
    </Modal>
  );
}

export function ApplyRequirementDialog({ requirement, onClose, onSaved }: {
  requirement: TrainingRequirement; onClose: () => void; onSaved: () => void;
}) {
  const save = useMutation({ mutationFn: (form: FormData) => applyForTrainingRequirement(requirement.id, {
    ...readWindow(form), deliveryNotes: field(form, 'notes') || undefined,
  }), onSuccess: onSaved });
  return (
    <Modal open title="Apply to deliver training" onClose={() => { if (!save.isPending) onClose(); }}>
      <form className="space-y-4 p-5" onSubmit={event => {
        event.preventDefault();
        if (!save.isPending) save.mutate(new FormData(event.currentTarget));
      }}>
        <p className="text-sm text-slate-700">{requirement.title} · {requirement.centerName} · {requirement.seats} seats</p>
        <p className="text-sm text-slate-700">Propose one session per day, including the first and last days. The center is reserved only if your proposal is accepted.</p>
        <fieldset disabled={save.isPending} className="space-y-3">
          <WindowFields bounds={requirement} />
          <label className="form-label">Delivery plan and equipment needs
            <textarea className="form-input" name="notes" rows={4} maxLength={1500} />
          </label>
        </fieldset>
        {save.error && <p role="alert" className="text-sm text-red-800">{bookingError(save.error)}</p>}
        <div className="flex justify-end gap-2">
          <button type="button" className="btn-outline" disabled={save.isPending} onClick={onClose}>Cancel</button>
          <button type="submit" className="btn-primary" disabled={save.isPending}>Submit application</button>
        </div>
      </form>
    </Modal>
  );
}

export function LegacyRequirementCentreDialog({ requirement, onClose, onSaved }: {
  requirement: TrainingRequirement; onClose: () => void; onSaved: (saved: TrainingRequirement) => void;
}) {
  const save = useMutation({ mutationFn: (centerId: number) => setTrainingCentre(requirement.id, centerId), onSuccess: onSaved });
  return (
    <Modal open title="Assign training center" onClose={() => { if (!save.isPending) onClose(); }}>
      <form className="space-y-4 p-5" onSubmit={event => {
        event.preventDefault();
        if (!save.isPending) save.mutate(Number(field(new FormData(event.currentTarget), 'centerId')));
      }}>
        <p className="text-sm">{requirement.title} · {requirement.districtName} · {requirement.seats} seats</p>
        {requirement.status === 'AWARDED' && <p className="text-sm">Confirming the center reserves the selected provider’s proposed sessions.</p>}
        <fieldset disabled={save.isPending}>
          <TrainingCentreField stateId={requirement.stateId} districtId={requirement.districtId} />
        </fieldset>
        {save.error && <p role="alert" className="text-sm text-red-800">{bookingError(save.error)}</p>}
        <div className="flex justify-end gap-2">
          <button type="button" className="btn-outline" disabled={save.isPending} onClick={onClose}>Cancel</button>
          <button type="submit" className="btn-primary" disabled={save.isPending}>Confirm training center</button>
        </div>
      </form>
    </Modal>
  );
}
