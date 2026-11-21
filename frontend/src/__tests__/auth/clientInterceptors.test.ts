import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import MockAdapter from 'axios-mock-adapter';
import { apiClient, apiGet, apiPost } from '@/api/client';
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
});
