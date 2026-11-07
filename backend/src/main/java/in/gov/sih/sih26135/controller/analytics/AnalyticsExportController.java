package in.gov.sih.sih26135.controller.analytics;

import in.gov.sih.sih26135.dto.request.AnalyticsFilterCriteria;
import in.gov.sih.sih26135.entity.TrainingProvider;
import in.gov.sih.sih26135.entity.User;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.repository.TrainingProviderRepository;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import in.gov.sih.sih26135.service.AnalyticsExportService;
import jakarta.validation.constraints.Positive;
import java.util.Objects;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * REST controller for statutory Government MIS Report Generation and Streaming Data Exports.
 *
 * <p>Base Route: /api/v1/analytics/export
 *
 * <p>Supported Export Operations:
 * <ul>
 *   <li>District Skill Development Plan (DSDP): {@code /dsdp/csv}, {@code /dsdp/json}</li>
 *   <li>Ministry Quarterly Progress Report (QPR): {@code /qpr/csv}, {@code /qpr/json}</li>
 *   <li>Training Provider Performance Scorecard: {@code /provider-scorecard/csv}, {@code /provider-scorecard/json}</li>
 * </ul>
 *
 * <p>Security & Guardrail Principles:
 * <ul>
 *   <li>All endpoints enforce authenticated identity and territorial tenancy scopes before streaming begins.</li>
 *   <li>Strict rate limits (sliding window) and concurrent export quotas prevent resource starvation.</li>
 *   <li>Server-side row ceilings prevent unbounded database reads.</li>
 *   <li>Phase 27.14 statistical disclosure controls (k=5 cell-level outcome and complementary suppression) are applied.</li>
 *   <li>Safe spreadsheet formula injection escaping protects all CSV cell contents.</li>
 * </ul>
 */
@Validated
@RestController
@RequestMapping("/api/v1/analytics/export")
@PreAuthorize("isAuthenticated()")
public class AnalyticsExportController {

  private static final String CSV_MEDIA_TYPE = "text/csv; charset=UTF-8";

  private final AnalyticsExportService exportService;
  private final WorkflowSecurityScopeValidator scopeValidator;
  private final TrainingProviderRepository trainingProviderRepository;

  public AnalyticsExportController(
      AnalyticsExportService exportService,
      WorkflowSecurityScopeValidator scopeValidator,
      TrainingProviderRepository trainingProviderRepository) {
    this.exportService = Objects.requireNonNull(exportService, "exportService is required");
    this.scopeValidator = Objects.requireNonNull(scopeValidator, "scopeValidator is required");
    this.trainingProviderRepository = Objects.requireNonNull(trainingProviderRepository, "trainingProviderRepository is required");
  }

  // =========================================================================
  // 1. District Skill Development Plan (DSDP) Exports
  // =========================================================================

