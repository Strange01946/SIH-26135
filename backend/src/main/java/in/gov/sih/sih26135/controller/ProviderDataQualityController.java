package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.request.DismissDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.request.ResolveDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.response.CenterScorecardResponse;
import in.gov.sih.sih26135.dto.response.DataQualityIssueEventResponse;
import in.gov.sih.sih26135.dto.response.DataQualityRuleExecutionSummary;
import in.gov.sih.sih26135.dto.response.ProviderDataQualityIssueResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.response.PagedResponse;
import in.gov.sih.sih26135.response.PaginationUtils;
import in.gov.sih.sih26135.service.ProviderDataQualityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * REST controller for Phase 30.5 Training Provider Data Quality Remediation & Center Scorecards.
 *
 * <p>Base Routes:
 * <ul>
 *   <li>{@code /api/v1/provider/data-quality} — Issue tracker, detail, bounded timeline, and remediation actions</li>
 *   <li>{@code /api/v1/provider/centers/{centerId}/scorecard} — Center cleanliness index & enrollment outcomes</li>
 * </ul>
 */
@Tag(name = "Provider Data Quality & Scorecards", description = "Training provider data quality tracker, remediation lifecycle, and center cleanliness scorecards")
@Validated
@RestController
@RequestMapping("/api/v1/provider")
@PreAuthorize("hasAnyAuthority('ROLE_training_provider', 'center.manage', 'enrollment.manage')")
public class ProviderDataQualityController {

  private static final java.util.Map<String, String> ALLOWED_SORT_FIELDS = java.util.Map.of(
      "id", "id",
      "detectedAt", "detectedAt",
      "detected_at", "detectedAt",
      "severity", "severity",
      "status", "status"
  );

  private final ProviderDataQualityService providerDataQualityService;

  public ProviderDataQualityController(ProviderDataQualityService providerDataQualityService) {
    this.providerDataQualityService = Objects.requireNonNull(providerDataQualityService, "providerDataQualityService is required");
  }

