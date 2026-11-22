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
} from '@/api/types';

export interface AuthContextType {
  user: AuthenticatedUserResponse | null;
  isAuthenticated: boolean;
  isInitializing: boolean;
  personas: UserPersona[];
  primaryPersona: UserPersona | null;
  defaultLandingRoute: string;
  login: (credentials: LoginRequest) => Promise<AuthResponse>;
  establishSession: (response: AuthResponse) => void;
  logout: () => void;
  refreshProfile: () => Promise<AuthenticatedUserResponse | null>;
  hasRole: (role: string) => boolean;
  hasAnyRole: (roles: string[]) => boolean;
  hasAuthority: (authority: string) => boolean;
}

const defaultUnauthenticatedContext: AuthContextType = {
  user: null,
  isAuthenticated: false,
  isInitializing: false,
  personas: [],
  primaryPersona: null,
  defaultLandingRoute: '/login',
  login: async () => {
    throw new Error('login must be called within an AuthProvider');
  },
  establishSession: () => {},
  logout: () => {},
  refreshProfile: async () => null,
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

  // Synchronize state when sessionManager terminates session externally (e.g. 401 refresh failure)
  useEffect(() => {
    const unsubscribe = subscribeToSession((active) => {
      if (!active) {
        setUser(null);
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
        return profile;
      }
      return null;
    } catch (err: unknown) {
      const apiErr = err as { status?: number };
      if (apiErr?.status === 401 || apiErr?.status === 403) {
        if (getSessionGeneration() === generation) {
          clearSession();
          setUser(null);
        }
      }
      return null;
    }
  }, []);

  // Startup session restoration
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
        if (isMounted && getSessionGeneration() === generation) {
          const profile = await fetchProfile();
          if (!profile && getSessionGeneration() === generation) {
            clearSession();
            setUser(null);
          }
        }
      } catch {
        if (isMounted && getSessionGeneration() === generation) {
          clearSession();
          setUser(null);
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
  }, [fetchProfile]);

  // Establish authenticated session directly from AuthResponse (used by login and registration)
  const establishSession = useCallback((response: AuthResponse): void => {
    // Store in-memory access token and persistent refresh token (increments sessionGeneration)
    setTokens(response.accessToken, response.refreshToken);
    const sessionGen = getSessionGeneration();

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
        }
      })
      .catch((err: unknown) => {
        const apiErr = err as { status?: number };
        // If /me explicitly fails due to authentication or authorization rejection (401/403),
        // clear the session to prevent an invalid, deactivated, or revoked account from remaining authenticated.
        if (apiErr?.status === 401 || apiErr?.status === 403) {
          if (getSessionGeneration() === sessionGen) {
            clearSession();
            setUser(null);
          }
        }
        // For temporary network errors (status 0, status >= 500), retain immediateUser so the UI is resilient.
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

  // Logout handler
  const logout = useCallback(() => {
    clearSession();
    setUser(null);
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

  const personas = useMemo(
    () => getUserPersonas(user?.authorities ?? []),
    [user?.authorities],
  );

  const primaryPersona = useMemo(
    () => (personas.length > 0 ? personas[0] : null),
    [personas],
  );

  const defaultLandingRoute = useMemo(
    () => getDefaultLandingRoute(user?.authorities ?? []),
    [user?.authorities],
  );

  const value = useMemo<AuthContextType>(
    () => ({
      user,
      isAuthenticated: Boolean(user),
      isInitializing,
      personas,
      primaryPersona,
      defaultLandingRoute,
      login,
      establishSession,
      logout,
      refreshProfile: fetchProfile,
      hasRole,
      hasAnyRole,
      hasAuthority,
    }),
    [
      user,
      isInitializing,
      personas,
      primaryPersona,
      defaultLandingRoute,
      login,
      establishSession,
      logout,
      fetchProfile,
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
