import { apiGet, apiPost } from './client';
import type { TrainingEnrollment } from '@/types/trainee';

export type TrainingBatchListing = {
  id: number;
  code: string;
  title: string;
  skills: string;
  provider: string;
  venue?: string;
  districtId: number;
  district?: string;
  state?: string;
  startsOn: string;
  endsOn?: string;
  firstSession?: string;
  status: string;
  governmentLed: boolean;
  seatsAvailable: number;
};

export type TrainingBatchDetails = {
  batch: TrainingBatchListing;
  address?: string;
  sessions: { startsAt: string; endsAt: string }[];
  registrationOpen: boolean;
  consentGranted: boolean;
  enrollmentNumber?: string;
  enrollmentStatus?: string;
};

export type TrainingSearch = { search?: string; stateId?: number; districtId?: number; from?: string; through?: string; page: number };
export type BatchRosterEntry = {
  enrollmentId: number;
  enrollmentNumber: string;
  name: string;
  registrationNumber: string;
  status: string;
  email?: string;
  phone?: string;
};

const ownTraining = '/api/v1/trainees/me/training';
export const trainingRegistrationKey = ['training-registration'] as const;
export function availableSeatLabel(seats: number) {
  if (seats === 0) return 'Batch full';
  return seats === 1 ? '1 seat available' : `${seats} seats available`;
}
export const findOpenTraining = (params: TrainingSearch, signal?: AbortSignal) =>
  apiGet<{ batches: TrainingBatchListing[]; hasNext: boolean }>(ownTraining, { params, signal });
export const viewTrainingBatch = (id: number, signal?: AbortSignal) =>
  apiGet<TrainingBatchDetails>(`${ownTraining}/${id}`, { signal });
export const registerForTraining = (id: number) =>
  apiPost<undefined, TrainingEnrollment>(`${ownTraining}/${id}/enrollment`, undefined);
export const getTrainingBatchRoster = (id: number, page: number, signal?: AbortSignal) =>
  apiGet<{ entries: BatchRosterEntry[]; hasNext: boolean }>(`/api/v1/training-batches/${id}/roster`, { params: { page }, signal });