  /**
   * Retrieves server-side paginated and multi-filtered Data Quality issues for the authenticated training provider.
   */
  @GetMapping("/data-quality/issues")
  public ResponseEntity<ApiResponse<PagedResponse<ProviderDataQualityIssueResponse>>> getIssues(
      @RequestParam(value = "severity", required = false) String severity,
      @RequestParam(value = "category", required = false) String category,
      @RequestParam(value = "status", required = false) String status,
      @RequestParam(value = "entityType", required = false) String entityType,
      @RequestParam(value = "centerId", required = false) @Positive Long centerId,
      @RequestParam(value = "activeOnly", required = false) Boolean activeOnly,
      @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
      @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(100) int size,
      @RequestParam(value = "sort", defaultValue = "detectedAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {

    Pageable pageable = PaginationUtils.buildPageable(page, size, sort, direction, ALLOWED_SORT_FIELDS, "detectedAt");
    Page<ProviderDataQualityIssueResponse> issues = providerDataQualityService.getProviderIssues(
        severity, category, status, entityType, centerId, activeOnly, pageable);

    return ResponseEntity.ok(ApiResponse.success(
        "Provider data quality issues retrieved successfully",
        PagedResponse.of(issues),
        httpRequest.getRequestURI()));
  }

  /**
   * Retrieves a single Data Quality issue by ID with provider tenancy verification.
   */
  @GetMapping("/data-quality/issues/{id}")
  public ResponseEntity<ApiResponse<ProviderDataQualityIssueResponse>> getIssueById(
      @PathVariable @Positive Long id,
      HttpServletRequest httpRequest) {

    ProviderDataQualityIssueResponse issue = providerDataQualityService.getIssueById(id);
    return ResponseEntity.ok(ApiResponse.success(
        "Data quality issue retrieved successfully",
        issue,
        httpRequest.getRequestURI()));
  }

  /**
   * Retrieves bounded historical event timeline for an issue with provider tenancy verification.
   */
  @GetMapping("/data-quality/issues/{id}/events")
  public ResponseEntity<ApiResponse<List<DataQualityIssueEventResponse>>> getIssueEvents(
      @PathVariable @Positive Long id,
      @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
      @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(100) int size,
      HttpServletRequest httpRequest) {

    List<DataQualityIssueEventResponse> events = providerDataQualityService.getIssueEvents(id, page, size);
    return ResponseEntity.ok(ApiResponse.success(
        "Data quality issue events retrieved successfully",
        events,
        httpRequest.getRequestURI()));
  }

  /**
   * Transitions an eligible issue to IN_PROGRESS status.
   */
  @PostMapping("/data-quality/issues/{id}/investigate")
  public ResponseEntity<ApiResponse<ProviderDataQualityIssueResponse>> startInvestigation(
      @PathVariable @Positive Long id,
      HttpServletRequest httpRequest) {

    ProviderDataQualityIssueResponse issue = providerDataQualityService.startInvestigation(id);
    return ResponseEntity.ok(ApiResponse.success(
        "Data quality issue investigation started successfully",
        issue,
        httpRequest.getRequestURI()));
  }

  /**
   * Resolves a data quality issue with mandatory resolution notes.
   */
  @PostMapping("/data-quality/issues/{id}/resolve")
  public ResponseEntity<ApiResponse<ProviderDataQualityIssueResponse>> resolveIssue(
      @PathVariable @Positive Long id,
      @Valid @RequestBody ResolveDataQualityIssueRequest request,
      HttpServletRequest httpRequest) {

    ProviderDataQualityIssueResponse issue = providerDataQualityService.resolveIssue(id, request);
    return ResponseEntity.ok(ApiResponse.success(
        "Data quality issue resolved successfully",
        issue,
        httpRequest.getRequestURI()));
  }

  /**
   * Dismisses a data quality issue (marking as false positive or suppressed) with mandatory remarks.
   */
  @PostMapping("/data-quality/issues/{id}/dismiss")
  public ResponseEntity<ApiResponse<ProviderDataQualityIssueResponse>> dismissIssue(
      @PathVariable @Positive Long id,
      @Valid @RequestBody DismissDataQualityIssueRequest request,
      HttpServletRequest httpRequest) {

    ProviderDataQualityIssueResponse issue = providerDataQualityService.dismissIssue(id, request);
    return ResponseEntity.ok(ApiResponse.success(
        "Data quality issue dismissed successfully",
        issue,
        httpRequest.getRequestURI()));
  }

  /**
   * Retrieves Center Performance & Cleanliness Scorecard for an affiliated training center.
   */
  @GetMapping("/centers/{centerId}/scorecard")
  public ResponseEntity<ApiResponse<CenterScorecardResponse>> getCenterScorecard(
      @PathVariable @Positive Long centerId,
      HttpServletRequest httpRequest) {

    CenterScorecardResponse scorecard = providerDataQualityService.getCenterScorecard(centerId);
    return ResponseEntity.ok(ApiResponse.success(
        "Center data quality scorecard retrieved successfully",
        scorecard,
        httpRequest.getRequestURI()));
  }

  /**
   * Triggers background rule re-evaluation after domain record remediation.
   */
  @PostMapping("/data-quality/re-evaluate")
  public ResponseEntity<ApiResponse<List<DataQualityRuleExecutionSummary>>> triggerReEvaluation(
      @RequestParam(value = "issueId", required = false) @Positive Long issueId,
      HttpServletRequest httpRequest) {

    List<DataQualityRuleExecutionSummary> summaries = providerDataQualityService.triggerReEvaluation(issueId);
    boolean hasSkipped = summaries != null && summaries.stream().anyMatch(s -> "SKIPPED".equalsIgnoreCase(s.status()));
    boolean hasFailed = summaries != null && summaries.stream().anyMatch(s -> "FAILED".equalsIgnoreCase(s.status()));
    if (hasFailed) {
      throw new BadRequestException("Data quality re-evaluation failed for one or more rules", "SCAN_FAILED");
    }
    if (hasSkipped) {
      if (issueId != null) {
        throw new BadRequestException("Data quality re-evaluation is not supported for requested rule", "UNSUPPORTED_RE_EVALUATION");
      }
      return ResponseEntity.ok(ApiResponse.success(
          "Rule re-evaluation partially completed: some rules were skipped due to unavailable detectors",
          summaries,
          httpRequest.getRequestURI()));
    }
    return ResponseEntity.ok(ApiResponse.success(
        "Rule re-evaluation triggered successfully",
        summaries,
        httpRequest.getRequestURI()));
  }
}
