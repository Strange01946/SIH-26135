package in.gov.sih.sih26135.service.employer;

import in.gov.sih.sih26135.service.ActiveGeography;

import in.gov.sih.sih26135.dto.employer.EmployerPortal;
import in.gov.sih.sih26135.dto.request.CreateEmploymentExitEventRequest;
import in.gov.sih.sih26135.dto.request.CreateEmploymentRecordRequest;
import in.gov.sih.sih26135.dto.request.CreateEmploymentVerificationEvidenceRequest;
import in.gov.sih.sih26135.dto.request.CreateEmploymentVerificationRequest;
import in.gov.sih.sih26135.dto.request.CreateJobPostingRequest;
import in.gov.sih.sih26135.dto.request.CreateSalaryHistoryRequest;
import in.gov.sih.sih26135.dto.request.EmploymentExitAndUnemploymentRequest;
import in.gov.sih.sih26135.dto.request.PlacementToEmploymentTransitionRequest;
import in.gov.sih.sih26135.dto.request.RecordVerificationVerdictRequest;
import in.gov.sih.sih26135.dto.request.UpdateEmploymentRecordRequest;
import in.gov.sih.sih26135.dto.request.UpdatePlacementRecordRequest;
import in.gov.sih.sih26135.dto.response.PlacementToEmploymentTransitionResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.response.PagedResponse;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import in.gov.sih.sih26135.service.CandidateLifecycleOrchestratorService;
import in.gov.sih.sih26135.service.EmploymentRecordService;
import in.gov.sih.sih26135.service.JobPostingService;
import in.gov.sih.sih26135.service.PlacementRecordService;
import jakarta.persistence.EntityManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(readOnly = true)
public class EmployerPortalService {
  private static final Set<String> CONFIRMABLE_PLACEMENTS = Set.of("OFFERED", "ACCEPTED", "JOINED");
  private static final String JOB_SELECT = """
      SELECT p.job_posting_id, p.posting_code, p.posting_title, j.job_role_name, q.level_name,
        d.district_name, p.vacancies, p.min_salary, p.max_salary, f.frequency_name, p.currency_code,
        s.status_code, p.closing_date
      FROM job_postings p JOIN job_roles j ON j.job_role_id = p.job_role_id
      JOIN districts d ON d.district_id = p.district_id
      JOIN ref_job_posting_status s ON s.job_posting_status_id = p.job_posting_status_id
      LEFT JOIN ref_qualification_level q ON q.qualification_level_id = p.qualification_level_id
      LEFT JOIN ref_salary_frequency f ON f.salary_frequency_id = p.salary_frequency_id
      WHERE p.employer_id = ? AND p.deleted_at IS NULL
      """;
  private static final String CASE_JOINS = """
      FROM employment_verification_requests r
      JOIN employment_records e ON e.employment_id = r.employment_id AND e.employer_id = r.employer_id
        AND e.trainee_id = r.trainee_id AND e.deleted_at IS NULL
      JOIN trainees t ON t.trainee_id = e.trainee_id AND t.deleted_at IS NULL
      JOIN ref_employment_verification_request_status rs
        ON rs.employment_verification_request_status_id = r.employment_verification_request_status_id
      """;
  private static final String CASE_SCOPE = CASE_JOINS + " WHERE r.employer_id = ? ";
  private static final String CASE_SELECT = """
      SELECT r.employment_verification_request_id, r.request_number, CONCAT_WS(' ', t.first_name, t.last_name),
        e.employment_number, j.job_role_name, e.start_date, e.end_date, e.starting_salary, f.frequency_name, e.currency_code,
        r.requested_at, r.due_at, rs.status_code, current_status.status_code, verdict_status.status_code,
        rs.is_open_flag AND v.employment_verification_id IS NULL, e.is_current,
        proof.employment_verification_evidence_id, proof.original_filename
      """ + CASE_JOINS + """
      JOIN ref_record_verification_status current_status ON current_status.record_verification_status_id = e.record_verification_status_id
      LEFT JOIN job_roles j ON j.job_role_id = e.job_role_id
      LEFT JOIN ref_salary_frequency f ON f.salary_frequency_id = e.salary_frequency_id
      LEFT JOIN employment_verifications v ON v.employment_verification_request_id = r.employment_verification_request_id
      LEFT JOIN ref_record_verification_status verdict_status ON verdict_status.record_verification_status_id = v.record_verification_status_id
      LEFT JOIN employment_verification_evidence proof ON proof.employment_verification_evidence_id = (
        SELECT MAX(x.employment_verification_evidence_id) FROM employment_verification_evidence x
        WHERE x.employment_verification_id = v.employment_verification_id AND x.document_reference_code LIKE 'EMP-%')
      WHERE r.employer_id = ?
      """;

