import React, { useState } from 'react';
import { useOutletContext } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import type { TraineeProfile } from '@/types/trainee';
import {
  getTraineeOutcomeSummary,
  getTraineeSkillGapProfile,
  acceptSkillGapRecommendation,
  rejectSkillGapRecommendation,
  getTraineeSurveyResponses,
  submitSurveyResponse,
  submitSurveyResponseAnswer,
} from '@/api/traineeApi';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { EmptyState } from '@/components/feedback/EmptyState';

interface TraineeContextType {
  trainee: TraineeProfile | null;
  traineeId?: number;
  isUnlinked: boolean;
  isLoading: boolean;
}

export const TraineeJourneyPage: React.FC = () => {
  const { traineeId, isUnlinked, isLoading: isIdentityLoading } =
    useOutletContext<TraineeContextType>();
  const queryClient = useQueryClient();

  // Feedback form state
  const [courseSatisfaction, setCourseSatisfaction] = useState<number>(5);
  const [currentEmployment, setCurrentEmployment] = useState<string>('EMPLOYED');
  const [feedbackRemarks, setFeedbackRemarks] = useState<string>('');
  const [surveySubmitSuccess, setSurveySubmitSuccess] = useState<string | null>(null);
  const [surveySubmitError, setSurveySubmitError] = useState<string | null>(null);

  // Skill gap rejection modal/input state
  const [rejectingRecId, setRejectingRecId] = useState<number | null>(null);
  const [rejectionRemarks, setRejectionRemarks] = useState<string>('');
  const [remediationActionError, setRemediationActionError] = useState<string | null>(null);

  // 1. Outcome Summary Query
  const outcomesQuery = useQuery({
    queryKey: ['trainee', 'outcomes', traineeId],
    queryFn: () => getTraineeOutcomeSummary(traineeId!),
    enabled: Boolean(traineeId),
    retry: false,
  });

  // 2. Skill Gap Profile Query
  const skillGapQuery = useQuery({
    queryKey: ['trainee', 'skillgap', traineeId],
    queryFn: () => getTraineeSkillGapProfile(traineeId!),
    enabled: Boolean(traineeId),
    retry: false,
  });

  // 3. Survey Responses Query
  const surveyResponsesQuery = useQuery({
    queryKey: ['trainee', 'surveys', traineeId],
    queryFn: () => getTraineeSurveyResponses(traineeId!),
    enabled: Boolean(traineeId),
    retry: false,
  });

  // Accept Recommendation Mutation
  const acceptMutation = useMutation({
    mutationFn: (recId: number) =>
      acceptSkillGapRecommendation(recId, {
        traineeId: traineeId!,
        remarks: 'Accepted by trainee via portal',
      }),
    onSuccess: () => {
      setRemediationActionError(null);
      queryClient.invalidateQueries({ queryKey: ['trainee', 'skillgap', traineeId] });
    },
    onError: (err: any) => {
      setRemediationActionError(err?.message || 'Failed to accept recommendation.');
    },
  });

  // Reject Recommendation Mutation
  const rejectMutation = useMutation({
    mutationFn: ({ recId, remarks }: { recId: number; remarks: string }) =>
      rejectSkillGapRecommendation(recId, {
        traineeId: traineeId!,
        remarks,
      }),
    onSuccess: () => {
      setRejectingRecId(null);
      setRejectionRemarks('');
      setRemediationActionError(null);
      queryClient.invalidateQueries({ queryKey: ['trainee', 'skillgap', traineeId] });
    },
    onError: (err: any) => {
      setRemediationActionError(err?.message || 'Failed to submit rejection.');
    },
  });

  // Submit Survey Mutation
  const surveyMutation = useMutation({
    mutationFn: async () => {
      // Create response header
      const responseHeader = await submitSurveyResponse({
        surveyId: 1, // Standard Post-Training Employment Survey
        traineeId: traineeId!,
        attemptNumber: 1,
        statusId: 1,
      });

      // Submit feedback answer
      if (responseHeader?.id) {
        await submitSurveyResponseAnswer({
          surveyResponseId: responseHeader.id,
          surveyQuestionId: 1,
          answerText: feedbackRemarks,
          numericRating: courseSatisfaction,
        });
      }
      return responseHeader;
    },
    onSuccess: () => {
      setSurveySubmitSuccess('Thank you! Your post-training feedback has been recorded.');
      setSurveySubmitError(null);
      setFeedbackRemarks('');
      queryClient.invalidateQueries({ queryKey: ['trainee', 'surveys', traineeId] });
    },
    onError: (err: any) => {
      setSurveySubmitError(
        err?.message ||
          'Survey submission is restricted or requires an active campaign invitation.',
      );
      setSurveySubmitSuccess(null);
    },
  });

  if (isIdentityLoading) {
    return (
      <div className="bg-white rounded-lg p-12 border border-gov-neutral-200 flex justify-center">
        <LoadingSpinner size="md" />
      </div>
    );
  }

  if (isUnlinked || !traineeId) {
    return (
      <div className="bg-white rounded-lg p-8 border border-gov-neutral-200">
        <EmptyState
          title="Career Journey Restricted"
          description="A linked candidate identification record is required to view career milestones and post-training feedback surveys."
        />
      </div>
    );
  }

  const outcome = outcomesQuery.data;
  const skillProfile = skillGapQuery.data;
  const recommendations = skillProfile?.recommendations ?? [];
  const pastSurveys = surveyResponsesQuery.data?.content ?? [];

  return (
    <div className="space-y-6">
      {/* Career Milestones Section */}
      <section
        aria-labelledby="career-milestones-heading"
        className="bg-white rounded-lg shadow-sm border border-gov-neutral-200 p-6"
      >
        <div className="pb-4 border-b border-gov-neutral-200 mb-6">
          <h2
            id="career-milestones-heading"
            className="text-lg font-bold text-gov-neutral-900"
          >
            Longitudinal Career Journey &amp; Verified Outcomes
          </h2>
          <p className="text-xs text-gov-neutral-500">
            Official post-training employment trajectory verified under the National Vocational Tracking Framework.
          </p>
        </div>

        {outcomesQuery.isLoading ? (
          <div className="py-8 flex justify-center">
            <LoadingSpinner size="md" />
          </div>
        ) : outcomesQuery.isError ? (
          <div className="text-sm text-gov-neutral-600 bg-gov-neutral-50 p-4 rounded border border-gov-neutral-200">
            <span>Employment outcome data is being compiled by state field verification officers.</span>
          </div>
        ) : outcome ? (
          <div className="space-y-6">
            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4">
              <div className="bg-gov-neutral-50 p-4 rounded-lg border border-gov-neutral-200">
                <span className="text-xs font-semibold uppercase text-gov-neutral-500">
                  Current Status
                </span>
                <p className="text-lg font-bold text-gov-neutral-900 mt-1">
                  {outcome.snapshotIsEmployedFlag ? (
                    <span className="text-emerald-700">● Gainfully Employed</span>
                  ) : (
                    <span className="text-amber-700">Seeking Placement</span>
                  )}
                </p>
              </div>
              <div className="bg-gov-neutral-50 p-4 rounded-lg border border-gov-neutral-200">
                <span className="text-xs font-semibold uppercase text-gov-neutral-500">
                  Verified Employer
                </span>
                <p className="text-base font-bold text-gov-neutral-900 mt-1 truncate">
                  {outcome.lastVerifiedEmployerName || 'Under Verification'}
                </p>
              </div>
              <div className="bg-gov-neutral-50 p-4 rounded-lg border border-gov-neutral-200">
                <span className="text-xs font-semibold uppercase text-gov-neutral-500">
                  Monthly Emolument
                </span>
                <p className="text-lg font-bold text-gov-neutral-900 mt-1 font-mono">
                  {outcome.monthlySalary ? `₹${outcome.monthlySalary.toLocaleString('en-IN')}` : '—'}
                </p>
              </div>
              <div className="bg-gov-neutral-50 p-4 rounded-lg border border-gov-neutral-200">
                <span className="text-xs font-semibold uppercase text-gov-neutral-500">
                  Placement Date
                </span>
                <p className="text-base font-semibold text-gov-neutral-800 mt-1">
                  {outcome.lastPlacementDate ? new Date(outcome.lastPlacementDate).toLocaleDateString() : '—'}
                </p>
              </div>
            </div>
          </div>
        ) : (
          <EmptyState
            title="No Outcome Records"
            description="No post-training employment records have been confirmed yet."
          />
        )}
      </section>

      {/* Skill Gap Remediation & Recommendations Section */}
      <section
        aria-labelledby="skill-recommendations-heading"
        className="bg-white rounded-lg shadow-sm border border-gov-neutral-200 p-6"
      >
        <div className="pb-4 border-b border-gov-neutral-200 mb-4 flex items-center justify-between">
          <div>
            <h3
              id="skill-recommendations-heading"
              className="text-base font-bold text-gov-neutral-900"
            >
              Skill Enhancement &amp; Remediation Pathways
            </h3>
            <p className="text-xs text-gov-neutral-500">
              Personalized course recommendations generated from skill gap assessments.
            </p>
          </div>
          {(acceptMutation.isPending || rejectMutation.isPending) && (
            <LoadingSpinner size="sm" />
          )}
        </div>

        {remediationActionError && (
          <div
            role="alert"
            className="bg-red-50 border border-red-200 p-3 rounded text-red-900 text-xs mb-4 flex justify-between"
          >
            <span>{remediationActionError}</span>
            <button
              type="button"
              onClick={() => setRemediationActionError(null)}
              className="font-bold underline ml-2"
            >
              Dismiss
            </button>
          </div>
        )}

        {skillGapQuery.isLoading ? (
          <div className="py-6 flex justify-center">
            <LoadingSpinner size="md" />
          </div>
        ) : skillGapQuery.isError || recommendations.length === 0 ? (
          <div className="bg-gov-neutral-50 p-6 rounded-lg border border-gov-neutral-200 text-center">
            <p className="text-sm font-semibold text-gov-neutral-800">
              No Pending Upskilling Recommendations
            </p>
            <p className="text-xs text-gov-neutral-500 mt-1 max-w-md mx-auto">
              You are currently up to date on your skill pathways. When an assessment detects a competency gap, recommended bridge courses will appear here.
            </p>
          </div>
        ) : (
          <div className="space-y-4">
            {recommendations.map((rec) => (
              <div
                key={rec.id}
                className="border border-gov-neutral-200 rounded-lg p-4 flex flex-col md:flex-row md:items-center justify-between gap-4"
              >
                <div>
                  <h4 className="text-sm font-bold text-gov-neutral-900">
                    {rec.recommendedCourseName || `Remediation Course #${rec.id}`}
                  </h4>
                  <p className="text-xs text-gov-neutral-600 mt-0.5">
                    {rec.remarks || 'Recommended to bridge identified competency gap.'}
                  </p>
                  <span className="text-[11px] font-mono text-gov-neutral-500 mt-1 block">
                    Status: {rec.status || 'PROPOSED'}
                  </span>
                </div>

                <div className="flex items-center gap-2 shrink-0">
                  {rejectingRecId === rec.id ? (
                    <div className="flex items-center gap-2">
                      <input
                        type="text"
                        placeholder="Reason for declining..."
                        value={rejectionRemarks}
                        onChange={(e) => setRejectionRemarks(e.target.value)}
                        className="text-xs border border-gov-neutral-300 rounded px-2 py-1.5 focus:ring-1 focus:ring-gov-navy focus:outline-none"
                      />
                      <button
                        type="button"
                        disabled={rejectMutation.isPending || !rejectionRemarks.trim()}
                        onClick={() =>
                          rejectMutation.mutate({
                            recId: rec.id,
                            remarks: rejectionRemarks.trim(),
                          })
                        }
                        className="px-3 py-1.5 text-xs font-bold bg-red-600 text-white rounded hover:bg-red-700 disabled:opacity-50"
                      >
                        Confirm
                      </button>
                      <button
                        type="button"
                        onClick={() => setRejectingRecId(null)}
                        className="px-2 py-1.5 text-xs text-gov-neutral-600 hover:underline"
                      >
                        Cancel
                      </button>
                    </div>
                  ) : (
                    <>
                      <button
                        type="button"
                        disabled={acceptMutation.isPending || rejectMutation.isPending}
                        onClick={() => acceptMutation.mutate(rec.id)}
                        className="px-3.5 py-1.5 text-xs font-bold rounded bg-emerald-700 text-white hover:bg-emerald-800 disabled:opacity-50"
                      >
                        Accept Pathway
                      </button>
                      <button
                        type="button"
                        disabled={acceptMutation.isPending || rejectMutation.isPending}
                        onClick={() => setRejectingRecId(rec.id)}
                        className="px-3 py-1.5 text-xs font-semibold rounded border border-gov-neutral-300 text-gov-neutral-700 hover:bg-gov-neutral-50 disabled:opacity-50"
                      >
                        Decline
                      </button>
                    </>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </section>

      {/* Post-Training Survey & Feedback Section */}
      <section
        aria-labelledby="survey-feedback-heading"
        className="bg-white rounded-lg shadow-sm border border-gov-neutral-200 p-6"
      >
        <div className="pb-4 border-b border-gov-neutral-200 mb-6">
          <h3
            id="survey-feedback-heading"
            className="text-base font-bold text-gov-neutral-900"
          >
            Post-Training Feedback &amp; Verification Survey
          </h3>
          <p className="text-xs text-gov-neutral-500">
            Share your experience to help enhance national training quality and verify employment outcomes.
          </p>
        </div>

        {surveySubmitSuccess && (
          <div
            role="status"
            className="bg-emerald-50 border border-emerald-200 p-4 rounded-lg text-emerald-900 text-sm mb-6 flex items-center justify-between"
          >
            <span>{surveySubmitSuccess}</span>
            <button
              type="button"
              onClick={() => setSurveySubmitSuccess(null)}
              className="text-xs font-bold underline"
            >
              Dismiss
            </button>
          </div>
        )}

        {surveySubmitError && (
          <div
            role="alert"
            className="bg-amber-50 border border-amber-200 p-4 rounded-lg text-amber-900 text-sm mb-6 flex items-center justify-between"
          >
            <span>{surveySubmitError}</span>
            <button
              type="button"
              onClick={() => setSurveySubmitError(null)}
              className="text-xs font-bold underline"
            >
              Dismiss
            </button>
          </div>
        )}

        {/* Dynamic Questionnaire Form */}
        <form
          onSubmit={(e) => {
            e.preventDefault();
            surveyMutation.mutate();
          }}
          className="space-y-6 max-w-xl"
        >
          {/* Question 1: Current Employment Situation */}
          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-gov-neutral-700 mb-2">
              1. What is your current employment status?
            </label>
            <div className="space-y-2">
              {[
                { id: 'WAGE_EMPLOYED', label: 'Employed in a company / organization (Wage Employed)' },
                { id: 'SELF_EMPLOYED', label: 'Self-employed / Entrepreneur' },
                { id: 'IN_EDUCATION', label: 'Pursuing higher education / further training' },
                { id: 'UNEMPLOYED', label: 'Currently looking for a job' },
              ].map((opt) => (
                <label
                  key={opt.id}
                  className="flex items-center gap-3 text-xs md:text-sm text-gov-neutral-800 cursor-pointer"
                >
                  <input
                    type="radio"
                    name="employmentStatus"
                    value={opt.id}
                    checked={currentEmployment === opt.id}
                    onChange={(e) => setCurrentEmployment(e.target.value)}
                    className="h-4 w-4 text-gov-navy border-gov-neutral-300 focus:ring-gov-navy"
                  />
                  <span>{opt.label}</span>
                </label>
              ))}
            </div>
          </div>

          {/* Question 2: Course Content Relevance Rating */}
          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-gov-neutral-700 mb-2">
              2. How relevant was your training program to current industry requirements?
            </label>
            <div className="flex items-center gap-2">
              {[1, 2, 3, 4, 5].map((star) => (
                <button
                  key={star}
                  type="button"
                  onClick={() => setCourseSatisfaction(star)}
                  className={`w-10 h-10 rounded border text-sm font-bold transition-all focus:outline-none focus:ring-2 focus:ring-gov-navy ${
                    courseSatisfaction >= star
                      ? 'bg-gov-navy text-white border-gov-navy shadow-sm'
                      : 'bg-white text-gov-neutral-600 border-gov-neutral-300 hover:bg-gov-neutral-50'
                  }`}
                  aria-label={`Rate ${star} out of 5 stars`}
                >
                  {star}★
                </button>
              ))}
              <span className="text-xs text-gov-neutral-500 ml-2">
                ({courseSatisfaction} out of 5)
              </span>
            </div>
          </div>

          {/* Question 3: Qualitative Feedback */}
          <div>
            <label
              htmlFor="feedbackRemarks"
              className="block text-xs font-bold uppercase tracking-wider text-gov-neutral-700 mb-2"
            >
              3. Suggestions or feedback for program coordinators:
            </label>
            <textarea
              id="feedbackRemarks"
              rows={3}
              value={feedbackRemarks}
              onChange={(e) => setFeedbackRemarks(e.target.value)}
              placeholder="Share any comments regarding training quality, center equipment, or placement support..."
              className="w-full text-xs md:text-sm border border-gov-neutral-300 rounded-md p-3 focus:outline-none focus:ring-2 focus:ring-gov-navy"
            />
          </div>

          <div>
            <button
              type="submit"
              disabled={surveyMutation.isPending}
              className="px-5 py-2.5 text-xs md:text-sm font-bold rounded bg-gov-navy text-white hover:bg-gov-navy-light focus:outline-none focus:ring-2 focus:ring-gov-navy focus:ring-offset-2 disabled:opacity-50 transition-colors inline-flex items-center gap-2"
            >
              {surveyMutation.isPending ? (
                <>
                  <LoadingSpinner size="sm" />
                  <span>Submitting Survey...</span>
                </>
              ) : (
                <span>Submit Feedback Survey</span>
              )}
            </button>
          </div>
        </form>

        {/* Survey History Section */}
        {pastSurveys.length > 0 && (
          <div className="mt-8 pt-6 border-t border-gov-neutral-200">
            <h4 className="text-xs font-bold uppercase tracking-wider text-gov-neutral-500 mb-3">
              Previously Submitted Surveys
            </h4>
            <div className="space-y-2">
              {pastSurveys.map((resp) => (
                <div
                  key={resp.id}
                  className="text-xs bg-gov-neutral-50 p-3 rounded border border-gov-neutral-200 flex justify-between"
                >
                  <span className="font-semibold text-gov-neutral-800">
                    Survey Response #{resp.id} (Attempt {resp.attemptNumber})
                  </span>
                  <span className="text-gov-neutral-500">
                    {resp.submittedAt ? new Date(resp.submittedAt).toLocaleDateString() : 'Recorded'}
                  </span>
                </div>
              ))}
            </div>
          </div>
        )}
      </section>
    </div>
  );
};

export default TraineeJourneyPage;
