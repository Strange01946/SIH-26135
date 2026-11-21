import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Outlet, Route, Routes } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { TraineeOverviewPage } from '@/pages/trainee/TraineeOverviewPage';
import * as traineeApi from '@/api/traineeApi';
import type { TraineeProfile, TrainingEnrollment, AttendanceRecord, TraineeOutcomeSummary } from '@/types/trainee';
import type { PagedResponse } from '@/api/types';

vi.mock('@/api/traineeApi');

const emptyEnrollmentPaged: PagedResponse<TrainingEnrollment> = {
  content: [],
  page: 0,
  size: 20,
  totalElements: 0,
  totalPages: 0,
  first: true,
  last: true,
  hasNext: false,
  hasPrevious: false,
};

function renderOverviewWithContext(contextValue: any) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });

  const LayoutMock = () => <Outlet context={contextValue} />;

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/trainee/dashboard']}>
        <Routes>
          <Route element={<LayoutMock />}>
            <Route path="/trainee/dashboard" element={<TraineeOverviewPage />} />
          </Route>
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('TraineeOverviewPage Component', () => {
  const mockTrainee: TraineeProfile = {
    id: 42,
    userId: 7,
    registrationNumber: 'TRN-2026-0042',
    firstName: 'Priya',
    lastName: 'Sharma',
    email: 'priya.sharma@example.gov.in',
    phone: '+919876543210',
    stateId: 1,
    districtId: 10,
    currentEmploymentStatusId: 1,
    profileStatusId: 3,
  };

  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('renders candidate profile demographic details and registration number', async () => {
    vi.spyOn(traineeApi, 'getTraineeEnrollments').mockResolvedValueOnce(emptyEnrollmentPaged);
    vi.spyOn(traineeApi, 'getTraineeAttendance').mockResolvedValueOnce([]);
    vi.spyOn(traineeApi, 'getTraineeConsents').mockResolvedValueOnce([]);
    vi.spyOn(traineeApi, 'getTraineeOutcomeSummary').mockResolvedValueOnce({
      traineeId: 42,
      snapshotIsEmployedFlag: false,
    });

    renderOverviewWithContext({
      trainee: mockTrainee,
      traineeId: 42,
      isUnlinked: false,
      isLoading: false,
    });

    expect(screen.getByText('TRN-2026-0042')).toBeInTheDocument();
    expect(screen.getByText('Priya Sharma')).toBeInTheDocument();
    expect(screen.getByText('priya.sharma@example.gov.in')).toBeInTheDocument();
    expect(screen.getByText('+919876543210')).toBeInTheDocument();
    expect(screen.getByText(/Active Registration/i)).toBeInTheDocument();
  });

  it('renders active enrollment information when enrollment records exist', async () => {
    const mockEnrollments: TrainingEnrollment[] = [
      {
        id: 101,
        enrollmentNumber: 'ENR-2026-101',
        traineeId: 42,
        courseId: 5,
        courseName: 'Solar PV Installer Technician',
        centerName: 'NSDC Skill Center Bengaluru',
        batchCode: 'SOLAR-2026-B1',
        isCompletedFlag: false,
      },
    ];

    vi.spyOn(traineeApi, 'getTraineeEnrollments').mockResolvedValueOnce({
      content: mockEnrollments,
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
      first: true,
      last: true,
      hasNext: false,
      hasPrevious: false,
    });
    vi.spyOn(traineeApi, 'getTraineeAttendance').mockResolvedValueOnce([]);
    vi.spyOn(traineeApi, 'getTraineeConsents').mockResolvedValueOnce([]);
    vi.spyOn(traineeApi, 'getTraineeOutcomeSummary').mockResolvedValueOnce({
      traineeId: 42,
    });

    renderOverviewWithContext({
      trainee: mockTrainee,
      traineeId: 42,
      isUnlinked: false,
      isLoading: false,
    });

    await waitFor(() => {
      expect(screen.getByText('Solar PV Installer Technician')).toBeInTheDocument();
    });
    expect(screen.getByText('NSDC Skill Center Bengaluru')).toBeInTheDocument();
    expect(screen.getByText(/SOLAR-2026-B1/)).toBeInTheDocument();
  });

  it('calculates factual attendance rate and session counts accurately', async () => {
    const mockAttendance: AttendanceRecord[] = [
      { id: 1, enrollmentId: 101, traineeId: 42, batchId: 5, sessionDate: '2026-11-01', attendanceStatusCode: 'PRESENT', attendanceStatusName: 'Present', countsAsPresent: true },
      { id: 2, enrollmentId: 101, traineeId: 42, batchId: 5, sessionDate: '2026-11-02', attendanceStatusCode: 'PRESENT', attendanceStatusName: 'Present', countsAsPresent: true },
      { id: 3, enrollmentId: 101, traineeId: 42, batchId: 5, sessionDate: '2026-11-03', attendanceStatusCode: 'PRESENT', attendanceStatusName: 'Present', countsAsPresent: true },
      { id: 4, enrollmentId: 101, traineeId: 42, batchId: 5, sessionDate: '2026-11-04', attendanceStatusCode: 'ABSENT', attendanceStatusName: 'Absent', countsAsPresent: false },
    ]; // 3 out of 4 = 75%

    vi.spyOn(traineeApi, 'getTraineeEnrollments').mockResolvedValueOnce(emptyEnrollmentPaged);
    vi.spyOn(traineeApi, 'getTraineeAttendance').mockResolvedValueOnce(mockAttendance);
    vi.spyOn(traineeApi, 'getTraineeConsents').mockResolvedValueOnce([]);
    vi.spyOn(traineeApi, 'getTraineeOutcomeSummary').mockResolvedValueOnce({ traineeId: 42 });

    renderOverviewWithContext({
      trainee: mockTrainee,
      traineeId: 42,
      isUnlinked: false,
      isLoading: false,
    });

    await waitFor(() => {
      expect(screen.getByText('75%')).toBeInTheDocument();
    });
    expect(screen.getByText('(3/4 sessions attended)')).toBeInTheDocument();
    expect(screen.getByText('Recorded Sessions')).toBeInTheDocument();
  });

  it('calculates active DPDP consents accurately using effective consent resolution', async () => {
    const mockConsents = [
      // DATA_PROCESSING: Granted
      {
        id: 1,
        traineeId: 42,
        consentTypeId: 1,
        consentTypeCode: 'DATA_PROCESSING',
        consentStatusCode: 'GRANTED',
        grantedAt: '2026-01-01T00:00:00Z',
        updatedAt: '2026-01-01T00:00:00Z',
      },
      // COMMUNICATION_SMS: Had an older grant, then REVOKED
      {
        id: 2,
        traineeId: 42,
        consentTypeId: 5,
        consentTypeCode: 'COMMUNICATION_SMS',
        consentStatusCode: 'GRANTED',
        grantedAt: '2026-01-01T00:00:00Z',
        updatedAt: '2026-01-01T00:00:00Z',
      },
      {
        id: 3,
        traineeId: 42,
        consentTypeId: 5,
        consentTypeCode: 'COMMUNICATION_SMS',
        consentStatusCode: 'REVOKED',
        revokedAt: '2026-06-01T00:00:00Z',
        updatedAt: '2026-06-01T00:00:00Z',
      },
    ];

    vi.spyOn(traineeApi, 'getTraineeEnrollments').mockResolvedValueOnce(emptyEnrollmentPaged);
    vi.spyOn(traineeApi, 'getTraineeAttendance').mockResolvedValueOnce([]);
    vi.spyOn(traineeApi, 'getTraineeConsents').mockResolvedValueOnce(mockConsents as any);
    vi.spyOn(traineeApi, 'getTraineeOutcomeSummary').mockResolvedValueOnce({ traineeId: 42 });

    renderOverviewWithContext({
      trainee: mockTrainee,
      traineeId: 42,
      isUnlinked: false,
      isLoading: false,
    });

    await waitFor(() => {
      // Only 1 is active (DATA_PROCESSING), because COMMUNICATION_SMS was revoked
      expect(screen.getByText('1')).toBeInTheDocument();
      expect(screen.getByText('Active Consents')).toBeInTheDocument();
    });
  });

  it('renders longitudinal placement outcome when candidate is employed', async () => {
    const mockOutcome: TraineeOutcomeSummary = {
      traineeId: 42,
      snapshotIsEmployedFlag: true,
      lastVerifiedEmployerName: 'Schneider Electric India',
      monthlySalary: 32000,
      lastPlacementDate: '2026-10-15',
    };

    vi.spyOn(traineeApi, 'getTraineeEnrollments').mockResolvedValueOnce(emptyEnrollmentPaged);
    vi.spyOn(traineeApi, 'getTraineeAttendance').mockResolvedValueOnce([]);
    vi.spyOn(traineeApi, 'getTraineeConsents').mockResolvedValueOnce([]);
    vi.spyOn(traineeApi, 'getTraineeOutcomeSummary').mockResolvedValueOnce(mockOutcome);

    renderOverviewWithContext({
      trainee: mockTrainee,
      traineeId: 42,
      isUnlinked: false,
      isLoading: false,
    });

    await waitFor(() => {
      expect(screen.getByText('Schneider Electric India')).toBeInTheDocument();
    });
    expect(screen.getByText(/32,000/)).toBeInTheDocument();
    expect(screen.getByText(/Employed/i)).toBeInTheDocument();
  });

  it('renders unlinked empty state notice when candidate is not mapped to Trainee ID', () => {
    renderOverviewWithContext({
      trainee: null,
      traineeId: undefined,
      isUnlinked: true,
      isLoading: false,
    });

    expect(screen.getByText(/Candidate Profile Not Linked/i)).toBeInTheDocument();
  });

  it('handles HTTP 403 authorization restrictions gracefully without application crashes', async () => {
    vi.spyOn(traineeApi, 'getTraineeEnrollments').mockRejectedValueOnce({
      status: 403,
      message: 'Access Denied',
    });
    vi.spyOn(traineeApi, 'getTraineeAttendance').mockRejectedValueOnce({
      status: 403,
      message: 'Access Denied',
    });
    vi.spyOn(traineeApi, 'getTraineeConsents').mockResolvedValueOnce([]);
    vi.spyOn(traineeApi, 'getTraineeOutcomeSummary').mockResolvedValueOnce({ traineeId: 42 });

    renderOverviewWithContext({
      trainee: mockTrainee,
      traineeId: 42,
      isUnlinked: false,
      isLoading: false,
    });

    await waitFor(() => {
      expect(screen.getByText(/Administrative live-sync notice/i)).toBeInTheDocument();
    });
    expect(screen.getByText(/Attendance view restricted/i)).toBeInTheDocument();
  });
});
