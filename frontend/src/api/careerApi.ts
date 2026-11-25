import { apiGet, apiPost } from './client';
import type { components } from './generated/api.types';
import type { FollowupSubmission, FollowupSubmissionDetail, ReportedCareerUpdate } from './traineeApi';

type Schemas = components['schemas'];
const career = '/api/v1/career-progress';
const employment = '/api/v1/employments';

export interface CareerCandidate {
  id: number;
  registrationNumber: string;
  name: string;
  statusCode: string;
  status: string;
}

export interface EmploymentPeriod {
  id: number;
  number: string;
  employer?: string;
  role?: string;
  startDate: string;
  endDate?: string;
  status: string;
  verificationStatus?: string;
  canRecordExit: boolean;
}

export interface CareerHistory {
  candidate: CareerCandidate;
  employments: EmploymentPeriod[];
  exitEvents: Schemas['EmploymentExitEventResponse'][];
  unemploymentEvents: Schemas['TraineeUnemploymentEventResponse'][];
  reportedUpdate?: ReportedCareerUpdate;
}

export interface EmploymentRetention {
  employment: EmploymentPeriod;
  asOf: string;
  milestones: Array<{
    months: number;
    dueOn: string;
    status: 'NOT_YET_DUE' | 'RETAINED' | 'ENDED_BEFORE_MILESTONE';
  }>;
  wages: WageObservation[];
}

export interface WageObservation {
  date: string;
  amount: number;
  currency: string;
  frequency?: string;
  verificationStatus?: string;
  observationMonth?: number;
  startingWage: boolean;
}

export interface ExitOptions {
  unemploymentReasons: { id: number; name: string }[];
  exitReasons: { id: number; name: string }[];
}

export interface ExitInput {
  separationDate: string;
  exitType: 'VOLUNTARY' | 'CONTRACT_END' | 'LAY_OFF';
  exitReasonId?: number;
  unemploymentReasonId?: number;
  confirmUnemployment: boolean;
  remarks?: string;
}

export const findCareerCandidates = (search: string, signal?: AbortSignal) =>
  apiGet<CareerCandidate[]>(`${career}/candidates`, { params: { search }, signal });

export const getCareerHistory = (traineeId: number, signal?: AbortSignal) =>
  apiGet<CareerHistory>(`${career}/candidates/${traineeId}`, { signal });

export const getEmploymentRetention = (employmentId: number, signal?: AbortSignal) =>
  apiGet<EmploymentRetention>(`${employment}/${employmentId}/retention`, { signal });

export const getCareerExitOptions = (signal?: AbortSignal) =>
  apiGet<ExitOptions>(`${career}/exit-options`, { signal });

export const recordCareerExit = (employmentId: number, input: ExitInput) =>
  apiPost<ExitInput, Schemas['EmploymentExitAndUnemploymentResponse']>(`${employment}/${employmentId}/exit`, input);

export const confirmCareerUnemployment = (employmentId: number, unemploymentReasonId: number, remarks?: string) =>
  apiPost<{ unemploymentReasonId: number; remarks?: string }, Schemas['TraineeUnemploymentEventResponse']>(`${employment}/${employmentId}/unemployment`,
    { unemploymentReasonId, remarks });

type CandidateFollowups = {
  followupConsentGranted: boolean;
  campaigns: Array<{ id: number; name: string; survey: string; closesOn?: string | null }>;
  assignments: Array<{ id: number; name: string; scheduledOn: string; status: string }>;
  recentSubmissions: FollowupSubmission[];
};
const surveysFor = (traineeId: number) => `${career}/candidates/${traineeId}/surveys`;

export const getCandidateFollowups = (traineeId: number, signal?: AbortSignal) =>
  apiGet<CandidateFollowups>(surveysFor(traineeId), { signal });

export const scheduleCandidateFollowup = (traineeId: number, input: Schemas['ScheduleRequest']) =>
  apiPost<Schemas['ScheduleRequest'], Schemas['Assignment']>(surveysFor(traineeId), input);

export const getCandidateSurveySubmission = (traineeId: number, submissionId: number, signal?: AbortSignal) =>
  apiGet<FollowupSubmissionDetail>(`${surveysFor(traineeId)}/${submissionId}`, { signal });
