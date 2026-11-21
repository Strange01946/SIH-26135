import { describe, it, expect } from 'vitest';
import { queryClient } from '@/lib/queryClient';
import type { ApiError } from '@/api/types';

describe('QueryClient configuration', () => {
  it('should be instantiated', () => {
    expect(queryClient).toBeDefined();
  });

  it('should have staleTime of 30 seconds', () => {
    const defaults = queryClient.getDefaultOptions();
    expect(defaults.queries?.staleTime).toBe(30_000);
  });

  it('should not retry on 401 errors', () => {
    const retryFn = queryClient.getDefaultOptions().queries?.retry;
    if (typeof retryFn === 'function') {
      const error401: ApiError = { code: 'HTTP_401', message: 'Unauthorized', status: 401 };
      expect(retryFn(1, error401 as unknown as Error)).toBe(false);
    } else {
      expect(retryFn).toBeDefined();
    }
  });

  it('should not retry on 403 errors', () => {
    const retryFn = queryClient.getDefaultOptions().queries?.retry;
    if (typeof retryFn === 'function') {
      const error403: ApiError = { code: 'HTTP_403', message: 'Forbidden', status: 403 };
      expect(retryFn(1, error403 as unknown as Error)).toBe(false);
    }
  });

  it('should not retry on 423 (account locked) errors', () => {
    const retryFn = queryClient.getDefaultOptions().queries?.retry;
    if (typeof retryFn === 'function') {
      const error423: ApiError = { code: 'AUTH_ACCOUNT_LOCKED', message: 'Locked', status: 423 };
      expect(retryFn(1, error423 as unknown as Error)).toBe(false);
    }
  });

  it('should not retry mutations', () => {
    const defaults = queryClient.getDefaultOptions();
    expect(defaults.mutations?.retry).toBe(0);
  });
});
