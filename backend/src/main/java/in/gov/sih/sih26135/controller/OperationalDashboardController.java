package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.request.OperationalDashboardFilter;
import in.gov.sih.sih26135.dto.response.OperationalDataQualityScorecardResponse;
import in.gov.sih.sih26135.dto.response.OperationalImportMetricsResponse;
import in.gov.sih.sih26135.dto.response.OperationalSystemHealthResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.OperationalDashboardService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only REST controller exposing operational health scorecards, data quality index,
 * bulk ingestion metrics, and system health / audit mutation monitoring APIs.
 *
 * <p>Base Route: {@code /api/v1/operational}
 */
@Validated
@RestController
@RequestMapping("/api/v1/operational")
@PreAuthorize("hasAnyAuthority('audit.read', 'system.manage', 'analytics.read', 'system.event.read', 'system.event.manage')")
public class OperationalDashboardController {

  private static final Set<String> ALLOWED_QUERY_PARAMS = Set.of(
      "startDate",
      "endDate",
      "stateId",
      "districtId",
      "trainingProviderId"
  );

  private final OperationalDashboardService dashboardService;

  public OperationalDashboardController(OperationalDashboardService dashboardService) {
    this.dashboardService = Objects.requireNonNull(dashboardService, "OperationalDashboardService is required");
  }

  @GetMapping("/data-quality-scorecard")
  public ResponseEntity<ApiResponse<OperationalDataQualityScorecardResponse>> getDataQualityScorecard(
      @RequestParam(value = "startDate", required = false) String startDate,
      @RequestParam(value = "endDate", required = false) String endDate,
      @RequestParam(value = "stateId", required = false) @Positive Long stateId,
      @RequestParam(value = "districtId", required = false) @Positive Long districtId,
      @RequestParam(value = "trainingProviderId", required = false) @Positive Long trainingProviderId,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest);
    OperationalDashboardFilter filter = OperationalDashboardFilter.parseAndValidate(
        startDate, endDate, stateId, districtId, trainingProviderId, LocalDateTime.now());
    OperationalDataQualityScorecardResponse response = dashboardService.getDataQualityScorecard(filter);
    return ResponseEntity.ok(ApiResponse.success(
        "Operational data quality scorecard retrieved successfully",
        response,
        httpRequest.getRequestURI()));
  }

  @GetMapping("/import-metrics")
  public ResponseEntity<ApiResponse<OperationalImportMetricsResponse>> getImportMetrics(
      @RequestParam(value = "startDate", required = false) String startDate,
      @RequestParam(value = "endDate", required = false) String endDate,
      @RequestParam(value = "stateId", required = false) @Positive Long stateId,
      @RequestParam(value = "districtId", required = false) @Positive Long districtId,
      @RequestParam(value = "trainingProviderId", required = false) @Positive Long trainingProviderId,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest);
    OperationalDashboardFilter filter = OperationalDashboardFilter.parseAndValidate(
        startDate, endDate, stateId, districtId, trainingProviderId, LocalDateTime.now());
    OperationalImportMetricsResponse response = dashboardService.getImportMetrics(filter);
    return ResponseEntity.ok(ApiResponse.success(
        "Operational import metrics retrieved successfully",
        response,
        httpRequest.getRequestURI()));
  }

  @GetMapping("/system-health")
  public ResponseEntity<ApiResponse<OperationalSystemHealthResponse>> getSystemHealth(
      @RequestParam(value = "startDate", required = false) String startDate,
      @RequestParam(value = "endDate", required = false) String endDate,
      @RequestParam(value = "stateId", required = false) @Positive Long stateId,
      @RequestParam(value = "districtId", required = false) @Positive Long districtId,
      @RequestParam(value = "trainingProviderId", required = false) @Positive Long trainingProviderId,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest);
    OperationalDashboardFilter filter = OperationalDashboardFilter.parseAndValidate(
        startDate, endDate, stateId, districtId, trainingProviderId, LocalDateTime.now());
    OperationalSystemHealthResponse response = dashboardService.getSystemHealth(filter);
    return ResponseEntity.ok(ApiResponse.success(
        "Operational system health retrieved successfully",
        response,
        httpRequest.getRequestURI()));
  }

  private void validateAllowedQueryParameters(HttpServletRequest request) {
    if (request == null || request.getParameterNames() == null) {
      return;
    }
    var paramNames = request.getParameterNames();
    while (paramNames.hasMoreElements()) {
      String paramName = paramNames.nextElement();
      if (!ALLOWED_QUERY_PARAMS.contains(paramName)) {
        throw new BadRequestException(
            "Unsupported query parameter: " + paramName,
            "UNSUPPORTED_PARAMETER");
      }
    }
  }
}
