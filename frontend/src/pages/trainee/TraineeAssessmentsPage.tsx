import React, { useState } from 'react';
import { useOutletContext } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import type { TraineeProfile, Certification, AssessmentResult } from '@/types/trainee';
import {
  getTraineeAssessments,
  getTraineeCertifications,
  downloadCertificatePdf,
} from '@/api/traineeApi';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { EmptyState } from '@/components/feedback/EmptyState';
import { ErrorState } from '@/components/feedback/ErrorState';
import { getOwnAssessmentAppointments } from '@/api/batchAssessmentApi';
import { AssessmentDocumentButton, BatchAssessmentSummary } from '@/components/assessment/BatchAssessmentSummary';

function TraineeBatchAssessments() {
  const appointments = useQuery({ queryKey: ['trainee-batch-assessments'], queryFn: getOwnAssessmentAppointments, refetchInterval: 15_000 });
  if (appointments.data?.length === 0) return null;
  return <section className="card p-6 space-y-3" aria-labelledby="trainee-batch-assessments">
    <h3 id="trainee-batch-assessments" className="font-bold">Batch assessments</h3>
    {appointments.isLoading && <LoadingSpinner label="Loading batch assessments" />}
    {appointments.isError && <ErrorState title="Could not load batch assessments" error={appointments.error} onRetry={() => void appointments.refetch()} />}
    {appointments.data?.map((appointment) => <article className="rounded border p-4 space-y-2" key={appointment.batchCode}>
      <BatchAssessmentSummary appointment={appointment} />
      {appointment.resultId ? <>
        <p className="text-sm font-semibold">Outcome: {appointment.outcome}</p>
        {appointment.theory != null && <p className="text-sm">Theory {appointment.theory} · Practical {appointment.practical} · Viva {appointment.viva} · Total {appointment.total}</p>}
        {appointment.outcome === 'PASS' && !appointment.certificateId && (
          <p className="text-sm text-amber-800">Certificate pending issuance. Your assessment result is available.</p>
        )}
        <AssessmentDocumentButton resultId={appointment.resultId} certificate={!!appointment.certificateId} />
      </> : <p className="text-sm">{appointment.status === 'OPEN' ? 'Awaiting assessor assignment'
        : appointment.completedAt ? 'Assessment completed. Results pending.' : 'Assessment scheduled'}</p>}
    </article>)}
  </section>;
}

export type AssessmentOutcomeVerdict =
  | 'PASS'
  | 'FAIL'
  | 'PENDING'
  | 'WITHHELD'
  | 'ABSENT'
  | 'INCOMPLETE'
  | 'UNKNOWN';

export interface AssessmentOutcomeDisplay {
  verdict: AssessmentOutcomeVerdict;
  isPass: boolean;
  label: string;
  badgeClass: string;
}

/**
 * Resolves assessment outcome authoritatively from backend DTO fields.
 * Strict conformance: Never infers PASS from a score percentage alone.
 */
