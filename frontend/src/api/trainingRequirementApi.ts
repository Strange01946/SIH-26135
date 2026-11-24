import { apiGet, apiPost } from './client';

export type TrainingWindow = { startDate: string; endDate: string; dailyStartTime: string; dailyEndTime: string };
export type PublishRequirement = {
  title: string; skillsToTeach: string; description?: string;
  stateId: number; districtId: number; centerId: number; seats: number;
  earliestDate: string; latestDate: string; dailyStartTime: string; dailyEndTime: string;
};
export type TrainingRequirement = Omit<PublishRequirement, 'centerId'> & {
  id: number; stateName: string; districtName: string;
  centerId?: number | null; centerName?: string | null; centerAddress?: string | null;
  status: 'OPEN' | 'AWARDED'; acceptingApplications: boolean;
  selectedProviderId?: number | null; selectedProviderName?: string | null;
  publishedAt: string; selectedAt?: string | null; bookingId?: number | null; bookingStatus?: string | null;
};
export type ApplyForRequirement = TrainingWindow & { deliveryNotes?: string };
export type TrainingApplication = ApplyForRequirement & {
  id: number; requirementId: number; requirementTitle: string; providerId: number; providerName: string;
  status: 'APPLIED' | 'SELECTED' | 'NOT_SELECTED'; submittedAt: string;
};
export type RequirementDetails = {
  requirement: TrainingRequirement; applications: TrainingApplication[]; myApplication?: TrainingApplication | null;
};

export const trainingRequirementsKey = ['training-requirements'] as const;
const route = '/api/v1/training-requirements';
export const getGovernmentRequirements = (page: number, size: number, signal: AbortSignal) =>
  apiGet<TrainingRequirement[]>(route + '/government', { params: { page, size }, signal });
export const getOpenRequirements = (page: number, size: number, stateId: number, districtId: number, signal: AbortSignal) =>
  apiGet<TrainingRequirement[]>(route + '/open', { params: { page, size, stateId: stateId || undefined, districtId: districtId || undefined }, signal });
export const getMyTrainingApplications = (page: number, size: number, signal: AbortSignal) =>
  apiGet<TrainingApplication[]>(route + '/my-applications', { params: { page, size }, signal });
export const getTrainingRequirement = (id: number, government: boolean, signal: AbortSignal) =>
  apiGet<RequirementDetails>(`${route}${government ? '/government' : ''}/${id}`, { signal });
export const publishTrainingRequirement = (data: PublishRequirement) =>
  apiPost<PublishRequirement, TrainingRequirement>(route + '/government', data);
export const applyForTrainingRequirement = (id: number, data: ApplyForRequirement) =>
  apiPost<ApplyForRequirement, TrainingApplication>(`${route}/${id}/applications`, data);
export const selectTrainingProvider = (id: number, applicationId: number) =>
  apiPost<{ applicationId: number }, TrainingRequirement>(`${route}/government/${id}/select`, { applicationId });
export const setTrainingCentre = (id: number, centerId: number) =>
  apiPost<{ centerId: number }, TrainingRequirement>(`${route}/government/${id}/centre`, { centerId });
