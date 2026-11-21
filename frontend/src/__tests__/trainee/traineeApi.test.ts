import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import {
  getTraineeByUserId,
  getTraineeById,
  getTraineeEnrollments,
  getTraineeAttendance,
  getTraineeConsents,
  createTraineeConsent,
  updateTraineeConsentStatus,
  getTraineeAssessments,
  getTraineeCertifications,
  downloadCertificatePdf,
  getTraineeOutcomeSummary,
  getTraineeSkillGapProfile,
  acceptSkillGapRecommendation,
  rejectSkillGapRecommendation,
  getTraineeSurveyResponses,
  submitSurveyResponse,
  submitSurveyResponseAnswer,
} from '@/api/traineeApi';
import { apiClient } from '@/api/client';

describe('Trainee Domain API Services (traineeApi)', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('getTraineeByUserId fetches trainee profile by userId query parameter', async () => {
    const mockTrainee = {
      id: 42,
      userId: 7,
      registrationNumber: 'TRN-2026-0042',
      firstName: 'Ramesh',
      lastName: 'Kumar',
    };

    const getSpy = vi.spyOn(apiClient, 'get').mockResolvedValueOnce({
      data: {
        success: true,
        data: mockTrainee,
        correlationId: 'test-corr-1',
      },
      status: 200,
    } as any);

    const result = await getTraineeByUserId(7);

    expect(getSpy).toHaveBeenCalledWith('/api/v1/trainees', {
      params: { userId: 7 },
    });
    expect(result).toEqual(mockTrainee);
  });

  it('getTraineeById fetches trainee profile by primary key', async () => {
    const mockTrainee = {
      id: 42,
      registrationNumber: 'TRN-2026-0042',
      firstName: 'Ramesh',
    };

    const getSpy = vi.spyOn(apiClient, 'get').mockResolvedValueOnce({
      data: {
        success: true,
        data: mockTrainee,
        correlationId: 'test-corr-2',
      },
      status: 200,
    } as any);

    const result = await getTraineeById(42);

    expect(getSpy).toHaveBeenCalledWith('/api/v1/trainees/42', undefined);
    expect(result).toEqual(mockTrainee);
  });

  it('getTraineeEnrollments fetches paginated enrollments for traineeId', async () => {
    const mockPaged = {
      content: [{ id: 101, enrollmentNumber: 'ENR-001', courseName: 'Solar PV Tech' }],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
      first: true,
      last: true,
    };

    const getSpy = vi.spyOn(apiClient, 'get').mockResolvedValueOnce({
      data: {
        success: true,
        data: mockPaged,
      },
      status: 200,
    } as any);

    const result = await getTraineeEnrollments(42, 1, 10);

    expect(getSpy).toHaveBeenCalledWith('/api/v1/training-enrollments', {
      params: { traineeId: 42, page: 1, size: 10 },
    });
    expect(result).toEqual(mockPaged);
  });

  it('getTraineeAttendance fetches attendance records list for traineeId', async () => {
    const mockRecords = [
      { id: 201, sessionDate: '2026-11-01', countsAsPresent: true, attendanceStatusCode: 'PRESENT' },
    ];

    const getSpy = vi.spyOn(apiClient, 'get').mockResolvedValueOnce({
      data: {
        success: true,
        data: mockRecords,
      },
      status: 200,
    } as any);

    const result = await getTraineeAttendance(42);

    expect(getSpy).toHaveBeenCalledWith('/api/v1/attendance-records', {
      params: { traineeId: 42 },
    });
    expect(result).toEqual(mockRecords);
  });

  it('getTraineeConsents fetches statutory consents list for traineeId', async () => {
    const mockConsents = [
      { id: 301, consentTypeCode: 'DATA_PROCESSING', consentStatusCode: 'GRANTED' },
    ];

    const getSpy = vi.spyOn(apiClient, 'get').mockResolvedValueOnce({
      data: {
        success: true,
        data: mockConsents,
      },
      status: 200,
    } as any);

    const result = await getTraineeConsents(42);

    expect(getSpy).toHaveBeenCalledWith('/api/v1/consents', {
      params: { traineeId: 42 },
    });
    expect(result).toEqual(mockConsents);
  });

  it('createTraineeConsent posts new consent payload', async () => {
    const payload = {
      traineeId: 42,
      consentTypeId: 1,
      consentStatusId: 1,
      purpose: 'Process registration and training records',
      policyVersion: 'v1.0',
    };

    const postSpy = vi.spyOn(apiClient, 'post').mockResolvedValueOnce({
      data: {
        success: true,
        data: { id: 302, ...payload, consentStatusCode: 'GRANTED' },
      },
      status: 201,
    } as any);

    const result = await createTraineeConsent(payload);

    expect(postSpy).toHaveBeenCalledWith('/api/v1/consents', payload, undefined);
    expect(result.id).toBe(302);
  });

  it('updateTraineeConsentStatus puts updated status payload to /api/v1/consents/{id}/status', async () => {
    const payload = {
      consentStatusId: 2,
      revokedAt: '2026-11-21T10:00:00Z',
    };

    const putSpy = vi.spyOn(apiClient, 'put').mockResolvedValueOnce({
      data: {
        success: true,
        data: { id: 301, consentStatusId: 2, consentStatusCode: 'REVOKED' },
      },
      status: 200,
    } as any);

    const result = await updateTraineeConsentStatus(301, payload);

    expect(putSpy).toHaveBeenCalledWith('/api/v1/consents/301/status', payload, undefined);
    expect(result.consentStatusCode).toBe('REVOKED');
  });

  it('getTraineeAssessments fetches assessment results list', async () => {
    const mockAssessments = [
      { id: 401, assessmentName: 'Practical Solar Installation', percentage: 88, outcomeCode: 'PASS' },
    ];

    const getSpy = vi.spyOn(apiClient, 'get').mockResolvedValueOnce({
      data: {
        success: true,
        data: mockAssessments,
      },
      status: 200,
    } as any);

    const result = await getTraineeAssessments(42);

    expect(getSpy).toHaveBeenCalledWith('/api/v1/assessment-results', {
      params: { traineeId: 42 },
    });
    expect(result).toEqual(mockAssessments);
  });

  it('getTraineeCertifications fetches certifications list', async () => {
    const mockCerts = [
      { id: 501, certificateNumber: 'CERT-2026-00501', courseName: 'Solar PV Tech' },
    ];

    const getSpy = vi.spyOn(apiClient, 'get').mockResolvedValueOnce({
      data: {
        success: true,
        data: mockCerts,
      },
      status: 200,
    } as any);

    const result = await getTraineeCertifications(42);

    expect(getSpy).toHaveBeenCalledWith('/api/v1/certifications', {
      params: { traineeId: 42 },
    });
    expect(result).toEqual(mockCerts);
  });

  it('downloadCertificatePdf validates PDF magic bytes and returns blob when valid', async () => {
    const pdfBlob = new Blob(['%PDF-1.4 test certificate binary content'], { type: 'application/pdf' });

    vi.spyOn(apiClient, 'get').mockResolvedValueOnce({
      data: pdfBlob,
      status: 200,
    } as any);

    const result = await downloadCertificatePdf(501);

    expect(result.success).toBe(true);
    expect(result.blob).toBeDefined();
  });

  it('downloadCertificatePdf returns descriptive error when payload does not contain PDF header', async () => {
    const nonPdfBlob = new Blob(['{"error":"Not a PDF"}'], { type: 'application/json' });

    vi.spyOn(apiClient, 'get').mockResolvedValueOnce({
      data: nonPdfBlob,
      status: 200,
    } as any);

    const result = await downloadCertificatePdf(501);

    expect(result.success).toBe(false);
    expect(result.message).toContain('non-PDF payload');
  });

  it('downloadCertificatePdf catches HTTP 404 or network errors gracefully', async () => {
    vi.spyOn(apiClient, 'get').mockRejectedValueOnce(new Error('Endpoint not implemented'));

    const result = await downloadCertificatePdf(501);

    expect(result.success).toBe(false);
    expect(result.message).toContain('Endpoint not implemented');
  });

  it('getTraineeOutcomeSummary fetches longitudinal summary with traineeId query parameter', async () => {
    const mockOutcome = {
      traineeId: 42,
      snapshotIsEmployedFlag: true,
      lastVerifiedEmployerName: 'Tata Power Solar',
      monthlySalary: 28000,
    };

    const getSpy = vi.spyOn(apiClient, 'get').mockResolvedValueOnce({
      data: {
        success: true,
        data: mockOutcome,
      },
      status: 200,
    } as any);

    const result = await getTraineeOutcomeSummary(42);

    expect(getSpy).toHaveBeenCalledWith('/api/v1/analytics/summaries/trainee-outcomes', {
      params: { traineeId: 42 },
    });
    expect(result).toEqual(mockOutcome);
  });

  it('getTraineeSkillGapProfile fetches skill gap and recommendations profile', async () => {
    const mockProfile = {
      traineeId: 42,
      hasAssessment: true,
      recommendations: [{ id: 10, status: 'PROPOSED' }],
    };

    const getSpy = vi.spyOn(apiClient, 'get').mockResolvedValueOnce({
      data: {
        success: true,
        data: mockProfile,
      },
      status: 200,
    } as any);

    const result = await getTraineeSkillGapProfile(42);

    expect(getSpy).toHaveBeenCalledWith('/api/v1/workflows/skill-gap-remediation/profile', {
      params: { traineeId: 42 },
    });
    expect(result).toEqual(mockProfile);
  });

  it('getTraineeSurveyResponses fetches paged survey responses for traineeId', async () => {
    const mockPaged = {
      content: [{ id: 99, surveyId: 1, attemptNumber: 1 }],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
      first: true,
      last: true,
      hasNext: false,
      hasPrevious: false,
    };

    const getSpy = vi.spyOn(apiClient, 'get').mockResolvedValueOnce({
      data: {
        success: true,
        data: mockPaged,
      },
      status: 200,
    } as any);

    const result = await getTraineeSurveyResponses(42, 0, 20);

    expect(getSpy).toHaveBeenCalledWith('/api/v1/survey-responses', {
      params: { traineeId: 42, page: 0, size: 20 },
    });
    expect(result).toEqual(mockPaged);
  });

  it('acceptSkillGapRecommendation posts to workflow accept endpoint', async () => {
    const postSpy = vi.spyOn(apiClient, 'post').mockResolvedValueOnce({
      data: {
        success: true,
        data: { id: 10, status: 'ACCEPTED' },
      },
      status: 200,
    } as any);

    const result = await acceptSkillGapRecommendation(10, { traineeId: 42, remarks: 'Accepted' });

    expect(postSpy).toHaveBeenCalledWith(
      '/api/v1/workflows/skill-gap-remediation/recommendations/10/accept',
      { traineeId: 42, remarks: 'Accepted' },
      undefined,
    );
    expect(result.status).toBe('ACCEPTED');
  });

  it('rejectSkillGapRecommendation posts to workflow reject endpoint with mandatory remarks', async () => {
    const postSpy = vi.spyOn(apiClient, 'post').mockResolvedValueOnce({
      data: {
        success: true,
        data: { id: 10, status: 'REJECTED' },
      },
      status: 200,
    } as any);

    const result = await rejectSkillGapRecommendation(10, { traineeId: 42, remarks: 'Shift conflict' });

    expect(postSpy).toHaveBeenCalledWith(
      '/api/v1/workflows/skill-gap-remediation/recommendations/10/reject',
      { traineeId: 42, remarks: 'Shift conflict' },
      undefined,
    );
    expect(result.status).toBe('REJECTED');
  });

  it('submitSurveyResponse and submitSurveyResponseAnswer post to survey endpoints', async () => {
    const respSpy = vi.spyOn(apiClient, 'post').mockResolvedValueOnce({
      data: {
        success: true,
        data: { id: 99, surveyId: 1, traineeId: 42 },
      },
      status: 201,
    } as any);

    const response = await submitSurveyResponse({ surveyId: 1, traineeId: 42 });
    expect(respSpy).toHaveBeenCalledWith('/api/v1/survey-responses', { surveyId: 1, traineeId: 42 }, undefined);
    expect(response.id).toBe(99);

    const ansSpy = vi.spyOn(apiClient, 'post').mockResolvedValueOnce({
      data: {
        success: true,
        data: { id: 1001, surveyResponseId: 99 },
      },
      status: 201,
    } as any);

    const ansResult = await submitSurveyResponseAnswer({
      surveyResponseId: 99,
      surveyQuestionId: 1,
      answerText: 'Great program',
    });
    expect(ansSpy).toHaveBeenCalledWith(
      '/api/v1/survey-response-answers',
      { surveyResponseId: 99, surveyQuestionId: 1, answerText: 'Great program' },
      undefined,
    );
    expect(ansResult.id).toBe(1001);
  });
});
