import { useState, type FormEvent } from 'react';
import { useMutation } from '@tanstack/react-query';
import { updateTrainingCenter } from '@/api/trainingCenterApi';
import type { ReferenceItem, RegistrationReferenceData } from '@/api/registrationApi';
import type { ApiError } from '@/api/types';
import { Modal } from '@/components/common/Modal';
import { StateDistrictFields } from '@/components/common/StateDistrictFields';
import type { TrainingCenter } from '@/types/provider';

export function centerActionError(error: ApiError | null) {
  return error?.fieldErrors?.[0]?.message || error?.message || 'The training center could not be saved.';
}

export function TrainingCenterForm({ center, reference, statuses, onClose, onSaved }: {
  center: TrainingCenter;
  reference: RegistrationReferenceData;
  statuses: ReferenceItem[];
  onClose: () => void;
  onSaved: (center: TrainingCenter) => void;
}) {
  const [name, setName] = useState(center.centerName ?? '');
  const [stateId, setStateId] = useState(center.stateId ?? 0);
  const [districtId, setDistrictId] = useState(center.districtId ?? 0);
  const [statusId, setStatusId] = useState(center.lifecycleStatusId ?? statuses.find(({ code }) => code === 'ACTIVE')?.id ?? 0);
  const [address1, setAddress1] = useState(center.addressLine1 ?? '');
  const [address2, setAddress2] = useState(center.addressLine2 ?? '');
  const [pincode, setPincode] = useState(center.pincode ?? '');
  const [capacity, setCapacity] = useState(center.operationalCapacity?.toString() ?? '');
  const [validation, setValidation] = useState('');
  const save = useMutation<TrainingCenter, ApiError>({ mutationFn: async () => {
    const optionalCapacity = capacity === '' ? null : Number(capacity);
    return updateTrainingCenter(center.id, {
      centerName: name.trim() === center.centerName ? undefined : name.trim(),
      stateId: stateId === center.stateId ? undefined : stateId,
      districtId: districtId === center.districtId ? undefined : districtId,
      lifecycleStatusId: statusId === center.lifecycleStatusId ? undefined : statusId,
      addressLine1: address1.trim() === (center.addressLine1 ?? '') ? undefined : address1.trim(),
      addressLine2: address2.trim() === (center.addressLine2 ?? '') ? undefined : address2.trim(),
      pincode: pincode === center.pincode ? undefined : pincode,
      operationalCapacity: optionalCapacity === (center.operationalCapacity ?? null) ? undefined : optionalCapacity,
    });
  }, onSuccess: onSaved });

  const geography = {
    ...reference,
    states: reference.states.some(({ id }) => id === center.stateId) ? reference.states
      : [...reference.states, { id: center.stateId, code: '', name: `State or territory ${center.stateId} (recorded)` }],
    districts: reference.districts.some(({ id }) => id === center.districtId) ? reference.districts
      : [...reference.districts, { id: center.districtId, parentId: center.stateId, code: '', name: `District ${center.districtId} (recorded)` }],
  };

  function submit(event: FormEvent) {
    event.preventDefault();
    if (save.isPending) return;
    if (!name.trim() || !stateId || !districtId || !statusId) {
      setValidation('Complete all required fields.');
      return;
    }
    setValidation('');
    save.mutate();
  }

  return (
    <Modal open onClose={() => { if (!save.isPending) onClose(); }} title="Edit training center">
      <form className="space-y-4 p-6" onSubmit={submit}>
        {(validation || save.isError) && <p role="alert" className="rounded bg-red-50 p-3 text-sm text-red-800">
          {validation || centerActionError(save.error)}
        </p>}
        <p className="text-sm text-slate-600">{center.centerCode} · Shared center</p>
        <fieldset className="grid grid-cols-1 gap-4 sm:grid-cols-2" disabled={save.isPending}>
          <div className="sm:col-span-2">
            <label htmlFor="center-name" className="form-label">Center name *</label>
            <input id="center-name" className="form-input" required maxLength={200} value={name}
              onChange={(event) => setName(event.target.value)} />
          </div>
          <StateDistrictFields prefix="center" reference={geography} required stateId={stateId} districtId={districtId}
            onChange={(state, district) => { setStateId(state); setDistrictId(district); }} />
          <div>
            <label htmlFor="center-status" className="form-label">Operational status *</label>
            <select id="center-status" className="form-input" required value={statusId || ''}
              onChange={(event) => setStatusId(Number(event.target.value))}>
              <option value="">Select a status</option>
              {center.lifecycleStatusId && !statuses.some(({ id }) => id === center.lifecycleStatusId) &&
                <option value={center.lifecycleStatusId}>Status {center.lifecycleStatusId} (recorded)</option>}
              {statuses.map(({ id, name: status }) => <option key={id} value={id}>{status}</option>)}
            </select>
          </div>
          <div>
            <label htmlFor="center-capacity" className="form-label">Operational capacity *</label>
            <input id="center-capacity" className="form-input" required type="number" min={1} max={2147483647} step={1}
              value={capacity} onChange={(event) => setCapacity(event.target.value)} />
          </div>
          <div className="sm:col-span-2">
            <label htmlFor="center-address-1" className="form-label">Address line 1 (optional)</label>
            <input id="center-address-1" className="form-input" maxLength={200} value={address1}
              onChange={(event) => setAddress1(event.target.value)} />
          </div>
          <div>
            <label htmlFor="center-address-2" className="form-label">Address line 2 (optional)</label>
            <input id="center-address-2" className="form-input" maxLength={200} value={address2}
              onChange={(event) => setAddress2(event.target.value)} />
          </div>
          <div>
            <label htmlFor="center-pincode" className="form-label">PIN code *</label>
            <input id="center-pincode" className="form-input" required inputMode="numeric" pattern="[1-9][0-9]{5}" maxLength={6}
              value={pincode} onChange={(event) => setPincode(event.target.value)} />
          </div>
        </fieldset>
        <div className="flex justify-end gap-2">
          <button type="button" className="btn-outline" disabled={save.isPending} onClick={onClose}>Cancel</button>
          <button type="submit" className="btn-primary" disabled={save.isPending}>{save.isPending ? 'Saving…' : 'Save center'}</button>
        </div>
      </form>
    </Modal>
  );
}
