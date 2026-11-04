package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.response.SkillGapDashboardResponse;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.SkillGapDashboardService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for Skill Gap, Industry Demand Alignment, and Remediation Analytics Dashboards.
 *
 * <p>Base Route: /api/v1/dashboards
 * Authorization: requires {@code analytics.read} authority.
 */
@Validated
@RestController
@RequestMapping("/api/v1/dashboards")
@PreAuthorize("hasAuthority('analytics.read')")
public class SkillGapDashboardController {

  private final SkillGapDashboardService skillGapDashboardService;

  public SkillGapDashboardController(SkillGapDashboardService skillGapDashboardService) {
    this.skillGapDashboardService = skillGapDashboardService;
  }

  /**
   * Retrieves skill gap dashboard analytics, including top current skill gaps (formal Query Q13)
   * and severity breakdown with remediation recommendation uptake metrics (formal Query Q14).
   *
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 200 OK with {@link SkillGapDashboardResponse} in {@link ApiResponse}
   */
  @GetMapping("/skill-gaps")
  public ResponseEntity<ApiResponse<SkillGapDashboardResponse>> getSkillGapDashboard(
      HttpServletRequest httpRequest) {
    SkillGapDashboardResponse response = skillGapDashboardService.getSkillGapDashboard();
    return ResponseEntity.ok(ApiResponse.success(
        "Skill gap analytics retrieved successfully",
        response,
        httpRequest.getRequestURI()
    ));
  }
}
