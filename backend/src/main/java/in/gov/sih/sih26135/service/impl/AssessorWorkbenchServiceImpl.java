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

  private String resolveAuthoritativeIssuingBody(Assessment assessment) {
    // 1. Parent Organization (if accredited awarding body / council)
    if (assessment.getTrainingBatch() != null && assessment.getTrainingBatch().getTrainingProvider() != null) {
      TrainingProvider provider = assessment.getTrainingBatch().getTrainingProvider();
      if (provider.getOrganizationId() != null && organizationRepository != null) {
        Optional<Organization> org = organizationRepository.findById(provider.getOrganizationId());
        if (org.isPresent() && org.get().getOrganizationName() != null && !org.get().getOrganizationName().isBlank()) {
          return org.get().getOrganizationName().trim();
        }
      }
    }

    // 2. Sector Skill Council / Awarding Body via Course Sector
    if (assessment.getCourse() != null && assessment.getCourse().getSector() != null) {
      String sectorName = assessment.getCourse().getSector().getSectorName();
      if (sectorName != null && !sectorName.isBlank()) {
        String cleanSector = sectorName.trim();
        if (cleanSector.toLowerCase(Locale.ROOT).contains("council")
            || cleanSector.toLowerCase(Locale.ROOT).contains("body")
            || cleanSector.toLowerCase(Locale.ROOT).contains("board")) {
          return cleanSector;
        }
        return cleanSector + " Skill Council";
      }
    }

    // 3. Provider Name
    if (assessment.getTrainingBatch() != null && assessment.getTrainingBatch().getTrainingProvider() != null) {
      TrainingProvider provider = assessment.getTrainingBatch().getTrainingProvider();
      if (provider.getProviderName() != null && !provider.getProviderName().isBlank()) {
        return provider.getProviderName().trim();
      }
    }

    // 4. Program
    if (assessment.getProgram() != null && assessment.getProgram().getProgramName() != null && !assessment.getProgram().getProgramName().isBlank()) {
      return assessment.getProgram().getProgramName().trim();
    }

    return "Authorized Awarding Body";
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

    if (search != null && !search.isBlank()) {
      String pattern = "%" + search.trim() + "%";
      sql.append(" AND (a.assessment_name LIKE ? OR a.assessment_code LIKE ? OR c.course_name LIKE ? OR tb.batch_code LIKE ?)");
      params.add(pattern);
      params.add(pattern);
      params.add(pattern);
      params.add(pattern);
    }

    sql.append(" ORDER BY a.assessment_date ASC, a.assessment_id ASC");

    return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
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

      // Idempotent return without database mutations
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

      // Certification on Finalize for Passed candidates
      if (!request.isDraft() && isPass) {
        Optional<Certification> existingCert = certificationRepository.findFirstByAssessmentResultId(savedResult.getId());
        if (existingCert.isEmpty()) {
          existingCert = certificationRepository.findFirstByTrainingEnrollmentId(enrollment.getId());
        }

        if (existingCert.isEmpty()) {
          Course course = assessment.getCourse();
          Program program = assessment.getProgram();

          String courseCode = course != null ? course.getCourseCode() : "CRS";
          String certNumber = "CERT-" + courseCode + "-" + traineeId + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
          String issuingBody = resolveAuthoritativeIssuingBody(assessment);

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

          // Evaluate training enrollment completion
          if (enrollment.getEnrollmentStatus() != null) {
            Long curStatusId = enrollment.getEnrollmentStatus().getId();
            if (curStatusId == 1L || curStatusId == 2L) { // Only active enrollments
              boolean meetsRequirements = true;

              // Batch status check: batch must not be CANCELLED (5) or SUSPENDED (6)
              if (assessment.getTrainingBatch() != null && assessment.getTrainingBatch().getBatchStatus() != null) {
                Long batchStatusId = assessment.getTrainingBatch().getBatchStatus().getId();
                if (batchStatusId == 5L || batchStatusId == 6L) {
                  meetsRequirements = false;
                }
              }

              // Attendance requirement check: if attendance records exist for this enrollment, verify minimum attendance (70%)
              if (meetsRequirements) {
                Long totalSessions = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM attendance_records WHERE enrollment_id = ?",
                    Long.class, enrollment.getId());
                if (totalSessions != null && totalSessions > 0) {
                  Long attendedSessions = jdbcTemplate.queryForObject(
                      "SELECT COUNT(*) FROM attendance_records WHERE enrollment_id = ? AND attendance_status_id IN (1, 3, 6)",
                      Long.class, enrollment.getId());
                  double rate = (attendedSessions != null ? attendedSessions.doubleValue() : 0.0) / totalSessions.doubleValue();
                  if (rate < 0.70) {
                    meetsRequirements = false;
                    log.info("Enrollment {} has attendance rate {} below 70% threshold; training completion deferred", enrollment.getId(), rate);
                  }
                }
              }

              if (meetsRequirements && statusCompleted != null) {
                enrollment.setEnrollmentStatus(statusCompleted);
                if (enrollment.getActualCompletionDate() == null) {
                  enrollment.setActualCompletionDate(today);
                }
                enrollment.setUpdatedAt(now);
                trainingEnrollmentRepository.save(enrollment);
              }
            } else if (curStatusId == 3L) { // Already completed
              if (enrollment.getActualCompletionDate() == null) {
                enrollment.setActualCompletionDate(today);
                enrollment.setUpdatedAt(now);
                trainingEnrollmentRepository.save(enrollment);
              }
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
        ? cert.getIssuingBody()
        : "Authorized Awarding Body";

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
