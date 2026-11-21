import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useTraineeIdentity } from '@/hooks/useTraineeIdentity';
import * as traineeApi from '@/api/traineeApi';
import * as authContext from '@/context/AuthContext';
import type { TraineeProfile } from '@/types/trainee';
import type { ApiError } from '@/api/types';

vi.mock('@/api/traineeApi');
vi.mock('@/context/AuthContext');

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  });
  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
}

describe('useTraineeIdentity Hook', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('resolves and returns TraineeProfile when candidate is linked to User ID', async () => {
    const mockUser = {
      id: 7,
      username: 'trainee_test',
      email: 'trainee@test.gov.in',
      authorities: ['ROLE_trainee', 'trainee.read', 'consent.manage'],
      roles: ['trainee'],
    };

    vi.spyOn(authContext, 'useAuth').mockReturnValue({
      user: mockUser as any,
      isAuthenticated: true,
      roles: ['trainee'],
      authorities: ['trainee.read'],
      hasRole: () => true,
      hasAuthority: () => true,
    } as any);

    const mockProfile: TraineeProfile = {
      id: 42,
      userId: 7,
      registrationNumber: 'TRN-2026-0042',
      firstName: 'Priya',
      lastName: 'Sharma',
      stateId: 1,
      districtId: 10,
      currentEmploymentStatusId: 1,
      profileStatusId: 3,
    };

    vi.spyOn(traineeApi, 'getTraineeByUserId').mockResolvedValueOnce(mockProfile);

    const { result } = renderHook(() => useTraineeIdentity(), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isLoading).toBe(false));

    expect(result.current.trainee).toEqual(mockProfile);
    expect(result.current.traineeId).toBe(42);
    expect(result.current.isUnlinked).toBe(false);
    expect(result.current.isError).toBe(false);
  });

  it('detects unlinked candidate on HTTP 404 RESOURCE_NOT_FOUND without raising an uncaught application error', async () => {
    const mockUser = {
      id: 99,
      username: 'unlinked_trainee',
      authorities: ['ROLE_trainee', 'trainee.read'],
    };

    vi.spyOn(authContext, 'useAuth').mockReturnValue({
      user: mockUser as any,
      isAuthenticated: true,
      roles: ['trainee'],
      authorities: ['trainee.read'],
      hasRole: () => true,
      hasAuthority: () => true,
    } as any);

    const notFoundError: ApiError = {
      code: 'RESOURCE_NOT_FOUND',
      message: 'Trainee not found with userId: 99',
      status: 404,
    };

    vi.spyOn(traineeApi, 'getTraineeByUserId').mockRejectedValueOnce(notFoundError);

    const { result } = renderHook(() => useTraineeIdentity(), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isLoading).toBe(false));

    expect(result.current.trainee).toBeNull();
    expect(result.current.traineeId).toBeUndefined();
    expect(result.current.isUnlinked).toBe(true);
    // isError should be false for clean unlinked state
    expect(result.current.isError).toBe(false);
    expect(result.current.error).toBeNull();
  });

  it('does not dispatch query when user is unauthenticated', () => {
    vi.spyOn(authContext, 'useAuth').mockReturnValue({
      user: null,
      isAuthenticated: false,
      roles: [],
      authorities: [],
      hasRole: () => false,
      hasAuthority: () => false,
    } as any);

    const spy = vi.spyOn(traineeApi, 'getTraineeByUserId');

    const { result } = renderHook(() => useTraineeIdentity(), {
      wrapper: createWrapper(),
    });

    expect(result.current.isLoading).toBe(false);
    expect(result.current.trainee).toBeNull();
    expect(spy).not.toHaveBeenCalled();
  });
});
