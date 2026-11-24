package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.request.AnalyticsFilterCriteria;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import in.gov.sih.sih26135.service.SpecializedAnalyticsService;
import in.gov.sih.sih26135.service.SpecializedAnalyticsService.DropoutSignal;
import in.gov.sih.sih26135.service.SpecializedAnalyticsService.ExitAreas;
import in.gov.sih.sih26135.service.SpecializedAnalyticsService.IncidentDay;
import in.gov.sih.sih26135.service.SpecializedAnalyticsService.WageDistributions;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/dashboards/specialized")
@PreAuthorize("hasAuthority('analytics.read')")
@SecurityRequirement(name = "bearerAuth")
public class SpecializedAnalyticsController {
  private final SpecializedAnalyticsService analytics;
  private final WorkflowSecurityScopeValidator scope;

  public SpecializedAnalyticsController(SpecializedAnalyticsService analytics, WorkflowSecurityScopeValidator scope) {
    this.analytics = analytics;
    this.scope = scope;
  }

  @GetMapping("/wage-distributions")
  public ApiResponse<WageDistributions> getSpecializedWages(@ParameterObject AnalyticsFilterCriteria criteria) {
    validateScope(criteria);
    return ApiResponse.ok(analytics.wages(criteria));
  }

  @GetMapping("/exit-areas")
  public ApiResponse<ExitAreas> getSpecializedExitAreas(@ParameterObject AnalyticsFilterCriteria criteria) {
    validateScope(criteria);
    return ApiResponse.ok(analytics.exitAreas(criteria));
  }

  @GetMapping("/dropout-signals")
  public ApiResponse<List<DropoutSignal>> getSpecializedDropoutSignals(@ParameterObject AnalyticsFilterCriteria criteria) {
    validateScope(criteria);
    return ApiResponse.ok(analytics.dropoutSignals(criteria));
  }

  @GetMapping("/incident-trend")
  public ApiResponse<List<IncidentDay>> getSpecializedIncidentTrend(@RequestParam(defaultValue = "30") @Min(7) @Max(90) int days) {
    scope.validateNationwideScope();
    return ApiResponse.ok(analytics.incidents(days));
  }

  private void validateScope(AnalyticsFilterCriteria criteria) {
    scope.validateNationwideScope();
    scope.validateAnalyticsFilterScope(criteria);
  }
}
