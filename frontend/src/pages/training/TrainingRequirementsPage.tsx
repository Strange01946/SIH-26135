import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { getRegistrationReferenceData } from '@/api/registrationApi';
import {
  getGovernmentRequirements, getMyTrainingApplications, getOpenRequirements, getTrainingRequirement,
  selectTrainingProvider, trainingRequirementsKey, type TrainingApplication, type TrainingRequirement,
} from '@/api/trainingRequirementApi';
import { StateDistrictFields } from '@/components/common/StateDistrictFields';
import { ErrorState } from '@/components/feedback/ErrorState';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { useAuth } from '@/context/AuthContext';
import { simulationActive } from '@/lib/demoEnvironment';
import { bookingError } from '@/pages/provider/VenueBookingForm';
import { ApplyRequirementDialog, LegacyRequirementCentreDialog, PublishRequirementDialog } from './TrainingRequirementForms';

const pageSize = 20;
const applicationLabels = { APPLIED: 'Submitted', SELECTED: 'Selected', NOT_SELECTED: 'Not selected' };

function timeWindow(start: string, end: string) {
  return `${start.slice(0, 5)}–${end.slice(0, 5)} IST`;
}

function RequirementSummary({ requirement }: { requirement: TrainingRequirement }) {
  return (
    <>
      {requirement.centerName && <p><strong>{requirement.centerName}</strong></p>}
      <p>{requirement.districtName}, {requirement.stateName} · {requirement.seats} seats</p>
      <p>{requirement.earliestDate} to {requirement.latestDate} · {timeWindow(requirement.dailyStartTime, requirement.dailyEndTime)}</p>
    </>
  );
}

function ApplicationSummary({ application }: { application: TrainingApplication }) {
  return (
    <div className="space-y-1 text-sm">
      <p><strong>{applicationLabels[application.status]}</strong> · {application.startDate} to {application.endDate}
        {' · '}{timeWindow(application.dailyStartTime, application.dailyEndTime)}</p>
      {application.deliveryNotes && <p className="whitespace-pre-wrap text-slate-700">{application.deliveryNotes}</p>}
    </div>
  );
}

function RequirementRecord({ id, government, onApply, onAwarded }: {
  id: number; government: boolean; onApply: (requirement: TrainingRequirement) => void; onAwarded: (message: string) => void;
}) {
  const { user } = useAuth();
  const [settingCentre, setSettingCentre] = useState(false);
  const detail = useQuery({ queryKey: [...trainingRequirementsKey, 'detail', user?.id, government, id],
    queryFn: ({ signal }) => getTrainingRequirement(id, government, signal), staleTime: 0 });
  const select = useMutation({ mutationFn: (applicationId: number) => selectTrainingProvider(id, applicationId),
    onSuccess: () => onAwarded('Provider selected and proposed sessions reserved.') });
  if (detail.isPending) return <LoadingSpinner label="Loading requirement" />;
  if (detail.isError) return <ErrorState title="Could not load the training requirement" error={detail.error} onRetry={() => void detail.refetch()} />;
  const { requirement, applications, myApplication } = detail.data;
  const assigned = !government && myApplication?.status === 'SELECTED';
  return (
    <section className="card space-y-4 p-5" aria-label="Training requirement details">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="section-title">{requirement.title}</h2>
          <div className="text-sm text-slate-700"><RequirementSummary requirement={requirement} /></div>
          {requirement.centerAddress && <p className="mt-1 text-sm text-slate-700">{requirement.centerAddress}</p>}
        </div>
        <span className="badge-info">{requirement.status === 'AWARDED' ? 'Awarded' : !requirement.centerId ? 'Center required'
          : requirement.acceptingApplications ? 'Open' : 'Window closed'}</span>
      </div>
      <div className="text-sm">
        <h3 className="font-semibold">Skills to teach</h3>
        <p className="whitespace-pre-wrap">{requirement.skillsToTeach}</p>
      </div>
      {requirement.description && <div className="text-sm">
        <h3 className="font-semibold">Delivery requirements</h3>
        <p className="whitespace-pre-wrap">{requirement.description}</p>
      </div>}
      {government ? (
        <div className="space-y-2 border-t pt-4">
          {requirement.selectedProviderName && <p className="text-sm"><strong>Selected provider</strong> {requirement.selectedProviderName}</p>}
          {requirement.bookingId && <Link className="btn-outline inline-flex"
            to={`/admin/venue-bookings?booking=${requirement.bookingId}`}>View venue booking</Link>}
          {!requirement.centerId && !requirement.bookingId && <button type="button" className="btn-outline"
            onClick={() => setSettingCentre(true)}>Assign training center</button>}
          <h3 className="font-semibold">Provider applications</h3>
          {applications.length === 0 ? <p className="text-sm text-slate-600">No applications yet.</p>
            : <ul className="divide-y">{applications.map(application => (
              <li key={application.id} className="flex flex-wrap items-center justify-between gap-3 py-3">
                <div>
                  <h4 className="font-semibold">{application.providerName}</h4>
                  <ApplicationSummary application={application} />
                </div>
                {requirement.acceptingApplications && application.status === 'APPLIED' && (
                  <button type="button" className="btn-primary" disabled={select.isPending} onClick={() => {
                    if (window.confirm(`Select ${application.providerName} and reserve their proposed sessions at ${requirement.centerName}? Other applications will be marked Not selected.`))
                      select.mutate(application.id);
                  }}>Select provider</button>
                )}
              </li>
            ))}</ul>}
          {select.error && <p role="alert" className="text-sm text-red-800">{bookingError(select.error)}</p>}
          {settingCentre && <LegacyRequirementCentreDialog requirement={requirement} onClose={() => setSettingCentre(false)}
            onSaved={saved => {
              setSettingCentre(false);
              onAwarded(saved.bookingId ? 'Training center assigned and proposed sessions reserved.' : 'Training center assigned.');
            }} />}
        </div>
      ) : (
        <div className="space-y-3 border-t pt-4">
          {myApplication && <ApplicationSummary application={myApplication} />}
          {!myApplication && requirement.acceptingApplications && (
            <button type="button" className="btn-primary" onClick={() => onApply(requirement)}>Apply to deliver training</button>
          )}
          {assigned && (requirement.bookingId ? (
            <Link className="btn-outline inline-flex" to={`/provider/venue-bookings?booking=${requirement.bookingId}`}>View venue booking</Link>
          ) : (
            <p role="status" className="text-sm">The reviewer needs to confirm the training center.</p>
          ))}
        </div>
      )}
    </section>
  );
}

