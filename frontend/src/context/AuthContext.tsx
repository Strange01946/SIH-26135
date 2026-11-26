import React, {
  createContext,
  useContext,
  useState,
  useEffect,
  useCallback,
  useMemo,
} from 'react';
import { apiGet, apiPost } from '@/api/client';
import {
  hasSession,
  refreshAccessToken,
  setTokens,
  clearSession,
  subscribeToSession,
  getSessionGeneration,
} from '@/lib/sessionManager';
import {
  type AuthenticatedUserResponse,
  type AuthResponse,
  type LoginRequest,
  type UserPersona,
  getUserPersonas,
  getDefaultLandingRoute,
  getDefaultLandingRouteForPersona,
} from '@/api/types';
import { queryClient } from '@/lib/queryClient';

const personaStorageKey = 'sih_active_persona';

export interface AuthContextType {
  user: AuthenticatedUserResponse | null;
  isAuthenticated: boolean;
  isInitializing: boolean;
  connectionError: string | null;
  personas: UserPersona[];
  primaryPersona: UserPersona | null;
  activePersona: UserPersona | null;
  setActivePersona: (persona: UserPersona) => void;
  defaultLandingRoute: string;
  login: (credentials: LoginRequest) => Promise<AuthResponse>;
  establishSession: (response: AuthResponse) => void;
  logout: () => void;
  refreshProfile: () => Promise<AuthenticatedUserResponse | null>;
  retrySession: () => Promise<void>;
  hasRole: (role: string) => boolean;
  hasAnyRole: (roles: string[]) => boolean;
  hasAuthority: (authority: string) => boolean;
}

