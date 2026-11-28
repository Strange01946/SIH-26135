package in.gov.sih.sih26135.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import in.gov.sih.sih26135.dto.trainee.TraineeSurvey;
import in.gov.sih.sih26135.dto.response.EmploymentRecordResponse;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.response.PagedResponse;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import in.gov.sih.sih26135.service.EmploymentRecordService;
import in.gov.sih.sih26135.service.CareerRecordStatus;
import in.gov.sih.sih26135.service.LatestCareerUpdate;
import in.gov.sih.sih26135.service.TraineeEmploymentService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/trainees/me")
@PreAuthorize("hasAuthority('ROLE_trainee')")
@SecurityRequirement(name = "bearerAuth")
public class TraineeCareerController {
  private final WorkflowSecurityScopeValidator scope;
  private final EmploymentRecordService employments;
  private final JdbcTemplate jdbc;
  private final TraineeEmploymentService traineeJobs;

  public TraineeCareerController(WorkflowSecurityScopeValidator scope, EmploymentRecordService employments,
      JdbcTemplate jdbc, TraineeEmploymentService traineeJobs) {
    this.scope = scope;
    this.employments = employments;
    this.jdbc = jdbc;
    this.traineeJobs = traineeJobs;
  }

  @GetMapping("/employment-history")
  public ApiResponse<PagedResponse<EmploymentPeriod>> getEmploymentHistory(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
    long traineeId = currentTrainee();
    var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startDate", "id"));
    var history = employments.getEmploymentRecordsByTrainee(traineeId, pageable);
    var training = traineeJobs.linkedTraining(traineeId, history.getContent().stream().map(EmploymentRecordResponse::id).toList());
    var records = history.map(record ->
        new EmploymentPeriod(record.id(), record.employerName(), record.jobRoleName(), record.engagementTypeCode(),
            record.startDate(), record.endDate(), record.employmentSpellStatusName(), record.recordVerificationStatusCode(),
            record.startingSalary(), record.currencyCode(), record.salaryFrequencyCode(), record.salaryFrequencyId(),
            record.enrollmentId(), record.employmentInfoSourceCode(), record.placementId() == null
                && "TRAINEE_DECLARATION".equals(record.employmentInfoSourceCode())
                && "SELF_REPORTED".equals(record.recordVerificationStatusCode()),
            training.getOrDefault(record.id(), List.of())));
    return ApiResponse.ok(PagedResponse.of(records));
  }

  @GetMapping("/employment-options")
  public ApiResponse<TraineeEmploymentService.Options> traineeEmploymentOptions() {
    return ApiResponse.ok(traineeJobs.options(currentTrainee()));
  }

  @PostMapping("/employment-history")
  public ApiResponse<Long> addTraineeEmployment(@Valid @RequestBody TraineeEmploymentService.JobInput input) {
    return ApiResponse.ok(traineeJobs.save(currentTrainee(), null, input));
  }

  @PutMapping("/employment-history/{id}")
  public ApiResponse<Long> updateTraineeEmployment(@PathVariable @Positive long id,
      @Valid @RequestBody TraineeEmploymentService.JobInput input) {
    return ApiResponse.ok(traineeJobs.save(currentTrainee(), id, input));
  }

  @GetMapping("/career-status")
  public ApiResponse<CareerStatus> getCareerStatus() {
    long traineeId = currentTrainee();
    var periods = jdbc.query("""
        SELECT u.unemployment_event_id,u.start_date,u.end_date,r.reason_name,u.is_current,v.status_name
        FROM trainee_unemployment_events u
        JOIN ref_unemployment_reason r ON r.unemployment_reason_id=u.unemployment_reason_id
        JOIN ref_record_verification_status v ON v.record_verification_status_id=u.record_verification_status_id
        WHERE u.trainee_id=? ORDER BY u.start_date DESC,u.unemployment_event_id DESC LIMIT 20
        """, (rs, row) -> new UnemploymentPeriod(rs.getLong(1), rs.getObject(2, LocalDate.class), rs.getObject(3, LocalDate.class),
            rs.getString(4), rs.getBoolean(5), rs.getString(6)), traineeId);
    var report = LatestCareerUpdate.find(jdbc, traineeId);
    return ApiResponse.ok(new CareerStatus(CareerRecordStatus.current(jdbc, traineeId, report), periods, report));
  }

  private long currentTrainee() {
    return scope.resolveAuthenticatedTrainee().filter(trainee -> trainee.getDeletedAt() == null)
        .map(Trainee::getId).orElseThrow(() -> new ResourceNotFoundException("No trainee profile is linked to this account"));
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record EmploymentPeriod(long id, String employer, String jobRole, String engagementType,
                                 LocalDate startDate, LocalDate endDate, String status, String verificationStatus,
                                 BigDecimal startingPay, String currency, String payFrequency, Long payFrequencyId,
                                 Long enrollmentId, String employmentSource, boolean editable,
                                 List<TraineeEmploymentService.LinkedTraining> relatedTraining) {}
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record CareerStatus(CareerRecordStatus status, List<UnemploymentPeriod> unemploymentPeriods,
                             TraineeSurvey.CareerUpdate reportedUpdate) {}
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record UnemploymentPeriod(long id, LocalDate startsOn, LocalDate endsOn, String reason, boolean current,
                                   String verificationStatus) {}
}
