import { apiGet, apiPost } from './client';
import type { AuthResponse } from './types';

export interface ReferenceItem {
  id: number;
  code: string;
  name: string;
}

export interface RegistrationReferenceData {
  genders: ReferenceItem[];
  states: ReferenceItem[];
  districts: ReferenceItem[];
  organizationTypes: ReferenceItem[];
  employmentStatuses: ReferenceItem[];
}

export interface RegisterTraineePayload {
  username: string;
  email: string;
  password: string;
  firstName: string;
  middleName?: string;
  lastName?: string;
  dateOfBirth?: string;
  genderId: number;
  phone?: string;
  stateId: number;
  districtId: number;
  addressLine1?: string;
  addressLine2?: string;
  pincode?: string;
  educationLevelId?: number;
  currentEmploymentStatusId?: number;
}

export interface RegisterTrainingProviderPayload {
  username: string;
  email: string;
  password: string;
  providerName: string;
  registrationNumber: string;
  organizationTypeId?: number;
  contactPersonName: string;
  contactEmail: string;
  contactPhone?: string;
  stateId: number;
  districtId: number;
  addressLine1?: string;
  addressLine2?: string;
  pincode?: string;
}

export interface RegisterEmployerPayload {
  username: string;
  email: string;
  password: string;
  employerName: string;
  registrationNumber?: string;
  gstin?: string;
  organizationTypeId?: number;
  industryId?: number;
  sectorId?: number;
  companySizeId?: number;
  contactPersonName: string;
  contactEmail: string;
  contactPhone?: string;
  stateId: number;
  districtId: number;
  addressLine1?: string;
  addressLine2?: string;
  pincode?: string;
}

export interface OnboardingApplicationResponse {
  applicationId: number;
  applicationNumber: string;
  applicantType: string;
  userId: number;
  username?: string;
  entityType: string;
  entityId: number;
  organizationName: string;
  contactPerson: string;
  contactEmail: string;
  contactPhone?: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  rejectionReason?: string;
  reviewedByUserId?: number;
  reviewedAt?: string;
  createdAt: string;
  message?: string;
}

export interface ApplicationStatusResponse {
  applicationNumber: string;
  applicantType: string;
  organizationName: string;
  contactEmail: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  rejectionReason?: string;
  submittedAt: string;
  reviewedAt?: string;
}

export async function registerTrainee(payload: RegisterTraineePayload): Promise<AuthResponse> {
  return apiPost<RegisterTraineePayload, AuthResponse>('/api/v1/auth/register/trainee', payload);
}

export async function registerTrainingProvider(
  payload: RegisterTrainingProviderPayload
): Promise<OnboardingApplicationResponse> {
  return apiPost<RegisterTrainingProviderPayload, OnboardingApplicationResponse>(
    '/api/v1/auth/register/training-provider',
    payload
  );
}

export async function registerEmployer(
  payload: RegisterEmployerPayload
): Promise<OnboardingApplicationResponse> {
  return apiPost<RegisterEmployerPayload, OnboardingApplicationResponse>(
    '/api/v1/auth/register/employer',
    payload
  );
}

export async function checkApplicationStatus(
  applicationNumber: string
): Promise<ApplicationStatusResponse> {
  return apiGet<ApplicationStatusResponse>(`/api/v1/auth/register/status/${encodeURIComponent(applicationNumber)}`);
}

export async function getRegistrationReferenceData(): Promise<RegistrationReferenceData> {
  return apiGet<RegistrationReferenceData>('/api/v1/auth/register/reference-data');
}
