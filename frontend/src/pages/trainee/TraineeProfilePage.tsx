import { useState, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { getRegistrationReferenceData, type RegistrationReferenceData } from '@/api/registrationApi';
import {
  getOwnTraineeProfile, getTraineeEnrollments, updateOwnTraineeProfile,
  type TraineeSelfProfile, type UpdateOwnTraineeProfile,
} from '@/api/traineeApi';
import type { ApiError } from '@/api/types';
import { StateDistrictFields } from '@/components/common/StateDistrictFields';
import { ReportedCareer } from '@/components/career/ReportedCareer';
import { OwnCareerSkills } from '@/components/career/CareerSkillsPanel';
import { EmptyState } from '@/components/feedback/EmptyState';
import { ErrorState } from '@/components/feedback/ErrorState';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { useAuth } from '@/context/AuthContext';
import { simulationActive } from '@/lib/demoEnvironment';

const todayInIndia = () => new Intl.DateTimeFormat('sv-SE', { timeZone: 'Asia/Kolkata' }).format(new Date());

function PersonalDetailsForm({ data, reference, saving, error, save, reset }: {
  data: TraineeSelfProfile;
  reference: RegistrationReferenceData;
  saving: boolean;
  error: ApiError | null;
  save: (request: UpdateOwnTraineeProfile) => void;
  reset: () => void;
}) {
  const { profile } = data;
  const [stateId, setStateId] = useState(profile.stateId);
  const [districtId, setDistrictId] = useState(profile.districtId);
  const districtChanged = stateId !== profile.stateId || districtId !== profile.districtId;

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (saving || !stateId || !districtId) return;
    const form = new FormData(event.currentTarget);
    const field = (name: string) => String(form.get(name) ?? '').trim();
    save({
      firstName: field('firstName'), middleName: field('middleName') || undefined,
      lastName: field('lastName') || undefined, dateOfBirth: field('dateOfBirth') || undefined,
      genderId: Number(field('genderId')), email: field('email') || undefined, phone: field('phone') || undefined,
      addressLine1: field('addressLine1') || undefined, addressLine2: field('addressLine2') || undefined,
      stateId, districtId, pincode: field('pincode'), educationLevelId: Number(field('educationLevelId')) || undefined,
    });
  }

  return (
    <form className="card space-y-5 p-5 sm:p-6" onSubmit={submit}>
      <fieldset className="space-y-3" disabled={saving}>
        <legend className="section-title">Personal details</legend>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <label className="form-label">First name *
            <input className="form-input" name="firstName" autoComplete="given-name" required maxLength={80} defaultValue={profile.firstName} />
          </label>
          <label className="form-label">Middle name
            <input className="form-input" name="middleName" autoComplete="additional-name" maxLength={80} defaultValue={profile.middleName ?? ''} />
          </label>
          <label className="form-label">Last name
            <input className="form-input" name="lastName" autoComplete="family-name" maxLength={80} defaultValue={profile.lastName ?? ''} />
          </label>
          <label className="form-label">Date of birth
            <input className="form-input" name="dateOfBirth" type="date" autoComplete="bday"
              max={todayInIndia()} defaultValue={profile.dateOfBirth ?? ''} />
          </label>
          <div>
            <label className="form-label" htmlFor="profile-gender">Gender *</label>
            <select className="form-input" id="profile-gender" name="genderId" required defaultValue={profile.genderId ?? ''}>
              <option value="">Select a gender</option>
              {reference.genders.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}
            </select>
          </div>
          <div>
            <label className="form-label" htmlFor="profile-education">Education level</label>
            <select className="form-input" id="profile-education" name="educationLevelId" defaultValue={profile.educationLevelId ?? ''}>
              <option value="">Select an education level</option>
              {data.educationLevels?.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}
            </select>
          </div>
        </div>
      </fieldset>
      <fieldset className="space-y-3 border-t pt-4" disabled={saving}>
        <legend className="section-title">Contact details and residence</legend>
        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <label className="form-label" htmlFor="profile-email">Contact email</label>
            <input className="form-input" id="profile-email" name="email" type="email" autoComplete="email" maxLength={255}
              defaultValue={profile.email ?? ''} aria-describedby="profile-contact-email" />
            <p id="profile-contact-email" className="mt-1 text-xs text-slate-600">Used for trainee follow-ups.</p>
          </div>
          <label className="form-label">Phone number
            <input className="form-input" name="phone" type="tel" autoComplete="tel" maxLength={15} defaultValue={profile.phone ?? ''} />
          </label>
          <label className="form-label">Address line 1
            <input className="form-input" name="addressLine1" autoComplete="address-line1" maxLength={200}
              defaultValue={profile.addressLine1 ?? ''} />
          </label>
          <label className="form-label">Address line 2
            <input className="form-input" name="addressLine2" autoComplete="address-line2" maxLength={200}
              defaultValue={profile.addressLine2 ?? ''} />
          </label>
          <StateDistrictFields prefix="profile-residence" reference={reference} stateId={stateId} districtId={districtId} required
            onChange={(state, district) => { setStateId(state); setDistrictId(district); }} />
          <div>
            <label className="form-label" htmlFor="profile-pincode">PIN code *</label>
            <input className="form-input" id="profile-pincode" name="pincode" autoComplete="postal-code" inputMode="numeric" maxLength={6}
              pattern="[1-9][0-9]{5}" required defaultValue={profile.pincode ?? ''} aria-describedby="profile-pin-hint" />
            <p id="profile-pin-hint" className="mt-1 text-xs text-slate-600">Six digits, starting with 1–9.</p>
          </div>
          {!districtChanged && (data.blockName || data.locationName) && <div className="text-sm text-slate-700">
            {data.blockName && <p><strong>Recorded block</strong> {data.blockName}</p>}
            {data.locationName && <p><strong>Recorded locality</strong> {data.locationName}</p>}
          </div>}
        </div>
      </fieldset>
      {error && <div className="rounded-md bg-red-50 p-3 text-sm text-red-800" role="alert">
        <p>{error.message}</p>
        {error.fieldErrors?.length ? <ul className="mt-1 list-disc pl-5">
          {error.fieldErrors.map(item => <li key={item.field + item.message}>{item.message}</li>)}
        </ul> : null}
      </div>}
      <div className="flex flex-wrap gap-3">
        <button type="submit" className="btn-primary" disabled={saving}>{saving ? 'Saving…' : 'Save profile'}</button>
        <button type="button" className="btn-outline" onClick={reset} disabled={saving}>Reset form</button>
      </div>
    </form>
  );
}

