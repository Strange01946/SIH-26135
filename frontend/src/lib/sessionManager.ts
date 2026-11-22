import axios from 'axios';
import { queryClient } from './queryClient';
import type { ApiResponse, AuthResponse } from '@/api/types';

const REFRESH_TOKEN_STORAGE_KEY = 'sih_refresh_token';

// In-memory access token storage (never exposed to window or localStorage)
let memoryAccessToken: string | null = null;

// Monotonically increasing session generation counter to invalidate in-flight async operations
let sessionGeneration = 0;

/**
 * Returns the current session generation counter.
 * Incremented whenever a session is created, replaced, or terminated.
 */
export function getSessionGeneration(): number {
  return sessionGeneration;
}

// Single-flight refresh token promise to coalesce concurrent 401s
let inFlightRefreshPromise: Promise<string | null> | null = null;

// Subscribers for session lifecycle events (e.g., AuthContext)
type SessionListener = (isAuthenticated: boolean) => void;
const listeners = new Set<SessionListener>();

/**
 * Retrieves the current in-memory JWT access token.
 */
export function getAccessToken(): string | null {
  return memoryAccessToken;
}

/**
 * Sets the current in-memory access token.
 */
export function setAccessToken(token: string | null): void {
  memoryAccessToken = token;
}

/**
 * Retrieves the stored refresh token from sessionStorage.
 */
export function getRefreshToken(): string | null {
  try {
    return sessionStorage.getItem(REFRESH_TOKEN_STORAGE_KEY);
  } catch {
    return null;
  }
}

/**
 * Stores the refresh token in sessionStorage.
 */
export function setRefreshToken(token: string | null): void {
  try {
    if (token) {
      sessionStorage.setItem(REFRESH_TOKEN_STORAGE_KEY, token);
    } else {
      sessionStorage.removeItem(REFRESH_TOKEN_STORAGE_KEY);
    }
  } catch {
    // Gracefully handle environments with restricted storage access
  }
}

/**
 * Checks whether a refresh token is present in storage.
 */
export function hasSession(): boolean {
  return Boolean(getRefreshToken());
}

/**
 * Atomically stores newly received access and refresh tokens.
 */
export function setTokens(accessToken: string, refreshToken?: string): void {
  sessionGeneration++;
  memoryAccessToken = accessToken;
  if (refreshToken) {
    setRefreshToken(refreshToken);
  }
  notifyListeners(true);
}

/**
 * Fully terminates the current session:
 * 1. Increments session generation to invalidate any in-flight requests.
 * 2. Clears in-memory access token.
 * 3. Clears sessionStorage refresh token.
 * 4. Clears TanStack Query cache to prevent data leakage across users.
 * 5. Notifies listeners of unauthenticated state.
 */
export function clearSession(): void {
  sessionGeneration++;
  memoryAccessToken = null;
  setRefreshToken(null);
  inFlightRefreshPromise = null;

  // Clear query client cache to ensure no previous-user data persists
  try {
    queryClient.clear();
  } catch {
    // Ignore in non-browser or test setups
  }

  notifyListeners(false);
}

/**
 * Executes token refresh using POST /api/v1/auth/refresh.
 * Coalesces concurrent calls so only one HTTP refresh request is dispatched.
 * Protected against race conditions via session generation validation.
 */
export async function refreshAccessToken(): Promise<string> {
  // If a refresh is already in-flight, return the existing promise
  if (inFlightRefreshPromise) {
    const token = await inFlightRefreshPromise;
    if (!token) {
      throw new Error('Token refresh failed');
    }
    return token;
  }

  const existingRefreshToken = getRefreshToken();
  if (!existingRefreshToken) {
    clearSession();
    throw new Error('No refresh token available');
  }

  const requestGeneration = sessionGeneration;

  // Create single-flight promise
  inFlightRefreshPromise = (async () => {
    try {
      const baseUrl = import.meta.env.VITE_API_BASE_URL ?? '';
      // Use direct axios call to bypass apiClient interceptors (prevents recursion)
      const response = await axios.post<ApiResponse<AuthResponse>>(
        `${baseUrl}/api/v1/auth/refresh`,
        { refreshToken: existingRefreshToken },
        {
          headers: {
            'Content-Type': 'application/json',
            Accept: 'application/json',
          },
          timeout: 15_000,
        },
      );

      // Invalidation check: verify session was not logged out or replaced while network call was in-flight
      if (sessionGeneration !== requestGeneration || !getRefreshToken()) {
        throw new Error('Session invalidated during token refresh');
      }

      const authData = response.data?.data;
      if (!authData?.accessToken) {
        throw new Error('Malformed token refresh response');
      }

      // Store renewed access token and rotated refresh token
      memoryAccessToken = authData.accessToken;
      if (authData.refreshToken) {
        setRefreshToken(authData.refreshToken);
      }

      return authData.accessToken;
    } catch (err) {
      // Invalidate session on failure only if session hasn't already been superseded
      if (sessionGeneration === requestGeneration) {
        // Authoritative auth rejection: HTTP 401 or HTTP 403 (expired, revoked, or disabled)
        const isAxios = axios.isAxiosError(err);
        const status = isAxios
          ? err.response?.status
          : ((err as { status?: number; response?: { status?: number } })?.status ??
             (err as { response?: { status?: number } })?.response?.status);
        const isAuthRejection = status === 401 || status === 403;

        // Invalidate session on explicit 401/403 rejection.
        // For temporary network failures (no response, offline, or 5xx server error), preserve recoverable refresh token in storage.
        if (isAuthRejection) {
          clearSession();
        }
      }
      throw err;
    } finally {
      if (sessionGeneration === requestGeneration) {
        inFlightRefreshPromise = null;
      }
    }
  })();

  const newToken = await inFlightRefreshPromise;
  if (!newToken) {
    throw new Error('Token refresh returned null');
  }
  return newToken;
}

/**
 * Subscribe to session state transitions.
 */
export function subscribeToSession(listener: SessionListener): () => void {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}

function notifyListeners(isAuthenticated: boolean): void {
  listeners.forEach((listener) => {
    try {
      listener(isAuthenticated);
    } catch {
      // Prevent listener errors from interrupting execution
    }
  });
}
