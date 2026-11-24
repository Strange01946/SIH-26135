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
 * Authoritative persona types representing the backend's complete role model.
 * Differentiates Super Admins, National Government Admins, State Admins,
 * Government Officers, District Officers, Analysts, Verifiers, Providers, Assessors, Employers, and Trainees.
 */
export type UserPersona =
  | 'SUPER_ADMIN'
  | 'GOVERNMENT_ADMIN'
  | 'STATE_ADMIN'
  | 'GOVERNMENT_OFFICER'
  | 'DISTRICT_OFFICER'
  | 'ANALYST'
  | 'VERIFIER'
  | 'TRAINING_PROVIDER'
  | 'ASSESSOR'
  | 'EMPLOYER'
  | 'TRAINEE'
  | 'ADMIN';

/**
 * Checks whether a given persona belongs to an administrative or government oversight category.
 */
export function isAdministrativePersona(persona: UserPersona | null | undefined): boolean {
  if (!persona) return false;
  return [
    'SUPER_ADMIN',
    'GOVERNMENT_ADMIN',
    'STATE_ADMIN',
    'GOVERNMENT_OFFICER',
    'DISTRICT_OFFICER',
    'ANALYST',
    'ADMIN',
  ].includes(persona);
}

/**
 * Helper to compute high-level personas from backend authority strings and user tenancy profile.
 * Prioritizes authoritative roles (ROLE_*) and preserves distinct operational identities between
 * Super Admins, National Government Admins, State Admins, Government Officers, District Officers,
 * Analysts, Verifiers, Providers, Assessors, Employers, and Trainees.
 */
export function getUserPersonas(
  authorities: string[],
  userProfile?: { stateId?: number | null; districtId?: number | null } | null,
): UserPersona[] {
  if (!Array.isArray(authorities) || authorities.length === 0) {
    return [];
  }
  const lowerAuth = authorities.map((a) => a.toLowerCase().trim());
  const roleAuths = lowerAuth.filter((a) => a.startsWith('role_'));

  if (roleAuths.length === 0) {
    return [];
  }

  const personas: UserPersona[] = [];

  // Super Administrator
  if (roleAuths.some((r) => ['role_super_admin'].includes(r))) {
    personas.push('SUPER_ADMIN');
  }

  // Government Administrator / State Administrator
  if (roleAuths.some((r) => ['role_government_admin', 'role_state_admin'].includes(r))) {
    if (userProfile?.stateId != null || roleAuths.includes('role_state_admin')) {
      personas.push('STATE_ADMIN');
    } else {
      personas.push('GOVERNMENT_ADMIN');
    }
  }

  // Government Officer (Operational)
  if (roleAuths.some((r) => ['role_government_officer'].includes(r))) {
    personas.push('GOVERNMENT_OFFICER');
  }

  // District Officer (District-scoped)
  if (roleAuths.some((r) => ['role_district_officer'].includes(r))) {
    personas.push('DISTRICT_OFFICER');
  }

  // Data & Policy Analyst
  if (roleAuths.some((r) => ['role_analyst'].includes(r))) {
    personas.push('ANALYST');
  }

  // Verification Agency
  if (roleAuths.some((r) => ['role_verifier'].includes(r))) {
    personas.push('VERIFIER');
  }

  // Generic or legacy role_admin (maps to SUPER_ADMIN if no specific admin persona was derived)
  if (
    roleAuths.some((r) => ['role_admin'].includes(r)) &&
    !personas.some(isAdministrativePersona)
  ) {
    personas.push('SUPER_ADMIN');
  }

  // Training Provider
  if (
    roleAuths.some((r) =>
      ['role_training_provider', 'role_tp'].includes(r),
    )
  ) {
    personas.push('TRAINING_PROVIDER');
  }

  // Assessor
  if (
    roleAuths.some((r) =>
      ['role_assessor'].includes(r),
    )
  ) {
    personas.push('ASSESSOR');
  }

  // Employer
  if (
    roleAuths.some((r) =>
      ['role_employer'].includes(r),
    )
  ) {
    personas.push('EMPLOYER');
  }

  // Trainee
  if (
    roleAuths.some((r) =>
      ['role_trainee'].includes(r),
    )
  ) {
    personas.push('TRAINEE');
  }

  // Fail closed: without authoritative ROLE_* assignments, granular permissions
  // (e.g. system.manage, trainee.read) MUST NOT infer any portal persona.
  return personas;
}

