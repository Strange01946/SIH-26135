import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ProviderDataQualityPage } from '@/pages/provider/ProviderDataQualityPage';
import * as authContext from '@/context/AuthContext';
import * as providerDqApi from '@/api/providerDataQualityApi';
import * as providerApi from '@/api/providerPortalApi';
import * as clientApi from '@/api/client';
import type { AuthenticatedUserResponse } from '@/api/types';
import type { ProviderDataQualityIssue, DataQualityIssueEvent } from '@/types/providerDataQuality';

vi.mock('@/api/providerDataQualityApi');
vi.mock('@/api/providerPortalApi');
vi.mock('@/api/client', () => ({
  apiGet: vi.fn(),
  apiGetPaged: vi.fn(),
  apiPost: vi.fn(),
  apiPut: vi.fn(),
  apiUpload: vi.fn(),
  apiDelete: vi.fn(),
  apiDownload: vi.fn(),
}));

describe('Provider Data Quality Remediation Workbench', () => {
  const mockUser: AuthenticatedUserResponse = {
    id: 100,
    username: 'provider_user',
    email: 'provider@example.com',
    authorities: ['ROLE_TRAINING_PROVIDER'],
    trainingProviderId: 1010905,
    employerId: null,
    stateId: 1,
    districtId: 2,
  };

  const mockIssue: ProviderDataQualityIssue = {
    id: 42,
    ruleId: 10,
    ruleCode: 'DQ_ENROLLMENT_DATES_INVALID',
    ruleName: 'Enrollment Completion Before Start Date',
    categoryId: 2,
    categoryCode: 'CONSISTENCY',
    categoryName: 'Consistency',
    severityId: 3,
    severityCode: 'HIGH',
    severityName: 'High',
    statusId: 1,
    statusCode: 'OPEN',
    statusName: 'Open',
    isOpen: true,
    entityType: 'TrainingEnrollment',
    entityId: 999,
    centerId: 99887,
    centerName: 'North Delhi Center',
    batchId: 501,
    batchCode: 'BATCH-2026-01',
    issueSummary: 'Enrollment actual completion date precedes start date',
    detectedAt: '2026-11-20T10:00:00Z',
    updatedAt: null,
    resolvedAt: null,
    resolvedByUserId: null,
    resolutionNotes: null,
    remediationPermitted: true,
    remediationReason: 'Training Enrollment dates and status can be corrected directly.',
  };

  const mockEvents: DataQualityIssueEvent[] = [
    {
      id: 1,
      issueId: 42,
      statusId: 1,
      statusCode: 'OPEN',
      statusName: 'Open',
      changedByUserId: 100,
      changedAt: '2026-11-20T10:00:00Z',
      remarks: 'Automated evaluation detected anomaly',
    },
    {
      id: 2,
      issueId: 42,
      statusId: 2,
      statusCode: 'IN_PROGRESS',
      statusName: 'In Progress',
      changedByUserId: 100,
      changedAt: '2026-11-21T11:00:00Z',
      remarks: 'Provider commenced investigation',
    },
  ];

  function setupAuth() {
    const auth: authContext.AuthContextType = {
      user: mockUser,
      isAuthenticated: true,
      isInitializing: false,
      connectionError: null,
      personas: ['TRAINING_PROVIDER'],
      primaryPersona: 'TRAINING_PROVIDER',
      defaultLandingRoute: '/provider/dashboard',
      login: vi.fn(),
      establishSession: vi.fn(),
      logout: vi.fn(),
      refreshProfile: vi.fn(),
      retrySession: vi.fn(),
      hasRole: (r: string) => mockUser.authorities?.includes(r) ?? false,
      hasAnyRole: (roles: string[]) => roles.some((r) => mockUser.authorities?.includes(r)),
      hasAuthority: (a: string) => mockUser.authorities?.includes(a) ?? false,
    };
    vi.spyOn(authContext, 'useAuth').mockReturnValue(auth);
    return auth;
  }

  function renderWorkbench() {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });
    return render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <ProviderDataQualityPage />
        </MemoryRouter>
      </QueryClientProvider>
    );
  }

  beforeEach(() => {
    vi.clearAllMocks();
    setupAuth();

    vi.mocked(providerApi.getTrainingCenters).mockResolvedValue({
      content: [{ id: 99887, centerName: 'North Delhi Center', centerCode: 'TC-01', providerId: 1010905, stateId: 1, districtId: 2 }],
      totalElements: 1,
      page: 0,
      size: 50,
      totalPages: 1,
      first: true,
      last: true,
      hasNext: false,
      hasPrevious: false,
    });

    vi.mocked(providerDqApi.getProviderDataQualityIssues).mockResolvedValue({
      content: [mockIssue],
      totalElements: 1,
      page: 0,
      size: 10,
      totalPages: 1,
      first: true,
      last: true,
      hasNext: false,
      hasPrevious: false,
    });

    vi.mocked(providerDqApi.getDataQualityIssueById).mockResolvedValue(mockIssue);
    vi.mocked(providerDqApi.getDataQualityIssueEvents).mockResolvedValue(mockEvents);
    vi.mocked(clientApi.apiPut).mockResolvedValue({ success: true });
  });

  it('renders workbench title, tabs, and filter controls', async () => {
    renderWorkbench();

    expect(screen.getByText(/Data Quality Remediation Tracker/i)).toBeInTheDocument();
    expect(screen.getByTestId('tab-active-queue')).toBeInTheDocument();
    expect(screen.getByTestId('tab-all-issues')).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText('#42')).toBeInTheDocument();
      expect(screen.getByText('High')).toBeInTheDocument();
      expect(screen.getByText('North Delhi Center')).toBeInTheDocument();
      expect(screen.getByText('Enrollment actual completion date precedes start date')).toBeInTheDocument();
    });
  });

  it('opens issue detail drawer with bounded timeline when Inspect is clicked', async () => {
    renderWorkbench();

    await waitFor(() => {
      expect(screen.getByTestId('btn-inspect-42')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('btn-inspect-42'));

    await waitFor(() => {
      expect(screen.getByText(/Data Quality Issue #42 Detail/i)).toBeInTheDocument();
      expect(screen.getByText(/Audit Event Trail/i)).toBeInTheDocument();
      expect(screen.getByText('Automated evaluation detected anomaly')).toBeInTheDocument();
      expect(screen.getByText('Provider commenced investigation')).toBeInTheDocument();
    });
  });

  it('opens remediation modal and submits resolution with notes', async () => {
    vi.mocked(providerDqApi.resolveDataQualityIssue).mockResolvedValue({
      ...mockIssue,
      statusCode: 'RESOLVED',
      statusName: 'Resolved',
      isOpen: false,
      resolvedAt: '2026-11-22T12:00:00Z',
      resolutionNotes: 'Corrected enrollment completion date to adhere to batch schedule.',
    });

    renderWorkbench();

    await waitFor(() => {
      expect(screen.getByTestId('btn-remediate-42')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('btn-remediate-42'));

    await waitFor(() => {
      expect(screen.getByTestId('remediation-modal')).toBeInTheDocument();
      expect(screen.getByTestId('input-resolution-notes')).toBeInTheDocument();
    });

    // Enter mandatory resolution notes
    fireEvent.change(screen.getByTestId('input-resolution-notes'), {
      target: { value: 'Corrected enrollment completion date to adhere to batch schedule.' },
    });

    // Submit resolution
    const submitBtn = screen.getByTestId('btn-submit-resolution');
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(clientApi.apiPut).toHaveBeenCalledWith(
        '/api/v1/training-enrollments/999',
        expect.any(Object)
      );
      expect(providerDqApi.resolveDataQualityIssue).toHaveBeenCalledWith(42, {
        resolutionNotes: 'Corrected enrollment completion date to adhere to batch schedule.',
      });
    });
  });

  it('opens dismissal modal and requires remarks before submitting', async () => {
    vi.mocked(providerDqApi.dismissDataQualityIssue).mockResolvedValue({
      ...mockIssue,
      statusCode: 'DISMISSED',
      statusName: 'Dismissed',
      isOpen: false,
    });

    renderWorkbench();

    await waitFor(() => {
      expect(screen.getByTestId('btn-dismiss-42')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('btn-dismiss-42'));

    await waitFor(() => {
      expect(screen.getByText(/Dismiss Issue #42/i)).toBeInTheDocument();
      expect(screen.getByTestId('input-dismiss-remarks')).toBeInTheDocument();
    });

    fireEvent.change(screen.getByTestId('input-dismiss-remarks'), {
      target: { value: 'Confirmed false positive with state directorate' },
    });

    const confirmBtn = screen.getByTestId('btn-confirm-dismiss');
    fireEvent.click(confirmBtn);

    await waitFor(() => {
      expect(providerDqApi.dismissDataQualityIssue).toHaveBeenCalledWith(42, {
        remarks: 'Confirmed false positive with state directorate',
      });
    });
  });

  it('switches between active queue and all issues tabs', async () => {
    renderWorkbench();

    await waitFor(() => {
      expect(screen.getByTestId('tab-all-issues')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('tab-all-issues'));

    await waitFor(() => {
      expect(providerDqApi.getProviderDataQualityIssues).toHaveBeenCalledWith(
        expect.objectContaining({ activeOnly: false })
      );
    });
  });
});
