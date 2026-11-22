import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AssessorScoringPage } from '@/pages/assessor/AssessorScoringPage';
import * as assessorApi from '@/api/assessorApi';
import type { AssessorRosterResponse } from '@/types/assessor';

vi.mock('@/api/assessorApi');
vi.mock('@/context/AuthContext', () => ({
  useAuth: () => ({
    user: {
      id: 10,
      username: 'assessor_tester',
      fullName: 'Vikram Assessor',
      email: 'assessor@ncvet.gov.in',
      roles: ['ROLE_assessor'],
    },
    isAuthenticated: true,
  }),
}));

describe('AssessorScoringPage Component', () => {
  const mockRoster: AssessorRosterResponse = {
    assessment: {
      id: 201,
      assessmentId: 201,
      assessmentCode: 'ASM-TEST-201',
      assessmentName: 'Practical Wiring Certification Assessment',
      courseName: 'Solar PV Installer Technician',
      batchCode: 'BAT-2026-DEL-01',
      centerName: 'Delhi Central Skill Center',
      assessmentDate: '2026-11-24',
      maximumScore: 100,
      passScore: 50,
      theoryMaxScore: 30,
      practicalMaxScore: 50,
      vivaMaxScore: 20,
      theoryMax: 30,
      practicalMax: 50,
      vivaMax: 20,
      totalCandidates: 2,
      candidateCount: 2,
      scoredCandidates: 0,
      scoredCount: 0,
      isFinalized: false,
    },
    candidates: [
      {
        traineeId: 101,
        enrollmentId: 1001,
        registrationNumber: 'REG-TRN-101',
        traineeCode: 'REG-TRN-101',
        fullName: 'Rahul Sharma',
        gender: 'Male',
        attemptNumber: 1,
        appeared: true,
        theoryScore: null,
        practicalScore: null,
        vivaScore: null,
        totalScore: null,
        scorePercentage: null,
        assessmentOutcomeCode: 'PENDING',
        outcome: 'PENDING',
        isFinalized: false,
      },
      {
        traineeId: 102,
        enrollmentId: 1002,
        registrationNumber: 'REG-TRN-102',
        traineeCode: 'REG-TRN-102',
        fullName: 'Pooja Verma',
        gender: 'Female',
        attemptNumber: 1,
        appeared: true,
        theoryScore: null,
        practicalScore: null,
        vivaScore: null,
        totalScore: null,
        scorePercentage: null,
        assessmentOutcomeCode: 'PENDING',
        outcome: 'PENDING',
        isFinalized: false,
      },
    ],
  };

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(assessorApi.getAssessmentRoster).mockResolvedValue(mockRoster);
    vi.mocked(assessorApi.saveDraftScores).mockResolvedValue({
      assessmentId: 201,
      isFinalized: false,
      totalProcessed: 2,
      passedCount: 0,
      failedCount: 0,
      absentCount: 0,
      certificatesIssued: 0,
      results: [],
    });
    vi.mocked(assessorApi.finalizeAssessment).mockResolvedValue({
      assessmentId: 201,
      isFinalized: true,
      totalProcessed: 2,
      passedCount: 2,
      failedCount: 0,
      absentCount: 0,
      certificatesIssued: 2,
      results: [],
    });
  });

  function renderScoringPage() {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    return render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={['/assessor/assessments/201']}>
          <Routes>
            <Route path="/assessor/assessments/:assessmentId" element={<AssessorScoringPage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );
  }

  it('renders assessment metadata, component scoring rules, and candidate roster', async () => {
    renderScoringPage();

    await waitFor(() => {
      expect(screen.getByText('ASM-TEST-201')).toBeInTheDocument();
      expect(screen.getByText('Practical Wiring Certification Assessment')).toBeInTheDocument();
      expect(screen.getByText(/Solar PV Installer/)).toBeInTheDocument();
      expect(screen.getByText('Rahul Sharma')).toBeInTheDocument();
      expect(screen.getByText('Pooja Verma')).toBeInTheDocument();
    });

    expect(screen.getByText('Save Draft')).toBeInTheDocument();
    expect(screen.getByText('Finalize & Issue Certificates')).toBeInTheDocument();
  });

  it('calculates total score, percentage, and updates outcome preview in real-time', async () => {
    renderScoringPage();

    await waitFor(() => {
      expect(screen.getByText('Rahul Sharma')).toBeInTheDocument();
    });

    const theoryInputs = screen.getAllByPlaceholderText('0.00');
    // First candidate's inputs
    const c1Theory = theoryInputs[0];
    const c1Practical = theoryInputs[1];
    const c1Viva = theoryInputs[2];

    fireEvent.change(c1Theory, { target: { value: '25.00' } });
    fireEvent.change(c1Practical, { target: { value: '45.00' } });
    fireEvent.change(c1Viva, { target: { value: '15.00' } });

    // Total should be 85.00, percentage 85.0%
    await waitFor(() => {
      expect(screen.getByText('85.00')).toBeInTheDocument();
      expect(screen.getByText('85.0%')).toBeInTheDocument();
      expect(screen.getByText('PASS')).toBeInTheDocument();
    });
  });

  it('flags out-of-bounds scores and shows inline validation error', async () => {
    renderScoringPage();

    await waitFor(() => {
      expect(screen.getByText('Rahul Sharma')).toBeInTheDocument();
    });

    const theoryInputs = screen.getAllByPlaceholderText('0.00');
    const c1Theory = theoryInputs[0];

    // Theory max is 30, enter 35
    fireEvent.change(c1Theory, { target: { value: '35.00' } });

    await waitFor(() => {
      expect(screen.getByText(/0 - 30 max/)).toBeInTheDocument();
    });
  });

  it('toggles attendance and marks candidate as Absent', async () => {
    renderScoringPage();

    await waitFor(() => {
      expect(screen.getByText('Rahul Sharma')).toBeInTheDocument();
    });

    const attendanceButtons = screen.getAllByRole('button', { name: /Present/i });
    fireEvent.click(attendanceButtons[0]);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Absent/i })).toBeInTheDocument();
      expect(screen.getByText('ABSENT')).toBeInTheDocument();
    });
  });

  it('saves draft scores when Save Draft is clicked', async () => {
    renderScoringPage();

    await waitFor(() => {
      expect(screen.getByText('Rahul Sharma')).toBeInTheDocument();
    });

    const saveDraftBtn = screen.getByRole('button', { name: /Save Draft/i });
    fireEvent.click(saveDraftBtn);

    await waitFor(() => {
      expect(assessorApi.saveDraftScores).toHaveBeenCalledWith(201, expect.any(Array));
    });
  });

  it('opens confirmation modal and finalizes assessment', async () => {
    renderScoringPage();

    await waitFor(() => {
      expect(screen.getByText('Rahul Sharma')).toBeInTheDocument();
    });

    // Enter full scores for candidate 1
    const inputs = screen.getAllByPlaceholderText('0.00');
    fireEvent.change(inputs[0], { target: { value: '25' } });
    fireEvent.change(inputs[1], { target: { value: '45' } });
    fireEvent.change(inputs[2], { target: { value: '15' } });

    // Enter full scores for candidate 2
    fireEvent.change(inputs[3], { target: { value: '20' } });
    fireEvent.change(inputs[4], { target: { value: '40' } });
    fireEvent.change(inputs[5], { target: { value: '12' } });

    const finalizeBtn = screen.getByRole('button', { name: /Finalize & Issue Certificates/i });
    fireEvent.click(finalizeBtn);

    // Modal should appear
    await waitFor(() => {
      expect(screen.getByText('Confirm & Finalize')).toBeInTheDocument();
    });

    // Confirm
    const confirmBtn = screen.getByRole('button', { name: /Confirm & Finalize/i });
    fireEvent.click(confirmBtn);

    await waitFor(() => {
      expect(assessorApi.finalizeAssessment).toHaveBeenCalledWith(201, expect.any(Array));
    });
  });
});
