package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.response.CourseEmployabilityResponse;
import in.gov.sih.sih26135.dto.response.LeaderboardResponse;
import in.gov.sih.sih26135.dto.response.ProviderPerformanceResponse;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import in.gov.sih.sih26135.service.InstitutionalAnalyticsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for Institutional & Curriculum Performance Dashboards.
 * Exposes training provider rankings (Query Q04) and course outcome & employability scorecards (Query Q05).
 *
 * <p>Base Route: /api/v1/dashboards
 * Authorization: requires {@code analytics.read} authority and nationwide administrative scope.
 */
@Validated
@RestController
@RequestMapping("/api/v1/dashboards")
@PreAuthorize("hasAuthority('analytics.read')")
public class InstitutionalAnalyticsController {

  private final InstitutionalAnalyticsService institutionalAnalyticsService;
  private final WorkflowSecurityScopeValidator scopeValidator;

  public InstitutionalAnalyticsController(
      InstitutionalAnalyticsService institutionalAnalyticsService,
      WorkflowSecurityScopeValidator scopeValidator) {
    this.institutionalAnalyticsService = java.util.Objects.requireNonNull(institutionalAnalyticsService, "InstitutionalAnalyticsService is required");
    this.scopeValidator = java.util.Objects.requireNonNull(scopeValidator, "WorkflowSecurityScopeValidator is required");
  }

  /**
   * Retrieves the institutional performance leaderboard of training providers (formal query Q04).
   * Ranks providers by employment conversion rate descending, tie-broken by enrollment volume descending.
   * Restricted to National Administrators with nationwide authority.
   *
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 200 OK with {@link LeaderboardResponse} of {@link ProviderPerformanceResponse} in {@link ApiResponse}
   */
  @GetMapping("/providers")
  public ResponseEntity<ApiResponse<LeaderboardResponse<ProviderPerformanceResponse>>> getProviderLeaderboard(
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    LeaderboardResponse<ProviderPerformanceResponse> response =
        institutionalAnalyticsService.getProviderLeaderboard();
    return ResponseEntity.ok(ApiResponse.success(
        "Training provider leaderboard retrieved successfully",
        response,
        httpRequest.getRequestURI()
    ));
  }

  /**
   * Retrieves the course outcome and curriculum employability scorecard (formal query Q05).
   * Ranks courses by employment conversion rate descending, tie-broken by enrollment volume descending.
   * Restricted to National Administrators with nationwide authority.
   *
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 200 OK with {@link LeaderboardResponse} of {@link CourseEmployabilityResponse} in {@link ApiResponse}
   */
  @GetMapping("/courses")
  public ResponseEntity<ApiResponse<LeaderboardResponse<CourseEmployabilityResponse>>> getCourseLeaderboard(
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    LeaderboardResponse<CourseEmployabilityResponse> response =
        institutionalAnalyticsService.getCourseLeaderboard();
    return ResponseEntity.ok(ApiResponse.success(
        "Course employability leaderboard retrieved successfully",
        response,
        httpRequest.getRequestURI()
    ));
  }
}
