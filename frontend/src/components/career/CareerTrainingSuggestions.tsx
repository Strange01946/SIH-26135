import { useEffect, useRef, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useLocation } from 'react-router-dom';
import { careerSkillsKey, getCareerTraining, saveCareerTrainingChoice, type TrainingChoice } from '@/api/careerSkillsApi';
import { ErrorState } from '@/components/feedback/ErrorState';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { simulationActive } from '@/lib/demoEnvironment';
import { careerDate } from './CareerInsights';

export function CareerTrainingSuggestions({ traineeId }: { traineeId: number }) {
  const [input, setInput] = useState('');
  const [search, setSearch] = useState('');
  const { hash } = useLocation();
  const section = useRef<HTMLElement>(null);
  const queryClient = useQueryClient();
  const training = useQuery({ queryKey: [...careerSkillsKey, 'training', traineeId, search],
    queryFn: ({ signal }) => getCareerTraining(search, signal), enabled: !simulationActive,
    retry: false, refetchOnMount: 'always', refetchOnWindowFocus: 'always' });
  const save = useMutation({ mutationFn: ({ batchId, choice }: { batchId: number; choice: TrainingChoice }) =>
    saveCareerTrainingChoice(batchId, choice),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [...careerSkillsKey, 'training', traineeId] }) });

  useEffect(() => {
    if (hash === '#training-suggestions') section.current?.focus();
  }, [hash]);

  if (simulationActive) return null;
  return (
    <section ref={section} id="training-suggestions" tabIndex={-1} className="card scroll-mt-24 space-y-4 p-5"
      aria-label="Upcoming training suggestions">
      <h2 className="section-title">Upcoming training suggestions</h2>
      <form className="flex flex-wrap items-end gap-2" onSubmit={event => { event.preventDefault(); setSearch(input.trim()); }}>
        <label className="form-label mb-0 min-w-48 flex-1">Search upcoming training by skill or location
          <input className="form-input" value={input} maxLength={120} onChange={event => setInput(event.target.value)} />
        </label>
        <button type="submit" className="btn-primary">Search training</button>
        {search && <button type="button" className="btn-secondary" onClick={() => { setInput(''); setSearch(''); }}>Show my matches</button>}
      </form>
      {training.isPending ? <LoadingSpinner label="Looking for upcoming training" /> : training.isError ? (
        <ErrorState error={training.error} title="Could not load upcoming training" onRetry={() => void training.refetch()} />
      ) : training.data.length === 0 ? (
        <p className="text-sm text-slate-600">{search ? 'No upcoming training matches this search.' : 'No upcoming sessions match your recorded skill gaps. You can search by skill or location.'}</p>
      ) : (
        <ul className="grid gap-3 lg:grid-cols-2">
          {training.data.map(session => <li key={session.batchId} className="space-y-2 rounded border border-slate-200 p-4 text-sm">
            <h3 className="font-semibold text-navy">{session.topic}</h3>
            <p>{session.provider} · {session.venue} · {session.district}, {session.state}</p>
            <p>{careerDate(session.startsOn)}{session.endsOn && session.startsOn !== session.endsOn && ` to ${careerDate(session.endsOn)}`}
              {session.firstSession && ` · First session ${session.firstSession.slice(11, 16)} IST`}</p>
            <p><strong>Skills taught</strong> · {session.skillsToTeach}</p>
            {session.matchedSkills.length > 0 && <p className="text-navy"><strong>Matches your gaps</strong> · {session.matchedSkills.join(', ')}</p>}
            {session.relatedToJobGoal && <p className="text-navy">Related to your desired job</p>}
            <p>{session.status === 'OPEN' ? 'Enrollment open' : 'Planned'} · {session.seatsAvailable} seats available</p>
            {session.firstSession && <Link className="inline-block text-navy underline"
              to={`/trainee/find-training/${session.batchId}`}>{session.enrolled ? 'View timetable' : 'View batch details'}</Link>}
            {session.enrolled ? <p className="badge-success">Already enrolled</p> : <div className="flex flex-wrap items-center gap-2">
              <button type="button" className="btn-primary" disabled={save.isPending || session.choice === 'INTERESTED'}
                onClick={() => save.mutate({ batchId: session.batchId, choice: 'INTERESTED' })}>
                {session.choice === 'INTERESTED' ? 'Interested' : 'Mark interested'}
              </button>
              <button type="button" className="btn-secondary" disabled={save.isPending || session.choice === 'DISMISSED'}
                onClick={() => save.mutate({ batchId: session.batchId, choice: 'DISMISSED' })}>
                {session.choice === 'DISMISSED' ? 'Not interested' : 'Mark not interested'}
              </button>
              {session.choice !== 'UNDECIDED' && <button type="button" className="text-navy underline" disabled={save.isPending}
                onClick={() => save.mutate({ batchId: session.batchId, choice: 'UNDECIDED' })}>Clear choice</button>}
            </div>}
          </li>)}
        </ul>
      )}
      {training.data && training.data.length > 0 && <p className="text-sm text-slate-600">Marking interest shares an aggregate count with the provider. Register from the batch details when enrollment opens.</p>}
      {save.isError && <p role="alert" className="text-sm text-red-800">{save.error.message}</p>}
    </section>
  );
}
