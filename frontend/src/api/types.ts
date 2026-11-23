/**
 * Standard API response envelope from ApiResponse<T> in the Java backend.
 * Fields path, code, correlationId are @JsonInclude(NON_NULL) — may be absent.
 */
export interface ApiResponse<T> {
  success: boolean;
  code?: string;
  message: string;
  data: T | null;
  timestamp: string; // ISO-8601 UTC
  path?: string;
  correlationId?: string;
}

/**
 * Paginated response wrapper from PagedResponse<T> in the Java backend.
 * Enveloped as ApiResponse<PagedResponse<T>>.
 */
export interface PagedResponse<T> {
  content: T[];
  page: number; // 0-based
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  hasNext: boolean;
  hasPrevious: boolean;
}

/**
 * Backend error response body matching ErrorResponse.java in Spring Boot backend.
 */
export interface BackendErrorResponse {
  status?: number;
  error?: string;
  code?: string;
  message?: string;
  path?: string;
  details?: string[];
  correlationId?: string;
  timestamp?: string;
  type?: string;
}

/**
 * Normalized API error for frontend error handling.
 */
export interface ApiError {
  code: string;
  message: string;
  status: number;
  path?: string;
  correlationId?: string;
  fieldErrors?: FieldError[];
  details?: string[];
  isNetworkError?: boolean;
  isTimeout?: boolean;
  isCanceled?: boolean;
}

export interface FieldError {
  field: string;
  message: string;
}

/**
 * Login request body matching LoginRequest.java
 */
export interface LoginRequest {
  usernameOrEmail: string;
  password: string;
}

/**
 * Refresh token request matching RefreshTokenRequest.java
 */
export interface RefreshTokenRequest {
  refreshToken: string;
}

/**
 * Auth response matching AuthResponse.java
 */
export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string; // "Bearer"
  expiresIn: number; // seconds
  userId: number;
  username: string;
  authorities: string[];
}

/**
 * Authenticated user profile matching AuthenticatedUserResponse.java from GET /api/v1/auth/me
 */
export interface AuthenticatedUserResponse {
  id: number;
  username: string;
  email?: string;
  fullName?: string;
  authorities: string[];
  organizationId?: number | null;
  departmentId?: number | null;
  stateId?: number | null;
  districtId?: number | null;
  trainingProviderId?: number | null;
  employerId?: number | null;
}

/**
 * High-level persona categories matching the 5 authorized user groups.
 */
export type UserPersona =
  | 'ADMIN'
  | 'TRAINING_PROVIDER'
  | 'ASSESSOR'
  | 'EMPLOYER'
  | 'TRAINEE';

/**
 * Helper to compute high-level personas from backend authority strings.
 * Prioritizes authoritative roles (ROLE_*) to ensure strict separation between
 * administrative actors, providers, assessors, employers, and trainees, preventing
 * shared operational read permissions (such as trainee.read or assessment.manage)
 * from bleeding cross-role navigation entries.
 */
export function getUserPersonas(authorities: string[]): UserPersona[] {
  if (!Array.isArray(authorities) || authorities.length === 0) {
    return [];
  }
  const lowerAuth = authorities.map((a) => a.toLowerCase().trim());
  const roleAuths = lowerAuth.filter((a) => a.startsWith('role_'));

  const personas: UserPersona[] = [];

  // When authoritative role assignments (ROLE_*) exist, use them exclusively
  if (roleAuths.length > 0) {
    if (
      roleAuths.some((r) =>
        [
          'role_admin',
          'role_super_admin',
          'role_government_admin',
          'role_government_officer',
          'role_district_officer',
          'role_analyst',
        ].includes(r),
      )
    ) {
      personas.push('ADMIN');
    }

    if (
      roleAuths.some((r) =>
        ['role_training_provider', 'role_tp'].includes(r),
      )
    ) {
      personas.push('TRAINING_PROVIDER');
    }

    if (
      roleAuths.some((r) =>
        ['role_assessor', 'role_verifier'].includes(r),
      )
    ) {
      personas.push('ASSESSOR');
    }

    if (
      roleAuths.some((r) =>
        ['role_employer'].includes(r),
      )
    ) {
      personas.push('EMPLOYER');
    }

    if (
      roleAuths.some((r) =>
        ['role_trainee'].includes(r),
      )
    ) {
      personas.push('TRAINEE');
    }

    return personas;
  }

  // Fallback for isolated unit tests / mocks without explicit ROLE_* prefixes
  if (lowerAuth.includes('system.manage')) {
    personas.push('ADMIN');
  }
  if (
    lowerAuth.some((a) =>
      ['course.manage', 'enrollment.manage', 'batch.manage'].includes(a),
    )
  ) {
    personas.push('TRAINING_PROVIDER');
  }
  if (
    lowerAuth.some((a) =>
      ['assessment.manage', 'verification.manage'].includes(a),
    )
  ) {
    personas.push('ASSESSOR');
  }
  if (
    lowerAuth.some((a) =>
      ['placement.manage', 'employment.manage'].includes(a),
    )
  ) {
    personas.push('EMPLOYER');
  }
  if (
    lowerAuth.includes('trainee.read') &&
    personas.length === 0
  ) {
    personas.push('TRAINEE');
  }

  return personas;
}

