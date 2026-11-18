package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.communication.dto.DispatchAttemptDto;
import in.gov.sih.sih26135.communication.dto.DispatchJobDto;
import in.gov.sih.sih26135.communication.queue.DurableDispatchQueueService;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.response.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.security.Principal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
 */
@RestController
@RequestMapping("/api/v1/communication")
@Validated
@PreAuthorize("hasAnyAuthority('communication.manage', 'system.manage')")
public class CommunicationJobController {

  private final DurableDispatchQueueService queueService;

  @Autowired
  public CommunicationJobController(DurableDispatchQueueService queueService) {
    this.queueService = queueService;
  }

  /**
   * Retrieves durable dispatch job details by primary key identifier.
   */
  @GetMapping("/jobs/{jobId}")
  public ResponseEntity<ApiResponse<DispatchJobDto>> getJobById(@PathVariable @Positive Long jobId) {
    DispatchJobDto job = queueService.getJobById(jobId);
    if (job == null) {
      throw new ResourceNotFoundException("DispatchJob", "id");
    }
    return ResponseEntity.ok(ApiResponse.ok(job));
  }

  /**
   * Retrieves execution attempt audit history for a dispatch job.
   */
  @GetMapping("/jobs/{jobId}/attempts")
  public ResponseEntity<ApiResponse<List<DispatchAttemptDto>>> getJobAttempts(@PathVariable @Positive Long jobId) {
    List<DispatchAttemptDto> attempts = queueService.getAttemptsByJobId(jobId);
    return ResponseEntity.ok(ApiResponse.ok(attempts));
  }

  /**
   * Retrieves paginated dead-letter queue records for administrative review.
   */
  @GetMapping("/dead-letter")
  public ResponseEntity<ApiResponse<List<DispatchJobDto>>> getDeadLetterJobs(
      @RequestParam(name = "offset", defaultValue = "0") @Min(0) int offset,
      @RequestParam(name = "limit", defaultValue = "20") @Min(1) @Max(100) int limit) {
    List<DispatchJobDto> deadLetters = queueService.getDeadLetterJobs(offset, limit);
    return ResponseEntity.ok(ApiResponse.ok(deadLetters));
  }

  /**
   * Manually replays an exhausted or dead-lettered job after issue remediation.
   */
  @PostMapping("/dead-letter/{jobId}/replay")
  public ResponseEntity<ApiResponse<DispatchJobDto>> replayDeadLetterJob(
      @PathVariable @Positive Long jobId,
      Principal principal) {
    String operatorUsername = principal != null ? principal.getName() : "system_operator";
    DispatchJobDto replayedJob = queueService.replayDeadLetterJob(jobId, operatorUsername);
    return ResponseEntity.ok(ApiResponse.ok(replayedJob));
  }
}
