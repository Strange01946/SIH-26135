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

  it('fails and clears session on authoritative 401/403 refresh rejection', async () => {
    setRefreshToken('expired-refresh-token');
    const authError = Object.assign(new Error('Token expired or revoked'), {
      response: { status: 401 },
    });
    vi.mocked(axios.post).mockRejectedValueOnce(authError);

    await expect(refreshAccessToken()).rejects.toThrow('Token expired or revoked');
    expect(getAccessToken()).toBeNull();
    expect(getRefreshToken()).toBeNull();
    expect(hasSession()).toBe(false);
  });

  it('preserves recoverable session state (refresh token) on transient network or 5xx refresh error', async () => {
    setRefreshToken('valid-refresh-token');
    vi.mocked(axios.post).mockRejectedValueOnce(new Error('Network error during refresh'));

    await expect(refreshAccessToken()).rejects.toThrow('Network error during refresh');
    // Session is NOT cleared on temporary network failure
    expect(getRefreshToken()).toBe('valid-refresh-token');
    expect(hasSession()).toBe(true);
  });

  it('prevents race conditions: in-flight refresh resolving after logout (clearSession) does not restore tokens', async () => {
    setRefreshToken('refresh-token-session-1');

    let resolveRefresh: (val: unknown) => void;
    const pendingPromise = new Promise((resolve) => {
      resolveRefresh = resolve;
    });

    vi.mocked(axios.post).mockReturnValueOnce(pendingPromise as never);

    // Refresh begins while user is authenticated in Session 1
    const refreshCall = refreshAccessToken();

    // User logs out while refresh request is in-flight
    clearSession();
    expect(getAccessToken()).toBeNull();
    expect(getRefreshToken()).toBeNull();
    expect(hasSession()).toBe(false);

    // Old refresh request resolves now
    resolveRefresh!({
      data: {
        success: true,
        data: {
          accessToken: 'stale-access-token-session-1',
          refreshToken: 'stale-refresh-token-session-1',
          userId: 1,
          username: 'user1',
        },
      },
    });

    // The refresh promise should reject due to session invalidation
    await expect(refreshCall).rejects.toThrow('Session invalidated during token refresh');

    // Crucial invariant: state must remain completely cleared, not revived
    expect(getAccessToken()).toBeNull();
    expect(getRefreshToken()).toBeNull();
    expect(hasSession()).toBe(false);
  });

  it('prevents race conditions: in-flight refresh for Session 1 resolving after Session 2 login does not overwrite new tokens', async () => {
    setRefreshToken('refresh-token-user-1');

    let resolveRefreshUser1: (val: unknown) => void;
    const pendingPromise = new Promise((resolve) => {
      resolveRefreshUser1 = resolve;
    });

    vi.mocked(axios.post).mockReturnValueOnce(pendingPromise as never);

    // User 1 refresh begins
    const refreshCallUser1 = refreshAccessToken();

    // User 2 logs in while User 1 refresh is in-flight
    setTokens('fresh-access-token-user-2', 'fresh-refresh-token-user-2');
    expect(getAccessToken()).toBe('fresh-access-token-user-2');
    expect(getRefreshToken()).toBe('fresh-refresh-token-user-2');

    // Old User 1 refresh request resolves
    resolveRefreshUser1!({
      data: {
        success: true,
        data: {
          accessToken: 'stale-access-token-user-1',
          refreshToken: 'stale-refresh-token-user-1',
          userId: 1,
          username: 'user1',
        },
      },
    });

    // User 1 refresh call rejects
    await expect(refreshCallUser1).rejects.toThrow('Session invalidated during token refresh');

    // Crucial invariant: User 2 tokens must remain intact and NOT be overwritten by User 1
    expect(getAccessToken()).toBe('fresh-access-token-user-2');
    expect(getRefreshToken()).toBe('fresh-refresh-token-user-2');
  });
});
