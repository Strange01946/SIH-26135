package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.response.DistrictGeographicResponse;
import in.gov.sih.sih26135.dto.response.GeographicOverviewResponse;
import in.gov.sih.sih26135.dto.response.StateGeographicResponse;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import in.gov.sih.sih26135.service.GeographicDashboardService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for Geographic, State, and District Heatmap Dashboards.
 * Exposes national geographic distributions, state-level outcome rollups, and
 * district-level performance rankings (Query Q06).
 *
 * <p>Base Route: /api/v1/dashboards
 * Authorization: requires {@code analytics.read} authority and enforced tenancy scope validation.
 */
@Validated
@RestController
@RequestMapping("/api/v1/dashboards")
@PreAuthorize("hasAuthority('analytics.read')")
public class GeographicDashboardController {

  private final GeographicDashboardService geographicDashboardService;
  private final WorkflowSecurityScopeValidator scopeValidator;

  public GeographicDashboardController(
      GeographicDashboardService geographicDashboardService,
      WorkflowSecurityScopeValidator scopeValidator) {
    this.geographicDashboardService = java.util.Objects.requireNonNull(geographicDashboardService, "GeographicDashboardService is required");
    this.scopeValidator = java.util.Objects.requireNonNull(scopeValidator, "WorkflowSecurityScopeValidator is required");
  }

  /**
   * Retrieves the national geographic distribution and regional overview.
   * Includes state outcome rollups, formal Q06 district rankings, and regional disparity metrics.
   * Restricted strictly to National Administrators with nationwide analytical authority.
   *
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 200 OK with {@link GeographicOverviewResponse} enveloped in {@link ApiResponse}
   */
  @GetMapping("/geography")
  public ResponseEntity<ApiResponse<GeographicOverviewResponse>> getNationalGeography(
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    GeographicOverviewResponse response = geographicDashboardService.getNationalGeographicOverview();
    return ResponseEntity.ok(ApiResponse.success(
        "National geographic overview retrieved successfully",
        response,
        httpRequest.getRequestURI()
    ));
  }

  /**
   * Retrieves state-specific geographic and outcome analytics.
   * Includes state-level summary KPIs, within-state district rankings, and training center distribution.
   * National administrators have cross-state access; state administrators are restricted to their assigned state.
   *
   * @param stateId unique identifier of the state
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 200 OK with {@link StateGeographicResponse} enveloped in {@link ApiResponse}
   */
  @GetMapping("/states/{stateId}")
  public ResponseEntity<ApiResponse<StateGeographicResponse>> getStateGeography(
      @PathVariable @Positive Long stateId,
      HttpServletRequest httpRequest) {
    scopeValidator.validateStateScope(stateId);
    StateGeographicResponse response = geographicDashboardService.getStateGeographicAnalytics(stateId);
    return ResponseEntity.ok(ApiResponse.success(
        "State geographic analytics retrieved successfully",
        response,
        httpRequest.getRequestURI()
    ));
  }

  /**
   * Retrieves district-specific outcome and regional analytics.
   * Includes district KPI performance, national and state ranks, and training center distributions.
   * National administrators have cross-district access; state administrators are restricted to constituent
   * districts belonging to their assigned state; district officers are restricted to their specific district.
   *
   * @param districtId unique identifier of the district
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 200 OK with {@link DistrictGeographicResponse} enveloped in {@link ApiResponse}
   */
  @GetMapping("/districts/{districtId}")
  public ResponseEntity<ApiResponse<DistrictGeographicResponse>> getDistrictGeography(
      @PathVariable @Positive Long districtId,
      HttpServletRequest httpRequest) {
    scopeValidator.validateDistrictScope(districtId);
    DistrictGeographicResponse response = geographicDashboardService.getDistrictGeographicAnalytics(districtId);
    return ResponseEntity.ok(ApiResponse.success(
        "District geographic analytics retrieved successfully",
        response,
        httpRequest.getRequestURI()
    ));
  }
}
