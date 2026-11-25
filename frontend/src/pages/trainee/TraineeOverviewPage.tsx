import React from 'react';
import { useOutletContext, Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import type { TraineeProfile } from '@/types/trainee';
import {
  getTraineeEnrollments,
  getTraineeAttendance,
  getTraineeConsents,
  getTraineeOutcomeSummary,
  getRecordedCareerStatus,
} from '@/api/traineeApi';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { EmptyState } from '@/components/feedback/EmptyState';
import { ReportedCareer } from '@/components/career/ReportedCareer';
import { OwnCareerSkills } from '@/components/career/CareerSkillsPanel';
import { calculateAttendanceStats } from './TraineeTrainingPage';
import {
  resolveEffectiveConsent,
  CONSENT_TYPES_CATALOG,
} from './TraineeConsentPage';

interface TraineeContextType {
  trainee: TraineeProfile | null;
  traineeId?: number;
  isUnlinked: boolean;
  isLoading: boolean;
  error: any;
  refetch: () => Promise<unknown>;
}

export const TraineeOverviewPage: React.FC = () => {
  const { trainee, traineeId, isUnlinked, isLoading: isIdentityLoading } =
    useOutletContext<TraineeContextType>();

  // Fetch Enrollments (swallow 403 gracefully if permissions not granted to trainee role)
  const enrollmentsQuery = useQuery({
    queryKey: ['trainee', 'enrollments', traineeId],
    queryFn: () => getTraineeEnrollments(traineeId!),
    enabled: Boolean(traineeId),
    retry: false,
  });

  // Fetch Attendance records
  const attendanceQuery = useQuery({
    queryKey: ['trainee', 'attendance', traineeId],
    queryFn: () => getTraineeAttendance(traineeId!),
    enabled: Boolean(traineeId),
    retry: false,
  });

  // Fetch DPDP Consents
  const consentsQuery = useQuery({
    queryKey: ['trainee', 'consents', traineeId],
    queryFn: () => getTraineeConsents(traineeId!),
    enabled: Boolean(traineeId),
    retry: false,
  });

  // Fetch Career Outcomes
  const outcomesQuery = useQuery({
    queryKey: ['trainee', 'outcomes', traineeId],
    queryFn: () => getTraineeOutcomeSummary(traineeId!),
    enabled: Boolean(traineeId),
    retry: false,
  });
  const career = useQuery({
    queryKey: ['trainee', 'recorded-career-status', traineeId],
    queryFn: ({ signal }) => getRecordedCareerStatus(signal),
    enabled: Boolean(traineeId),
    retry: false,
    refetchOnWindowFocus: 'always',
  });

  if (isIdentityLoading) {
    return (
      <div className="bg-white rounded-lg p-12 border border-gov-neutral-200 flex flex-col items-center justify-center">
        <LoadingSpinner size="lg" />
        <p className="text-gov-neutral-600 mt-4 text-sm font-medium">
          Loading Candidate Profile &amp; Dashboard...
        </p>
      </div>
    );
  }

  if (isUnlinked || !trainee) {
    return (
      <div className="space-y-6">
        <div className="bg-white rounded-lg p-8 border border-gov-neutral-200">
          <EmptyState
            title="Candidate Profile Not Linked"
            description="Your user credentials are valid, but no linked Trainee record was found. Please contact your training center administrator to bind your candidate ID."
          />
        </div>
      </div>
    );
  }

  // Calculate attendance statistics if records are available
  const attendanceList = attendanceQuery.data ?? [];
  const { totalSessions, presentCount, attendanceRate } =
    calculateAttendanceStats(attendanceList);

  // Active enrollments
  const enrollments = enrollmentsQuery.data?.content ?? [];
  const activeEnrollment = enrollments.find(
    (e) => !e.isTerminal && !e.isCompletedFlag,
  ) ?? enrollments[0];

  // Consents statistics using authoritative resolution
  const consents = consentsQuery.data ?? [];
  const activeConsentsCount = CONSENT_TYPES_CATALOG.filter(
    (c) => resolveEffectiveConsent(consents, c.code, c.id).isCurrentlyGranted,
  ).length;

  // Outcome
  const outcome = outcomesQuery.data;

  return (
    <div className="space-y-6">
      {/* Top Profile Summary Card */}
      <section
        aria-labelledby="candidate-profile-heading"
        className="bg-white rounded-lg shadow-sm border border-gov-neutral-200 p-6"
      >
        <div className="flex flex-col md:flex-row md:items-center justify-between pb-4 border-b border-gov-neutral-200 gap-4">
          <div>
            <h2
              id="candidate-profile-heading"
              className="text-lg font-bold text-gov-neutral-900"
            >
              Candidate Profile &amp; Identity
            </h2>
          </div>
          <Link to="/trainee/profile" className="btn-outline w-fit">Edit profile</Link>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-6 pt-5">
          <div>
            <dt className="text-xs font-medium text-gov-neutral-500 uppercase tracking-wider">
              Registration Number
            </dt>
            <dd className="text-sm font-bold text-gov-neutral-900 font-mono mt-1">
              {trainee.registrationNumber}
            </dd>
          </div>
          <div>
            <dt className="text-xs font-medium text-gov-neutral-500 uppercase tracking-wider">
              Full Name
            </dt>
            <dd className="text-sm font-semibold text-gov-neutral-900 mt-1">
              {trainee.firstName} {trainee.middleName ? `${trainee.middleName} ` : ''}
              {trainee.lastName ?? ''}
            </dd>
          </div>
          <div>
            <dt className="text-xs font-medium text-gov-neutral-500 uppercase tracking-wider">
              Email Address
            </dt>
            <dd className="text-sm text-gov-neutral-700 mt-1 truncate">
              {trainee.email || '—'}
            </dd>
          </div>
          <div>
            <dt className="text-xs font-medium text-gov-neutral-500 uppercase tracking-wider">
              Phone Number
            </dt>
            <dd className="text-sm text-gov-neutral-700 mt-1">
              {trainee.phone || '—'}
            </dd>
          </div>
        </div>
      </section>

      {/* KPI Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Active Enrollment Card */}
        <div className="bg-white rounded-lg shadow-sm border border-gov-neutral-200 p-6 flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between mb-3">
              <span className="text-xs font-bold uppercase tracking-wider text-gov-neutral-500">
                Current Training
              </span>
              <span className="text-xs font-semibold px-2 py-0.5 rounded bg-blue-50 text-blue-700 border border-blue-200">
                Enrollment
              </span>
            </div>
            {enrollmentsQuery.isLoading ? (
              <LoadingSpinner size="sm" />
            ) : enrollmentsQuery.isError ? (
              <div className="text-xs text-amber-700 bg-amber-50 p-2.5 rounded border border-amber-200">
                <span>Administrative live-sync notice: Enrollment records require center coordination.</span>
              </div>
            ) : activeEnrollment ? (
              <div className="space-y-1">
                <p className="text-base font-bold text-gov-neutral-900">
                  {activeEnrollment.courseName || `Course #${activeEnrollment.courseId}`}
                </p>
                <p className="text-xs text-gov-neutral-600">
                  {activeEnrollment.centerName || 'Training Center'}
                </p>
                <p className="text-xs text-gov-neutral-500 font-mono mt-2">
                  Batch: {activeEnrollment.batchCode || 'Standard Batch'}
                </p>
              </div>
            ) : (
              <p className="text-sm text-gov-neutral-500 italic">
                No active training enrollments on record.
              </p>
            )}
          </div>
          <div className="mt-4 pt-3 border-t border-gov-neutral-100">
            <Link
              to="/trainee/training"
              className="text-xs font-semibold text-gov-navy hover:underline inline-flex items-center gap-1"
            >
              View Training Details &rarr;
            </Link>
          </div>
        </div>

        {/* Attendance Percentage Card */}
        <div className="bg-white rounded-lg shadow-sm border border-gov-neutral-200 p-6 flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between mb-3">
              <span className="text-xs font-bold uppercase tracking-wider text-gov-neutral-500">
                Attendance Standing
              </span>
              <span className="text-xs font-semibold px-2 py-0.5 rounded bg-blue-50 text-blue-700 border border-blue-200">
                Recorded Sessions
              </span>
            </div>
            {attendanceQuery.isLoading ? (
              <LoadingSpinner size="sm" />
            ) : attendanceQuery.isError ? (
              <div className="text-xs text-amber-700 bg-amber-50 p-2.5 rounded border border-amber-200">
                <span>Attendance view restricted to authorized training center staff.</span>
              </div>
            ) : attendanceRate !== null ? (
              <div>
                <div className="flex items-baseline gap-2">
                  <span className="text-3xl font-extrabold text-gov-neutral-900">
                    {attendanceRate}%
                  </span>
                  <span className="text-xs text-gov-neutral-500">
                    ({presentCount}/{totalSessions} sessions attended)
                  </span>
                </div>
                <div className="w-full bg-gov-neutral-100 rounded-full h-2 mt-3 overflow-hidden">
                  <div
                    className="h-2 rounded-full bg-gov-navy"
                    style={{ width: `${Math.min(attendanceRate, 100)}%` }}
                  />
                </div>
              </div>
            ) : (
              <p className="text-sm text-gov-neutral-500 italic">
                No session attendance records logged.
              </p>
            )}
          </div>
          <div className="mt-4 pt-3 border-t border-gov-neutral-100">
            <Link
              to="/trainee/training"
              className="text-xs font-semibold text-gov-navy hover:underline inline-flex items-center gap-1"
            >
              Inspect Attendance Log &rarr;
            </Link>
          </div>
        </div>

        {/* DPDP Consent Status Card */}
        <div className="bg-white rounded-lg shadow-sm border border-gov-neutral-200 p-6 flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between mb-3">
              <span className="text-xs font-bold uppercase tracking-wider text-gov-neutral-500">
                DPDP Privacy Status
              </span>
              <span className="text-xs font-semibold px-2 py-0.5 rounded bg-emerald-50 text-emerald-700 border border-emerald-200">
                Act 2023 Sec. 6
              </span>
            </div>
            {consentsQuery.isLoading ? (
              <LoadingSpinner size="sm" />
            ) : consentsQuery.isError ? (
              <p className="text-xs text-red-600">Unable to query consent status.</p>
            ) : (
              <div className="space-y-1">
                <p className="text-2xl font-extrabold text-gov-neutral-900">
                  {activeConsentsCount}{' '}
                  <span className="text-sm font-normal text-gov-neutral-500">
                    Active Consents
                  </span>
                </p>
                <p className="text-xs text-gov-neutral-600">
                  Data processing &amp; operational notifications are managed under your DPDP consent preferences.
                </p>
              </div>
            )}
          </div>
          <div className="mt-4 pt-3 border-t border-gov-neutral-100">
            <Link
              to="/trainee/consent"
              className="text-xs font-semibold text-gov-navy hover:underline inline-flex items-center gap-1"
            >
              Manage Consents &rarr;
            </Link>
          </div>
        </div>
      </div>

      {/* Career Outcomes & Longitudinal Tracking Section */}
      <section
        aria-labelledby="career-outcome-heading"
        className="bg-white rounded-lg shadow-sm border border-gov-neutral-200 p-6"
      >
        <div className="flex items-center justify-between pb-4 border-b border-gov-neutral-200 mb-4">
          <div>
            <h3
              id="career-outcome-heading"
              className="text-base font-bold text-gov-neutral-900"
            >
              Post-Training Placement &amp; Longitudinal Career Outcome
            </h3>
            <p className="text-xs text-gov-neutral-500">
              Summary of the placement and employment records linked to your profile.
            </p>
          </div>
          <Link
            to="/trainee/journey"
            className="text-xs font-semibold text-gov-navy hover:underline"
          >
            Full Career Journey &rarr;
          </Link>
        </div>

        {career.data?.reportedUpdate && <div className="mb-4"><ReportedCareer update={career.data.reportedUpdate} /></div>}
        {outcomesQuery.isLoading ? (
          <div className="py-6 flex justify-center">
            <LoadingSpinner size="sm" />
          </div>
        ) : outcomesQuery.isError ? (
          <div role="alert" className="text-sm text-red-800">
            Employment outcomes could not be loaded.{' '}
            <button type="button" className="underline" onClick={() => void outcomesQuery.refetch()}>Retry</button>
          </div>
        ) : outcome ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-6">
            {!career.data?.reportedUpdate && <div>
              <span className="text-xs font-medium text-gov-neutral-500 uppercase tracking-wider">
                Employment status
              </span>
              <p className="text-sm font-bold text-gov-neutral-900 mt-1">
                {career.data?.status.name ?? outcome.snapshotEmploymentStatusCode?.replace(/_/g, ' ') ?? 'Unknown'}
              </p>
            </div>}
            <div>
              <span className="text-xs font-medium text-gov-neutral-500 uppercase tracking-wider">
                Enrollments with a placement
              </span>
              <p className="text-sm font-semibold text-gov-neutral-800 mt-1">
                {outcome.placedEnrollmentCount ?? '—'}
              </p>
            </div>
            <div>
              <span className="text-xs font-medium text-gov-neutral-500 uppercase tracking-wider">
                Current wage employment records
              </span>
              <p className="text-sm font-semibold text-gov-neutral-800 mt-1">
                {outcome.currentWageEmploymentCount ?? '—'}
              </p>
            </div>
            <div>
              <span className="text-xs font-medium text-gov-neutral-500 uppercase tracking-wider">
                Current self-employment records
              </span>
              <p className="text-sm text-gov-neutral-700 mt-1">
                {outcome.currentSelfEmploymentCount ?? '—'}
              </p>
            </div>
          </div>
        ) : (
          <p className="text-sm text-gov-neutral-500 italic py-2">
            No employment outcome records registered yet.
          </p>
        )}
      </section>
      {traineeId && <OwnCareerSkills traineeId={traineeId} />}
    </div>
  );
};

export default TraineeOverviewPage;
