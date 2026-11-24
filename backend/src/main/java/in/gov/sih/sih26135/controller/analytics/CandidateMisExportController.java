package in.gov.sih.sih26135.controller.analytics;

import in.gov.sih.sih26135.dto.request.AnalyticsFilterCriteria;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import in.gov.sih.sih26135.service.export.CandidateMisExportService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/v1/analytics/export/candidates")
@PreAuthorize("hasAuthority('analytics.read') and hasAuthority('trainee.read')")
@SecurityRequirement(name = "bearerAuth")
public class CandidateMisExportController {
  private final CandidateMisExportService exports;
  private final WorkflowSecurityScopeValidator scope;

  public CandidateMisExportController(CandidateMisExportService exports, WorkflowSecurityScopeValidator scope) {
    this.exports = exports;
    this.scope = scope;
  }

  @GetMapping("/columns")
  public ApiResponse<List<CandidateMisExportService.Column>> getCandidateExportColumns() {
    scope.validateNationwideScope();
    return ApiResponse.ok(exports.columns());
  }

  @GetMapping("/{format:csv|json}")
  public ResponseEntity<StreamingResponseBody> exportCandidateOutcomes(
      @PathVariable String format, @RequestParam(required = false) List<String> columns,
      @Valid @ParameterObject AnalyticsFilterCriteria criteria) {
    scope.validateNationwideScope();
    scope.validateAnalyticsFilterScope(criteria);
    Long userId = scope.resolveAuthenticatedUserId();
    if (userId == null) {
      throw new AccessDeniedException("Access denied");
    }
    var file = exports.prepare(format, columns, criteria, userId);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_TYPE, "csv".equals(format) ? "text/csv; charset=UTF-8" : "application/json")
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"candidate-outcomes." + format + "\"")
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .header("X-Content-Type-Options", "nosniff")
        .body(file);
  }
}
