import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { LoginPage } from '@/pages/auth/LoginPage';
import * as authContext from '@/context/AuthContext';
import type { ApiError, AuthResponse } from '@/api/types';

describe('LoginPage Component', () => {
  const mockLogin = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  function renderLoginPage(initialEntries = ['/login'], authOverrides = {}) {
    const defaultAuth: authContext.AuthContextType = {
      user: null,
      isAuthenticated: false,
      isInitializing: false,
      connectionError: null,
      personas: [],
      primaryPersona: null,
      activePersona: null,
      setActivePersona: vi.fn(),
      defaultLandingRoute: '/admin/dashboard',
      login: mockLogin,
      establishSession: vi.fn(),
      logout: vi.fn(),
      refreshProfile: vi.fn(),
      retrySession: vi.fn(),
      hasRole: vi.fn(),
      hasAnyRole: vi.fn(),
      hasAuthority: vi.fn(),
      ...authOverrides,
    };

    vi.spyOn(authContext, 'useAuth').mockReturnValue(defaultAuth);

    return render(
      <MemoryRouter initialEntries={initialEntries}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/admin/dashboard" element={<div>Admin Dashboard Target</div>} />
          <Route path="/provider/dashboard" element={<div>Provider Dashboard Target</div>} />
          <Route path="/assessor/dashboard" element={<div>Assessor Dashboard Target</div>} />
          <Route path="/employer/dashboard" element={<div>Employer Dashboard Target</div>} />
          <Route path="/trainee/dashboard" element={<div>Trainee Dashboard Target</div>} />
          <Route path="/batches" element={<div>Batches Management Target</div>} />
          <Route path="/target-dest" element={<div>Target Destination</div>} />
        </Routes>
      </MemoryRouter>,
    );
  }

  it('renders login page with official title, inputs, and submit button', () => {
    renderLoginPage();

    expect(screen.getByRole('heading', { name: /^Sign in$/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/Username or Official Email/i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/Enter your password/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /^Sign In$/i })).toBeInTheDocument();
  });

  it('validates blank fields on submission using Zod schema', async () => {
    const user = userEvent.setup();
    renderLoginPage();

    const submitButton = screen.getByRole('button', { name: /^Sign In$/i });
    await user.click(submitButton);

    expect(await screen.findByText(/Username or email.*required/i)).toBeInTheDocument();
    expect(await screen.findByText(/Password is required/i)).toBeInTheDocument();
    expect(mockLogin).not.toHaveBeenCalled();
  });

  it('toggles password visibility when the eye button is clicked', async () => {
    const user = userEvent.setup();
    renderLoginPage();

    const passwordInput = screen.getByPlaceholderText(/Enter your password/i);
    const toggleButton = screen.getByRole('button', { name: 'Show password' });

    expect(passwordInput).toHaveAttribute('type', 'password');

    // Toggle to visible
    await user.click(toggleButton);
    expect(passwordInput).toHaveAttribute('type', 'text');
    expect(screen.getByRole('button', { name: 'Hide password' })).toBeInTheDocument();

    // Toggle back to hidden
    await user.click(screen.getByRole('button', { name: 'Hide password' }));
    expect(passwordInput).toHaveAttribute('type', 'password');
    expect(screen.getByRole('button', { name: 'Show password' })).toBeInTheDocument();
  });

  it('disables submit button and shows loading state during submission', async () => {
    const user = userEvent.setup();
    let resolveLogin: (res: AuthResponse) => void;
    mockLogin.mockReturnValueOnce(
      new Promise<AuthResponse>((resolve) => {
        resolveLogin = resolve;
      }),
    );

    renderLoginPage();

    await user.type(screen.getByLabelText(/Username or Official Email/i), 'officer1');
    await user.type(screen.getByPlaceholderText(/Enter your password/i), 'SecretPass123!');

    const submitButton = screen.getByRole('button', { name: /^Sign In$/i });
    await user.click(submitButton);

    // During in-flight request: button is disabled with spinner / loading label
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Verifying Credentials/i })).toBeDisabled();
    });

    // Resolve in-flight login
    resolveLogin!({
      accessToken: 'acc',
      refreshToken: 'ref',
      tokenType: 'Bearer',
      expiresIn: 3600,
      userId: 1,
      username: 'officer1',
      authorities: ['ROLE_ADMIN'],
    });

    await waitFor(() => {
      expect(screen.queryByRole('button', { name: /Verifying Credentials/i })).toBeNull();
    });
  });

  it('displays session expired alert banner when query param sessionExpired=true is present', () => {
    renderLoginPage(['/login?sessionExpired=true']);

    expect(
      screen.getByText(/Your session has expired\. Please sign in again to continue your work\./i),
    ).toBeInTheDocument();
  });

  it('displays invalid credentials alert on HTTP 401 error', async () => {
    const user = userEvent.setup();
    const apiError: ApiError = {
      status: 401,
      code: 'INVALID_CREDENTIALS',
      message: 'Bad credentials',
    };
    mockLogin.mockRejectedValueOnce(apiError);

    renderLoginPage();

    await user.type(screen.getByLabelText(/Username or Official Email/i), 'wronguser');
    await user.type(screen.getByPlaceholderText(/Enter your password/i), 'wrongpass');
    await user.click(screen.getByRole('button', { name: /^Sign In$/i }));

    expect(
      await screen.findByText(
        /Invalid username or password\. Please verify your credentials and try again\./i,
      ),
    ).toBeInTheDocument();
  });

  it('displays official account locked alert without fake countdown on HTTP 423', async () => {
    const user = userEvent.setup();
    const lockoutError: ApiError = {
      status: 423,
      code: 'AUTH_ACCOUNT_LOCKED',
      message: 'Account locked',
    };
    mockLogin.mockRejectedValueOnce(lockoutError);

    renderLoginPage();

    await user.type(screen.getByLabelText(/Username or Official Email/i), 'lockeduser');
    await user.type(screen.getByPlaceholderText(/Enter your password/i), 'somepass');
    await user.click(screen.getByRole('button', { name: /^Sign In$/i }));

    const alertBanner = await screen.findByRole('alert');
    expect(alertBanner).toBeInTheDocument();
    expect(
      screen.getByText(
        /Your account has been temporarily locked due to consecutive failed login attempts\. Please contact your system administrator or try again later\./i,
      ),
    ).toBeInTheDocument();

    // Strict invariant: verify NO fake countdown timer string is displayed
    expect(screen.queryByText(/unlock in \d+/i)).toBeNull();
    expect(screen.queryByText(/seconds remaining/i)).toBeNull();
  });

  it('displays user-friendly network error alert when network fails', async () => {
    const user = userEvent.setup();
    const networkError: ApiError = {
      status: 0,
      code: 'NETWORK_ERROR',
      message: 'Network Error',
      isNetworkError: true,
    };
    mockLogin.mockRejectedValueOnce(networkError);

    renderLoginPage();

    await user.type(screen.getByLabelText(/Username or Official Email/i), 'myuser');
    await user.type(screen.getByPlaceholderText(/Enter your password/i), 'mypass');
    await user.click(screen.getByRole('button', { name: /^Sign In$/i }));

    expect(
      await screen.findByText(
        /Unable to connect to the authentication service\. Please check your network connection\./i,
      ),
    ).toBeInTheDocument();
  });

  it('automatically redirects authenticated user away from /login to default landing route', async () => {
    renderLoginPage(['/login'], {
      isAuthenticated: true,
      user: {
        id: 1,
        username: 'admin',
        authorities: ['ROLE_ADMIN'],
      },
    });

    expect(await screen.findByText('Admin Dashboard Target')).toBeInTheDocument();
  });

  describe('Post-Login Dynamic Destination & Authority Validation', () => {
    const rolesToTest = [
      { role: 'ROLE_ADMIN', expected: 'Admin Dashboard Target' },
      { role: 'ROLE_TRAINING_PROVIDER', expected: 'Provider Dashboard Target' },
      { role: 'ROLE_ASSESSOR', expected: 'Assessor Dashboard Target' },
      { role: 'ROLE_EMPLOYER', expected: 'Employer Dashboard Target' },
      { role: 'ROLE_TRAINEE', expected: 'Trainee Dashboard Target' },
    ];

    rolesToTest.forEach(({ role, expected }) => {
      it(`calculates post-login landing route freshly for ${role} -> ${expected}`, async () => {
        const user = userEvent.setup();
        mockLogin.mockResolvedValueOnce({
          accessToken: 'token',
          refreshToken: 'refresh',
          tokenType: 'Bearer',
          expiresIn: 3600,
          userId: 1,
          username: 'test_user',
          authorities: [role],
        });

        renderLoginPage();

        await user.type(screen.getByLabelText(/Username or Official Email/i), 'user');
        await user.type(screen.getByPlaceholderText(/Enter your password/i), 'pass123');
        await user.click(screen.getByRole('button', { name: /^Sign In$/i }));

        expect(await screen.findByText(expected)).toBeInTheDocument();
      });
    });

    it('calculates destination for multi-role user with both ADMIN and TRAINING_PROVIDER', async () => {
      const user = userEvent.setup();
      mockLogin.mockResolvedValueOnce({
        accessToken: 'token',
        refreshToken: 'refresh',
        tokenType: 'Bearer',
        expiresIn: 3600,
        userId: 1,
        username: 'multi_user',
        authorities: ['ROLE_ADMIN', 'ROLE_TRAINING_PROVIDER'],
      });

      renderLoginPage();

      await user.type(screen.getByLabelText(/Username or Official Email/i), 'admin_tp');
      await user.type(screen.getByPlaceholderText(/Enter your password/i), 'pass123');
      await user.click(screen.getByRole('button', { name: /^Sign In$/i }));

      // ADMIN takes precedence
      expect(await screen.findByText('Admin Dashboard Target')).toBeInTheDocument();
    });

    it('preserves from destination when authorized: Admin logging in with from=/admin/dashboard', async () => {
      const user = userEvent.setup();
      mockLogin.mockResolvedValueOnce({
        accessToken: 'token',
        refreshToken: 'refresh',
        tokenType: 'Bearer',
        expiresIn: 3600,
        userId: 1,
        username: 'admin_officer',
        authorities: ['ROLE_ADMIN'],
      });

      renderLoginPage([
        {
          pathname: '/login',
          state: { from: { pathname: '/admin/dashboard' } },
        } as unknown as string,
      ]);

      await user.type(screen.getByLabelText(/Username or Official Email/i), 'admin');
      await user.type(screen.getByPlaceholderText(/Enter your password/i), 'pass');
      await user.click(screen.getByRole('button', { name: /^Sign In$/i }));

      expect(await screen.findByText('Admin Dashboard Target')).toBeInTheDocument();
    });

    it('prevents 403 trap: Trainee logging in with from=/admin/dashboard is safely redirected to /trainee/dashboard', async () => {
      const user = userEvent.setup();
      mockLogin.mockResolvedValueOnce({
        accessToken: 'token',
        refreshToken: 'refresh',
        tokenType: 'Bearer',
        expiresIn: 3600,
        userId: 1,
        username: 'trainee_learner',
        authorities: ['ROLE_TRAINEE'],
      });

      renderLoginPage([
        {
          pathname: '/login',
          state: { from: { pathname: '/admin/dashboard' } },
        } as unknown as string,
      ]);

      await user.type(screen.getByLabelText(/Username or Official Email/i), 'trainee');
      await user.type(screen.getByPlaceholderText(/Enter your password/i), 'pass');
      await user.click(screen.getByRole('button', { name: /^Sign In$/i }));

      // MUST NOT land on Admin Dashboard or 403; must land safely on Trainee Dashboard!
      expect(await screen.findByText('Trainee Dashboard Target')).toBeInTheDocument();
      expect(screen.queryByText('Admin Dashboard Target')).toBeNull();
    });

    it('preserves from destination when authorized: Training Provider logging in with from=/batches', async () => {
      const user = userEvent.setup();
      mockLogin.mockResolvedValueOnce({
        accessToken: 'token',
        refreshToken: 'refresh',
        tokenType: 'Bearer',
        expiresIn: 3600,
        userId: 1,
        username: 'provider_manager',
        authorities: ['ROLE_TRAINING_PROVIDER'],
      });

      renderLoginPage([
        {
          pathname: '/login',
          state: { from: { pathname: '/batches' } },
        } as unknown as string,
      ]);

      await user.type(screen.getByLabelText(/Username or Official Email/i), 'tp');
      await user.type(screen.getByPlaceholderText(/Enter your password/i), 'pass');
      await user.click(screen.getByRole('button', { name: /^Sign In$/i }));

      expect(await screen.findByText('Batches Management Target')).toBeInTheDocument();
    });

    it('redirects to role landing route when from destination is unauthorized: Trainee logging in with from=/batches', async () => {
      const user = userEvent.setup();
      mockLogin.mockResolvedValueOnce({
        accessToken: 'token',
        refreshToken: 'refresh',
        tokenType: 'Bearer',
        expiresIn: 3600,
        userId: 1,
        username: 'trainee_user',
        authorities: ['ROLE_TRAINEE'],
      });

      renderLoginPage([
        {
          pathname: '/login',
          state: { from: { pathname: '/batches' } },
        } as unknown as string,
      ]);

      await user.type(screen.getByLabelText(/Username or Official Email/i), 'trainee');
      await user.type(screen.getByPlaceholderText(/Enter your password/i), 'pass');
      await user.click(screen.getByRole('button', { name: /^Sign In$/i }));

      // Trainee cannot access /batches, so safely redirected to Trainee Dashboard
      expect(await screen.findByText('Trainee Dashboard Target')).toBeInTheDocument();
      expect(screen.queryByText('Batches Management Target')).toBeNull();
    });
  });
});
