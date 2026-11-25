import { useState, type FormEvent } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link, useOutletContext } from 'react-router-dom';
import { getRegistrationReferenceData } from '@/api/registrationApi';
import { availableSeatLabel, findOpenTraining, trainingRegistrationKey, type TrainingSearch } from '@/api/trainingRegistrationApi';
import { StateDistrictFields } from '@/components/common/StateDistrictFields';
import { ErrorState } from '@/components/feedback/ErrorState';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { careerDate } from '@/components/career/CareerInsights';
import { simulationActive } from '@/lib/demoEnvironment';

const emptyFilters = { search: '', stateId: 0, districtId: 0, from: '', through: '' };

export function TraineeFindTrainingPage() {
  const { traineeId, isLoading } = useOutletContext<{ traineeId?: number; isLoading: boolean }>();
  if (isLoading) return <LoadingSpinner label="Loading trainee profile" />;
  if (simulationActive) return <p className="card p-5">Training registration is available in live mode.</p>;
  if (!traineeId) return <p className="card p-5">A linked trainee profile is required to register for training.</p>;

  return <TrainingSearchResults traineeId={traineeId} />;
}

function TrainingSearchResults({ traineeId }: { traineeId: number }) {
  const [draft, setDraft] = useState(emptyFilters);
  const [filters, setFilters] = useState<TrainingSearch>({ page: 0 });
  const reference = useQuery({ queryKey: ['registration', 'reference-data'], queryFn: getRegistrationReferenceData });
  const upcoming = useQuery({
    queryKey: [...trainingRegistrationKey, 'catalog', traineeId, filters],
    queryFn: ({ signal }) => findOpenTraining(filters, signal),
    retry: false,
  });

  function search(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setFilters({ search: draft.search.trim() || undefined, stateId: draft.stateId || undefined,
      districtId: draft.districtId || undefined, from: draft.from || undefined, through: draft.through || undefined, page: 0 });
  }

  return (
    <div className="space-y-5">
      <header>
        <h1 className="page-title">Find training</h1>
        <p className="text-sm text-slate-600">Search upcoming batches with approved venues and open registration. Results can include related words and close spellings.</p>
      </header>
      <form className="card space-y-3 p-5" onSubmit={search} aria-label="Filter training">
        <label className="form-label">Search by title or skill
          <input type="search" className="form-input" value={draft.search} maxLength={120} placeholder="e.g. solar wiring"
            onChange={event => setDraft({ ...draft, search: event.target.value })} />
        </label>
        <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
          <StateDistrictFields prefix="find-training" reference={reference.data ?? null}
            stateId={draft.stateId} districtId={draft.districtId}
            onChange={(stateId, districtId) => setDraft({ ...draft, stateId, districtId })}
            disabled={!reference.data} statePlaceholder="All states and territories" districtPlaceholder="All districts" />
          <label className="form-label">Training starts from
            <input type="date" className="form-input" value={draft.from} max={draft.through || undefined}
              onChange={event => setDraft({ ...draft, from: event.target.value })} />
          </label>
          <label className="form-label">Training starts through
            <input type="date" className="form-input" value={draft.through} min={draft.from || undefined}
              onChange={event => setDraft({ ...draft, through: event.target.value })} />
          </label>
        </div>
        {reference.isError && <p role="alert" className="text-sm text-red-800">
          District filters could not load. <button type="button" className="underline" onClick={() => void reference.refetch()}>Retry</button>
        </p>}
        <div className="flex flex-wrap gap-2">
          <button className="btn-primary" type="submit">Search training</button>
          <button className="btn-secondary" type="button" onClick={() => { setDraft(emptyFilters); setFilters({ page: 0 }); }}>Clear filters</button>
        </div>
      </form>
      {upcoming.isPending ? <LoadingSpinner label="Loading open training" /> : upcoming.isError ? (
        <ErrorState title="Could not load open training" error={upcoming.error} onRetry={() => void upcoming.refetch()} />
      ) : upcoming.data.batches.length === 0 ? (
        <p className="card p-5 text-sm text-slate-600">No open training batches match these filters.</p>
      ) : (
        <ul className="grid gap-4 lg:grid-cols-2" aria-label="Open training batches">
          {upcoming.data.batches.map(batch => (
            <li key={batch.id} className="card space-y-3 p-5">
              <div>
                <p className="text-sm text-slate-600">{batch.governmentLed ? 'Government-led' : 'Provider-led'} · {batch.code}</p>
                <h2 className="section-title"><Link className="hover:underline" to={`/trainee/find-training/${batch.id}`}>{batch.title}</Link></h2>
              </div>
              <p className="text-sm">{batch.skills}</p>
              <p className="text-sm text-slate-600">{batch.provider} · {batch.venue} · {batch.district}, {batch.state}</p>
              <p className="text-sm">{careerDate(batch.startsOn)}{batch.endsOn && batch.startsOn !== batch.endsOn && ` to ${careerDate(batch.endsOn)}`}</p>
              <p className="text-sm font-medium">{availableSeatLabel(batch.seatsAvailable)}</p>
            </li>
          ))}
        </ul>
      )}
      {(filters.page > 0 || upcoming.data?.hasNext) && (
        <nav className="flex items-center gap-3" aria-label="Training results pages">
          <button type="button" className="btn-secondary" disabled={filters.page === 0 || upcoming.isFetching}
            onClick={() => setFilters({ ...filters, page: filters.page - 1 })}>Previous</button>
          <span className="text-sm">Page {filters.page + 1}</span>
          <button type="button" className="btn-secondary" disabled={!upcoming.data?.hasNext || upcoming.isFetching}
            onClick={() => setFilters({ ...filters, page: filters.page + 1 })}>Next</button>
        </nav>
      )}
    </div>
  );
}