function TrainingHistory({ traineeId }: { traineeId: number }) {
  const history = useQuery({
    queryKey: ['trainee', 'enrollments', traineeId],
    queryFn: () => getTraineeEnrollments(traineeId),
    retry: false,
  });
  return (
    <section className="card space-y-3 p-5 sm:p-6" aria-labelledby="profile-training-history">
      <h3 className="section-title" id="profile-training-history">Training history</h3>
      {history.isPending ? <LoadingSpinner label="Loading training history" /> :
        history.isError ? <ErrorState title="Could not load training history" error={history.error}
          onRetry={() => void history.refetch()} /> :
          history.data.content.length === 0 ? <p className="text-sm text-slate-600">No training enrollments recorded.</p> :
            <ul className="divide-y">
              {history.data.content.slice(0, 3).map(enrollment =>
                <li key={enrollment.id} className="py-2 text-sm">
                  <p className="font-semibold">{enrollment.courseName || enrollment.batchCode || enrollment.enrollmentNumber}</p>
                  <p className="text-slate-600">{[enrollment.batchCode, enrollment.enrollmentStatusName, enrollment.enrollmentDate]
                    .filter(Boolean).join(' · ')}</p>
                </li>)}
            </ul>}
      <div className="flex flex-wrap gap-4 text-sm font-semibold text-navy underline">
        <Link to="/trainee/training">View training and attendance</Link>
        <Link to="/trainee/assessments">View assessments and certificates</Link>
        <Link to="/trainee/consent">Manage consent</Link>
      </div>
    </section>
  );
}