export function resolveAssessmentOutcome(
  result: AssessmentResult,
): AssessmentOutcomeDisplay {
  const code = (
    result.assessmentOutcomeCode ||
    result.outcomeCode ||
    ''
  )
    .trim()
    .toUpperCase();

  // 1. Authoritative non-passing status codes take absolute precedence over isPassFlag
  if (code === 'PENDING') {
    return {
      verdict: 'PENDING',
      isPass: false,
      label:
        result.assessmentOutcomeName ||
        result.outcomeName ||
        'PENDING EVALUATION',
      badgeClass: 'bg-blue-50 text-blue-700 border border-blue-200',
    };
  }

  if (code === 'WITHHELD') {
    return {
      verdict: 'WITHHELD',
      isPass: false,
      label: result.assessmentOutcomeName || result.outcomeName || 'WITHHELD',
      badgeClass: 'bg-amber-50 text-amber-700 border border-amber-200',
    };
  }

  if (code === 'ABSENT') {
    return {
      verdict: 'ABSENT',
      isPass: false,
      label: result.assessmentOutcomeName || result.outcomeName || 'ABSENT',
      badgeClass: 'bg-gov-neutral-100 text-gov-neutral-600 border border-gov-neutral-300',
    };
  }

  if (code === 'INCOMPLETE') {
    return {
      verdict: 'INCOMPLETE',
      isPass: false,
      label:
        result.assessmentOutcomeName ||
        result.outcomeName ||
        'INCOMPLETE',
      badgeClass: 'bg-amber-50 text-amber-700 border border-amber-200',
    };
  }

  // 2. Explicit failure: outcome code FAIL or isPassFlag === false
  // If code === 'PASS' but isPassFlag === false, isPassFlag=false denies pass.
  if (code === 'FAIL' || result.isPassFlag === false) {
    return {
      verdict: 'FAIL',
      isPass: false,
      label:
        (code !== 'PASS' && (result.assessmentOutcomeName || result.outcomeName)) ||
        'FAIL',
      badgeClass: 'bg-red-50 text-red-700 border border-red-200',
    };
  }

  // 3. Passing outcome: requires code === 'PASS' or isPassFlag === true
  // (with isPassFlag not being false and code not being a non-pass code)
  if (code === 'PASS' || result.isPassFlag === true) {
    return {
      verdict: 'PASS',
      isPass: true,
      label: result.assessmentOutcomeName || result.outcomeName || 'PASS',
      badgeClass: 'bg-emerald-50 text-emerald-700 border border-emerald-200',
    };
  }

  // 4. Incomplete or unknown DTO (never infer PASS from percentage)
  return {
    verdict: 'UNKNOWN',
    isPass: false,
    label:
      result.assessmentOutcomeName ||
      result.outcomeName ||
      'AWAITING EVALUATION',
    badgeClass: 'bg-gov-neutral-100 text-gov-neutral-700 border border-gov-neutral-300',
  };
}

interface TraineeContextType {
  trainee: TraineeProfile | null;
  traineeId?: number;
  isUnlinked: boolean;
  isLoading: boolean;
}

