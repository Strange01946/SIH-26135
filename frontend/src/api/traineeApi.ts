import {
  apiGet,
  apiGetPaged,
  apiPost,
  apiPut,
  apiDownload,
} from './client';
import type { PagedResponse } from './types';
import type {
  TraineeProfile,
  TrainingEnrollment,
  AttendanceRecord,
  TraineeConsent,
  CreateTraineeConsentPayload,
  UpdateTraineeConsentStatusPayload,
  AssessmentResult,
  Certification,
  SurveyResponse,
  CreateSurveyResponsePayload,
  CreateSurveyResponseAnswerPayload,
  TraineeOutcomeSummary,
  TraineeSkillGapProfile,
} from '@/types/trainee';

/**
 * Trainee Domain API Services
 * Strictly bound to Java Spring Boot backend endpoints.
 */

/**
 * Resolves a trainee record by linked auth User ID.
 * Backend route: GET /api/v1/trainees?userId={userId}
 * Returns 404 RESOURCE_NOT_FOUND when user is not linked to any trainee.
 */
export async function getTraineeByUserId(userId: number): Promise<TraineeProfile> {
  return apiGet<TraineeProfile>('/api/v1/trainees', {
    params: { userId },
  });
}

/**
 * Retrieves complete trainee profile by primary key identifier.
 * Backend route: GET /api/v1/trainees/{id}
 */
export async function getTraineeById(id: number): Promise<TraineeProfile> {
  return apiGet<TraineeProfile>(`/api/v1/trainees/${id}`);
}

/**
 * Retrieves paginated training enrollments for a trainee.
 * Backend route: GET /api/v1/training-enrollments?traineeId={traineeId}
 * Requires authority: enrollment.manage
 */
export async function getTraineeEnrollments(
  traineeId: number,
  page = 0,
  size = 20,
): Promise<PagedResponse<TrainingEnrollment>> {
  return apiGetPaged<TrainingEnrollment>('/api/v1/training-enrollments', {
    params: { traineeId, page, size },
  });
}

/**
 * Retrieves attendance records for a trainee.
 * Backend route: GET /api/v1/attendance-records?traineeId={traineeId}
 * Requires authority: enrollment.manage
 */
export async function getTraineeAttendance(
  traineeId: number,
): Promise<AttendanceRecord[]> {
  return apiGet<AttendanceRecord[]>('/api/v1/attendance-records', {
    params: { traineeId },
  });
}

/**
 * Retrieves all DPDP consents registered for a trainee.
 * Backend route: GET /api/v1/consents?traineeId={traineeId}
 * Requires authority: consent.manage
 */
export async function getTraineeConsents(
  traineeId: number,
): Promise<TraineeConsent[]> {
  return apiGet<TraineeConsent[]>('/api/v1/consents', {
    params: { traineeId },
  });
}

/**
 * Creates a new trainee consent record.
 * Backend route: POST /api/v1/consents
 * Requires authority: consent.manage
 */
export async function createTraineeConsent(
  payload: CreateTraineeConsentPayload,
): Promise<TraineeConsent> {
  return apiPost<CreateTraineeConsentPayload, TraineeConsent>(
    '/api/v1/consents',
    payload,
  );
}

/**
 * Updates an existing trainee consent's status (e.g. REVOKED or GRANTED).
 * Backend route: PUT /api/v1/consents/{id}/status
 * Requires authority: consent.manage
 */
export async function updateTraineeConsentStatus(
  consentId: number,
  payload: UpdateTraineeConsentStatusPayload,
): Promise<TraineeConsent> {
  return apiPut<UpdateTraineeConsentStatusPayload, TraineeConsent>(
    `/api/v1/consents/${consentId}/status`,
    payload,
  );
}

/**
 * Retrieves assessment results for a trainee.
 * Backend route: GET /api/v1/assessment-results?traineeId={traineeId}
 * Requires authority: assessment.manage
 */
export async function getTraineeAssessments(
  traineeId: number,
): Promise<AssessmentResult[]> {
  return apiGet<AssessmentResult[]>('/api/v1/assessment-results', {
    params: { traineeId },
  });
}

/**
 * Retrieves certifications awarded to a trainee.
 * Backend route: GET /api/v1/certifications?traineeId={traineeId}
 * Requires authority: assessment.manage
 */
export async function getTraineeCertifications(
  traineeId: number,
): Promise<Certification[]> {
  return apiGet<Certification[]>('/api/v1/certifications', {
    params: { traineeId },
  });
}

