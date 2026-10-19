package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.CreateCertificationRequest;
import in.gov.sih.sih26135.dto.request.RecordAssessmentAndIssueCertificateRequest;
import in.gov.sih.sih26135.dto.request.UpdateTrainingEnrollmentRequest;
import in.gov.sih.sih26135.dto.response.AssessmentResultResponse;
import in.gov.sih.sih26135.dto.response.CertificationResponse;
import in.gov.sih.sih26135.dto.response.RecordAssessmentAndIssueCertificateResponse;
import in.gov.sih.sih26135.dto.response.TraineeAssessmentResponse;
import in.gov.sih.sih26135.dto.response.TraineeTrainingOutcomeSummaryResponse;
import in.gov.sih.sih26135.dto.response.TrainingEnrollmentResponse;
import in.gov.sih.sih26135.entity.CourseSkill;
import in.gov.sih.sih26135.entity.RefSkillGapStatus;
import in.gov.sih.sih26135.entity.SkillGap;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.repository.CourseSkillRepository;
import in.gov.sih.sih26135.repository.RefSkillGapStatusRepository;
import in.gov.sih.sih26135.repository.SkillGapRepository;
import in.gov.sih.sih26135.service.AssessmentResultService;
import in.gov.sih.sih26135.service.CertificationService;
import in.gov.sih.sih26135.service.TraineeAssessmentService;
import in.gov.sih.sih26135.service.TrainingEnrollmentService;
import in.gov.sih.sih26135.service.TrainingOutcomeWorkflowService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TrainingOutcomeWorkflowServiceImpl implements TrainingOutcomeWorkflowService {

  private static final String STATUS_CODE_IN_REMEDIATION = "IN_REMEDIATION";
  private static final String STATUS_CODE_RESOLVED = "RESOLVED";

  private final TrainingEnrollmentService trainingEnrollmentService;
  private final TraineeAssessmentService traineeAssessmentService;
  private final AssessmentResultService assessmentResultService;
  private final CertificationService certificationService;
  private final CourseSkillRepository courseSkillRepository;
  private final SkillGapRepository skillGapRepository;
  private final RefSkillGapStatusRepository refSkillGapStatusRepository;

  public TrainingOutcomeWorkflowServiceImpl(
      TrainingEnrollmentService trainingEnrollmentService,
      TraineeAssessmentService traineeAssessmentService,
      AssessmentResultService assessmentResultService,
      CertificationService certificationService,
      CourseSkillRepository courseSkillRepository,
      SkillGapRepository skillGapRepository,
      RefSkillGapStatusRepository refSkillGapStatusRepository) {
    this.trainingEnrollmentService = trainingEnrollmentService;
    this.traineeAssessmentService = traineeAssessmentService;
    this.assessmentResultService = assessmentResultService;
    this.certificationService = certificationService;
    this.courseSkillRepository = courseSkillRepository;
    this.skillGapRepository = skillGapRepository;
    this.refSkillGapStatusRepository = refSkillGapStatusRepository;
  }

  @Override
  @Transactional
  public RecordAssessmentAndIssueCertificateResponse recordAssessmentResultAndIssueCertificate(
      RecordAssessmentAndIssueCertificateRequest request) {
    if (request == null) {
      throw new BadRequestException("Request body cannot be null", "REQUEST_BODY_NULL");
    }
    if (request.getAssessmentResultRequest() == null) {
      throw new BadRequestException("Assessment result request is required", "ASSESSMENT_RESULT_REQUEST_REQUIRED");
    }

    CreateCertificationRequest certRequest = request.getCertificationRequest();
    if (certRequest != null && certRequest.getTraineeId() != null
        && request.getAssessmentResultRequest().getTraineeId() != null
        && !certRequest.getTraineeId().equals(request.getAssessmentResultRequest().getTraineeId())) {
      throw new BadRequestException(
          "Certification trainee ID does not match assessment result trainee",
          "CERTIFICATION_TRAINEE_MISMATCH"
      );
    }

    Long targetEnrollmentId = null;
    TrainingEnrollmentResponse targetEnrollment = null;
    if (request.getCompletionEnrollmentStatusId() != null) {
      targetEnrollmentId = request.getEnrollmentId();
      if (targetEnrollmentId == null && certRequest != null) {
        targetEnrollmentId = certRequest.getEnrollmentId();
      }
      if (targetEnrollmentId == null) {
        throw new BadRequestException(
            "Enrollment ID is required to update enrollment status",
            "ENROLLMENT_ID_REQUIRED"
        );
      }
      targetEnrollment = trainingEnrollmentService.getById(targetEnrollmentId);
      if (request.getAssessmentResultRequest().getTraineeId() != null
          && !targetEnrollment.getTraineeId().equals(request.getAssessmentResultRequest().getTraineeId())) {
        throw new BadRequestException(
            "Enrollment trainee does not match assessment result trainee",
            "ENROLLMENT_TRAINEE_MISMATCH"
        );
      }
      if (certRequest != null && certRequest.getTraineeId() != null
          && !targetEnrollment.getTraineeId().equals(certRequest.getTraineeId())) {
        throw new BadRequestException(
            "Enrollment trainee does not match assessment result trainee",
            "ENROLLMENT_TRAINEE_MISMATCH"
        );
      }
    }

    AssessmentResultResponse result = assessmentResultService.createAssessmentResult(
        request.getAssessmentResultRequest());

    if (targetEnrollment != null && !targetEnrollment.getTraineeId().equals(result.getTraineeId())) {
      throw new BadRequestException(
          "Enrollment trainee does not match assessment result trainee",
          "ENROLLMENT_TRAINEE_MISMATCH"
      );
    }

    CertificationResponse certification = null;
    if (certRequest != null) {
      if (!Boolean.TRUE.equals(result.getIsPassFlag())) {
        throw new BadRequestException(
            "Cannot issue certificate for non-passing assessment result",
            "ASSESSMENT_NOT_PASSED"
        );
      }
      if (certRequest.getTraineeId() != null && !certRequest.getTraineeId().equals(result.getTraineeId())) {
        throw new BadRequestException(
            "Certification trainee ID does not match assessment result trainee",
            "CERTIFICATION_TRAINEE_MISMATCH"
        );
      }
      certRequest.setAssessmentResultId(result.getId());
      certRequest.setTraineeId(result.getTraineeId());
      certification = certificationService.createCertification(certRequest);
    }

    TrainingEnrollmentResponse updatedEnrollment = null;
    if (targetEnrollment != null) {
      UpdateTrainingEnrollmentRequest updateReq = new UpdateTrainingEnrollmentRequest();
      updateReq.setStartDate(targetEnrollment.getStartDate());
      updateReq.setExpectedCompletionDate(targetEnrollment.getExpectedCompletionDate());
      updateReq.setActualCompletionDate(
          targetEnrollment.getActualCompletionDate() != null
              ? targetEnrollment.getActualCompletionDate()
              : LocalDate.now()
      );
      updateReq.setEnrollmentStatusId(request.getCompletionEnrollmentStatusId());
      updateReq.setDropoutReasonId(targetEnrollment.getDropoutReasonId());
      updateReq.setDropoutRemarks(targetEnrollment.getDropoutRemarks());

      updatedEnrollment = trainingEnrollmentService.updateEnrollment(targetEnrollmentId, updateReq);

      // WF-001: Resolve IN_REMEDIATION skill gaps when:
      //   (a) the new enrollment status is a completion status, AND
      //   (b) the assessment result is a pass.
      if (Boolean.TRUE.equals(updatedEnrollment.getIsCompletedFlag())
          && Boolean.TRUE.equals(result.getIsPassFlag())) {
        resolveInRemediationGapsForCompletion(
            updatedEnrollment.getTraineeId(),
            updatedEnrollment.getCourseId(),
            updatedEnrollment.getActualCompletionDate()
        );
      }
    }

    return new RecordAssessmentAndIssueCertificateResponse(result, certification, updatedEnrollment);
  }

  /**
   * Resolves any current IN_REMEDIATION skill gaps for the given trainee whose gap skill is taught
   * by the completed course. Each eligible gap has its status set to RESOLVED, resolvedOn set to
   * the actual completion date, and isCurrent set to false (archiving it from the current-gap view).
   *
   * <p>Invoked only when both the enrollment status is a completion status and the assessment
   * result records a pass. Gaps with any other status (OPEN, RESOLVED, CLOSED_UNRESOLVED) are
   * left untouched.
   */
  private void resolveInRemediationGapsForCompletion(
      Long traineeId, Long courseId, LocalDate resolvedOn) {
    if (traineeId == null || courseId == null) {
      return;
    }

    // Find every skill taught by the completed course.
    List<CourseSkill> courseSkills = courseSkillRepository.findByCourseId(courseId);
    if (courseSkills == null || courseSkills.isEmpty()) {
      return;
    }
    Set<Long> taughtSkillIds = courseSkills.stream()
        .filter(cs -> cs != null && cs.getSkillId() != null)
        .map(CourseSkill::getSkillId)
        .collect(Collectors.toSet());

    if (taughtSkillIds.isEmpty()) {
      return;
    }

    // Find all current IN_REMEDIATION gaps for this trainee.
    List<SkillGap> inRemediationGaps = skillGapRepository
        .findByTraineeIdAndIsCurrentTrueAndSkillGapStatusStatusCode(
            traineeId, STATUS_CODE_IN_REMEDIATION);

    if (inRemediationGaps == null || inRemediationGaps.isEmpty()) {
      return;
    }

    // Keep only gaps whose skill reference is valid and taught by the completed course.
    List<SkillGap> eligibleGaps = inRemediationGaps.stream()
        .filter(g -> g != null && g.getSkill() != null && g.getSkill().getId() != null)
        .filter(g -> taughtSkillIds.contains(g.getSkill().getId()))
        .toList();

    if (eligibleGaps.isEmpty()) {
      return;
    }

    // Look up RESOLVED status once. Fail closed if it is missing from the database.
    RefSkillGapStatus resolvedStatus = refSkillGapStatusRepository
        .findByStatusCode(STATUS_CODE_RESOLVED)
        .orElseThrow(() -> new ResourceNotFoundException("RefSkillGapStatus", "statusCode"));

    LocalDate effectiveResolvedOn = resolvedOn != null ? resolvedOn : LocalDate.now();
    LocalDateTime now = LocalDateTime.now();

    for (SkillGap gap : eligibleGaps) {
      if (gap.getIdentifiedOn() != null && effectiveResolvedOn.isBefore(gap.getIdentifiedOn())) {
        throw new BadRequestException(
            "Resolved date cannot be before identified date", "RESOLVED_DATE_BEFORE_IDENTIFIED");
      }
      gap.setSkillGapStatus(resolvedStatus);
      gap.setResolvedOn(effectiveResolvedOn);
      gap.setIsCurrent(false);
      gap.setUpdatedAt(now);
      skillGapRepository.save(gap);
    }
  }

  @Override
  public TraineeTrainingOutcomeSummaryResponse getTraineeTrainingOutcomeSummary(
      Long traineeId, Long enrollmentId) {
    if (traineeId == null) {
      throw new BadRequestException("Trainee ID is required", "TRAINEE_ID_REQUIRED");
    }
    if (enrollmentId == null) {
      throw new BadRequestException("Enrollment ID is required", "ENROLLMENT_ID_REQUIRED");
    }

    TrainingEnrollmentResponse enrollment = trainingEnrollmentService.getById(enrollmentId);
    if (!enrollment.getTraineeId().equals(traineeId)) {
      throw new BadRequestException(
          "Enrollment does not belong to the specified trainee",
          "TRAINEE_ENROLLMENT_MISMATCH"
      );
    }

    List<TraineeAssessmentResponse> assessments = traineeAssessmentService.getByEnrollmentId(enrollmentId);
    List<AssessmentResultResponse> results = assessmentResultService.getByTraineeId(traineeId);
    List<CertificationResponse> certs = certificationService.getByEnrollmentId(enrollmentId);

    return new TraineeTrainingOutcomeSummaryResponse(enrollment, assessments, results, certs);
  }
}
