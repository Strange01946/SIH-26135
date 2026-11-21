import axios, {
  AxiosError,
  AxiosInstance,
  AxiosRequestConfig,
  AxiosResponse,
  InternalAxiosRequestConfig,
} from 'axios';
import { v4 as uuidv4 } from 'uuid';
import {
  getAccessToken as getSessionAccessToken,
  setTokens,
  clearSession,
  refreshAccessToken,
  hasSession,
  getSessionGeneration,
} from '@/lib/sessionManager';
import type {
  ApiResponse,
  ApiError,
  PagedResponse,
  FieldError,
  BackendErrorResponse,
} from './types';

/** Base URL — empty means Vite dev proxy routes /api to localhost:8080 */
const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '';

/** Axios instance for JSON API calls */
export const apiClient: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  headers: {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  },
  timeout: 30_000,
});

/** Request interceptor: attach X-Correlation-ID and Authorization */
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig): InternalAxiosRequestConfig => {
    // Attach fresh correlation ID per request if not explicitly provided
    if (!config.headers['X-Correlation-ID']) {
      config.headers['X-Correlation-ID'] = uuidv4();
    }

    // For FormData uploads, remove default application/json so browser generates multipart boundary
    if (config.data instanceof FormData) {
      delete config.headers['Content-Type'];
    }

    // Attach Bearer token from session manager if present
    const token = getSessionAccessToken();
    if (token && !config.headers['Authorization']) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }

    // Record session generation at dispatch time to prevent cross-session replay or stale invalidation
    const requestWithGen = config as InternalAxiosRequestConfig & { _sessionGeneration?: number };
    if (requestWithGen._sessionGeneration === undefined) {
      requestWithGen._sessionGeneration = getSessionGeneration();
    }

    return config;
  },
  (error: unknown) => Promise.reject(error),
);

/** Response interceptor: unwrap ApiResponse<T>.data, normalize errors, and handle 401 token refresh */
apiClient.interceptors.response.use(
  (response: AxiosResponse): AxiosResponse => {
    // For streaming/binary responses, pass through unchanged
    if (
      response.config.responseType === 'blob' ||
      response.config.responseType === 'arraybuffer'
    ) {
      return response;
    }

    const body = response.data as ApiResponse<unknown>;

    // Backend returned success:false in a 2xx (defensive check)
    if (body && typeof body === 'object' && 'success' in body && !body.success) {
      return Promise.reject(
        normalizeApiError(body, response.status),
      ) as never;
    }

    return response;
  },
  async (error: unknown) => {
    const axiosError = error as AxiosError<unknown>;
    const originalRequest = axiosError.config as
      | (InternalAxiosRequestConfig & { _retry?: boolean })
      | undefined;

    // If the error response was received as a Blob (e.g. from failed apiDownload), parse JSON text
    if (
      axiosError.response?.data instanceof Blob &&
      (axiosError.response.data.type?.includes('application/json') ||
        axiosError.response.status >= 400)
    ) {
      try {
        const text = await axiosError.response.data.text();
        axiosError.response.data = JSON.parse(text);
      } catch {
        // Fall back if blob text cannot be parsed as JSON
      }
    }

    // 401 Unauthorized handling with single-flight silent refresh
    if (
      axiosError.response?.status === 401 &&
      originalRequest &&
      !originalRequest._retry
    ) {
      const url = originalRequest.url ?? '';
      const isAuthEndpoint =
        url.includes('/api/v1/auth/login') ||
        url.includes('/api/v1/auth/refresh');

      const currentGeneration = getSessionGeneration();
      const requestGeneration = (originalRequest as InternalAxiosRequestConfig & { _sessionGeneration?: number })
        ?._sessionGeneration;

      // Invariant: If request originated from an older session generation, do NOT replay or refresh.
      // An outdated request from a previous session must NEVER be replayed with a newer user's token,
      // and must NEVER trigger session clearing on a newer session.
      if (requestGeneration !== undefined && requestGeneration !== currentGeneration) {
        return Promise.reject(normalizeAxiosError(axiosError));
      }

      // Do not attempt refresh for auth endpoints themselves to prevent loops
      if (!isAuthEndpoint && hasSession()) {
        originalRequest._retry = true;

        const method = (originalRequest.method ?? 'get').toLowerCase();
        const isSafeMethod = ['get', 'head', 'options'].includes(method);
        const isExplicitlyIdempotent = originalRequest.headers?.['X-Idempotent'] === 'true';

        // Restrict automatic replay to demonstrably safe (read) requests or requests
        // with explicit idempotency support to prevent duplicate business operations
        if (isSafeMethod || isExplicitlyIdempotent) {
          try {
            const newAccessToken = await refreshAccessToken();

            // Verify session was not logged out or replaced while refresh was in-flight
            if (getSessionGeneration() !== currentGeneration) {
              return Promise.reject(normalizeAxiosError(axiosError));
            }

            originalRequest.headers['Authorization'] = `Bearer ${newAccessToken}`;
            return apiClient(originalRequest);
          } catch {
            // Only clear session if session has not already been superseded by another login/logout
            if (getSessionGeneration() === currentGeneration) {
              clearSession();
            }
            return Promise.reject(normalizeAxiosError(axiosError));
          }
        } else {
          // For non-idempotent mutation requests (POST, PATCH, PUT, DELETE):
          // Do NOT automatically replay to prevent duplicate transactions or state corruptions.
          // Trigger background refresh so user's active session is restored for subsequent actions.
          refreshAccessToken().catch(() => {
            if (getSessionGeneration() === currentGeneration) {
              clearSession();
            }
          });
          return Promise.reject(normalizeAxiosError(axiosError));
        }
      }
    }

    return Promise.reject(normalizeAxiosError(axiosError));
  },
);

