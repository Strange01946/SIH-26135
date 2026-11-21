import React, { useState } from 'react';
import { useLocation, useNavigate, useSearchParams, Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useAuth } from '@/context/AuthContext';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { type ApiError, getDefaultLandingRoute, isRouteAuthorizedForAuthorities } from '@/api/types';

const loginSchema = z.object({
  usernameOrEmail: z
    .string()
    .min(1, 'Username or email address is required')
    .max(255, 'Username or email cannot exceed 255 characters'),
  password: z
    .string()
    .min(1, 'Password is required')
    .max(128, 'Password cannot exceed 128 characters'),
});

type LoginFormData = z.infer<typeof loginSchema>;

export function LoginPage(): React.JSX.Element {
  const { login, isAuthenticated, user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams] = useSearchParams();

  const [showPassword, setShowPassword] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isLocked, setIsLocked] = useState(false);

  const sessionExpired = searchParams.get('sessionExpired') === 'true';

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      usernameOrEmail: '',
      password: '',
    },
  });

  // Redirect if already authenticated
  React.useEffect(() => {
    if (isAuthenticated && user) {
      const from = (location.state as { from?: { pathname?: string } })?.from?.pathname;
      const destination =
        from && isRouteAuthorizedForAuthorities(from, user.authorities)
          ? from
          : getDefaultLandingRoute(user.authorities);
      navigate(destination, { replace: true });
    }
  }, [isAuthenticated, user, location.state, navigate]);

  const onSubmit = async (data: LoginFormData) => {
    setErrorMessage(null);
    setIsLocked(false);

    try {
      const authResponse = await login(data);
      const authorities = authResponse.authorities ?? [];
      const from = (location.state as { from?: { pathname?: string } })?.from?.pathname;

      // Calculate destination freshly from newly verified authorities
      // Only preserve 'from' destination if user is authorized for that page
      const destination =
        from && isRouteAuthorizedForAuthorities(from, authorities)
          ? from
          : getDefaultLandingRoute(authorities);

      navigate(destination, { replace: true });
    } catch (err) {
      const apiError = err as ApiError;

      if (apiError.status === 423 || apiError.code === 'AUTH_ACCOUNT_LOCKED' || apiError.code === 'ACCOUNT_LOCKED') {
        setIsLocked(true);
        setErrorMessage(
          'Your account has been temporarily locked due to consecutive failed login attempts. Please contact your system administrator or try again later.',
        );
      } else if (apiError.status === 401 || apiError.code === 'INVALID_CREDENTIALS') {
        setErrorMessage('Invalid username or password. Please verify your credentials and try again.');
      } else if (apiError.isNetworkError) {
        setErrorMessage('Unable to connect to the authentication service. Please check your network connection.');
      } else if (apiError.isTimeout) {
        setErrorMessage('The authentication request timed out. Please try again.');
      } else {
        setErrorMessage(apiError.message || 'An unexpected error occurred during sign in. Please try again.');
      }
    }
  };

  return (
    <div className="min-h-screen flex flex-col justify-center py-12 sm:px-6 lg:px-8 bg-gray-50">
      {/* Top flag accent stripe */}
      <div className="fixed top-0 left-0 right-0 h-1 bg-gradient-to-r from-saffron via-white to-india-green" />

      <div className="sm:mx-auto sm:w-full sm:max-w-md text-center">
        {/* Government Emblem */}
        <div
          className="mx-auto h-16 w-16 rounded-full bg-navy/10 flex items-center justify-center text-3xl shadow-sm mb-3"
          aria-hidden="true"
        >
          🏛️
        </div>
        <h1 className="text-2xl font-bold text-navy tracking-tight">
          National Skill &amp; Placement Tracking Platform
        </h1>
        <p className="mt-1 text-xs text-gray-500 font-medium">
          Ministry of Skill Development &amp; Entrepreneurship · Government of India
        </p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md px-4 sm:px-0">
        <div className="bg-white py-8 px-6 shadow-sm border border-gray-200 rounded-lg sm:px-10">
          <h2 className="text-lg font-semibold text-gray-900 border-b border-gray-100 pb-3 mb-6">
            Sign In to Official Portal
          </h2>

          {/* Session Expired Banner */}
          {sessionExpired && (
            <div
              role="status"
              className="mb-5 rounded-md bg-amber-50 p-3.5 border border-amber-200 text-sm text-amber-900 flex items-start gap-2.5"
            >
              <span className="text-amber-600 mt-0.5" aria-hidden="true">
                ℹ️
              </span>
              <span>Your session has expired. Please sign in again to continue your work.</span>
            </div>
          )}

          {/* Authentication Error Alert */}
          {errorMessage && (
            <div
              role="alert"
              className={`mb-5 rounded-md p-3.5 border text-sm flex items-start gap-2.5 ${
                isLocked
                  ? 'bg-red-50 border-red-300 text-red-900'
                  : 'bg-red-50 border-red-200 text-red-800'
              }`}
            >
              <span className="text-red-600 mt-0.5" aria-hidden="true">
                ⚠️
              </span>
              <span className="leading-snug">{errorMessage}</span>
            </div>
          )}

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-5" noValidate>
            {/* Username or Email Input */}
            <div>
              <label htmlFor="usernameOrEmail" className="form-label">
                Username or Official Email <span className="text-red-600">*</span>
              </label>
              <input
                id="usernameOrEmail"
                type="text"
                autoComplete="username"
                disabled={isSubmitting}
                aria-invalid={Boolean(errors.usernameOrEmail)}
                aria-describedby={errors.usernameOrEmail ? 'username-error' : undefined}
                className="form-input"
                placeholder="Enter username or official email"
                {...register('usernameOrEmail')}
              />
              {errors.usernameOrEmail && (
                <p id="username-error" className="form-error" role="alert">
                  {errors.usernameOrEmail.message}
                </p>
              )}
            </div>

            {/* Password Input with Visibility Toggle */}
            <div>
              <label htmlFor="password" className="form-label">
                Password <span className="text-red-600">*</span>
              </label>
              <div className="relative">
                <input
                  id="password"
                  type={showPassword ? 'text' : 'password'}
                  autoComplete="current-password"
                  disabled={isSubmitting}
                  aria-invalid={Boolean(errors.password)}
                  aria-describedby={errors.password ? 'password-error' : undefined}
                  className="form-input pr-11"
                  placeholder="Enter your password"
                  {...register('password')}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword((prev) => !prev)}
                  disabled={isSubmitting}
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                  className="absolute inset-y-0 right-0 px-3 flex items-center text-sm text-gray-500 hover:text-gray-700 focus:outline-none focus:text-navy"
                >
                  {showPassword ? '🙈' : '👁️'}
                </button>
              </div>
              {errors.password && (
                <p id="password-error" className="form-error" role="alert">
                  {errors.password.message}
                </p>
              )}
            </div>

            {/* Submit Button */}
            <div>
              <button
                type="submit"
                disabled={isSubmitting}
                className="w-full btn-primary justify-center py-2.5 text-base font-medium shadow-sm"
              >
                {isSubmitting ? (
                  <span className="flex items-center gap-2">
                    <LoadingSpinner size="sm" />
                    <span>Verifying Credentials…</span>
                  </span>
                ) : (
                  'Sign In'
                )}
              </button>
            </div>
          </form>

          {/* Official Security Notice */}
          <div className="mt-6 pt-4 border-t border-gray-100 text-center">
            <p className="text-xs text-gray-500 leading-normal">
              Unauthorized access to this government system is strictly prohibited under the IT Act, 2000.
              All sessions are monitored and logged for security and auditing purposes.
            </p>
          </div>
        </div>

        {/* Back to Portal Landing */}
        <div className="mt-4 text-center">
          <Link to="/" className="text-xs text-navy hover:underline font-medium">
            ← Return to Platform Home
          </Link>
        </div>
      </div>
    </div>
  );
}