  @GetMapping("/dsdp/csv")
  public ResponseEntity<StreamingResponseBody> exportDsdpCsv(
      @RequestParam("districtId") @Positive Long districtId,
      @ModelAttribute AnalyticsFilterCriteria criteria) {
    Long userId = resolveAuthenticatedUserId();
    scopeValidator.validateDistrictScope(districtId);
    if (criteria != null) {
      scopeValidator.validateAnalyticsFilterScope(criteria);
    }

    String filename = sanitizeFilename("dsdp-district-" + districtId + ".csv");
    StreamingResponseBody responseBody = exportService.exportDsdpCsv(districtId, criteria, userId);

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_TYPE, CSV_MEDIA_TYPE)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .header("X-Content-Type-Options", "nosniff")
        .header("Cache-Control", "no-cache, no-store, must-revalidate")
        .body(responseBody);
  }

  @GetMapping("/dsdp/json")
  public ResponseEntity<StreamingResponseBody> exportDsdpJson(
      @RequestParam("districtId") @Positive Long districtId,
      @ModelAttribute AnalyticsFilterCriteria criteria) {
    Long userId = resolveAuthenticatedUserId();
    scopeValidator.validateDistrictScope(districtId);
    if (criteria != null) {
      scopeValidator.validateAnalyticsFilterScope(criteria);
    }

    String filename = sanitizeFilename("dsdp-district-" + districtId + ".json");
    StreamingResponseBody responseBody = exportService.exportDsdpJson(districtId, criteria, userId);

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .header("X-Content-Type-Options", "nosniff")
        .header("Cache-Control", "no-cache, no-store, must-revalidate")
        .body(responseBody);
  }

  // =========================================================================
  // 2. Ministry Quarterly Progress Report (QPR) Exports
  // =========================================================================

  @GetMapping("/qpr/csv")
  public ResponseEntity<StreamingResponseBody> exportQprCsv(
      @RequestParam(name = "fiscalYear", defaultValue = "FY2026-27") String fiscalYear,
      @RequestParam(name = "quarter", defaultValue = "Q1") String quarter,
      @ModelAttribute AnalyticsFilterCriteria criteria) {
    Long userId = resolveAuthenticatedUserId();
    scopeValidator.validateNationwideScope();
    if (criteria != null) {
      scopeValidator.validateAnalyticsFilterScope(criteria);
    }

    String filename = sanitizeFilename("qpr-" + fiscalYear + "-" + quarter + ".csv");
    StreamingResponseBody responseBody = exportService.exportQprCsv(fiscalYear, quarter, criteria, userId);

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_TYPE, CSV_MEDIA_TYPE)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .header("X-Content-Type-Options", "nosniff")
        .header("Cache-Control", "no-cache, no-store, must-revalidate")
        .body(responseBody);
  }

  @GetMapping("/qpr/json")
  public ResponseEntity<StreamingResponseBody> exportQprJson(
      @RequestParam(name = "fiscalYear", defaultValue = "FY2026-27") String fiscalYear,
      @RequestParam(name = "quarter", defaultValue = "Q1") String quarter,
      @ModelAttribute AnalyticsFilterCriteria criteria) {
    Long userId = resolveAuthenticatedUserId();
    scopeValidator.validateNationwideScope();
    if (criteria != null) {
      scopeValidator.validateAnalyticsFilterScope(criteria);
    }

    String filename = sanitizeFilename("qpr-" + fiscalYear + "-" + quarter + ".json");
    StreamingResponseBody responseBody = exportService.exportQprJson(fiscalYear, quarter, criteria, userId);

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .header("X-Content-Type-Options", "nosniff")
        .header("Cache-Control", "no-cache, no-store, must-revalidate")
        .body(responseBody);
  }

  // =========================================================================
  // 3. Training Provider Performance Scorecard Exports
  // =========================================================================

  @GetMapping("/provider-scorecard/csv")
  public ResponseEntity<StreamingResponseBody> exportProviderScorecardCsv(
      @RequestParam(name = "providerId", required = false) @Positive Long providerId,
      @RequestParam(name = "stateId", required = false) @Positive Long stateId,
      @ModelAttribute AnalyticsFilterCriteria criteria) {
    Long userId = resolveAuthenticatedUserId();
    ResolvedProviderScope effectiveScope = validateAndResolveProviderExportScope(providerId, stateId);
    if (criteria != null) {
      scopeValidator.validateAnalyticsFilterScope(criteria);
    }

    String filename = sanitizeFilename("provider-scorecard"
        + (effectiveScope.providerId() != null ? "-provider-" + effectiveScope.providerId() : "")
        + (effectiveScope.stateId() != null ? "-state-" + effectiveScope.stateId() : "")
        + ".csv");

    StreamingResponseBody responseBody = exportService.exportProviderScorecardCsv(
        effectiveScope.providerId(), effectiveScope.stateId(), criteria, userId
    );

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_TYPE, CSV_MEDIA_TYPE)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .header("X-Content-Type-Options", "nosniff")
        .header("Cache-Control", "no-cache, no-store, must-revalidate")
        .body(responseBody);
  }

  @GetMapping("/provider-scorecard/json")
  public ResponseEntity<StreamingResponseBody> exportProviderScorecardJson(
      @RequestParam(name = "providerId", required = false) @Positive Long providerId,
      @RequestParam(name = "stateId", required = false) @Positive Long stateId,
      @ModelAttribute AnalyticsFilterCriteria criteria) {
    Long userId = resolveAuthenticatedUserId();
    ResolvedProviderScope effectiveScope = validateAndResolveProviderExportScope(providerId, stateId);
    if (criteria != null) {
      scopeValidator.validateAnalyticsFilterScope(criteria);
    }

    String filename = sanitizeFilename("provider-scorecard"
        + (effectiveScope.providerId() != null ? "-provider-" + effectiveScope.providerId() : "")
        + (effectiveScope.stateId() != null ? "-state-" + effectiveScope.stateId() : "")
        + ".json");

    StreamingResponseBody responseBody = exportService.exportProviderScorecardJson(
        effectiveScope.providerId(), effectiveScope.stateId(), criteria, userId
    );

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .header("X-Content-Type-Options", "nosniff")
        .header("Cache-Control", "no-cache, no-store, must-revalidate")
        .body(responseBody);
  }

  // =========================================================================
  // Scope Resolution Helpers
  // =========================================================================

  private record ResolvedProviderScope(Long providerId, Long stateId) {}

  private ResolvedProviderScope validateAndResolveProviderExportScope(Long requestedProviderId, Long requestedStateId) {
    // 1. Nationwide Administrator: has unrestricted access across all providers and states
    if (scopeValidator.isNationwideAdmin()) {
      return new ResolvedProviderScope(requestedProviderId, requestedStateId);
    }

    // 2. Training Provider Persona: restricted strictly to own training provider identity
    if (scopeValidator.isTrainingProviderCaller()) {
      Optional<User> userOpt = scopeValidator.resolveAuthenticatedUser();
      Long userProviderId = userOpt.map(User::getTrainingProviderId).orElse(null);
      if (userProviderId == null) {
        throw new AccessDeniedException("Access denied: provider persona has no associated provider ID");
      }
      if (requestedProviderId != null && !requestedProviderId.equals(userProviderId)) {
        throw new AccessDeniedException("Access denied: training provider cannot export scorecards for other providers");
      }
      // Training provider can only view their own provider, never state-wide or nationwide
      return new ResolvedProviderScope(userProviderId, null);
    }

    // 3. State Administrator: restricted strictly to providers within assigned state boundary
    if (scopeValidator.isStateAdmin()) {
      Optional<User> userOpt = scopeValidator.resolveAuthenticatedUser();
      Long userStateId = userOpt.map(User::getStateId).orElse(null);
      if (userStateId == null) {
        throw new AccessDeniedException("Access denied: state admin has no associated state ID");
      }
      if (requestedStateId != null && !requestedStateId.equals(userStateId)) {
        throw new AccessDeniedException("Access denied: state admin cannot export other states");
      }
      if (requestedProviderId != null) {
        TrainingProvider tp = trainingProviderRepository.findById(requestedProviderId)
            .orElseThrow(() -> new ResourceNotFoundException("Training provider not found with ID: " + requestedProviderId));
        if (!userStateId.equals(tp.getStateId())) {
          throw new AccessDeniedException("Access denied: provider does not belong to authorized state");
        }
      }
      return new ResolvedProviderScope(requestedProviderId, userStateId);
    }

    // 4. All other personas (Employers, Trainees, District Officers without authority, Permission-only) are denied
    throw new AccessDeniedException("Access denied: caller role is not authorized for provider scorecard exports");
  }

  private Long resolveAuthenticatedUserId() {
    Long userId = scopeValidator.resolveAuthenticatedUserId();
    if (userId == null) {
      throw new AccessDeniedException("Access denied: unauthenticated request");
    }
    return userId;
  }

  private String sanitizeFilename(String filename) {
    return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
  }
}