/**
 * Returns the default dashboard route for a specific user persona.
 */
export function getDefaultLandingRouteForPersona(persona: UserPersona): string {
  switch (persona) {
    case 'ADMIN':
      return '/admin/dashboard';
    case 'TRAINING_PROVIDER':
      return '/provider/dashboard';
    case 'ASSESSOR':
      return '/assessor/dashboard';
    case 'EMPLOYER':
      return '/employer/dashboard';
    case 'TRAINEE':
      return '/trainee/dashboard';
    default:
      return '/dashboard';
  }
}

/**
 * Returns human-readable display label for a user persona.
 */
export function formatPersonaLabel(persona: UserPersona | null | undefined): string {
  switch (persona) {
    case 'ADMIN':
      return 'Administrator';
    case 'TRAINING_PROVIDER':
      return 'Training Provider';
    case 'ASSESSOR':
      return 'Assessor';
    case 'EMPLOYER':
      return 'Employer';
    case 'TRAINEE':
      return 'Trainee';
    default:
      return 'User';
  }
}

/**
 * Returns the default dashboard route for a user based on granted authorities.
 */
export function getDefaultLandingRoute(authorities: string[]): string {
  const personas = getUserPersonas(authorities);
  if (personas.length > 0) {
    return getDefaultLandingRouteForPersona(personas[0]);
  }
  return '/dashboard';
}

/**
 * Checks whether a given route pathname is authorized for the granted authorities.
 * Used during post-login redirect to prevent routing users into 403 Forbidden pages.
 */
export function isRouteAuthorizedForAuthorities(
  pathname: string,
  authorities: string[],
): boolean {
  if (!pathname || typeof pathname !== 'string') return false;
  const cleanPath = pathname.split('?')[0].split('#')[0].toLowerCase().trim();

  // Public and general routes
  if (cleanPath === '' || cleanPath === '/' || cleanPath === '/dashboard' || cleanPath === '/login' || cleanPath === '/forbidden') {
    return true;
  }

  const personas = getUserPersonas(authorities);

  // Persona console roots
  if (cleanPath.startsWith('/admin')) {
    return personas.includes('ADMIN');
  }
  if (cleanPath.startsWith('/provider')) {
    return personas.includes('TRAINING_PROVIDER');
  }
  if (cleanPath.startsWith('/assessor')) {
    return personas.includes('ASSESSOR');
  }
  if (cleanPath.startsWith('/employer')) {
    return personas.includes('EMPLOYER');
  }
  if (cleanPath.startsWith('/trainee')) {
    return personas.includes('TRAINEE');
  }

  // Operational domain sections
  if (cleanPath.startsWith('/trainees')) {
    return personas.includes('ADMIN') || personas.includes('TRAINING_PROVIDER') || personas.includes('ASSESSOR');
  }
  if (cleanPath.startsWith('/batches')) {
    return personas.includes('ADMIN') || personas.includes('TRAINING_PROVIDER');
  }
  if (cleanPath.startsWith('/assessments')) {
    return personas.includes('ADMIN') || personas.includes('ASSESSOR');
  }
  if (cleanPath.startsWith('/placements')) {
    return personas.includes('ADMIN') || personas.includes('EMPLOYER') || personas.includes('TRAINING_PROVIDER');
  }
  if (cleanPath.startsWith('/employment')) {
    return personas.includes('ADMIN') || personas.includes('EMPLOYER');
  }
  if (
    cleanPath.startsWith('/analytics') ||
    cleanPath.startsWith('/data-quality') ||
    cleanPath.startsWith('/audit') ||
    cleanPath.startsWith('/users')
  ) {
    return personas.includes('ADMIN');
  }
  if (cleanPath.startsWith('/import')) {
    return personas.includes('ADMIN') || personas.includes('TRAINING_PROVIDER');
  }

  return false;
}

/**
 * RBAC roles as defined in the backend.
 */
export type UserRole =
  | 'ROLE_ADMIN'
  | 'ROLE_super_admin'
  | 'ROLE_government_admin'
  | 'ROLE_TRAINING_PROVIDER'
  | 'ROLE_training_provider'
  | 'ROLE_ASSESSOR'
  | 'ROLE_verifier'
  | 'ROLE_EMPLOYER'
  | 'ROLE_employer'
  | 'ROLE_TRAINEE'
  | 'ROLE_trainee';

/**
 * Error codes returned by the backend GlobalExceptionHandler.
 */
export type ApiErrorCode =
  | 'ARGUMENT_INVALID'
  | 'VALIDATION_ERROR'
  | 'INVALID_CREDENTIALS'
  | 'RESOURCE_NOT_FOUND'
  | 'AUTH_ACCOUNT_LOCKED'
  | 'ACCOUNT_LOCKED'
  | 'MALFORMED_INPUT'
  | 'CONSENT_WITHDRAWN'
  | 'CONSENT_NOT_GRANTED'
  | 'OPERATION_SUCCESS'
  | string;
