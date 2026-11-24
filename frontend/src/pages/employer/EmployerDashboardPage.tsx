import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { getEmployerPlacements, getEmployerVacancies, getEmployerVerifications } from '@/api/employerApi';
import { ErrorState } from '@/components/feedback/ErrorState';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { EmployerPageHeading, useEmployerReferences } from './EmployerUi';

export function EmployerDashboardPage() {
  const reference = useEmployerReferences();
  const vacancies = useQuery({
    queryKey: ['employer', 'vacancies', 0],
    queryFn: ({ signal }) => getEmployerVacancies(0, signal),
    enabled: reference.isSuccess,
  });
  const placements = useQuery({
    queryKey: ['employer', 'placements', 0],
    queryFn: ({ signal }) => getEmployerPlacements(0, signal),
    enabled: reference.isSuccess,
  });
  const verification = useQuery({
    queryKey: ['employer', 'verifications', 'pending', 0],
    queryFn: ({ signal }) => getEmployerVerifications('pending', 0, signal),
    enabled: reference.isSuccess,
  });

  return (
    <div className="space-y-5">
      <EmployerPageHeading title="Employer portal" />
      {reference.isPending ? <LoadingSpinner label="Loading employer profile" /> : reference.isError ? (
        <ErrorState error={reference.error} title="Unable to load the employer profile" onRetry={() => void reference.refetch()} />
      ) : (
        <>
          <h2 className="text-lg font-semibold text-slate-800">{reference.data.employerName}</h2>
          <div className="grid gap-4 sm:grid-cols-3">
            {[
              { label: 'Job vacancies', path: 'vacancies', query: vacancies },
              { label: 'Candidate placements', path: 'placements', query: placements },
              { label: 'Open verification requests', path: 'verifications', query: verification },
            ].map(({ label, path, query }) => (
              <Link key={path} to={`/employer/${path}`} className="card block space-y-2 border-l-4 border-l-navy hover:bg-slate-50 focus-visible:outline-2 focus-visible:outline-navy">
                <h3 className="text-sm font-medium text-slate-700">{label}</h3>
                <p className="text-2xl font-bold text-navy">
                  {query.isPending ? '…' : query.isError ? <span className="text-sm">Unavailable</span> : query.data.totalElements}
                </p>
              </Link>
            ))}
          </div>
        </>
      )}
    </div>
  );
}
