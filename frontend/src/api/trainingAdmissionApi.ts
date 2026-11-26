import { apiGet } from './client';

export type TrainingAdmission = {
  minimumEducationLevelId?: number | null;
  minimumEducationLevel?: string | null;
  recommendedPrerequisites?: string | null;
};

export type TrainingAdmissionInput = {
  minimumEducationLevelId?: number;
  recommendedPrerequisites?: string;
};

export type EducationEligibility = 'ELIGIBLE' | 'EDUCATION_REQUIRED' | 'BELOW_MINIMUM';

export const getTrainingEducationLevels = (signal?: AbortSignal) =>
  apiGet<{ id: number; name: string }[]>('/api/v1/training-admission/education-levels', { signal });

export const getBatchAdmission = (id: number, signal?: AbortSignal) =>
  apiGet<TrainingAdmission>(`/api/v1/training-batches/${id}/admission`, { signal });
