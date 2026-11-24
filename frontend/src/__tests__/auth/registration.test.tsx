import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { RegistrationPage } from '@/pages/auth/RegistrationPage';
import { AdminOnboardingPage } from '@/pages/admin/AdminOnboardingPage';
import { AdminUsersPage } from '@/pages/admin/AdminUsersPage';
import * as registrationApi from '@/api/registrationApi';
import * as adminApi from '@/api/adminApi';
import * as AuthContextModule from '@/context/AuthContext';

describe('Phase 30.3A — Registration & Onboarding Frontend Unit Tests', () => {
  beforeEach(() => {
    vi.clearAllMocks();

    vi.spyOn(registrationApi, 'getRegistrationReferenceData').mockResolvedValue({
      genders: [{ id: 1, code: 'FEMALE', name: 'Female' }, { id: 2, code: 'MALE', name: 'Male' }],
      states: [{ id: 101, code: 'DL', name: 'Delhi' }],
      districts: [{ id: 1001, code: '1001', name: 'New Delhi', parentId: 101 }],
      organizationTypes: [{ id: 5, code: 'TRAINING_PROVIDER', name: 'Training Provider' }],
      employmentStatuses: [{ id: 1, code: 'UNEMPLOYED', name: 'Unemployed' }],
    });
  });

  describe('RegistrationPage Component', () => {
    it('renders all four navigation tabs and default candidate form', async () => {
      render(
        <MemoryRouter>
          <RegistrationPage />
        </MemoryRouter>
      );

      expect(screen.getByText('Skill India Unified Portal')).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /^Candidate Registration$/i })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /^Training Provider$/i })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /^Employer \/ Industry$/i })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /^Track Status$/i })).toBeInTheDocument();

      expect(screen.getByText('Candidate / Trainee Self-Registration')).toBeInTheDocument();
      expect(screen.getByPlaceholderText('e.g. ramesh_kumar')).toBeInTheDocument();
      expect(screen.getByLabelText(/PIN Code/i)).toBeRequired();
    });

    it('switches tabs and displays Training Provider application form', async () => {
      render(
        <MemoryRouter>
          <RegistrationPage />
        </MemoryRouter>
      );

      const providerTab = screen.getByRole('button', { name: /Training Provider/i });
      await userEvent.click(providerTab);

      expect(screen.getByText('Training Provider Onboarding Application')).toBeInTheDocument();
      expect(screen.getByText(/Registration \/ NSDC Number \*/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/PIN Code/i)).toBeRequired();
    });

    it('validates password match on candidate submission', async () => {
      render(
        <MemoryRouter>
          <RegistrationPage />
        </MemoryRouter>
      );

      const usernameInput = screen.getByPlaceholderText('e.g. ramesh_kumar');
      const emailInput = screen.getByPlaceholderText('candidate@example.com');
      const passwordInput = screen.getByLabelText(/^Password \*/i);
      const confirmPasswordInput = screen.getByLabelText(/^Confirm Password \*/i);

      await userEvent.type(usernameInput, 'testcandidate');
      await userEvent.type(emailInput, 'test@example.com');
      await userEvent.type(passwordInput, 'Password123!');
      await userEvent.type(confirmPasswordInput, 'DifferentPassword!');

      const firstNameInput = screen.getByLabelText(/First Name \*/i);
      await userEvent.type(firstNameInput, 'Rahul');
      await userEvent.selectOptions(screen.getByLabelText(/Gender/), '1');
      await userEvent.click(screen.getByRole('checkbox', { name: /I agree to the use of my information/i }));
      await userEvent.selectOptions(screen.getByLabelText(/State or union territory/i), '101');
      await userEvent.selectOptions(screen.getByLabelText(/District/i), '1001');
      await userEvent.type(screen.getByLabelText(/PIN Code/i), '110001');

      const submitBtn = screen.getByRole('button', { name: /Complete Candidate Registration/i });
      fireEvent.click(submitBtn);

      await waitFor(() => {
        expect(screen.getByText('Passwords do not match')).toBeInTheDocument();
      });
    });

    it('submits training provider application and displays application confirmation card', async () => {
      vi.spyOn(registrationApi, 'registerTrainingProvider').mockResolvedValue({
        applicationId: 42,
        applicationNumber: 'APP-TP-999888',
        applicantType: 'TRAINING_PROVIDER',
        userId: 105,
        entityType: 'TRAINING_PROVIDER',
        entityId: 201,
        organizationName: 'Global Skill Institute',
        contactPerson: 'Director Sharma',
        contactEmail: 'director@globalskill.edu',
        status: 'PENDING',
        createdAt: new Date().toISOString(),
        message: 'Application submitted successfully.',
      });

      render(
        <MemoryRouter>
          <RegistrationPage />
        </MemoryRouter>
      );

      await userEvent.click(screen.getByRole('button', { name: /Training Provider/i }));

      // Fill required fields
      await userEvent.type(screen.getByLabelText(/Institution \/ Provider Name \*/i), 'Global Skill Institute');
      await userEvent.type(screen.getByLabelText(/Registration \/ NSDC Number \*/i), 'NSDC-2026-99');
      await userEvent.type(screen.getByLabelText(/Authorized Contact Person \*/i), 'Director Sharma');
      await userEvent.type(screen.getByLabelText(/Portal Login Username \*/i), 'director_sharma');
      await userEvent.type(screen.getByLabelText(/Account Email \*/i), 'director@globalskill.edu');

      const passwords = screen.getAllByLabelText(/Password \*/i);
      await userEvent.type(passwords[0], 'ValidPassword123!');
      await userEvent.type(screen.getByLabelText(/Confirm Password \*/i), 'ValidPassword123!');
      await userEvent.selectOptions(screen.getByLabelText(/State or union territory/i), '101');
      await userEvent.selectOptions(screen.getByLabelText(/District/i), '1001');
      await userEvent.type(screen.getByLabelText(/PIN Code/i), '110001');

      fireEvent.click(screen.getByRole('button', { name: /Submit Training Provider Application/i }));

      await waitFor(() => {
        expect(screen.getByText('Application Submitted Successfully')).toBeInTheDocument();
        expect(screen.getByText('APP-TP-999888')).toBeInTheDocument();
        expect(screen.getByText('PENDING APPROVAL')).toBeInTheDocument();
        expect(registrationApi.registerTrainingProvider).toHaveBeenCalledWith(expect.objectContaining({
          email: 'director@globalskill.edu', contactEmail: undefined, pincode: '110001',
        }));
      });
    });

    it('tracks application status and renders result', async () => {
      vi.spyOn(registrationApi, 'checkApplicationStatus').mockResolvedValue({
        applicationNumber: 'APP-EMP-777666',
        applicantType: 'EMPLOYER',
        organizationName: 'Infosys Skill Division',
        contactEmail: 'hr@infosys.com',
        status: 'APPROVED',
        submittedAt: '2026-11-20T10:00:00Z',
        reviewedAt: '2026-11-21T10:00:00Z',
      });

      render(
        <MemoryRouter>
          <RegistrationPage />
        </MemoryRouter>
      );

      await userEvent.click(screen.getByRole('button', { name: /Track Status/i }));

      const searchInput = screen.getByPlaceholderText(/Enter Application Number/i);
      await userEvent.type(searchInput, 'APP-EMP-777666');

      fireEvent.click(screen.getByRole('button', { name: /Check Status/i }));

      await waitFor(() => {
        expect(screen.getByText('APP-EMP-777666')).toBeInTheDocument();
        expect(screen.getByText('Infosys Skill Division')).toBeInTheDocument();
        expect(screen.getByText('APPROVED')).toBeInTheDocument();
        expect(screen.getByText(/Application has been approved!/i)).toBeInTheDocument();
      });
    });
  });

  describe('AdminOnboardingPage Component', () => {
    it('renders onboarding requests table and allows approval', async () => {
      vi.spyOn(AuthContextModule, 'useAuth').mockReturnValue({
        user: { id: 1, username: 'admin_user', authorities: ['ROLE_super_admin'] } as any,
        isAuthenticated: true,
        isInitializing: false,
        connectionError: null,
        personas: ['SUPER_ADMIN'],
        primaryPersona: 'SUPER_ADMIN',
        activePersona: 'SUPER_ADMIN',
        setActivePersona: vi.fn(),
        defaultLandingRoute: '/admin/dashboard',
        login: vi.fn(),
        logout: vi.fn(),
        establishSession: vi.fn(),
        refreshProfile: vi.fn(),
        retrySession: vi.fn(),
        hasRole: vi.fn((role) => role === 'ROLE_super_admin' || role === 'ROLE_ADMIN'),
        hasAnyRole: vi.fn(),
        hasAuthority: vi.fn((auth) => auth === 'user.manage'),
      });

      const mockApps: registrationApi.OnboardingApplicationResponse[] = [
        {
          applicationId: 10,
          applicationNumber: 'APP-TP-12345',
          applicantType: 'TRAINING_PROVIDER',
          userId: 55,
          entityType: 'TRAINING_PROVIDER',
          entityId: 101,
          organizationName: 'National Skill Academy',
          contactPerson: 'Dr. Verma',
          contactEmail: 'verma@nsa.in',
          status: 'PENDING',
          createdAt: '2026-11-20T10:00:00Z',
        },
      ];

      vi.spyOn(adminApi, 'listOnboardingApplications').mockResolvedValue(mockApps);
      const approveSpy = vi.spyOn(adminApi, 'approveOnboardingApplication').mockResolvedValue({
        ...mockApps[0],
        status: 'APPROVED',
      });

      vi.spyOn(window, 'confirm').mockReturnValue(true);

      render(
        <QueryClientProvider client={new QueryClient()}>
          <MemoryRouter><AdminOnboardingPage /></MemoryRouter>
        </QueryClientProvider>
      );

      await waitFor(() => {
        expect(screen.getByText('National Skill Academy')).toBeInTheDocument();
        expect(screen.getByText('APP-TP-12345')).toBeInTheDocument();
      });

      const approveBtn = screen.getByRole('button', { name: /Approve/i });
      fireEvent.click(approveBtn);

      await waitFor(() => {
        expect(approveSpy).toHaveBeenCalledWith(10);
      });
    });
  });

  describe('AdminUsersPage Component', () => {
    it('renders user directory and opens provisioning modal', async () => {
      const mockUsers: adminApi.AdminUserResponse[] = [
        {
          id: 1,
          username: 'admin_user',
          email: 'admin@example.com',
          userStatusId: 2,
          userStatusCode: 'ACTIVE',
          createdAt: '2026-11-01T10:00:00Z',
          updatedAt: '2026-11-01T10:00:00Z',
        },
      ];

      vi.spyOn(adminApi, 'listUsers').mockResolvedValue(mockUsers);
      vi.spyOn(AuthContextModule, 'useAuth').mockReturnValue({
        user: { id: 1, username: 'admin_user', email: 'admin@example.com', authorities: ['ROLE_super_admin'], userStatusId: 2 } as any,
        isAuthenticated: true,
        isInitializing: false,
        connectionError: null,
        personas: ['ADMIN'],
        primaryPersona: 'ADMIN',
        activePersona: 'ADMIN',
        setActivePersona: vi.fn(),
        defaultLandingRoute: '/admin/dashboard',
        login: vi.fn(),
        logout: vi.fn(),
        establishSession: vi.fn(),
        refreshProfile: vi.fn(),
        retrySession: vi.fn(),
        hasRole: vi.fn((role) => role === 'ROLE_super_admin' || role === 'super_admin'),
        hasAnyRole: vi.fn(),
        hasAuthority: vi.fn(),
      });

      render(
        <MemoryRouter>
          <AdminUsersPage />
        </MemoryRouter>
      );

      await waitFor(() => {
        expect(screen.getByText('admin_user')).toBeInTheDocument();
      });

      const provisionBtn = screen.getByRole('button', { name: /\+ Provision Privileged User/i });
      await userEvent.click(provisionBtn);

      expect(screen.getByText('Provision Privileged User Account')).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /^Assessor$/i })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /^Government Admin$/i })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /^Super Admin$/i })).toBeInTheDocument();
    });
  });
});