export const TraineeAssessmentsPage: React.FC = () => {
  const { traineeId, isUnlinked, isLoading: isIdentityLoading } =
    useOutletContext<TraineeContextType>();

  const [downloadingCertId, setDownloadingCertId] = useState<number | null>(null);
  const [downloadStatus, setDownloadStatus] = useState<{
    certId: number;
    success: boolean;
    message: string;
  } | null>(null);

  // Query assessments
  const assessmentsQuery = useQuery({
    queryKey: ['trainee', 'assessments', traineeId],
    queryFn: () => getTraineeAssessments(traineeId!),
    enabled: Boolean(traineeId),
    retry: false,
  });

  // Query certifications
  const certificationsQuery = useQuery({
    queryKey: ['trainee', 'certifications', traineeId],
    queryFn: () => getTraineeCertifications(traineeId!),
    enabled: Boolean(traineeId),
    refetchInterval: 15_000,
    retry: false,
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
          title="Assessment Records Restricted"
          description="A linked candidate identification record is required to view examination scorecards and NCVET certificates."
        />
      </div>
    );
  }

  const assessmentResults = assessmentsQuery.data ?? [];
  const certifications = certificationsQuery.data ?? [];

  // Handler for PDF download
  const handleDownload = async (cert: Certification) => {
    setDownloadingCertId(cert.id);
    setDownloadStatus(null);
    try {
      const result = await downloadCertificatePdf(cert.id);
      if (result.success && result.blob) {
        const url = window.URL.createObjectURL(result.blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `Certificate_${cert.certificateNumber || cert.id}.pdf`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);

        setDownloadStatus({
          certId: cert.id,
          success: true,
          message: 'Certificate PDF downloaded successfully.',
        });
      } else {
        setDownloadStatus({
          certId: cert.id,
          success: false,
          message:
            result.message ||
            'Unable to download the certificate PDF.',
        });
      }
    } catch (err: any) {
      setDownloadStatus({
        certId: cert.id,
        success: false,
        message: err?.message || 'Certificate PDF download failed.',
      });
    } finally {
      setDownloadingCertId(null);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header Summary */}
      <section
        aria-labelledby="assessments-header-heading"
        className="bg-white rounded-lg shadow-sm border border-gov-neutral-200 p-6"
      >
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <h2
              id="assessments-header-heading"
              className="text-lg font-bold text-gov-neutral-900"
            >
              NCVET Assessments &amp; Certified Credentials
            </h2>
            <p className="text-xs text-gov-neutral-500">
              Assessment scores, outcomes, and certificates issued by your assessor.
            </p>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-xs font-semibold px-3 py-1 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200">
              {certificationsQuery.isSuccess ? certifications.length : '—'} Credentials Awarded
            </span>
          </div>
        </div>
      </section>

      {/* Download Status Notification */}
      {downloadStatus && (
        <div
          role="alert"
          className={`p-4 rounded-lg text-sm border flex items-center justify-between ${
            downloadStatus.success
              ? 'bg-emerald-50 border-emerald-200 text-emerald-900'
              : 'bg-amber-50 border-amber-200 text-amber-900'
          }`}
        >
          <span>{downloadStatus.message}</span>
          <button
            type="button"
            onClick={() => setDownloadStatus(null)}
            className="text-xs font-bold hover:underline ml-4"
          >
            Dismiss
          </button>
        </div>
      )}

      <TraineeBatchAssessments />
      {/* Certifications Section */}
      <section
        aria-labelledby="certifications-heading"
        className="bg-white rounded-lg shadow-sm border border-gov-neutral-200 p-6"
      >
        <div className="pb-4 border-b border-gov-neutral-200 mb-6">
          <h3
            id="certifications-heading"
            className="text-base font-bold text-gov-neutral-900"
          >
            Awarded Certifications &amp; Diplomas
          </h3>
          <p className="text-xs text-gov-neutral-500">
            Official verifiable qualification certificates recognized across India under NSQF.
          </p>
        </div>

        {certificationsQuery.isLoading ? (
          <div className="py-8 flex justify-center">
            <LoadingSpinner size="md" />
          </div>
        ) : certificationsQuery.isError ? (
          <ErrorState error={certificationsQuery.error} title="Unable to load certificates" onRetry={() => void certificationsQuery.refetch()} />
        ) : certifications.length === 0 ? (
          <EmptyState
            title="No Certificates Found"
            description="No certifications have been issued yet for this candidate. Certificates are generated after assessment evaluation."
          />
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {certifications.map((cert) => (
              <div
                key={cert.id}
                className="border border-gov-neutral-200 rounded-lg p-5 bg-gradient-to-br from-white to-gov-neutral-50 shadow-sm flex flex-col justify-between"
              >
                <div>
                  <div className="flex items-start justify-between gap-2">
                    <div>
                      <span className="text-xs font-mono font-bold text-gov-neutral-500 uppercase">
                        Cert No: {cert.certificateNumber}
                      </span>
                      <h4 className="text-base font-bold text-gov-neutral-900 mt-1">
                        {cert.courseName || `Course #${cert.courseId}`}
                      </h4>
                      <p className="text-xs text-gov-neutral-600 mt-0.5">
                        Program: {cert.programName || '—'}
                      </p>
                    </div>
                    <span className={`text-xs font-semibold px-2 py-0.5 rounded border shrink-0 ${
                      cert.certificateStatusCode === 'ISSUED'
                        ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                        : 'bg-gov-neutral-50 text-gov-neutral-700 border-gov-neutral-200'
                    }`}>
                      {cert.certificateStatusName || cert.certificateStatusCode || 'Status unavailable'}
                    </span>
                  </div>

                  <div className="grid grid-cols-2 gap-3 mt-4 pt-3 border-t border-gov-neutral-200 text-xs text-gov-neutral-600">
                    <div>
                      <span className="font-medium text-gov-neutral-500">Issued by</span>{' '}
                      <span className="font-semibold">{cert.issuingBody || '—'}</span>
                    </div>
                    <div>
                      <span className="font-medium text-gov-neutral-500">Issued on</span>{' '}
                      <span>{cert.issueDate ? new Date(cert.issueDate).toLocaleDateString() : '—'}</span>
                    </div>
                  </div>
                  {cert.certificateVerificationStatusCode === 'REJECTED' && (
                    <p className="mt-2 text-xs font-semibold text-red-800">Verification rejected</p>
                  )}
                </div>

                <div className="mt-5 pt-3 border-t border-gov-neutral-200 flex items-center justify-between">
                  <span className="text-[11px] text-gov-neutral-400 font-mono">
                    ID #{cert.id}
                  </span>
                  <button
                    type="button"
                    disabled={downloadingCertId === cert.id}
                    onClick={() => handleDownload(cert)}
                    className="px-3.5 py-1.5 text-xs font-bold rounded bg-gov-navy text-white hover:bg-gov-navy-light focus:outline-none focus:ring-2 focus:ring-gov-navy focus:ring-offset-2 disabled:opacity-50 transition-colors inline-flex items-center gap-1.5"
                  >
                    {downloadingCertId === cert.id ? (
                      <>
                        <LoadingSpinner size="sm" />
                          <span>Downloading…</span>
                      </>
                    ) : (
                      <>
                        <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
                        </svg>
                        <span>Download Certificate</span>
                      </>
                    )}
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>

      {/* Assessment Results Section */}
      <section
        aria-labelledby="assessment-results-heading"
        className="bg-white rounded-lg shadow-sm border border-gov-neutral-200 overflow-hidden"
      >
        <div className="p-6 border-b border-gov-neutral-200">
          <h3
            id="assessment-results-heading"
            className="text-base font-bold text-gov-neutral-900"
          >
            Assessment Evaluations &amp; Scorecards
          </h3>
          <p className="text-xs text-gov-neutral-500">
            Official grading and competency outcome marks scored across practical and theory examinations.
          </p>
        </div>

        {assessmentsQuery.isLoading ? (
          <div className="py-8 flex justify-center">
            <LoadingSpinner size="md" />
          </div>
        ) : assessmentsQuery.isError ? (
          <ErrorState error={assessmentsQuery.error} title="Unable to load assessment results" onRetry={() => void assessmentsQuery.refetch()} />
        ) : assessmentResults.length === 0 ? (
          <div className="p-8">
            <EmptyState
              title="No Assessment Records Found"
              description="No formal examination results have been submitted for this candidate yet."
            />
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-xs md:text-sm">
              <thead className="bg-gov-neutral-50 border-b border-gov-neutral-200 text-gov-neutral-700 uppercase tracking-wider text-xs">
                <tr>
                  <th scope="col" className="px-6 py-3 font-semibold">
                    Assessment Module
                  </th>
                  <th scope="col" className="px-6 py-3 font-semibold">
                    Date
                  </th>
                  <th scope="col" className="px-6 py-3 font-semibold">
                    Score / Max
                  </th>
                  <th scope="col" className="px-6 py-3 font-semibold">
                    Percentage
                  </th>
                  <th scope="col" className="px-6 py-3 font-semibold">
                    Grade
                  </th>
                  <th scope="col" className="px-6 py-3 font-semibold">
                    Outcome
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gov-neutral-200 font-sans">
                {assessmentResults.map((result) => {
                  const outcome = resolveAssessmentOutcome(result);
                  const obtained = result.obtainedScore ?? result.totalScore ?? '—';
                  const maximum = result.maximumScore ?? result.maxScore ?? '—';
                  const pct = result.scorePercentage ?? result.percentage;
                  const dateStr = result.resultDeclaredAt || result.assessedAt;

                  return (
                    <tr key={result.id} className="hover:bg-gov-neutral-50 transition-colors">
                      <td className="px-6 py-3.5 font-bold text-gov-neutral-900 whitespace-nowrap">
                        {result.assessmentName || `Assessment #${result.assessmentId || result.id}`}
                      </td>
                      <td className="px-6 py-3.5 text-gov-neutral-600 whitespace-nowrap">
                        {dateStr ? new Date(dateStr).toLocaleDateString() : '—'}
                      </td>
                      <td className="px-6 py-3.5 font-mono text-gov-neutral-800 whitespace-nowrap">
                        {obtained} / {maximum}
                      </td>
                      <td className="px-6 py-3.5 font-bold text-gov-neutral-900 whitespace-nowrap font-mono">
                        {pct !== null && pct !== undefined ? `${pct}%` : '—'}
                      </td>
                      <td className="px-6 py-3.5 font-semibold text-gov-neutral-700 whitespace-nowrap">
                        {result.grade || '—'}
                      </td>
                      <td className="px-6 py-3.5 whitespace-nowrap">
                        <span
                          className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-bold ${outcome.badgeClass}`}
                        >
                          {outcome.label}
                        </span>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
};

export default TraineeAssessmentsPage;
