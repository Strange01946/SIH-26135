import React from 'react';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { render, screen, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AuthProvider, useAuth, type AuthContextType } from '@/context/AuthContext';
import * as apiClient from '@/api/client';
import * as sessionManager from '@/lib/sessionManager';
import type { AuthenticatedUserResponse, AuthResponse } from '@/api/types';

function TestConsumer(): React.JSX.Element {
  const {
    user,
    isAuthenticated,
    isInitializing,
    connectionError,
    personas,
    primaryPersona,
    defaultLandingRoute,
    login,
    logout,
    retrySession,
    hasRole,
    hasAuthority,
  } = useAuth();

  return (
    <div>
      <div data-testid="isInitializing">{String(isInitializing)}</div>
      <div data-testid="isAuthenticated">{String(isAuthenticated)}</div>
      <div data-testid="connectionError">{connectionError ?? 'none'}</div>
      <div data-testid="username">{user?.username ?? 'none'}</div>
      <div data-testid="personas">{personas.join(',')}</div>
      <div data-testid="primaryPersona">{primaryPersona ?? 'none'}</div>
      <div data-testid="landingRoute">{defaultLandingRoute}</div>
      <div data-testid="hasRoleAdmin">{String(hasRole('ADMIN'))}</div>
      <div data-testid="hasAuthoritySystemManage">{String(hasAuthority('system.manage'))}</div>

      <button
        onClick={() =>
          login({
            usernameOrEmail: 'officer',
            password: 'password123',
          })
        }
      >
        Trigger Login
      </button>

      <button onClick={() => logout()}>Trigger Logout</button>
      <button onClick={() => { void retrySession(); }}>Trigger Retry</button>
    </div>
  );
}

