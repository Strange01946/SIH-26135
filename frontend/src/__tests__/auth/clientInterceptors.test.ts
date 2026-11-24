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

  it('preserves session on a transient refresh failure and reports that renewal is unavailable', async () => {
    sessionManager.setTokens('expired-token', 'invalid-refresh-token');
    const clearSessionSpy = vi.spyOn(sessionManager, 'clearSession');

    vi.spyOn(sessionManager, 'refreshAccessToken').mockRejectedValue(
      new Error('Network unavailable'),
    );

    mock.onGet('/api/v1/sensitive-data').reply(401, {
      success: false,
      message: 'Unauthorized',
      errorCode: 'UNAUTHORIZED',
      timestamp: new Date().toISOString(),
    });

    await expect(apiGet('/api/v1/sensitive-data')).rejects.toMatchObject({
      status: 503,
      code: 'SESSION_REFRESH_UNAVAILABLE',
    });

    expect(clearSessionSpy).not.toHaveBeenCalled();
    expect(sessionManager.getRefreshToken()).toBe('invalid-refresh-token');
  });

  it('renews an expired access token before dispatching a write', async () => {
    const payload = btoa(JSON.stringify({ exp: Math.floor(Date.now() / 1000) - 1 }));
    sessionManager.setTokens(`test.${payload}.test`, 'stored-refresh-token');
    const refresh = vi.spyOn(sessionManager, 'refreshAccessToken').mockResolvedValue('renewed-token');
    mock.onPost('/api/v1/training-batches').reply(201, { success: true, data: { id: 5 } });

    await expect(apiPost('/api/v1/training-batches', { capacity: 10 })).resolves.toEqual({ id: 5 });
    expect(refresh).toHaveBeenCalledOnce();
    expect(mock.history.post).toHaveLength(1);
    expect(mock.history.post[0].headers?.Authorization).toBe('Bearer renewed-token');
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

  describe('Race Conditions: Stale 401s, Cross-Session Replay Prevention & Logout Invalidation', () => {
    it('prevents cross-session replay: request from older session is NOT replayed with newly authenticated user token', async () => {
      // User 1 logs in
      sessionManager.setTokens('user1-access-token', 'user1-refresh-token');

      let resolveReplyUser1: (response: [number, unknown]) => void;
      const pendingReply = new Promise<[number, unknown]>((resolve) => {
        resolveReplyUser1 = resolve;
      });

      mock.onGet('/api/v1/user1-resource').reply(() => pendingReply);

      // User 1 dispatches request (stamped with User 1 session generation)
      const user1Request = apiGet('/api/v1/user1-resource');
      await Promise.resolve();

      // User 1 logs out and User 2 logs in with distinct tokens
      sessionManager.clearSession();
      sessionManager.setTokens('user2-access-token', 'user2-refresh-token');

      const refreshSpy = vi.spyOn(sessionManager, 'refreshAccessToken');

      // Now User 1's delayed request receives 401
      resolveReplyUser1!([
        401,
        {
          success: false,
          code: 'UNAUTHORIZED',
          message: 'User 1 token expired',
        },
      ]);

      // The request should reject immediately
      await expect(user1Request).rejects.toMatchObject({
        status: 401,
      });

      // INVARIANT 1: User 1 request must NOT trigger refreshAccessToken
      expect(refreshSpy).not.toHaveBeenCalled();

      // INVARIANT 2: User 1 request must NOT be replayed with User 2's token (only 1 outgoing get)
      expect(mock.history.get.length).toBe(1);
      expect(mock.history.get[0].headers?.['Authorization']).toBe('Bearer user1-access-token');

      // INVARIANT 3: User 2's session must remain 100% active and untouched
      expect(sessionManager.getAccessToken()).toBe('user2-access-token');
      expect(sessionManager.getRefreshToken()).toBe('user2-refresh-token');
      expect(sessionManager.hasSession()).toBe(true);
    });

    it('prevents stale refresh failure from calling clearSession on a newer session', async () => {
      // User 1 is active
      sessionManager.setTokens('user1-access-token', 'user1-refresh-token');

      let rejectRefresh: (err: Error) => void;
      const pendingRefresh = new Promise<string>((_resolve, reject) => {
        rejectRefresh = reject;
      });
      pendingRefresh.catch(() => {}); // Prevent unhandled rejection event in test runner

      // Spy on refreshAccessToken returning the pending promise
      vi.spyOn(sessionManager, 'refreshAccessToken').mockReturnValue(pendingRefresh);

      // User 1 sends request that gets 401
      mock.onGet('/api/v1/stale-request').reply(401, {
        success: false,
        code: 'UNAUTHORIZED',
      });

      const requestPromise = apiGet('/api/v1/stale-request');
      // Wait for request interceptor to dispatch and 401 to trigger refreshAccessToken
      await Promise.resolve();
      await Promise.resolve();

      // While refresh is pending on the network, User 1 logs out and User 2 logs in
      sessionManager.clearSession();
      sessionManager.setTokens('user2-fresh-token', 'user2-fresh-refresh');

      const clearSessionSpy = vi.spyOn(sessionManager, 'clearSession');

      // Now the old refresh failure completes/rejects
      rejectRefresh!(new Error('User 1 refresh token revoked'));

      await expect(requestPromise).rejects.toMatchObject({
        status: 401,
      });

      // INVARIANT: clearSession must NOT be called on User 2's active session!
      expect(clearSessionSpy).not.toHaveBeenCalled();
      expect(sessionManager.getAccessToken()).toBe('user2-fresh-token');
      expect(sessionManager.getRefreshToken()).toBe('user2-fresh-refresh');
      expect(sessionManager.hasSession()).toBe(true);
    });

    it('aborts request replay if user logs out while token refresh is in flight', async () => {
      sessionManager.setTokens('user1-token', 'user1-refresh');

      let resolveRefresh: (token: string) => void;
      const pendingRefresh = new Promise<string>((resolve) => {
        resolveRefresh = resolve;
      });

      vi.spyOn(sessionManager, 'refreshAccessToken').mockReturnValue(pendingRefresh);

      mock
        .onGet('/api/v1/data')
        .replyOnce(401, { success: false, code: 'UNAUTHORIZED' })
        .onGet('/api/v1/data')
        .replyOnce(200, { success: true, data: 'secret' });

      const req = apiGet('/api/v1/data');
      await Promise.resolve();
      await Promise.resolve();

      // User logs out while refresh is in flight
      sessionManager.clearSession();

      // Refresh completes
      resolveRefresh!('renewed-token');

      // Request must reject, and MUST NOT replay to the network
      await expect(req).rejects.toMatchObject({
        status: 401,
      });

      // Exactly 1 GET request made (initial 401), replay was aborted!
      expect(mock.history.get.length).toBe(1);
      expect(sessionManager.getAccessToken()).toBeNull();
      expect(sessionManager.hasSession()).toBe(false);
    });

    it('rejects delayed 401 arriving after logout without triggering refresh or redundant session clearance', async () => {
      sessionManager.setTokens('initial-token', 'initial-refresh');

      let resolve401: (val: [number, unknown]) => void;
      const delayedReply = new Promise<[number, unknown]>((resolve) => {
        resolve401 = resolve;
      });

      mock.onGet('/api/v1/delayed-endpoint').reply(() => delayedReply);

      const requestPromise = apiGet('/api/v1/delayed-endpoint');
      await Promise.resolve();

      // User logs out before server responds
      sessionManager.clearSession();

      const refreshSpy = vi.spyOn(sessionManager, 'refreshAccessToken');
      const clearSessionSpy = vi.spyOn(sessionManager, 'clearSession');

      // Server finally responds with 401
      resolve401!([401, { success: false, code: 'UNAUTHORIZED' }]);

      await expect(requestPromise).rejects.toMatchObject({
        status: 401,
      });

      // Stale 401 must not trigger refresh or redundant clearSession
      expect(refreshSpy).not.toHaveBeenCalled();
      expect(clearSessionSpy).not.toHaveBeenCalled();
    });

    it('confirms that an unrelated failure (e.g. 500 or 404) of a replayed GET request does not trigger session clearance', async () => {
      sessionManager.setTokens('valid-token', 'valid-refresh');

      const clearSessionSpy = vi.spyOn(sessionManager, 'clearSession');
      vi.spyOn(sessionManager, 'refreshAccessToken').mockResolvedValue('new-access-token');

      // First call fails with 401 (triggers refresh), replayed call fails with 500 (server error)
      mock
        .onGet('/api/v1/service-endpoint')
        .replyOnce(401, { success: false, code: 'TOKEN_EXPIRED' })
        .onGet('/api/v1/service-endpoint')
        .replyOnce(500, { success: false, code: 'INTERNAL_SERVER_ERROR', message: 'Database query timeout' });

      const requestPromise = apiGet('/api/v1/service-endpoint');

      await expect(requestPromise).rejects.toMatchObject({
        status: 500,
      });

      // Crucial Invariant: clearSession must NOT be called for an unrelated 500 on the replayed request!
      expect(clearSessionSpy).not.toHaveBeenCalled();
      expect(sessionManager.hasSession()).toBe(true);
      expect(mock.history.get.length).toBe(2);
      expect(mock.history.get[1].headers?.Authorization).toBe('Bearer new-access-token');
    });
  });
});
