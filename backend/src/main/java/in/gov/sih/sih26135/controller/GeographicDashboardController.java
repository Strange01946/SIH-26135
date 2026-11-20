package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.request.AnalyticsFilterCriteria;
import in.gov.sih.sih26135.dto.response.DistrictGeographicResponse;
import in.gov.sih.sih26135.dto.response.GeographicOverviewResponse;
import in.gov.sih.sih26135.dto.response.StateGeographicResponse;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import in.gov.sih.sih26135.service.GeographicDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
@Tag(name = "Dashboards - Geographic", description = "Territorial metrics, district heatmaps, and spatial outcomes")
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
  @Operation(
      summary = "Get National Geographic Overview",
      description = "Retrieves nationwide geographic distributions, state outcome rollups, and formal Q06 district rankings.",
      security = @SecurityRequirement(name = "bearerAuth"),
      responses = {
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "National geographic overview returned"),
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied - requires nationwide administrator scope")
      }
  )
  @GetMapping("/geography")
  public ResponseEntity<ApiResponse<GeographicOverviewResponse>> getNationalGeography(
      @Valid AnalyticsFilterCriteria criteria,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    scopeValidator.validateAnalyticsFilterScope(criteria);
    GeographicOverviewResponse response = (criteria == null || criteria.isEmpty())
        ? geographicDashboardService.getNationalGeographicOverview()
        : geographicDashboardService.getNationalGeographicOverview(criteria);
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
   * @param criteria optional filter criteria
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 200 OK with {@link StateGeographicResponse} enveloped in {@link ApiResponse}
   */
  @Operation(
      summary = "Get State Geographic Analytics",
      description = "Retrieves state-level outcome KPIs, constituent district rankings, and training center distributions.",
      security = @SecurityRequirement(name = "bearerAuth"),
      responses = {
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "State geographic analytics returned"),
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied - outside assigned state scope"),
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "State not found")
      }
  )
  @GetMapping("/states/{stateId}")
  public ResponseEntity<ApiResponse<StateGeographicResponse>> getStateGeography(
      @PathVariable @Positive Long stateId,
      @Valid AnalyticsFilterCriteria criteria,
      HttpServletRequest httpRequest) {
    scopeValidator.validateStateScope(stateId);
    scopeValidator.validateAnalyticsFilterScope(criteria);
    boolean hasFiltering = criteria != null && (
        criteria.hasTemporalFilter()
        || criteria.hasOrganizationalFilter()
        || criteria.hasDemographicFilter()
        || criteria.getDistrictId() != null
        || (criteria.getStateId() != null && !criteria.getStateId().equals(stateId))
    );
    StateGeographicResponse response = hasFiltering
        ? geographicDashboardService.getStateGeographicAnalytics(stateId, criteria)
        : geographicDashboardService.getStateGeographicAnalytics(stateId);
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
   * @param criteria optional filter criteria
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 200 OK with {@link DistrictGeographicResponse} enveloped in {@link ApiResponse}
   */
  @Operation(
      summary = "Get District Geographic Analytics",
      description = "Retrieves district KPI performance, statewide and nationwide rankings, and localized center metrics.",
      security = @SecurityRequirement(name = "bearerAuth"),
      responses = {
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "District geographic analytics returned"),
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied - outside assigned district tenancy"),
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "District not found")
      }
  )
  @GetMapping("/districts/{districtId}")
  public ResponseEntity<ApiResponse<DistrictGeographicResponse>> getDistrictGeography(
      @PathVariable @Positive Long districtId,
      @Valid AnalyticsFilterCriteria criteria,
      HttpServletRequest httpRequest) {
    scopeValidator.validateDistrictScope(districtId);
    scopeValidator.validateAnalyticsFilterScope(criteria);
    boolean hasFiltering = criteria != null && (
        criteria.hasTemporalFilter()
        || criteria.hasOrganizationalFilter()
        || criteria.hasDemographicFilter()
        || criteria.getStateId() != null
        || (criteria.getDistrictId() != null && !criteria.getDistrictId().equals(districtId))
    );
    DistrictGeographicResponse response = hasFiltering
        ? geographicDashboardService.getDistrictGeographicAnalytics(districtId, criteria)
        : geographicDashboardService.getDistrictGeographicAnalytics(districtId);
    return ResponseEntity.ok(ApiResponse.success(
        "District geographic analytics retrieved successfully",
        response,
        httpRequest.getRequestURI()
    ));
  }
}
