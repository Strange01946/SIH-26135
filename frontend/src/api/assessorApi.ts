import {
  apiGet,
  apiPost,
  apiDownload,
} from './client';
import type {
  AssessorAssessmentSummary,
  AssessorRosterResponse,
  CandidateScoreInput,
  SaveAssessmentScoresRequest,
  SaveAssessmentScoresResponse,
} from '@/types/assessor';

/**
 * Assessor Workbench API Services.
 * Strictly bound to Java Spring Boot backend endpoints under /api/v1/assessor.
 */

export interface GetAssessorAssessmentsParams {
  fromDate?: string;
  toDate?: string;
  status?: string;
  search?: string;
}

function normalizeSummary(item: AssessorAssessmentSummary): AssessorAssessmentSummary {
  const isFinal = item.isFinalized ?? (item.lifecycleStatusCode === 'FINALIZED' || item.lifecycleStatusCode === 'COMPLETED');
  return {
    ...item,
    isFinalized: Boolean(isFinal),
  };
}

/**
 * Retrieves assessments assigned to the authenticated assessor.
 * Endpoint: GET /api/v1/assessor/assessments
 */
export async function getAssessorAssessments(
  params?: GetAssessorAssessmentsParams,
): Promise<AssessorAssessmentSummary[]> {
  const list = await apiGet<AssessorAssessmentSummary[]>('/api/v1/assessor/assessments', {
    params,
  });
  return (list || []).map(normalizeSummary);
}

/**
 * Retrieves assessment header summary.
 * Endpoint: GET /api/v1/assessor/assessments/{id}
 */
export async function getAssessmentSummary(
  assessmentId: number,
): Promise<AssessorAssessmentSummary> {
  const summary = await apiGet<AssessorAssessmentSummary>(
    `/api/v1/assessor/assessments/${assessmentId}`,
  );
  return normalizeSummary(summary);
}

/**
 * Retrieves assessment candidate roster with latest scored/draft status.
 * Endpoint: GET /api/v1/assessor/assessments/{id}/roster
 */
export async function getAssessmentRoster(
  assessmentId: number,
): Promise<AssessorRosterResponse> {
  const roster = await apiGet<AssessorRosterResponse>(
    `/api/v1/assessor/assessments/${assessmentId}/roster`,
  );
  if (roster && roster.assessment) {
    const candidateFinalized = roster.candidates && roster.candidates.length > 0 && roster.candidates.every(c => c.isFinalized);
    const isFinal = roster.assessment.isFinalized ?? (
      roster.assessment.lifecycleStatusCode === 'FINALIZED' ||
      roster.assessment.lifecycleStatusCode === 'COMPLETED' ||
      candidateFinalized
    );
    roster.assessment = {
      ...roster.assessment,
      isFinalized: Boolean(isFinal),
    };
  }
  return roster;
}

/**
 * Saves draft scores (partial or full) without issuing certificates.
 * Endpoint: POST /api/v1/assessor/assessments/{id}/draft
 */
export async function saveDraftScores(
  assessmentId: number,
  candidates: CandidateScoreInput[],
): Promise<SaveAssessmentScoresResponse> {
  const payload: SaveAssessmentScoresRequest = {
    isDraft: true,
    candidates,
  };
  return apiPost<SaveAssessmentScoresRequest, SaveAssessmentScoresResponse>(
    `/api/v1/assessor/assessments/${assessmentId}/draft`,
    payload,
  );
}

/**
 * Finalizes assessment scores, evaluates PASS/FAIL, and issues certificates to passers.
 * Endpoint: POST /api/v1/assessor/assessments/{id}/finalize
 */
export async function finalizeAssessment(
  assessmentId: number,
  candidates: CandidateScoreInput[],
): Promise<SaveAssessmentScoresResponse> {
  const payload: SaveAssessmentScoresRequest = {
    isDraft: false,
    candidates,
  };
  return apiPost<SaveAssessmentScoresRequest, SaveAssessmentScoresResponse>(
    `/api/v1/assessor/assessments/${assessmentId}/finalize`,
    payload,
  );
}

/**
 * Downloads authentic certificate PDF byte stream (%PDF-1.4).
 * Endpoint: GET /api/v1/certifications/{id}/download
 */
export async function downloadCertificatePdf(
  certificationId: number,
): Promise<{ success: boolean; blob?: Blob; message?: string }> {
  try {
    const blob = await apiDownload(
      `/api/v1/certifications/${certificationId}/download`,
    );

    if (blob.type === 'application/pdf' || blob.size > 0) {
      const slice = blob.slice(0, 5);
      const text = await slice.text();
      if (text.startsWith('%PDF-')) {
        return { success: true, blob };
      }
    }

    return {
      success: false,
      message: 'Server returned a non-PDF payload for this certificate.',
    };
  } catch (error: any) {
    return {
      success: false,
      message:
        error?.message ||
        'Certificate PDF download failed or is not available.',
    };
  }
}
