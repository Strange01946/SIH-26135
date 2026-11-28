import { useState, type FormEvent } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { Modal } from '@/components/common/Modal';
import { ErrorState } from '@/components/feedback/ErrorState';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { getTraineeEmploymentOptions, saveTraineeEmployment, type EmploymentPeriod } from '@/api/traineeApi';

export function TraineeJobForm({ period, traineeId, onClose, onSaved }: {
  period?: EmploymentPeriod;
  traineeId: number;
  onClose: () => void;
  onSaved: () => Promise<void>;
}) {
  const [startDate, setStartDate] = useState(period?.startDate ?? '');
  const [trainingIds, setTrainingIds] = useState<number[]>(() =>
    period?.relatedTraining?.map(training => training.id) ?? (period?.enrollmentId ? [period.enrollmentId] : []));
  const [primaryId, setPrimaryId] = useState(period?.enrollmentId);
  const today = new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Kolkata', dateStyle: 'short' }).format(new Date());
  const options = useQuery({
    queryKey: ['trainee', 'employment-options', traineeId],
    queryFn: ({ signal }) => getTraineeEmploymentOptions(signal),
    staleTime: 0,
  });
  const save = useMutation({
    mutationFn: (form: FormData) => {
      const pay = String(form.get('pay') ?? '').trim();
      return saveTraineeEmployment({
        jobTitle: String(form.get('jobTitle')).trim(),
        employerName: String(form.get('employerName')).trim() || undefined,
        startDate,
        endDate: String(form.get('endDate') ?? '') || undefined,
        pay: pay ? Number(pay) : undefined,
        payFrequencyId: Number(form.get('payFrequency')),
        enrollmentId: primary,
        trainingEnrollmentIds: trainingIds,
      }, period?.id);
    },
    onSuccess: onSaved,
  });
  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (trainingDateConflict) return;
    save.mutate(new FormData(event.currentTarget));
  };
  const completed = options.data?.completedTraining ?? [];
  const available = new Set(completed.map(training => training.id));
  const choices = [...completed, ...(period?.relatedTraining ?? []).filter(training => !available.has(training.id))];
  const primary = primaryId != null && trainingIds.includes(primaryId) ? primaryId : trainingIds[0];
  const trainingDateConflict = choices.some(training => trainingIds.includes(training.id)
    && (!available.has(training.id) || (!!startDate && !!training.completedOn && training.completedOn > startDate)));

  function toggleTraining(id: number, checked: boolean) {
    setTrainingIds(ids => checked ? [...ids, id] : ids.filter(value => value !== id));
    setPrimaryId(checked ? primary ?? id : primary === id ? undefined : primary);
  }

  return <Modal open onClose={() => { if (!save.isPending) onClose(); }} title={period ? 'Edit employment' : 'Add employment'}>
    {options.isPending ? <LoadingSpinner label="Loading employment form" /> : options.isError ? (
      <ErrorState title="Could not load employment options" error={options.error} onRetry={() => void options.refetch()} />
    ) : <form className="space-y-3" onSubmit={submit}>
      <label className="form-label">Job role
        <input className="form-input" name="jobTitle" required maxLength={160} defaultValue={period?.jobRole} />
      </label>
      <label className="form-label">Employer name (optional)
        <input className="form-input" name="employerName" maxLength={160} defaultValue={period?.employer} />
      </label>
      <div className="grid gap-3 sm:grid-cols-2">
        <label className="form-label">Joining date
          <input className="form-input" name="startDate" type="date" max={today} required value={startDate}
            onChange={event => setStartDate(event.target.value)} />
        </label>
        <label className="form-label">Last working day (optional)
          <input className="form-input" name="endDate" type="date" min={startDate || undefined} max={today} defaultValue={period?.endDate} />
        </label>
        <label className="form-label">Starting pay in INR (optional)
          <input className="form-input" name="pay" type="number" step="0.01" min="0" max="9999999999.99" defaultValue={period?.startingPay} />
        </label>
        <label className="form-label">Pay frequency
          <select className="form-input" name="payFrequency" required defaultValue={period?.payFrequencyId ?? ''}>
            <option value="">Select pay frequency</option>
            {options.data.payFrequencies.map(item => <option key={item.id} value={item.id}>{item.label}</option>)}
          </select>
        </label>
      </div>
      <fieldset className="space-y-2" aria-describedby="employment-training-hint">
        <legend className="font-medium">Related completed training</legend>
        <p id="employment-training-hint" className="text-sm text-slate-600">
          {choices.length === 0 ? 'You have no completed training to link.'
            : 'Select any relevant training completed on or before your joining date.'}
        </p>
        {choices.length > 0 && <div className="max-h-44 space-y-1 overflow-y-auto rounded border p-2">
          {choices.map(item => {
            const checked = trainingIds.includes(item.id);
            const unavailable = !available.has(item.id);
            const tooLate = !!startDate && !!item.completedOn && item.completedOn > startDate;
            return <label className="flex items-start gap-2 rounded p-1 text-sm" key={item.id}>
              <input type="checkbox" className="mt-1 accent-blue-800" checked={checked}
                disabled={!checked && (unavailable || tooLate)}
                onChange={event => toggleTraining(item.id, event.target.checked)} />
              <span>{[item.name, item.batchCode, item.completedOn].filter(Boolean).join(' · ')}
                {unavailable && ' · No longer available'}
                {tooLate && ' · Completed after joining'}
              </span>
            </label>;
          })}
        </div>}
      </fieldset>
      {trainingIds.length > 0 && <label className="form-label">Primary training for MIS
        <select className="form-input" value={primary} onChange={event => setPrimaryId(Number(event.target.value))}>
          {choices.filter(training => trainingIds.includes(training.id)).map(item =>
            <option key={item.id} value={item.id}>{[item.name, item.batchCode, item.completedOn].filter(Boolean).join(' · ')}</option>)}
        </select>
      </label>}
      {trainingDateConflict && <p role="alert" className="text-sm text-red-800">Remove the unavailable training links or correct the joining date.</p>}
      {save.isError && <p role="alert" className="text-sm text-red-800">{save.error.message}</p>}
      <button type="submit" className="btn-primary" disabled={save.isPending || trainingDateConflict}>{save.isPending ? 'Saving…' : 'Save employment'}</button>
    </form>}
  </Modal>;
}
