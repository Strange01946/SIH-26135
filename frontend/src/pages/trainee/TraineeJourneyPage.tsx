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

      {/* Post-Training Survey & Longitudinal Feedback Section */}
      <section
        aria-labelledby="survey-feedback-heading"
        className="bg-white rounded-lg shadow-sm border border-gov-neutral-200 p-6"
      >
        <div className="pb-4 border-b border-gov-neutral-200 mb-6 flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <h3
              id="survey-feedback-heading"
              className="text-base font-bold text-gov-neutral-900"
            >
              Post-Training Feedback &amp; Verification Survey
            </h3>
            <p className="text-xs text-gov-neutral-500">
              National Vocational Tracking Framework longitudinal impact evaluation and feedback campaigns.
            </p>
          </div>
          <span className="text-xs font-semibold px-2.5 py-1 rounded-full bg-blue-50 text-blue-800 border border-blue-200 w-fit">
            Campaign-Assigned System
          </span>
        </div>

        {/* Campaign Required State */}
        <div className="bg-gov-neutral-50 border border-gov-neutral-200 rounded-lg p-5">
          <div className="flex items-start gap-3">
            <div className="w-8 h-8 rounded-full bg-blue-100 text-blue-700 flex items-center justify-center shrink-0 mt-0.5">
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
                />
              </svg>
            </div>
            <div className="space-y-1">
              <h4 className="text-sm font-bold text-gov-neutral-900">
                Feedback Surveys Require an Active Campaign Invitation
              </h4>
              <p className="text-xs text-gov-neutral-600 leading-relaxed">
                Post-training quality feedback and employment verification surveys are issued systematically through scheduled NCVET monitoring campaigns. Candidate self-initiated questionnaire submissions without an active campaign assignment are not supported by the central survey register.
              </p>
              <p className="text-xs text-gov-neutral-500 pt-1">
                When your cohort is selected for an evaluation round, an invitation with your unique campaign access key will appear here and be dispatched via your consented communication channels.
              </p>
            </div>
          </div>
        </div>

        {/* Survey History Section */}
        <div className="mt-6 pt-6 border-t border-gov-neutral-200">
          <h4 className="text-xs font-bold uppercase tracking-wider text-gov-neutral-500 mb-3">
            Completed Survey Records
          </h4>
          {surveyResponsesQuery.isLoading ? (
            <div className="py-4 flex justify-center">
              <LoadingSpinner size="sm" />
            </div>
          ) : surveyResponsesQuery.isError ? (
            <p className="text-xs text-gov-neutral-500 italic">
              Survey response records are accessible during active verification campaigns.
            </p>
          ) : pastSurveys.length > 0 ? (
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
          ) : (
            <p className="text-xs text-gov-neutral-500 italic">
              No previous survey responses recorded for your profile.
            </p>
          )}
        </div>
      </section>
    </div>
  );
};

export default TraineeJourneyPage;