  private final JdbcTemplate jdbc;
  private final WorkflowSecurityScopeValidator scope;
  private final JobPostingService jobs;
  private final PlacementRecordService placements;
  private final EmploymentRecordService employment;
  private final CandidateLifecycleOrchestratorService lifecycle;
  private final VerificationEvidenceFiles files;
  private final EntityManager entityManager;

  public EmployerPortalService(JdbcTemplate jdbc, WorkflowSecurityScopeValidator scope, JobPostingService jobs,
      PlacementRecordService placements, EmploymentRecordService employment,
      CandidateLifecycleOrchestratorService lifecycle, VerificationEvidenceFiles files, EntityManager entityManager) {
    this.jdbc = jdbc;
    this.scope = scope;
    this.jobs = jobs;
    this.placements = placements;
    this.employment = employment;
    this.lifecycle = lifecycle;
    this.files = files;
    this.entityManager = entityManager;
  }

  public EmployerPortal.References references() {
    Employer employer = employer();
    return new EmployerPortal.References(employer.name(),
        references("""
            SELECT j.job_role_id, j.job_role_name FROM job_roles j
            JOIN ref_lifecycle_status s ON s.lifecycle_status_id = j.lifecycle_status_id
            WHERE j.deleted_at IS NULL AND s.status_code = 'ACTIVE' ORDER BY j.job_role_name, j.job_role_id
            """),
        references("SELECT qualification_level_id, level_name FROM ref_qualification_level ORDER BY sort_order, level_name"),
        references("SELECT d.district_id, CONCAT(d.district_name, ' · ', s.state_name) " + ActiveGeography.DISTRICTS
            + " ORDER BY s.state_name, d.district_name"),
        references("SELECT engagement_type_id, type_name FROM ref_engagement_type WHERE requires_employer = 1 ORDER BY sort_order"),
        references("SELECT salary_frequency_id, frequency_name FROM ref_salary_frequency ORDER BY sort_order"),
        references("SELECT skill_level_id, level_name FROM skill_levels ORDER BY sort_order"),
        references("""
            SELECT employment_verification_rejection_reason_id, reason_name
            FROM ref_employment_verification_rejection_reason ORDER BY sort_order
            """),
        references("SELECT employment_exit_reason_id, reason_name FROM ref_employment_exit_reason ORDER BY sort_order"),
        references("SELECT separation_nature_id, nature_name FROM ref_separation_nature ORDER BY sort_order"));
  }

  public List<EmployerPortal.Reference> searchSkills(String search) {
    employer();
    String filter = search.strip().replace("!", "!!").replace("%", "!%").replace("_", "!_");
    return references("""
        SELECT sk.skill_id, sk.skill_name FROM skills sk
        JOIN ref_lifecycle_status s ON s.lifecycle_status_id = sk.lifecycle_status_id
        WHERE sk.deleted_at IS NULL AND s.status_code = 'ACTIVE'
          AND (sk.skill_name LIKE ? ESCAPE '!' OR sk.skill_code LIKE ? ESCAPE '!')
        ORDER BY sk.skill_name, sk.skill_id LIMIT 30
        """, "%" + filter + "%", "%" + filter + "%");
  }

  public PagedResponse<EmployerPortal.Vacancy> vacancies(int page, int size) {
    long employerId = employer().id();
    return paged(JOB_SELECT, "SELECT COUNT(*) FROM job_postings WHERE employer_id = ? AND deleted_at IS NULL",
        "p.job_posting_id DESC", page, size, this::vacancy, employerId);
  }

