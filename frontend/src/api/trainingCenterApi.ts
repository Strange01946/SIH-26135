import { apiDelete, apiGet, apiPut } from './client';
import type { components } from './generated/api.types';
import type { ReferenceItem } from './registrationApi';
import type { TrainingCenter } from '@/types/provider';

const centers = '/api/v1/training-centers';
export type UpdateTrainingCenter = components['schemas']['UpdateTrainingCenterRequest'];
export const getCenterStatuses = (signal: AbortSignal) =>
  apiGet<ReferenceItem[]>(centers + '/lifecycle-statuses', { signal });

export const updateTrainingCenter = (id: number, payload: UpdateTrainingCenter) =>
  apiPut<UpdateTrainingCenter, TrainingCenter>(centers + '/' + id, payload);

export const removeTrainingCenter = (id: number) => apiDelete(centers + '/' + id);
