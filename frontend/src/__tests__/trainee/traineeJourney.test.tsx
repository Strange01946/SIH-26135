import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter, Outlet, Route, Routes } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { TraineeJourneyPage } from '@/pages/trainee/TraineeJourneyPage';
import * as traineeApi from '@/api/traineeApi';
import type { TraineeOutcomeSummary, TraineeSkillGapProfile } from '@/types/trainee';
import type { PagedResponse } from '@/api/types';
import type { SurveyResponse } from '@/types/trainee';

vi.mock('@/api/traineeApi');

const emptySurveyPaged: PagedResponse<SurveyResponse> = {
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

function renderJourneyWithContext(contextValue: any) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });

  const LayoutMock = () => <Outlet context={contextValue} />;

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/trainee/journey']}>
        <Routes>
          <Route element={<LayoutMock />}>
            <Route path="/trainee/journey" element={<TraineeJourneyPage />} />
          </Route>
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('TraineeJourneyPage Component', () => {
  const mockOutcome: TraineeOutcomeSummary = {
    traineeId: 42,
    snapshotIsEmployedFlag: true,
    lastVerifiedEmployerName: 'Tata Power Solar Systems',
    monthlySalary: 35000,
    lastPlacementDate: '2026-10-01',
  };

  const mockSkillProfile: TraineeSkillGapProfile = {
    traineeId: 42,
    hasAssessment: true,
    recommendationsCount: 1,
    recommendations: [
      {
        id: 77,
        recommendedCourseName: 'High Voltage Grid Safety Bridge Course',
        status: 'PROPOSED',
        remarks: 'Targeted upskilling for commercial installations',
      },
    ],
  };

  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('renders career milestones with verified employer, salary, and status', async () => {
    vi.spyOn(traineeApi, 'getTraineeOutcomeSummary').mockResolvedValue(mockOutcome);
    vi.spyOn(traineeApi, 'getTraineeSkillGapProfile').mockResolvedValue(mockSkillProfile);
    vi.spyOn(traineeApi, 'getTraineeSurveyResponses').mockResolvedValue(emptySurveyPaged);

    renderJourneyWithContext({ traineeId: 42, isUnlinked: false, isLoading: false });

    await waitFor(() => {
      expect(screen.getByText('Tata Power Solar Systems')).toBeInTheDocument();
    });

    expect(screen.getByText(/35,000/)).toBeInTheDocument();
    expect(screen.getByText(/Gainfully Employed/i)).toBeInTheDocument();
  });

  it('renders skill gap recommendations and handles Accept action', async () => {
    vi.spyOn(traineeApi, 'getTraineeOutcomeSummary').mockResolvedValue(mockOutcome);
    vi.spyOn(traineeApi, 'getTraineeSkillGapProfile').mockResolvedValue(mockSkillProfile);
    vi.spyOn(traineeApi, 'getTraineeSurveyResponses').mockResolvedValue(emptySurveyPaged);
    const acceptSpy = vi.spyOn(traineeApi, 'acceptSkillGapRecommendation').mockResolvedValueOnce({
      id: 77,
      status: 'ACCEPTED',
    });

    renderJourneyWithContext({ traineeId: 42, isUnlinked: false, isLoading: false });

    await waitFor(() => {
      expect(screen.getByText('High Voltage Grid Safety Bridge Course')).toBeInTheDocument();
    });

    const acceptButton = screen.getByRole('button', { name: /Accept Pathway/i });
    fireEvent.click(acceptButton);

    await waitFor(() => {
      expect(acceptSpy).toHaveBeenCalledWith(
        77,
        expect.objectContaining({ traineeId: 42 }),
      );
    });
  });

  it('handles Decline recommendation action with mandatory remarks input', async () => {
    vi.spyOn(traineeApi, 'getTraineeOutcomeSummary').mockResolvedValue(mockOutcome);
    vi.spyOn(traineeApi, 'getTraineeSkillGapProfile').mockResolvedValue(mockSkillProfile);
    vi.spyOn(traineeApi, 'getTraineeSurveyResponses').mockResolvedValue(emptySurveyPaged);
    const rejectSpy = vi.spyOn(traineeApi, 'rejectSkillGapRecommendation').mockResolvedValueOnce({
      id: 77,
      status: 'REJECTED',
    });

    renderJourneyWithContext({ traineeId: 42, isUnlinked: false, isLoading: false });

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Decline/i })).toBeInTheDocument();
    });

    // Click Decline to open reason prompt
    fireEvent.click(screen.getByRole('button', { name: /Decline/i }));

    const input = screen.getByPlaceholderText(/Reason for declining/i);
    expect(input).toBeInTheDocument();

    fireEvent.change(input, { target: { value: 'Relocating to another district' } });

    const confirmButton = screen.getByRole('button', { name: /Confirm/i });
    fireEvent.click(confirmButton);

    await waitFor(() => {
      expect(rejectSpy).toHaveBeenCalledWith(
        77,
        expect.objectContaining({
          traineeId: 42,
          remarks: 'Relocating to another district',
        }),
      );
    });
  });

  it('renders honest campaign-required notice for surveys and shows empty past responses', async () => {
    vi.spyOn(traineeApi, 'getTraineeOutcomeSummary').mockResolvedValue(mockOutcome);
    vi.spyOn(traineeApi, 'getTraineeSkillGapProfile').mockResolvedValue({ traineeId: 42, hasAssessment: false });
    vi.spyOn(traineeApi, 'getTraineeSurveyResponses').mockResolvedValue(emptySurveyPaged);

    renderJourneyWithContext({ traineeId: 42, isUnlinked: false, isLoading: false });

    await waitFor(() => {
      expect(screen.getByText('Post-Training Feedback & Verification Survey')).toBeInTheDocument();
      expect(
        screen.getByText(/No previous survey responses recorded for your profile/i),
      ).toBeInTheDocument();
    });

    expect(screen.getByText('Campaign-Assigned System')).toBeInTheDocument();
    expect(
      screen.getByText(/Feedback Surveys Require an Active Campaign Invitation/i),
    ).toBeInTheDocument();
  });

  it('renders completed survey responses history when records exist', async () => {
    const mockPastSurveys: PagedResponse<SurveyResponse> = {
      content: [
        {
          id: 555,
          surveyId: 10,
          traineeId: 42,
          attemptNumber: 1,
          submittedAt: '2026-10-25T14:30:00Z',
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
      first: true,
      last: true,
      hasNext: false,
      hasPrevious: false,
    };

    vi.spyOn(traineeApi, 'getTraineeOutcomeSummary').mockResolvedValue(mockOutcome);
    vi.spyOn(traineeApi, 'getTraineeSkillGapProfile').mockResolvedValue({ traineeId: 42, hasAssessment: false });
    vi.spyOn(traineeApi, 'getTraineeSurveyResponses').mockResolvedValue(mockPastSurveys);

    renderJourneyWithContext({ traineeId: 42, isUnlinked: false, isLoading: false });

    await waitFor(() => {
      expect(screen.getByText(/Survey Response #555 \(Attempt 1\)/i)).toBeInTheDocument();
    });
  });
});
