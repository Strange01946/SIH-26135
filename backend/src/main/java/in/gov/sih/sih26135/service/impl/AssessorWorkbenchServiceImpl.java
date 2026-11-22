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
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.entity.TraineeAssessment;
import in.gov.sih.sih26135.entity.TrainingBatch;
import in.gov.sih.sih26135.entity.TrainingEnrollment;
import in.gov.sih.sih26135.entity.User;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.repository.AssessmentRepository;
import in.gov.sih.sih26135.repository.AssessmentResultRepository;
import in.gov.sih.sih26135.repository.CertificationRepository;
import in.gov.sih.sih26135.repository.CourseRepository;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
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
  public static final Long CERT_VERIF_STATUS_VERIFIED_ID = 3L;
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
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
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
        if (remarks != null && remarks.contains("\"theory\"")) {
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

    Assessment assessment = assessmentRepository.findById(assessmentId)
        .orElseThrow(() -> new ResourceNotFoundException("Assessment", "id=" + assessmentId));

    User caller = resolveAuthenticatedCaller();
    if (!isAdministrativeCaller() && (assessment.getEvaluatorUserId() == null || !assessment.getEvaluatorUserId().equals(caller.getId()))) {
      throw new AccessDeniedException("Assessment does not belong to authenticated assessor scope");
    }

    // Check if assessment is already finalized
    if (assessment.getLifecycleStatusId() != null && assessment.getLifecycleStatusId().equals(LIFECYCLE_FINALIZED_ID)) {
      if (request.isDraft()) {
        throw new BadRequestException("Cannot save draft on an already finalized assessment", "ASSESSMENT_ALREADY_FINALIZED");
      }
      // If finalizing again, check if already completed
      log.info("Assessment {} is already finalized. Verifying idempotent response.", assessmentId);
    }

    BigDecimal maxTotal = assessment.getMaximumScore() != null ? assessment.getMaximumScore() : BigDecimal.valueOf(100.00);
    BigDecimal theoryMax = maxTotal.multiply(BigDecimal.valueOf(0.30)).setScale(2, RoundingMode.HALF_UP);
    BigDecimal practicalMax = maxTotal.multiply(BigDecimal.valueOf(0.50)).setScale(2, RoundingMode.HALF_UP);
    BigDecimal vivaMax = maxTotal.multiply(BigDecimal.valueOf(0.20)).setScale(2, RoundingMode.HALF_UP);
    BigDecimal passScore = assessment.getPassScore() != null
        ? assessment.getPassScore()
        : maxTotal.multiply(BigDecimal.valueOf(0.50)).setScale(2, RoundingMode.HALF_UP);

    Long batchId = assessment.getTrainingBatch() != null ? assessment.getTrainingBatch().getId() : null;

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
    RefCertificateVerificationStatus verifStatusVerified = refCertificateVerificationStatusRepository.findById(CERT_VERIF_STATUS_VERIFIED_ID)
        .orElseThrow(() -> new ResourceNotFoundException("RefCertificateVerificationStatus", "id=" + CERT_VERIF_STATUS_VERIFIED_ID));
    RefEnrollmentStatus statusCompleted = refEnrollmentStatusRepository.findById(ENROLLMENT_STATUS_COMPLETED_ID).orElse(null);

    for (CandidateScoreInput input : request.getCandidates()) {
      Long traineeId = input.getTraineeId();
      Long enrollmentId = input.getEnrollmentId();

      // Verify enrollment belongs to batch
      TrainingEnrollment enrollment = trainingEnrollmentRepository.findById(enrollmentId)
          .orElseThrow(() -> new ResourceNotFoundException("TrainingEnrollment", "id=" + enrollmentId));

      if (batchId != null && (enrollment.getTrainingBatch() == null || !enrollment.getTrainingBatch().getId().equals(batchId))) {
        throw new BadRequestException("Trainee #" + traineeId + " enrollment #" + enrollmentId + " does not belong to assessment batch #" + batchId, "INVALID_CANDIDATE");
      }
      if (enrollment.getTrainee() == null || !enrollment.getTrainee().getId().equals(traineeId)) {
        throw new BadRequestException("Enrollment trainee mismatch for candidate #" + traineeId, "TRAINEE_ENROLLMENT_MISMATCH");
      }

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

      // JSON remarks for component storage
      String jsonRemarks = String.format(Locale.ROOT,
          "{\"theory\":%s,\"practical\":%s,\"viva\":%s,\"status\":\"%s\"%s}",
          theory != null ? theory.toPlainString() : "null",
          practical != null ? practical.toPlainString() : "null",
          viva != null ? viva.toPlainString() : "null",
          statusStr,
          input.getRemarks() != null && !input.getRemarks().isBlank()
              ? ",\"note\":\"" + input.getRemarks().replace("\"", "\\\"") + "\""
              : ""
      );

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

          Certification cert = new Certification();
          cert.setCertificateNumber(certNumber);
          cert.setTrainee(trainee);
          cert.setTrainingEnrollment(enrollment);
          cert.setCourse(course);
          cert.setProgram(program);
          cert.setAssessmentResult(savedResult);
          cert.setIssuingBody("National Council for Vocational Education and Training (NCVET)");
          cert.setIssueDate(today);
          cert.setExpiryDate(today.plusYears(5));
          cert.setCertificateStatus(statusIssued);
          cert.setCertificateVerificationStatus(verifStatusVerified);
          cert.setVerifiedAt(now);
          cert.setVerifiedByUserId(caller.getId());
          cert.setCreatedAt(now);
          cert.setUpdatedAt(now);

          certificationRepository.save(cert);
          certsIssued++;

          // Update training enrollment status to COMPLETED
          if (statusCompleted != null) {
            enrollment.setEnrollmentStatus(statusCompleted);
          }
          enrollment.setActualCompletionDate(today);
          enrollment.setUpdatedAt(now);
          trainingEnrollmentRepository.save(enrollment);
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
  public byte[] generateCertificatePdf(Long certificationId) {
    if (certificationId == null || certificationId <= 0) {
      throw new BadRequestException("Certification ID must be valid and positive");
    }

    Certification cert = certificationRepository.findById(certificationId)
        .orElseThrow(() -> new ResourceNotFoundException("Certification", "id=" + certificationId));

    Trainee trainee = cert.getTrainee();
    Course course = cert.getCourse();
    TrainingEnrollment enrollment = cert.getTrainingEnrollment();
    AssessmentResult result = cert.getAssessmentResult();

    String traineeName = trainee != null ? (trainee.getFirstName() + " " + Objects.toString(trainee.getLastName(), "")).trim() : "Candidate";
    String regNumber = trainee != null ? trainee.getRegistrationNumber() : "N/A";
    String courseName = course != null ? course.getCourseName() : "Vocational Training Program";
    String courseCode = course != null ? course.getCourseCode() : "N/A";
    String certNumber = cert.getCertificateNumber();
    String issueDate = cert.getIssueDate() != null ? cert.getIssueDate().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy")) : "N/A";
    String issuingBody = cert.getIssuingBody() != null ? cert.getIssuingBody() : "National Council for Vocational Education and Training (NCVET)";

    String scoreStr = "Grade: Distinction / Pass";
    if (result != null && result.getScorePercentage() != null) {
      scoreStr = "Score Achieved: " + result.getScorePercentage().toPlainString() + "% (PASS)";
    }

    // Construct valid PDF 1.4 document
    return buildPdfDocument(traineeName, regNumber, courseName, courseCode, certNumber, issueDate, issuingBody, scoreStr);
  }

  private byte[] buildPdfDocument(
      String traineeName,
      String regNumber,
      String courseName,
      String courseCode,
      String certNumber,
      String issueDate,
      String issuingBody,
      String scoreStr) {

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try {
      // Content stream commands for PDF
      String textStream = String.format(Locale.ROOT,
          "BT\n" +
          "/F1 20 Tf\n" +
          "150 720 Td\n" +
          "(GOVERNMENT OF INDIA) Tj\n" +
          "/F1 14 Tf\n" +
          "-40 -28 Td\n" +
          "(MINISTRY OF SKILL DEVELOPMENT AND ENTREPRENEURSHIP) Tj\n" +
          "/F2 11 Tf\n" +
          "20 -20 Td\n" +
          "(National Council for Vocational Education and Training - NCVET) Tj\n" +
          "/F1 16 Tf\n" +
          "30 -40 Td\n" +
          "(CERTIFICATE OF COMPETENCY) Tj\n" +
          "/F2 12 Tf\n" +
          "-120 -40 Td\n" +
          "(This is to certify that) Tj\n" +
          "/F1 16 Tf\n" +
          "0 -24 Td\n" +
          "(%s) Tj\n" +
          "/F2 11 Tf\n" +
          "0 -18 Td\n" +
          "(Registration No: %s) Tj\n" +
          "/F2 12 Tf\n" +
          "0 -30 Td\n" +
          "(has successfully completed the authorized national skill assessment in) Tj\n" +
          "/F1 14 Tf\n" +
          "0 -24 Td\n" +
          "(%s [%s]) Tj\n" +
          "/F2 11 Tf\n" +
          "0 -24 Td\n" +
          "(%s) Tj\n" +
          "/F2 10 Tf\n" +
          "0 -40 Td\n" +
          "(Certificate Number: %s) Tj\n" +
          "0 -16 Td\n" +
          "(Date of Issuance: %s) Tj\n" +
          "0 -16 Td\n" +
          "(Issuing Authority: %s) Tj\n" +
          "0 -16 Td\n" +
          "(Verification Status: DIGITALLY VERIFIED - NCVET NATIONAL PORTAL) Tj\n" +
          "ET\n",
          escapePdfText(traineeName),
          escapePdfText(regNumber),
          escapePdfText(courseName),
          escapePdfText(courseCode),
          escapePdfText(scoreStr),
          escapePdfText(certNumber),
          escapePdfText(issueDate),
          escapePdfText(issuingBody)
      );

      byte[] textBytes = textStream.getBytes(StandardCharsets.US_ASCII);

      // Assemble PDF objects and calculate offsets
      List<Integer> offsets = new ArrayList<>();
      offsets.add(0); // 0th object

      StringBuilder sb = new StringBuilder();
      sb.append("%PDF-1.4\n");
      sb.append("%\u00E2\u00E3\u00CF\u00D3\n"); // 4 binary bytes for proper PDF detection

      // Object 1: Catalog
      offsets.add(sb.length());
      sb.append("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

      // Object 2: Pages
      offsets.add(sb.length());
      sb.append("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");

      // Object 3: Page
      offsets.add(sb.length());
      sb.append("3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R /Resources << /Font << /F1 5 0 R /F2 6 0 R >> >> >>\nendobj\n");

      // Object 4: Contents
      offsets.add(sb.length());
      sb.append("4 0 obj\n<< /Length ").append(textBytes.length).append(" >>\nstream\n");
      sb.append(textStream);
      sb.append("\nendstream\nendobj\n");

      // Object 5: Font F1 (Helvetica-Bold)
      offsets.add(sb.length());
      sb.append("5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>\nendobj\n");

      // Object 6: Font F2 (Helvetica)
      offsets.add(sb.length());
      sb.append("6 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n");

      // Xref table
      int xrefOffset = sb.length();
      sb.append("xref\n");
      sb.append("0 7\n");
      sb.append("0000000000 65535 f \n");
      for (int i = 1; i <= 6; i++) {
        sb.append(String.format(Locale.ROOT, "%010d 00000 n \n", offsets.get(i)));
      }

      // Trailer
      sb.append("trailer\n");
      sb.append("<< /Size 7 /Root 1 0 R >>\n");
      sb.append("startxref\n");
      sb.append(xrefOffset).append("\n");
      sb.append("%%EOF\n");

      return sb.toString().getBytes(StandardCharsets.UTF_8);
    } catch (Exception e) {
      log.error("Failed to generate PDF: {}", e.getMessage(), e);
      // Fallback valid minimal PDF
      String fallback = "%PDF-1.4\n1 0 obj<< /Type /Catalog /Pages 2 0 R >>endobj\n2 0 obj<< /Type /Pages /Kids [3 0 R] /Count 1 >>endobj\n3 0 obj<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] >>endobj\nxref\n0 4\n0000000000 65535 f \n0000000009 00000 n \n0000000058 00000 n \n0000000115 00000 n \ntrailer<< /Size 4 /Root 1 0 R >>\nstartxref\n185\n%%EOF\n";
      return fallback.getBytes(StandardCharsets.UTF_8);
    }
  }

  private String escapePdfText(String text) {
    if (text == null) return "";
    return text.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
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
