import { useQuery } from '@tanstack/react-query';
import { getTrainingEducationLevels, type TrainingAdmission, type TrainingAdmissionInput } from '@/api/trainingAdmissionApi';

export function readTrainingAdmission(form: FormData): TrainingAdmissionInput {
  return {
    minimumEducationLevelId: Number(form.get('minimumEducationLevelId')) || undefined,
    recommendedPrerequisites: String(form.get('recommendedPrerequisites') ?? '').trim() || undefined,
  };
}

export function TrainingAdmissionFields() {
  const levels = useQuery({
    queryKey: ['training-admission', 'education-levels'],
    queryFn: ({ signal }) => getTrainingEducationLevels(signal),
    staleTime: 300_000,
  });
  return <div className="space-y-3">
    <label className="form-label">Minimum education level
      <select className="form-input" name="minimumEducationLevelId" defaultValue="" disabled={!levels.isSuccess}>
        <option value="">Open to all</option>
        {levels.data?.map(level => <option key={level.id} value={level.id}>{level.name}</option>)}
      </select>
    </label>
    {levels.isPending && <p className="text-sm text-slate-600">Loading education levels…</p>}
    {levels.isError && <button type="button" className="text-sm text-navy underline" onClick={() => void levels.refetch()}>
      Could not load education levels. Retry
    </button>}
    <label className="form-label">Recommended prerequisites (optional)
      <textarea className="form-input" name="recommendedPrerequisites" rows={2} maxLength={1000}
        placeholder="For example, basic programming experience" />
    </label>
  </div>;
}

export function TrainingAdmissionRows({ admission }: { admission?: TrainingAdmission }) {
  return <>
    <div>
      <dt className="font-semibold">Minimum education level</dt>
      <dd className="text-sm">{admission?.minimumEducationLevel || 'Open to all'}</dd>
    </div>
    {admission?.recommendedPrerequisites && <div>
      <dt className="font-semibold">Recommended prerequisites (optional)</dt>
      <dd className="whitespace-pre-wrap break-words text-sm">{admission.recommendedPrerequisites}</dd>
    </div>}
  </>;
}
