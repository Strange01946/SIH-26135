import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getTrainingBatchRoster } from '@/api/trainingRegistrationApi';
import { ErrorState } from '@/components/feedback/ErrorState';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';

export function ProviderBatchRoster({ batchId }: { batchId: number }) {
  const [open, setOpen] = useState(false);
  const [page, setPage] = useState(0);
  const roster = useQuery({
    queryKey: ['provider', 'batch-roster', batchId, page],
    queryFn: ({ signal }) => getTrainingBatchRoster(batchId, page, signal),
    enabled: open, retry: false, refetchInterval: open ? 15_000 : false,
  });

  return (
    <details className="card space-y-3 p-5" onToggle={event => setOpen(event.currentTarget.open)}>
      <summary className="cursor-pointer font-semibold text-navy">Batch roster and contact details</summary>
      {open && (roster.isPending ? <LoadingSpinner label="Loading batch roster" /> : roster.isError ? (
        <ErrorState title="Could not load the batch roster" error={roster.error} onRetry={() => void roster.refetch()} />
      ) : (
        <>
          {roster.data.entries.length === 0 ? <p className="text-sm text-slate-600">No registrations on this page.</p> : (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[28rem] text-left text-sm" aria-label="Batch contact roster">
                <thead className="border-b border-slate-200">
                  <tr><th scope="col" className="p-2">Trainee</th><th scope="col" className="p-2">Enrollment</th>
                    <th scope="col" className="p-2">Contact</th></tr>
                </thead>
                <tbody className="divide-y divide-slate-200">
                  {roster.data.entries.map(entry => (
                    <tr key={entry.enrollmentId}>
                      <th scope="row" className="p-2 font-semibold">{entry.name}<span className="block font-normal">{entry.registrationNumber}</span></th>
                      <td className="p-2">{entry.enrollmentNumber}</td>
                      <td className="p-2">
                        {entry.email && <p><a href={`mailto:${entry.email}`} className="text-navy underline">{entry.email}</a></p>}
                        {entry.phone && <p><a href={`tel:${entry.phone}`} className="text-navy underline">{entry.phone}</a></p>}
                        {!entry.email && !entry.phone && <span className="text-slate-600">No contact shared</span>}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          {(page > 0 || roster.data.hasNext) && (
            <nav className="flex items-center gap-3" aria-label="Roster pages">
              <button type="button" className="btn-secondary" disabled={page === 0 || roster.isFetching}
                onClick={() => setPage(page - 1)}>Previous</button>
              <span className="text-sm">Page {page + 1}</span>
              <button type="button" className="btn-secondary" disabled={!roster.data.hasNext || roster.isFetching}
                onClick={() => setPage(page + 1)}>Next</button>
            </nav>
          )}
        </>
      ))}
    </details>
  );
}
