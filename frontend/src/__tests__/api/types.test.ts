import { describe, it, expect } from 'vitest';
import type {
  ApiResponse,
  PagedResponse,
  ApiError,
  AuthResponse,
  BackendErrorResponse,
} from '@/api/types';
import {
  getUserPersonas,
  getDefaultLandingRoute,
  isRouteAuthorizedForAuthorities,
} from '@/api/types';

describe('ApiResponse type contract', () => {
  it('should accept a valid success ApiResponse', () => {
    const response: ApiResponse<{ id: number }> = {
      success: true,
      message: 'Operation completed successfully',
      data: { id: 1 },
      timestamp: '2026-11-20T10:15:30.123Z',
    };
    expect(response.success).toBe(true);
    expect(response.data).toEqual({ id: 1 });
    expect(response.code).toBeUndefined();
    expect(response.path).toBeUndefined();
    expect(response.correlationId).toBeUndefined();
  });

  it('should accept an error ApiResponse with code and path', () => {
    const response: ApiResponse<null> = {
      success: false,
      code: 'RESOURCE_NOT_FOUND',
      message: 'Trainee not found.',
      data: null,
      timestamp: '2026-11-20T10:15:30.123Z',
      path: '/api/v1/trainees/999',
      correlationId: 'c8f9b2a1-0000-4000-a000-000000000001',
    };
    expect(response.success).toBe(false);
    expect(response.code).toBe('RESOURCE_NOT_FOUND');
    expect(response.path).toBeDefined();
  });

  it('should accept a valid PagedResponse', () => {
    const paged: PagedResponse<{ id: number }> = {
      content: [{ id: 1 }],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
      first: true,
      last: true,
      hasNext: false,
      hasPrevious: false,
    };
    expect(paged.content).toHaveLength(1);
    expect(paged.page).toBe(0);
    expect(paged.hasNext).toBe(false);
  });

  it('should accept a valid AuthResponse', () => {
    const auth: AuthResponse = {
      accessToken: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.sig',
      refreshToken: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.refresh.sig',
      tokenType: 'Bearer',
      expiresIn: 900000,
      userId: 1,
      username: 'admin',
      authorities: ['ROLE_ADMIN'],
    };
    expect(auth.tokenType).toBe('Bearer');
    expect(auth.authorities).toContain('ROLE_ADMIN');
  });

  it('ApiError should support validation fieldErrors and operational metadata', () => {
    const error: ApiError = {
      code: 'VALIDATION_ERROR',
      message: 'Request validation failed',
      status: 400,
      path: '/api/v1/trainees',
      correlationId: 'c8f9b2a1-0000-4000-a000-000000000001',
      details: ['aadhaarNumber: Aadhaar is invalid'],
      fieldErrors: [{ field: 'aadhaarNumber', message: 'Aadhaar is invalid' }],
    };
    expect(error.status).toBe(400);
    expect(error.code).toBe('VALIDATION_ERROR');
    expect(error.fieldErrors).toHaveLength(1);
    expect(error.fieldErrors?.[0].field).toBe('aadhaarNumber');
  });

  it('BackendErrorResponse should match Spring Boot ErrorResponse.java structure', () => {
    const backendError: BackendErrorResponse = {
      status: 423,
      error: 'Locked',
      code: 'AUTH_ACCOUNT_LOCKED',
      message: 'Account locked due to failed attempts',
      path: '/api/v1/auth/login',
      correlationId: 'c8f9b2a1-0000-4000-a000-000000000001',
      timestamp: '2026-11-21T18:00:00',
    };
    expect(backendError.status).toBe(423);
    expect(backendError.code).toBe('AUTH_ACCOUNT_LOCKED');
  });

  describe('Persona mapping and Route Authorization', () => {
    it('maps all five personas accurately from role and authority strings', () => {
      expect(getUserPersonas(['ROLE_ADMIN'])).toContain('ADMIN');
      expect(getUserPersonas(['system.manage'])).toContain('ADMIN');
      expect(getUserPersonas(['ROLE_TRAINING_PROVIDER'])).toContain('TRAINING_PROVIDER');
      expect(getUserPersonas(['course.manage'])).toContain('TRAINING_PROVIDER');
      expect(getUserPersonas(['ROLE_ASSESSOR'])).toContain('ASSESSOR');
      expect(getUserPersonas(['ROLE_EMPLOYER'])).toContain('EMPLOYER');
      expect(getUserPersonas(['ROLE_TRAINEE'])).toContain('TRAINEE');
      expect(getUserPersonas(['trainee.read'])).toContain('TRAINEE');
    });

    it('computes default landing route following government hierarchy', () => {
      expect(getDefaultLandingRoute(['ROLE_ADMIN'])).toBe('/admin/dashboard');
      expect(getDefaultLandingRoute(['ROLE_TRAINING_PROVIDER'])).toBe('/provider/dashboard');
      expect(getDefaultLandingRoute(['ROLE_ASSESSOR'])).toBe('/assessor/dashboard');
      expect(getDefaultLandingRoute(['ROLE_EMPLOYER'])).toBe('/employer/dashboard');
      expect(getDefaultLandingRoute(['ROLE_TRAINEE'])).toBe('/trainee/dashboard');
      expect(getDefaultLandingRoute([])).toBe('/dashboard');
    });

    it('validates whether a route is authorized for given authorities', () => {
      // General routes
      expect(isRouteAuthorizedForAuthorities('/dashboard', [])).toBe(true);
      expect(isRouteAuthorizedForAuthorities('/login', [])).toBe(true);

      // Admin routes
      expect(isRouteAuthorizedForAuthorities('/admin/dashboard', ['ROLE_ADMIN'])).toBe(true);
      expect(isRouteAuthorizedForAuthorities('/admin/dashboard', ['ROLE_TRAINEE'])).toBe(false);

      // Provider routes
      expect(isRouteAuthorizedForAuthorities('/provider/dashboard', ['ROLE_TRAINING_PROVIDER'])).toBe(true);
      expect(isRouteAuthorizedForAuthorities('/provider/dashboard', ['ROLE_EMPLOYER'])).toBe(false);

      // Trainee routes
      expect(isRouteAuthorizedForAuthorities('/trainee/dashboard', ['ROLE_TRAINEE'])).toBe(true);
      expect(isRouteAuthorizedForAuthorities('/trainee/dashboard', ['ROLE_ADMIN'])).toBe(false);

      // Domain routes
      expect(isRouteAuthorizedForAuthorities('/batches', ['ROLE_TRAINING_PROVIDER'])).toBe(true);
      expect(isRouteAuthorizedForAuthorities('/batches', ['ROLE_TRAINEE'])).toBe(false);
      expect(isRouteAuthorizedForAuthorities('/analytics', ['ROLE_ADMIN'])).toBe(true);
      expect(isRouteAuthorizedForAuthorities('/analytics', ['ROLE_TRAINING_PROVIDER'])).toBe(false);
    });
  });
});
