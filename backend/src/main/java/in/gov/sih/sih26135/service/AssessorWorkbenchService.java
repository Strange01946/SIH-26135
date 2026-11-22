package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.assessor.AssessorAssessmentSummaryResponse;
import in.gov.sih.sih26135.dto.assessor.AssessorRosterResponse;
import in.gov.sih.sih26135.dto.assessor.SaveAssessmentScoresRequest;
import in.gov.sih.sih26135.dto.assessor.SaveAssessmentScoresResponse;
import java.time.LocalDate;
import java.util.List;

/**
 * Service interface for Phase 30.6 Assessor Workbench:
 * Assessment scheduling, candidate roster management, draft scoring, finalization, and certification.
 */
public interface AssessorWorkbenchService {

  /**
   * Retrieves all assessments assigned to the authenticated assessor.
   */
  List<AssessorAssessmentSummaryResponse> getAssessorAssessments(
      LocalDate fromDate,
      LocalDate toDate,
      String status,
      String search);

  /**
   * Retrieves summary details of a specific assessment event.
   */
  AssessorAssessmentSummaryResponse getAssessmentSummary(Long assessmentId);

  /**
   * Retrieves the candidate roster for an assessment, initializing attempt rows if missing.
   */
  AssessorRosterResponse getAssessmentRoster(Long assessmentId);

  /**
   * Saves candidate scores as draft or finalizes the assessment and issues certificates.
   */
  SaveAssessmentScoresResponse saveScores(Long assessmentId, SaveAssessmentScoresRequest request);

  /**
   * Generates authentic PDF binary data for an issued certificate.
   */
  byte[] generateCertificatePdf(Long certificationId);
}