  @Transactional
  public EmployerPortal.Vacancy publish(EmployerPortal.VacancyRequest input) {
    Employer employer = employer();
    activeJobRole(input.jobRoleId());
    long stateId = requiredReference("SELECT d.state_id " + ActiveGeography.DISTRICTS + " AND d.district_id = ?",
        input.districtId(), "Work district");
    requiredReference("SELECT engagement_type_id FROM ref_engagement_type WHERE requires_employer = 1 AND engagement_type_id = ?",
        input.engagementTypeId(), "Engagement type");
    for (var skill : input.skills()) {
      requiredReference("""
          SELECT sk.skill_id FROM skills sk JOIN ref_lifecycle_status s ON s.lifecycle_status_id = sk.lifecycle_status_id
          WHERE sk.skill_id = ? AND sk.deleted_at IS NULL AND s.status_code = 'ACTIVE'
          """, skill.skillId(), "Skill");
      requiredReference("SELECT skill_level_id FROM skill_levels WHERE skill_level_id = ?", skill.levelId(), "Skill level");
    }
    if (new HashSet<>(input.skills().stream().map(EmployerPortal.SkillTag::skillId).toList()).size() != input.skills().size()) {
      throw new BadRequestException("Each skill may be added once.", "DUPLICATE_JOB_SKILL");
    }

    var request = new CreateJobPostingRequest(number("JOB"), input.title().strip(), input.jobRoleId(),
        input.engagementTypeId(), stateId, input.districtId(), LocalDate.now(),
        lookup("SELECT job_posting_status_id FROM ref_job_posting_status WHERE status_code = 'OPEN'"));
    request.setEmployerId(employer.id());
    request.setCreatedByUserId(employer.userId());
    request.setDescription(input.description());
    request.setQualificationLevelId(input.qualificationId());
    request.setVacancies(input.vacancies());
    request.setMinSalary(input.minimumPay());
    request.setMaxSalary(input.maximumPay());
    request.setSalaryFrequencyId(input.payFrequencyId());
    request.setCurrencyCode("INR");
    request.setClosingDate(input.closingDate());
    var job = jobs.createJobPosting(request);
    long mandatory = lookup("SELECT skill_importance_id FROM ref_skill_importance WHERE importance_code = 'MANDATORY'");
    jdbc.batchUpdate("""
        INSERT INTO job_posting_skills (job_posting_id, skill_id, required_skill_level_id, skill_importance_id)
        VALUES (?, ?, ?, ?)
        """, input.skills().stream().map(skill -> new Object[] {job.id(), skill.skillId(), skill.levelId(), mandatory}).toList());
    return jdbc.queryForObject(JOB_SELECT + " AND p.job_posting_id = ?", this::vacancy, employer.id(), job.id());
  }

