import { TrainingAdmissionRows } from '@/pages/training/TrainingAdmissionFields';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useOutletContext, useParams } from 'react-router-dom';
import { careerSkillsKey } from '@/api/careerSkillsApi';
import { availableSeatLabel, registerForTraining, trainingRegistrationKey, viewTrainingBatch, type TrainingBatchDetails } from '@/api/trainingRegistrationApi';
import { careerDate } from '@/components/career/CareerInsights';
import { ErrorState } from '@/components/feedback/ErrorState';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';

export function TraineeTrainingBatchPage() {
  const { batchId } = useParams();
  const id = Number(batchId);
  const { traineeId, isLoading } = useOutletContext<{ traineeId?: number; isLoading: boolean }>();

  if (isLoading) return <LoadingSpinner label="Loading trainee profile" />;

  if (!traineeId || !Number.isSafeInteger(id) || id < 1) return <p className="card p-5">This training batch is unavailable.</p>;
  return <TrainingBatch key={id} id={id} traineeId={traineeId} />;
}

function TrainingBatch({ id, traineeId }: { id: number; traineeId: number }) {
  const client = useQueryClient();
  const key = [...trainingRegistrationKey, 'detail', traineeId, id] as const;
  const details = useQuery({
    queryKey: key, queryFn: ({ signal }) => viewTrainingBatch(id, signal),
    retry: false, refetchOnMount: 'always',
  });
  const enroll = useMutation({
    mutationFn: () => registerForTraining(id),
    onSuccess: async result => {
      client.setQueryData<TrainingBatchDetails>(key, current => current && {
        ...current,
        batch: { ...current.batch, seatsAvailable: Math.max(0, current.batch.seatsAvailable - 1) },
        enrollmentNumber: result.enrollmentNumber,
        enrollmentStatus: result.enrollmentStatusCode ?? 'ENROLLED',
      });
      await Promise.all([
        client.invalidateQueries({ queryKey: trainingRegistrationKey }),
        client.invalidateQueries({ queryKey: ['trainee', 'enrollments', traineeId] }),
        client.invalidateQueries({ queryKey: [...careerSkillsKey, 'training', traineeId] }),
      ]);
    },
    onError: () => client.invalidateQueries({ queryKey: key }),
  });

  if (details.isPending) return <LoadingSpinner label="Loading training batch" />;
  if (details.isError && !details.data) return (
    <div className="space-y-3">
      <Link className="text-navy underline" to="/trainee/find-training">Find training</Link>
      {enroll.isError && <p role="alert" className="text-red-800">{enroll.error.message}</p>}
      <ErrorState title="Could not open training batch" error={details.error} onRetry={() => void details.refetch()} />
    </div>
  );

  const { batch, address, sessions, enrollmentNumber, enrollmentStatus, registrationOpen, consentGranted, educationEligibility } = details.data;
  const meetsEducation = !batch.admission?.minimumEducationLevelId || educationEligibility === 'ELIGIBLE';
  const mayRegister = registrationOpen && consentGranted && meetsEducation && !enrollmentNumber && batch.seatsAvailable > 0;
  return (
    <div className="space-y-5">
      <nav className="flex flex-wrap gap-4 text-sm" aria-label="Training navigation">
        <Link className="text-navy underline" to="/trainee/find-training">Find training</Link>
        <Link className="text-navy underline" to="/trainee/training">My Training &amp; Attendance</Link>
      </nav>
      <section className="card space-y-4 p-5 sm:p-6" aria-label="Batch details">
        <header>
          <p className="text-sm text-slate-600">{batch.governmentLed ? 'Government-led' : 'Provider-led'} · {batch.code}</p>
          <h1 className="page-title">{batch.title}</h1>
        </header>
        <dl className="grid gap-4 sm:grid-cols-2">
          <div><dt className="font-semibold">Skills taught</dt><dd className="text-sm">{batch.skills}</dd></div>
          <div><dt className="font-semibold">Training provider</dt><dd className="text-sm">{batch.provider}</dd></div>
          <TrainingAdmissionRows admission={batch.admission} />
          <div>
            <dt className="font-semibold">Training venue</dt>
            <dd className="text-sm">
              <p>{batch.venue || 'Venue unavailable'}</p>
              <p>{[address, batch.district, batch.state].filter(Boolean).join(', ')}</p>
            </dd>
          </div>
          <div><dt className="font-semibold">Training dates</dt>
            <dd className="text-sm">{careerDate(batch.startsOn)}{batch.endsOn && batch.startsOn !== batch.endsOn && ` to ${careerDate(batch.endsOn)}`}</dd></div>
        </dl>
        <div className="space-y-2 border-t border-slate-200 pt-4">
          {batch.status === 'CANCELLED' && <p className="text-sm font-semibold text-red-800">This batch was cancelled.</p>}
          {registrationOpen && <p className="text-sm font-medium">{availableSeatLabel(batch.seatsAvailable)}</p>}
          {enrollmentNumber ? (
            <p role="status" className="text-sm">
              <strong className="capitalize">{enroll.isSuccess ? 'Seat confirmed' : enrollmentStatus === 'ENROLLED' ? 'Registered' : enrollmentStatus?.replace(/_/g, ' ').toLowerCase()}</strong>
              {' · '}{enrollmentNumber}
            </p>
          ) : !registrationOpen ? (
            <p className="text-sm text-slate-600">{batch.status === 'PLANNED' ? 'Registration has not opened yet.' : 'Registration is closed.'}</p>
          ) : !meetsEducation ? (
            <p className="text-sm text-amber-900">{educationEligibility === 'BELOW_MINIMUM'
              ? `This training requires at least ${batch.admission?.minimumEducationLevel}.`
              : 'Add your education level to check whether you qualify.'}{' '}
              <Link className="text-navy underline" to="/trainee/profile">Update education in your profile</Link>
            </p>
          ) : !consentGranted ? (
            <p className="text-sm">To register, grant data processing consent in{' '}
              <Link className="text-navy underline" to="/trainee/consent">DPDP Privacy &amp; Consent</Link>.
            </p>
          ) : null}
          {!enrollmentNumber && registrationOpen && <button type="button" className="btn-primary"
            disabled={!mayRegister || details.isFetching || enroll.isPending}
            onClick={() => enroll.mutate()}>{enroll.isPending ? 'Registering…' : 'Register for training'}</button>}
          {enroll.isError && <p role="alert" className="text-sm text-red-800">{enroll.error.message}</p>}
        </div>
      </section>
      <section className="card space-y-3 p-5 sm:p-6" aria-labelledby="training-timetable">
        <h2 className="section-title" id="training-timetable">Session timetable</h2>
        {sessions.length === 0 ? <p className="text-sm text-slate-600">There are no scheduled sessions to display.</p> : (
          <table className="w-full text-left text-sm">
            <thead><tr className="border-b border-slate-200"><th scope="col" className="pb-2">Date</th>
              <th scope="col" className="pb-2">Starts (IST)</th><th scope="col" className="pb-2">Ends (IST)</th></tr></thead>
            <tbody className="divide-y divide-slate-200">
              {sessions.map(session => <tr key={session.startsAt}>
                <td className="py-2">{careerDate(session.startsAt.slice(0, 10))}</td>
                <td className="py-2">{session.startsAt.slice(11, 16)}</td>
                <td className="py-2">{session.endsAt.slice(11, 16)}
                  {session.endsAt.slice(0, 10) !== session.startsAt.slice(0, 10) && ` · ${careerDate(session.endsAt.slice(0, 10))}`}</td>
              </tr>)}
            </tbody>
          </table>
        )}
      </section>
    </div>
  );
}
