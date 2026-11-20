package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.communication.dto.DispatchAttemptDto;
import in.gov.sih.sih26135.communication.dto.DispatchJobDto;
import in.gov.sih.sih26135.communication.queue.DurableDispatchQueueService;
import in.gov.sih.sih26135.entity.User;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.security.InternalAuthenticationToken;
import in.gov.sih.sih26135.security.InternalServicePrincipal;
import in.gov.sih.sih26135.security.TenantScopeType;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.security.Principal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Administrative and operational REST endpoints for monitoring durable dispatch jobs,
 * inspecting execution attempt history, and reviewing/replaying dead-lettered notifications.
 *
 * <p>Enforces strict multi-tenant isolation:
 * <ul>
 *   <li>Training providers are strictly confined to their own provider's dispatch jobs and dead letters.</li>
 *   <li>Cross-tenant inspection or replay attempts by provider-scoped users are denied.</li>
 *   <li>Nationwide access requires verified administrative authorities (super_admin or nationwide government_admin).</li>
 * </ul>
 */
@Tag(name = "Communication - Dispatch Queue", description = "Multi-channel asynchronous delivery queues and dispatch jobs")
@RestController
@RequestMapping("/api/v1/communication")
@Validated
@PreAuthorize("hasAnyAuthority('communication.manage', 'system.manage')")
public class CommunicationJobController {

  private final DurableDispatchQueueService queueService;
  private final WorkflowSecurityScopeValidator securityScopeValidator;

  @Autowired
  public CommunicationJobController(
      DurableDispatchQueueService queueService,
      WorkflowSecurityScopeValidator securityScopeValidator) {
    this.queueService = Objects.requireNonNull(queueService, "DurableDispatchQueueService is required");
    this.securityScopeValidator = Objects.requireNonNull(securityScopeValidator, "WorkflowSecurityScopeValidator is required");
  }

  /**
   * Retrieves durable dispatch job details by primary key identifier, enforcing tenant isolation.
   */
  @Operation(
      summary = "Get Dispatch Job by ID",
      description = "Retrieves durable dispatch job details and delivery status, enforcing tenant isolation.",
      security = @SecurityRequirement(name = "bearerAuth"),
      responses = {
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Dispatch job found"),
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied - job belongs to different tenant"),
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Dispatch job not found")
      }
  )
  @GetMapping("/jobs/{jobId}")
  public ResponseEntity<ApiResponse<DispatchJobDto>> getJobById(@PathVariable @Positive Long jobId) {
    DispatchJobDto job = queueService.getJobById(jobId);
    if (job == null) {
      throw new ResourceNotFoundException("DispatchJob", "id");
    }

    validateJobTenantScope(job);
    return ResponseEntity.ok(ApiResponse.ok(job));
  }

  /**
   * Retrieves execution attempt audit history for a dispatch job, enforcing tenant isolation.
   */
  @Operation(
      summary = "Get Job Execution Attempts",
      description = "Retrieves execution attempt audit history for a dispatch job, enforcing tenant isolation.",
      security = @SecurityRequirement(name = "bearerAuth"),
      responses = {
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Job execution attempts returned"),
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied - job belongs to different tenant"),
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Dispatch job not found")
      }
  )
  @GetMapping("/jobs/{jobId}/attempts")
  public ResponseEntity<ApiResponse<List<DispatchAttemptDto>>> getJobAttempts(@PathVariable @Positive Long jobId) {
    DispatchJobDto job = queueService.getJobById(jobId);
    if (job == null) {
      throw new ResourceNotFoundException("DispatchJob", "id");
    }

    validateJobTenantScope(job);
    List<DispatchAttemptDto> attempts = queueService.getAttemptsByJobId(jobId);
    return ResponseEntity.ok(ApiResponse.ok(attempts));
  }

  /**
   * Retrieves paginated dead-letter queue records for administrative review,
   * strictly partitioned by authorized tenant.
   */
  @Operation(
      summary = "Get Dead-Letter Queue Jobs",
      description = "Retrieves paginated dead-letter queue records for administrative review, strictly partitioned by authorized tenant.",
      security = @SecurityRequirement(name = "bearerAuth"),
      responses = {
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Dead letter jobs retrieved successfully"),
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied - caller lacks required tenant scope")
      }
  )
  @GetMapping("/dead-letter")
  public ResponseEntity<ApiResponse<List<DispatchJobDto>>> getDeadLetterJobs(
      @RequestParam(name = "offset", defaultValue = "0") @Min(0) int offset,
      @RequestParam(name = "limit", defaultValue = "20") @Min(1) @Max(100) int limit) {

    if (securityScopeValidator.isAdministrativeCaller()) {
      List<DispatchJobDto> deadLetters = queueService.getDeadLetterJobs(offset, limit);
      return ResponseEntity.ok(ApiResponse.ok(deadLetters));
    }

    // Provider-scoped user: strictly restrict to their own provider ID
    Long providerId = resolveCallerProviderId();
    if (providerId == null) {
      throw new AccessDeniedException("Access denied: caller has no authorized training provider scope");
    }

    List<DispatchJobDto> deadLetters = queueService.getDeadLetterJobsByProvider(providerId, offset, limit);
    return ResponseEntity.ok(ApiResponse.ok(deadLetters));
  }

  /**
   * Manually replays an exhausted or dead-lettered job after issue remediation,
   * enforcing strict tenant isolation on the replay operation.
   */
  @Operation(
      summary = "Replay Dead-Letter Job",
      description = "Manually replays an exhausted or dead-lettered job after issue remediation, enforcing strict tenant isolation.",
      security = @SecurityRequirement(name = "bearerAuth"),
      responses = {
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Job re-enqueued for delivery"),
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied - job belongs to different tenant"),
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Dispatch job not found")
      }
  )
  @PostMapping("/dead-letter/{jobId}/replay")
  public ResponseEntity<ApiResponse<DispatchJobDto>> replayDeadLetterJob(
      @PathVariable @Positive Long jobId,
      Principal principal) {

    DispatchJobDto job = queueService.getJobById(jobId);
    if (job == null) {
      throw new ResourceNotFoundException("DispatchJob", "id");
    }

    validateJobTenantScope(job);
    String operatorUsername = principal != null ? principal.getName() : "system_operator";
    DispatchJobDto replayedJob = queueService.replayDeadLetterJob(jobId, operatorUsername);
    return ResponseEntity.ok(ApiResponse.ok(replayedJob));
  }

  /**
   * Authoritatively validates that the authenticated caller has permission to view/operate on the given job.
   */
  private void validateJobTenantScope(DispatchJobDto job) {
    if (job.trainingProviderId() != null) {
      securityScopeValidator.validateProviderScope(job.trainingProviderId());
    } else {
      securityScopeValidator.validateNationwideScope();
    }
  }

  private Long resolveCallerProviderId() {
    Optional<User> authUser = securityScopeValidator.resolveAuthenticatedUser();
    if (authUser.isPresent() && authUser.get().getTrainingProviderId() != null) {
      return authUser.get().getTrainingProviderId();
    }

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth instanceof InternalAuthenticationToken internalAuth) {
      InternalServicePrincipal principal = (InternalServicePrincipal) internalAuth.getPrincipal();
      if (principal.getTenantScopeType() == TenantScopeType.PROVIDER) {
        return principal.getTenantId();
      }
    }

    return null;
  }
}
