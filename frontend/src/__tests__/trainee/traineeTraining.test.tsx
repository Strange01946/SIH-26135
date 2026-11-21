import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter, Outlet, Route, Routes } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { TraineeTrainingPage } from '@/pages/trainee/TraineeTrainingPage';
import * as traineeApi from '@/api/traineeApi';
import type { TrainingEnrollment, AttendanceRecord } from '@/types/trainee';
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

function renderTrainingWithContext(contextValue: any) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });

  const LayoutMock = () => <Outlet context={contextValue} />;

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/trainee/training']}>
        <Routes>
          <Route element={<LayoutMock />}>
            <Route path="/trainee/training" element={<TraineeTrainingPage />} />
          </Route>
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('TraineeTrainingPage Component', () => {
  const mockEnrollments: TrainingEnrollment[] = [
    {
      id: 101,
      enrollmentNumber: 'ENR-2026-101',
      traineeId: 42,
      courseId: 5,
      courseName: 'Solar PV Installer Technician',
      providerName: 'Skill India Training Ltd',
      centerName: 'NSDC Center Whitefield',
      batchCode: 'SOLAR-2026-B1',
      enrollmentDate: '2026-09-01',
      isCompletedFlag: false,
      enrollmentStatusName: 'In Training',
    },
    {
      id: 102,
      enrollmentNumber: 'ENR-2026-102',
      traineeId: 42,
      courseId: 8,
      courseName: 'Electrical Safety Standard',
      providerName: 'Skill India Training Ltd',
      centerName: 'NSDC Center Whitefield',
      batchCode: 'SAFETY-2026-B2',
      enrollmentDate: '2026-10-01',
      isCompletedFlag: true,
      enrollmentStatusName: 'Completed',
    },
  ];

  const mockAttendance: AttendanceRecord[] = [
    {
      id: 1,
      enrollmentId: 101,
      traineeId: 42,
      batchId: 10,
      batchCode: 'SOLAR-2026-B1',
      sessionDate: '2026-11-01',
      sessionSequence: 1,
      attendanceStatusCode: 'PRESENT',
      attendanceStatusName: 'Present',
      countsAsPresent: true,
      remarks: 'Theory Session 1',
    },
    {
      id: 2,
      enrollmentId: 101,
      traineeId: 42,
      batchId: 10,
      batchCode: 'SOLAR-2026-B1',
      sessionDate: '2026-11-02',
      sessionSequence: 2,
      attendanceStatusCode: 'PRESENT',
      attendanceStatusName: 'Present',
      countsAsPresent: true,
      remarks: 'Lab Session 1',
    },
    {
      id: 3,
      enrollmentId: 102,
      traineeId: 42,
      batchId: 20,
      batchCode: 'SAFETY-2026-B2',
      sessionDate: '2026-11-03',
      sessionSequence: 1,
      attendanceStatusCode: 'ABSENT',
      attendanceStatusName: 'Absent',
      countsAsPresent: false,
      remarks: 'Unexcused absence',
    },
  ];

  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('renders enrollments list with course name, center, provider, and status badges', async () => {
    const enrollmentsPaged: PagedResponse<TrainingEnrollment> = {
      content: mockEnrollments,
      page: 0,
      size: 20,
      totalElements: 2,
      totalPages: 1,
      first: true,
      last: true,
      hasNext: false,
      hasPrevious: false,
    };

    vi.spyOn(traineeApi, 'getTraineeEnrollments').mockResolvedValue(enrollmentsPaged);
    vi.spyOn(traineeApi, 'getTraineeAttendance').mockResolvedValue(mockAttendance);

    renderTrainingWithContext({ traineeId: 42, isUnlinked: false, isLoading: false });

    await waitFor(() => {
      expect(screen.getByText('Solar PV Installer Technician')).toBeInTheDocument();
    });
    expect(screen.getByText('Electrical Safety Standard')).toBeInTheDocument();
    expect(screen.getByText('In Training')).toBeInTheDocument();
    expect(screen.getByText('Completed')).toBeInTheDocument();
  });

  it('renders session-by-session attendance table and standing statistics', async () => {
    const enrollmentsPaged: PagedResponse<TrainingEnrollment> = {
      content: mockEnrollments,
      page: 0,
      size: 20,
      totalElements: 2,
      totalPages: 1,
      first: true,
      last: true,
      hasNext: false,
      hasPrevious: false,
    };

    vi.spyOn(traineeApi, 'getTraineeEnrollments').mockResolvedValue(enrollmentsPaged);
    vi.spyOn(traineeApi, 'getTraineeAttendance').mockResolvedValue(mockAttendance);

    renderTrainingWithContext({ traineeId: 42, isUnlinked: false, isLoading: false });

    await waitFor(() => {
      expect(screen.getByText('67%')).toBeInTheDocument();
    });

    expect(screen.getByText('Session-by-Session Attendance Log')).toBeInTheDocument();
    // Total logged sessions: 3, Present: 2, Absent: 1, Calculated Standing: 67%
    expect(screen.getByText('3')).toBeInTheDocument();
    expect(screen.getByText('2')).toBeInTheDocument();
    expect(screen.getByText('1')).toBeInTheDocument();
    expect(screen.getByText('⚠ Below 70% Benchmark')).toBeInTheDocument();
  });

  it('filters attendance rows when clicking batch filter and restores on clear filter', async () => {
    const enrollmentsPaged: PagedResponse<TrainingEnrollment> = {
      content: mockEnrollments,
      page: 0,
      size: 20,
      totalElements: 2,
      totalPages: 1,
      first: true,
      last: true,
      hasNext: false,
      hasPrevious: false,
    };

    vi.spyOn(traineeApi, 'getTraineeEnrollments').mockResolvedValue(enrollmentsPaged);
    vi.spyOn(traineeApi, 'getTraineeAttendance').mockResolvedValue(mockAttendance);

    renderTrainingWithContext({ traineeId: 42, isUnlinked: false, isLoading: false });

    await waitFor(() => {
      expect(screen.getByText('Solar PV Installer Technician')).toBeInTheDocument();
    });

    // Click filter for Batch 1 (enrollment 101)
    const filterButtons = screen.getAllByRole('button', { name: /Filter Attendance for this Batch/i });
    fireEvent.click(filterButtons[0]);

    // Now only 2 sessions for batch 101 should be present, standing is 100%
    await waitFor(() => {
      expect(screen.getByText('100%')).toBeInTheDocument();
    });
    expect(screen.getByText('✓ Eligible for Certification')).toBeInTheDocument();

    // Click Clear Batch Filter
    const clearButton = screen.getByRole('button', { name: /Clear Batch Filter/i });
    fireEvent.click(clearButton);

    await waitFor(() => {
      expect(screen.getByText('67%')).toBeInTheDocument();
    });
  });

  it('displays administrative notice when attendance service returns 403', async () => {
    vi.spyOn(traineeApi, 'getTraineeEnrollments').mockResolvedValueOnce(emptyEnrollmentPaged);
    vi.spyOn(traineeApi, 'getTraineeAttendance').mockRejectedValueOnce({
      status: 403,
      message: 'Forbidden',
    });

    renderTrainingWithContext({ traineeId: 42, isUnlinked: false, isLoading: false });

    await waitFor(() => {
      expect(screen.getByText(/Attendance records are restricted to verified biometric/i)).toBeInTheDocument();
    });
  });
});
