import axios, {
  AxiosError,
  AxiosInstance,
  AxiosRequestConfig,
  AxiosResponse,
  InternalAxiosRequestConfig,
} from 'axios';
import { v4 as uuidv4 } from 'uuid';
import type { ApiResponse, ApiError, PagedResponse } from './types';

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
    // Always attach a fresh correlation ID per request
    config.headers['X-Correlation-ID'] = uuidv4();

    // Attach Bearer token if stored (Phase 30.2 will populate this)
    const token = getAccessToken();
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }

    return config;
  },
  (error: unknown) => Promise.reject(error),
);

/** Response interceptor: unwrap ApiResponse<T>.data or normalize errors */
apiClient.interceptors.response.use(
  (response: AxiosResponse<ApiResponse<unknown>>): AxiosResponse => {
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
  (error: AxiosError<ApiResponse<null>>) => {
    return Promise.reject(normalizeAxiosError(error));
  },
);

// ---------------------------------------------------------------------------
// Token storage (Phase 30.2 will implement the full auth store)
// ---------------------------------------------------------------------------

export function getAccessToken(): string | null {
  return sessionStorage.getItem('sih_access_token');
}

export function setAccessToken(token: string): void {
  sessionStorage.setItem('sih_access_token', token);
}

export function clearTokens(): void {
  sessionStorage.removeItem('sih_access_token');
  sessionStorage.removeItem('sih_refresh_token');
}

// ---------------------------------------------------------------------------
// Error normalization
// ---------------------------------------------------------------------------

function normalizeApiError(
  body: ApiResponse<unknown>,
  status: number,
): ApiError {
  return {
    code: body.code ?? 'UNKNOWN_ERROR',
    message: body.message ?? 'An unexpected error occurred.',
    status,
    path: body.path,
    correlationId: body.correlationId,
  };
}

function normalizeAxiosError(error: AxiosError<ApiResponse<null>>): ApiError {
  const status = error.response?.status ?? 0;
  const body = error.response?.data;

  if (body && typeof body === 'object' && 'message' in body) {
    return normalizeApiError(body as ApiResponse<unknown>, status);
  }

  // Network error or non-JSON error body
  const fallbackMessages: Record<number, string> = {
    400: 'Invalid request. Please check your input.',
    401: 'Authentication required. Please log in.',
    403: 'You do not have permission to perform this action.',
    404: 'The requested resource was not found.',
    409: 'A conflict occurred. The resource may already exist.',
    423: 'Your account has been temporarily locked. Please try again later.',
    429: 'Too many requests. Please slow down.',
    500: 'An internal server error occurred. Please try again later.',
    503: 'The service is temporarily unavailable. Please try again.',
  };

  return {
    code: status === 423 ? 'AUTH_ACCOUNT_LOCKED' : `HTTP_${status}`,
    message:
      fallbackMessages[status] ?? error.message ?? 'An unexpected error occurred.',
    status,
  };
}

// ---------------------------------------------------------------------------
// Typed helper methods
// ---------------------------------------------------------------------------

/** Fetch a single resource. Returns the unwrapped data. */
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

/** POST a resource. Returns the unwrapped data. */
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

/** PUT a resource. Returns the unwrapped data. */
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

/** PATCH a resource. Returns the unwrapped data. */
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

/** Download a streaming/binary file (CSV or JSON export). Returns Blob. */
export async function apiDownload(
  url: string,
  config?: AxiosRequestConfig,
): Promise<Blob> {
  const response = await apiClient.get(url, {
    ...config,
    responseType: 'blob',
  });
  return response.data as Blob;
}

/** Upload multipart form data. Returns the unwrapped data. */
export async function apiUpload<TResponse>(
  url: string,
  formData: FormData,
  config?: AxiosRequestConfig,
): Promise<TResponse> {
  const response = await apiClient.post<ApiResponse<TResponse>>(url, formData, {
    ...config,
    headers: {
      // Let browser/axios set Content-Type with boundary for multipart
      ...config?.headers,
    },
  });
  return response.data.data as TResponse;
}
