import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ProviderScorecardsPage } from '@/pages/provider/ProviderScorecardsPage';
import * as authContext from '@/context/AuthContext';
import * as providerDqApi from '@/api/providerDataQualityApi';
import * as providerApi from '@/api/providerPortalApi';
import type { AuthenticatedUserResponse } from '@/api/types';
import type { CenterScorecard } from '@/types/providerDataQuality';

vi.mock('@/api/providerDataQualityApi');
vi.mock('@/api/providerPortalApi');

describe('Provider Center Quality Scorecards', () => {
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

  const mockScorecard: CenterScorecard = {
    centerId: 99887,
    centerCode: 'TC-01',
    centerName: 'North Delhi Center',
    providerId: 1010905,
    cleanlinessIndexPct: 85.5,
    cleanlinessFormula: '((eligiblePopulation - distinctAffectedEntities) / eligiblePopulation) * 100',
    cleanlinessScope: 'Center enrollments evaluated by active Phase 28 rules',
    eligiblePopulation: 100,
    distinctAffectedEntities: 14,
    cleanEntitiesEstimate: 86,
    totalActiveIssues: 14,
    activeIssuesBySeverity: {
      CRITICAL: 2,
      HIGH: 5,
      MEDIUM: 4,
      LOW: 3,
    },
    activeIssuesByCategory: {
      COMPLETENESS: 6,
      CONSISTENCY: 5,
      VALIDITY: 2,
      DUPLICATE: 1,
    },
    totalEnrollments: 120,
    completedEnrollments: 96,
    droppedEnrollments: 12,
    inProgressEnrollments: 12,
    completionRatePct: 80.0,
    dropoutRatePct: 10.0,
    evaluatedAt: '2026-11-22T12:00:00Z',
  };

  function setupAuth() {
    const auth: authContext.AuthContextType = {
      user: mockUser,
      isAuthenticated: true,
      isInitializing: false,
      connectionError: null,
      personas: ['TRAINING_PROVIDER'],
      primaryPersona: 'TRAINING_PROVIDER',
      activePersona: 'TRAINING_PROVIDER',
      setActivePersona: vi.fn(),
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

  function renderScorecards() {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });
    return render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <ProviderScorecardsPage />
        </MemoryRouter>
      </QueryClientProvider>
    );
  }

  beforeEach(() => {
    vi.clearAllMocks();
    setupAuth();

    vi.mocked(providerApi.getTrainingCenters).mockResolvedValue({
      content: [
        { id: 99887, centerName: 'North Delhi Center', centerCode: 'TC-01', providerId: 1010905, stateId: 1, districtId: 2 },
        { id: 99888, centerName: 'South Delhi Center', centerCode: 'TC-02', providerId: 1010905, stateId: 1, districtId: 2 },
      ],
      totalElements: 2,
      page: 0,
      size: 50,
      totalPages: 1,
      first: true,
      last: true,
      hasNext: false,
      hasPrevious: false,
    });

    vi.mocked(providerDqApi.getCenterScorecard).mockResolvedValue(mockScorecard);
  });

  it('renders scorecard header, center selector, and cleanliness index metrics', async () => {
    renderScorecards();

    expect(screen.getByText(/Training Center Cleanliness Scorecards/i)).toBeInTheDocument();

    await waitFor(() => {
      // Cleanliness gauge percentage
      expect(screen.getByText(/85\.50%/)).toBeInTheDocument();
      // Cleanliness gauge label
      expect(screen.getByText('Cleanliness Index')).toBeInTheDocument();
      // Population breakdown
      expect(screen.getByText('100')).toBeInTheDocument(); // eligible population
      expect(screen.getByText('14')).toBeInTheDocument(); // affected entities
      expect(screen.getByText('86')).toBeInTheDocument(); // clean estimate
    });
  });

  it('displays completion vs dropout rate comparison bars', async () => {
    renderScorecards();

    await waitFor(() => {
      expect(screen.getByText(/80\.00%/)).toBeInTheDocument(); // Completion rate
      expect(screen.getByText(/10\.00%/)).toBeInTheDocument(); // Dropout rate
      expect(screen.getByText(/\(96\/120\)/)).toBeInTheDocument();
      expect(screen.getByText(/\(12\/120\)/)).toBeInTheDocument();
    });
  });

  it('displays active defect distribution by severity and category', async () => {
    renderScorecards();

    await waitFor(() => {
      expect(screen.getByText(/Active Issues in North Delhi Center/i)).toBeInTheDocument();
      expect(screen.getByText('CRITICAL')).toBeInTheDocument();
      expect(screen.getByText('HIGH')).toBeInTheDocument();
      expect(screen.getByText('MEDIUM')).toBeInTheDocument();
      expect(screen.getByText('LOW')).toBeInTheDocument();
      expect(screen.getByText('COMPLETENESS')).toBeInTheDocument();
      expect(screen.getByText('CONSISTENCY')).toBeInTheDocument();
      expect(screen.getByText('VALIDITY')).toBeInTheDocument();
      expect(screen.getByText('DUPLICATE')).toBeInTheDocument();
    });
  });

  it('displays honest insufficient cohort message when eligible population is zero', async () => {
    vi.mocked(providerDqApi.getCenterScorecard).mockResolvedValue({
      ...mockScorecard,
      cleanlinessIndexPct: null,
      eligiblePopulation: 0,
      distinctAffectedEntities: 0,
      cleanEntitiesEstimate: 0,
    });

    renderScorecards();

    await waitFor(() => {
      expect(screen.getByTestId('no-data-cleanliness')).toBeInTheDocument();
      expect(screen.getByText('Insufficient Evaluated Cohort')).toBeInTheDocument();
    });
  });

  it('allows switching selected center in dropdown', async () => {
    renderScorecards();

    await waitFor(() => {
      expect(screen.getByTestId('select-center-scorecard')).toBeInTheDocument();
    });

    fireEvent.change(screen.getByTestId('select-center-scorecard'), {
      target: { value: '99888' },
    });

    await waitFor(() => {
      expect(providerDqApi.getCenterScorecard).toHaveBeenCalledWith(99888);
    });
  });
});
