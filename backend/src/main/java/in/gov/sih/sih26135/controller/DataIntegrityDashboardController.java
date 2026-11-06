package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.response.FollowupDashboardResponse;
import in.gov.sih.sih26135.dto.response.SurveyDashboardResponse;
import in.gov.sih.sih26135.dto.response.VerificationDashboardResponse;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.DataIntegrityDashboardService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for Verification Integrity, Longitudinal Follow-up, and Survey Analytics Dashboards.
 *
 * <p>Base Route: /api/v1/dashboards
 * Authorization: requires {@code analytics.read} authority.
 */
@Validated
@RestController
@RequestMapping("/api/v1/dashboards")
@PreAuthorize("hasAuthority('analytics.read')")
public class DataIntegrityDashboardController {

  private final DataIntegrityDashboardService dataIntegrityDashboardService;

  public DataIntegrityDashboardController(DataIntegrityDashboardService dataIntegrityDashboardService) {
    this.dataIntegrityDashboardService = dataIntegrityDashboardService;
  }

  /**
   * Retrieves employment verification integrity and method efficacy analytics (formal Queries Q21 & Q22).
   *
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 200 OK with {@link VerificationDashboardResponse} in {@link ApiResponse}
   */
  @GetMapping("/verifications")
  public ResponseEntity<ApiResponse<VerificationDashboardResponse>> getVerificationDashboard(
      HttpServletRequest httpRequest) {
    VerificationDashboardResponse response = dataIntegrityDashboardService.getVerificationDashboard();
    return ResponseEntity.ok(ApiResponse.success(
        "Verification integrity analytics retrieved successfully",
        response,
        httpRequest.getRequestURI()
    ));
  }

  /**
   * Retrieves longitudinal follow-up outreach success and SLA tracking analytics (formal Query Q19).
   *
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 200 OK with {@link FollowupDashboardResponse} in {@link ApiResponse}
   */
  @GetMapping("/followups")
  public ResponseEntity<ApiResponse<FollowupDashboardResponse>> getFollowupDashboard(
      HttpServletRequest httpRequest) {
    FollowupDashboardResponse response = dataIntegrityDashboardService.getFollowupDashboard();
    return ResponseEntity.ok(ApiResponse.success(
        "Follow-up outreach analytics retrieved successfully",
        response,
        httpRequest.getRequestURI()
    ));
  }

  /**
   * Retrieves post-training survey response pipeline and satisfaction analytics (formal Query Q20).
   *
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 200 OK with {@link SurveyDashboardResponse} in {@link ApiResponse}
   */
  @GetMapping("/surveys")
  public ResponseEntity<ApiResponse<SurveyDashboardResponse>> getSurveyDashboard(
      HttpServletRequest httpRequest) {
    SurveyDashboardResponse response = dataIntegrityDashboardService.getSurveyDashboard();
    return ResponseEntity.ok(ApiResponse.success(
        "Survey response pipeline analytics retrieved successfully",
        response,
        httpRequest.getRequestURI()
    ));
  }
}
