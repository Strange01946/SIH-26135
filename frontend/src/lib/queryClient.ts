import { QueryClient } from '@tanstack/react-query';
import type { ApiError } from '@/api/types';

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000, // 30 seconds
      gcTime: 5 * 60_000, // 5 minutes
      retry: (failureCount: number, error: unknown) => {
        const apiError = error as ApiError;
        // Do not retry auth errors, not-found, or forbidden
        if ([401, 403, 404, 423].includes(apiError?.status)) return false;
        return failureCount < 1;
      },
      refetchOnWindowFocus: false,
    },
    mutations: {
      retry: 0,
    },
  },
});
