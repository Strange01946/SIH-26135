import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Sidebar } from '@/components/layout/Sidebar';
import { ProviderOverviewPage } from '@/pages/provider/ProviderOverviewPage';
import { ProviderCentersPage } from '@/pages/provider/ProviderCentersPage';
import { ProviderCoursesPage } from '@/pages/provider/ProviderCoursesPage';
import { ProviderBatchesPage } from '@/pages/provider/ProviderBatchesPage';
import { ProviderEnrollmentPage } from '@/pages/provider/ProviderEnrollmentPage';
import { ProviderBulkImportPage } from '@/pages/provider/ProviderBulkImportPage';
import { ProviderQuarantinePage } from '@/pages/provider/ProviderQuarantinePage';
import * as authContext from '@/context/AuthContext';
import * as providerApi from '@/api/providerPortalApi';
import type { AuthenticatedUserResponse } from '@/api/types';

vi.mock('@/api/providerPortalApi');

describe('Training Provider Portal Frontend Components', () => {
  const mockUser: AuthenticatedUserResponse = {
    id: 100,
    username: 'provider_admin',
    email: 'provider@example.com',
    authorities: ['ROLE_TRAINING_PROVIDER'],
    trainingProviderId: 5,
    employerId: null,
    stateId: 1,
    districtId: 2,
  };

  function setupAuth(user: AuthenticatedUserResponse | null = mockUser) {
    const auth: authContext.AuthContextType = {
      user,
      isAuthenticated: Boolean(user),
      isInitializing: false,
      connectionError: null,
      personas: user?.authorities?.includes('ROLE_TRAINING_PROVIDER') ? ['TRAINING_PROVIDER'] : [],
      primaryPersona: user?.authorities?.includes('ROLE_TRAINING_PROVIDER') ? 'TRAINING_PROVIDER' : null,
      activePersona: user?.authorities?.includes('ROLE_TRAINING_PROVIDER') ? 'TRAINING_PROVIDER' : null,
      setActivePersona: vi.fn(),
      defaultLandingRoute: '/provider/dashboard',
      login: vi.fn(),
      establishSession: vi.fn(),
      logout: vi.fn(),
      refreshProfile: vi.fn(),
      retrySession: vi.fn(),
      hasRole: (r: string) => user?.authorities?.includes(r) ?? false,
      hasAnyRole: (roles: string[]) => roles.some((r) => user?.authorities?.includes(r)),
      hasAuthority: (a: string) => user?.authorities?.includes(a) ?? false,
    };
    vi.spyOn(authContext, 'useAuth').mockReturnValue(auth);
    return auth;
  }

  function renderWithProviders(ui: React.ReactElement, initialRoute = '/') {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
      },
    });

    return render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={[initialRoute]}>
          {ui}
        </MemoryRouter>
      </QueryClientProvider>,
    );
  }

  beforeEach(() => {
    vi.clearAllMocks();
    setupAuth();
  });

  describe('Sidebar Navigation for Training Provider', () => {
    it('renders all Phase 30.4 provider navigation links', () => {
      renderWithProviders(<Sidebar />);

      expect(screen.getByText('Provider Console')).toBeInTheDocument();
      expect(screen.getByText('Training Centers')).toBeInTheDocument();
      expect(screen.getByText('Approved Courses')).toBeInTheDocument();
      expect(screen.getByText('Batch Management')).toBeInTheDocument();
      expect(screen.getByText('Candidate Enrollment')).toBeInTheDocument();
      expect(screen.getByText('Bulk Ingestion')).toBeInTheDocument();
      expect(screen.getByText('Import Quarantine')).toBeInTheDocument();
    });
  });

  describe('ProviderOverviewPage', () => {
    it('renders provider metric cards and batches table', async () => {
      vi.mocked(providerApi.getTrainingCenters).mockResolvedValue({
        content: [{ id: 1, centerName: 'Delhi Hub', centerCode: 'TC-01', providerId: 5, stateId: 1, districtId: 2 }],
        totalElements: 3,
        page: 0,
        size: 5,
        totalPages: 1,
        first: true,
        last: true,
        hasNext: false,
        hasPrevious: false,
      });

      vi.mocked(providerApi.getCourses).mockResolvedValue({
        content: [{ id: 1, courseName: 'Solar Technician', courseCode: 'SOL-01', durationHours: 120, sectorName: 'Green Energy' }],
        totalElements: 5,
        page: 0,
        size: 5,
        totalPages: 1,
        first: true,
        last: true,
        hasNext: false,
        hasPrevious: false,
      });

      vi.mocked(providerApi.getTrainingBatches).mockResolvedValue({
        content: [
          {
            id: 10,
            batchCode: 'BATCH-2026-DEL-01',
            centerId: 1,
            centerName: 'Delhi Hub',
            courseId: 1,
            courseName: 'Solar Technician',
            providerId: 5,
            capacity: 30,
            enrolledCount: 28,
            availableSeats: 2,
            batchStatusId: 1,
            startDate: '2026-06-01',
            endDate: '2026-08-31',
          },
        ],
        totalElements: 1,
        page: 0,
        size: 5,
        totalPages: 1,
        first: true,
        last: true,
        hasNext: false,
        hasPrevious: false,
      });

      vi.mocked(providerApi.getTrainingEnrollments).mockResolvedValue({
        content: [],
        totalElements: 28,
        page: 0,
        size: 5,
        totalPages: 1,
        first: true,
        last: true,
        hasNext: false,
        hasPrevious: false,
      });

      renderWithProviders(<ProviderOverviewPage />);

      expect(screen.getByText('Training Provider Portal')).toBeInTheDocument();
      await waitFor(() => {
        expect(screen.getByText('BATCH-2026-DEL-01')).toBeInTheDocument();
        expect(screen.getByText('2 left')).toBeInTheDocument();
      });
    });
  });

  describe('ProviderCentersPage', () => {
    it('renders affiliated training centers list', async () => {
      vi.mocked(providerApi.getTrainingCenters).mockResolvedValue({
        content: [
          {
            id: 1,
            centerName: 'Skill Center Alpha',
            centerCode: 'TC-ALP-01',
            providerId: 5,
            stateId: 1,
            districtId: 2,
          },
        ],
        totalElements: 1,
        page: 0,
        size: 10,
        totalPages: 1,
        first: true,
        last: true,
        hasNext: false,
        hasPrevious: false,
      });

      renderWithProviders(<ProviderCentersPage />);

      expect(screen.getByText('Training Centers Directory')).toBeInTheDocument();
      await waitFor(() => {
        expect(screen.getByText('Skill Center Alpha')).toBeInTheDocument();
        expect(screen.getByText('TC-ALP-01')).toBeInTheDocument();
      });
    });
  });

  describe('ProviderCoursesPage', () => {
    it('renders approved courses catalog', async () => {
      vi.mocked(providerApi.getCourses).mockResolvedValue({
        content: [
          {
            id: 101,
            courseName: 'Electric Vehicle Maintenance Specialist',
            courseCode: 'CRS-EV-01',
            durationHours: 160,
            sectorName: 'Automotive',
          },
        ],
        totalElements: 1,
        page: 0,
        size: 10,
        totalPages: 1,
        first: true,
        last: true,
        hasNext: false,
        hasPrevious: false,
      });

      renderWithProviders(<ProviderCoursesPage />);

      expect(screen.getByText('Approved Courses Directory')).toBeInTheDocument();
      await waitFor(() => {
        expect(screen.getByText('Electric Vehicle Maintenance Specialist')).toBeInTheDocument();
        expect(screen.getByText('CRS-EV-01')).toBeInTheDocument();
        expect(screen.getByText('Automotive')).toBeInTheDocument();
      });
    });
  });

  describe('ProviderBatchesPage', () => {
    it('displays batches and opens create batch modal', async () => {
      vi.mocked(providerApi.getTrainingBatches).mockResolvedValue({
        content: [
          {
            id: 20,
            batchCode: 'BATCH-FULL-01',
            centerId: 1,
            courseId: 101,
            providerId: 5,
            capacity: 25,
            enrolledCount: 25,
            availableSeats: 0,
            batchStatusId: 1,
            startDate: '2026-06-01',
            endDate: '2026-08-31',
          },
        ],
        totalElements: 1,
        page: 0,
        size: 10,
        totalPages: 1,
        first: true,
        last: true,
        hasNext: false,
        hasPrevious: false,
      });

      vi.mocked(providerApi.getTrainingCenters).mockResolvedValue({
        content: [{ id: 1, centerName: 'Main Campus', centerCode: 'TC-01', providerId: 5, stateId: 1, districtId: 2 }],
        totalElements: 1,
        page: 0,
        size: 100,
        totalPages: 1,
        first: true,
        last: true,
        hasNext: false,
        hasPrevious: false,
      });

      vi.mocked(providerApi.getCourses).mockResolvedValue({
        content: [{ id: 101, courseName: 'EV Tech', courseCode: 'EV-01', durationHours: 100 }],
        totalElements: 1,
        page: 0,
        size: 100,
        totalPages: 1,
        first: true,
        last: true,
        hasNext: false,
        hasPrevious: false,
      });

      renderWithProviders(<ProviderBatchesPage />);

      expect(screen.getByText('Training Batches Workbench')).toBeInTheDocument();
      await waitFor(() => {
        expect(screen.getByText('BATCH-FULL-01')).toBeInTheDocument();
        expect(screen.getByText('FULL')).toBeInTheDocument();
      });

      // Click Create Batch button
      const createBtn = screen.getByRole('button', { name: /Create New Batch/i });
      fireEvent.click(createBtn);

      expect(screen.getByText('Create Training Batch')).toBeInTheDocument();
      expect(screen.getByText('Authorized Training Center *')).toBeInTheDocument();
    });
  });

  describe('ProviderEnrollmentPage', () => {
    it('searches trainee by registration number and displays candidate card with DPDP badge', async () => {
      vi.mocked(providerApi.getTrainingBatches).mockResolvedValue({
        content: [
          {
            id: 10,
            batchCode: 'BATCH-OPEN-01',
            centerId: 1,
            courseId: 101,
            providerId: 5,
            capacity: 30,
            enrolledCount: 10,
            availableSeats: 20,
            batchStatusId: 1,
            startDate: '2026-06-01',
            endDate: '2026-08-31',
          },
        ],
        totalElements: 1,
        page: 0,
        size: 50,
        totalPages: 1,
        first: true,
        last: true,
        hasNext: false,
        hasPrevious: false,
      });

      vi.mocked(providerApi.getTrainingEnrollments).mockResolvedValue({
        content: [],
        totalElements: 0,
        page: 0,
        size: 10,
        totalPages: 0,
        first: true,
        last: true,
        hasNext: false,
        hasPrevious: false,
      });

      vi.mocked(providerApi.searchTraineeByRegNumber).mockResolvedValue({
        id: 77,
        userId: 12,
        registrationNumber: 'TRN-2026-9999',
        firstName: 'Rahul',
        lastName: 'Verma',
        email: 'rahul.verma@example.gov.in',
        phone: '+919876543210',
        stateId: 1,
        districtId: 2,
        currentEmploymentStatusId: 1,
        profileStatusId: 2,
      });

      renderWithProviders(<ProviderEnrollmentPage />);

      expect(screen.getByText('Candidate Trainee Enrollment Workbench')).toBeInTheDocument();

      const searchInput = screen.getByPlaceholderText(/TRN-2026-0001/i);
      fireEvent.change(searchInput, { target: { value: 'TRN-2026-9999' } });
      fireEvent.click(screen.getByRole('button', { name: /Find Trainee/i }));

      await waitFor(() => {
        expect(screen.getByText('VERIFIED PROFILE')).toBeInTheDocument();
        expect(screen.getByText(/TRN-2026-9999/)).toBeInTheDocument();
      });
    });
  });

  describe('ProviderBulkImportPage & ProviderQuarantinePage', () => {
    it('renders bulk import drag and drop upload zone supporting CSV and JSON', () => {
      renderWithProviders(<ProviderBulkImportPage />);

      expect(screen.getByText('Multipart Bulk Ingestion Workbench')).toBeInTheDocument();
      expect(screen.getByText(/1\. Select & Stage Dataset File/i)).toBeInTheDocument();
      expect(screen.getByText(/Drag and drop your dataset \(CSV or JSON\) here/i)).toBeInTheDocument();
    });

    it('renders quarantine inspection grid and handles empty state', async () => {
      renderWithProviders(<ProviderQuarantinePage />);

      expect(screen.getByText('Import Quarantine Inspection & Error Export')).toBeInTheDocument();
      expect(screen.getByText('Load Records')).toBeInTheDocument();
      expect(screen.getByText(/No Import Batch Specified/i)).toBeInTheDocument();
    });

    it('triggers authenticated CSV export and manages object URL lifecycle', async () => {
      const mockBlob = new Blob(['source_row,error_code\n1,TEST_ERR'], { type: 'text/csv' });
      vi.mocked(providerApi.downloadQuarantineExport).mockResolvedValue(mockBlob);
      vi.mocked(providerApi.getImportBatchStatus).mockResolvedValue({
        importBatchId: 99,
        batchCode: 'IMP-TEST-99',
        entityType: 'Trainee',
        status: 'PARTIALLY_COMPLETED',
        rowCount: 10,
        successCount: 8,
        failureCount: 2,
        duplicateCount: 0,
        skippedCount: 0,
        isTerminal: true,
      });
      vi.mocked(providerApi.getQuarantinedRecords).mockResolvedValue({
        content: [
          {
            id: 1,
            importBatchId: 99,
            sourceRowNumber: 1,
            entityType: 'Trainee',
            importRecordStatusCode: 'FAILED',
            errorCode: 'TEST_ERR',
            errorMessage: 'Sample failure',
            createdAt: '2026-11-22T10:00:00',
          },
        ],
        totalElements: 1,
        page: 0,
        size: 15,
        totalPages: 1,
        first: true,
        last: true,
        hasNext: false,
        hasPrevious: false,
      });

      const createObjectURLSpy = vi.spyOn(window.URL, 'createObjectURL').mockReturnValue('blob:mock-url');
      const revokeObjectURLSpy = vi.spyOn(window.URL, 'revokeObjectURL').mockImplementation(() => {});

      renderWithProviders(<ProviderQuarantinePage />, '/provider/quarantine?batchId=99');

      await waitFor(() => {
        expect(screen.getByText('Export Error CSV')).toBeInTheDocument();
      });

      const exportBtn = screen.getByRole('button', { name: /Export Error CSV/i });
      fireEvent.click(exportBtn);

      await waitFor(() => {
        expect(providerApi.downloadQuarantineExport).toHaveBeenCalledWith(99, true);
        expect(createObjectURLSpy).toHaveBeenCalledWith(mockBlob);
        expect(revokeObjectURLSpy).toHaveBeenCalledWith('blob:mock-url');
        expect(screen.getByText(/Quarantine CSV exported successfully/i)).toBeInTheDocument();
      });

      createObjectURLSpy.mockRestore();
      revokeObjectURLSpy.mockRestore();
    });
  });
});
