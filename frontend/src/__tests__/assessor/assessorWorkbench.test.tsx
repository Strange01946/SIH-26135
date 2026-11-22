import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AssessorWorkbenchPage } from '@/pages/assessor/AssessorWorkbenchPage';
import * as assessorApi from '@/api/assessorApi';
import type { AssessorAssessmentSummary } from '@/types/assessor';

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

describe('AssessorWorkbenchPage Component', () => {
  const mockAssessments: AssessorAssessmentSummary[] = [
    {
      id: 101,
      assessmentId: 101,
      assessmentCode: 'ASM-ELEC-001',
      assessmentName: 'Field Electrician Certification Exam',
      courseName: 'Field Technician — Electrical Appliances',
      courseCode: 'ELE-201',
      batchCode: 'BAT-2026-DEL-01',
      centerName: 'Delhi Skill Hub Okhla',
      assessmentDate: '2026-11-25',
      maximumScore: 100,
      passScore: 50,
      theoryMaxScore: 30,
      practicalMaxScore: 50,
      vivaMaxScore: 20,
      totalCandidates: 25,
      candidateCount: 25,
      scoredCandidates: 25,
      scoredCount: 25,
      passedCandidates: 22,
      failedCandidates: 3,
      isFinalized: true,
      lifecycleStatusId: 2,
    },
    {
      id: 102,
      assessmentId: 102,
      assessmentCode: 'ASM-PLUMB-002',
      assessmentName: 'Plumbing Systems Practical Evaluation',
      courseName: 'General Plumbing Specialist',
      courseCode: 'PLB-101',
      batchCode: 'BAT-2026-NOI-03',
      centerName: 'Noida Center of Excellence',
      assessmentDate: '2026-11-28',
      maximumScore: 100,
      passScore: 50,
      theoryMaxScore: 30,
      practicalMaxScore: 50,
      vivaMaxScore: 20,
      totalCandidates: 18,
      candidateCount: 18,
      scoredCandidates: 0,
      scoredCount: 0,
      passedCandidates: 0,
      failedCandidates: 0,
      isFinalized: false,
      lifecycleStatusId: 1,
    },
  ];

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(assessorApi.getAssessorAssessments).mockResolvedValue(mockAssessments);
  });

  function renderWorkbench() {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    return render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={['/assessor/dashboard']}>
          <Routes>
            <Route path="/assessor/dashboard" element={<AssessorWorkbenchPage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );
  }

  it('renders assessor identity header and authoritative metrics', async () => {
    renderWorkbench();

    await waitFor(() => {
      expect(screen.getByText('Assessor Workbench')).toBeInTheDocument();
      expect(screen.getByText('ROLE_ASSESSOR')).toBeInTheDocument();
      expect(screen.getByText(/Vikram Assessor/)).toBeInTheDocument();
    });

    // Check Metrics Ribbon
    expect(screen.getByText('Assigned Assessments')).toBeInTheDocument();
    expect(screen.getByText('2')).toBeInTheDocument(); // 2 total
    expect(screen.getByText('Scheduled / Pending')).toBeInTheDocument();
    expect(screen.getByText('Finalized / Certified')).toBeInTheDocument();
  });

  it('renders assessment table with correct badges, progress and actions', async () => {
    renderWorkbench();

    await waitFor(() => {
      expect(screen.getByText('ASM-ELEC-001')).toBeInTheDocument();
      expect(screen.getByText('ASM-PLUMB-002')).toBeInTheDocument();
    });

    // Finalized badge on first assessment
    expect(screen.getAllByText('Finalized').length).toBeGreaterThan(0);
    // Scheduled badge on second assessment
    expect(screen.getAllByText('Scheduled').length).toBeGreaterThan(0);

    // Check Action Buttons
    expect(screen.getByText('View Results')).toBeInTheDocument();
    expect(screen.getByText('Scoring Grid')).toBeInTheDocument();
  });

  it('switches between Table and Calendar view smoothly', async () => {
    renderWorkbench();

    await waitFor(() => {
      expect(screen.getByText('ASM-ELEC-001')).toBeInTheDocument();
    });

    // Click Calendar button
    const calendarBtn = screen.getByRole('button', { name: /Calendar/i });
    fireEvent.click(calendarBtn);

    // Verify Calendar view controls
    expect(screen.getByRole('button', { name: /Today/i })).toBeInTheDocument();
    expect(screen.getByText('Sun')).toBeInTheDocument();
    expect(screen.getByText('Mon')).toBeInTheDocument();

    // Switch back to Table view
    const tableBtn = screen.getByRole('button', { name: /Table/i });
    fireEvent.click(tableBtn);

    expect(screen.getByText('Assessment Code / Name')).toBeInTheDocument();
  });
});
