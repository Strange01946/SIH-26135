import type { RegistrationReferenceData } from '@/api/registrationApi';

interface Props {
  prefix: string;
  reference: RegistrationReferenceData | null;
  stateId: number;
  districtId: number;
  onChange: (stateId: number, districtId: number) => void;
  required?: boolean;
  disabled?: boolean;
  showDistrict?: boolean;
}

export function StateDistrictFields({ prefix, reference, stateId, districtId, onChange, required = false, disabled, showDistrict = true }: Props) {
  const state = `${prefix}-state`;
  const district = `${prefix}-district`;
  const districts = reference?.districts.filter((item) => item.parentId === stateId) ?? [];
  return (
    <>
      <div className="min-w-0">
        <label htmlFor={state} className="form-label">State or union territory{required && ' *'}</label>
        <select id={state} className="form-input" value={stateId || ''} disabled={disabled} required={required}
          onChange={(event) => onChange(Number(event.target.value), 0)}>
          <option value="">{required ? 'Select a state or union territory' : 'No state or territory assigned'}</option>
          {reference?.states.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}
        </select>
      </div>
      {showDistrict && <div className="min-w-0">
        <label htmlFor={district} className="form-label">District{required && ' *'}</label>
        <select id={district} className="form-input" value={districtId || ''} disabled={disabled || !stateId}
          required={required} onChange={(event) => onChange(stateId, Number(event.target.value))}>
          <option value="">{!stateId ? 'Select a state first' : required ? 'Select a district' : 'No district assigned'}</option>
          {districts.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}
        </select>
      </div>}
    </>
  );
}
