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
 * Normalized API error for frontend error handling.
 */
export interface ApiError {
  code: string;
  message: string;
  status: number;
  path?: string;
  correlationId?: string;
  fieldErrors?: FieldError[];
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
  expiresIn: number; // milliseconds
  userId: number;
  username: string;
  authorities: string[];
}

/**
 * RBAC roles as defined in the backend.
 */
export type UserRole =
  | 'ROLE_ADMIN'
  | 'ROLE_TRAINING_PROVIDER'
  | 'ROLE_ASSESSOR'
  | 'ROLE_EMPLOYER'
  | 'ROLE_TRAINEE';

/**
 * Error codes returned by the backend GlobalExceptionHandler.
 */
export type ApiErrorCode =
  | 'ARGUMENT_INVALID'
  | 'RESOURCE_NOT_FOUND'
  | 'AUTH_ACCOUNT_LOCKED'
  | 'MALFORMED_INPUT'
  | 'CONSENT_WITHDRAWN'
  | 'CONSENT_NOT_GRANTED'
  | 'OPERATION_SUCCESS'
  | string; // allow forward compatibility
