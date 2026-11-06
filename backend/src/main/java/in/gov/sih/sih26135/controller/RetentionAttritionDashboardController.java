package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.response.AttritionDashboardResponse;
import in.gov.sih.sih26135.dto.response.LongitudinalRetentionResponse;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import in.gov.sih.sih26135.service.RetentionAttritionDashboardService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for Longitudinal Retention Survival, Unemployment Duration,
 * Re-employment Conversion, and Employment Attrition Dashboards.
 *
 * <p>Base Route: /api/v1/dashboards
 * Authorization: requires {@code analytics.read} authority and nationwide administrative scope.
 */
@Validated
@RestController
@RequestMapping("/api/v1/dashboards")
@PreAuthorize("hasAuthority('analytics.read')")
public class RetentionAttritionDashboardController {

  private final RetentionAttritionDashboardService retentionAttritionDashboardService;
  private final WorkflowSecurityScopeValidator scopeValidator;

  public RetentionAttritionDashboardController(RetentionAttritionDashboardService retentionAttritionDashboardService) {
    this(retentionAttritionDashboardService, null);
  }

  @Autowired
  public RetentionAttritionDashboardController(
      RetentionAttritionDashboardService retentionAttritionDashboardService,
      @Autowired(required = false) WorkflowSecurityScopeValidator scopeValidator) {
    this.retentionAttritionDashboardService = retentionAttritionDashboardService;
    this.scopeValidator = scopeValidator;
  }

  /**
   * Retrieves longitudinal post-placement retention tracking metrics (formal Query Q12).
   * Exposes independent observable counts and survival rates at 6, 12, 24, and 36-month milestones.
   * Restricted to National Administrators with nationwide authority.
   *
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 200 OK with {@link LongitudinalRetentionResponse} in {@link ApiResponse}
   */
  @GetMapping("/retention")
  public ResponseEntity<ApiResponse<LongitudinalRetentionResponse>> getLongitudinalRetention(
      HttpServletRequest httpRequest) {
    if (scopeValidator != null) {
      scopeValidator.validateNationwideScope();
    }
    LongitudinalRetentionResponse response =
        retentionAttritionDashboardService.getLongitudinalRetention();
    return ResponseEntity.ok(ApiResponse.success(
        "Longitudinal retention analytics retrieved successfully",
        response,
        httpRequest.getRequestURI()
    ));
  }

  /**
   * Retrieves consolidated unemployment KPIs, duration metrics, re-employment conversion,
   * separation nature classifications, and exit root causes (formal Queries Q15, Q16, Q17, Q18).
   * Restricted to National Administrators with nationwide authority.
   *
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 200 OK with {@link AttritionDashboardResponse} in {@link ApiResponse}
   */
  @GetMapping("/attrition")
  public ResponseEntity<ApiResponse<AttritionDashboardResponse>> getAttritionDashboard(
      HttpServletRequest httpRequest) {
    if (scopeValidator != null) {
      scopeValidator.validateNationwideScope();
    }
    AttritionDashboardResponse response =
        retentionAttritionDashboardService.getAttritionDashboard();
    return ResponseEntity.ok(ApiResponse.success(
        "Attrition and unemployment analytics retrieved successfully",
        response,
        httpRequest.getRequestURI()
    ));
  }
}
