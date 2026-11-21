import { useQuery } from '@tanstack/react-query';
import { useAuth } from '@/context/AuthContext';
import { getTraineeByUserId } from '@/api/traineeApi';
import type { TraineeProfile } from '@/types/trainee';
import type { ApiError } from '@/api/types';

export interface UseTraineeIdentityResult {
  trainee: TraineeProfile | null;
  traineeId: number | undefined;
  isLoading: boolean;
  isUnlinked: boolean;
  isError: boolean;
  error: ApiError | Error | null;
  refetch: () => Promise<unknown>;
}

/**
 * Hook to resolve the authenticated user's Trainee Profile.
 * 
 * Automatically resolves the linkage between auth User ID (e.g. from /auth/me)
 * and the operational Trainee record (via GET /api/v1/trainees?userId={userId}).
 * 
 * If the user has the trainee role but no linked profile exists in the trainees table,
 * the backend returns HTTP 404 RESOURCE_NOT_FOUND. This hook intercepts that condition
 * and flags isUnlinked = true, allowing the UI to render an actionable linkage notice
 * rather than an unhandled application error.
 */
export function useTraineeIdentity(): UseTraineeIdentityResult {
  const { user, isAuthenticated } = useAuth();
  const userId = user?.id;

  const query = useQuery<TraineeProfile, ApiError | Error>({
    queryKey: ['trainee', 'identity', userId],
    queryFn: async () => {
      if (!userId) {
        throw new Error('User is not authenticated');
      }
      return getTraineeByUserId(userId);
    },
    enabled: isAuthenticated && Boolean(userId),
    staleTime: 5 * 60 * 1000,
    retry: (failureCount, error: any) => {
      // Do not retry 404 (unlinked candidate) or 403 (unauthorized)
      if (error?.status === 404 || error?.status === 403) {
        return false;
      }
      return failureCount < 2;
    },
  });

  const apiError = query.error as ApiError | undefined;
  const isUnlinked = Boolean(
    apiError &&
      (apiError.status === 404 ||
        apiError.code === 'RESOURCE_NOT_FOUND' ||
        apiError.message?.toLowerCase().includes('not found')),
  );

  return {
    trainee: query.data ?? null,
    traineeId: query.data?.id,
    isLoading: query.isLoading,
    isUnlinked,
    isError: query.isError && !isUnlinked,
    error: isUnlinked ? null : (query.error ?? null),
    refetch: query.refetch,
  };
}