// ---------------------------------------------------------------------------
// Token storage compatibility helpers
// ---------------------------------------------------------------------------

export function getAccessToken(): string | null {
  return getSessionAccessToken();
}

export function setAccessToken(token: string): void {
  setTokens(token);
}

export function clearTokens(): void {
  clearSession();
}

// ---------------------------------------------------------------------------
// Error normalization
// ---------------------------------------------------------------------------

export function normalizeApiError(
  body: ApiResponse<unknown> | BackendErrorResponse,
  status: number,
): ApiError {
  const code = body.code ?? (status === 423 ? 'AUTH_ACCOUNT_LOCKED' : `HTTP_${status}`);
  const message = body.message ?? getDefaultStatusMessage(status);
  const path = body.path;
  const correlationId = body.correlationId;

  // Parse field-level validation errors from Spring Boot ErrorResponse details
  const fieldErrors: FieldError[] = [];
  const details = (body as BackendErrorResponse).details;

  if (Array.isArray(details)) {
    for (const item of details) {
      if (typeof item === 'string') {
        const colonIndex = item.indexOf(':');
        if (colonIndex > 0) {
          fieldErrors.push({
            field: item.substring(0, colonIndex).trim(),
            message: item.substring(colonIndex + 1).trim(),
          });
        }
      }
    }
  }

  return {
    code,
    message,
    status,
    path,
    correlationId,
    details: Array.isArray(details) ? details : undefined,
    fieldErrors: fieldErrors.length > 0 ? fieldErrors : undefined,
  };
}

export function normalizeAxiosError(error: AxiosError<unknown>): ApiError {
  // Handle request cancellation
  if (axios.isCancel(error)) {
    return {
      code: 'REQUEST_CANCELED',
      message: 'Request was canceled.',
      status: 0,
      isCanceled: true,
    };
  }

  // Handle timeout errors
  if (error.code === 'ECONNABORTED' || error.message?.toLowerCase().includes('timeout')) {
    return {
      code: 'REQUEST_TIMEOUT',
      message: 'The request timed out. Please try again.',
      status: 408,
      isTimeout: true,
    };
  }

  // Handle network failure (no response received from server or Network Error)
  if (
    error.code === 'ERR_NETWORK' ||
    error.message === 'Network Error' ||
    !error.response
  ) {
    return {
      code: 'NETWORK_ERROR',
      message: 'Unable to connect to server. Please check your network connection.',
      status: 0,
      isNetworkError: true,
    };
  }

  const status = error.response?.status ?? 0;
  const body = error.response?.data;

  if (body && typeof body === 'object') {
    const errorBody = body as BackendErrorResponse;
    if (errorBody.message || errorBody.code || errorBody.details) {
      return normalizeApiError(errorBody, status);
    }
  }

  return {
    code: status === 423 ? 'AUTH_ACCOUNT_LOCKED' : `HTTP_${status}`,
    message: getDefaultStatusMessage(status) ?? error.message ?? 'An unexpected error occurred.',
    status,
  };
}

function getDefaultStatusMessage(status: number): string {
  const fallbackMessages: Record<number, string> = {
    400: 'Invalid request. Please check your input.',
    401: 'Authentication required. Please log in.',
    403: 'You do not have permission to perform this action.',
    404: 'The requested resource was not found.',
    409: 'A conflict occurred. The resource may already exist.',
    423: 'Your account has been temporarily locked due to consecutive failed login attempts. Please contact your system administrator or try again later.',
    429: 'Too many requests. Please slow down.',
    500: 'An internal server error occurred. Please try again later.',
    503: 'The service is temporarily unavailable. Please try again.',
  };
  return fallbackMessages[status] ?? 'An unexpected error occurred.';
}

// ---------------------------------------------------------------------------
// Typed helper methods — Unwrapped data (convenient for components)
// ---------------------------------------------------------------------------

/** Fetch a single resource. Returns the unwrapped data payload. */
export async function apiGet<T>(
  url: string,
  config?: AxiosRequestConfig,
): Promise<T> {
  const response = await apiClient.get<ApiResponse<T>>(url, config);
  return response.data.data as T;
}

/** Fetch a paginated list. Returns PagedResponse<T>. */
export async function apiGetPaged<T>(
  url: string,
  config?: AxiosRequestConfig,
): Promise<PagedResponse<T>> {
  const response = await apiClient.get<ApiResponse<PagedResponse<T>>>(
    url,
    config,
  );
  return response.data.data as PagedResponse<T>;
}

