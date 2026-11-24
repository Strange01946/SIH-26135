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

  describe('Persona mapping and Route Authorization (Fail-Closed RBAC)', () => {
    it('maps every authoritative backend role accurately to its distinct persona', () => {
      expect(getUserPersonas(['ROLE_super_admin'])).toEqual(['SUPER_ADMIN']);
      expect(getUserPersonas(['ROLE_government_admin'])).toEqual(['GOVERNMENT_ADMIN']);
      expect(getUserPersonas(['ROLE_government_admin'], { stateId: 10 })).toEqual(['STATE_ADMIN']);
      expect(getUserPersonas(['ROLE_state_admin'])).toEqual(['STATE_ADMIN']);
      expect(getUserPersonas(['ROLE_government_officer'])).toEqual(['GOVERNMENT_OFFICER']);
      expect(getUserPersonas(['ROLE_district_officer'])).toEqual(['DISTRICT_OFFICER']);
      expect(getUserPersonas(['ROLE_analyst'])).toEqual(['ANALYST']);
      expect(getUserPersonas(['ROLE_verifier'])).toEqual(['VERIFIER']);
      expect(getUserPersonas(['ROLE_training_provider'])).toEqual(['TRAINING_PROVIDER']);
      expect(getUserPersonas(['ROLE_tp'])).toEqual(['TRAINING_PROVIDER']);
      expect(getUserPersonas(['ROLE_assessor'])).toEqual(['ASSESSOR']);
      expect(getUserPersonas(['ROLE_employer'])).toEqual(['EMPLOYER']);
      expect(getUserPersonas(['ROLE_trainee'])).toEqual(['TRAINEE']);
      expect(getUserPersonas(['ROLE_admin'])).toEqual(['SUPER_ADMIN']);
    });

    it('fails closed when authorities array is empty or non-array', () => {
      expect(getUserPersonas([])).toEqual([]);
      expect(getUserPersonas(null as unknown as string[])).toEqual([]);
      expect(getUserPersonas(undefined as unknown as string[])).toEqual([]);
    });

    it('fails closed when only granular operational permissions are provided without ROLE_*', () => {
      // Must NOT infer ADMIN from system.manage
      expect(getUserPersonas(['system.manage'])).toEqual([]);
      // Must NOT infer TRAINING_PROVIDER from course.manage or batch.manage
      expect(getUserPersonas(['course.manage', 'batch.manage'])).toEqual([]);
      // Must NOT infer ASSESSOR from assessment.manage
      expect(getUserPersonas(['assessment.manage'])).toEqual([]);
      // Must NOT infer EMPLOYER from placement.manage
      expect(getUserPersonas(['placement.manage'])).toEqual([]);
      // Must NOT infer TRAINEE from trainee.read
      expect(getUserPersonas(['trainee.read'])).toEqual([]);
      // Combinations of permissions without role
      expect(getUserPersonas(['system.manage', 'course.manage', 'trainee.read'])).toEqual([]);
    });

    it('fails closed when only unknown or unsupported role authorities are present', () => {
      expect(getUserPersonas(['ROLE_UNKNOWN_CUSTOM_ROLE'])).toEqual([]);
      expect(getUserPersonas(['ROLE_EXTERNAL_VENDOR'])).toEqual([]);
    });

    it('supports legitimate multi-role assignments correctly', () => {
      const multi = getUserPersonas(['ROLE_super_admin', 'ROLE_assessor']);
      expect(multi).toContain('SUPER_ADMIN');
      expect(multi).toContain('ASSESSOR');
      expect(multi).toHaveLength(2);
    });

    it('prevents shared operational permissions from granting unrelated personas', () => {
      // An assessor with trainee.read and batch.manage must only get ASSESSOR persona
      const assessor = getUserPersonas(['ROLE_ASSESSOR', 'trainee.read', 'batch.manage']);
      expect(assessor).toEqual(['ASSESSOR']);

      // A provider with system.manage permission must only get TRAINING_PROVIDER persona
      const provider = getUserPersonas(['ROLE_TRAINING_PROVIDER', 'system.manage']);
      expect(provider).toEqual(['TRAINING_PROVIDER']);
    });

    it('computes default landing route following government hierarchy and fails closed for unauthorized', () => {
      expect(getDefaultLandingRoute(['ROLE_ADMIN'])).toBe('/admin/dashboard');
      expect(getDefaultLandingRoute(['ROLE_TRAINING_PROVIDER'])).toBe('/provider/dashboard');
      expect(getDefaultLandingRoute(['ROLE_ASSESSOR'])).toBe('/assessor/dashboard');
      expect(getDefaultLandingRoute(['ROLE_EMPLOYER'])).toBe('/employer/dashboard');
      expect(getDefaultLandingRoute(['ROLE_TRAINEE'])).toBe('/trainee/dashboard');
      expect(getDefaultLandingRoute([])).toBe('/forbidden');
      expect(getDefaultLandingRoute(['system.manage'])).toBe('/forbidden');
      expect(getDefaultLandingRoute(['ROLE_UNKNOWN'])).toBe('/forbidden');
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
