import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import MockAdapter from 'axios-mock-adapter';
import { apiClient, apiGet, apiPost, apiPut, apiPatch, apiDelete } from '@/api/client';
import * as sessionManager from '@/lib/sessionManager';

describe('API Client Auth Interceptors & Refresh Loop Protection', () => {
  let mock: MockAdapter;

  beforeEach(() => {
    mock = new MockAdapter(apiClient);
    sessionStorage.clear();
    sessionManager.clearSession();
    vi.restoreAllMocks();
  });

  afterEach(() => {
    mock.restore();
    sessionStorage.clear();
    sessionManager.clearSession();
  });

  it('attaches Bearer token to request when access token is present', async () => {
    sessionManager.setAccessToken('sample-access-jwt');

    mock.onGet('/api/v1/protected-data').reply(200, {
      success: true,
      message: 'Success',
      data: { secret: 'verified' },
      timestamp: new Date().toISOString(),
    });

    await apiGet('/api/v1/protected-data');

    expect(mock.history.get.length).toBe(1);
    expect(mock.history.get[0].headers?.['Authorization']).toBe('Bearer sample-access-jwt');
  });

  it('does not attach Authorization header when no access token is set', async () => {
    mock.onGet('/api/v1/public-data').reply(200, {
      success: true,
      message: 'Success',
      data: { public: true },
      timestamp: new Date().toISOString(),
    });

    await apiGet('/api/v1/public-data');

    expect(mock.history.get.length).toBe(1);
    expect(mock.history.get[0].headers?.['Authorization']).toBeUndefined();
  });

  it('automatically triggers silent refresh on 401 and replays request with renewed token', async () => {
    // Setup initial session
    sessionManager.setTokens('initial-expired-token', 'stored-refresh-token');

    // Spy on refreshAccessToken
    const refreshSpy = vi.spyOn(sessionManager, 'refreshAccessToken').mockResolvedValue('renewed-jwt-token');

    // First call returns 401, second call (retried) returns 200
    mock
      .onGet('/api/v1/user/profile')
      .replyOnce(401, {
        success: false,
        message: 'JWT expired',
        errorCode: 'UNAUTHORIZED',
        timestamp: new Date().toISOString(),
      })
      .onGet('/api/v1/user/profile')
      .replyOnce(200, {
        success: true,
        message: 'Success',
        data: { name: 'Admin Officer' },
        timestamp: new Date().toISOString(),
      });

    const result = await apiGet<{ name: string }>('/api/v1/user/profile');

    expect(refreshSpy).toHaveBeenCalledTimes(1);
    expect(result.name).toBe('Admin Officer');
    expect(mock.history.get.length).toBe(2);
    expect(mock.history.get[1].headers?.['Authorization']).toBe('Bearer renewed-jwt-token');
  });

  it('prevents refresh loop on 401 from /api/v1/auth/login and rejects immediately', async () => {
    const refreshSpy = vi.spyOn(sessionManager, 'refreshAccessToken');

    mock.onPost('/api/v1/auth/login').reply(401, {
      success: false,
      message: 'Bad credentials',
      code: 'INVALID_CREDENTIALS',
      timestamp: new Date().toISOString(),
    });

    await expect(
      apiPost('/api/v1/auth/login', { usernameOrEmail: 'bad', password: 'wrong' }),
    ).rejects.toMatchObject({
      status: 401,
      code: 'INVALID_CREDENTIALS',
    });

    expect(refreshSpy).not.toHaveBeenCalled();
  });

  it('prevents refresh loop on 401 from /api/v1/auth/refresh and rejects immediately', async () => {
    const refreshSpy = vi.spyOn(sessionManager, 'refreshAccessToken');

    mock.onPost('/api/v1/auth/refresh').reply(401, {
      success: false,
      message: 'Refresh token expired',
      errorCode: 'TOKEN_EXPIRED',
      timestamp: new Date().toISOString(),
    });

    await expect(
      apiPost('/api/v1/auth/refresh', { refreshToken: 'expired' }),
    ).rejects.toMatchObject({
      status: 401,
    });

    expect(refreshSpy).not.toHaveBeenCalled();
  });

  it('clears session when silent token refresh fails and rejects the original call', async () => {
    sessionManager.setTokens('expired-token', 'invalid-refresh-token');
    const clearSessionSpy = vi.spyOn(sessionManager, 'clearSession');

    vi.spyOn(sessionManager, 'refreshAccessToken').mockRejectedValue(
      new Error('Refresh failed with 401'),
    );

    mock.onGet('/api/v1/sensitive-data').reply(401, {
      success: false,
      message: 'Unauthorized',
      errorCode: 'UNAUTHORIZED',
      timestamp: new Date().toISOString(),
    });

    await expect(apiGet('/api/v1/sensitive-data')).rejects.toMatchObject({
      status: 401,
    });

    expect(clearSessionSpy).toHaveBeenCalled();
  });

  it('does not retry infinitely if retried request still returns 401 (_retry guard)', async () => {
    sessionManager.setTokens('initial-token', 'valid-refresh-token');
    const refreshSpy = vi.spyOn(sessionManager, 'refreshAccessToken').mockResolvedValue('new-token');

    // Both first attempt and retried attempt return 401
    mock.onGet('/api/v1/persistent-401').reply(401, {
      success: false,
      message: 'Permanent 401',
      errorCode: 'UNAUTHORIZED',
      timestamp: new Date().toISOString(),
    });

    await expect(apiGet('/api/v1/persistent-401')).rejects.toMatchObject({
      status: 401,
    });

    // Refresh must only have been invoked ONCE, not looping
    expect(refreshSpy).toHaveBeenCalledTimes(1);
    expect(mock.history.get.length).toBe(2);
  });

  describe('Unsafe Mutation Request Replay Protection', () => {
    it('does NOT automatically replay POST request encountering 401 to prevent duplicate operations', async () => {
      sessionManager.setTokens('expired-token', 'valid-refresh-token');
      const refreshSpy = vi.spyOn(sessionManager, 'refreshAccessToken').mockResolvedValue('fresh-token');

      mock.onPost('/api/v1/training-batches').reply(401, {
        success: false,
        message: 'JWT expired',
        code: 'UNAUTHORIZED',
      });

      await expect(
        apiPost('/api/v1/training-batches', { batchName: 'Welding 2026' }),
      ).rejects.toMatchObject({
        status: 401,
      });

      // Crucial invariant: mutation must NOT have been replayed
      expect(mock.history.post.length).toBe(1);
      // Background refresh triggered so future user actions succeed
      expect(refreshSpy).toHaveBeenCalledTimes(1);
    });

    it('does NOT automatically replay PUT, PATCH, or DELETE requests encountering 401', async () => {
      sessionManager.setTokens('expired-token', 'valid-refresh-token');
      vi.spyOn(sessionManager, 'refreshAccessToken').mockResolvedValue('fresh-token');

      mock.onPut('/api/v1/trainees/1').reply(401, { success: false, code: 'UNAUTHORIZED' });
      mock.onPatch('/api/v1/assessments/1').reply(401, { success: false, code: 'UNAUTHORIZED' });
      mock.onDelete('/api/v1/batches/1').reply(401, { success: false, code: 'UNAUTHORIZED' });

      await expect(apiPut('/api/v1/trainees/1', { name: 'Bob' })).rejects.toMatchObject({ status: 401 });
      await expect(apiPatch('/api/v1/assessments/1', { score: 95 })).rejects.toMatchObject({ status: 401 });
      await expect(apiDelete('/api/v1/batches/1')).rejects.toMatchObject({ status: 401 });

      expect(mock.history.put.length).toBe(1);
      expect(mock.history.patch.length).toBe(1);
      expect(mock.history.delete.length).toBe(1);
    });

    it('replays mutation request when explicitly marked with X-Idempotent: true header', async () => {
      sessionManager.setTokens('expired-token', 'valid-refresh-token');
      vi.spyOn(sessionManager, 'refreshAccessToken').mockResolvedValue('renewed-token');

      mock
        .onPost('/api/v1/idempotent-action')
        .replyOnce(401, { success: false, code: 'UNAUTHORIZED' })
        .onPost('/api/v1/idempotent-action')
        .replyOnce(200, {
          success: true,
          message: 'Idempotent Success',
          data: { confirmed: true },
          timestamp: new Date().toISOString(),
        });

      const result = await apiPost<{ actionId: string }, { confirmed: boolean }>(
        '/api/v1/idempotent-action',
        { actionId: 'sync-1' },
        { headers: { 'X-Idempotent': 'true' } },
      );

      expect(result.confirmed).toBe(true);
      expect(mock.history.post.length).toBe(2);
      expect(mock.history.post[1].headers?.['Authorization']).toBe('Bearer renewed-token');
    });

    it('handles concurrent 401 responses with mixed safe (GET) and unsafe (POST) methods', async () => {
      sessionManager.setTokens('expired-token', 'valid-refresh-token');
      const refreshSpy = vi.spyOn(sessionManager, 'refreshAccessToken').mockResolvedValue('coalesced-token');

      // GET safe read
      mock
        .onGet('/api/v1/read-data')
        .replyOnce(401, { success: false, code: 'UNAUTHORIZED' })
        .onGet('/api/v1/read-data')
        .replyOnce(200, {
          success: true,
          message: 'OK',
          data: { value: 42 },
          timestamp: new Date().toISOString(),
        });

      // POST unsafe mutation
      mock.onPost('/api/v1/mutate-data').reply(401, {
        success: false,
        message: 'Unauthorized mutation',
        code: 'UNAUTHORIZED',
      });

      // Fire both concurrently
      const [getResult, postError] = await Promise.allSettled([
        apiGet<{ value: number }>('/api/v1/read-data'),
        apiPost('/api/v1/mutate-data', { payload: 'important' }),
      ]);

      // GET succeeded via replay
      expect(getResult.status).toBe('fulfilled');
      if (getResult.status === 'fulfilled') {
        expect(getResult.value.value).toBe(42);
      }

      // POST failed without replay
      expect(postError.status).toBe('rejected');
      expect(mock.history.post.length).toBe(1);

      // Both requests trigger refreshAccessToken (which coalesces internally via sessionManager)
      expect(refreshSpy).toHaveBeenCalledTimes(2);
    });
  });
});
