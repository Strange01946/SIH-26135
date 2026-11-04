package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.response.DashboardKpiStripResponse;
import in.gov.sih.sih26135.dto.response.ExecutiveDashboardResponse;
import in.gov.sih.sih26135.dto.response.LeaderboardResponse;
import in.gov.sih.sih26135.dto.response.PipelineFunnelResponse;
import in.gov.sih.sih26135.dto.response.SchemeProgramPerformanceResponse;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.ExecutiveDashboardService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for Executive and National Overview Dashboards.
 * Provides ministry leadership with consolidated visibility into national programme outcomes,
 * training pipeline conversions, and scheme comparative performance.
 *
 * <p>Base Route: /api/v1/dashboards
 * Authorization: requires {@code analytics.read} authority.
 */
@Validated
@RestController
@RequestMapping("/api/v1/dashboards")
@PreAuthorize("hasAuthority('analytics.read')")
public class ExecutiveDashboardController {

  private final ExecutiveDashboardService executiveDashboardService;

  public ExecutiveDashboardController(ExecutiveDashboardService executiveDashboardService) {
    this.executiveDashboardService = executiveDashboardService;
  }

  /**
   * Retrieves the executive overview dashboard combining Q01, Q02, and Q03.
   * Note: {@code /overview} and {@code /national} are intentional semantic aliases serving
   * the exact same consolidated national executive aggregation without parameters,
   * supporting generic executive navigation and national-hierarchy reporting respectively.
   *
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 200 OK with {@link ExecutiveDashboardResponse} enveloped in {@link ApiResponse}
   */
  @GetMapping("/overview")
  public ResponseEntity<ApiResponse<ExecutiveDashboardResponse>> getExecutiveOverview(
      HttpServletRequest httpRequest) {
    ExecutiveDashboardResponse response = executiveDashboardService.getExecutiveDashboard();
    return ResponseEntity.ok(ApiResponse.success(
        "Executive dashboard overview retrieved successfully",
        response,
        httpRequest.getRequestURI()
    ));
  }

  /**
   * Retrieves the national overview dashboard combining Q01, Q02, and Q03.
   * Note: {@code /overview} and {@code /national} are intentional semantic aliases serving
   * the exact same consolidated national executive aggregation without parameters,
   * supporting generic executive navigation and national-hierarchy reporting respectively.
   *
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 200 OK with {@link ExecutiveDashboardResponse} enveloped in {@link ApiResponse}
   */
  @GetMapping("/national")
  public ResponseEntity<ApiResponse<ExecutiveDashboardResponse>> getNationalOverview(
      HttpServletRequest httpRequest) {
    ExecutiveDashboardResponse response = executiveDashboardService.getExecutiveDashboard();
    return ResponseEntity.ok(ApiResponse.success(
        "National dashboard overview retrieved successfully",
        response,
        httpRequest.getRequestURI()
    ));
  }

  /**
   * Retrieves the standalone Dashboard KPI Strip (Q01).
   *
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 200 OK with {@link DashboardKpiStripResponse} enveloped in {@link ApiResponse}
   */
  @GetMapping("/kpis")
  public ResponseEntity<ApiResponse<DashboardKpiStripResponse>> getKpiStrip(
      HttpServletRequest httpRequest) {
    DashboardKpiStripResponse response = executiveDashboardService.getDashboardKpiStrip();
    return ResponseEntity.ok(ApiResponse.success(
        "Dashboard KPI strip retrieved successfully",
        response,
        httpRequest.getRequestURI()
    ));
  }

  /**
   * Retrieves the standalone Training Pipeline Conversion Funnel (Q02).
   *
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 200 OK with {@link PipelineFunnelResponse} enveloped in {@link ApiResponse}
   */
  @GetMapping("/pipeline-funnel")
  public ResponseEntity<ApiResponse<PipelineFunnelResponse>> getPipelineFunnel(
      HttpServletRequest httpRequest) {
    PipelineFunnelResponse response = executiveDashboardService.getTrainingPipelineFunnel();
    return ResponseEntity.ok(ApiResponse.success(
        "Training pipeline conversion funnel retrieved successfully",
        response,
        httpRequest.getRequestURI()
    ));
  }

  /**
   * Retrieves the standalone Scheme & Program Comparative Performance Matrix (Q03).
   *
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 200 OK with {@link LeaderboardResponse} enveloped in {@link ApiResponse}
   */
  @GetMapping("/scheme-matrix")
  public ResponseEntity<ApiResponse<LeaderboardResponse<SchemeProgramPerformanceResponse>>> getSchemeMatrix(
      HttpServletRequest httpRequest) {
    LeaderboardResponse<SchemeProgramPerformanceResponse> response = executiveDashboardService.getSchemeProgramPerformanceMatrix();
    return ResponseEntity.ok(ApiResponse.success(
        "Scheme and program performance matrix retrieved successfully",
        response,
        httpRequest.getRequestURI()
    ));
  }
}
