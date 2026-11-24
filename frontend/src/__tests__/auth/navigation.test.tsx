import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { Header } from '@/components/layout/Header';
import { Sidebar } from '@/components/layout/Sidebar';
import * as authContext from '@/context/AuthContext';
import type { AuthenticatedUserResponse } from '@/api/types';

describe('Navigation, Layout & Multi-Persona Sidebar Filtering', () => {
  const mockLogout = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  function setupAuth(user: AuthenticatedUserResponse | null, overrides = {}) {
    const defaultAuth: authContext.AuthContextType = {
      user,
      isAuthenticated: Boolean(user),
      isInitializing: false,
      connectionError: null,
      personas: user?.authorities?.includes('ROLE_ADMIN') ? ['ADMIN'] : [],
      primaryPersona: user?.authorities?.includes('ROLE_ADMIN') ? 'ADMIN' : null,
      activePersona: user?.authorities?.includes('ROLE_ADMIN') ? 'ADMIN' : null,
      setActivePersona: vi.fn(),
      defaultLandingRoute: '/dashboard',
      login: vi.fn(),
      establishSession: vi.fn(),
      logout: mockLogout,
      refreshProfile: vi.fn(),
      retrySession: vi.fn(),
      hasRole: (r: string) => user?.authorities?.includes(r) ?? false,
      hasAnyRole: (roles: string[]) => roles.some((r) => user?.authorities?.includes(r)),
      hasAuthority: (a: string) => user?.authorities?.includes(a) ?? false,
      ...overrides,
    };

    vi.spyOn(authContext, 'useAuth').mockReturnValue(defaultAuth);
    return defaultAuth;
  }

  // 1. Mobile navigation ESC key handling
  it('closes mobile sidebar drawer when Escape key is pressed', () => {
    const handleClose = vi.fn();

    render(
      <MemoryRouter>
        <Sidebar isMobileOpen={true} onCloseMobile={handleClose} />
      </MemoryRouter>,
    );

    // Verify mobile navigation is present
    expect(screen.getByRole('navigation', { name: 'Mobile navigation' })).toBeInTheDocument();

    // Fire ESC key
    fireEvent.keyDown(window, { key: 'Escape', code: 'Escape' });

    expect(handleClose).toHaveBeenCalledTimes(1);
  });

  // 2. Header sign out button calls logout() and redirects to /login
  it('calls logout and redirects to /login when Sign Out button is clicked in Header', async () => {
    const user = userEvent.setup();
    setupAuth({
      id: 1,
      username: 'chief_admin',
      authorities: ['ROLE_ADMIN'],
    });

    render(
      <MemoryRouter initialEntries={['/admin/dashboard']}>
        <Routes>
          <Route
            path="/admin/dashboard"
            element={
              <div>
                <Header />
                <div>Dashboard Body</div>
              </div>
            }
          />
          <Route path="/login" element={<div>Signed Out Login Page</div>} />
        </Routes>
      </MemoryRouter>,
    );

    expect(screen.getByText('chief_admin')).toBeInTheDocument();

    const signOutBtn = screen.getByRole('button', { name: /Sign Out/i });
    await user.click(signOutBtn);

    expect(mockLogout).toHaveBeenCalledTimes(1);
    expect(await screen.findByText('Signed Out Login Page')).toBeInTheDocument();
  });

  // 3. Multi-Persona Sidebar Filtering
  describe('Persona Navigation Filtering', () => {
    it('renders admin operational links for Government Administrator persona', () => {
      render(
        <MemoryRouter>
          <Sidebar currentRole="ROLE_ADMIN" />
        </MemoryRouter>,
      );

      expect(screen.getByText('Admin Console')).toBeInTheDocument();
      expect(screen.getByText('Onboarding Requests')).toBeInTheDocument();
      expect(screen.getByText('User Provisioning')).toBeInTheDocument();

      // Must not leak other persona portals
      expect(screen.queryByText('Trainee Portal')).toBeNull();
      expect(screen.queryByText('Provider Console')).toBeNull();
      expect(screen.queryByText('Assessor Console')).toBeNull();
      expect(screen.queryByText('Employer Console')).toBeNull();
    });

    it('renders training provider links for Training Provider persona', () => {
      setupAuth({ id: 2, username: 'provider', authorities: ['ROLE_training_provider', 'center.manage'] });
      render(
        <MemoryRouter>
          <Sidebar currentRole="ROLE_TRAINING_PROVIDER" />
        </MemoryRouter>,
      );

      expect(screen.getByText('Provider Console')).toBeInTheDocument();
      expect(screen.getByText('Training Centers')).toBeInTheDocument();
      expect(screen.getByText('Approved Courses')).toBeInTheDocument();
      expect(screen.getByText('Batch Management')).toBeInTheDocument();
      expect(screen.getByText('Candidate Enrollment')).toBeInTheDocument();
      expect(screen.getByText('Bulk Ingestion')).toBeInTheDocument();
      expect(screen.getByText('Import Quarantine')).toBeInTheDocument();
      expect(screen.getByText('Quality Remediation')).toBeInTheDocument();
      expect(screen.getByText('Center Scorecards')).toBeInTheDocument();

      // Must not leak Admin, Assessor, or Trainee portals
      expect(screen.queryByText('Admin Console')).toBeNull();
      expect(screen.queryByText('Assessor Console')).toBeNull();
      expect(screen.queryByText('Trainee Portal')).toBeNull();
      expect(screen.queryByText('Employer Console')).toBeNull();
    });

    it('renders assessor evaluation links for Assessor persona without cross-role leakage', () => {
      render(
        <MemoryRouter>
          <Sidebar currentRole="ROLE_ASSESSOR" />
        </MemoryRouter>,
      );

      expect(screen.getByText('Assessor Console')).toBeInTheDocument();

      // Strict isolation: must NOT see Trainee Portal or Provider / Admin links
      expect(screen.queryByText('Trainee Portal')).toBeNull();
      expect(screen.queryByText('My Training & Attendance')).toBeNull();
      expect(screen.queryByText('DPDP Privacy & Consent')).toBeNull();
      expect(screen.queryByText('Batch Management')).toBeNull();
      expect(screen.queryByText('Admin Console')).toBeNull();
    });

    it('renders employer links for Employer persona', () => {
      render(
        <MemoryRouter>
          <Sidebar currentRole="ROLE_EMPLOYER" />
        </MemoryRouter>,
      );

      expect(screen.getByText('Employer Console')).toBeInTheDocument();

      // Must not see Trainees management or Batches
      expect(screen.queryByText('Trainee Portal')).toBeNull();
      expect(screen.queryByText('Batch Management')).toBeNull();
      expect(screen.queryByText('Admin Console')).toBeNull();
    });

    it('renders strictly learner links for Trainee persona', () => {
      render(
        <MemoryRouter>
          <Sidebar currentRole="ROLE_TRAINEE" />
        </MemoryRouter>,
      );

      expect(screen.getByText('Trainee Portal')).toBeInTheDocument();
      expect(screen.getByText('My Training & Attendance')).toBeInTheDocument();
      expect(screen.getByText('DPDP Privacy & Consent')).toBeInTheDocument();
      expect(screen.getByText('Assessments & Certificates')).toBeInTheDocument();
      expect(screen.getByText('Career Journey & Surveys')).toBeInTheDocument();

      // Must not see any operational administration links
      expect(screen.queryByText('Admin Console')).toBeNull();
      expect(screen.queryByText('Provider Console')).toBeNull();
      expect(screen.queryByText('Assessor Console')).toBeNull();
      expect(screen.queryByText('User Provisioning')).toBeNull();
    });
  });
});