  public PagedResponse<EmployerPortal.Placement> placements(int page, int size) {
    long employerId = employer().id();
    String joins = """
        FROM placement_records p JOIN trainees t ON t.trainee_id = p.trainee_id AND t.deleted_at IS NULL
        """;
    String filter = " WHERE p.employer_id = ? AND p.deleted_at IS NULL ";
    String sql = """
        SELECT p.placement_id, p.placement_number, CONCAT_WS(' ', t.first_name, t.last_name),
          c.course_name, p.job_role_id, j.job_role_name, ps.status_code, js.status_code, p.offer_date, p.actual_joining_date,
          p.offered_salary, p.salary_frequency_id, f.frequency_name, p.currency_code, e.employment_id
        """ + joins + """
        LEFT JOIN courses c ON c.course_id = p.course_id
        LEFT JOIN job_roles j ON j.job_role_id = p.job_role_id
        JOIN ref_placement_status ps ON ps.placement_status_id = p.placement_status_id
        JOIN ref_joining_status js ON js.joining_status_id = p.joining_status_id
        LEFT JOIN ref_salary_frequency f ON f.salary_frequency_id = p.salary_frequency_id
        LEFT JOIN employment_records e ON e.placement_id = p.placement_id AND e.employer_id = p.employer_id
        """ + filter;
    return paged(sql, "SELECT COUNT(*) " + joins + filter, "p.placement_id DESC", page, size, (rs, row) -> {
      String status = rs.getString(7);
      String joining = rs.getString(8);
      Long employmentId = rs.getObject(15, Long.class);
      return new EmployerPortal.Placement(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
          rs.getObject(5, Long.class), rs.getString(6), status, joining, rs.getObject(9, LocalDate.class),
          rs.getObject(10, LocalDate.class), rs.getBigDecimal(11), rs.getObject(12, Long.class), rs.getString(13),
          rs.getString(14), employmentId, employmentId == null && confirmable(status, joining));
    }, employerId);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public PlacementToEmploymentTransitionResponse confirm(long placementId, EmployerPortal.ConfirmPlacementRequest input) {
    Employer employer = employer();
    if (jdbc.queryForList("""
        SELECT placement_id FROM placement_records
        WHERE placement_id = ? AND employer_id = ? AND deleted_at IS NULL FOR UPDATE
        """, Long.class, placementId, employer.id()).isEmpty()) {
      throw new ResourceNotFoundException("Placement for this employer was not found");
    }
    var placement = placements.getPlacementRecordById(placementId);
    if (!confirmable(placement.placementStatusCode(), placement.joiningStatusCode())) {
      throw new ConflictException("This placement is not eligible for joining confirmation.", "PLACEMENT_NOT_CONFIRMABLE");
    }
    activeJobRole(input.jobRoleId());
    var update = new UpdatePlacementRecordRequest();
    update.setJobRoleId(input.jobRoleId());
    update.setActualJoiningDate(input.joiningDate());
    update.setOfferedSalary(input.offeredPay());
    update.setJoiningSalary(input.offeredPay());
    update.setSalaryFrequencyId(input.payFrequencyId());
    placements.updatePlacementRecord(placementId, update);

    long pending = lookup("SELECT record_verification_status_id FROM ref_record_verification_status WHERE status_code = 'PENDING'");
    long source = lookup("SELECT employment_info_source_id FROM ref_employment_info_source WHERE source_code = 'PLACEMENT_CONVERSION'");
    long active = lookup("SELECT employment_spell_status_id FROM ref_employment_spell_status WHERE status_code = 'ACTIVE'");
    var spell = new CreateEmploymentRecordRequest(number("EMP"), placement.traineeId(), placement.engagementTypeId(),
        active, input.joiningDate(), source, pending);
    spell.setIsCurrent(true);
    var baseline = new CreateSalaryHistoryRequest();
    baseline.setEmploymentInfoSourceId(source);
    baseline.setRecordVerificationStatusId(pending);
    baseline.setObservationMonthOffset(0);
    return lifecycle.orchestratePlacementToEmployment(new PlacementToEmploymentTransitionRequest(
        placementId, spell, baseline,
        lookup("SELECT joining_status_id FROM ref_joining_status WHERE status_code = 'JOINED'"),
        lookup("SELECT placement_status_id FROM ref_placement_status WHERE status_code = 'JOINED'")));
  }

  public PagedResponse<EmployerPortal.Verification> verifications(boolean pending, int page, int size) {
    long employerId = employer().id();
    String filter = pending ? " AND rs.is_open_flag = 1" : "";
    return paged(CASE_SELECT + filter, "SELECT COUNT(*) " + CASE_SCOPE + filter,
        pending ? "r.due_at IS NULL, r.due_at, r.employment_verification_request_id" : "r.employment_verification_request_id DESC",
        page, size, EmployerPortalService::verification, employerId);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public EmployerPortal.Verification submitVerdict(long requestId, EmployerPortal.VerdictRequest input, MultipartFile evidence) {
    Employer employer = employer();
    var target = jdbc.query("""
        SELECT e.employment_id, rs.is_open_flag, e.start_date, e.is_current
        """ + CASE_SCOPE + " AND r.employment_verification_request_id = ? FOR UPDATE OF r, e",
        (rs, row) -> new VerificationTarget(rs.getLong(1), rs.getBoolean(2), rs.getObject(3, LocalDate.class), rs.getBoolean(4)),
        employer.id(), requestId).stream().findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("Verification request for this employer was not found"));
    if (!target.open()) {
      throw new ConflictException("This verification request has already been closed.", "VERIFICATION_REQUEST_CLOSED");
    }
    validateDecision(input, target);
    boolean dispute = input.decision() == EmployerPortal.Decision.DISPUTE;
    long resultStatus = requiredReference("SELECT record_verification_status_id FROM ref_record_verification_status WHERE status_code = ?",
        dispute ? "REJECTED" : "VERIFIED", "Verification result");
    long source = lookup("SELECT employment_info_source_id FROM ref_employment_info_source WHERE source_code = 'EMPLOYER_CONFIRMATION'");
    var verdict = new CreateEmploymentVerificationRequest(number("VER"), target.employmentId(), resultStatus,
        lookup("SELECT employment_verification_method_id FROM ref_employment_verification_method WHERE method_code = 'EMPLOYER_PORTAL'"), null);
    verdict.setEmploymentVerificationRequestId(requestId);
    verdict.setEmploymentInfoSourceId(source);
    verdict.setEmployerRespondentUserId(employer.userId());
    verdict.setEmployerConfirmedFlag(!dispute);
    verdict.setVerifiedAt(LocalDateTime.now());
    if (dispute) {
      verdict.setEmploymentVerificationRejectionReasonId(input.rejectionReasonId());
      verdict.setRejectionNotes(input.notes());
    } else {
      verdict.setNotes(input.notes());
    }

    var proof = evidence == null ? List.<CreateEmploymentVerificationEvidenceRequest>of()
        : List.of(evidenceRequest(evidence, employer.userId()));
    lifecycle.orchestrateEmploymentVerificationVerdict(new RecordVerificationVerdictRequest(verdict, proof,
        lookup("SELECT employment_verification_request_status_id FROM ref_employment_verification_request_status WHERE status_code = 'COMPLETED'"),
        resultStatus));
    if (input.decision() == EmployerPortal.Decision.REPORT_EXIT) {
      recordExit(target.employmentId(), input, employer.userId(), source, resultStatus);
    }
    entityManager.flush();
    return jdbc.queryForObject(CASE_SELECT + " AND r.employment_verification_request_id = ?",
        EmployerPortalService::verification, employer.id(), requestId);
  }

  public String evidenceReference(long requestId, long evidenceId) {
    long employerId = employer().id();
    return jdbc.query("""
        SELECT x.document_reference_code
        FROM employment_verification_evidence x
        JOIN employment_verifications v ON v.employment_verification_id = x.employment_verification_id
        JOIN employment_verification_requests r ON r.employment_verification_request_id = v.employment_verification_request_id
        JOIN employment_records e ON e.employment_id = r.employment_id AND e.employer_id = r.employer_id AND e.deleted_at IS NULL
        WHERE r.employer_id = ? AND r.employment_verification_request_id = ?
          AND x.employment_verification_evidence_id = ?
        """, (rs, row) -> rs.getString(1), employerId, requestId, evidenceId).stream().findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("Verification evidence was not found"));
  }

