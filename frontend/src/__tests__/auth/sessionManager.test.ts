import { describe, it, expect, beforeEach, vi } from 'vitest';
import axios from 'axios';
import {
  getAccessToken,
  setAccessToken,
  getRefreshToken,
  setRefreshToken,
  hasSession,
  setTokens,
  clearSession,
  refreshAccessToken,
  subscribeToSession,
} from '@/lib/sessionManager';
import { queryClient } from '@/lib/queryClient';

vi.mock('axios');

describe('Session Manager (lib/sessionManager)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    sessionStorage.clear();
    clearSession();
  });

  it('manages in-memory access token without storing it in sessionStorage', () => {
    expect(getAccessToken()).toBeNull();
    setAccessToken('jwt-access-token-123');
    expect(getAccessToken()).toBe('jwt-access-token-123');
    expect(sessionStorage.getItem('sih_access_token')).toBeNull();
  });

  it('stores and retrieves refresh token from sessionStorage', () => {
    expect(getRefreshToken()).toBeNull();
    setRefreshToken('refresh-token-xyz');
    expect(getRefreshToken()).toBe('refresh-token-xyz');
    expect(sessionStorage.getItem('sih_refresh_token')).toBe('refresh-token-xyz');
    expect(hasSession()).toBe(true);

    setRefreshToken(null);
    expect(getRefreshToken()).toBeNull();
    expect(sessionStorage.getItem('sih_refresh_token')).toBeNull();
    expect(hasSession()).toBe(false);
  });

  it('atomically sets access and refresh tokens and notifies subscribers', () => {
    const listener = vi.fn();
    const unsubscribe = subscribeToSession(listener);

    setTokens('access-token-abc', 'refresh-token-def');

    expect(getAccessToken()).toBe('access-token-abc');
    expect(getRefreshToken()).toBe('refresh-token-def');
    expect(listener).toHaveBeenCalledWith(true);

    unsubscribe();
  });

  it('clears session, purges query cache, and notifies subscribers (Cache Isolation)', () => {
    const queryClearSpy = vi.spyOn(queryClient, 'clear');
    const listener = vi.fn();
    const unsubscribe = subscribeToSession(listener);

    setTokens('token-1', 'refresh-1');
    listener.mockClear();

    clearSession();

    expect(getAccessToken()).toBeNull();
    expect(getRefreshToken()).toBeNull();
    expect(hasSession()).toBe(false);
    expect(queryClearSpy).toHaveBeenCalled();
    expect(listener).toHaveBeenCalledWith(false);

    unsubscribe();
  });

  it('coalesces concurrent token refresh calls into a single network request', async () => {
    setRefreshToken('valid-refresh-token');

    let resolvePromise: (value: unknown) => void;
    const pendingPromise = new Promise((resolve) => {
      resolvePromise = resolve;
    });

    vi.mocked(axios.post).mockReturnValueOnce(pendingPromise as never);

    // Call refreshAccessToken concurrently 3 times
    const call1 = refreshAccessToken();
    const call2 = refreshAccessToken();
    const call3 = refreshAccessToken();

    // Verify axios.post was only called ONCE
    expect(axios.post).toHaveBeenCalledTimes(1);

    // Resolve the single in-flight request
    resolvePromise!({
      data: {
        success: true,
        data: {
          accessToken: 'renewed-access-token',
          refreshToken: 'rotated-refresh-token',
          userId: 'usr-1',
          username: 'testuser',
        },
      },
    });

    const [res1, res2, res3] = await Promise.all([call1, call2, call3]);
    expect(res1).toBe('renewed-access-token');
    expect(res2).toBe('renewed-access-token');
    expect(res3).toBe('renewed-access-token');
    expect(getAccessToken()).toBe('renewed-access-token');
    expect(getRefreshToken()).toBe('rotated-refresh-token');
  });

  it('fails and clears session when no refresh token is present', async () => {
    sessionStorage.clear();
    await expect(refreshAccessToken()).rejects.toThrow('No refresh token available');
    expect(getAccessToken()).toBeNull();
  });

  it('fails and clears session on refresh API failure', async () => {
    setRefreshToken('bad-refresh-token');
    vi.mocked(axios.post).mockRejectedValueOnce(new Error('Network error during refresh'));

    await expect(refreshAccessToken()).rejects.toThrow('Network error during refresh');
    expect(getAccessToken()).toBeNull();
    expect(getRefreshToken()).toBeNull();
    expect(hasSession()).toBe(false);
  });
});
