import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import MockAdapter from 'axios-mock-adapter';
import {
  apiClient,
  apiGet,
  apiGetPaged,
  apiGetEnvelope,
  apiPost,
  apiPostEnvelope,
  apiPut,
  apiPatch,
  apiDelete,
  apiUpload,
  apiDownload,
  setAccessToken,
  clearTokens,
} from '@/api/client';
import type { ApiError, ApiResponse } from '@/api/types';

describe('API Client Runtime Behavior & Integration Tests', () => {
  let mock: MockAdapter;

  beforeEach(() => {
    mock = new MockAdapter(apiClient);
    clearTokens();
  });

  afterEach(() => {
    mock.restore();
    clearTokens();
  });

  // 1. Correlation IDs
  it('attaches X-Correlation-ID (UUID v4) to actual outgoing requests', async () => {
    mock.onGet('/api/v1/health').reply(200, {
      success: true,
      message: 'OK',
      data: { status: 'UP' },
      timestamp: '2026-11-21T12:00:00Z',
    });

    await apiGet('/api/v1/health');

    expect(mock.history.get.length).toBe(1);
    const correlationHeader = mock.history.get[0].headers?.['X-Correlation-ID'];
    expect(correlationHeader).toBeDefined();

    const uuidV4Regex =
      /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
    expect(uuidV4Regex.test(String(correlationHeader))).toBe(true);
  });

  // 2. Bearer Token Authorization
  it('attaches Authorization Bearer token to outgoing request when set', async () => {
    setAccessToken('eyJhbGciOiJIUzI1NiJ9.test.sig');

    mock.onGet('/api/v1/auth/me').reply(200, {
      success: true,
      message: 'Profile retrieved',
      data: { username: 'admin', userId: 1 },
      timestamp: '2026-11-21T12:00:00Z',
    });

    await apiGet('/api/v1/auth/me');

    expect(mock.history.get.length).toBe(1);
    const authHeader = mock.history.get[0].headers?.['Authorization'];
    expect(authHeader).toBe('Bearer eyJhbGciOiJIUzI1NiJ9.test.sig');
  });

  // 3. Envelope Unwrapping
  it('unwraps ApiResponse<T>.data for successful GET requests', async () => {
    mock.onGet('/api/v1/trainees/42').reply(200, {
      success: true,
      code: 'OPERATION_SUCCESS',
      message: 'Trainee retrieved',
      data: { id: 42, fullName: 'Aarav Sharma' },
      timestamp: '2026-11-21T12:00:00Z',
      path: '/api/v1/trainees/42',
      correlationId: 'c8f9b2a1-0000-4000-a000-000000000001',
    });

    const data = await apiGet<{ id: number; fullName: string }>('/api/v1/trainees/42');
    expect(data).toEqual({ id: 42, fullName: 'Aarav Sharma' });
  });

  it('unwraps ApiResponse<PagedResponse<T>>.data for paged requests', async () => {
    mock.onGet('/api/v1/trainees').reply(200, {
      success: true,
      message: 'Trainees list',
      data: {
        content: [{ id: 1 }, { id: 2 }],
        page: 0,
        size: 20,
        totalElements: 2,
        totalPages: 1,
        first: true,
        last: true,
        hasNext: false,
        hasPrevious: false,
      },
      timestamp: '2026-11-21T12:00:00Z',
    });

    const paged = await apiGetPaged<{ id: number }>('/api/v1/trainees');
    expect(paged.content).toHaveLength(2);
    expect(paged.totalElements).toBe(2);
    expect(paged.first).toBe(true);
  });

  // 4. Envelope Metadata Access
  it('preserves full ApiResponse envelope metadata when using apiGetEnvelope', async () => {
    mock.onGet('/api/v1/trainees/42').reply(200, {
      success: true,
      code: 'OPERATION_SUCCESS',
      message: 'Trainee retrieved',
      data: { id: 42 },
      timestamp: '2026-11-21T12:00:00Z',
      path: '/api/v1/trainees/42',
      correlationId: 'test-corr-42',
    });

    const envelope: ApiResponse<{ id: number }> =
      await apiGetEnvelope<{ id: number }>('/api/v1/trainees/42');

    expect(envelope.success).toBe(true);
    expect(envelope.code).toBe('OPERATION_SUCCESS');
    expect(envelope.message).toBe('Trainee retrieved');
    expect(envelope.data).toEqual({ id: 42 });
    expect(envelope.correlationId).toBe('test-corr-42');
    expect(envelope.path).toBe('/api/v1/trainees/42');
  });

  // 5. Defensive Rejection on success: false
  it('rejects when backend returns HTTP 200 with success: false', async () => {
    mock.onPost('/api/v1/actions/execute').reply(200, {
      success: false,
      code: 'PRECONDITION_FAILED',
      message: 'Preconditions not met for workflow',
      data: null,
      timestamp: '2026-11-21T12:00:00Z',
    });

    await expect(apiPost('/api/v1/actions/execute', {})).rejects.toMatchObject({
      code: 'PRECONDITION_FAILED',
      message: 'Preconditions not met for workflow',
      status: 200,
    });
  });

  // 6. HTTP Error Normalization
  it('normalizes HTTP 401 Unauthorized errors correctly', async () => {
    mock.onGet('/api/v1/secure').reply(401, {
      status: 401,
      error: 'Unauthorized',
      code: 'AUTH_INVALID_TOKEN',
      message: 'JWT token expired or invalid',
      path: '/api/v1/secure',
    });

    try {
      await apiGet('/api/v1/secure');
      expect.fail('Should have thrown an error');
    } catch (err) {
      const apiError = err as ApiError;
      expect(apiError.status).toBe(401);
      expect(apiError.code).toBe('AUTH_INVALID_TOKEN');
      expect(apiError.message).toBe('JWT token expired or invalid');
    }
  });

  it('normalizes HTTP 403 Forbidden errors correctly', async () => {
    mock.onGet('/api/v1/admin/users').reply(403, {
      status: 403,
      error: 'Forbidden',
      code: 'ACCESS_DENIED',
      message: 'Insufficient authorities for user management',
    });

    await expect(apiGet('/api/v1/admin/users')).rejects.toMatchObject({
      status: 403,
      code: 'ACCESS_DENIED',
      message: 'Insufficient authorities for user management',
    });
  });

  it('normalizes HTTP 423 Account Lockout correctly', async () => {
    mock.onPost('/api/v1/auth/login').reply(423, {
      status: 423,
      error: 'Locked',
      code: 'AUTH_ACCOUNT_LOCKED',
      message: 'Account locked due to consecutive failed authentication attempts',
      path: '/api/v1/auth/login',
    });

    await expect(
      apiPost('/api/v1/auth/login', { usernameOrEmail: 'admin', password: 'bad' }),
    ).rejects.toMatchObject({
      status: 423,
      code: 'AUTH_ACCOUNT_LOCKED',
      message: 'Account locked due to consecutive failed authentication attempts',
    });
  });

  // 7. Field-Level Validation Details (Spring Boot ErrorResponse format)
  it('parses field-level validation errors from backend ErrorResponse details', async () => {
    mock.onPost('/api/v1/trainees').reply(400, {
      status: 400,
      error: 'Bad Request',
      code: 'VALIDATION_ERROR',
      message: 'Request validation failed',
      path: '/api/v1/trainees',
      details: [
        'aadhaarNumber: Aadhaar number must be 12 digits',
        'email: Must be a well-formed email address',
        'dateOfBirth: Date of birth cannot be in the future',
      ],
      correlationId: 'c8f9b2a1-0000-4000-a000-000000000001',
    });

    try {
      await apiPost('/api/v1/trainees', {});
      expect.fail('Should have failed validation');
    } catch (err) {
      const apiError = err as ApiError;
      expect(apiError.status).toBe(400);
      expect(apiError.code).toBe('VALIDATION_ERROR');
      expect(apiError.details).toHaveLength(3);
      expect(apiError.fieldErrors).toBeDefined();
      expect(apiError.fieldErrors).toHaveLength(3);
      expect(apiError.fieldErrors?.[0]).toEqual({
        field: 'aadhaarNumber',
        message: 'Aadhaar number must be 12 digits',
      });
      expect(apiError.fieldErrors?.[1]).toEqual({
        field: 'email',
        message: 'Must be a well-formed email address',
      });
      expect(apiError.fieldErrors?.[2]).toEqual({
        field: 'dateOfBirth',
        message: 'Date of birth cannot be in the future',
      });
    }
  });

  // 8. Network Failures & Timeouts
  it('handles network failure safely with isNetworkError flag', async () => {
    mock.onGet('/api/v1/offline').networkError();

    try {
      await apiGet('/api/v1/offline');
      expect.fail('Should have thrown network error');
    } catch (err) {
      const apiError = err as ApiError;
      expect(apiError.code).toBe('NETWORK_ERROR');
      expect(apiError.status).toBe(0);
      expect(apiError.isNetworkError).toBe(true);
      expect(apiError.message).toContain('network');
    }
  });

  it('handles request timeout safely with isTimeout flag', async () => {
    mock.onGet('/api/v1/slow').timeout();

    try {
      await apiGet('/api/v1/slow');
      expect.fail('Should have thrown timeout error');
    } catch (err) {
      const apiError = err as ApiError;
      expect(apiError.code).toBe('REQUEST_TIMEOUT');
      expect(apiError.status).toBe(408);
      expect(apiError.isTimeout).toBe(true);
      expect(apiError.message).toContain('timed out');
    }
  });

  // 9. Multipart Request Handling
  it('does not send application/json Content-Type for FormData uploads', async () => {
    mock.onPost('/api/v1/import-batches').reply(200, {
      success: true,
      message: 'Batch uploaded',
      data: { batchId: 99, status: 'PROCESSING' },
      timestamp: '2026-11-21T12:00:00Z',
    });

    const formData = new FormData();
    formData.append('file', new Blob(['test,data'], { type: 'text/csv' }), 'test.csv');
    formData.append('entityType', 'TRAINEE');

    const result = await apiUpload<{ batchId: number }>('/api/v1/import-batches', formData);
    expect(result).toEqual({ batchId: 99, status: 'PROCESSING' });

    expect(mock.history.post.length).toBe(1);
    const contentType = mock.history.post[0].headers?.['Content-Type'];
    // In browser/mock environment, application/json must not be present
    expect(contentType).not.toBe('application/json');
  });

  // 10. Binary Downloads
  it('bypasses JSON envelope unwrapping for binary downloads', async () => {
    const csvContent = 'id,name,role\n1,Rajesh,Trainee\n2,Priya,Trainer';
    const blob = new Blob([csvContent], { type: 'text/csv' });

    mock.onGet('/api/v1/analytics/exports/trainees').reply(200, blob, {
      'content-type': 'text/csv',
    });

    const downloadedBlob = await apiDownload('/api/v1/analytics/exports/trainees');
    expect(downloadedBlob).toBeDefined();
    expect(downloadedBlob).toBeInstanceOf(Blob);
  });

  // 11. Empty Response Handling (204 No Content)
  it('handles 204 No Content responses safely for DELETE', async () => {
    mock.onDelete('/api/v1/trainees/99').reply(204);

    await expect(apiDelete('/api/v1/trainees/99')).resolves.toBeUndefined();
    expect(mock.history.delete.length).toBe(1);
  });

  // 12. Request Cancellation
  it('handles request cancellation via AbortController correctly', async () => {
    mock.onGet('/api/v1/cancelable').reply(() => new Promise(() => {})); // Never resolves

    const controller = new AbortController();
    const promise = apiGet('/api/v1/cancelable', { signal: controller.signal });

    controller.abort();

    try {
      await promise;
      expect.fail('Should have been canceled');
    } catch (err) {
      const apiError = err as ApiError;
      expect(apiError.code).toBe('REQUEST_CANCELED');
      expect(apiError.isCanceled).toBe(true);
      expect(apiError.status).toBe(0);
    }
  });

  // 13. PUT and PATCH operations
  it('unwraps data payload for PUT and PATCH requests', async () => {
    mock.onPut('/api/v1/trainees/1').reply(200, {
      success: true,
      message: 'Updated',
      data: { id: 1, updated: true },
    });
    mock.onPatch('/api/v1/trainees/1').reply(200, {
      success: true,
      message: 'Patched',
      data: { id: 1, patched: true },
    });

    const putData = await apiPut('/api/v1/trainees/1', { name: 'New' });
    expect(putData).toEqual({ id: 1, updated: true });

    const patchData = await apiPatch('/api/v1/trainees/1', { status: 'ACTIVE' });
    expect(patchData).toEqual({ id: 1, patched: true });
  });

  // 14. Envelope helpers for POST
  it('supports apiPostEnvelope to access correlationId and code on creation', async () => {
    mock.onPost('/api/v1/trainees').reply(201, {
      success: true,
      code: 'TRAINEE_CREATED',
      message: 'Trainee registered successfully',
      data: { id: 50 },
      correlationId: 'reg-corr-123',
    });

    const envelope = await apiPostEnvelope('/api/v1/trainees', { name: 'Vikram' });
    expect(envelope.code).toBe('TRAINEE_CREATED');
    expect(envelope.correlationId).toBe('reg-corr-123');
    expect(envelope.data).toEqual({ id: 50 });
  });
});