/**
 * Returns the default dashboard route for a specific user persona.
 */
export function getDefaultLandingRouteForPersona(persona: UserPersona): string {
  switch (persona) {
    case 'SUPER_ADMIN':
    case 'GOVERNMENT_ADMIN':
    case 'STATE_ADMIN':
    case 'GOVERNMENT_OFFICER':
    case 'DISTRICT_OFFICER':
    case 'ANALYST':
    case 'ADMIN':
      return '/admin/dashboard';
    case 'TRAINING_PROVIDER':
      return '/provider/dashboard';
    case 'ASSESSOR':
      return '/assessor/dashboard';
    case 'EMPLOYER':
    case 'VERIFIER':
      return '/employer/dashboard';
    case 'TRAINEE':
      return '/trainee/dashboard';
    default:
      return '/forbidden';
  }
}

/**
 * Returns human-readable display label for a user persona.
 */
export function formatPersonaLabel(persona: UserPersona | null | undefined): string {
  switch (persona) {
    case 'SUPER_ADMIN':
      return 'Super Administrator';
    case 'GOVERNMENT_ADMIN':
      return 'Government Administrator';
    case 'STATE_ADMIN':
      return 'State Administrator';
    case 'GOVERNMENT_OFFICER':
      return 'Government Officer';
    case 'DISTRICT_OFFICER':
      return 'District Officer';
    case 'ANALYST':
      return 'Data & Policy Analyst';
    case 'VERIFIER':
      return 'Verification Agency';
    case 'TRAINING_PROVIDER':
      return 'Training Provider';
    case 'ASSESSOR':
      return 'Assessor';
    case 'EMPLOYER':
      return 'Employer';
    case 'TRAINEE':
      return 'Trainee';
    case 'ADMIN':
      return 'Administrator';
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
  return '/forbidden';
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
  const hasAdmin = personas.some(isAdministrativePersona);

  // Persona console roots
  if (cleanPath.startsWith('/admin')) {
    return hasAdmin;
  }
  if (cleanPath.startsWith('/provider')) {
    return personas.includes('TRAINING_PROVIDER');
  }
  if (cleanPath.startsWith('/assessor')) {
    return personas.includes('ASSESSOR');
  }
  if (cleanPath === '/communication' || cleanPath === '/communication/') {
    const permissions = authorities.map((authority) => authority.trim().toLowerCase());
    const communicationStaff = ['communication.manage', 'survey.manage', 'system.manage']
      .some((permission) => permissions.includes(permission));
    return ((personas.includes('SUPER_ADMIN') || personas.includes('GOVERNMENT_ADMIN')) && communicationStaff)
      || (personas.includes('ASSESSOR') && permissions.includes('assessment.manage'));
  }
  if (cleanPath === '/audit' || cleanPath === '/audit/') {
    const governanceAuthorities = new Set(['audit.read', 'system.manage', 'system.event.read', 'system.event.manage', 'role_admin', 'role_super_admin']);
    return authorities.some((authority) => governanceAuthorities.has(authority.trim().toLowerCase()));
  }
  if (cleanPath.startsWith('/employer')) {
    return personas.includes('EMPLOYER') || personas.includes('VERIFIER');
  }
  if (cleanPath.startsWith('/trainee')) {
    return personas.includes('TRAINEE');
  }

  // Operational domain sections
  if (cleanPath.startsWith('/trainees')) {
    return hasAdmin || personas.includes('TRAINING_PROVIDER') || personas.includes('ASSESSOR');
  }
  if (cleanPath.startsWith('/batches')) {
    return hasAdmin || personas.includes('TRAINING_PROVIDER');
  }
  if (cleanPath.startsWith('/assessments')) {
    return hasAdmin || personas.includes('ASSESSOR');
  }
  if (cleanPath.startsWith('/placements')) {
    return hasAdmin || personas.includes('EMPLOYER') || personas.includes('TRAINING_PROVIDER');
  }
  if (cleanPath.startsWith('/employment')) {
    return hasAdmin || personas.includes('EMPLOYER') || personas.includes('VERIFIER');
  }
  if (cleanPath.startsWith('/data-quality')) {
    return hasAdmin || personas.includes('TRAINING_PROVIDER');
  }
  if (
    cleanPath.startsWith('/analytics') ||
    cleanPath.startsWith('/users')
  ) {
    return hasAdmin;
  }
  if (cleanPath.startsWith('/import')) {
    return hasAdmin || personas.includes('TRAINING_PROVIDER');
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
