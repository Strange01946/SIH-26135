package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.config.cache.AnalyticsCacheEvictionService;
import in.gov.sih.sih26135.dto.career.CareerProgress.*;
import in.gov.sih.sih26135.dto.request.CreateEmploymentExitEventRequest;
import in.gov.sih.sih26135.dto.request.CreateTraineeUnemploymentEventRequest;
import in.gov.sih.sih26135.dto.request.EmploymentExitAndUnemploymentRequest;
import in.gov.sih.sih26135.dto.response.EmploymentExitAndUnemploymentResponse;
import in.gov.sih.sih26135.dto.response.EmploymentRecordResponse;
import in.gov.sih.sih26135.dto.response.TraineeUnemploymentEventResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.repository.EmploymentRecordRepository;
import in.gov.sih.sih26135.repository.RefEmploymentSpellStatusRepository;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CareerProgressService {
  private static final String CANDIDATES = """
      SELECT t.trainee_id, t.registration_number, CONCAT_WS(' ', t.first_name, t.last_name),
             s.status_code, s.status_name
      FROM trainees t
      JOIN ref_employment_status s ON s.employment_status_id = t.current_employment_status_id
      WHERE t.deleted_at IS NULL
      """;
  private static final RowMapper<Candidate> CANDIDATE = (rs, row) ->
      new Candidate(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5));

  private final JdbcTemplate jdbc;
  private final WorkflowSecurityScopeValidator scope;
  private final EmploymentLifecycleWorkflowService lifecycle;
  private final EmploymentRecordService employments;
  private final EmploymentRecordRepository employmentRecords;
  private final EmploymentExitEventService exits;
  private final TraineeUnemploymentEventService unemployment;
  private final SalaryHistoryService wages;
  private final RefEmploymentSpellStatusRepository spellStatuses;
  private final AnalyticsCacheEvictionService analyticsCache;
  private final TraineeConsentService consent;

  public CareerProgressService(JdbcTemplate jdbc, WorkflowSecurityScopeValidator scope,
      EmploymentLifecycleWorkflowService lifecycle, EmploymentRecordService employments,
      EmploymentRecordRepository employmentRecords, EmploymentExitEventService exits,
      TraineeUnemploymentEventService unemployment, SalaryHistoryService wages,
      RefEmploymentSpellStatusRepository spellStatuses, AnalyticsCacheEvictionService analyticsCache,
      TraineeConsentService consent) {
    this.jdbc = jdbc;
    this.scope = scope;
    this.lifecycle = lifecycle;
    this.employments = employments;
    this.employmentRecords = employmentRecords;
    this.exits = exits;
    this.unemployment = unemployment;
    this.wages = wages;
    this.spellStatuses = spellStatuses;
    this.analyticsCache = analyticsCache;
    this.consent = consent;
  }

  public List<Candidate> findCandidates(String search) {
    String term = "%" + search.strip().replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    return jdbc.query(CANDIDATES + """
        AND (t.registration_number LIKE ? ESCAPE '!'
          OR CONCAT_WS(' ', t.first_name, t.last_name) LIKE ? ESCAPE '!')
        ORDER BY t.last_name, t.first_name, t.trainee_id LIMIT 25
        """, CANDIDATE, term, term);
  }

  public CandidateHistory history(long traineeId) {
    var candidate = jdbc.query(CANDIDATES + " AND t.trainee_id = ?", CANDIDATE, traineeId).stream()
        .findFirst().orElseThrow(() -> new ResourceNotFoundException("Trainee", "traineeId"));
    var timeline = lifecycle.getTraineeCareerTimeline(traineeId);
    var periods = timeline.employmentRecords().stream()
        .sorted(Comparator.comparing(EmploymentRecordResponse::startDate).thenComparing(EmploymentRecordResponse::id).reversed())
        .map(CareerProgressService::period).toList();
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    boolean canViewReport = authentication != null && authentication.getAuthorities().stream()
        .anyMatch(authority -> "survey.manage".equals(authority.getAuthority()));
    var report = canViewReport && consent.hasActiveConsent(traineeId, LatestCareerUpdate.CONSENT_CODE)
        ? LatestCareerUpdate.find(jdbc, traineeId) : null;
    var status = CareerRecordStatus.current(jdbc, traineeId, report);
    var current = new Candidate(candidate.id(), candidate.registrationNumber(), candidate.name(), status.code(), status.name());
    return new CandidateHistory(current, periods, timeline.exitEvents(), timeline.unemploymentEvents(), report);
  }

  public EmploymentRetention retention(long employmentId) {
    var record = employment(employmentId);
    scope.validateTraineeScope(record.traineeId());
    LocalDate today = LocalDate.now();
    var milestones = Stream.of(3, 6, 12).map(months -> {
      LocalDate dueOn = record.startDate().plusMonths(months);
      var status = dueOn.isAfter(today) ? RetentionStatus.NOT_YET_DUE
          : record.endDate() != null && record.endDate().isBefore(dueOn)
              ? RetentionStatus.ENDED_BEFORE_MILESTONE : RetentionStatus.RETAINED;
      return new RetentionMilestone(months, dueOn, status);
    }).toList();
    var salary = wages.getSalaryHistoryByEmploymentRecord(employmentId).stream()
        .filter(wage -> !wage.effectiveFrom().isAfter(today)
            && (record.endDate() == null || !wage.effectiveFrom().isAfter(record.endDate())))
        .map(wage -> new Wage(wage.effectiveFrom(), wage.salaryAmount(), wage.currencyCode(),
            wage.salaryFrequencyCode(), wage.recordVerificationStatusCode(), wage.observationMonthOffset(), false))
        .sorted(Comparator.comparing(Wage::date)).toList();
    var observations = new ArrayList<Wage>();
    if (!record.startDate().isAfter(today) && record.startingSalary() != null
        && salary.stream().noneMatch(wage -> wage.date().equals(record.startDate()))) {
      observations.add(new Wage(record.startDate(), record.startingSalary(), record.currencyCode(),
          record.salaryFrequencyCode(), record.recordVerificationStatusCode(), null, true));
    }
    observations.addAll(salary);
    return new EmploymentRetention(period(record), today, milestones, observations);
  }

  public ExitOptions exitOptions() {
    return new ExitOptions(references("ref_unemployment_reason", "unemployment_reason_id"),
        references("ref_employment_exit_reason", "employment_exit_reason_id"));
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public EmploymentExitAndUnemploymentResponse recordExit(long employmentId, EmploymentExitRequest input) {
    var record = lockCandidateForEmployment(employmentId);
    if (!period(record).canRecordExit()) {
      throw new ConflictException("This employment period has already ended", "EMPLOYMENT_ALREADY_ENDED");
    }
    if (input.confirmUnemployment()) {
      verifyUnemploymentCanStart(record, input.separationDate());
    }

    var type = input.exitType();
    long reason = type == ExitType.VOLUNTARY && input.exitReasonId() != null
        ? input.exitReasonId()
        : reference("ref_employment_exit_reason", "employment_exit_reason_id", "reason_code", type.defaultReason());
    long source = governmentSource();
    long pending = pendingVerification();
    var exit = new CreateEmploymentExitEventRequest(employmentId, input.separationDate(), reason,
        reference("ref_separation_nature", "separation_nature_id", "nature_code", type.nature()), source, pending);
    exit.setRemarks(input.remarks());
    var unemploymentRequest = input.confirmUnemployment()
        ? unemploymentRequest(input.unemploymentReasonId(), input.remarks()) : null;
    EmploymentExitAndUnemploymentResponse result;
    if (record.employerId() != null) {
      result = lifecycle.recordExitAndInitiateUnemployment(new EmploymentExitAndUnemploymentRequest(exit, unemploymentRequest));
    } else {
      var exitEvent = exits.createExitEvent(exit);
      result = new EmploymentExitAndUnemploymentResponse(exitEvent, unemploymentRequest == null ? null
          : startUnemployment(record, exitEvent.id(), exitEvent.separationDate(), unemploymentRequest));
      analyticsCache.evictOnEmploymentExitOrTransition(this);
    }
    var entity = employmentRecords.findById(employmentId).orElseThrow();
    entity.setEmploymentSpellStatus(spellStatuses.findByStatusCode(type.spellStatus()).orElseThrow());
    if (unemploymentRequest != null) {
      setUnemployed(record.traineeId(), unemploymentRequest.getLabourStatusId());
    }
    return result;
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public TraineeUnemploymentEventResponse confirmUnemployment(long employmentId, ReportUnemploymentRequest input) {
    var employment = lockCandidateForEmployment(employmentId);
    var exit = exits.getExitEventByEmploymentId(employmentId);
    if (employment.endDate() == null || exit.separationDate().isAfter(LocalDate.now())) {
      throw new ConflictException("The recorded employment exit has not taken effect", "EMPLOYMENT_EXIT_NOT_EFFECTIVE");
    }
    verifyUnemploymentCanStart(employment, exit.separationDate());
    var request = unemploymentRequest(input.unemploymentReasonId(), input.remarks());
    var result = startUnemployment(employment, exit.id(), exit.separationDate(), request);
    setUnemployed(employment.traineeId(), request.getLabourStatusId());
    analyticsCache.evictOnEmploymentExitOrTransition(this);
    return result;
  }

  private EmploymentRecordResponse employment(long id) {
    var record = employments.getEmploymentRecordById(id);
    if (record.deletedAt() != null) {
      throw new ResourceNotFoundException("EmploymentRecord", "employmentId");
    }
    return record;
  }

  private EmploymentRecordResponse lockCandidateForEmployment(long id) {
    var traineeId = jdbc.query("""
        SELECT trainee_id FROM employment_records WHERE employment_id = ? AND deleted_at IS NULL
        """, (rs, row) -> rs.getLong(1), id).stream().findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("EmploymentRecord", "employmentId"));
    scope.validateTraineeScope(traineeId);
    jdbc.query("""
        SELECT trainee_id FROM trainees WHERE trainee_id = ? AND deleted_at IS NULL FOR UPDATE
        """, (rs, row) -> rs.getLong(1), traineeId).stream().findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("Trainee", "traineeId"));
    jdbc.query("""
        SELECT employment_id FROM employment_records WHERE employment_id = ? AND deleted_at IS NULL FOR UPDATE
        """, (rs, row) -> rs.getLong(1), id).stream().findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("EmploymentRecord", "employmentId"));
    return employment(id);
  }

  private void verifyUnemploymentCanStart(EmploymentRecordResponse employment, LocalDate separationDate) {
    var timeline = lifecycle.getTraineeCareerTimeline(employment.traineeId());
    if (timeline.employmentRecords().stream().anyMatch(other -> !other.id().equals(employment.id())
        && (Boolean.TRUE.equals(other.isCurrent()) || other.endDate() == null || other.endDate().isAfter(separationDate)))) {
      throw new ConflictException("Another employment period overlaps or follows this exit", "OTHER_EMPLOYMENT_PERIOD");
    }
    if (timeline.unemploymentEvents().stream().anyMatch(event -> Boolean.TRUE.equals(event.isCurrent())
        || Objects.equals(event.precedingEmploymentId(), employment.id())
        || !event.startDate().isBefore(separationDate))) {
      throw new ConflictException("An unemployment period has already been recorded for or after this exit",
          "UNEMPLOYMENT_ALREADY_RECORDED");
    }
  }

  private CreateTraineeUnemploymentEventRequest unemploymentRequest(Long reason, String remarks) {
    if (reason == null) {
      throw new BadRequestException("Select the reason for unemployment", "UNEMPLOYMENT_REASON_REQUIRED");
    }
    var input = new CreateTraineeUnemploymentEventRequest();
    input.setLabourStatusId(reference("ref_employment_status", "employment_status_id", "status_code", "UNEMPLOYED"));
    input.setUnemploymentReasonId(reason);
    input.setEmploymentInfoSourceId(governmentSource());
    input.setRecordVerificationStatusId(pendingVerification());
    input.setIsCurrent(true);
    input.setRemarks(remarks);
    return input;
  }

  private TraineeUnemploymentEventResponse startUnemployment(EmploymentRecordResponse employment, long exitId,
      LocalDate separationDate, CreateTraineeUnemploymentEventRequest input) {
    input.setTraineeId(employment.traineeId());
    input.setPrecedingEmploymentId(employment.id());
    input.setEmploymentExitEventId(exitId);
    input.setEnrollmentId(employment.enrollmentId());
    input.setPlacementId(employment.placementId());
    input.setStartDate(separationDate);
    return unemployment.createUnemploymentEvent(input);
  }

  private void setUnemployed(long traineeId, long statusId) {
    jdbc.update("UPDATE trainees SET current_employment_status_id = ? WHERE trainee_id = ?", statusId, traineeId);
  }

  private long governmentSource() {
    return reference("ref_employment_info_source", "employment_info_source_id", "source_code", "GOVERNMENT_IMPORT");
  }

  private long pendingVerification() {
    return reference("ref_record_verification_status", "record_verification_status_id", "status_code", "PENDING");
  }

  private long reference(String table, String idColumn, String codeColumn, String code) {
    return jdbc.queryForObject("SELECT " + idColumn + " FROM " + table + " WHERE " + codeColumn + " = ?", Long.class, code);
  }

  private List<Reference> references(String table, String idColumn) {
    return jdbc.query("SELECT " + idColumn + ", reason_name FROM " + table + " ORDER BY sort_order",
        (rs, row) -> new Reference(rs.getLong(1), rs.getString(2)));
  }

  private static Employment period(EmploymentRecordResponse record) {
    return new Employment(record.id(), record.employmentNumber(), record.employerName(), record.jobRoleName(),
        record.startDate(), record.endDate(), record.employmentSpellStatusName(), record.recordVerificationStatusCode(),
        Boolean.TRUE.equals(record.isCurrent()) && Boolean.TRUE.equals(record.isSpellActive()) && record.endDate() == null);
  }
}
