package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.response.EconomicOutcomeSummaryResponse;
import in.gov.sih.sih26135.dto.response.EmployerHiringPerformanceResponse;
import in.gov.sih.sih26135.dto.response.LeaderboardResponse;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import in.gov.sih.sih26135.service.EconomicOutcomeDashboardService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for Employer Hiring, Wage Growth, Economic Uplift, and Employment Mix Dashboards.
 * Exposes employer hiring scorecards (formal Query Q07) and consolidated economic outcome analytics
 * (formal Queries Q08, Q09, Q10, Q11).
 *
 * <p>Base Route: /api/v1/dashboards
 * Authorization: requires {@code analytics.read} authority and nationwide administrative scope.
 */
@Validated
@RestController
@RequestMapping("/api/v1/dashboards")
@PreAuthorize("hasAuthority('analytics.read')")
public class EconomicOutcomeDashboardController {

  private final EconomicOutcomeDashboardService economicOutcomeDashboardService;
  private final WorkflowSecurityScopeValidator scopeValidator;

  public EconomicOutcomeDashboardController(EconomicOutcomeDashboardService economicOutcomeDashboardService) {
    this(economicOutcomeDashboardService, null);
  }

  @Autowired
  public EconomicOutcomeDashboardController(
      EconomicOutcomeDashboardService economicOutcomeDashboardService,
      @Autowired(required = false) WorkflowSecurityScopeValidator scopeValidator) {
    this.economicOutcomeDashboardService = economicOutcomeDashboardService;
    this.scopeValidator = scopeValidator;
  }

  /**
   * Retrieves the employer hiring performance leaderboard scorecard (formal Query Q07).
   * Ranks employers by joined count descending, tie-broken by placement count descending.
   * Restricted to National Administrators with nationwide authority.
   *
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 200 OK with {@link LeaderboardResponse} of {@link EmployerHiringPerformanceResponse} in {@link ApiResponse}
   */
  @GetMapping("/employers")
  public ResponseEntity<ApiResponse<LeaderboardResponse<EmployerHiringPerformanceResponse>>> getEmployerLeaderboard(
      HttpServletRequest httpRequest) {
    if (scopeValidator != null) {
      scopeValidator.validateNationwideScope();
    }
    LeaderboardResponse<EmployerHiringPerformanceResponse> response =
        economicOutcomeDashboardService.getEmployerLeaderboard();
    return ResponseEntity.ok(ApiResponse.success(
        "Employer hiring leaderboard retrieved successfully",
        response,
        httpRequest.getRequestURI()
    ));
  }

  /**
   * Retrieves consolidated wage progression, economic uplift, salary distribution,
   * and employment mix analytics (formal Queries Q08, Q09, Q10, Q11).
   * Restricted to National Administrators with nationwide authority.
   *
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 200 OK with {@link EconomicOutcomeSummaryResponse} in {@link ApiResponse}
   */
  @GetMapping("/economic-outcomes")
  public ResponseEntity<ApiResponse<EconomicOutcomeSummaryResponse>> getEconomicOutcomeSummary(
      HttpServletRequest httpRequest) {
    if (scopeValidator != null) {
      scopeValidator.validateNationwideScope();
    }
    EconomicOutcomeSummaryResponse response =
        economicOutcomeDashboardService.getEconomicOutcomeSummary();
    return ResponseEntity.ok(ApiResponse.success(
        "Economic outcome analytics retrieved successfully",
        response,
        httpRequest.getRequestURI()
    ));
  }
}
