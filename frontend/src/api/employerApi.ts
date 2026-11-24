import { apiDownload, apiGet, apiGetPaged, apiPost, apiUpload } from './client';
import type { components } from './generated/api.types';

const employer = '/api/v1/employer';

type EmployerSchemas = components['schemas'];
export type EmployerReference = Required<EmployerSchemas['Reference']>;

export interface EmployerReferences {
  employerName: string;
  jobRoles: EmployerReference[];
  qualifications: EmployerReference[];
  districts: EmployerReference[];
  engagementTypes: EmployerReference[];
  payFrequencies: EmployerReference[];
  skillLevels: EmployerReference[];
  rejectionReasons: EmployerReference[];
  exitReasons: EmployerReference[];
  separationNatures: EmployerReference[];
}

export type VacancyInput = EmployerSchemas['VacancyRequest'];

export interface EmployerVacancy {
  id: number;
  code: string;
  title: string;
  jobRole: string;
  qualification?: string;
  district: string;
  vacancies: number;
  minimumPay?: number;
  maximumPay?: number;
  payFrequency?: string;
  currency: string;
  status: string;
  closesOn?: string;
  skills: { id: number; name: string; level: string }[];
}

export interface EmployerPlacement {
  id: number;
  number: string;
  candidateName: string;
  course?: string;
  jobRoleId?: number;
  jobRole?: string;
  placementStatus: string;
  joiningStatus: string;
  offeredOn?: string;
  joinedOn?: string;
  offeredPay?: number;
  payFrequencyId?: number;
  payFrequency?: string;
  currency: string;
  employmentId?: number;
  canConfirm: boolean;
}

export type ConfirmPlacementInput = EmployerSchemas['ConfirmPlacementRequest'];

export type VerificationInput = EmployerSchemas['VerdictRequest'];
export type VerificationDecision = VerificationInput['decision'];

export interface EmployerVerification {
  id: number;
  number: string;
  candidateName: string;
  employmentNumber: string;
  jobRole?: string;
  startedOn: string;
  endedOn?: string;
  startingPay?: number;
  payFrequency?: string;
  currency: string;
  requestedAt: string;
  dueAt?: string;
  requestStatus: string;
  employmentVerificationStatus: string;
  verdict?: string;
  canRespond: boolean;
  canReportExit: boolean;
  evidenceId?: number;
  evidenceName?: string;
}

export function getEmployerReferences(signal?: AbortSignal) {
  return apiGet<EmployerReferences>(`${employer}/reference-data`, { signal });
}

export function searchEmployerSkills(search: string, signal?: AbortSignal) {
  return apiGet<EmployerReference[]>(`${employer}/skills`, { params: { search }, signal });
}

export function getEmployerVacancies(page: number, signal?: AbortSignal) {
  return apiGetPaged<EmployerVacancy>(`${employer}/job-postings`, { params: { page }, signal });
}

export function publishEmployerVacancy(input: VacancyInput) {
  return apiPost<VacancyInput, EmployerVacancy>(`${employer}/job-postings`, input);
}

export function getEmployerPlacements(page: number, signal?: AbortSignal) {
  return apiGetPaged<EmployerPlacement>(`${employer}/placements`, { params: { page }, signal });
}

export function confirmEmployerPlacement(id: number, input: ConfirmPlacementInput) {
  return apiPost<ConfirmPlacementInput, { employmentRecord: { employmentNumber: string } }>(
    `${employer}/placements/${id}/confirm`, input,
  );
}

export function getEmployerVerifications(view: 'pending' | 'history', page: number, signal?: AbortSignal) {
  const path = view === 'pending' ? '/verifications/pending' : '/verifications';
  return apiGetPaged<EmployerVerification>(employer + path, { params: { page }, signal });
}

export function submitEmployerVerdict(id: number, input: VerificationInput, file?: File) {
  const multipart = new FormData();
  multipart.append('verdict', new Blob([JSON.stringify(input)], { type: 'application/json' }));
  if (file) multipart.append('evidence', file);
  return apiUpload<EmployerVerification>(`${employer}/verifications/${id}/verdict`, multipart);
}

export function downloadEmployerEvidence(requestId: number, evidenceId: number) {
  return apiDownload(`${employer}/verifications/${requestId}/evidence/${evidenceId}`);
}
