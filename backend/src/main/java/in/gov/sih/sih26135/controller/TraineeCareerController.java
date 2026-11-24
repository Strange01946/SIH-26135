package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.response.PagedResponse;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import in.gov.sih.sih26135.service.EmploymentRecordService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/trainees/me/employment-history")
@PreAuthorize("hasAuthority('ROLE_trainee')")
@SecurityRequirement(name = "bearerAuth")
public class TraineeCareerController {
  private final WorkflowSecurityScopeValidator scope;
  private final EmploymentRecordService employments;

  public TraineeCareerController(WorkflowSecurityScopeValidator scope, EmploymentRecordService employments) {
    this.scope = scope;
    this.employments = employments;
  }

  @GetMapping
  public ApiResponse<PagedResponse<EmploymentPeriod>> getEmploymentHistory(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
    long traineeId = scope.resolveAuthenticatedTrainee().filter(trainee -> trainee.getDeletedAt() == null)
        .map(Trainee::getId).orElseThrow(() -> new ResourceNotFoundException("No trainee profile is linked to this account"));
    var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startDate", "id"));
    var records = employments.getEmploymentRecordsByTrainee(traineeId, pageable).map(record ->
        new EmploymentPeriod(record.id(), record.employerName(), record.jobRoleName(), record.engagementTypeCode(),
            record.startDate(), record.endDate(), record.employmentSpellStatusName(), record.recordVerificationStatusCode(),
            record.startingSalary(), record.currencyCode(), record.salaryFrequencyCode()));
    return ApiResponse.ok(PagedResponse.of(records));
  }

  public record EmploymentPeriod(long id, String employer, String jobRole, String engagementType,
                                 LocalDate startDate, LocalDate endDate, String status, String verificationStatus,
                                 BigDecimal startingPay, String currency, String payFrequency) {}
}
