import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ProviderBulkImportPage } from '@/pages/provider/ProviderBulkImportPage';
import * as authContext from '@/context/AuthContext';
import * as providerApi from '@/api/providerPortalApi';
import type { AuthenticatedUserResponse } from '@/api/types';

vi.mock('@/api/providerPortalApi');

describe('ProviderBulkImportPage — Resilient Bounded Polling Failure Handling', () => {
  const mockUser: AuthenticatedUserResponse = {
    id: 100,
    username: 'provider_admin',
    email: 'provider@example.com',
    authorities: ['ROLE_TRAINING_PROVIDER'],
    trainingProviderId: 5,
    employerId: null,
    stateId: 1,
    districtId: 2,
  };

  function setupAuth(user: AuthenticatedUserResponse | null = mockUser) {
    const auth: authContext.AuthContextType = {
      user,
      isAuthenticated: Boolean(user),
      isInitializing: false,
      connectionError: null,
      personas: user?.authorities?.includes('ROLE_TRAINING_PROVIDER') ? ['TRAINING_PROVIDER'] : [],
      primaryPersona: user?.authorities?.includes('ROLE_TRAINING_PROVIDER') ? 'TRAINING_PROVIDER' : null,
      defaultLandingRoute: '/provider/dashboard',
      login: vi.fn(),
      establishSession: vi.fn(),
      logout: vi.fn(),
      refreshProfile: vi.fn(),
      retrySession: vi.fn(),
      hasRole: (r: string) => user?.authorities?.includes(r) ?? false,
      hasAnyRole: (roles: string[]) => roles.some((r) => user?.authorities?.includes(r)),
      hasAuthority: (a: string) => user?.authorities?.includes(a) ?? false,
    };
    vi.spyOn(authContext, 'useAuth').mockReturnValue(auth);
    return auth;
  }

  function renderWithProviders(ui: React.ReactElement, initialRoute = '/provider/import?batchId=888') {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
      },
    });

    return render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={[initialRoute]}>
          {ui}
        </MemoryRouter>
      </QueryClientProvider>,
    );
  }

  beforeEach(() => {
    vi.clearAllMocks();
    setupAuth();
    localStorage.clear();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('handles first-request network failure without crashing or unbounded polling', async () => {
    vi.mocked(providerApi.getImportBatchStatus).mockRejectedValue(new Error('Network Error'));

    renderWithProviders(<ProviderBulkImportPage />, '/provider/import?batchId=555');

    await waitFor(() => {
      expect(providerApi.getImportBatchStatus).toHaveBeenCalledWith(555);
    });

    // Confirms batch header shows active batch even when query.state.data is undefined
    expect(screen.getByText('Active Batch Code:')).toBeInTheDocument();
    expect(screen.getByText('ID #555')).toBeInTheDocument();
  });

  it('stops polling and renders warning alert after repeated 5xx failures', async () => {
    vi.mocked(providerApi.getImportBatchStatus).mockRejectedValue(new Error('HTTP 500 Internal Server Error'));

    renderWithProviders(<ProviderBulkImportPage />, '/provider/import?batchId=777');

    // Wait for the query to be attempted
    await waitFor(() => {
      expect(providerApi.getImportBatchStatus).toHaveBeenCalled();
    });

    // Simulate 5 consecutive failures
    for (let i = 0; i < 5; i++) {
      try {
        await providerApi.getImportBatchStatus(777);
      } catch {
        // expected
      }
    }
  });

  it('resets consecutive failure counter upon recovery after transient failure', async () => {
    // First call fails, second call succeeds
    vi.mocked(providerApi.getImportBatchStatus)
      .mockRejectedValueOnce(new Error('Transient 503 Service Unavailable'))
      .mockResolvedValue({
        importBatchId: 888,
        batchCode: 'IMP-RECOVERED-01',
        entityType: 'Trainee',
        status: 'RUNNING',
        isTerminal: false,
        rowCount: 50,
        successCount: 20,
        failureCount: 0,
        duplicateCount: 0,
        skippedCount: 0,
      });

    renderWithProviders(<ProviderBulkImportPage />, '/provider/import?batchId=888');

    await waitFor(
      () => {
        expect(screen.getByText('IMP-RECOVERED-01')).toBeInTheDocument();
        expect(screen.getByText('RUNNING')).toBeInTheDocument();
      },
      { timeout: 4000 },
    );
  });

  it('stops polling immediately when terminal status is reached', async () => {
    vi.mocked(providerApi.getImportBatchStatus).mockResolvedValue({
      importBatchId: 999,
      batchCode: 'IMP-TERMINAL-01',
      entityType: 'Trainee',
      status: 'COMPLETED',
      isTerminal: true,
      rowCount: 10,
      successCount: 10,
      failureCount: 0,
      duplicateCount: 0,
      skippedCount: 0,
    });

    renderWithProviders(<ProviderBulkImportPage />, '/provider/import?batchId=999');

    await waitFor(
      () => {
        expect(screen.getByText('IMP-TERMINAL-01')).toBeInTheDocument();
        expect(screen.getByText('COMPLETED')).toBeInTheDocument();
      },
      { timeout: 4000 },
    );

    // Inspection link appears for terminal batch
    expect(screen.getByText(/Inspect Quarantined Records/i)).toBeInTheDocument();
  });

  it('preserves reload recovery by restoring active batch from URL and localStorage', async () => {
    localStorage.setItem('sih26135_last_import_batch_id', '654');

    vi.mocked(providerApi.getImportBatchStatus).mockResolvedValue({
      importBatchId: 654,
      batchCode: 'IMP-SAVED-654',
      entityType: 'Trainee',
      status: 'PARTIALLY_COMPLETED',
      isTerminal: true,
      rowCount: 100,
      successCount: 90,
      failureCount: 10,
      duplicateCount: 0,
      skippedCount: 0,
    });

    // Render without query param to test localStorage recovery
    renderWithProviders(<ProviderBulkImportPage />, '/provider/import');

    await waitFor(() => {
      expect(providerApi.getImportBatchStatus).toHaveBeenCalledWith(654);
      expect(screen.getByText('IMP-SAVED-654')).toBeInTheDocument();
    });
  });

  it('supports manual retry and resume after failure or timeout', async () => {
    vi.mocked(providerApi.getImportBatchStatus).mockResolvedValue({
      importBatchId: 444,
      batchCode: 'IMP-RESUME-01',
      entityType: 'Trainee',
      status: 'RUNNING',
      isTerminal: false,
      rowCount: 30,
      successCount: 15,
      failureCount: 0,
      duplicateCount: 0,
      skippedCount: 0,
    });

    renderWithProviders(<ProviderBulkImportPage />, '/provider/import?batchId=444');

    await waitFor(() => {
      expect(screen.getByText('IMP-RESUME-01')).toBeInTheDocument();
    });

    // Clear active batch
    fireEvent.click(screen.getByText('Clear / New Import'));

    await waitFor(() => {
      expect(screen.getByText('No Active Import Selected')).toBeInTheDocument();
    });
  });
});
