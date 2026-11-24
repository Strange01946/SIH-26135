import { useState, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { confirmEmployerPlacement, getEmployerPlacements, type EmployerPlacement } from '@/api/employerApi';
import { Modal } from '@/components/common/Modal';
import { EmptyState } from '@/components/feedback/EmptyState';
import { ErrorState } from '@/components/feedback/ErrorState';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { EmployerFormError, EmployerPageHeading, EmployerPager, EmployerSelect, formatPay, indiaToday, showStatus, useEmployerReferences } from './EmployerUi';

export function EmployerPlacementsPage() {
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState<EmployerPlacement | null>(null);
  const [receipt, setReceipt] = useState('');
  const queryClient = useQueryClient();
  const references = useEmployerReferences();
  const placements = useQuery({
    queryKey: ['employer', 'placements', page],
    queryFn: ({ signal }) => getEmployerPlacements(page, signal),
  });
  const confirm = useMutation({
    mutationFn: ({ id, details }: { id: number; details: Parameters<typeof confirmEmployerPlacement>[1] }) =>
      confirmEmployerPlacement(id, details),
    onSuccess: async (result) => {
      setReceipt(`Joining confirmed · Employment record ${result.employmentRecord.employmentNumber}`);
      setSelected(null);
      await queryClient.invalidateQueries({ queryKey: ['employer', 'placements'] });
    },
  });

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selected || confirm.isPending) return;
    const form = new FormData(event.currentTarget);
    confirm.mutate({ id: selected.id, details: {
      jobRoleId: Number(form.get('jobRoleId')),
      joiningDate: String(form.get('joiningDate')),
      offeredPay: Number(form.get('offeredPay')),
      payFrequencyId: Number(form.get('payFrequencyId')),
    }});
  }

  return (
    <div className="space-y-5">
      <EmployerPageHeading title="Candidate placements" />
      {receipt && <p role="status" className="rounded border border-emerald-200 bg-emerald-50 p-3 text-sm text-emerald-900">{receipt}</p>}
      {placements.isPending ? <LoadingSpinner label="Loading placements" /> : placements.isError ? (
        <ErrorState error={placements.error} title="Unable to load placements" onRetry={() => void placements.refetch()} />
      ) : placements.data.content.length === 0 ? (
        <EmptyState title="No placements yet" description="Candidates appear here when a placement is assigned to your organisation." />
      ) : (
        <>
          <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
            <table className="w-full min-w-[720px] text-left text-sm">
              <thead className="bg-slate-50 text-slate-700">
                <tr>
                  <th scope="col" className="p-3">Candidate</th>
                  <th scope="col" className="p-3">Job role</th>
                  <th scope="col" className="p-3">Offered pay</th>
                  <th scope="col" className="p-3">Placement</th>
                  <th scope="col" className="p-3">Joining date</th>
                  <th scope="col" className="p-3">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200">
                {placements.data.content.map((record) => (
                  <tr key={record.id}>
                    <td className="p-3">
                      <p className="font-semibold text-slate-900">{record.candidateName}</p>
                      <p className="text-xs text-slate-600">{record.number}{record.course && ` · ${record.course}`}</p>
                    </td>
                    <td className="p-3">{record.jobRole || '—'}</td>
                    <td className="p-3">{formatPay(record.offeredPay, record.currency)}
                      <span className="block text-xs text-slate-600">{record.payFrequency}</span></td>
                    <td className="p-3"><span className="badge-info">{showStatus(record.placementStatus)}</span>
                      {record.joiningStatus !== record.placementStatus && <span className="block text-xs text-slate-600">{showStatus(record.joiningStatus)}</span>}</td>
                    <td className="p-3">{record.joinedOn || '—'}</td>
                    <td className="p-3">
                      {record.canConfirm ? (
                        <button type="button" className="btn-outline" onClick={() => { confirm.reset(); setSelected(record); }}>Confirm joining</button>
                      ) : record.employmentId ? <span className="text-xs font-medium text-emerald-800">Employment recorded</span> : '—'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <EmployerPager data={placements.data} onPage={setPage} />
        </>
      )}
      <Modal open={selected !== null} onClose={() => { if (!confirm.isPending) setSelected(null); }} title="Confirm candidate joining">
        {selected && (references.isPending ? <LoadingSpinner label="Loading placement options" /> : references.isError ? (
          <ErrorState error={references.error} title="Unable to load placement options" onRetry={() => void references.refetch()} />
        ) : (
          <form onSubmit={submit} key={selected.id} className="space-y-4 p-6">
            <p className="text-sm text-slate-600">{selected.candidateName} · {selected.number}</p>
            <fieldset disabled={confirm.isPending} className="grid gap-4 sm:grid-cols-2">
              <EmployerSelect name="jobRoleId" label="Job role" options={references.data.jobRoles} required defaultValue={selected.jobRoleId ?? ''} />
              <label className="space-y-1 text-sm font-medium"><span>Joining date</span>
                <input name="joiningDate" type="date" className="form-input" defaultValue={selected.joinedOn ?? ''}
                  min={selected.offeredOn ?? undefined} max={indiaToday()} required /></label>
              <label className="space-y-1 text-sm font-medium"><span>Offered pay ({selected.currency})</span>
                <input name="offeredPay" type="number" className="form-input" defaultValue={selected.offeredPay ?? ''}
                  min={0} max={9999999999.99} step={0.01} required /></label>
              <EmployerSelect name="payFrequencyId" label="Pay frequency" options={references.data.payFrequencies}
                required defaultValue={selected.payFrequencyId ?? ''} />
            </fieldset>
            <p className="text-sm text-slate-600">The entered pay is recorded as the starting wage for this employment.</p>
            {confirm.isError && <EmployerFormError error={confirm.error} />}
            <button className="btn-primary" type="submit" disabled={confirm.isPending}>{confirm.isPending ? 'Recording…' : 'Record joining'}</button>
          </form>
        ))}
      </Modal>
    </div>
  );
}
