import React, { useState } from 'react';
import { useOutletContext } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import type { TraineeProfile } from '@/types/trainee';
import {
  getTraineeOutcomeSummary,
  getTraineeSkillGapProfile,
  acceptSkillGapRecommendation,
  rejectSkillGapRecommendation,
  getEmploymentHistory,
} from '@/api/traineeApi';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { EmptyState } from '@/components/feedback/EmptyState';
import { ErrorState } from '@/components/feedback/ErrorState';
import { RetentionPanel, SkillGapInsights } from '@/components/career/CareerInsights';
import { TraineeSurveys } from './TraineeSurveys';

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

  const [employmentPage, setEmploymentPage] = useState(0);
  const [retentionEmploymentId, setRetentionEmploymentId] = useState<number>();
  const employment = useQuery({
    queryKey: ['trainee', 'employment-history', traineeId, employmentPage],
    queryFn: ({ signal }) => getEmploymentHistory(employmentPage, signal),
    enabled: Boolean(traineeId),
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

  return (
    <div className="space-y-6">
      <section className="card space-y-4" aria-labelledby="career-milestones-heading">
        <h2 id="career-milestones-heading" className="text-lg font-bold text-navy">Employment history</h2>
        {outcomesQuery.isError && (
          <ErrorState error={outcomesQuery.error} title="Unable to load current employment status" onRetry={() => void outcomesQuery.refetch()} />
        )}
        {outcome?.snapshotEmploymentStatusCode && (
          <p className="text-sm">Current status <strong className="capitalize">{outcome.snapshotEmploymentStatusCode.toLowerCase().replace(/_/g, ' ')}</strong></p>
        )}
        {employment.isLoading ? <LoadingSpinner label="Loading employment history" /> : employment.isError ? (
          <ErrorState error={employment.error} title="Unable to load employment history" onRetry={() => void employment.refetch()} />
        ) : employment.data?.content.length ? (
          <>
            <ul className="space-y-2">
              {employment.data.content.map((period) => (
                <li key={period.id} className="rounded border p-3 text-sm space-y-1">
                  <p className="font-semibold">{period.jobRole || period.employer || 'Employment record'}</p>
                  <p>{[period.jobRole && period.employer, period.engagementType?.replace(/_/g, ' '), period.status].filter(Boolean).join(' · ')}</p>
                  <p>{[period.startDate, period.endDate].filter(Boolean).join(' to ')}</p>
                  {period.verificationStatus && <p>Verification <strong>{period.verificationStatus.replace(/_/g, ' ')}</strong></p>}
                  {period.startingPay != null && (
                    <p>Starting pay {[period.currency, period.startingPay.toLocaleString('en-IN'), period.payFrequency?.toLowerCase()].filter(Boolean).join(' ')}</p>
                  )}
                  <button type="button" className="btn-secondary mt-2" aria-expanded={retentionEmploymentId === period.id}
                    onClick={() => setRetentionEmploymentId(retentionEmploymentId === period.id ? undefined : period.id)}>
                    {retentionEmploymentId === period.id ? 'Hide retention and wages' : 'View retention and wages'}
                  </button>
                  {retentionEmploymentId === period.id && <div className="mt-3 border-t pt-3"><RetentionPanel employmentId={period.id} /></div>}
                </li>
              ))}
            </ul>
            {employment.data.totalPages > 1 && (
              <div className="flex gap-2">
                <button type="button" className="btn-outline" disabled={!employment.data.hasPrevious} onClick={() => { setRetentionEmploymentId(undefined); setEmploymentPage((page) => page - 1); }}>Previous employment records</button>
                <button type="button" className="btn-outline" disabled={!employment.data.hasNext} onClick={() => { setRetentionEmploymentId(undefined); setEmploymentPage((page) => page + 1); }}>Next employment records</button>
              </div>
            )}
          </>
        ) : <p className="text-sm text-gray-600">There are no employment records linked to your profile.</p>}
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

        {skillProfile && Boolean(skillProfile.currentSkillGaps?.length) && (
          <div className="mb-4"><SkillGapInsights profile={skillProfile} /></div>
        )}
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
        ) : skillGapQuery.isError ? (
          <ErrorState error={skillGapQuery.error} title="Unable to load course recommendations" onRetry={() => void skillGapQuery.refetch()} />
        ) : recommendations.length === 0 ? (
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
                    Status: {rec.status || (rec.isAcceptedFlag === true ? 'Accepted' : 'Available')}
                  </span>
                </div>

                {!rec.isAcceptedFlag && (!rec.status || rec.status === 'PROPOSED') && (
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
                )}
              </div>
            ))}
          </div>
        )}
      </section>

      <TraineeSurveys traineeId={traineeId} />
    </div>
  );
};

export default TraineeJourneyPage;