  private void validateDecision(EmployerPortal.VerdictRequest input, VerificationTarget target) {
    if (input.decision() == EmployerPortal.Decision.DISPUTE && input.rejectionReasonId() == null) {
      throw new BadRequestException("Select a reason for disputing this employment claim.", "REJECTION_REASON_REQUIRED");
    }
    if (input.decision() == EmployerPortal.Decision.REPORT_EXIT
        && (!target.current() || input.exitDate() == null || input.exitDate().isBefore(target.startDate())
            || input.exitReasonId() == null || input.separationNatureId() == null)) {
      throw new BadRequestException("An active employment and a valid separation date, reason and type are required.", "INVALID_EMPLOYMENT_EXIT");
    }
  }

  private void recordExit(long employmentId, EmployerPortal.VerdictRequest input, long actor, long source, long verified) {
    var exit = new CreateEmploymentExitEventRequest(employmentId, input.exitDate(), input.exitReasonId(), input.separationNatureId(), source, verified);
    exit.setRemarks(input.notes());
    exit.setVerifiedAt(LocalDateTime.now());
    exit.setVerifiedByUserId(actor);
    var result = lifecycle.orchestrateEmploymentExitAndUnemployment(new EmploymentExitAndUnemploymentRequest(exit, null));
    String status = switch (result.exitEvent().separationNatureCode()) {
      case "CONTRACT_END" -> "COMPLETED";
      case "INVOLUNTARY" -> "TERMINATED";
      default -> "ENDED";
    };
    var update = new UpdateEmploymentRecordRequest();
    update.setEmploymentSpellStatusId(requiredReference(
        "SELECT employment_spell_status_id FROM ref_employment_spell_status WHERE status_code = ?", status, "Employment status"));
    employment.updateEmploymentRecord(employmentId, update);
  }

  private CreateEmploymentVerificationEvidenceRequest evidenceRequest(MultipartFile evidence, long userId) {
    var stored = files.save(evidence);
    var request = new CreateEmploymentVerificationEvidenceRequest(null,
        lookup("SELECT employment_verification_evidence_type_id FROM ref_employment_verification_evidence_type WHERE type_code = 'PAYSLIP'"),
        stored.reference(), LocalDateTime.now());
    request.setContentSha256(stored.sha256());
    request.setOriginalFilename(stored.filename());
    request.setUploadedByUserId(userId);
    return request;
  }