/**
 * Attempts to download a certificate PDF file.
 * Backend route: GET /api/v1/certifications/{id}/download
 * Performs header and magic byte validation.
 */
export async function downloadCertificatePdf(
  certificationId: number,
): Promise<{ success: boolean; blob?: Blob; message?: string }> {
  try {
    const blob = await apiDownload(
      `/api/v1/certifications/${certificationId}/download`,
    );

    // Validate that the returned blob is actually a PDF
    if (blob.type === 'application/pdf' || blob.size > 0) {
      // Validate PDF magic bytes if size is sufficient (%PDF-)
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
        'Certificate PDF generation is not available for this record.',
    };
  }
}

/**
 * Retrieves longitudinal trainee career outcome summary from the analytics view.
 * Backend route: GET /api/v1/analytics/summaries/trainee-outcomes?traineeId={traineeId}
 * Accessible by: ROLE_trainee (scoped to own traineeId) or analytics.read
 */
export async function getTraineeOutcomeSummary(
  traineeId: number,
): Promise<TraineeOutcomeSummary> {
  return apiGet<TraineeOutcomeSummary>(
    '/api/v1/analytics/summaries/trainee-outcomes',
    {
      params: { traineeId },
    },
  );
}

/**
 * Retrieves trainee skill gap and recommendation profile.
 * Backend route: GET /api/v1/workflows/skill-gap-remediation/profile?traineeId={traineeId}
 * Accessible by: assessment.manage or enrollment.manage
 */
export async function getTraineeSkillGapProfile(
  traineeId: number,
): Promise<TraineeSkillGapProfile> {
  return apiGet<TraineeSkillGapProfile>(
    '/api/v1/workflows/skill-gap-remediation/profile',
    {
      params: { traineeId },
    },
  );
}

/**
 * Accepts a skill gap remediation recommendation.
 * Backend route: POST /api/v1/workflows/skill-gap-remediation/recommendations/{id}/accept
 * Accessible by: hasRole('trainee') or assessment.manage
 */
export async function acceptSkillGapRecommendation(
  recommendationId: number,
  payload?: { traineeId?: number; remarks?: string },
): Promise<{ id: number; status: string }> {
  return apiPost<{ traineeId?: number; remarks?: string } | undefined, { id: number; status: string }>(
    `/api/v1/workflows/skill-gap-remediation/recommendations/${recommendationId}/accept`,
    payload,
  );
}

/**
 * Rejects a skill gap remediation recommendation.
 * Backend route: POST /api/v1/workflows/skill-gap-remediation/recommendations/{id}/reject
 * Accessible by: hasRole('trainee') or assessment.manage
 */
export async function rejectSkillGapRecommendation(
  recommendationId: number,
  payload: { traineeId?: number; remarks: string },
): Promise<{ id: number; status: string }> {
  return apiPost<{ traineeId?: number; remarks: string }, { id: number; status: string }>(
    `/api/v1/workflows/skill-gap-remediation/recommendations/${recommendationId}/reject`,
    payload,
  );
}

/**
 * Retrieves survey responses for a trainee.
 * Backend route: GET /api/v1/survey-responses?traineeId={traineeId}
 * Requires administrative authority: survey.manage (HTTP 403 for candidate ROLE_trainee).
 */
export async function getTraineeSurveyResponses(
  traineeId: number,
  page = 0,
  size = 20,
): Promise<PagedResponse<SurveyResponse>> {
  return apiGetPaged<SurveyResponse>('/api/v1/survey-responses', {
    params: { traineeId, page, size },
  });
}

/**
 * Submits a survey response header.
 * Backend route: POST /api/v1/survey-responses
 * Requires administrative authority: survey.manage (HTTP 403 for candidate ROLE_trainee).
 */
export async function submitSurveyResponse(
  payload: CreateSurveyResponsePayload,
): Promise<SurveyResponse> {
  return apiPost<CreateSurveyResponsePayload, SurveyResponse>(
    '/api/v1/survey-responses',
    payload,
  );
}

/**
 * Submits an individual question answer for a survey response.
 * Backend route: POST /api/v1/survey-response-answers
 * Requires administrative authority: survey.manage (HTTP 403 for candidate ROLE_trainee).
 */
export async function submitSurveyResponseAnswer(
  payload: CreateSurveyResponseAnswerPayload,
): Promise<any> {
  return apiPost<CreateSurveyResponseAnswerPayload, any>(
    '/api/v1/survey-response-answers',
    payload,
  );
}
