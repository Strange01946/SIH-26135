import React from 'react';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { render, screen, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AuthProvider, useAuth } from '@/context/AuthContext';
import * as apiClient from '@/api/client';
import * as sessionManager from '@/lib/sessionManager';
import type { AuthenticatedUserResponse, AuthResponse } from '@/api/types';

function TestConsumer(): React.JSX.Element {
  const {
    user,
    isAuthenticated,
    isInitializing,
    personas,
    primaryPersona,
    defaultLandingRoute,
    login,
    logout,
    hasRole,
    hasAuthority,
  } = useAuth();

  return (
    <div>
      <div data-testid="isInitializing">{String(isInitializing)}</div>
      <div data-testid="isAuthenticated">{String(isAuthenticated)}</div>
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

  it('clears session on mount when silent refresh fails', async () => {
    sessionManager.setRefreshToken('invalid-refresh-token');

    vi.spyOn(sessionManager, 'refreshAccessToken').mockRejectedValue(
      new Error('Token expired or revoked'),
    );

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
    expect(sessionManager.getAccessToken()).toBeNull();
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
});