  private Employer employer() {
    var user = scope.resolveAuthenticatedUser().orElseThrow(() -> new AccessDeniedException("Employer account is unavailable"));
    if (user.getEmployerId() == null || user.getDeletedAt() != null) {
      throw new AccessDeniedException("Your account needs an approved employer profile.");
    }
    scope.validateEmployerScope(user.getEmployerId());
    return jdbc.query("""
        SELECT e.employer_name FROM employers e
        JOIN ref_lifecycle_status s ON s.lifecycle_status_id = e.lifecycle_status_id
        WHERE e.employer_id = ? AND e.deleted_at IS NULL AND s.status_code = 'ACTIVE'
        """, (rs, row) -> new Employer(user.getEmployerId(), user.getId(), rs.getString(1)), user.getEmployerId())
        .stream().findFirst().orElseThrow(() -> new AccessDeniedException("Your employer profile is inactive."));
  }

  private EmployerPortal.Vacancy vacancy(ResultSet rs, int row) throws SQLException {
    long id = rs.getLong(1);
    var skills = jdbc.query("""
        SELECT sk.skill_id, sk.skill_name, l.level_name FROM job_posting_skills x
        JOIN skills sk ON sk.skill_id = x.skill_id JOIN skill_levels l ON l.skill_level_id = x.required_skill_level_id
        WHERE x.job_posting_id = ? ORDER BY sk.skill_name
        """, (result, index) -> new EmployerPortal.VacancySkill(result.getLong(1), result.getString(2), result.getString(3)), id);
    return new EmployerPortal.Vacancy(id, rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5),
        rs.getString(6), rs.getInt(7), rs.getBigDecimal(8), rs.getBigDecimal(9), rs.getString(10),
        rs.getString(11), rs.getString(12), rs.getObject(13, LocalDate.class), skills);
  }

  private static EmployerPortal.Verification verification(ResultSet rs, int row) throws SQLException {
    return new EmployerPortal.Verification(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5),
        rs.getObject(6, LocalDate.class), rs.getObject(7, LocalDate.class), rs.getBigDecimal(8), rs.getString(9), rs.getString(10),
        rs.getObject(11, LocalDateTime.class), rs.getObject(12, LocalDateTime.class), rs.getString(13), rs.getString(14),
        rs.getString(15), rs.getBoolean(16), rs.getBoolean(17), rs.getObject(18, Long.class), rs.getString(19));
  }

  private void activeJobRole(long id) {
    requiredReference("""
        SELECT j.job_role_id FROM job_roles j JOIN ref_lifecycle_status s ON s.lifecycle_status_id = j.lifecycle_status_id
        WHERE j.job_role_id = ? AND j.deleted_at IS NULL AND s.status_code = 'ACTIVE'
        """, id, "Job role");
  }

  private <T> PagedResponse<T> paged(String select, String count, String order, int page, int size, RowMapper<T> mapper, long employerId) {
    var pageable = PageRequest.of(page, size);
    var rows = jdbc.query(select + " ORDER BY " + order + " LIMIT ? OFFSET ?", mapper, employerId, size, pageable.getOffset());
    return PagedResponse.of(new PageImpl<>(rows, pageable, jdbc.queryForObject(count, Long.class, employerId)));
  }

  private List<EmployerPortal.Reference> references(String sql, Object... args) {
    return jdbc.query(sql, (rs, row) -> new EmployerPortal.Reference(rs.getLong(1), rs.getString(2)), args);
  }

  private long lookup(String sql) {
    return jdbc.queryForObject(sql, Long.class);
  }

  private long requiredReference(String sql, Object value, String label) {
    return jdbc.queryForList(sql, Long.class, value).stream().findFirst()
        .orElseThrow(() -> new BadRequestException(label + " is unavailable.", "INVALID_REFERENCE"));
  }

  private static boolean confirmable(String status, String joining) {
    return CONFIRMABLE_PLACEMENTS.contains(status) && !"NO_SHOW".equals(joining);
  }

  private static String number(String prefix) {
    return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 26).toUpperCase(java.util.Locale.ROOT);
  }

  private record Employer(long id, long userId, String name) {}
  private record VerificationTarget(long employmentId, boolean open, LocalDate startDate, boolean current) {}
}