const defaultUnauthenticatedContext: AuthContextType = {
  user: null,
  isAuthenticated: false,
  isInitializing: false,
  connectionError: null,
  personas: [],
  primaryPersona: null,
  activePersona: null,
  setActivePersona: () => {},
  defaultLandingRoute: '/login',
  login: async () => {
    throw new Error('login must be called within an AuthProvider');
  },
  establishSession: () => {},
  logout: () => {},
  refreshProfile: async () => null,
  retrySession: async () => {},
  hasRole: () => false,
  hasAnyRole: () => false,
  hasAuthority: () => false,
};

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({
  children,
}: {
  children: React.ReactNode;
}): React.JSX.Element {
  const [user, setUser] = useState<AuthenticatedUserResponse | null>(null);
  const [isInitializing, setIsInitializing] = useState<boolean>(true);
  const [connectionError, setConnectionError] = useState<string | null>(null);

  // Keep route permissions synchronized with refresh results and session expiry.
  useEffect(() => {
    const unsubscribe = subscribeToSession((active, renewedSession) => {
      if (!active) {
        setUser(null);
        setConnectionError(null);
      } else if (renewedSession && Array.isArray(renewedSession.authorities)) {
        setUser((current) => current?.id === renewedSession.userId
          ? { ...current, authorities: renewedSession.authorities }
          : current);
      }
    });
    return unsubscribe;
  }, []);

  // Fetch verified profile from GET /api/v1/auth/me (protected against race conditions)
  const fetchProfile = useCallback(async (): Promise<AuthenticatedUserResponse | null> => {
    const generation = getSessionGeneration();
    try {
      const profile = await apiGet<AuthenticatedUserResponse>('/api/v1/auth/me');
      if (getSessionGeneration() === generation) {
        setUser(profile);
        setConnectionError(null);
        return profile;
      }
      return null;
    } catch (err: unknown) {
      const apiErr = err as { status?: number; response?: { status?: number } };
      const status = apiErr?.status ?? apiErr?.response?.status;
      if (status === 401 || status === 403) {
        if (getSessionGeneration() === generation) {
          clearSession();
          setUser(null);
          setConnectionError(null);
        }
      } else {
        if (getSessionGeneration() === generation) {
          setConnectionError('Unable to verify profile due to network or server error.');
        }
      }
      return null;
    }
  }, []);

  // Startup session restoration with explicit error classification
  useEffect(() => {
    let isMounted = true;

    async function initializeSession() {
      if (!hasSession()) {
        if (isMounted) {
          setIsInitializing(false);
        }
        return;
      }

      const generation = getSessionGeneration();

      try {
        // Attempt silent refresh using stored refresh token
        await refreshAccessToken();
        if (!isMounted || getSessionGeneration() !== generation) {
          return;
        }

        // Fetch verified profile from /api/v1/auth/me
        const profile = await apiGet<AuthenticatedUserResponse>('/api/v1/auth/me');
        if (isMounted && getSessionGeneration() === generation) {
          setUser(profile);
          setConnectionError(null);
        }
      } catch (err: unknown) {
        if (!isMounted || getSessionGeneration() !== generation) {
          return;
        }

        const apiErr = err as { status?: number; response?: { status?: number } };
        const status = apiErr?.status ?? apiErr?.response?.status;
        const isAuthError = status === 401 || status === 403;

        if (isAuthError) {
          // Explicit 401/403 authorization rejection: invalidate session and reset user state
          clearSession();
          setUser(null);
          setConnectionError(null);
        } else {
          // Temporary network failure or 5xx:
          // Preserve recoverable session state in storage (do NOT call clearSession()),
          // set reconnection error state, and DO NOT falsely claim authenticated profile verification.
          setUser(null);
          setConnectionError('Unable to connect to authentication server. Please check your network and retry.');
        }
      } finally {
        if (isMounted) {
          setIsInitializing(false);
        }
      }
    }

    initializeSession();

    return () => {
      isMounted = false;
    };
  }, []);

  // Retry session restoration (for reconnection when network was offline or server recovered)
  const retrySession = useCallback(async (): Promise<void> => {
    if (!hasSession()) return;
    const generation = getSessionGeneration();
    try {
      await refreshAccessToken();
      if (getSessionGeneration() === generation) {
        const profile = await apiGet<AuthenticatedUserResponse>('/api/v1/auth/me');
        if (getSessionGeneration() === generation) {
          setUser(profile);
          setConnectionError(null);
        }
      }
    } catch (err: unknown) {
      const apiErr = err as { status?: number; response?: { status?: number } };
      const status = apiErr?.status ?? apiErr?.response?.status;
      if (status === 401 || status === 403) {
        if (getSessionGeneration() === generation) {
          clearSession();
          setUser(null);
          setConnectionError(null);
        }
      } else {
        if (getSessionGeneration() === generation) {
          setConnectionError('Reconnection attempt failed. Please check your network.');
        }
      }
    }
  }, []);

  // Establish authenticated session directly from AuthResponse (used by login and registration)
  const establishSession = useCallback((response: AuthResponse): void => {
    // Clear any previous user's cached queries and persona
    try {
      localStorage.removeItem(personaStorageKey);
    } catch {
      // Ignore storage errors
    }
    queryClient.clear();

    // Store in-memory access token and persistent refresh token (increments sessionGeneration)
    setTokens(response.accessToken, response.refreshToken);
    const sessionGen = getSessionGeneration();
    setConnectionError(null);

    // Populate user profile immediately from response
    const immediateUser: AuthenticatedUserResponse = {
      id: response.userId,
      username: response.username,
      authorities: response.authorities ?? [],
    };
    setUser(immediateUser);

    // Asynchronously fetch full tenancy profile from /me without blocking login completion
    apiGet<AuthenticatedUserResponse>('/api/v1/auth/me')
      .then((fullProfile) => {
        if (fullProfile && getSessionGeneration() === sessionGen) {
          setUser(fullProfile);
          setConnectionError(null);
        }
      })
      .catch((err: unknown) => {
        const apiErr = err as { status?: number; response?: { status?: number } };
        const status = apiErr?.status ?? apiErr?.response?.status;
        // If /me explicitly fails due to authentication or authorization rejection (401/403),
        // clear the session to prevent an invalid, deactivated, or revoked account from remaining authenticated.
        if (status === 401 || status === 403) {
          if (getSessionGeneration() === sessionGen) {
            clearSession();
            setUser(null);
            setConnectionError(null);
          }
        } else {
          // For temporary network errors (status 0, status >= 500), retain immediateUser so the UI is resilient
          if (getSessionGeneration() === sessionGen) {
            setConnectionError('Profile synchronization pending: unable to reach server.');
          }
        }
      });
  }, []);

  // Login handler (protected against race conditions)
  const login = useCallback(
    async (credentials: LoginRequest): Promise<AuthResponse> => {
      const response = await apiPost<LoginRequest, AuthResponse>(
        '/api/v1/auth/login',
        credentials,
      );
      establishSession(response);
      return response;
    },
    [establishSession],
  );

  // Active Persona Context for role-specific workbench navigation & multi-role switching
  const [activePersonaState, setActivePersonaState] = useState<UserPersona | null>(() => {
    try {
      const saved = localStorage.getItem(personaStorageKey);
      return (saved as UserPersona) || null;
    } catch {
      return null;
    }
  });

  const personas = useMemo(
    () => getUserPersonas(user?.authorities ?? [], user),
    [user?.authorities, user?.stateId, user?.districtId],
  );

  const primaryPersona = useMemo(
    () => (personas.length > 0 ? personas[0] : null),
    [personas],
  );

  // Synchronize activePersona with available personas
  useEffect(() => {
    if (personas.length === 0) {
      if (activePersonaState !== null) {
        setActivePersonaState(null);
        try {
          localStorage.removeItem(personaStorageKey);
        } catch {
          // Ignore storage errors
        }
      }
    } else if (!activePersonaState || !personas.includes(activePersonaState)) {
      const fallbackPersona = personas[0];
      setActivePersonaState(fallbackPersona);
      try {
        localStorage.setItem(personaStorageKey, fallbackPersona);
      } catch {
        // Ignore storage errors
      }
    }
  }, [personas, activePersonaState]);

  const setActivePersona = useCallback(
    (newPersona: UserPersona) => {
      if (personas.includes(newPersona)) {
        setActivePersonaState(newPersona);
        try {
          localStorage.setItem(personaStorageKey, newPersona);
        } catch {
          // Ignore storage errors
        }
        queryClient.invalidateQueries();
      }
    },
    [personas],
  );

  // Logout handler
  const logout = useCallback(() => {
    clearSession();
    setUser(null);
    setActivePersonaState(null);
    try {
      localStorage.removeItem(personaStorageKey);
    } catch {
      // Ignore storage errors
    }
    queryClient.clear();
  }, []);

  // Check single authority or role (case-insensitive)
  const hasAuthority = useCallback(
    (authority: string): boolean => {
      if (!user || !Array.isArray(user.authorities)) return false;
      const target = authority.toLowerCase().trim();
      return user.authorities.some((a) => a.toLowerCase().trim() === target);
    },
    [user],
  );

  // Check specific role (adds ROLE_ prefix if needed)
  const hasRole = useCallback(
    (role: string): boolean => {
      if (!user || !Array.isArray(user.authorities)) return false;
      const cleanRole = role.toUpperCase().startsWith('ROLE_')
        ? role.toLowerCase().trim()
        : `role_${role.toLowerCase().trim()}`;
      return user.authorities.some((a) => a.toLowerCase().trim() === cleanRole);
    },
    [user],
  );

  // Check if user has ANY of the specified roles or authorities
  const hasAnyRole = useCallback(
    (roles: string[]): boolean => {
      if (!user || !Array.isArray(user.authorities)) return false;
      return roles.some((r) => hasRole(r) || hasAuthority(r));
    },
    [user, hasRole, hasAuthority],
  );

  const defaultLandingRoute = useMemo(
    () => {
      if (activePersonaState && personas.includes(activePersonaState)) {
        return getDefaultLandingRouteForPersona(activePersonaState);
      }
      return getDefaultLandingRoute(user?.authorities ?? []);
    },
    [activePersonaState, personas, user?.authorities],
  );

  const value = useMemo<AuthContextType>(
    () => ({
      user,
      isAuthenticated: Boolean(user),
      isInitializing,
      connectionError,
      personas,
      primaryPersona,
      activePersona: activePersonaState,
      setActivePersona,
      defaultLandingRoute,
      login,
      establishSession,
      logout,
      refreshProfile: fetchProfile,
      retrySession,
      hasRole,
      hasAnyRole,
      hasAuthority,
    }),
    [
      user,
      isInitializing,
      connectionError,
      personas,
      primaryPersona,
      activePersonaState,
      setActivePersona,
      defaultLandingRoute,
      login,
      establishSession,
      logout,
      fetchProfile,
      retrySession,
      hasRole,
      hasAnyRole,
      hasAuthority,
    ],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  return context || defaultUnauthenticatedContext;
}
