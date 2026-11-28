package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.request.AnalyticsFilterCriteria;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import in.gov.sih.sih26135.service.ExecutiveMisService;
import in.gov.sih.sih26135.service.ExecutiveMisService.FilterOptions;
import in.gov.sih.sih26135.service.ExecutiveMisService.ProviderQuality;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboards/mis")
@PreAuthorize(WorkflowSecurityScopeValidator.GOVERNMENT_MIS_READ)
@SecurityRequirement(name = "bearerAuth")
public class ExecutiveMisController {
  private final ExecutiveMisService mis;
  private final WorkflowSecurityScopeValidator scope;

  public ExecutiveMisController(ExecutiveMisService mis, WorkflowSecurityScopeValidator scope) {
    this.mis = mis;
    this.scope = scope;
  }

  @GetMapping("/filters")
  public ApiResponse<FilterOptions> getMisFilterOptions() {
    var criteria = new AnalyticsFilterCriteria();
    scope.applyGovernmentMisScope(criteria);
    return ApiResponse.ok(mis.filterOptions(criteria));
  }

  @GetMapping("/provider-quality")
  public ApiResponse<List<ProviderQuality>> getMisProviderQuality(@ParameterObject AnalyticsFilterCriteria criteria) {
    scope.applyGovernmentMisScope(criteria);
    return ApiResponse.ok(mis.providerQuality(criteria));
  }
}