/** POST a resource. Returns the unwrapped data payload. */
export async function apiPost<TBody, TResponse>(
  url: string,
  body: TBody,
  config?: AxiosRequestConfig,
): Promise<TResponse> {
  const response = await apiClient.post<ApiResponse<TResponse>>(
    url,
    body,
    config,
  );
  return response.data.data as TResponse;
}

/** PUT a resource. Returns the unwrapped data payload. */
export async function apiPut<TBody, TResponse>(
  url: string,
  body: TBody,
  config?: AxiosRequestConfig,
): Promise<TResponse> {
  const response = await apiClient.put<ApiResponse<TResponse>>(
    url,
    body,
    config,
  );
  return response.data.data as TResponse;
}

/** PATCH a resource. Returns the unwrapped data payload. */
export async function apiPatch<TBody, TResponse>(
  url: string,
  body: TBody,
  config?: AxiosRequestConfig,
): Promise<TResponse> {
  const response = await apiClient.patch<ApiResponse<TResponse>>(
    url,
    body,
    config,
  );
  return response.data.data as TResponse;
}

/** DELETE a resource. Returns void (204 or empty 200). */
export async function apiDelete(
  url: string,
  config?: AxiosRequestConfig,
): Promise<void> {
  await apiClient.delete(url, config);
}

/**
 * Download a file as a binary Blob (CSV or JSON export).
 * 
 * NOTE: In-browser Blob downloads buffer the entire response payload in browser
 * memory before returning the Blob. For huge multi-gigabyte datasets, true
 * progressive streaming directly to disk requires the File System Access API
 * or direct stream consumption, which is planned for the dedicated export integration phase.
 */
export async function apiDownload(
  url: string,
  config?: AxiosRequestConfig,
): Promise<Blob> {
  const response = await apiClient.get<Blob>(url, {
    ...config,
    responseType: 'blob',
  });
  return response.data;
}

/**
 * Upload multipart form data. Returns the unwrapped data payload.
 * Automatically ensures Content-Type is omitted so the browser sets the boundary.
 */
export async function apiUpload<TResponse>(
  url: string,
  formData: FormData,
  config?: AxiosRequestConfig,
): Promise<TResponse> {
  const headers = { ...config?.headers };
  delete (headers as Record<string, unknown>)['Content-Type'];

  const response = await apiClient.post<ApiResponse<TResponse>>(url, formData, {
    ...config,
    headers: {
      ...headers,
      'Content-Type': undefined,
    },
  });
  return response.data.data as TResponse;
}

// ---------------------------------------------------------------------------
// Typed helper methods — Full envelope (preserves metadata, codes, correlationId)
// ---------------------------------------------------------------------------

/** Fetch a single resource and return the complete ApiResponse<T> envelope. */
export async function apiGetEnvelope<T>(
  url: string,
  config?: AxiosRequestConfig,
): Promise<ApiResponse<T>> {
  const response = await apiClient.get<ApiResponse<T>>(url, config);
  return response.data;
}

/** Fetch a paginated list and return the complete ApiResponse<PagedResponse<T>> envelope. */
export async function apiGetPagedEnvelope<T>(
  url: string,
  config?: AxiosRequestConfig,
): Promise<ApiResponse<PagedResponse<T>>> {
  const response = await apiClient.get<ApiResponse<PagedResponse<T>>>(
    url,
    config,
  );
  return response.data;
}

/** POST a resource and return the complete ApiResponse<TResponse> envelope. */
export async function apiPostEnvelope<TBody, TResponse>(
  url: string,
  body: TBody,
  config?: AxiosRequestConfig,
): Promise<ApiResponse<TResponse>> {
  const response = await apiClient.post<ApiResponse<TResponse>>(
    url,
    body,
    config,
  );
  return response.data;
}

/** PUT a resource and return the complete ApiResponse<TResponse> envelope. */
export async function apiPutEnvelope<TBody, TResponse>(
  url: string,
  body: TBody,
  config?: AxiosRequestConfig,
): Promise<ApiResponse<TResponse>> {
  const response = await apiClient.put<ApiResponse<TResponse>>(
    url,
    body,
    config,
  );
  return response.data;
}

/** PATCH a resource and return the complete ApiResponse<TResponse> envelope. */
export async function apiPatchEnvelope<TBody, TResponse>(
  url: string,
  body: TBody,
  config?: AxiosRequestConfig,
): Promise<ApiResponse<TResponse>> {
  const response = await apiClient.patch<ApiResponse<TResponse>>(
    url,
    body,
    config,
  );
  return response.data;
}

/** Upload multipart form data and return the complete ApiResponse<TResponse> envelope. */
export async function apiUploadEnvelope<TResponse>(
  url: string,
  formData: FormData,
  config?: AxiosRequestConfig,
): Promise<ApiResponse<TResponse>> {
  const headers = { ...config?.headers };
  delete (headers as Record<string, unknown>)['Content-Type'];

  const response = await apiClient.post<ApiResponse<TResponse>>(url, formData, {
    ...config,
    headers: {
      ...headers,
      'Content-Type': undefined,
    },
  });
  return response.data;
}
