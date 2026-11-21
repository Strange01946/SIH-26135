import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter, Outlet, Route, Routes } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import {
  TraineeAssessmentsPage,
  resolveAssessmentOutcome,
} from '@/pages/trainee/TraineeAssessmentsPage';
import * as traineeApi from '@/api/traineeApi';
import type { Certification, AssessmentResult } from '@/types/trainee';

vi.mock('@/api/traineeApi');

function renderAssessmentsWithContext(contextValue: any) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });

  const LayoutMock = () => <Outlet context={contextValue} />;

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/trainee/assessments']}>
        <Routes>
          <Route element={<LayoutMock />}>
            <Route path="/trainee/assessments" element={<TraineeAssessmentsPage />} />
          </Route>
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('TraineeAssessmentsPage Component', () => {
  const mockCertifications: Certification[] = [
    {
      id: 501,
      certificateNumber: 'CERT-2026-00501',
      traineeId: 42,
      courseId: 5,
      courseName: 'Solar PV Installer Technician',
      programName: 'Pradhan Mantri Kaushal Vikas Yojana',
      issuingBody: 'NCVET & Skill Council for Green Jobs',
      issueDate: '2026-10-20',
      certificateVerificationStatusName: 'Verified',
      certificateStatusName: 'Active',
    },
  ];

  const mockAssessments: AssessmentResult[] = [
    {
      id: 401,
      traineeId: 42,
      assessmentId: 10,
      assessmentName: 'National Trade Test — Practical Wiring',
      assessedAt: '2026-10-15',
      totalScore: 88,
      maxScore: 100,
      percentage: 88,
      grade: 'A',
      outcomeCode: 'PASS',
      outcomeName: 'Passed with Distinction',
    },
    {
      id: 402,
      traineeId: 42,
      assessmentId: 11,
      assessmentName: 'Solar Safety Regulations Theory',
      assessedAt: '2026-10-16',
      totalScore: 92,
      maxScore: 100,
      percentage: 92,
      grade: 'O',
      outcomeCode: 'PASS',
      outcomeName: 'Pass',
    },
  ];

  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('renders certifications list and assessment evaluations scorecard table', async () => {
    vi.spyOn(traineeApi, 'getTraineeAssessments').mockResolvedValueOnce(mockAssessments);
    vi.spyOn(traineeApi, 'getTraineeCertifications').mockResolvedValueOnce(mockCertifications);

    renderAssessmentsWithContext({ traineeId: 42, isUnlinked: false, isLoading: false });

    await waitFor(() => {
      expect(screen.getByText(/CERT-2026-00501/)).toBeInTheDocument();
    });

    expect(screen.getByText('Solar PV Installer Technician')).toBeInTheDocument();
    expect(screen.getByText('NCVET & Skill Council for Green Jobs')).toBeInTheDocument();
    expect(screen.getByText('Verified')).toBeInTheDocument();

    // Assessments table
    expect(screen.getByText('National Trade Test — Practical Wiring')).toBeInTheDocument();
    expect(screen.getByText('88 / 100')).toBeInTheDocument();
    expect(screen.getByText('88%')).toBeInTheDocument();
    expect(screen.getByText('Passed with Distinction')).toBeInTheDocument();
  });

  it('handles certificate PDF download when binary response is valid', async () => {
    vi.spyOn(traineeApi, 'getTraineeAssessments').mockResolvedValueOnce([]);
    vi.spyOn(traineeApi, 'getTraineeCertifications').mockResolvedValueOnce(mockCertifications);

    const pdfBlob = new Blob(['%PDF-1.4 sample binary'], { type: 'application/pdf' });
    vi.spyOn(traineeApi, 'downloadCertificatePdf').mockResolvedValueOnce({
      success: true,
      blob: pdfBlob,
    });

    const createObjectURLSpy = vi.fn().mockReturnValue('blob:mock-url');
    const revokeObjectURLSpy = vi.fn();
    window.URL.createObjectURL = createObjectURLSpy;
    window.URL.revokeObjectURL = revokeObjectURLSpy;

    renderAssessmentsWithContext({ traineeId: 42, isUnlinked: false, isLoading: false });

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Download Certificate/i })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /Download Certificate/i }));

    await waitFor(() => {
      expect(screen.getByText(/Certificate PDF downloaded successfully/i)).toBeInTheDocument();
    });
    expect(createObjectURLSpy).toHaveBeenCalledWith(pdfBlob);
    expect(revokeObjectURLSpy).toHaveBeenCalledWith('blob:mock-url');
  });

  it('handles certificate PDF download when backend does not stream binary file', async () => {
    vi.spyOn(traineeApi, 'getTraineeAssessments').mockResolvedValueOnce([]);
    vi.spyOn(traineeApi, 'getTraineeCertifications').mockResolvedValueOnce(mockCertifications);

    vi.spyOn(traineeApi, 'downloadCertificatePdf').mockResolvedValueOnce({
      success: false,
      message: 'Certificate PDF export service is not enabled on this environment.',
    });

    renderAssessmentsWithContext({ traineeId: 42, isUnlinked: false, isLoading: false });

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Download Certificate/i })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /Download Certificate/i }));

    await waitFor(() => {
      expect(screen.getByText(/Certificate PDF export service is not enabled/i)).toBeInTheDocument();
    });
  });

  it('handles 403 authorization restrictions gracefully', async () => {
    vi.spyOn(traineeApi, 'getTraineeAssessments').mockRejectedValueOnce({
      status: 403,
      message: 'Access Denied',
    });
    vi.spyOn(traineeApi, 'getTraineeCertifications').mockRejectedValueOnce({
      status: 403,
      message: 'Access Denied',
    });

    renderAssessmentsWithContext({ traineeId: 42, isUnlinked: false, isLoading: false });

    await waitFor(() => {
      expect(screen.getByText(/Certificate Registry Notice/i)).toBeInTheDocument();
    });
    expect(screen.getByText(/Assessment score records require evaluation clearance/i)).toBeInTheDocument();
  });

  describe('resolveAssessmentOutcome() authoritative evaluation tests', () => {
    it('respects backend isPassFlag=false even when percentage is >= 50%', () => {
      const assessment: AssessmentResult = {
        id: 1,
        traineeId: 42,
        isPassFlag: false,
        outcomeCode: 'FAIL',
        outcomeName: 'Failed Practical Test',
        percentage: 75, // High percentage, but failed
        scorePercentage: 75,
      };

      const outcome = resolveAssessmentOutcome(assessment);
      expect(outcome.isPass).toBe(false);
      expect(outcome.verdict).toBe('FAIL');
      expect(outcome.label).toBe('Failed Practical Test');
      expect(outcome.badgeClass).toContain('text-red-700');
    });

    it('respects backend isPassFlag=true even when percentage is < 50%', () => {
      const assessment: AssessmentResult = {
        id: 2,
        traineeId: 42,
        isPassFlag: true,
        outcomeCode: 'PASS',
        outcomeName: 'Passed Qualified',
        percentage: 42, // Lower percentage, but passing threshold met for this QP
      };

      const outcome = resolveAssessmentOutcome(assessment);
      expect(outcome.isPass).toBe(true);
      expect(outcome.verdict).toBe('PASS');
      expect(outcome.label).toBe('Passed Qualified');
      expect(outcome.badgeClass).toContain('text-emerald-700');
    });

    it('identifies PENDING status correctly without defaulting to fail or pass', () => {
      const assessment: AssessmentResult = {
        id: 3,
        traineeId: 42,
        outcomeCode: 'PENDING',
        outcomeName: 'Evaluation in Progress',
      };

      const outcome = resolveAssessmentOutcome(assessment);
      expect(outcome.isPass).toBe(false);
      expect(outcome.verdict).toBe('PENDING');
      expect(outcome.label).toBe('Evaluation in Progress');
      expect(outcome.badgeClass).toContain('text-blue-700');
    });

    it('identifies WITHHELD status correctly', () => {
      const assessment: AssessmentResult = {
        id: 4,
        traineeId: 42,
        outcomeCode: 'WITHHELD',
      };

      const outcome = resolveAssessmentOutcome(assessment);
      expect(outcome.isPass).toBe(false);
      expect(outcome.label).toBe('WITHHELD');
      expect(outcome.badgeClass).toContain('text-amber-700');
    });

    it('identifies ABSENT status correctly', () => {
      const assessment: AssessmentResult = {
        id: 5,
        traineeId: 42,
        outcomeCode: 'ABSENT',
      };

      const outcome = resolveAssessmentOutcome(assessment);
      expect(outcome.isPass).toBe(false);
      expect(outcome.label).toBe('ABSENT');
      expect(outcome.badgeClass).toContain('text-gov-neutral-600');
    });

    it('handles missing outcomeCode and isPassFlag gracefully as Awaiting Evaluation', () => {
      const assessment: AssessmentResult = {
        id: 6,
        traineeId: 42,
      };

      const outcome = resolveAssessmentOutcome(assessment);
      expect(outcome.isPass).toBe(false);
      expect(outcome.label).toBe('AWAITING EVALUATION');
    });
  });
});
