package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.config.cache.AnalyticsCacheEvictionService;
import in.gov.sih.sih26135.dto.request.CreateEmploymentRecordRequest;
import in.gov.sih.sih26135.dto.request.CreateSalaryHistoryRequest;
import in.gov.sih.sih26135.dto.request.UpdateEmploymentRecordRequest;
import in.gov.sih.sih26135.entity.EmploymentRecord;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.repository.EmploymentRecordRepository;
import in.gov.sih.sih26135.repository.TrainingEnrollmentRepository;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TraineeEmploymentService {
  private final JdbcTemplate jdbc;
  private final EmploymentRecordService employments;
  private final EmploymentRecordRepository records;
  private final TrainingEnrollmentRepository enrollments;
  private final SalaryHistoryService wages;
  private final TraineeConsentService consent;
  private final AuditLogService audit;
  private final AnalyticsCacheEvictionService analytics;

  public TraineeEmploymentService(JdbcTemplate jdbc, EmploymentRecordService employments,
      EmploymentRecordRepository records, TrainingEnrollmentRepository enrollments, SalaryHistoryService wages,
      TraineeConsentService consent, AuditLogService audit, AnalyticsCacheEvictionService analytics) {
    this.jdbc = jdbc;
    this.employments = employments;
    this.records = records;
    this.enrollments = enrollments;
    this.wages = wages;
    this.consent = consent;
    this.audit = audit;
    this.analytics = analytics;
  }

  public Options options(long traineeId) {
    var frequencies = jdbc.query("""
        SELECT salary_frequency_id,frequency_name FROM ref_salary_frequency ORDER BY sort_order
        """, (rs, row) -> new PayFrequency(rs.getLong(1), rs.getString(2)));
    return new Options(frequencies, completedTraining(traineeId));
  }

  @Transactional
  public long save(long traineeId, Long employmentId, JobInput input) {
    jdbc.queryForObject("SELECT trainee_id FROM trainees WHERE trainee_id=? FOR UPDATE", Long.class, traineeId);
    consent.validateConsent(traineeId, "DATA_PROCESSING");
    if (input.endDate() != null && input.endDate().isBefore(input.startDate())) {
      throw new BadRequestException("Last working day must be on or after the joining date.", "INVALID_DATE_RANGE");
    }
    Long enrollmentId = input.enrollmentId();
    long source = jdbc.queryForObject("""
        SELECT employment_info_source_id FROM ref_employment_info_source WHERE source_code='TRAINEE_DECLARATION'
        """, Long.class);
    long declared = jdbc.queryForObject("""
        SELECT record_verification_status_id FROM ref_record_verification_status WHERE status_code='SELF_REPORTED'
        """, Long.class);
    long status = jdbc.queryForObject("""
        SELECT employment_spell_status_id FROM ref_employment_spell_status WHERE status_code=?
        """, Long.class, input.endDate() == null ? "ACTIVE" : "ENDED");

    EmploymentRecord record = employmentId == null ? null : records.findById(employmentId).filter(existing -> existing.getDeletedAt() == null
        && existing.getTrainee().getId().equals(traineeId)
        && existing.getPlacementRecord() == null && existing.getEmploymentInfoSource().getId().equals(source)
        && existing.getRecordVerificationStatus().getId().equals(declared))
        .orElseThrow(() -> new ResourceNotFoundException("Editable employment record was not found."));
    List<Long> linkedIds = validateTrainingLinks(input, traineeId, record);
    if (employmentId == null) {
      long wageType = jdbc.queryForObject("""
          SELECT engagement_type_id FROM ref_engagement_type WHERE type_code='WAGE_EMPLOYMENT'
          """, Long.class);
      var request = new CreateEmploymentRecordRequest("EMP-" + UUID.randomUUID().toString().substring(0, 24),
          traineeId, wageType, status, input.startDate(), source, declared);
      request.setEndDate(input.endDate());
      request.setEnrollmentId(enrollmentId);
      request.setStartingSalary(input.pay());
      request.setSalaryFrequencyId(input.payFrequencyId());
      record = records.findById(employments.createSelfReportedEmployment(request).id()).orElseThrow();
    } else {
      validateHistory(record, input, source);
      var changes = new UpdateEmploymentRecordRequest();
      changes.setStartDate(input.startDate());
      changes.setEndDate(input.endDate());
      changes.setIsCurrent(input.endDate() == null);
      changes.setEmploymentSpellStatusId(status);
      changes.setSalaryFrequencyId(input.payFrequencyId());
      employments.updateEmploymentRecord(employmentId, changes);
      record.setEnrollment(enrollmentId == null ? null : enrollments.getReferenceById(enrollmentId));
      record.setStartingSalary(input.pay());
    }
    record.setReportedJobTitle(input.jobTitle().strip());
    record.setReportedEmployerName(input.employerName() == null || input.employerName().isBlank() ? null : input.employerName().strip());
    records.saveAndFlush(record);
    long jobId = record.getId();
    jdbc.update("DELETE FROM employment_training_links WHERE employment_id=?", jobId);
    var secondaryLinks = linkedIds.stream().filter(id -> !id.equals(enrollmentId))
        .map(id -> new Object[]{jobId, id}).toList();
    if (!secondaryLinks.isEmpty()) {
      jdbc.batchUpdate("INSERT INTO employment_training_links(employment_id,enrollment_id) VALUES(?,?)", secondaryLinks);
    }
    saveStartingPay(jobId, traineeId, input, source, declared);
    audit.recordStateChange(employmentId == null ? "CREATE" : "UPDATE", "EmploymentRecord", jobId,
        "Trainee saved employment details", null, Map.of("traineeId", traineeId, "trainingEnrollmentIds", linkedIds), List.of());
    analytics.evictOnPlacementTransition(this);
    return jobId;
  }

  private List<Long> validateTrainingLinks(JobInput input, long traineeId, EmploymentRecord record) {
    Long primary = input.enrollmentId();
    List<Long> ids = input.trainingEnrollmentIds();
    if (ids == null) {
      if (primary == null) return List.of();
      ids = new ArrayList<>();
      ids.add(primary);
      if (record != null && record.getEnrollment() != null && primary.equals(record.getEnrollment().getId())) {
        ids.addAll(jdbc.queryForList(
            "SELECT enrollment_id FROM employment_training_links WHERE employment_id=?", Long.class, record.getId()));
      }
    }
    if ((ids.isEmpty() && primary != null) || (!ids.isEmpty() && !ids.contains(primary))) {
      throw new BadRequestException("Choose a primary training from your selections.", "PRIMARY_EMPLOYMENT_TRAINING_REQUIRED");
    }
    if (new HashSet<>(ids).size() != ids.size()) {
      throw new BadRequestException("Select each training enrollment once.", "DUPLICATE_EMPLOYMENT_TRAINING");
    }
    if (!ids.isEmpty()) {
      var eligible = completedTraining(traineeId).stream()
          .filter(training -> !training.completedOn().isAfter(input.startDate()))
          .map(CompletedTraining::id).collect(java.util.stream.Collectors.toSet());
      if (!eligible.containsAll(ids)) {
        throw new BadRequestException("Choose training enrollments you completed by the joining date.", "INVALID_EMPLOYMENT_ENROLLMENT");
      }
    }
    return ids;
  }

  public Map<Long, List<LinkedTraining>> linkedTraining(long traineeId, List<Long> employmentIds) {
    if (employmentIds.isEmpty()) return Map.of();
    String placeholders = String.join(",", Collections.nCopies(employmentIds.size(), "?"));
    List<Object> parameters = new ArrayList<>();
    parameters.add(traineeId);
    parameters.addAll(employmentIds);
    return jdbc.query("""
        WITH jobs AS (
          SELECT employment_id,enrollment_id FROM employment_records
          WHERE trainee_id=? AND deleted_at IS NULL AND employment_id IN (%s)
        ), training_links AS (
          SELECT employment_id,enrollment_id,1 is_primary FROM jobs WHERE enrollment_id IS NOT NULL
          UNION ALL
          SELECT j.employment_id,x.enrollment_id,0 FROM jobs j
          JOIN employment_training_links x USING(employment_id)
          WHERE NOT (x.enrollment_id <=> j.enrollment_id)
        )
        SELECT x.employment_id,e.enrollment_id,c.course_name,b.batch_code,
          COALESCE(e.actual_completion_date,b.end_date) completed_on,x.is_primary
        FROM training_links x JOIN training_enrollments e USING(enrollment_id)
        JOIN courses c USING(course_id) LEFT JOIN training_batches b USING(batch_id)
        ORDER BY x.is_primary DESC,completed_on DESC,e.enrollment_id DESC
        """.formatted(placeholders), rs -> {
      Map<Long, List<LinkedTraining>> result = new HashMap<>();
      while (rs.next()) {
        result.computeIfAbsent(rs.getLong(1), id -> new ArrayList<>()).add(new LinkedTraining(
            rs.getLong(2), rs.getString(3), rs.getString(4), rs.getObject(5, LocalDate.class), rs.getBoolean(6)));
      }
      return result;
    }, parameters.toArray());
  }

  private void validateHistory(EmploymentRecord record, JobInput input, long source) {
    if (!Objects.equals(record.getEndDate(), input.endDate()) && Boolean.TRUE.equals(jdbc.queryForObject("""
        SELECT EXISTS(SELECT 1 FROM employment_exit_events WHERE employment_id=?)
        """, Boolean.class, record.getId()))) {
      throw new ConflictException("This job already has an exit event. Its last working day is recorded there.", "EXIT_ALREADY_RECORDED");
    }
    if (Boolean.TRUE.equals(jdbc.queryForObject("""
        SELECT EXISTS(SELECT 1 FROM salary_history WHERE employment_id=?
          AND NOT (observation_month_offset <=> 0 AND employment_info_source_id=? AND verified_at IS NULL)
          AND (effective_from < ? OR (? IS NOT NULL AND effective_from > ?)))
        """, Boolean.class, record.getId(), source, input.startDate(), input.endDate(), input.endDate()))) {
      throw new ConflictException("The employment dates must include the existing wage observations.", "EMPLOYMENT_WAGE_DATES_CONFLICT");
    }
  }

  private void saveStartingPay(long id, long traineeId, JobInput input, long source, long declared) {
    var baseline = jdbc.query("""
        SELECT salary_history_id, employment_info_source_id, verified_at FROM salary_history
        WHERE employment_id=? AND observation_month_offset=0
        """, (rs, row) -> new Baseline(rs.getLong(1), rs.getLong(2), rs.getObject(3) != null), id)
        .stream().findFirst().orElse(null);
    if (baseline != null && (baseline.source() != source || baseline.verified())) {
      throw new ConflictException("Starting pay was recorded by another source.", "STARTING_PAY_ALREADY_RECORDED");
    }
    if (input.pay() == null) {
      if (baseline != null) wages.deleteSalaryHistory(baseline.id());
    } else if (baseline != null) {
      jdbc.update("""
          UPDATE salary_history SET salary_amount=?,salary_frequency_id=?,effective_from=?
          WHERE salary_history_id=?
          """, input.pay(), input.payFrequencyId(), input.startDate(), baseline.id());
    } else {
      var startingPay = new CreateSalaryHistoryRequest();
      startingPay.setEmploymentId(id);
      startingPay.setTraineeId(traineeId);
      startingPay.setSalaryAmount(input.pay());
      startingPay.setSalaryFrequencyId(input.payFrequencyId());
      startingPay.setCurrencyCode("INR");
      startingPay.setEffectiveFrom(input.startDate());
      startingPay.setObservationMonthOffset(0);
      startingPay.setEmploymentInfoSourceId(source);
      startingPay.setRecordVerificationStatusId(declared);
      wages.createSalaryHistory(startingPay);
    }
  }

  private List<CompletedTraining> completedTraining(long traineeId) {
    return jdbc.query("""
        SELECT e.enrollment_id,c.course_name,b.batch_code,COALESCE(e.actual_completion_date,b.end_date) completed_on
        FROM training_enrollments e JOIN courses c USING(course_id)
        JOIN ref_enrollment_status s USING(enrollment_status_id) LEFT JOIN training_batches b USING(batch_id)
        WHERE e.trainee_id=? AND e.deleted_at IS NULL AND s.is_completed_flag=1
          AND COALESCE(e.actual_completion_date,b.end_date) IS NOT NULL
        ORDER BY completed_on DESC,e.enrollment_id DESC
        """, (rs, row) -> new CompletedTraining(rs.getLong(1), rs.getString(2), rs.getString(3),
            rs.getObject(4, LocalDate.class)), traineeId);
  }

  private record Baseline(long id, long source, boolean verified) {}
  public record JobInput(@NotBlank @Size(max = 160) String jobTitle, @Size(max = 160) String employerName,
                         @NotNull @PastOrPresent LocalDate startDate, @PastOrPresent LocalDate endDate,
                         @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal pay,
                         @NotNull @Positive Long payFrequencyId, @Positive Long enrollmentId,
                         List<@NotNull @Positive Long> trainingEnrollmentIds) {}
  public record PayFrequency(long id, String label) {}
  public record CompletedTraining(long id, String name, String batchCode, LocalDate completedOn) {}
  public record LinkedTraining(long id, String name, String batchCode, LocalDate completedOn, boolean primary) {}
  public record Options(List<PayFrequency> payFrequencies, List<CompletedTraining> completedTraining) {}
}
