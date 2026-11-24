package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.communication.service.CommunicationCenterQueryService;
import in.gov.sih.sih26135.communication.service.CommunicationCenterQueryService.JobQueue;
import in.gov.sih.sih26135.communication.service.CommunicationCenterQueryService.LogRow;
import in.gov.sih.sih26135.communication.service.CommunicationDemoService;
import in.gov.sih.sih26135.communication.service.CommunicationDemoService.ConsentCheck;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.response.PagedResponse;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/communication")
@PreAuthorize("hasAnyAuthority('communication.manage', 'survey.manage', 'system.manage')")
public class CommunicationCenterController {
  private final CommunicationCenterQueryService queries;
  private final CommunicationDemoService demo;
  private final WorkflowSecurityScopeValidator scope;

  public CommunicationCenterController(CommunicationCenterQueryService queries, CommunicationDemoService demo,
      WorkflowSecurityScopeValidator scope) {
    this.queries = queries;
    this.demo = demo;
    this.scope = scope;
  }

  @GetMapping("/center")
  public ApiResponse<Capabilities> capabilities() {
    requireOperationsScope();
    return ApiResponse.ok(new Capabilities(demo.isEnabled()));
  }

  @GetMapping("/logs")
  public ApiResponse<PagedResponse<LogRow>> logs(
      @RequestParam(required = false) @Size(max = 32) String channel,
      @RequestParam(required = false) @Size(max = 32) String status,
      @RequestParam(required = false) @Size(max = 64) String q,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    requireOperationsScope();
    return ApiResponse.ok(queries.logs(channel, status, q, page, size));
  }

  @GetMapping("/jobs")
  public ApiResponse<JobQueue> jobs(
      @RequestParam(required = false) @Size(max = 32) String status,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    requireOperationsScope();
    return ApiResponse.ok(queries.jobs(status, page, size));
  }

  @PostMapping("/demo/receipts")
  public ApiResponse<LogRow> createDemoReceipt() {
    requireOperationsScope();
    return ApiResponse.ok(demo.createReceipt(scope.resolveAuthenticatedUserId()));
  }

  @PostMapping("/demo/receipts/{logId}/deliver")
  public ApiResponse<LogRow> deliverDemoReceipt(@PathVariable @Positive long logId) {
    requireOperationsScope();
    return ApiResponse.ok(demo.deliverReceipt(logId, scope.resolveAuthenticatedUserId()));
  }

  @PostMapping("/demo/consent")
  public ApiResponse<ConsentCheck> checkConsent() {
    requireOperationsScope();
    return ApiResponse.ok(demo.checkWithdrawnConsent(scope.resolveAuthenticatedUserId()));
  }

  private void requireOperationsScope() {
    scope.validateNationwideScope();
    if (!scope.isAdministrativeCaller()) {
      throw new AccessDeniedException("Communication operations require administrative access");
    }
  }

  public record Capabilities(boolean demoAvailable) {}
}
