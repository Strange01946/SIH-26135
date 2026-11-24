import { useState, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { getTrainingCenters } from '@/api/providerPortalApi';
import { getRegistrationReferenceData } from '@/api/registrationApi';
import { getCenterStatuses, removeTrainingCenter } from '@/api/trainingCenterApi';
import type { ApiError } from '@/api/types';
import { Modal } from '@/components/common/Modal';
import { StateDistrictFields } from '@/components/common/StateDistrictFields';
import { EmptyState } from '@/components/feedback/EmptyState';
import { ErrorState } from '@/components/feedback/ErrorState';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { useAuth } from '@/context/AuthContext';
import { simulationActive } from '@/lib/demoEnvironment';
import type { TrainingCenter } from '@/types/provider';
import { TrainingCenterForm, centerActionError } from './TrainingCenterForm';

const pageSize = 10;

export function ProviderCentersPage({ admin = false }: { admin?: boolean }) {
  const { user, hasAuthority } = useAuth();
  const queryClient = useQueryClient();
  const providerId = admin ? undefined : user?.trainingProviderId ?? undefined;
  const canManage = !simulationActive && hasAuthority('center.manage') && (admin || Boolean(providerId));

  const [page, setPage] = useState(0);
  const [searchCode, setSearchCode] = useState('');
  const [code, setCode] = useState('');
  const [stateId, setStateId] = useState(0);
  const [districtId, setDistrictId] = useState(0);
  const [editor, setEditor] = useState<TrainingCenter | null>(null);
  const [removal, setRemoval] = useState<TrainingCenter | null>(null);
  const [notice, setNotice] = useState('');

  const geography = useQuery({
    queryKey: ['registration-reference-data'],
    queryFn: getRegistrationReferenceData,
    enabled: canManage || simulationActive,
  });
  const statuses = useQuery({
    queryKey: ['training-centers', 'lifecycle-statuses'],
    queryFn: ({ signal }) => getCenterStatuses(signal),
    enabled: canManage,
  });
  const centers = useQuery({
    queryKey: ['training-centers', 'directory', user?.id, providerId, page, code, stateId, districtId],
    queryFn: () => getTrainingCenters({
      providerId, code: code || undefined, districtId: districtId || undefined,
      stateId: stateId || undefined, page, size: pageSize, sort: 'id', direction: 'DESC',
    }),
    enabled: admin || Boolean(providerId),
  });
  const rows = (centers.data?.content ?? []).filter((center) => !center.deletedAt
    && (!code || ((!stateId || center.stateId === stateId) && (!districtId || center.districtId === districtId))));
  const totalPages = centers.data?.totalPages ?? 0;
  const editingAvailable = Boolean(geography.data && statuses.data?.length);
  const remove = useMutation<void, ApiError, TrainingCenter>({
    mutationFn: (center: TrainingCenter) => removeTrainingCenter(center.id),
    onSuccess: (_, center) => {
      setRemoval(null);
      setCode('');
      setSearchCode('');
      setPage(rows.length === 1 ? Math.max(0, page - 1) : page);
      setNotice(`${center.centerName} was removed from the directory.`);
      refreshCenters(!code);
    },
  });

  function refreshCenters(refetchDirectory = true) {
    void queryClient.invalidateQueries({ queryKey: ['training-centers', 'directory'], refetchType: refetchDirectory ? 'active' : 'none' });
    void queryClient.invalidateQueries({ queryKey: ['provider', 'centers'] });
    void queryClient.invalidateQueries({ queryKey: ['provider-centers-filter'] });
    void queryClient.invalidateQueries({ queryKey: ['provider-centers-scorecard-list'] });
  }

  function search(event: FormEvent) {
    event.preventDefault();
    setCode(searchCode.trim());
    setPage(0);
  }

  function clearFilters() {
    setSearchCode('');
    setCode('');
    setStateId(0);
    setDistrictId(0);
    setPage(0);
  }

  function saved(center: TrainingCenter) {
    setNotice(`${center.centerName} was updated.`);
    setEditor(null);
    refreshCenters();
  }

  return (
    <div className="space-y-5">
      <header className="card flex flex-wrap items-center justify-between gap-4 border-l-4 border-l-navy p-6">
        <div>
          <h1 className="text-2xl font-bold text-navy">Training Centers Directory</h1>
          <p className="mt-1 text-sm text-slate-600">
            {admin ? 'Review training locations by district and manage their operational records.'
              : 'Review temporary training sites approved for your provider.'}
          </p>
        </div>
        {canManage && admin && <Link className="btn-primary" to="/admin/venue-bookings?view=shared">Manage shared centers</Link>}
        {!admin && hasAuthority('venue.booking.request') && <Link className="btn-primary"
          to="/provider/venue-bookings?view=shared">Book a training venue</Link>}
      </header>

      {simulationActive && <p className="text-sm text-slate-700" role="status">Switch to live mode to manage training centers.</p>}
      {notice && <p className="rounded-md border border-emerald-200 bg-emerald-50 p-3 text-sm text-emerald-900" role="status">{notice}</p>}
      {!admin && !providerId && <EmptyState title="Provider assignment required"
        description="Your account must be linked to a training provider before its centers can be managed." />}
      {canManage && !editingAvailable && (
        geography.isError || statuses.isError ? (
          <ErrorState title="Could not load center reference data" error={geography.error ?? statuses.error}
            onRetry={() => { void geography.refetch(); void statuses.refetch(); }} />
        ) : geography.isPending || statuses.isPending ? (
          <p className="text-sm text-slate-600" role="status">Loading state, district and status options…</p>
        ) : <p className="text-sm text-slate-600" role="status">No operational status options are available.</p>
      )}

      {(admin || providerId) && (
        <>
          <form className="card space-y-4 p-4" onSubmit={search}>
            {admin && <div className="grid gap-4 sm:grid-cols-2">
              <StateDistrictFields prefix="centers-filter" reference={geography.data ?? null} stateId={stateId} districtId={districtId}
                disabled={!geography.data} onChange={(state, district) => { setStateId(state); setDistrictId(district); setPage(0); }} />
            </div>}
            <div className="flex flex-wrap items-end gap-3">
              <div className="min-w-48 flex-1">
                <label htmlFor="centers-search" className="form-label">Center code</label>
                <input id="centers-search" className="form-input" maxLength={32} value={searchCode}
                  onChange={(event) => setSearchCode(event.target.value)} />
              </div>
              <button type="submit" className="btn-primary">Search</button>
              {(code || stateId || districtId) && <button type="button" className="btn-outline" onClick={clearFilters}>Clear filters</button>}
              {centers.data && <p className="text-sm text-slate-600">Total centers {code ? rows.length : centers.data.totalElements}</p>}
            </div>
          </form>

          {centers.isPending ? <LoadingSpinner label="Loading centers" /> : centers.isError ? (
            <ErrorState title="Could not load training centers" error={centers.error} onRetry={() => { void centers.refetch(); }} />
          ) : rows.length === 0 ? (
            <EmptyState title="No Training Centers Found"
              description={code || stateId ? 'Clear or change the filters to search again.' : 'Training centers appear here when they are registered.'} />
          ) : (
            <section className="card space-y-4 p-4" aria-label="Training centers">
              <div className="overflow-x-auto">
                <table className="w-full text-left text-sm">
                  <thead className="border-b border-slate-200 bg-slate-50 text-slate-700">
                    <tr>
                      <th scope="col" className="p-3">Center</th>
                      {admin && <th scope="col" className="p-3">Training provider</th>}
                      <th scope="col" className="p-3">State / district</th>
                      <th scope="col" className="p-3">Capacity</th>
                      <th scope="col" className="p-3">Status</th>
                      {canManage && <th scope="col" className="p-3">Actions</th>}
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {rows.map((center) => {
                      const status = statuses.data?.find(({ id }) => id === center.lifecycleStatusId);
                      const district = geography.data?.districts.find(({ id }) => id === center.districtId)?.name;
                      const state = geography.data?.states.find(({ id }) => id === center.stateId)?.name;
                      return (
                        <tr key={center.id}>
                          <th scope="row" className="p-3 font-medium text-slate-900">
                            {center.centerName}
                            <span className="block font-mono text-xs text-navy">{center.centerCode}</span>
                            {center.addressLine1 && <span className="block text-xs font-normal text-slate-600">{center.addressLine1}</span>}
                          </th>
                          {admin && <td className="p-3">{center.venueType === 'SHARED' ? 'Shared center'
                            : center.providerName ?? center.providerCode ?? `Provider ${center.providerId}`}</td>}
                          <td className="p-3">
                            {state ?? center.stateName ?? `State or territory ${center.stateId}`}
                            <span className="block text-xs text-slate-600">{district ?? center.districtName ?? `District ${center.districtId}`}</span>
                          </td>
                          <td className="p-3">{center.operationalCapacity?.toLocaleString('en-IN') ?? '—'}</td>
                          <td className="p-3">{status?.name ?? center.lifecycleStatusCode ?? (center.lifecycleStatusId ? `Status ${center.lifecycleStatusId}` : '—')}</td>
                          {canManage && <td className="p-3">{center.venueType === 'TEMPORARY' ? (
                            <Link className="text-navy underline" to={admin ? '/admin/venue-bookings' : '/provider/venue-bookings'}>View venue bookings</Link>
                          ) : (
                            <div className="flex flex-wrap gap-2">
                              <button type="button" className="btn-outline" aria-label={`Edit ${center.centerName}`} disabled={!editingAvailable}
                                onClick={() => { setNotice(''); setEditor(center); }}>Edit</button>
                              <button type="button" className="btn-outline text-red-700" aria-label={`Remove ${center.centerName}`}
                                onClick={() => { setNotice(''); remove.reset(); setRemoval(center); }}>Remove</button>
                            </div>)}
                          </td>}
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
              {totalPages > 1 && <nav className="flex items-center justify-between gap-2 border-t border-slate-200 pt-4" aria-label="Center directory pages">
                <span className="text-sm text-slate-600">Page {page + 1} of {totalPages}</span>
                <div className="flex gap-2">
                  <button type="button" className="btn-outline" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</button>
                  <button type="button" className="btn-outline" disabled={page + 1 >= totalPages} onClick={() => setPage(page + 1)}>Next</button>
                </div>
              </nav>}
            </section>
          )}
        </>
      )}

      {editor && geography.data && statuses.data && (
        <TrainingCenterForm center={editor}
          reference={geography.data} statuses={statuses.data} onClose={() => setEditor(null)} onSaved={saved} />
      )}
      {removal && (
        <Modal open onClose={() => { if (!remove.isPending) setRemoval(null); }} title="Remove training center">
          <div className="space-y-4 p-6">
            <p>Remove <strong>{removal.centerName}</strong> from the directory? A center with training records can be set to Inactive from Edit.</p>
            {remove.isError && <p role="alert" className="text-sm text-red-700">{centerActionError(remove.error)}</p>}
            <div className="flex justify-end gap-2">
              <button type="button" className="btn-outline" disabled={remove.isPending} onClick={() => setRemoval(null)}>Cancel</button>
              <button type="button" className="btn-primary bg-red-700" disabled={remove.isPending}
                onClick={() => remove.mutate(removal)}>{remove.isPending ? 'Removing…' : 'Remove center'}</button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
}