export function TrainingRequirementsPage({ government = false }: { government?: boolean }) {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<'open' | 'applications'>('open');
  const [page, setPage] = useState(0);
  const [state, setState] = useState(0);
  const [district, setDistrict] = useState(0);
  const [selected, setSelected] = useState<number | null>(null);
  const [publishing, setPublishing] = useState(false);
  const [applying, setApplying] = useState<TrainingRequirement | null>(null);
  const [notice, setNotice] = useState('');
  const viewingApplications = !government && tab === 'applications';
  const reference = useQuery({ queryKey: ['registration-reference-data'], queryFn: getRegistrationReferenceData, enabled: !simulationActive });
  const requirements = useQuery({ queryKey: [...trainingRequirementsKey, 'list', user?.id, government, page, state, district],
    queryFn: ({ signal }) => government ? getGovernmentRequirements(page, pageSize, signal) : getOpenRequirements(page, pageSize, state, district, signal),
    enabled: !simulationActive && !viewingApplications, staleTime: 0 });
  const applications = useQuery({ queryKey: [...trainingRequirementsKey, 'applications', user?.id, page],
    queryFn: ({ signal }) => getMyTrainingApplications(page, pageSize, signal),
    enabled: !simulationActive && viewingApplications, staleTime: 0 });
  const list = viewingApplications ? applications : requirements;

  function refresh() {
    void queryClient.invalidateQueries({ queryKey: trainingRequirementsKey });
  }

  return (
    <div className="space-y-5">
      <header className="card flex flex-wrap items-center justify-between gap-3 border-l-4 border-l-navy p-6">
        <div>
          <h1 className="text-2xl font-bold text-navy">{government ? 'Training Requirements' : 'Training Opportunities'}</h1>
          <p className="mt-1 text-sm text-slate-600">{government ? 'Publish training needs, compare applications and assign delivery.'
            : 'Propose sessions for government-requested training at the published center.'}</p>
        </div>
        {government && !simulationActive && <button type="button" className="btn-primary" disabled={!reference.data}
          onClick={() => setPublishing(true)}>New requirement</button>}
      </header>
      {simulationActive ? <p role="status" className="card p-5">Switch to live mode to manage training requirements.</p> : <>
        {notice && <p role="status" className="rounded border border-emerald-200 bg-emerald-50 p-3 text-sm text-emerald-900">{notice}</p>}
        {reference.isError && <ErrorState title="Could not load locations" error={reference.error} onRetry={() => void reference.refetch()} />}
        <section className="card space-y-4 p-5" aria-label={government ? 'Government requirements' : 'Training opportunities list'}>
          <div className="flex flex-wrap justify-between gap-2">
            {!government && <div className="flex gap-2">
              <button type="button" className={viewingApplications ? 'btn-outline' : 'btn-primary'} aria-pressed={!viewingApplications}
                onClick={() => { setTab('open'); setPage(0); setSelected(null); }}>Open requirements</button>
              <button type="button" className={viewingApplications ? 'btn-primary' : 'btn-outline'} aria-pressed={viewingApplications}
                onClick={() => { setTab('applications'); setPage(0); setSelected(null); }}>My applications</button>
            </div>}
            <button type="button" className="btn-outline" onClick={refresh}>Refresh</button>
          </div>
          {!government && !viewingApplications && reference.data && <div className="grid gap-3 sm:grid-cols-2">
            <StateDistrictFields prefix="opportunity-filter" reference={reference.data} stateId={state} districtId={district}
              onChange={(newState, newDistrict) => { setState(newState); setDistrict(newDistrict); setPage(0); setSelected(null); }} />
          </div>}
          {list.isPending ? <LoadingSpinner label="Loading training requirements" />
            : list.isError ? <ErrorState title="Could not load training requirements" error={list.error} onRetry={() => void list.refetch()} />
              : list.data?.length === 0 ? <p className="text-sm text-slate-600">{viewingApplications ? 'You have no applications yet.' : 'No training requirements match this view.'}</p>
                : <ul className="divide-y">{viewingApplications ? applications.data?.map(application => (
                  <li key={application.id} className="flex flex-wrap items-center justify-between gap-2 py-3">
                    <div><p className="font-semibold">{application.requirementTitle}</p><ApplicationSummary application={application} /></div>
                    {application.status === 'SELECTED' && <button type="button" className="btn-outline"
                      onClick={() => setSelected(application.requirementId)}>View assignment</button>}
                  </li>
                )) : requirements.data?.map(requirement => (
                  <li key={requirement.id} className="flex flex-wrap items-center justify-between gap-2 py-3">
                    <div>
                      <p className="font-semibold">{requirement.title}</p>
                      <p className="whitespace-pre-wrap text-sm text-slate-700">{requirement.skillsToTeach}</p>
                      <div className="text-sm text-slate-600"><RequirementSummary requirement={requirement} /></div>
                      {government && requirement.selectedProviderName && <p className="text-sm">Awarded to {requirement.selectedProviderName}</p>}
                    </div>
                    <button type="button" className="btn-outline" onClick={() => setSelected(requirement.id)}>View requirement</button>
                  </li>
                ))}</ul>}
          {(page > 0 || list.data?.length === pageSize) && <nav aria-label="Requirement pages" className="flex gap-2">
            <button type="button" className="btn-outline" disabled={page === 0} onClick={() => { setPage(page - 1); setSelected(null); }}>Previous page</button>
            <button type="button" className="btn-outline" disabled={list.data?.length !== pageSize}
              onClick={() => { setPage(page + 1); setSelected(null); }}>Next page</button>
          </nav>}
        </section>
        {selected && <RequirementRecord id={selected} key={selected} government={government} onApply={setApplying}
          onAwarded={message => {
            setNotice(message);
            refresh();
            void queryClient.invalidateQueries({ queryKey: ['venue-bookings'] });
            void queryClient.invalidateQueries({ queryKey: ['provider', 'batches'] });
          }} />}
        {publishing && reference.data && <PublishRequirementDialog reference={reference.data} state={user?.stateId ?? 0} district={user?.districtId ?? 0}
          onClose={() => setPublishing(false)} onSaved={id => {
            setPublishing(false); setPage(0); setSelected(id); setNotice('Requirement published.'); refresh();
          }} />}
        {applying && <ApplyRequirementDialog requirement={applying} onClose={() => setApplying(null)}
          onSaved={() => { setApplying(null); setNotice('Application submitted.'); refresh(); }} />}
      </>}
    </div>
  );
}
