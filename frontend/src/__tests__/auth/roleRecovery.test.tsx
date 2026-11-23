import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { Header } from '@/components/layout/Header';
import { Sidebar } from '@/components/layout/Sidebar';
import * as authContext from '@/context/AuthContext';
import { getUserPersonas } from '@/api/types';

describe('Phase 30.6B — Role Recovery, Navigation Isolation & Context Switching', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('1. Authoritative Persona Isolation (Elimination of Cross-Role Permission Bleed)', () => {
    it('strict assessor isolation: presence of trainee.read does not grant TRAINEE persona to assessor', () => {
      const assessorAuthorities = [
        'ROLE_assessor',
        'trainee.read',
        'assessment.read',
        'assessment.submit',
        'certification.read',
        'certification.issue',
      ];
      const personas = getUserPersonas(assessorAuthorities);

      expect(personas).toEqual(['ASSESSOR']);
      expect(personas).not.toContain('TRAINEE');
      expect(personas).not.toContain('TRAINING_PROVIDER');
      expect(personas).not.toContain('ADMIN');
    });

    it('strict provider isolation: presence of trainee.read and assessment.manage does not leak ASSESSOR or TRAINEE to provider', () => {
      const providerAuthorities = [
        'ROLE_training_provider',
        'trainee.read',
        'course.manage',
        'enrollment.manage',
        'batch.manage',
        'assessment.manage',
        'placement.manage',
      ];
      const personas = getUserPersonas(providerAuthorities);

      expect(personas).toEqual(['TRAINING_PROVIDER']);
      expect(personas).not.toContain('ASSESSOR');
      expect(personas).not.toContain('TRAINEE');
      expect(personas).not.toContain('EMPLOYER');
    });

    it('strict trainee isolation: trainee role only receives TRAINEE persona', () => {
      const traineeAuthorities = ['ROLE_trainee', 'trainee.read'];
      const personas = getUserPersonas(traineeAuthorities);

      expect(personas).toEqual(['TRAINEE']);
      expect(personas).not.toContain('ADMIN');
      expect(personas).not.toContain('ASSESSOR');
      expect(personas).not.toContain('TRAINING_PROVIDER');
    });

    it('strict admin isolation: super_admin role with nationwide read permissions does not leak operational personas', () => {
      const adminAuthorities = [
        'ROLE_super_admin',
        'trainee.read',
        'assessment.manage',
        'placement.manage',
        'course.manage',
      ];
      const personas = getUserPersonas(adminAuthorities);

      expect(personas).toEqual(['ADMIN']);
      expect(personas).not.toContain('TRAINEE');
      expect(personas).not.toContain('ASSESSOR');
      expect(personas).not.toContain('TRAINING_PROVIDER');
    });

    it('legitimate multi-role user: receives multiple personas strictly from role codes', () => {
      const multiRoleAuthorities = [
        'ROLE_admin',
        'ROLE_assessor',
        'trainee.read',
        'assessment.manage',
      ];
      const personas = getUserPersonas(multiRoleAuthorities);

      expect(personas).toContain('ADMIN');
      expect(personas).toContain('ASSESSOR');
      expect(personas).not.toContain('TRAINEE');
      expect(personas).toHaveLength(2);
    });
  });

  describe('2. Sidebar Navigation Strict Persona Filtering', () => {
    function setupAuth(overrides: Partial<authContext.AuthContextType> = {}) {
      const defaultAuth: authContext.AuthContextType = {
        user: { id: 10, username: 'test_user', authorities: ['ROLE_assessor'] },
        isAuthenticated: true,
        isInitializing: false,
        connectionError: null,
        personas: ['ASSESSOR'],
        primaryPersona: 'ASSESSOR',
        activePersona: 'ASSESSOR',
        setActivePersona: vi.fn(),
        defaultLandingRoute: '/assessor/dashboard',
        login: vi.fn(),
        establishSession: vi.fn(),
        logout: vi.fn(),
        refreshProfile: vi.fn(),
        retrySession: vi.fn(),
        hasRole: vi.fn(),
        hasAnyRole: vi.fn(),
        hasAuthority: vi.fn(),
        ...overrides,
      };

      vi.spyOn(authContext, 'useAuth').mockReturnValue(defaultAuth);
      return defaultAuth;
    }

    it('assessor sees ONLY Assessor Console and zero trainee or placeholder links', () => {
      setupAuth({
        personas: ['ASSESSOR'],
        primaryPersona: 'ASSESSOR',
        activePersona: 'ASSESSOR',
      });

      render(
        <MemoryRouter>
          <Sidebar />
        </MemoryRouter>,
      );

      // Assessor items present
      expect(screen.getByText('Assessor Console')).toBeInTheDocument();

      // Zero Trainee portal items
      expect(screen.queryByText('Trainee Portal')).toBeNull();
      expect(screen.queryByText('My Training & Attendance')).toBeNull();
      expect(screen.queryByText('DPDP Privacy & Consent')).toBeNull();
      expect(screen.queryByText('Assessments & Certificates')).toBeNull();
      expect(screen.queryByText('Career Journey & Surveys')).toBeNull();

      // Zero Provider or Admin items
      expect(screen.queryByText('Provider Console')).toBeNull();
      expect(screen.queryByText('Training Centers')).toBeNull();
      expect(screen.queryByText('Admin Console')).toBeNull();
      expect(screen.queryByText('User Provisioning')).toBeNull();
    });

    it('trainee sees ONLY Trainee portal links and zero assessor, admin, or provider links', () => {
      setupAuth({
        user: { id: 20, username: 'trainee_user', authorities: ['ROLE_trainee'] },
        personas: ['TRAINEE'],
        primaryPersona: 'TRAINEE',
        activePersona: 'TRAINEE',
      });

      render(
        <MemoryRouter>
          <Sidebar />
        </MemoryRouter>,
      );

      // Trainee portal items present
      expect(screen.getByText('Trainee Portal')).toBeInTheDocument();
      expect(screen.getByText('My Training & Attendance')).toBeInTheDocument();
      expect(screen.getByText('DPDP Privacy & Consent')).toBeInTheDocument();
      expect(screen.getByText('Assessments & Certificates')).toBeInTheDocument();
      expect(screen.getByText('Career Journey & Surveys')).toBeInTheDocument();

      // Zero assessor or management items
      expect(screen.queryByText('Assessor Console')).toBeNull();
      expect(screen.queryByText('Provider Console')).toBeNull();
      expect(screen.queryByText('Admin Console')).toBeNull();
      expect(screen.queryByText('Batch Management')).toBeNull();
    });

    it('training provider sees ONLY Provider console links', () => {
      setupAuth({
        user: { id: 30, username: 'tp_user', authorities: ['ROLE_training_provider'] },
        personas: ['TRAINING_PROVIDER'],
        primaryPersona: 'TRAINING_PROVIDER',
        activePersona: 'TRAINING_PROVIDER',
      });

      render(
        <MemoryRouter>
          <Sidebar />
        </MemoryRouter>,
      );

      // Provider items present
      expect(screen.getByText('Provider Console')).toBeInTheDocument();
      expect(screen.getByText('Training Centers')).toBeInTheDocument();
      expect(screen.getByText('Approved Courses')).toBeInTheDocument();
      expect(screen.getByText('Batch Management')).toBeInTheDocument();
      expect(screen.getByText('Candidate Enrollment')).toBeInTheDocument();
      expect(screen.getByText('Bulk Ingestion')).toBeInTheDocument();
      expect(screen.getByText('Import Quarantine')).toBeInTheDocument();
      expect(screen.getByText('Quality Remediation')).toBeInTheDocument();
      expect(screen.getByText('Center Scorecards')).toBeInTheDocument();

      // Zero foreign items
      expect(screen.queryByText('Assessor Console')).toBeNull();
      expect(screen.queryByText('Trainee Portal')).toBeNull();
      expect(screen.queryByText('Admin Console')).toBeNull();
    });
  });

  describe('3. Multi-Role Context Switching in Header', () => {
    it('renders single role text badge when user has only one persona', () => {
      vi.spyOn(authContext, 'useAuth').mockReturnValue({
        user: { id: 10, username: 'assessor_user', authorities: ['ROLE_assessor'] },
        isAuthenticated: true,
        isInitializing: false,
        connectionError: null,
        personas: ['ASSESSOR'],
        primaryPersona: 'ASSESSOR',
        activePersona: 'ASSESSOR',
        setActivePersona: vi.fn(),
        defaultLandingRoute: '/assessor/dashboard',
        login: vi.fn(),
        establishSession: vi.fn(),
        logout: vi.fn(),
        refreshProfile: vi.fn(),
        retrySession: vi.fn(),
        hasRole: vi.fn(),
        hasAnyRole: vi.fn(),
        hasAuthority: vi.fn(),
      });

      render(
        <MemoryRouter>
          <Header />
        </MemoryRouter>,
      );

      expect(screen.getByText('assessor_user')).toBeInTheDocument();
      expect(screen.getByText('Assessor')).toBeInTheDocument();
      // Dropdown toggle not rendered for single role
      expect(screen.queryByRole('button', { name: /Switch operational role/i })).toBeNull();
    });

    it('renders role switcher dropdown and switches persona when user has multiple personas', async () => {
      const user = userEvent.setup();
      const mockSetActivePersona = vi.fn();

      vi.spyOn(authContext, 'useAuth').mockReturnValue({
        user: { id: 50, username: 'multi_role_user', authorities: ['ROLE_admin', 'ROLE_assessor'] },
        isAuthenticated: true,
        isInitializing: false,
        connectionError: null,
        personas: ['ADMIN', 'ASSESSOR'],
        primaryPersona: 'ADMIN',
        activePersona: 'ADMIN',
        setActivePersona: mockSetActivePersona,
        defaultLandingRoute: '/admin/dashboard',
        login: vi.fn(),
        establishSession: vi.fn(),
        logout: vi.fn(),
        refreshProfile: vi.fn(),
        retrySession: vi.fn(),
        hasRole: vi.fn(),
        hasAnyRole: vi.fn(),
        hasAuthority: vi.fn(),
      });

      render(
        <MemoryRouter initialEntries={['/admin/dashboard']}>
          <Routes>
            <Route path="/admin/dashboard" element={<Header />} />
            <Route path="/assessor/dashboard" element={<div>Assessor Destination</div>} />
          </Routes>
        </MemoryRouter>,
      );

      // Dropdown toggle exists
      const dropdownBtn = screen.getByRole('button', { name: /Switch operational role/i });
      expect(dropdownBtn).toBeInTheDocument();
      expect(dropdownBtn).toHaveTextContent('Administrator');

      // Click dropdown to open menu
      await user.click(dropdownBtn);

      expect(screen.getByText('Switch Role Context')).toBeInTheDocument();

      // Click Assessor option
      const assessorOption = screen.getByRole('button', { name: /Assessor/i });
      await user.click(assessorOption);

      expect(mockSetActivePersona).toHaveBeenCalledWith('ASSESSOR');
      expect(await screen.findByText('Assessor Destination')).toBeInTheDocument();
    });
  });
});