export function TraineeProfilePage() {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [formVersion, setFormVersion] = useState(0);
  const [saved, setSaved] = useState(false);
  const key = ['trainee', 'own-profile', user?.id] as const;
  const profile = useQuery<TraineeSelfProfile, ApiError>({
    queryKey: key, queryFn: ({ signal }) => getOwnTraineeProfile(signal),
    enabled: Boolean(user?.id) && !simulationActive, retry: false, refetchOnWindowFocus: 'always',
  });
  const reference = useQuery({
    queryKey: ['registration-reference-data'], queryFn: getRegistrationReferenceData, enabled: !simulationActive,
  });
  const update = useMutation<TraineeSelfProfile, ApiError, UpdateOwnTraineeProfile>({
    mutationFn: updateOwnTraineeProfile,
    onMutate: async () => {
      await Promise.all([
        queryClient.cancelQueries({ queryKey: key, exact: true }),
        queryClient.cancelQueries({ queryKey: ['trainee', 'identity', user?.id], exact: true }),
      ]);
    },
    onSuccess: data => {
      queryClient.setQueryData(key, data);
      queryClient.setQueryData(['trainee', 'identity', user?.id], data.profile);
      setSaved(true);
      setFormVersion(version => version + 1);
    },
  });

  if (simulationActive) return <p className="card p-5" role="status">Switch to live mode to edit your profile.</p>;
  if (profile.isPending || reference.isPending) return <LoadingSpinner label="Loading your profile" />;
  if (profile.isError) {
    if (profile.error.status === 404) {
      return <EmptyState title="Profile not linked" description="Contact your training coordinator to link your trainee registration." />;
    }
    return <ErrorState title="Could not load your profile" error={profile.error} onRetry={() => void profile.refetch()} />;
  }
  if (reference.isError) return <ErrorState title="Could not load profile options" error={reference.error}
    onRetry={() => void reference.refetch()} />;

  return (
    <div className="space-y-5">
      <section className="card space-y-4 border-l-4 border-l-navy p-5 sm:p-6">
        <h2 className="text-2xl font-bold text-navy">My Profile</h2>
        <dl className="grid gap-4 break-words text-sm sm:grid-cols-2 lg:grid-cols-3">
          <div><dt className="text-slate-600">Registration number</dt><dd className="font-semibold">{profile.data.profile.registrationNumber}</dd></div>
          <div><dt className="text-slate-600">Username</dt><dd className="font-semibold">{user?.username}</dd></div>
          <div><dt className="text-slate-600">Sign-in email</dt><dd className="font-semibold">{user?.email || 'Unavailable'}</dd></div>
          <div><dt className="text-slate-600">Profile status</dt><dd className="font-semibold">{profile.data.profileStatus}</dd></div>
          {!profile.data.reportedUpdate && <div><dt className="text-slate-600">Recorded employment status</dt>
            <dd className="font-semibold">{profile.data.employmentStatus}</dd></div>}
        </dl>
        {profile.data.reportedUpdate && <ReportedCareer update={profile.data.reportedUpdate} />}
      </section>
      <OwnCareerSkills traineeId={profile.data.profile.id} edit />
      {saved && <p className="rounded-md bg-emerald-50 p-3 text-sm text-emerald-900" role="status">Profile saved.</p>}
      <PersonalDetailsForm key={formVersion} data={profile.data} reference={reference.data} saving={update.isPending} error={update.error}
        save={data => { setSaved(false); update.mutate(data); }}
        reset={() => { setSaved(false); update.reset(); setFormVersion(version => version + 1); }} />
      <TrainingHistory traineeId={profile.data.profile.id} />
    </div>
  );
}
