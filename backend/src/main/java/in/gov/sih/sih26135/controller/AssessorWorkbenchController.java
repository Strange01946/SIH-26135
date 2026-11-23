package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.assessor.AssessorAssessmentSummaryResponse;
import in.gov.sih.sih26135.dto.assessor.AssessorRosterResponse;
import in.gov.sih.sih26135.dto.assessor.SaveAssessmentScoresRequest;
import in.gov.sih.sih26135.dto.assessor.SaveAssessmentScoresResponse;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.AssessorWorkbenchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for Phase 30.6 Assessor Workbench:
 * Assessment scheduling, candidate roster management, draft scoring, finalization, and certification.
 *
 * <p>Base Route: {@code /api/v1/assessor}
 */
@Validated
@RestController
@RequestMapping("/api/v1/assessor")
@PreAuthorize("hasAnyAuthority('assessment.manage', 'ROLE_assessor', 'ROLE_super_admin', 'ROLE_government_admin')")
@Tag(name = "Assessor Workbench", description = "Assessor assessment scheduling, scoring grid, candidate rosters, and certification")
public class AssessorWorkbenchController {

  private final AssessorWorkbenchService assessorWorkbenchService;

  public AssessorWorkbenchController(AssessorWorkbenchService assessorWorkbenchService) {
    this.assessorWorkbenchService = assessorWorkbenchService;
  }

  /**
   * Retrieves all assessments assigned to the authenticated assessor.
   */
  @GetMapping("/assessments")
  @Operation(summary = "Get assigned assessments for authenticated assessor")
  public ResponseEntity<ApiResponse<List<AssessorAssessmentSummaryResponse>>> getAssignedAssessments(
      @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
      @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
      @RequestParam(value = "status", required = false) String status,
      @RequestParam(value = "search", required = false) String search,
      HttpServletRequest httpRequest) {

    List<AssessorAssessmentSummaryResponse> assessments =
        assessorWorkbenchService.getAssessorAssessments(fromDate, toDate, status, search);

    return ResponseEntity.ok(ApiResponse.success(
        "Assigned assessments retrieved successfully",
        assessments,
        httpRequest.getRequestURI()));
  }

  /**
   * Retrieves summary details of a specific assessment event.
   */
  @GetMapping("/assessments/{id}")
  @Operation(summary = "Get assessment summary by ID")
  public ResponseEntity<ApiResponse<AssessorAssessmentSummaryResponse>> getAssessmentSummary(
      @PathVariable @Positive Long id,
      HttpServletRequest httpRequest) {

    AssessorAssessmentSummaryResponse assessment = assessorWorkbenchService.getAssessmentSummary(id);

    return ResponseEntity.ok(ApiResponse.success(
        "Assessment details retrieved successfully",
        assessment,
        httpRequest.getRequestURI()));
  }

  /**
   * Retrieves the candidate roster for an assessment, initializing attempt rows if missing.
   */
  @GetMapping("/assessments/{id}/roster")
  @Operation(summary = "Get candidate roster for an assessment")
  public ResponseEntity<ApiResponse<AssessorRosterResponse>> getAssessmentRoster(
      @PathVariable @Positive Long id,
      HttpServletRequest httpRequest) {

    AssessorRosterResponse roster = assessorWorkbenchService.getAssessmentRoster(id);

    return ResponseEntity.ok(ApiResponse.success(
        "Assessment candidate roster retrieved successfully",
        roster,
        httpRequest.getRequestURI()));
  }

  /**
   * Saves candidate scores as a draft without declaring final results or issuing certificates.
   */
  @PostMapping("/assessments/{id}/draft")
  @Operation(summary = "Save candidate scoring draft")
  public ResponseEntity<ApiResponse<SaveAssessmentScoresResponse>> saveDraftScores(
      @PathVariable @Positive Long id,
      @Valid @RequestBody SaveAssessmentScoresRequest request,
      HttpServletRequest httpRequest) {

    request.setDraft(true);
    SaveAssessmentScoresResponse response = assessorWorkbenchService.saveScores(id, request);

    return ResponseEntity.ok(ApiResponse.success(
        "Assessment draft scores saved successfully",
        response,
        httpRequest.getRequestURI()));
  }

  /**
   * Finalizes assessment scores, declares authoritative results, and issues certificates for passed candidates.
   */
  @PostMapping("/assessments/{id}/finalize")
  @Operation(summary = "Finalize assessment and issue certificates")
  public ResponseEntity<ApiResponse<SaveAssessmentScoresResponse>> finalizeAssessment(
      @PathVariable @Positive Long id,
      @Valid @RequestBody SaveAssessmentScoresRequest request,
      HttpServletRequest httpRequest) {

    request.setDraft(false);
    SaveAssessmentScoresResponse response = assessorWorkbenchService.saveScores(id, request);

    return ResponseEntity.ok(ApiResponse.success(
        "Assessment finalized and certificates issued successfully",
        response,
        httpRequest.getRequestURI()));
  }

  /**
   * Re-evaluates pending candidates on a finalized assessment and issues certificates for any who have become eligible.
   */
  @PostMapping("/assessments/{id}/issue-pending")
  @Operation(summary = "Issue certificates for eligible pending candidates")
  public ResponseEntity<ApiResponse<SaveAssessmentScoresResponse>> issuePendingCertificates(
      @PathVariable @Positive Long id,
      HttpServletRequest httpRequest) {

    SaveAssessmentScoresResponse response = assessorWorkbenchService.issuePendingCertificates(id);

    return ResponseEntity.ok(ApiResponse.success(
        "Pending candidate requirements re-evaluated and eligible certificates issued",
        response,
        httpRequest.getRequestURI()));
  }
}
