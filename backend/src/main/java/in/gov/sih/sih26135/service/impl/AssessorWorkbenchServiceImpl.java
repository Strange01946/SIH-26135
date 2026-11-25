package in.gov.sih.sih26135.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.sih.sih26135.dto.assessor.AssessorAssessmentSummaryResponse;
import in.gov.sih.sih26135.dto.assessor.AssessorCandidateRosterItemResponse;
import in.gov.sih.sih26135.dto.assessor.AssessorRosterResponse;
import in.gov.sih.sih26135.dto.assessor.CandidateScoreInput;
import in.gov.sih.sih26135.dto.assessor.SaveAssessmentScoresRequest;
import in.gov.sih.sih26135.dto.assessor.SaveAssessmentScoresResponse;
import in.gov.sih.sih26135.entity.Assessment;
import in.gov.sih.sih26135.entity.AssessmentResult;
import in.gov.sih.sih26135.entity.Certification;
import in.gov.sih.sih26135.entity.Course;
import in.gov.sih.sih26135.entity.Program;
import in.gov.sih.sih26135.entity.RefAssessmentOutcome;
import in.gov.sih.sih26135.entity.RefCertificateStatus;
import in.gov.sih.sih26135.entity.RefCertificateVerificationStatus;
import in.gov.sih.sih26135.entity.RefEnrollmentStatus;
import in.gov.sih.sih26135.entity.Organization;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.entity.TraineeAssessment;
import in.gov.sih.sih26135.entity.TrainingBatch;
import in.gov.sih.sih26135.entity.TrainingEnrollment;
import in.gov.sih.sih26135.entity.TrainingProvider;
import in.gov.sih.sih26135.entity.User;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.repository.AssessmentRepository;
import in.gov.sih.sih26135.repository.AssessmentResultRepository;
import in.gov.sih.sih26135.repository.CertificationRepository;
import in.gov.sih.sih26135.repository.CourseRepository;
import in.gov.sih.sih26135.repository.OrganizationRepository;
import in.gov.sih.sih26135.repository.ProgramRepository;
import in.gov.sih.sih26135.repository.RefAssessmentOutcomeRepository;
import in.gov.sih.sih26135.repository.RefCertificateStatusRepository;
import in.gov.sih.sih26135.repository.RefCertificateVerificationStatusRepository;
import in.gov.sih.sih26135.repository.RefEnrollmentStatusRepository;
import in.gov.sih.sih26135.repository.TraineeAssessmentRepository;
import in.gov.sih.sih26135.repository.TraineeRepository;
import in.gov.sih.sih26135.repository.TrainingBatchRepository;
import in.gov.sih.sih26135.repository.TrainingEnrollmentRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import in.gov.sih.sih26135.service.AssessorWorkbenchService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AssessorWorkbenchServiceImpl implements AssessorWorkbenchService {

  private static final Logger log = LoggerFactory.getLogger(AssessorWorkbenchServiceImpl.class);

  public static final Long LIFECYCLE_SCHEDULED_ID = 1L;
  public static final Long LIFECYCLE_FINALIZED_ID = 2L;

  public static final Long OUTCOME_PENDING_ID = 1L;
  public static final Long OUTCOME_PASS_ID = 2L;
  public static final Long OUTCOME_FAIL_ID = 3L;
  public static final Long OUTCOME_ABSENT_ID = 4L;

  public static final Long CERT_STATUS_ISSUED_ID = 1L;
  public static final Long CERT_STATUS_SUPERSEDED_ID = 2L;
  public static final Long CERT_STATUS_REVOKED_ID = 3L;
  public static final Long CERT_STATUS_EXPIRED_ID = 4L;

  public static final Long CERT_VERIF_STATUS_UNVERIFIED_ID = 1L;
  public static final Long CERT_VERIF_STATUS_PENDING_ID = 2L;
  public static final Long CERT_VERIF_STATUS_VERIFIED_ID = 3L;
  public static final Long CERT_VERIF_STATUS_REJECTED_ID = 4L;
  public static final Long ENROLLMENT_STATUS_COMPLETED_ID = 3L;

  private final AssessmentRepository assessmentRepository;
  private final TraineeAssessmentRepository traineeAssessmentRepository;
  private final AssessmentResultRepository assessmentResultRepository;
  private final CertificationRepository certificationRepository;
  private final TrainingEnrollmentRepository trainingEnrollmentRepository;
  private final TrainingBatchRepository trainingBatchRepository;
  private final CourseRepository courseRepository;
  private final ProgramRepository programRepository;
  private final TraineeRepository traineeRepository;
  private final UserRepository userRepository;
  private final RefAssessmentOutcomeRepository refAssessmentOutcomeRepository;
  private final RefCertificateStatusRepository refCertificateStatusRepository;
  private final RefCertificateVerificationStatusRepository refCertificateVerificationStatusRepository;
  private final RefEnrollmentStatusRepository refEnrollmentStatusRepository;
  private final OrganizationRepository organizationRepository;
  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;

  public AssessorWorkbenchServiceImpl(
      AssessmentRepository assessmentRepository,
      TraineeAssessmentRepository traineeAssessmentRepository,
      AssessmentResultRepository assessmentResultRepository,
      CertificationRepository certificationRepository,
      TrainingEnrollmentRepository trainingEnrollmentRepository,
      TrainingBatchRepository trainingBatchRepository,
      CourseRepository courseRepository,
      ProgramRepository programRepository,
      TraineeRepository traineeRepository,
      UserRepository userRepository,
      RefAssessmentOutcomeRepository refAssessmentOutcomeRepository,
      RefCertificateStatusRepository refCertificateStatusRepository,
      RefCertificateVerificationStatusRepository refCertificateVerificationStatusRepository,
      RefEnrollmentStatusRepository refEnrollmentStatusRepository,
      OrganizationRepository organizationRepository,
      JdbcTemplate jdbcTemplate,
      ObjectMapper objectMapper) {
    this.assessmentRepository = assessmentRepository;
    this.traineeAssessmentRepository = traineeAssessmentRepository;
    this.assessmentResultRepository = assessmentResultRepository;
    this.certificationRepository = certificationRepository;
    this.trainingEnrollmentRepository = trainingEnrollmentRepository;
    this.trainingBatchRepository = trainingBatchRepository;
    this.courseRepository = courseRepository;
    this.programRepository = programRepository;
    this.traineeRepository = traineeRepository;
    this.userRepository = userRepository;
    this.refAssessmentOutcomeRepository = refAssessmentOutcomeRepository;
    this.refCertificateStatusRepository = refCertificateStatusRepository;
    this.refCertificateVerificationStatusRepository = refCertificateVerificationStatusRepository;
    this.refEnrollmentStatusRepository = refEnrollmentStatusRepository;
    this.organizationRepository = organizationRepository;
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
  }

  @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
  public static class AssessmentComponentScoresJson {
    private BigDecimal theory;
    private BigDecimal practical;
    private BigDecimal viva;
    private String status;
    private String note;

    public AssessmentComponentScoresJson() {}

    public AssessmentComponentScoresJson(BigDecimal theory, BigDecimal practical, BigDecimal viva, String status, String note) {
      this.theory = theory;
      this.practical = practical;
      this.viva = viva;
      this.status = status;
      this.note = note;
    }

    public BigDecimal getTheory() { return theory; }
    public void setTheory(BigDecimal theory) { this.theory = theory; }
    public BigDecimal getPractical() { return practical; }
    public void setPractical(BigDecimal practical) { this.practical = practical; }
    public BigDecimal getViva() { return viva; }
    public void setViva(BigDecimal viva) { this.viva = viva; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
  }

  private AssessmentComponentScoresJson parseComponentScores(String remarks) {
    if (remarks == null || remarks.isBlank()) {
      return null;
    }
    try {
      return objectMapper.readValue(remarks, AssessmentComponentScoresJson.class);
    } catch (Exception e) {
      log.debug("Could not parse JSON remarks: {}", e.getMessage());
      return null;
    }
  }

  private boolean isScoreEqual(BigDecimal a, BigDecimal b) {
    if (a == null && b == null) return true;
    if (a == null || b == null) return false;
    return a.compareTo(b) == 0;
  }

  @Override
  public Optional<String> resolveAuthoritativeIssuingBody(Assessment assessment) {
    if (assessment == null) {
      return Optional.empty();
    }

    // 1. Check Training Provider's Parent Organization (Awarding Body / Ministry / Council)
    if (assessment.getTrainingBatch() != null && assessment.getTrainingBatch().getTrainingProvider() != null) {
      TrainingProvider provider = assessment.getTrainingBatch().getTrainingProvider();
      if (provider.getOrganizationId() != null && organizationRepository != null) {
        Optional<Organization> orgOpt = organizationRepository.findById(provider.getOrganizationId());
        if (orgOpt.isPresent()) {
          Organization org = orgOpt.get();
          if (org.getDeletedAt() == null && Long.valueOf(1L).equals(org.getLifecycleStatusId())) {
            Long typeId = org.getOrganizationTypeId();
            // 1: CENTRAL_MINISTRY, 2: STATE_DEPARTMENT, 3: AUTONOMOUS_BODY, 4: SECTOR_SKILL_COUNCIL
            if (typeId != null && (typeId == 1L || typeId == 2L || typeId == 3L || typeId == 4L)) {
              if (org.getOrganizationName() != null && !org.getOrganizationName().isBlank()) {
                return Optional.of(org.getOrganizationName().trim());
              }
            }
          }
        }
      }
    }

    // 2. Check Program Department Organization (e.g. Ministry / Department administering the program)
    if (assessment.getProgram() != null && assessment.getProgram().getId() != null) {
      try {
        List<Map<String, Object>> orgRows = jdbcTemplate.queryForList(
            "SELECT o.organization_name, o.organization_type_id, o.lifecycle_status_id " +
            "FROM programs p " +
            "JOIN departments d ON p.department_id = d.department_id " +
            "JOIN organizations o ON d.organization_id = o.organization_id " +
            "WHERE p.program_id = ? AND p.deleted_at IS NULL AND d.deleted_at IS NULL AND o.deleted_at IS NULL",
            assessment.getProgram().getId()
        );
        if (!orgRows.isEmpty()) {
          Map<String, Object> row = orgRows.get(0);
          Long typeId = ((Number) row.get("organization_type_id")).longValue();
          Long lifecycleId = ((Number) row.get("lifecycle_status_id")).longValue();
          if (lifecycleId == 1L && (typeId == 1L || typeId == 2L || typeId == 3L || typeId == 4L)) {
            String orgName = (String) row.get("organization_name");
            if (orgName != null && !orgName.isBlank()) {
              return Optional.of(orgName.trim());
            }
          }
        }
      } catch (Exception e) {
        log.debug("Error checking program department organization: {}", e.getMessage());
      }
    }

    // 3. Check registered Sector Skill Council Organization in authoritative organizations table
    if (assessment.getCourse() != null && assessment.getCourse().getSector() != null) {
      String sectorName = assessment.getCourse().getSector().getSectorName();
      if (sectorName != null && !sectorName.isBlank()) {
        try {
          List<String> matchingOrgs = jdbcTemplate.queryForList(
              "SELECT organization_name FROM organizations " +
              "WHERE organization_type_id = 4 AND lifecycle_status_id = 1 AND deleted_at IS NULL " +
              "AND (LOWER(organization_name) LIKE LOWER(?) OR LOWER(organization_code) LIKE LOWER(?)) LIMIT 1",
              String.class,
              "%" + sectorName.trim() + "%",
              "%" + sectorName.trim() + "%"
          );
          if (!matchingOrgs.isEmpty() && matchingOrgs.get(0) != null && !matchingOrgs.get(0).isBlank()) {
            return Optional.of(matchingOrgs.get(0).trim());
          }
        } catch (Exception e) {
          log.debug("Error checking sector council organization: {}", e.getMessage());
        }
      }
    }

    // NEVER invent placeholders, display name claims, or fake authority strings
    return Optional.empty();
  }

  @Override
  public boolean isAttendanceRequirementSatisfied(Course course, Long enrollmentId) {
    if (enrollmentId == null) {
      return false;
    }

    // 1. Check Course Delivery Mode
    Long deliveryModeId = null;
    if (course != null && course.getDeliveryMode() != null) {
      deliveryModeId = course.getDeliveryMode().getId();
    } else if (course != null && course.getId() != null) {
      try {
        deliveryModeId = jdbcTemplate.queryForObject(
            "SELECT delivery_mode_id FROM courses WHERE course_id = ?",
            Long.class, course.getId());
      } catch (Exception e) {
        log.debug("Could not determine delivery_mode_id for course {}: {}", course.getId(), e.getMessage());
      }
    }

    // Mode 2 = ONLINE (self-paced / virtual without mandatory physical classroom attendance)
    if (deliveryModeId != null && deliveryModeId == 2L) {
      log.info("Enrollment {}: Course delivery mode is ONLINE (2). Physical attendance requirement is not applicable.", enrollmentId);
      return true;
    }

    // 2. For OFFLINE (1), BLENDED (3), OJT (4), or unspecified: physical session attendance is mandatory!
    Long totalSessions = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM attendance_records WHERE enrollment_id = ?",
        Long.class, enrollmentId);

    // Missing attendance evidence is NOT treated as satisfied
    if (totalSessions == null || totalSessions == 0) {
      log.info("Enrollment {}: Zero attendance records found for mandatory attendance course. Requirement NOT satisfied.", enrollmentId);
      return false;
    }

    // Attendance status 1 = PRESENT, 3 = LATE, 6 = HALF_DAY (counts_as_present = 1)
    Long attendedSessions = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM attendance_records WHERE enrollment_id = ? AND attendance_status_id IN (1, 3, 6)",
        Long.class, enrollmentId);

    double rate = (attendedSessions != null ? attendedSessions.doubleValue() : 0.0) / totalSessions.doubleValue();
    if (rate < 0.70) {
      log.info("Enrollment {}: Attendance rate {}/{} ({}%) is below mandatory 70% threshold. Requirement NOT satisfied.",
          enrollmentId, attendedSessions, totalSessions, Math.round(rate * 100));
      return false;
    }

    log.info("Enrollment {}: Attendance rate {}/{} ({}%) satisfies mandatory 70% threshold.",
        enrollmentId, attendedSessions, totalSessions, Math.round(rate * 100));
    return true;
  }

  @Override
  public List<AssessorAssessmentSummaryResponse> getAssessorAssessments(
      LocalDate fromDate,
      LocalDate toDate,
      String status,
      String search) {

    User caller = resolveAuthenticatedCaller();
    boolean isAdmin = isAdministrativeCaller();

    StringBuilder sql = new StringBuilder("""
        SELECT a.assessment_id, a.assessment_code, a.assessment_name,
               a.assessment_type_id, rat.type_code as assessment_type_code, rat.type_name as assessment_type_name,
               a.course_id, c.course_code, c.course_name,
               a.batch_id, tb.batch_code,
               tc.center_id, tc.center_code, tc.center_name,
               tp.provider_id, tp.provider_code, tp.provider_name,
               a.program_id, p.program_code, p.program_name,
               a.assessment_date, a.maximum_score, a.pass_score,
               a.lifecycle_status_id,
               a.evaluator_user_id, a.evaluator_name,
               (SELECT COUNT(*) FROM training_enrollments te WHERE te.batch_id = a.batch_id AND te.deleted_at IS NULL) as total_candidates,
               (SELECT COUNT(DISTINCT ar.assessment_result_id)
                FROM assessment_results ar
                JOIN trainee_assessments ta ON ar.trainee_assessment_id = ta.trainee_assessment_id
                WHERE ta.assessment_id = a.assessment_id) as scored_candidates,
               (SELECT COUNT(DISTINCT ar.assessment_result_id)
                FROM assessment_results ar
                JOIN trainee_assessments ta ON ar.trainee_assessment_id = ta.trainee_assessment_id
                WHERE ta.assessment_id = a.assessment_id AND ar.assessment_outcome_id = 2) as passed_candidates,
               (SELECT COUNT(DISTINCT ar.assessment_result_id)
                FROM assessment_results ar
                JOIN trainee_assessments ta ON ar.trainee_assessment_id = ta.trainee_assessment_id
                WHERE ta.assessment_id = a.assessment_id AND ar.assessment_outcome_id IN (3, 4)) as failed_candidates
        FROM assessments a
        JOIN ref_assessment_type rat ON a.assessment_type_id = rat.assessment_type_id
        JOIN courses c ON a.course_id = c.course_id
        JOIN training_batches tb ON a.batch_id = tb.batch_id
        LEFT JOIN training_centers tc ON tb.center_id = tc.center_id
        LEFT JOIN training_providers tp ON tb.provider_id = tp.provider_id
        JOIN programs p ON a.program_id = p.program_id
        WHERE a.deleted_at IS NULL
        """);

    List<Object> params = new ArrayList<>();

    if (!isAdmin) {
      sql.append(" AND a.evaluator_user_id = ?");
      params.add(caller.getId());
    }

    if (fromDate != null) {
      sql.append(" AND a.assessment_date >= ?");
      params.add(fromDate);
    }
    if (toDate != null) {
      sql.append(" AND a.assessment_date <= ?");
      params.add(toDate);
    }

    if (status != null && !status.isBlank()) {
      String normStatus = status.trim().toUpperCase(Locale.ROOT);
      if ("FINALIZED".equals(normStatus) || "COMPLETED".equals(normStatus)) {
        sql.append(" AND a.lifecycle_status_id = 2");
      } else if ("SCHEDULED".equals(normStatus) || "PENDING".equals(normStatus)) {
        sql.append(" AND a.lifecycle_status_id = 1");
      }
    }

    sql.append(" ORDER BY a.assessment_date ASC, a.assessment_id ASC");

    var rows = jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
      Long id = rs.getLong("assessment_id");
      BigDecimal maxScore = rs.getBigDecimal("maximum_score");
      if (maxScore == null || maxScore.compareTo(BigDecimal.ZERO) <= 0) {
        maxScore = BigDecimal.valueOf(100.00);
      }
      BigDecimal passScore = rs.getBigDecimal("pass_score");
      if (passScore == null) {
        passScore = maxScore.multiply(BigDecimal.valueOf(0.50)).setScale(2, RoundingMode.HALF_UP);
      }

      BigDecimal theoryMax = maxScore.multiply(BigDecimal.valueOf(0.30)).setScale(2, RoundingMode.HALF_UP);
      BigDecimal practicalMax = maxScore.multiply(BigDecimal.valueOf(0.50)).setScale(2, RoundingMode.HALF_UP);
      BigDecimal vivaMax = maxScore.multiply(BigDecimal.valueOf(0.20)).setScale(2, RoundingMode.HALF_UP);

      Long lifecycleStatusId = rs.getLong("lifecycle_status_id");
      String lifecycleStatusCode = lifecycleStatusId == 2L ? "FINALIZED" : "SCHEDULED";

      return new AssessorAssessmentSummaryResponse(
          id,
          rs.getString("assessment_code"),
          rs.getString("assessment_name"),
          rs.getLong("assessment_type_id"),
          rs.getString("assessment_type_code"),
          rs.getString("assessment_type_name"),
          rs.getLong("course_id"),
          rs.getString("course_code"),
          rs.getString("course_name"),
          rs.getLong("batch_id"),
          rs.getString("batch_code"),
          rs.getObject("center_id") != null ? rs.getLong("center_id") : null,
          rs.getString("center_code"),
          rs.getString("center_name"),
          rs.getObject("provider_id") != null ? rs.getLong("provider_id") : null,
          rs.getString("provider_code"),
          rs.getString("provider_name"),
          rs.getLong("program_id"),
          rs.getString("program_code"),
          rs.getString("program_name"),
          rs.getDate("assessment_date") != null ? rs.getDate("assessment_date").toLocalDate() : null,
          maxScore,
          passScore,
          theoryMax,
          practicalMax,
          vivaMax,
          lifecycleStatusId,
          lifecycleStatusCode,
          rs.getInt("total_candidates"),
          rs.getInt("scored_candidates"),
          rs.getInt("passed_candidates"),
          rs.getInt("failed_candidates"),
          rs.getObject("evaluator_user_id") != null ? rs.getLong("evaluator_user_id") : null,
          rs.getString("evaluator_name"),
          Boolean.valueOf("FINALIZED".equalsIgnoreCase(lifecycleStatusCode) || "COMPLETED".equalsIgnoreCase(lifecycleStatusCode))
      );
    }, params.toArray());
    return new in.gov.sih.sih26135.service.KeywordSearch(search).rank(rows, row ->
        String.join(" ", row.assessmentName(), row.assessmentCode(), row.courseName(), row.courseCode(), row.batchCode()));
  }

  @Override
  public AssessorAssessmentSummaryResponse getAssessmentSummary(Long assessmentId) {
    if (assessmentId == null || assessmentId <= 0) {
      throw new BadRequestException("Assessment ID must be valid and positive");
    }
    List<AssessorAssessmentSummaryResponse> list = getAssessorAssessments(null, null, null, null);
    return list.stream()
        .filter(a -> a.id().equals(assessmentId))
        .findFirst()
        .orElseGet(() -> {
          // If not in assigned list, check if assessment exists at all
          Assessment asm = assessmentRepository.findById(assessmentId)
              .orElseThrow(() -> new ResourceNotFoundException("Assessment", "id=" + assessmentId));
          User caller = resolveAuthenticatedCaller();
          if (!isAdministrativeCaller() && (asm.getEvaluatorUserId() == null || !asm.getEvaluatorUserId().equals(caller.getId()))) {
            throw new AccessDeniedException("Assessment does not belong to authenticated assessor scope");
          }
          // Admin viewing unassigned or different evaluator assessment
          return mapToSummary(asm);
        });
  }

  @Override
  @Transactional
  public AssessorRosterResponse getAssessmentRoster(Long assessmentId) {
    if (assessmentId == null || assessmentId <= 0) {
      throw new BadRequestException("Assessment ID must be valid and positive");
    }

    Assessment assessment = assessmentRepository.findById(assessmentId)
        .orElseThrow(() -> new ResourceNotFoundException("Assessment", "id=" + assessmentId));

    User caller = resolveAuthenticatedCaller();
    if (!isAdministrativeCaller() && (assessment.getEvaluatorUserId() == null || !assessment.getEvaluatorUserId().equals(caller.getId()))) {
      throw new AccessDeniedException("Assessment does not belong to authenticated assessor scope");
    }

    AssessorAssessmentSummaryResponse summary = mapToSummary(assessment);
    Long batchId = assessment.getTrainingBatch() != null ? assessment.getTrainingBatch().getId() : null;
    if (batchId == null) {
      return new AssessorRosterResponse(summary, Collections.emptyList());
    }

    // 1. Fetch all candidate enrollments in this batch
    String rosterSql = """
        SELECT te.enrollment_id, te.enrollment_number, res.status_code as enrollment_status_code,
               t.trainee_id, t.registration_number,
               CONCAT(t.first_name, ' ', COALESCE(t.last_name, '')) as full_name,
               rg.gender_name, t.email, t.phone
        FROM training_enrollments te
        JOIN trainees t ON te.trainee_id = t.trainee_id
        LEFT JOIN ref_gender rg ON t.gender_id = rg.gender_id
        LEFT JOIN ref_enrollment_status res ON te.enrollment_status_id = res.enrollment_status_id
        WHERE te.batch_id = ? AND te.deleted_at IS NULL AND t.deleted_at IS NULL
          AND (te.enrollment_status_id IS NULL OR te.enrollment_status_id NOT IN (4, 7))
        ORDER BY t.first_name, t.last_name, t.trainee_id
        """;

    List<Map<String, Object>> candidateRows = jdbcTemplate.queryForList(rosterSql, batchId);
    List<AssessorCandidateRosterItemResponse> candidates = new ArrayList<>();

    for (Map<String, Object> row : candidateRows) {
      Long traineeId = ((Number) row.get("trainee_id")).longValue();
      Long enrollmentId = ((Number) row.get("enrollment_id")).longValue();
      String regNumber = (String) row.get("registration_number");
      String fullName = ((String) row.get("full_name")).trim();
      String gender = (String) row.get("gender_name");
      String email = (String) row.get("email");
      String phone = (String) row.get("phone");
      String enrollmentNumber = (String) row.get("enrollment_number");
      String enrollmentStatusCode = (String) row.get("enrollment_status_code");

      // 2. Find or provision TraineeAssessment row
      TraineeAssessment ta = traineeAssessmentRepository
          .findByAssessmentIdAndTraineeId(assessmentId, traineeId)
          .orElseGet(() -> {
            Trainee t = traineeRepository.getReferenceById(traineeId);
            TrainingEnrollment te = trainingEnrollmentRepository.getReferenceById(enrollmentId);
            TraineeAssessment newTa = new TraineeAssessment(
                assessment,
                te,
                t,
                1,
                true,
                assessment.getAssessmentDate()
            );
            newTa.setEvaluatorUserId(assessment.getEvaluatorUserId());
            newTa.setEvaluatorName(assessment.getEvaluatorName());
            LocalDateTime now = LocalDateTime.now();
            newTa.setCreatedAt(now);
            newTa.setUpdatedAt(now);
            return traineeAssessmentRepository.save(newTa);
          });

      // 3. Find any existing AssessmentResult
      Optional<AssessmentResult> resultOpt = assessmentResultRepository.findByTraineeAssessmentId(ta.getId());

      Long resultId = null;
      BigDecimal theory = null;
      BigDecimal practical = null;
      BigDecimal viva = null;
      BigDecimal total = null;
      BigDecimal percentage = null;
      Long outcomeId = null;
      String outcomeCode = "PENDING";
      String outcomeName = "Pending Evaluation";
      Boolean isPass = null;
      Boolean isFinalized = false;
      String remarks = null;
      Long certId = null;
      String certNumber = null;
      String certStatus = null;
      LocalDateTime resultDeclaredAt = null;

      if (resultOpt.isPresent()) {
        AssessmentResult res = resultOpt.get();
        resultId = res.getId();
        total = res.getObtainedScore();
        percentage = res.getScorePercentage();
        if (res.getAssessmentOutcome() != null) {
          outcomeId = res.getAssessmentOutcome().getId();
          outcomeCode = res.getAssessmentOutcome().getOutcomeCode();
          outcomeName = res.getAssessmentOutcome().getOutcomeName();
          isPass = res.getAssessmentOutcome().getIsPassFlag();
        }
        resultDeclaredAt = res.getResultDeclaredAt();
        remarks = res.getRemarks();

        // Parse breakdown from JSON in remarks if present
        AssessmentComponentScoresJson parsedScores = parseComponentScores(remarks);
        if (parsedScores != null) {
          theory = parsedScores.getTheory();
          practical = parsedScores.getPractical();
          viva = parsedScores.getViva();
          if (parsedScores.getStatus() != null) {
            isFinalized = "FINALIZED".equalsIgnoreCase(parsedScores.getStatus());
          }
        } else if (remarks != null && remarks.contains("\"theory\"")) {
          try {
            JsonNode root = objectMapper.readTree(remarks);
            if (root.has("theory") && !root.get("theory").isNull()) {
              theory = new BigDecimal(root.get("theory").asText());
            }
            if (root.has("practical") && !root.get("practical").isNull()) {
              practical = new BigDecimal(root.get("practical").asText());
            }
            if (root.has("viva") && !root.get("viva").isNull()) {
              viva = new BigDecimal(root.get("viva").asText());
            }
            if (root.has("status")) {
              isFinalized = "FINALIZED".equalsIgnoreCase(root.get("status").asText());
            }
          } catch (Exception e) {
            log.debug("Could not parse JSON remarks for result {}: {}", resultId, e.getMessage());
          }
        }

        // If remarks didn't define status, check resultDeclaredAt or outcome != PENDING
        if (!isFinalized && (resultDeclaredAt != null || (outcomeId != null && outcomeId != OUTCOME_PENDING_ID))) {
          isFinalized = true;
        }

        // Check if certificate exists for this result
        Optional<Certification> certOpt = certificationRepository.findFirstByAssessmentResultId(res.getId());
        if (certOpt.isEmpty()) {
          certOpt = certificationRepository.findFirstByTrainingEnrollmentId(enrollmentId);
        }
        if (certOpt.isPresent()) {
          Certification c = certOpt.get();
          certId = c.getId();
          certNumber = c.getCertificateNumber();
          certStatus = c.getCertificateStatus() != null ? c.getCertificateStatus().getStatusCode() : "ISSUED";
        } else if (Boolean.TRUE.equals(isPass)) {
          boolean attendanceMet = isAttendanceRequirementSatisfied(assessment.getCourse(), enrollmentId);
          if (!attendanceMet) {
            certStatus = "PENDING_ELIGIBILITY";
          } else {
            Optional<String> authIssuer = resolveAuthoritativeIssuingBody(assessment);
            if (authIssuer.isEmpty()) {
              certStatus = "PENDING_ISSUANCE";
            } else {
              certStatus = "ELIGIBLE";
            }
          }
        }
      }

      candidates.add(new AssessorCandidateRosterItemResponse(
          traineeId,
          regNumber,
          fullName,
          gender,
          email,
          phone,
          enrollmentId,
          enrollmentNumber,
          enrollmentStatusCode,
          ta.getId(),
          ta.getAttemptNumber(),
          ta.getAppearedFlag(),
          ta.getAssessmentDate(),
          resultId,
          theory,
          practical,
          viva,
          total,
          percentage,
          outcomeId,
          outcomeCode,
          outcomeName,
          isPass,
          isFinalized,
          remarks,
          certId,
          certNumber,
          certStatus,
          resultDeclaredAt
      ));
    }

    return new AssessorRosterResponse(summary, candidates);
  }

  @Override
  @Transactional
  public SaveAssessmentScoresResponse saveScores(Long assessmentId, SaveAssessmentScoresRequest request) {
    if (assessmentId == null || assessmentId <= 0) {
      throw new BadRequestException("Assessment ID must be valid and positive");
    }
    if (request == null || request.getCandidates() == null || request.getCandidates().isEmpty()) {
      throw new BadRequestException("Candidate scoring payload cannot be empty");
    }

    // Pessimistic write lock on assessment row to guarantee serial execution and prevent race conditions
    Assessment assessment = assessmentRepository.findByIdForUpdate(assessmentId)
        .orElseThrow(() -> new ResourceNotFoundException("Assessment", "id=" + assessmentId));

    User caller = resolveAuthenticatedCaller();
    if (!isAdministrativeCaller() && (assessment.getEvaluatorUserId() == null || !assessment.getEvaluatorUserId().equals(caller.getId()))) {
      throw new AccessDeniedException("Assessment does not belong to authenticated assessor scope");
    }

    Long batchId = assessment.getTrainingBatch() != null ? assessment.getTrainingBatch().getId() : null;

    // Load authoritative eligible batch roster from database
    List<TrainingEnrollment> batchEnrollments = batchId != null
        ? trainingEnrollmentRepository.findByTrainingBatchIdAndDeletedAtIsNull(batchId)
        : Collections.emptyList();

    Map<Long, TrainingEnrollment> eligibleEnrollmentsByTraineeId = batchEnrollments.stream()
        .filter(e -> e.getDeletedAt() == null && e.getTrainee() != null && e.getTrainee().getDeletedAt() == null)
        .filter(e -> e.getEnrollmentStatus() == null || (e.getEnrollmentStatus().getId() != 4L && e.getEnrollmentStatus().getId() != 7L)) // exclude DROPPED (4) & CANCELLED (7)
        .collect(Collectors.toMap(e -> e.getTrainee().getId(), e -> e, (a, b) -> a));

    // Reject finalization if eligible roster is empty
    if (!request.isDraft() && eligibleEnrollmentsByTraineeId.isEmpty()) {
      throw new BadRequestException("Cannot finalize assessment with an empty eligible roster", "EMPTY_ROSTER");
    }

    // Candidate payload validation: detect duplicates and foreign / mismatched candidates
    Set<Long> inputTraineeIds = new HashSet<>();
    Set<Long> inputEnrollmentIds = new HashSet<>();
    for (CandidateScoreInput input : request.getCandidates()) {
      if (input.getTraineeId() == null) {
        throw new BadRequestException("Trainee ID is required in candidate scoring input", "INVALID_CANDIDATE");
      }
      if (!inputTraineeIds.add(input.getTraineeId())) {
        throw new BadRequestException("Duplicate candidate ID " + input.getTraineeId() + " detected in scoring payload", "DUPLICATE_CANDIDATE");
      }
      if (input.getEnrollmentId() != null && !inputEnrollmentIds.add(input.getEnrollmentId())) {
        throw new BadRequestException("Duplicate enrollment ID " + input.getEnrollmentId() + " detected in scoring payload", "DUPLICATE_CANDIDATE");
      }
      if (!eligibleEnrollmentsByTraineeId.containsKey(input.getTraineeId())) {
        throw new BadRequestException("Candidate #" + input.getTraineeId() + " is not an eligible candidate for assessment batch #" + batchId, "INVALID_CANDIDATE");
      }
      TrainingEnrollment expectedEnrollment = eligibleEnrollmentsByTraineeId.get(input.getTraineeId());
      if (input.getEnrollmentId() != null && !input.getEnrollmentId().equals(expectedEnrollment.getId())) {
        throw new BadRequestException("Enrollment trainee mismatch for candidate #" + input.getTraineeId(), "TRAINEE_ENROLLMENT_MISMATCH");
      }
    }

    // On finalization: MUST cover the entire eligible roster
    if (!request.isDraft()) {
      for (Long eligibleTraineeId : eligibleEnrollmentsByTraineeId.keySet()) {
        if (!inputTraineeIds.contains(eligibleTraineeId)) {
          throw new BadRequestException("Assessment finalization requires scores for the complete eligible roster. Missing trainee #" + eligibleTraineeId, "INCOMPLETE_ROSTER");
        }
      }
    }

    // Check if assessment is already finalized (Immutability & Idempotency)
    boolean alreadyFinalized = assessment.getLifecycleStatusId() != null && assessment.getLifecycleStatusId().equals(LIFECYCLE_FINALIZED_ID);
    if (alreadyFinalized) {
      if (request.isDraft()) {
        throw new BadRequestException("Cannot save draft on an already finalized assessment", "ASSESSMENT_ALREADY_FINALIZED");
      }
      // Check if re-finalization payload is identical
      boolean isIdentical = true;
      if (request.getCandidates().size() != eligibleEnrollmentsByTraineeId.size()) {
        isIdentical = false;
      } else {
        for (CandidateScoreInput input : request.getCandidates()) {
          Long tId = input.getTraineeId();
          Optional<TraineeAssessment> existingTaOpt = traineeAssessmentRepository.findByAssessmentIdAndTraineeId(assessmentId, tId);
          if (existingTaOpt.isEmpty()) {
            isIdentical = false;
            break;
          }
          TraineeAssessment existingTa = existingTaOpt.get();

          // Enrollment identity match
          if (existingTa.getTrainingEnrollment() == null || input.getEnrollmentId() == null
              || !existingTa.getTrainingEnrollment().getId().equals(input.getEnrollmentId())) {
            isIdentical = false;
            break;
          }

          boolean expectedAppeared = input.getAppeared() != null ? input.getAppeared() : true;
          if (Boolean.TRUE.equals(existingTa.getAppearedFlag()) != expectedAppeared) {
            isIdentical = false;
            break;
          }
          Optional<AssessmentResult> existingResOpt = assessmentResultRepository.findByTraineeAssessmentId(existingTa.getId());
          if (existingResOpt.isEmpty()) {
            isIdentical = false;
            break;
          }
          AssessmentResult existingRes = existingResOpt.get();
          if (expectedAppeared) {
            BigDecimal inTheory = input.getTheoryScore();
            BigDecimal inPractical = input.getPracticalScore();
            BigDecimal inViva = input.getVivaScore();
            BigDecimal inTotal = (inTheory != null ? inTheory : BigDecimal.ZERO)
                .add(inPractical != null ? inPractical : BigDecimal.ZERO)
                .add(inViva != null ? inViva : BigDecimal.ZERO);
            if (existingRes.getObtainedScore() == null || existingRes.getObtainedScore().compareTo(inTotal) != 0) {
              isIdentical = false;
              break;
            }

            // Exact component marks comparison
            AssessmentComponentScoresJson existingScores = parseComponentScores(existingRes.getRemarks());
            if (existingScores == null) {
              isIdentical = false;
              break;
            }

            if (!isScoreEqual(inTheory, existingScores.getTheory())
                || !isScoreEqual(inPractical, existingScores.getPractical())
                || !isScoreEqual(inViva, existingScores.getViva())) {
              isIdentical = false;
              break;
            }
          } else {
            if (existingRes.getObtainedScore() != null && existingRes.getObtainedScore().compareTo(BigDecimal.ZERO) != 0) {
              isIdentical = false;
              break;
            }
          }
        }
      }

      if (!isIdentical) {
        throw new BadRequestException("Assessment is already finalized with different marks or outcomes and cannot be modified", "ASSESSMENT_ALREADY_FINALIZED");
      }

      // Check if any passed candidates who previously had pending requirements are NOW eligible!
      int newlyIssued = issuePendingCertificatesInternal(assessment);
      if (newlyIssued > 0) {
        log.info("Idempotent finalize re-evaluation issued {} pending certificates for assessment {}.", newlyIssued, assessmentId);
      }

      // Idempotent return without database mutations to finalized marks
      log.info("Assessment {} is already finalized and identical payload received. Returning idempotent read-only response.", assessmentId);
      AssessorRosterResponse currentRoster = getAssessmentRoster(assessmentId);
      int passed = 0;
      int failed = 0;
      int absent = 0;
      int certs = 0;
      for (AssessorCandidateRosterItemResponse c : currentRoster.candidates()) {
        if (Boolean.TRUE.equals(c.isPassFlag())) passed++;
        else if ("ABSENT".equalsIgnoreCase(c.assessmentOutcomeCode())) absent++;
        else failed++;
        if (c.certificateId() != null) certs++;
      }
      return new SaveAssessmentScoresResponse(
          assessmentId,
          true,
          currentRoster.candidates().size(),
          passed,
          failed,
          absent,
          certs,
          currentRoster.candidates()
      );
    }

    BigDecimal maxTotal = assessment.getMaximumScore() != null ? assessment.getMaximumScore() : BigDecimal.valueOf(100.00);
    BigDecimal theoryMax = maxTotal.multiply(BigDecimal.valueOf(0.30)).setScale(2, RoundingMode.HALF_UP);
    BigDecimal practicalMax = maxTotal.multiply(BigDecimal.valueOf(0.50)).setScale(2, RoundingMode.HALF_UP);
    BigDecimal vivaMax = maxTotal.multiply(BigDecimal.valueOf(0.20)).setScale(2, RoundingMode.HALF_UP);
    BigDecimal passScore = assessment.getPassScore() != null
        ? assessment.getPassScore()
        : maxTotal.multiply(BigDecimal.valueOf(0.50)).setScale(2, RoundingMode.HALF_UP);

    int totalProcessed = 0;
    int passedCount = 0;
    int failedCount = 0;
    int absentCount = 0;
    int certsIssued = 0;

    LocalDateTime now = LocalDateTime.now();
    LocalDate today = LocalDate.now();

    RefAssessmentOutcome outcomePending = refAssessmentOutcomeRepository.findById(OUTCOME_PENDING_ID)
        .orElseThrow(() -> new ResourceNotFoundException("RefAssessmentOutcome", "id=" + OUTCOME_PENDING_ID));
    RefAssessmentOutcome outcomePass = refAssessmentOutcomeRepository.findById(OUTCOME_PASS_ID)
        .orElseThrow(() -> new ResourceNotFoundException("RefAssessmentOutcome", "id=" + OUTCOME_PASS_ID));
    RefAssessmentOutcome outcomeFail = refAssessmentOutcomeRepository.findById(OUTCOME_FAIL_ID)
        .orElseThrow(() -> new ResourceNotFoundException("RefAssessmentOutcome", "id=" + OUTCOME_FAIL_ID));
    RefAssessmentOutcome outcomeAbsent = refAssessmentOutcomeRepository.findById(OUTCOME_ABSENT_ID)
        .orElseThrow(() -> new ResourceNotFoundException("RefAssessmentOutcome", "id=" + OUTCOME_ABSENT_ID));

    RefCertificateStatus statusIssued = refCertificateStatusRepository.findById(CERT_STATUS_ISSUED_ID)
        .orElseThrow(() -> new ResourceNotFoundException("RefCertificateStatus", "id=" + CERT_STATUS_ISSUED_ID));
    RefCertificateVerificationStatus verifStatusPending = refCertificateVerificationStatusRepository.findById(CERT_VERIF_STATUS_PENDING_ID)
        .orElseThrow(() -> new ResourceNotFoundException("RefCertificateVerificationStatus", "id=" + CERT_VERIF_STATUS_PENDING_ID));
    RefEnrollmentStatus statusCompleted = refEnrollmentStatusRepository.findById(ENROLLMENT_STATUS_COMPLETED_ID).orElse(null);

    for (CandidateScoreInput input : request.getCandidates()) {
      Long traineeId = input.getTraineeId();
      TrainingEnrollment enrollment = eligibleEnrollmentsByTraineeId.get(traineeId);
      Trainee trainee = enrollment.getTrainee();

      // Find or create TraineeAssessment
      TraineeAssessment ta = traineeAssessmentRepository
          .findByAssessmentIdAndTraineeId(assessmentId, traineeId)
          .orElseGet(() -> {
            TraineeAssessment newTa = new TraineeAssessment(
                assessment,
                enrollment,
                trainee,
                1,
                true,
                assessment.getAssessmentDate()
            );
            newTa.setEvaluatorUserId(caller.getId());
            newTa.setEvaluatorName(caller.getUsername());
            newTa.setCreatedAt(now);
            newTa.setUpdatedAt(now);
            return traineeAssessmentRepository.save(newTa);
          });

      boolean appeared = input.getAppeared() != null ? input.getAppeared() : true;
      ta.setAppearedFlag(appeared);
      ta.setUpdatedAt(now);
      traineeAssessmentRepository.save(ta);

      BigDecimal theory = input.getTheoryScore();
      BigDecimal practical = input.getPracticalScore();
      BigDecimal viva = input.getVivaScore();

      if (appeared) {
        // Validation checks
        if (theory != null && theory.compareTo(BigDecimal.ZERO) < 0) {
          throw new BadRequestException("Theory score cannot be negative for candidate #" + traineeId, "INVALID_SCORE");
        }
        if (practical != null && practical.compareTo(BigDecimal.ZERO) < 0) {
          throw new BadRequestException("Practical score cannot be negative for candidate #" + traineeId, "INVALID_SCORE");
        }
        if (viva != null && viva.compareTo(BigDecimal.ZERO) < 0) {
          throw new BadRequestException("Viva score cannot be negative for candidate #" + traineeId, "INVALID_SCORE");
        }

        if (theory != null && theory.compareTo(theoryMax) > 0) {
          throw new BadRequestException("Theory score (" + theory + ") exceeds maximum of " + theoryMax + " for candidate #" + traineeId, "INVALID_SCORE");
        }
        if (practical != null && practical.compareTo(practicalMax) > 0) {
          throw new BadRequestException("Practical score (" + practical + ") exceeds maximum of " + practicalMax + " for candidate #" + traineeId, "INVALID_SCORE");
        }
        if (viva != null && viva.compareTo(vivaMax) > 0) {
          throw new BadRequestException("Viva score (" + viva + ") exceeds maximum of " + vivaMax + " for candidate #" + traineeId, "INVALID_SCORE");
        }

        if (!request.isDraft()) {
          if (theory == null || practical == null || viva == null) {
            throw new BadRequestException("All components (theory, practical, viva) are required to finalize candidate #" + traineeId, "MISSING_SCORES");
          }
        }
      } else {
        // Absent candidate
        theory = BigDecimal.ZERO;
        practical = BigDecimal.ZERO;
        viva = BigDecimal.ZERO;
      }

      BigDecimal total = BigDecimal.ZERO;
      if (theory != null) total = total.add(theory);
      if (practical != null) total = total.add(practical);
      if (viva != null) total = total.add(viva);

      if (total.compareTo(maxTotal) > 0) {
        throw new BadRequestException("Aggregate score (" + total + ") exceeds maximum assessment marks (" + maxTotal + ")", "INVALID_SCORE");
      }

      BigDecimal percentage = total.multiply(BigDecimal.valueOf(100)).divide(maxTotal, 2, RoundingMode.HALF_UP);

      RefAssessmentOutcome outcome;
      boolean isPass = false;
      String statusStr;

      if (request.isDraft()) {
        outcome = outcomePending;
        isPass = false;
        statusStr = "DRAFT";
      } else {
        statusStr = "FINALIZED";
        if (!appeared) {
          outcome = outcomeAbsent;
          isPass = false;
          absentCount++;
        } else if (total.compareTo(passScore) >= 0) {
          outcome = outcomePass;
          isPass = true;
          passedCount++;
        } else {
          outcome = outcomeFail;
          isPass = false;
          failedCount++;
        }
      }

      // JSON remarks for component storage using typed DTO
      AssessmentComponentScoresJson scoresJson = new AssessmentComponentScoresJson(
          theory,
          practical,
          viva,
          statusStr,
          input.getRemarks() != null ? input.getRemarks() : ""
      );
      String jsonRemarks;
      try {
        jsonRemarks = objectMapper.writeValueAsString(scoresJson);
      } catch (Exception e) {
        log.warn("Failed to serialize component scores JSON: {}", e.getMessage());
        jsonRemarks = "{}";
      }

      // Persist AssessmentResult
      AssessmentResult result = assessmentResultRepository
          .findByTraineeAssessmentId(ta.getId())
          .orElseGet(() -> {
            AssessmentResult newRes = new AssessmentResult();
            newRes.setTraineeAssessment(ta);
            newRes.setTrainee(trainee);
            newRes.setMaximumScore(maxTotal);
            newRes.setCreatedAt(now);
            return newRes;
          });

      result.setObtainedScore(total);
      result.setMaximumScore(maxTotal);
      result.setScorePercentage(percentage);
      result.setAssessmentOutcome(outcome);
      result.setRemarks(jsonRemarks);
      result.setUpdatedAt(now);
      if (!request.isDraft()) {
        result.setResultDeclaredAt(now);
      }

      AssessmentResult savedResult = assessmentResultRepository.save(result);

      // Evaluate training completion requirements
      Course course = assessment.getCourse();
      Program program = assessment.getProgram();

      boolean attendanceMet = isAttendanceRequirementSatisfied(course, enrollment.getId());
      boolean batchActive = true;
      if (assessment.getTrainingBatch() != null && assessment.getTrainingBatch().getBatchStatus() != null) {
        Long batchStatusId = assessment.getTrainingBatch().getBatchStatus().getId();
        if (batchStatusId == 5L || batchStatusId == 6L) { // CANCELLED (5) or SUSPENDED (6)
          batchActive = false;
        }
      }

      boolean trainingRequirementsCompleted = batchActive && attendanceMet;

      // 1. Training Enrollment Completion Transition (Only for passing candidates on finalized assessment)
      if (enrollment.getEnrollmentStatus() != null) {
        Long curStatusId = enrollment.getEnrollmentStatus().getId();
        if (curStatusId == 1L || curStatusId == 2L) { // Only active enrollments: ENROLLED or IN_PROGRESS
          if (!request.isDraft() && isPass && trainingRequirementsCompleted && statusCompleted != null) {
            enrollment.setEnrollmentStatus(statusCompleted);
            if (enrollment.getActualCompletionDate() == null) {
              enrollment.setActualCompletionDate(today);
            }
            enrollment.setUpdatedAt(now);
            trainingEnrollmentRepository.save(enrollment);
            log.info("Enrollment {} marked COMPLETED (batch active, attendance satisfied, assessment passed).", enrollment.getId());
          } else {
            log.info("Enrollment {} retained status {} (batchActive={}, attendanceMet={}, isPass={}).",
                enrollment.getId(), curStatusId, batchActive, attendanceMet, isPass);
          }
        } else if (curStatusId == 3L) { // Already completed: preserve historical completion date
          if (enrollment.getActualCompletionDate() == null) {
            enrollment.setActualCompletionDate(today);
            enrollment.setUpdatedAt(now);
            trainingEnrollmentRepository.save(enrollment);
          }
        }
      }

      // 2. Certification on Finalize for Passed candidates (only if not draft and PASS)
      if (!request.isDraft() && isPass) {
        // Candidate must satisfy training completion conditions
        boolean isTrainingCompleted = enrollment.getEnrollmentStatus() != null
            && enrollment.getEnrollmentStatus().getId() == 3L;

        if (!isTrainingCompleted) {
          log.info("Candidate {} passed assessment but training completion is pending (batchActive={}, attendanceMet={}). Certificate issuance deferred.",
              traineeId, batchActive, attendanceMet);
        } else {
          // Check authoritative issuing body
          Optional<String> authoritativeIssuerOpt = resolveAuthoritativeIssuingBody(assessment);
          if (authoritativeIssuerOpt.isEmpty()) {
            log.info("Candidate {} passed and completed training, but authoritative issuing body is pending accreditation. Certificate issuance deferred.",
                traineeId);
          } else {
            // Both training completed AND authoritative issuer established -> ISSUE CERTIFICATE!
            Optional<Certification> existingCert = certificationRepository.findFirstByAssessmentResultId(savedResult.getId());
            if (existingCert.isEmpty()) {
              existingCert = certificationRepository.findFirstByTrainingEnrollmentId(enrollment.getId());
            }

            if (existingCert.isEmpty()) {
              String courseCode = course != null ? course.getCourseCode() : "CRS";
              String certNumber = "CERT-" + courseCode + "-" + traineeId + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
              String issuingBody = authoritativeIssuerOpt.get();

              Certification cert = new Certification();
              cert.setCertificateNumber(certNumber);
              cert.setTrainee(trainee);
              cert.setTrainingEnrollment(enrollment);
              cert.setCourse(course);
              cert.setProgram(program);
              cert.setAssessmentResult(savedResult);
              cert.setIssuingBody(issuingBody);
              cert.setIssueDate(today);
              cert.setExpiryDate(today.plusYears(5));
              cert.setCertificateStatus(statusIssued);
              cert.setCertificateVerificationStatus(verifStatusPending);
              cert.setVerifiedAt(null);
              cert.setVerifiedByUserId(null);
              cert.setCreatedAt(now);
              cert.setUpdatedAt(now);

              certificationRepository.save(cert);
              certsIssued++;
              log.info("Certificate {} issued for trainee {} under authoritative body '{}'.", certNumber, traineeId, issuingBody);
            }
          }
        }
      }

      totalProcessed++;
    }

    // On finalization, mark assessment lifecycleStatusId = 2 (FINALIZED)
    boolean isFinalized = !request.isDraft();
    if (isFinalized) {
      assessment.setLifecycleStatusId(LIFECYCLE_FINALIZED_ID);
      assessment.setUpdatedAt(now);
      assessmentRepository.save(assessment);
      log.info("Assessment {} marked FINALIZED by assessor {}. Total candidates: {}, Passed: {}, Failed: {}, Absent: {}, Certs: {}",
          assessmentId, caller.getId(), totalProcessed, passedCount, failedCount, absentCount, certsIssued);
    }

    AssessorRosterResponse updatedRoster = getAssessmentRoster(assessmentId);

    return new SaveAssessmentScoresResponse(
        assessmentId,
        isFinalized,
        totalProcessed,
        passedCount,
        failedCount,
        absentCount,
        certsIssued,
        updatedRoster.candidates()
    );
  }

  @Override
  @Transactional
  public SaveAssessmentScoresResponse issuePendingCertificates(Long assessmentId) {
    if (assessmentId == null || assessmentId <= 0) {
      throw new BadRequestException("Assessment ID must be valid and positive");
    }
    Assessment assessment = assessmentRepository.findByIdForUpdate(assessmentId)
        .orElseThrow(() -> new ResourceNotFoundException("Assessment", "id=" + assessmentId));

    User caller = resolveAuthenticatedCaller();
    if (!isAdministrativeCaller() && (assessment.getEvaluatorUserId() == null || !assessment.getEvaluatorUserId().equals(caller.getId()))) {
      throw new AccessDeniedException("Assessment does not belong to authenticated assessor scope");
    }

    int newlyIssued = issuePendingCertificatesInternal(assessment);
    log.info("issuePendingCertificates for assessment {} issued {} new certificates.", assessmentId, newlyIssued);

    AssessorRosterResponse currentRoster = getAssessmentRoster(assessmentId);
    int passed = 0;
    int failed = 0;
    int absent = 0;
    int certs = 0;
    for (AssessorCandidateRosterItemResponse c : currentRoster.candidates()) {
      if (Boolean.TRUE.equals(c.isPassFlag())) passed++;
      else if ("ABSENT".equalsIgnoreCase(c.assessmentOutcomeCode())) absent++;
      else failed++;
      if (c.certificateId() != null) certs++;
    }
    return new SaveAssessmentScoresResponse(
        assessmentId,
        true,
        currentRoster.candidates().size(),
        passed,
        failed,
        absent,
        certs,
        currentRoster.candidates()
    );
  }

  private int issuePendingCertificatesInternal(Assessment assessment) {
    if (assessment == null) return 0;
    Long batchId = assessment.getTrainingBatch() != null ? assessment.getTrainingBatch().getId() : null;
    if (batchId == null) return 0;

    List<TrainingEnrollment> batchEnrollments = trainingEnrollmentRepository.findByTrainingBatchIdAndDeletedAtIsNull(batchId);
    LocalDateTime now = LocalDateTime.now();
    LocalDate today = LocalDate.now();
    RefCertificateStatus statusIssued = refCertificateStatusRepository.findById(CERT_STATUS_ISSUED_ID).orElse(null);
    RefCertificateVerificationStatus verifStatusPending = refCertificateVerificationStatusRepository.findById(CERT_VERIF_STATUS_PENDING_ID).orElse(null);
    RefEnrollmentStatus statusCompleted = refEnrollmentStatusRepository.findById(ENROLLMENT_STATUS_COMPLETED_ID).orElse(null);

    int newlyIssued = 0;
    Course course = assessment.getCourse();
    Program program = assessment.getProgram();

    for (TrainingEnrollment enrollment : batchEnrollments) {
      if (enrollment.getTrainee() == null) continue;
      Long traineeId = enrollment.getTrainee().getId();

      Optional<TraineeAssessment> taOpt = traineeAssessmentRepository.findByAssessmentIdAndTraineeId(assessment.getId(), traineeId);
      if (taOpt.isEmpty()) continue;
      TraineeAssessment ta = taOpt.get();

      Optional<AssessmentResult> resOpt = assessmentResultRepository.findByTraineeAssessmentId(ta.getId());
      if (resOpt.isEmpty()) continue;
      AssessmentResult res = resOpt.get();

      // Only passed candidates
      boolean isPass = res.getAssessmentOutcome() != null && Boolean.TRUE.equals(res.getAssessmentOutcome().getIsPassFlag());
      if (!isPass) continue;

      // Check if already has a certificate
      Optional<Certification> existingCert = certificationRepository.findFirstByAssessmentResultId(res.getId());
      if (existingCert.isEmpty()) {
        existingCert = certificationRepository.findFirstByTrainingEnrollmentId(enrollment.getId());
      }
      if (existingCert.isPresent()) continue;

      // Check attendance & batch
      boolean attendanceMet = isAttendanceRequirementSatisfied(course, enrollment.getId());
      boolean batchActive = true;
      if (assessment.getTrainingBatch() != null && assessment.getTrainingBatch().getBatchStatus() != null) {
        Long bStatus = assessment.getTrainingBatch().getBatchStatus().getId();
        if (bStatus == 5L || bStatus == 6L) batchActive = false;
      }

      if (!batchActive || !attendanceMet) continue;

      // Mark enrollment completed if active
      if (enrollment.getEnrollmentStatus() != null) {
        Long curStatusId = enrollment.getEnrollmentStatus().getId();
        if (curStatusId == 1L || curStatusId == 2L) {
          if (statusCompleted != null) {
            enrollment.setEnrollmentStatus(statusCompleted);
            if (enrollment.getActualCompletionDate() == null) {
              enrollment.setActualCompletionDate(today);
            }
            enrollment.setUpdatedAt(now);
            trainingEnrollmentRepository.save(enrollment);
          }
        }
      }

      // Check authoritative issuer
      Optional<String> authIssuerOpt = resolveAuthoritativeIssuingBody(assessment);
      if (authIssuerOpt.isEmpty()) continue;

      // Issue certificate!
      String courseCode = course != null ? course.getCourseCode() : "CRS";
      String certNumber = "CERT-" + courseCode + "-" + traineeId + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
      Certification cert = new Certification();
      cert.setCertificateNumber(certNumber);
      cert.setTrainee(enrollment.getTrainee());
      cert.setTrainingEnrollment(enrollment);
      cert.setCourse(course);
      cert.setProgram(program);
      cert.setAssessmentResult(res);
      cert.setIssuingBody(authIssuerOpt.get());
      cert.setIssueDate(today);
      cert.setExpiryDate(today.plusYears(5));
      cert.setCertificateStatus(statusIssued);
      cert.setCertificateVerificationStatus(verifStatusPending);
      cert.setVerifiedAt(null);
      cert.setVerifiedByUserId(null);
      cert.setCreatedAt(now);
      cert.setUpdatedAt(now);

      certificationRepository.save(cert);
      newlyIssued++;
      log.info("Pending requirement resolved: Certificate {} issued for trainee {} under '{}'.", certNumber, traineeId, authIssuerOpt.get());
    }
    return newlyIssued;
  }

  @Override
  @Transactional(readOnly = true)
  public byte[] generateCertificatePdf(Long certificationId) {
    if (certificationId == null || certificationId <= 0) {
      throw new BadRequestException("Certification ID must be valid and positive");
    }

    Certification cert = certificationRepository.findById(certificationId)
        .orElseThrow(() -> new ResourceNotFoundException("Certification", "id=" + certificationId));

    // Access authorization check
    User caller = resolveAuthenticatedCaller();
    boolean isAdmin = isAdministrativeCaller();
    if (!isAdmin) {
      Integer assignedCount = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM certifications c " +
          "JOIN assessment_results ar ON c.assessment_result_id = ar.assessment_result_id " +
          "JOIN trainee_assessments ta ON ar.trainee_assessment_id = ta.trainee_assessment_id " +
          "JOIN assessments a ON ta.assessment_id = a.assessment_id " +
          "WHERE c.certification_id = ? AND a.evaluator_user_id = ?",
          Integer.class,
          certificationId,
          caller.getId()
      );
      boolean isAssignedAssessor = assignedCount != null && assignedCount > 0;

      boolean isOwnTrainee = false;
      Optional<Trainee> callerTrainee = traineeRepository.findByUserId(caller.getId());
      if (callerTrainee.isPresent()) {
        Integer ownCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM certifications WHERE certification_id = ? AND trainee_id = ?",
            Integer.class,
            certificationId,
            callerTrainee.get().getId()
        );
        isOwnTrainee = ownCount != null && ownCount > 0;
      }

      if (!isAssignedAssessor && !isOwnTrainee) {
        throw new AccessDeniedException("Access denied: You are not authorized to view or download certificate #" + certificationId);
      }
    }

    Trainee trainee = cert.getTrainee();
    Course course = cert.getCourse();
    AssessmentResult result = cert.getAssessmentResult();

    String traineeName = trainee != null ? (trainee.getFirstName() + " " + Objects.toString(trainee.getLastName(), "")).trim() : "Candidate";
    String regNumber = trainee != null ? trainee.getRegistrationNumber() : "N/A";
    String courseName = course != null ? course.getCourseName() : "Vocational Training Program";
    String courseCode = course != null ? course.getCourseCode() : "N/A";
    String certNumber = cert.getCertificateNumber();
    String issueDate = cert.getIssueDate() != null ? cert.getIssueDate().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy")) : "N/A";
    String issuingBody = cert.getIssuingBody() != null && !cert.getIssuingBody().isBlank()
        ? cert.getIssuingBody().trim()
        : null;

    if (issuingBody == null && result != null && result.getTraineeAssessment() != null
        && result.getTraineeAssessment().getAssessment() != null) {
      issuingBody = resolveAuthoritativeIssuingBody(result.getTraineeAssessment().getAssessment()).orElse(null);
    }

    if (issuingBody == null || issuingBody.isBlank()) {
      throw new BadRequestException("Certificate #" + certificationId + " has no authoritative issuing body recorded", "UNAUTHORIZED_ISSUING_BODY");
    }

    String assessmentTitle = "Standard Competency Evaluation";
    if (result != null && result.getTraineeAssessment() != null && result.getTraineeAssessment().getAssessment() != null) {
      Assessment asm = result.getTraineeAssessment().getAssessment();
      if (asm.getAssessmentName() != null) {
        assessmentTitle = asm.getAssessmentName() + " (" + asm.getAssessmentCode() + ")";
      }
    }

    String scoreStr = "Grade: PASS";
    if (result != null && result.getScorePercentage() != null) {
      scoreStr = "Score Achieved: " + result.getScorePercentage().toPlainString() + "% (PASS)";
    }

    String certStatusText = "Credential Status: ACTIVE / ISSUED";
    if (cert.getCertificateStatus() != null) {
      Long stId = cert.getCertificateStatus().getId();
      if (Long.valueOf(CERT_STATUS_REVOKED_ID).equals(stId) || "REVOKED".equalsIgnoreCase(cert.getCertificateStatus().getStatusCode())) {
        certStatusText = "Credential Status: REVOKED - INVALID CREDENTIAL";
      } else if (Long.valueOf(CERT_STATUS_EXPIRED_ID).equals(stId) || "EXPIRED".equalsIgnoreCase(cert.getCertificateStatus().getStatusCode())) {
        certStatusText = "Credential Status: EXPIRED - INVALID CREDENTIAL";
      } else if (Long.valueOf(CERT_STATUS_SUPERSEDED_ID).equals(stId) || "SUPERSEDED".equalsIgnoreCase(cert.getCertificateStatus().getStatusCode())) {
        certStatusText = "Credential Status: SUPERSEDED - PREVIOUS VERSION";
      }
    }

    String verifStatusText = "Verification Status: PENDING VERIFICATION";
    if (cert.getCertificateVerificationStatus() != null) {
      Long vId = cert.getCertificateVerificationStatus().getId();
      if (Long.valueOf(CERT_VERIF_STATUS_VERIFIED_ID).equals(vId) || "VERIFIED".equalsIgnoreCase(cert.getCertificateVerificationStatus().getStatusCode())) {
        verifStatusText = "Verification Status: VERIFIED - OFFICIAL REGISTER";
      } else if (Long.valueOf(CERT_VERIF_STATUS_REJECTED_ID).equals(vId) || "REJECTED".equalsIgnoreCase(cert.getCertificateVerificationStatus().getStatusCode())) {
        verifStatusText = "Verification Status: REJECTED / INVALID";
      }
    }

    return buildPdfDocument(traineeName, regNumber, courseName, courseCode, assessmentTitle, certNumber, issueDate, issuingBody, scoreStr, certStatusText, verifStatusText);
  }

  private byte[] buildPdfDocument(
      String traineeName,
      String regNumber,
      String courseName,
      String courseCode,
      String assessmentTitle,
      String certNumber,
      String issueDate,
      String issuingBody,
      String scoreStr,
      String certStatusText,
      String verifStatusText) {

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try {
      String textStream = String.format(Locale.ROOT,
          "BT\n" +
          "/F1 18 Tf\n" +
          "130 730 Td\n" +
          "(NATIONAL SKILL DEVELOPMENT SYSTEM) Tj\n" +
          "/F2 12 Tf\n" +
          "-20 -25 Td\n" +
          "(Official Competency and Certification Register) Tj\n" +
          "/F1 15 Tf\n" +
          "20 -38 Td\n" +
          "(CERTIFICATE OF COMPETENCY) Tj\n" +
          "/F2 11 Tf\n" +
          "-80 -35 Td\n" +
          "(This is to certify that) Tj\n" +
          "/F1 15 Tf\n" +
          "0 -22 Td\n" +
          "(%s) Tj\n" +
          "/F2 10 Tf\n" +
          "0 -16 Td\n" +
          "(Registration No: %s) Tj\n" +
          "/F2 11 Tf\n" +
          "0 -28 Td\n" +
          "(has successfully completed the authorized assessment in) Tj\n" +
          "/F1 13 Tf\n" +
          "0 -22 Td\n" +
          "(%s [%s]) Tj\n" +
          "/F2 10 Tf\n" +
          "0 -18 Td\n" +
          "(Assessment: %s) Tj\n" +
          "/F2 10 Tf\n" +
          "0 -18 Td\n" +
          "(%s) Tj\n" +
          "/F2 10 Tf\n" +
          "0 -35 Td\n" +
          "(Certificate Number: %s) Tj\n" +
          "0 -15 Td\n" +
          "(Date of Issuance: %s) Tj\n" +
          "0 -15 Td\n" +
          "(Issuing Authority: %s) Tj\n" +
          "/F1 10 Tf\n" +
          "0 -18 Td\n" +
          "(%s) Tj\n" +
          "/F2 10 Tf\n" +
          "0 -15 Td\n" +
          "(%s) Tj\n" +
          "ET\n",
          escapePdfText(traineeName),
          escapePdfText(regNumber),
          escapePdfText(courseName),
          escapePdfText(courseCode),
          escapePdfText(assessmentTitle),
          escapePdfText(scoreStr),
          escapePdfText(certNumber),
          escapePdfText(issueDate),
          escapePdfText(issuingBody),
          escapePdfText(certStatusText),
          escapePdfText(verifStatusText)
      );

      byte[] textBytes = textStream.getBytes(StandardCharsets.US_ASCII);

      List<Integer> offsets = new ArrayList<>();
      offsets.add(0);

      StringBuilder sb = new StringBuilder();
      sb.append("%PDF-1.4\n");
      sb.append("%\u00E2\u00E3\u00CF\u00D3\n");

      offsets.add(sb.length());
      sb.append("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

      offsets.add(sb.length());
      sb.append("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");

      offsets.add(sb.length());
      sb.append("3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R /Resources << /Font << /F1 5 0 R /F2 6 0 R >> >> >>\nendobj\n");

      offsets.add(sb.length());
      sb.append("4 0 obj\n<< /Length ").append(textBytes.length).append(" >>\nstream\n");
      sb.append(textStream);
      sb.append("\nendstream\nendobj\n");

      offsets.add(sb.length());
      sb.append("5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>\nendobj\n");

      offsets.add(sb.length());
      sb.append("6 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n");

      int xrefOffset = sb.length();
      sb.append("xref\n");
      sb.append("0 7\n");
      sb.append("0000000000 65535 f \n");
      for (int i = 1; i <= 6; i++) {
        sb.append(String.format(Locale.ROOT, "%010d 00000 n \n", offsets.get(i)));
      }

      sb.append("trailer\n");
      sb.append("<< /Size 7 /Root 1 0 R >>\n");
      sb.append("startxref\n");
      sb.append(xrefOffset).append("\n");
      sb.append("%%EOF\n");

      return sb.toString().getBytes(StandardCharsets.UTF_8);
    } catch (Exception e) {
      log.error("Failed to generate PDF: {}", e.getMessage(), e);
      String fallback = "%PDF-1.4\n1 0 obj<< /Type /Catalog /Pages 2 0 R >>endobj\n2 0 obj<< /Type /Pages /Kids [3 0 R] /Count 1 >>endobj\n3 0 obj<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] >>endobj\nxref\n0 4\n0000000000 65535 f \n0000000009 00000 n \n0000000058 00000 n \n0000000115 00000 n \ntrailer<< /Size 4 /Root 1 0 R >>\nstartxref\n185\n%%EOF\n";
      return fallback.getBytes(StandardCharsets.UTF_8);
    }
  }

  private String escapePdfText(String text) {
    if (text == null) return "";
    String clean = text.replaceAll("[^\\x20-\\x7E]", " ");
    return clean.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
  }

  private AssessorAssessmentSummaryResponse mapToSummary(Assessment asm) {
    Long id = asm.getId();
    BigDecimal maxScore = asm.getMaximumScore() != null ? asm.getMaximumScore() : BigDecimal.valueOf(100.00);
    BigDecimal passScore = asm.getPassScore() != null ? asm.getPassScore() : maxScore.multiply(BigDecimal.valueOf(0.50)).setScale(2, RoundingMode.HALF_UP);

    BigDecimal theoryMax = maxScore.multiply(BigDecimal.valueOf(0.30)).setScale(2, RoundingMode.HALF_UP);
    BigDecimal practicalMax = maxScore.multiply(BigDecimal.valueOf(0.50)).setScale(2, RoundingMode.HALF_UP);
    BigDecimal vivaMax = maxScore.multiply(BigDecimal.valueOf(0.20)).setScale(2, RoundingMode.HALF_UP);

    Long batchId = asm.getTrainingBatch() != null ? asm.getTrainingBatch().getId() : null;
    String batchCode = asm.getTrainingBatch() != null ? asm.getTrainingBatch().getBatchCode() : null;

    Long courseId = asm.getCourse() != null ? asm.getCourse().getId() : null;
    String courseCode = asm.getCourse() != null ? asm.getCourse().getCourseCode() : null;
    String courseName = asm.getCourse() != null ? asm.getCourse().getCourseName() : null;

    Long programId = asm.getProgram() != null ? asm.getProgram().getId() : null;
    String programCode = asm.getProgram() != null ? asm.getProgram().getProgramCode() : null;
    String programName = asm.getProgram() != null ? asm.getProgram().getProgramName() : null;

    Long centerId = null;
    String centerCode = null;
    String centerName = null;
    Long providerId = null;
    String providerCode = null;
    String providerName = null;

    if (asm.getTrainingBatch() != null) {
      TrainingBatch tb = asm.getTrainingBatch();
      if (tb.getTrainingCenter() != null) {
        centerId = tb.getTrainingCenter().getId();
        centerCode = tb.getTrainingCenter().getCenterCode();
        centerName = tb.getTrainingCenter().getCenterName();
      }
      if (tb.getTrainingProvider() != null) {
        providerId = tb.getTrainingProvider().getId();
        providerCode = tb.getTrainingProvider().getProviderCode();
        providerName = tb.getTrainingProvider().getProviderName();
      }
    }

    Long typeId = asm.getAssessmentType() != null ? asm.getAssessmentType().getId() : null;
    String typeCode = asm.getAssessmentType() != null ? asm.getAssessmentType().getTypeCode() : "SUMMATIVE";
    String typeName = asm.getAssessmentType() != null ? asm.getAssessmentType().getTypeName() : "Summative Examination";

    Long lifecycleId = asm.getLifecycleStatusId() != null ? asm.getLifecycleStatusId() : 1L;
    String lifecycleCode = lifecycleId == 2L ? "FINALIZED" : "SCHEDULED";

    int totalCandidates = 0;
    int scoredCandidates = 0;
    int passedCandidates = 0;
    int failedCandidates = 0;

    if (batchId != null) {
      totalCandidates = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM training_enrollments WHERE batch_id = ? AND deleted_at IS NULL",
          Integer.class, batchId);
      scoredCandidates = jdbcTemplate.queryForObject(
          "SELECT COUNT(DISTINCT ar.assessment_result_id) FROM assessment_results ar JOIN trainee_assessments ta ON ar.trainee_assessment_id = ta.trainee_assessment_id WHERE ta.assessment_id = ?",
          Integer.class, id);
      passedCandidates = jdbcTemplate.queryForObject(
          "SELECT COUNT(DISTINCT ar.assessment_result_id) FROM assessment_results ar JOIN trainee_assessments ta ON ar.trainee_assessment_id = ta.trainee_assessment_id WHERE ta.assessment_id = ? AND ar.assessment_outcome_id = 2",
          Integer.class, id);
      failedCandidates = jdbcTemplate.queryForObject(
          "SELECT COUNT(DISTINCT ar.assessment_result_id) FROM assessment_results ar JOIN trainee_assessments ta ON ar.trainee_assessment_id = ta.trainee_assessment_id WHERE ta.assessment_id = ? AND ar.assessment_outcome_id IN (3, 4)",
          Integer.class, id);
    }

    return new AssessorAssessmentSummaryResponse(
        id,
        asm.getAssessmentCode(),
        asm.getAssessmentName(),
        typeId,
        typeCode,
        typeName,
        courseId,
        courseCode,
        courseName,
        batchId,
        batchCode,
        centerId,
        centerCode,
        centerName,
        providerId,
        providerCode,
        providerName,
        programId,
        programCode,
        programName,
        asm.getAssessmentDate(),
        maxScore,
        passScore,
        theoryMax,
        practicalMax,
        vivaMax,
        lifecycleId,
        lifecycleCode,
        totalCandidates,
        scoredCandidates,
        passedCandidates,
        failedCandidates,
        asm.getEvaluatorUserId(),
        asm.getEvaluatorName(),
        Boolean.valueOf("FINALIZED".equalsIgnoreCase(lifecycleCode) || "COMPLETED".equalsIgnoreCase(lifecycleCode))
    );
  }

  private User resolveAuthenticatedCaller() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
      throw new AccessDeniedException("Authentication required for assessor workbench");
    }
    String username = auth.getName();
    return userRepository.findByUsername(username)
        .orElseThrow(() -> new AccessDeniedException("Authenticated user '" + username + "' not found"));
  }

  private boolean isAdministrativeCaller() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth.getAuthorities() == null) {
      return false;
    }
    return auth.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch(a -> "ROLE_super_admin".equalsIgnoreCase(a) || "ROLE_government_admin".equalsIgnoreCase(a));
  }
}
