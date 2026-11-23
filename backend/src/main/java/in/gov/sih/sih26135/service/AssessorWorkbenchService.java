package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.assessor.AssessorAssessmentSummaryResponse;
import in.gov.sih.sih26135.dto.assessor.AssessorRosterResponse;
import in.gov.sih.sih26135.dto.assessor.SaveAssessmentScoresRequest;
import in.gov.sih.sih26135.dto.assessor.SaveAssessmentScoresResponse;
import in.gov.sih.sih26135.entity.Assessment;
import in.gov.sih.sih26135.entity.Course;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
   * Evaluates pending requirements for passed candidates and issues certificates once eligible.
   */
  SaveAssessmentScoresResponse issuePendingCertificates(Long assessmentId);

  /**
   * Generates authentic PDF binary data for an issued certificate.
   */
  byte[] generateCertificatePdf(Long certificationId);

  /**
   * Resolves the genuinely authorized awarding body or issuing organization without inventing placeholders.
   */
  Optional<String> resolveAuthoritativeIssuingBody(Assessment assessment);

  /**
   * Verifies whether mandatory attendance requirements are met based on course delivery mode and recorded sessions.
   */
  boolean isAttendanceRequirementSatisfied(Course course, Long enrollmentId);
}