describe('AuthContext & AuthProvider', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    sessionStorage.clear();
    sessionManager.clearSession();
  });

  afterEach(() => {
    sessionStorage.clear();
    sessionManager.clearSession();
  });

  it('initializes as unauthenticated when no session exists in storage', async () => {
    await act(async () => {
      render(
        <AuthProvider>
          <TestConsumer />
        </AuthProvider>,
      );
    });

    await waitFor(() => {
      expect(screen.getByTestId('isInitializing').textContent).toBe('false');
    });

    expect(screen.getByTestId('isAuthenticated').textContent).toBe('false');
    expect(screen.getByTestId('username').textContent).toBe('none');
    expect(screen.getByTestId('landingRoute').textContent).toBe('/dashboard');
  });

  it('restores authenticated user profile from storage on mount (Session Restoration)', async () => {
    sessionManager.setRefreshToken('stored-persisted-refresh');

    vi.spyOn(sessionManager, 'refreshAccessToken').mockResolvedValue('fresh-jwt-token');

    const mockProfile: AuthenticatedUserResponse = {
      id: 99,
      username: 'restored_admin',
      authorities: ['ROLE_ADMIN', 'system.manage', 'audit.read'],
    };

    vi.spyOn(apiClient, 'apiGet').mockImplementation(async (url: string) => {
      if (url === '/api/v1/auth/me') {
        return mockProfile;
      }
      throw new Error(`Unexpected GET: ${url}`);
    });

    await act(async () => {
      render(
        <AuthProvider>
          <TestConsumer />
        </AuthProvider>,
      );
    });

    // After session resolution
    await waitFor(() => {
      expect(screen.getByTestId('isInitializing').textContent).toBe('false');
    });

    expect(screen.getByTestId('isAuthenticated').textContent).toBe('true');
    expect(screen.getByTestId('username').textContent).toBe('restored_admin');
    expect(screen.getByTestId('personas').textContent).toBe('ADMIN');
    expect(screen.getByTestId('primaryPersona').textContent).toBe('ADMIN');
    expect(screen.getByTestId('landingRoute').textContent).toBe('/admin/dashboard');
    expect(screen.getByTestId('hasRoleAdmin').textContent).toBe('true');
    expect(screen.getByTestId('hasAuthoritySystemManage').textContent).toBe('true');
  });

  it('clears session on mount when silent refresh fails with 401/403 authorization rejection', async () => {
    sessionManager.setRefreshToken('invalid-refresh-token');

    const authErr = Object.assign(new Error('Token expired or revoked'), { status: 401 });
    vi.spyOn(sessionManager, 'refreshAccessToken').mockRejectedValue(authErr);

    await act(async () => {
      render(
        <AuthProvider>
          <TestConsumer />
        </AuthProvider>,
      );
    });

    await waitFor(() => {
      expect(screen.getByTestId('isInitializing').textContent).toBe('false');
    });

    expect(screen.getByTestId('isAuthenticated').textContent).toBe('false');
    expect(screen.getByTestId('username').textContent).toBe('none');
    expect(screen.getByTestId('connectionError').textContent).toBe('none');
    expect(sessionManager.getAccessToken()).toBeNull();
    expect(sessionManager.getRefreshToken()).toBeNull();
  });

  it('preserves session in storage and sets connectionError on mount when silent refresh encounters network failure', async () => {
    sessionManager.setRefreshToken('valid-offline-refresh-token');

    const networkErr = new Error('Network error connecting to auth server');
    vi.spyOn(sessionManager, 'refreshAccessToken').mockRejectedValue(networkErr);

    await act(async () => {
      render(
        <AuthProvider>
          <TestConsumer />
        </AuthProvider>,
      );
    });

    await waitFor(() => {
      expect(screen.getByTestId('isInitializing').textContent).toBe('false');
    });

    // Invariant: does not falsely claim authenticated profile verification, but preserves stored token
    expect(screen.getByTestId('isAuthenticated').textContent).toBe('false');
    expect(screen.getByTestId('username').textContent).toBe('none');
    expect(screen.getByTestId('connectionError').textContent).toContain('Unable to connect to authentication server');
    expect(sessionManager.getRefreshToken()).toBe('valid-offline-refresh-token');
  });

  it('allows retrying session restoration via retrySession when network connectivity returns', async () => {
    sessionManager.setRefreshToken('valid-offline-refresh-token');

    const networkErr = new Error('Network error');
    const refreshSpy = vi.spyOn(sessionManager, 'refreshAccessToken').mockRejectedValueOnce(networkErr);

    const mockProfile: AuthenticatedUserResponse = {
      id: 99,
      username: 'reconnected_user',
      authorities: ['ROLE_TRAINEE'],
    };

    vi.spyOn(apiClient, 'apiGet').mockImplementation(async (url: string) => {
      if (url === '/api/v1/auth/me') return mockProfile;
      throw new Error(`Unexpected GET: ${url}`);
    });

    await act(async () => {
      render(
        <AuthProvider>
          <TestConsumer />
        </AuthProvider>,
      );
    });

    await waitFor(() => {
      expect(screen.getByTestId('isInitializing').textContent).toBe('false');
    });

    expect(screen.getByTestId('connectionError').textContent).not.toBe('none');
    expect(screen.getByTestId('isAuthenticated').textContent).toBe('false');

    // Server comes back online
    refreshSpy.mockResolvedValueOnce('new-valid-jwt');

    // Trigger retry
    await act(async () => {
      await userEvent.click(screen.getByRole('button', { name: 'Trigger Retry' }));
    });

    await waitFor(() => {
      expect(screen.getByTestId('isAuthenticated').textContent).toBe('true');
      expect(screen.getByTestId('username').textContent).toBe('reconnected_user');
      expect(screen.getByTestId('connectionError').textContent).toBe('none');
    });
  });

  it('logs in user, sets tokens, retrieves full profile, and updates context state', async () => {
    const user = userEvent.setup();

    const mockAuthResponse: AuthResponse = {
      accessToken: 'auth-jwt-token',
      refreshToken: 'auth-refresh-token',
      tokenType: 'Bearer',
      expiresIn: 3600,
      userId: 42,
      username: 'provider_user',
      authorities: ['ROLE_TRAINING_PROVIDER', 'course.manage'],
    };

    const mockMeProfile: AuthenticatedUserResponse = {
      id: 42,
      username: 'provider_user',
      authorities: ['ROLE_TRAINING_PROVIDER', 'course.manage', 'enrollment.manage'],
      trainingProviderId: 1,
    };

    vi.spyOn(apiClient, 'apiPost').mockResolvedValue(mockAuthResponse);
    vi.spyOn(apiClient, 'apiGet').mockImplementation(async (url: string) => {
      if (url === '/api/v1/auth/me') return mockMeProfile;
      throw new Error(`Unexpected URL: ${url}`);
    });

    await act(async () => {
      render(
        <AuthProvider>
          <TestConsumer />
        </AuthProvider>,
      );
    });

    await waitFor(() => {
      expect(screen.getByTestId('isInitializing').textContent).toBe('false');
    });

    // Click trigger login
    await act(async () => {
      await user.click(screen.getByRole('button', { name: 'Trigger Login' }));
    });

    await waitFor(() => {
      expect(screen.getByTestId('isAuthenticated').textContent).toBe('true');
    });

    expect(screen.getByTestId('username').textContent).toBe('provider_user');
    expect(screen.getByTestId('personas').textContent).toBe('TRAINING_PROVIDER');
    expect(screen.getByTestId('landingRoute').textContent).toBe('/provider/dashboard');
    expect(sessionManager.getAccessToken()).toBe('auth-jwt-token');
    expect(sessionManager.getRefreshToken()).toBe('auth-refresh-token');
  });

  it('logs out user and purges all tokens and state', async () => {
    const user = userEvent.setup();

    sessionManager.setTokens('existing-access', 'existing-refresh');

    const mockProfile: AuthenticatedUserResponse = {
      id: 1,
      username: 'admin',
      authorities: ['ROLE_ADMIN'],
    };

    vi.spyOn(sessionManager, 'refreshAccessToken').mockResolvedValue('existing-access');
    vi.spyOn(apiClient, 'apiGet').mockResolvedValue(mockProfile);

    await act(async () => {
      render(
        <AuthProvider>
          <TestConsumer />
        </AuthProvider>,
      );
    });

    await waitFor(() => {
      expect(screen.getByTestId('isAuthenticated').textContent).toBe('true');
    });

    // Trigger logout
    await act(async () => {
      await user.click(screen.getByRole('button', { name: 'Trigger Logout' }));
    });

    expect(screen.getByTestId('isAuthenticated').textContent).toBe('false');
    expect(screen.getByTestId('username').textContent).toBe('none');
    expect(sessionManager.getAccessToken()).toBeNull();
    expect(sessionManager.getRefreshToken()).toBeNull();
  });

  it('prevents race conditions: in-flight /auth/me resolving after logout does not restore user state', async () => {
    let resolveMe: (profile: AuthenticatedUserResponse) => void;
    const pendingMe = new Promise<AuthenticatedUserResponse>((resolve) => {
      resolveMe = resolve;
    });

    const mockAuthResponse: AuthResponse = {
      accessToken: 'token-user-1',
      refreshToken: 'refresh-user-1',
      tokenType: 'Bearer',
      expiresIn: 3600,
      userId: 1,
      username: 'user_1',
      authorities: ['ROLE_TRAINEE'],
    };

    vi.spyOn(apiClient, 'apiPost').mockResolvedValue(mockAuthResponse);
    vi.spyOn(apiClient, 'apiGet').mockImplementation(async (url: string) => {
      if (url === '/api/v1/auth/me') return pendingMe;
      throw new Error(`Unexpected GET: ${url}`);
    });

    let authContextInstance: AuthContextType | undefined;
    function CaptureConsumer() {
      authContextInstance = useAuth();
      return <div>User: {authContextInstance.user?.username ?? 'none'}</div>;
    }

    await act(async () => {
      render(
        <AuthProvider>
          <CaptureConsumer />
        </AuthProvider>,
      );
    });

    // User logs in
    await act(async () => {
      await authContextInstance!.login({
        usernameOrEmail: 'user_1',
        password: 'password123',
      });
    });

    expect(screen.getByText('User: user_1')).toBeInTheDocument();

    // User logs out while /auth/me is still pending
    act(() => {
      authContextInstance!.logout();
    });

    expect(screen.getByText('User: none')).toBeInTheDocument();

    // Now /auth/me resolves
    await act(async () => {
      resolveMe!({
        id: 1,
        username: 'user_1_full_profile',
        authorities: ['ROLE_TRAINEE'],
      });
    });

    // Invariant: User must remain 'none' and NOT be resurrected
    expect(screen.getByText('User: none')).toBeInTheDocument();
  });

  it('prevents race conditions: in-flight /auth/me for User 1 resolving after User 2 logs in does not overwrite User 2', async () => {
    let resolveUser1Me: (profile: AuthenticatedUserResponse) => void;
    const pendingUser1Me = new Promise<AuthenticatedUserResponse>((resolve) => {
      resolveUser1Me = resolve;
    });

    const authResponseUser1: AuthResponse = {
      accessToken: 'token-user-1',
      refreshToken: 'refresh-user-1',
      tokenType: 'Bearer',
      expiresIn: 3600,
      userId: 1,
      username: 'first_user',
      authorities: ['ROLE_TRAINEE'],
    };

    const authResponseUser2: AuthResponse = {
      accessToken: 'token-user-2',
      refreshToken: 'refresh-user-2',
      tokenType: 'Bearer',
      expiresIn: 3600,
      userId: 2,
      username: 'second_user',
      authorities: ['ROLE_ADMIN'],
    };

    const meProfileUser2: AuthenticatedUserResponse = {
      id: 2,
      username: 'second_user',
      authorities: ['ROLE_ADMIN'],
    };

    let meCallCount = 0;
    vi.spyOn(apiClient, 'apiPost').mockImplementation(async (_url, body) => {
      const loginBody = body as { usernameOrEmail: string };
      if (loginBody.usernameOrEmail === 'first_user') return authResponseUser1;
      return authResponseUser2;
    });

    vi.spyOn(apiClient, 'apiGet').mockImplementation(async (url: string) => {
      if (url === '/api/v1/auth/me') {
        meCallCount++;
        if (meCallCount === 1) return pendingUser1Me;
        return meProfileUser2;
      }
      throw new Error(`Unexpected GET: ${url}`);
    });

    let authContextInstance: AuthContextType | undefined;
    function CaptureConsumer() {
      authContextInstance = useAuth();
      return <div>User: {authContextInstance.user?.username ?? 'none'}</div>;
    }

    await act(async () => {
      render(
        <AuthProvider>
          <CaptureConsumer />
        </AuthProvider>,
      );
    });

    // User 1 logs in (its /auth/me is pending)
    await act(async () => {
      await authContextInstance!.login({
        usernameOrEmail: 'first_user',
        password: 'password123',
      });
    });

    expect(screen.getByText('User: first_user')).toBeInTheDocument();

    // User 2 logs in
    await act(async () => {
      await authContextInstance!.login({
        usernameOrEmail: 'second_user',
        password: 'password456',
      });
    });

    expect(screen.getByText('User: second_user')).toBeInTheDocument();

    // Now User 1 /auth/me resolves
    await act(async () => {
      resolveUser1Me!({
        id: 1,
        username: 'first_user_stale_profile',
        authorities: ['ROLE_TRAINEE'],
      });
    });

    // Invariant: User 2 state must NOT be overwritten by User 1
    expect(screen.getByText('User: second_user')).toBeInTheDocument();
  });

  it('clears session when /api/v1/auth/me rejects with HTTP 401 or HTTP 403 on establishSession', async () => {
    const authResponse: AuthResponse = {
      accessToken: 'token-revoked',
      refreshToken: 'refresh-revoked',
      tokenType: 'Bearer',
      expiresIn: 3600,
      userId: 55,
      username: 'deactivated_user',
      authorities: ['ROLE_TRAINEE'],
    };

    vi.spyOn(apiClient, 'apiPost').mockResolvedValue(authResponse);
    vi.spyOn(apiClient, 'apiGet').mockImplementation(async (url: string) => {
      if (url === '/api/v1/auth/me') {
        const err = new Error('Unauthorized');
        (err as unknown as { status: number }).status = 401;
        throw err;
      }
      throw new Error(`Unexpected GET: ${url}`);
    });

    let authContextInstance: AuthContextType | undefined;
    function CaptureConsumer() {
      authContextInstance = useAuth();
      return (
        <div>
          <div data-testid="auth-status">{String(authContextInstance.isAuthenticated)}</div>
          <div data-testid="auth-user">{authContextInstance.user?.username ?? 'none'}</div>
        </div>
      );
    }

    await act(async () => {
      render(
        <AuthProvider>
          <CaptureConsumer />
        </AuthProvider>,
      );
    });

    await act(async () => {
      await authContextInstance!.login({
        usernameOrEmail: 'deactivated_user',
        password: 'password123',
      });
    });

    await waitFor(() => {
      expect(screen.getByTestId('auth-status').textContent).toBe('false');
      expect(screen.getByTestId('auth-user').textContent).toBe('none');
    });

    expect(sessionManager.getAccessToken()).toBeNull();
  });

  it('retains immediateUser when /api/v1/auth/me rejects with network/500 error on establishSession', async () => {
    const authResponse: AuthResponse = {
      accessToken: 'token-valid',
      refreshToken: 'refresh-valid',
      tokenType: 'Bearer',
      expiresIn: 3600,
      userId: 77,
      username: 'active_user',
      authorities: ['ROLE_TRAINEE'],
    };

    vi.spyOn(apiClient, 'apiPost').mockResolvedValue(authResponse);
    vi.spyOn(apiClient, 'apiGet').mockImplementation(async (url: string) => {
      if (url === '/api/v1/auth/me') {
        const err = new Error('Server Error');
        (err as unknown as { status: number }).status = 503;
        throw err;
      }
      throw new Error(`Unexpected GET: ${url}`);
    });

    let authContextInstance: AuthContextType | undefined;
    function CaptureConsumer() {
      authContextInstance = useAuth();
      return (
        <div>
          <div data-testid="auth-status">{String(authContextInstance.isAuthenticated)}</div>
          <div data-testid="auth-user">{authContextInstance.user?.username ?? 'none'}</div>
        </div>
      );
    }

    await act(async () => {
      render(
        <AuthProvider>
          <CaptureConsumer />
        </AuthProvider>,
      );
    });

    await act(async () => {
      await authContextInstance!.login({
        usernameOrEmail: 'active_user',
        password: 'password123',
      });
    });

    await waitFor(() => {
      expect(screen.getByTestId('auth-status').textContent).toBe('true');
      expect(screen.getByTestId('auth-user').textContent).toBe('active_user');
    });

    expect(sessionManager.getAccessToken()).toBe('token-valid');
  });
});
