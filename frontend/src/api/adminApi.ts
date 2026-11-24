import { apiGet, apiPost } from './client';
import type { components } from './generated/api.types';
import type { OnboardingApplicationResponse } from './registrationApi';

export interface AdminUserResponse {
  id: number;
  username: string;
  email: string;
  userStatusId: number;
  userStatusCode?: string;
  lastLoginAt?: string;
  failedLoginCount?: number;
  lockedUntil?: string;
  organizationId?: number;
  departmentId?: number;
  stateId?: number;
  districtId?: number;
  trainingProviderId?: number;
  employerId?: number;
  createdAt: string;
  updatedAt: string;
}

export interface ProvisionAssessorPayload {
  username: string;
  email: string;
  password: string;
  fullName?: string;
  phone?: string;
  organizationId?: number;
  stateId?: number;
  districtId?: number;
}

export interface ProvisionGovernmentAdminPayload {
  username: string;
  email: string;
  password: string;
  fullName?: string;
  roleCode: 'government_admin' | 'state_admin' | 'government_officer' | 'district_officer' | 'analyst';
  organizationId?: number;
  stateId?: number;
  districtId?: number;
}

export interface ProvisionSuperAdminPayload {
  username: string;
  email: string;
  password: string;
  fullName?: string;
}

export async function listOnboardingApplications(
  status?: string, signal?: AbortSignal,
): Promise<OnboardingApplicationResponse[]> {
  const params: Record<string, string> = {};
  if (status) {
    params.status = status;
  }
  return apiGet<OnboardingApplicationResponse[]>('/api/v1/admin/onboarding/applications', { params, signal });
}

export const pendingOnboardingKey = ['admin', 'onboarding', 'pending'] as const;

export function getAdminImportMetrics(signal?: AbortSignal) {
  return apiGet<components['schemas']['OperationalImportMetricsResponse']>('/api/v1/operational/import-metrics', { signal });
}

export async function getOnboardingApplication(
  id: number
): Promise<OnboardingApplicationResponse> {
  return apiGet<OnboardingApplicationResponse>(`/api/v1/admin/onboarding/applications/${id}`);
}

export async function approveOnboardingApplication(
  id: number
): Promise<OnboardingApplicationResponse> {
  return apiPost<Record<string, never>, OnboardingApplicationResponse>(
    `/api/v1/admin/onboarding/applications/${id}/approve`,
    {}
  );
}

export async function rejectOnboardingApplication(
  id: number,
  rejectionReason?: string
): Promise<OnboardingApplicationResponse> {
  return apiPost<{ rejectionReason: string }, OnboardingApplicationResponse>(
    `/api/v1/admin/onboarding/applications/${id}/reject`,
    {
      rejectionReason: rejectionReason || 'Application rejected by administrator',
    }
  );
}

export async function provisionAssessor(
  payload: ProvisionAssessorPayload
): Promise<AdminUserResponse> {
  return apiPost<ProvisionAssessorPayload, AdminUserResponse>(
    '/api/v1/admin/users/provision-assessor',
    payload
  );
}

export async function provisionGovernmentAdmin(
  payload: ProvisionGovernmentAdminPayload
): Promise<AdminUserResponse> {
  return apiPost<ProvisionGovernmentAdminPayload, AdminUserResponse>(
    '/api/v1/admin/users/provision-government-admin',
    payload
  );
}

export async function provisionSuperAdmin(
  payload: ProvisionSuperAdminPayload
): Promise<AdminUserResponse> {
  return apiPost<ProvisionSuperAdminPayload, AdminUserResponse>(
    '/api/v1/admin/users/provision-super-admin',
    payload
  );
}

export async function listUsers(): Promise<AdminUserResponse[]> {
  return apiGet<AdminUserResponse[]>('/api/v1/users');
}
