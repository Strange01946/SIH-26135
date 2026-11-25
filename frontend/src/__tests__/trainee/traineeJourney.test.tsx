import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter, Outlet, Route, Routes } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { TraineeJourneyPage } from '@/pages/trainee/TraineeJourneyPage';
import * as traineeApi from '@/api/traineeApi';
import type { TraineeSkillGapProfile } from '@/types/trainee';
import type { PagedResponse } from '@/api/types';
import type { EmploymentPeriod } from '@/api/traineeApi';

vi.mock('@/api/traineeApi');
vi.mock('@/api/careerSkillsApi', () => ({ careerSkillsKey: ['career-skills'],
  getOwnCareerSkills: async () => ({ revision: 0, skills: [], evidence: [], requirements: [] }),
  getCareerTraining: async () => [],
  saveOwnCareerSkills: vi.fn(),
}));

const emptySurveys = { followupConsentGranted: true, careerUpdateAvailable: false, assignments: [], recentSubmissions: [] };
const employmentHistory: PagedResponse<EmploymentPeriod> = {
  content: [{ id: 1, employer: 'Tata Power Solar Systems', startingPay: 35000, currency: 'INR', payFrequency: 'MONTHLY' }],
  page: 0, size: 10, totalElements: 1, totalPages: 1, first: true, last: true, hasNext: false, hasPrevious: false,
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
  const mockCareerStatus = {
    status: { name: 'Wage employed', source: 'EMPLOYMENT_RECORD' },
    unemploymentPeriods: [],
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
    vi.mocked(traineeApi.getEmploymentHistory).mockResolvedValue(employmentHistory);
    vi.mocked(traineeApi.getFollowupSurveys).mockResolvedValue(emptySurveys);
  });

  it('renders career milestones with verified employer, salary, and status', async () => {
    vi.spyOn(traineeApi, 'getRecordedCareerStatus').mockResolvedValue(mockCareerStatus);
    vi.spyOn(traineeApi, 'getTraineeSkillGapProfile').mockResolvedValue(mockSkillProfile);

    renderJourneyWithContext({ traineeId: 42, isUnlinked: false, isLoading: false });

    await waitFor(() => {
      expect(screen.getByText('Tata Power Solar Systems')).toBeInTheDocument();
    });

    expect(screen.getByText(/35,000/)).toBeInTheDocument();
    expect(await screen.findByText('Wage employed')).toBeInTheDocument();
  });

  it('renders skill gap recommendations and handles Accept action', async () => {
    vi.spyOn(traineeApi, 'getRecordedCareerStatus').mockResolvedValue(mockCareerStatus);
    vi.spyOn(traineeApi, 'getTraineeSkillGapProfile').mockResolvedValue(mockSkillProfile);
    const acceptSpy = vi.spyOn(traineeApi, 'acceptSkillGapRecommendation').mockResolvedValueOnce({
      id: 77,
      status: 'ACCEPTED',
    });

    renderJourneyWithContext({ traineeId: 42, isUnlinked: false, isLoading: false });

    await waitFor(() => {
      expect(screen.getByText('High Voltage Grid Safety Bridge Course')).toBeInTheDocument();
    });

    const acceptButton = screen.getByRole('button', { name: /Interested in this pathway/i });
    fireEvent.click(acceptButton);

    await waitFor(() => {
      expect(acceptSpy).toHaveBeenCalledWith(
        77,
        expect.objectContaining({ traineeId: 42 }),
      );
    });
  });

  it('handles Decline recommendation action with mandatory remarks input', async () => {
    vi.spyOn(traineeApi, 'getRecordedCareerStatus').mockResolvedValue(mockCareerStatus);
    vi.spyOn(traineeApi, 'getTraineeSkillGapProfile').mockResolvedValue(mockSkillProfile);
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

  it('shows when no campaign assigned an open survey', async () => {
    vi.mocked(traineeApi.getRecordedCareerStatus).mockResolvedValue(mockCareerStatus);
    vi.mocked(traineeApi.getTraineeSkillGapProfile).mockResolvedValue({ traineeId: 42, hasAssessment: false });
    renderJourneyWithContext({ traineeId: 42, isUnlinked: false, isLoading: false });
    expect(await screen.findByText('You have no open survey assignments.')).toBeInTheDocument();
    expect(screen.getByText('The employment update questionnaire is currently unavailable.')).toBeInTheDocument();
    expect(screen.getByText('There are no previous survey submissions.')).toBeInTheDocument();
  });

  it('validates required answers and records a survey receipt in history', async () => {
    vi.mocked(traineeApi.getRecordedCareerStatus).mockResolvedValue(mockCareerStatus);
    vi.mocked(traineeApi.getTraineeSkillGapProfile).mockResolvedValue({ traineeId: 42, hasAssessment: false });
    const assignment = { taskId: 7, name: 'Six-month follow-up' };
    vi.mocked(traineeApi.getFollowupSurveys)
      .mockResolvedValueOnce({ ...emptySurveys, assignments: [assignment] })
      .mockResolvedValue({ ...emptySurveys, recentSubmissions: [{ id: 55, name: assignment.name }] });
    const question = { id: 1, text: 'Are you currently employed?', type: 'BOOLEAN', required: true, options: [] };
    vi.mocked(traineeApi.getFollowupQuestionnaire).mockResolvedValue({ assignment, versionId: 9, questions: [question] });
    const submit = vi.mocked(traineeApi.submitFollowupQuestionnaire).mockResolvedValue({ id: 55, name: assignment.name });
    renderJourneyWithContext({ traineeId: 42, isUnlinked: false, isLoading: false });
    fireEvent.click(await screen.findByRole('button', { name: 'Open Six-month follow-up' }));
    const submitButton = await screen.findByRole('button', { name: 'Submit survey' });
    fireEvent.click(submitButton);
    expect(screen.getByRole('alert')).toHaveTextContent('Answer this question');
    expect(submit).not.toHaveBeenCalled();
    fireEvent.change(screen.getByLabelText(question.text), { target: { value: 'false' } });
    fireEvent.click(submitButton);
    expect(await screen.findByText('Survey submitted · Six-month follow-up')).toBeInTheDocument();
    expect(submit).toHaveBeenCalledWith(7, { versionId: 9, answers: [{ questionId: 1, yes: false }] });
    await waitFor(() => expect(screen.queryByRole('button', { name: 'Open Six-month follow-up' })).not.toBeInTheDocument());
  });
});
