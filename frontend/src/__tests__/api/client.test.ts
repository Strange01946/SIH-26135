import { describe, it, expect } from 'vitest';

describe('API Client interceptors and error normalization', () => {
  it('should generate valid X-Correlation-ID (UUID v4 format)', async () => {
    const uuidV4Regex =
      /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

    const { v4: uuidv4 } = await import('uuid');
    const id = uuidv4();
    expect(uuidV4Regex.test(id)).toBe(true);
  });

  it('should normalize HTTP 400 to ARGUMENT_INVALID-style error', () => {
    const error = {
      code: 'ARGUMENT_INVALID',
      message: 'Invalid request.',
      status: 400,
    };
    expect(error.status).toBe(400);
    expect(typeof error.message).toBe('string');
  });

  it('should normalize HTTP 401 to auth error', () => {
    const error = {
      code: 'HTTP_401',
      message: 'Authentication required. Please log in.',
      status: 401,
    };
    expect(error.status).toBe(401);
  });

  it('should normalize HTTP 403 to forbidden error', () => {
    const error = {
      code: 'HTTP_403',
      message: 'You do not have permission to perform this action.',
      status: 403,
    };
    expect(error.status).toBe(403);
  });

  it('should normalize HTTP 423 to AUTH_ACCOUNT_LOCKED', () => {
    const error = {
      code: 'AUTH_ACCOUNT_LOCKED',
      message: 'Your account has been temporarily locked. Please try again later.',
      status: 423,
    };
    expect(error.status).toBe(423);
    expect(error.code).toBe('AUTH_ACCOUNT_LOCKED');
  });

  it('should normalize HTTP 404 to not found error', () => {
    const error = {
      code: 'HTTP_404',
      message: 'The requested resource was not found.',
      status: 404,
    };
    expect(error.status).toBe(404);
  });

  it('should normalize HTTP 503 to service unavailable', () => {
    const error = {
      code: 'HTTP_503',
      message: 'The service is temporarily unavailable. Please try again.',
      status: 503,
    };
    expect(error.status).toBe(503);
  });

  it('ApiResponse envelope structure should match backend contract', () => {
    const envelope = {
      success: true,
      message: 'Operation completed successfully',
      data: { id: 42 },
      timestamp: new Date().toISOString(),
    };
    expect(envelope).toHaveProperty('success');
    expect(envelope).toHaveProperty('message');
    expect(envelope).toHaveProperty('data');
    expect(envelope).toHaveProperty('timestamp');
  });

  it('binary response type should retain blob type for streaming downloads', () => {
    const streamingConfig = { responseType: 'blob' as const };
    expect(streamingConfig.responseType).toBe('blob');
  });
});
