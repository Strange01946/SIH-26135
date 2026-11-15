package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.EmploymentExitAndUnemploymentRequest;
import in.gov.sih.sih26135.dto.request.PlacementToEmploymentTransitionRequest;
import in.gov.sih.sih26135.dto.request.RecordAssessmentAndIssueCertificateRequest;
import in.gov.sih.sih26135.dto.request.RecordVerificationVerdictRequest;
import in.gov.sih.sih26135.dto.request.RemediationTriggerRequest;
import in.gov.sih.sih26135.dto.request.TraineeEnrollmentOrchestrationRequest;
import in.gov.sih.sih26135.dto.response.CandidateLifecycleSummaryResponse;
import in.gov.sih.sih26135.dto.response.EmploymentExitAndUnemploymentResponse;
import in.gov.sih.sih26135.dto.response.PlacementToEmploymentTransitionResponse;
import in.gov.sih.sih26135.dto.response.RecordAssessmentAndIssueCertificateResponse;
import in.gov.sih.sih26135.dto.response.RecordVerificationVerdictResponse;
import in.gov.sih.sih26135.dto.response.RemediationTriggerResponse;
import in.gov.sih.sih26135.dto.response.TraineeEnrollmentOrchestrationResponse;

/**
 * Authoritative orchestrator coordinating end-to-end domain transactions across the candidate lifecycle:
 * <ul>
 *   <li>Stage A: Registration & Enrollment with mandatory consent validation and batch-provider consistency.</li>
 *   <li>Stage B: Training & Assessment with eligibility-gated certificate issuance and event dispatching.</li>
 *   <li>Stage C: Placement to Employment transition with relational validation and salary tracking.</li>
 *   <li>Stage D: Employment Verification verdict attestation with official vs. self-reported differentiation.</li>
 *   <li>Stage E: Retention, Employment Exit, and atomic Unemployment recording.</li>
 *   <li>Stage F: Skill-Gap Remediation handoff and actionable evaluation.</li>
 *   <li>Read-Only: Unified candidate lifecycle journey and progress summary.</li>
 * </ul>
 *
 * <p>Preserves discrete transactional boundaries per lifecycle stage rather than executing
 * an impossible global monolithic transaction across months or years of candidate progression.
 */
public interface CandidateLifecycleOrchestratorService {

  /**
   * Stage A: Orchestrates candidate enrollment in a training batch with mandatory consent validation.
   *
   * @param request enrollment details with trainee, batch, and optional consent codes
   * @return orchestration response with created enrollment and verified consent status
   */
  TraineeEnrollmentOrchestrationResponse enrollCandidateWithConsentValidation(
      TraineeEnrollmentOrchestrationRequest request);

  /**
   * Stage B: Orchestrates assessment outcome recording and eligibility-gated certificate issuance.
   *
   * @param request assessment scores, trainee assessment ID, and optional certification details
   * @return outcome response with assessment results, issued certificate, and updated enrollment
   */
  RecordAssessmentAndIssueCertificateResponse recordAssessmentAndCertification(
      RecordAssessmentAndIssueCertificateRequest request);

  /**
   * Stage C: Orchestrates transitioning an accepted/joined placement into an active employment record.
   *
   * @param request placement ID, employment record creation parameters, and optional initial salary
   * @return transition response with updated placement and newly created employment record
   */
  PlacementToEmploymentTransitionResponse orchestratePlacementToEmployment(
      PlacementToEmploymentTransitionRequest request);

  /**
   * Stage D: Orchestrates recording an official employment verification verdict and attestation.
   *
   * @param request verification request, verifier assessment, and supporting evidence references
   * @return verdict response with synchronized verification state and employment record
   */
  RecordVerificationVerdictResponse orchestrateEmploymentVerificationVerdict(
      RecordVerificationVerdictRequest request);

  /**
   * Stage E: Orchestrates atomic employment exit and subsequent unemployment spell initiation.
   *
   * @param request exit event details and corresponding unemployment registration parameters
   * @return response with created exit event and linked unemployment record
   */
  EmploymentExitAndUnemploymentResponse orchestrateEmploymentExitAndUnemployment(
      EmploymentExitAndUnemploymentRequest request);

  /**
   * Stage F: Orchestrates skill-gap remediation evaluation following exit or verification failure.
   *
   * @param request trainee ID, trigger source, reference entity, and optional assessment data
   * @return response with evaluated recommendations or safe pending handoff status
   */
  RemediationTriggerResponse triggerSkillGapRemediation(
      RemediationTriggerRequest request);

  /**
   * Aggregates a comprehensive, unified candidate journey summary across all lifecycle stages.
   *
   * @param traineeId target trainee ID
   * @return unified lifecycle summary response
   */
  CandidateLifecycleSummaryResponse getCandidateLifecycleSummary(
      Long traineeId);
}
