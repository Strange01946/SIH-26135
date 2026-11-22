import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { ProtectedRoute } from '@/components/auth/ProtectedRoute';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { ForbiddenPage } from '@/pages/auth/ForbiddenPage';
import {
  AdminDashboard,
  ProviderDashboard,
  AssessorDashboard,
  EmployerDashboard,
  TraineeDashboard,
} from '@/pages/personas/PersonaDashboards';
import * as authContext from '@/context/AuthContext';

describe('Route Guards & Persona Access Control (ProtectedRoute, RoleGuard, PersonaDashboards)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  function setupAuthMock(overrides: Partial<authContext.AuthContextType> = {}) {
    const defaultAuth: authContext.AuthContextType = {
      user: null,
      isAuthenticated: false,
      isInitializing: false,
      connectionError: null,
      personas: [],
      primaryPersona: null,
      defaultLandingRoute: '/login',
      login: vi.fn(),
      establishSession: vi.fn(),
      logout: vi.fn(),
      refreshProfile: vi.fn(),
      retrySession: vi.fn(),
      hasRole: (r: string) => {
        const auths = defaultAuth.user?.authorities ?? [];
        return auths.some(
          (a) => a.toLowerCase() === r.toLowerCase() || a.toLowerCase() === `role_${r.toLowerCase()}`,
        );
      },
      hasAnyRole: (roles: string[]) => roles.some((r) => defaultAuth.hasRole(r)),
      hasAuthority: (auth: string) => {
        const auths = defaultAuth.user?.authorities ?? [];
        return auths.some((a) => a.toLowerCase() === auth.toLowerCase());
      },
      ...overrides,
    };

    vi.spyOn(authContext, 'useAuth').mockReturnValue(defaultAuth);
    return defaultAuth;
  }

  // 1. ProtectedRoute redirects unauthenticated users to /login with state
  it('redirects unauthenticated user to /login when accessing protected content', () => {
    setupAuthMock({ isAuthenticated: false, isInitializing: false });

    render(
      <MemoryRouter initialEntries={['/protected-area']}>
        <Routes>
          <Route path="/login" element={<div>Official Login Screen</div>} />
          <Route element={<ProtectedRoute />}>
            <Route path="/protected-area" element={<div>Top Secret Dashboard</div>} />
          </Route>
        </Routes>
      </MemoryRouter>,
    );

    expect(screen.getByText('Official Login Screen')).toBeInTheDocument();
    expect(screen.queryByText('Top Secret Dashboard')).toBeNull();
  });

  // 2. ProtectedRoute renders loading spinner without flashing protected content
  it('renders loading indicator while isInitializing is true, preventing content flash', () => {
    setupAuthMock({ isAuthenticated: false, isInitializing: true });

    render(
      <MemoryRouter initialEntries={['/protected-area']}>
        <Routes>
          <Route path="/login" element={<div>Official Login Screen</div>} />
          <Route element={<ProtectedRoute />}>
            <Route path="/protected-area" element={<div>Top Secret Dashboard</div>} />
          </Route>
        </Routes>
      </MemoryRouter>,
    );

    // Should NOT show login screen yet
    expect(screen.queryByText('Official Login Screen')).toBeNull();
    // Must NEVER show protected content before verification
    expect(screen.queryByText('Top Secret Dashboard')).toBeNull();
    // Loading indicator is shown
    expect(screen.getByRole('status', { name: /Loading…/i })).toBeInTheDocument();
  });

  // 3. ProtectedRoute renders child route when authenticated
  it('renders protected child component when user is authenticated', () => {
    setupAuthMock({
      isAuthenticated: true,
      isInitializing: false,
      user: {
        id: 1,
        username: 'authorized_user',
        authorities: ['ROLE_TRAINEE'],
      },
    });

    render(
      <MemoryRouter initialEntries={['/protected-area']}>
        <Routes>
          <Route element={<ProtectedRoute />}>
            <Route path="/protected-area" element={<div>Protected User Workspace</div>} />
          </Route>
        </Routes>
      </MemoryRouter>,
    );

    expect(screen.getByText('Protected User Workspace')).toBeInTheDocument();
  });

  // 4. RoleGuard redirects cross-role access to /forbidden (e.g. Trainee accessing Admin)
  it('redirects user with mismatched persona to /forbidden', () => {
    setupAuthMock({
      isAuthenticated: true,
      isInitializing: false,
      personas: ['TRAINEE'],
      user: {
        id: 2,
        username: 'learner_bob',
        authorities: ['ROLE_TRAINEE', 'trainee.read'],
      },
    });

    render(
      <MemoryRouter initialEntries={['/admin/dashboard']}>
        <Routes>
          <Route path="/forbidden" element={<ForbiddenPage />} />
          <Route element={<RoleGuard allowedPersonas={['ADMIN']} />}>
            <Route path="/admin/dashboard" element={<AdminDashboard />} />
          </Route>
        </Routes>
      </MemoryRouter>,
    );

    // Redirected to /forbidden
    expect(screen.getByRole('heading', { name: /403.*Access/i })).toBeInTheDocument();
    expect(screen.getByText(/You do not have the required role or authority/i)).toBeInTheDocument();
    expect(screen.queryByText(/Government Administrator Portal/i)).toBeNull();
  });

  // 5. Multi-role user can access both roles
  it('allows access to multi-role user possessing multiple persona authorities', () => {
    setupAuthMock({
      isAuthenticated: true,
      isInitializing: false,
      personas: ['ADMIN', 'TRAINING_PROVIDER'],
      user: {
        id: 3,
        username: 'admin_and_tp',
        authorities: [
          'ROLE_ADMIN',
          'ROLE_TRAINING_PROVIDER',
          'course.manage',
          'system.manage',
        ],
      },
    });

    // Test access to Admin route
    const { unmount } = render(
      <MemoryRouter initialEntries={['/admin/dashboard']}>
        <Routes>
          <Route path="/forbidden" element={<ForbiddenPage />} />
          <Route element={<RoleGuard allowedPersonas={['ADMIN']} />}>
            <Route path="/admin/dashboard" element={<AdminDashboard />} />
          </Route>
        </Routes>
      </MemoryRouter>,
    );

    expect(screen.getByRole('heading', { name: /Government Administrator Portal/i })).toBeInTheDocument();
    unmount();

    // Test access to Provider route with same multi-role user
    render(
      <MemoryRouter initialEntries={['/provider/dashboard']}>
        <Routes>
          <Route path="/forbidden" element={<ForbiddenPage />} />
          <Route element={<RoleGuard allowedPersonas={['TRAINING_PROVIDER']} />}>
            <Route path="/provider/dashboard" element={<ProviderDashboard />} />
          </Route>
        </Routes>
      </MemoryRouter>,
    );

    expect(screen.getByRole('heading', { name: /Training Provider Portal/i })).toBeInTheDocument();
  });

  // 6. Granular authority checks
  it('blocks access when required authority is absent', () => {
    setupAuthMock({
      isAuthenticated: true,
      isInitializing: false,
      personas: ['ADMIN'],
      user: {
        id: 4,
        username: 'limited_admin',
        authorities: ['ROLE_ADMIN', 'course.manage'],
      },
    });

    render(
      <MemoryRouter initialEntries={['/audit-logs']}>
        <Routes>
          <Route path="/forbidden" element={<ForbiddenPage />} />
          <Route element={<RoleGuard allowedAuthorities={['audit.read']} />}>
            <Route path="/audit-logs" element={<div>Audit Log Viewer</div>} />
          </Route>
        </Routes>
      </MemoryRouter>,
    );

    expect(screen.getByRole('heading', { name: /403.*Access/i })).toBeInTheDocument();
    expect(screen.queryByText('Audit Log Viewer')).toBeNull();
  });

  // 7. Verify all 5 persona dashboard stubs render clean government layout
  it('renders all 5 persona dashboard stubs with correct government portal headings', () => {
    const dashboards = [
      { comp: <AdminDashboard />, heading: /Government Administrator Portal/i },
      { comp: <ProviderDashboard />, heading: /Training Provider Portal/i },
      { comp: <AssessorDashboard />, heading: /Assessor & Verifier Portal/i },
      { comp: <EmployerDashboard />, heading: /Employer Portal/i },
      { comp: <TraineeDashboard />, heading: /Trainee Self-Service Portal/i },
    ];

    dashboards.forEach(({ comp, heading }) => {
      const { unmount } = render(<MemoryRouter>{comp}</MemoryRouter>);
      expect(screen.getByRole('heading', { name: heading })).toBeInTheDocument();
      unmount();
    });
  });
});
