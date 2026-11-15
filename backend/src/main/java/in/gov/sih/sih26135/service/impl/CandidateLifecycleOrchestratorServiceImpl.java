package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.annotation.AuditTrail;
import in.gov.sih.sih26135.config.cache.AnalyticsCacheEvictionService;
import in.gov.sih.sih26135.correlation.CorrelationContextHolder;
import in.gov.sih.sih26135.dto.request.CreateSkillGapAssessmentRequest;
import in.gov.sih.sih26135.dto.request.CreateTrainingEnrollmentRequest;
import in.gov.sih.sih26135.dto.request.EmploymentExitAndUnemploymentRequest;
import in.gov.sih.sih26135.dto.request.PlacementToEmploymentTransitionRequest;
import in.gov.sih.sih26135.dto.request.RecordAssessmentAndIssueCertificateRequest;
import in.gov.sih.sih26135.dto.request.RecordSkillGapAssessmentWithGapsRequest;
import in.gov.sih.sih26135.dto.request.RecordVerificationVerdictRequest;
import in.gov.sih.sih26135.dto.request.RemediationTriggerRequest;
import in.gov.sih.sih26135.dto.request.TraineeEnrollmentOrchestrationRequest;
import in.gov.sih.sih26135.dto.response.CandidateLifecycleSummaryResponse;
import in.gov.sih.sih26135.dto.response.CertificationResponse;
import in.gov.sih.sih26135.dto.response.EmploymentExitAndUnemploymentResponse;
import in.gov.sih.sih26135.dto.response.PlacementToEmploymentTransitionResponse;
import in.gov.sih.sih26135.dto.response.RecordAssessmentAndIssueCertificateResponse;
import in.gov.sih.sih26135.dto.response.RecordSkillGapAssessmentWithGapsResponse;
import in.gov.sih.sih26135.dto.response.RecordVerificationVerdictResponse;
import in.gov.sih.sih26135.dto.response.RemediationTriggerResponse;
import in.gov.sih.sih26135.dto.response.TraineeCareerTimelineResponse;
import in.gov.sih.sih26135.dto.response.TraineeConsentResponse;
import in.gov.sih.sih26135.dto.response.TraineeEnrollmentOrchestrationResponse;
import in.gov.sih.sih26135.dto.response.TraineePlacementEmploymentSummaryResponse;
import in.gov.sih.sih26135.dto.response.TraineeSkillGapProfileResponse;
import in.gov.sih.sih26135.dto.response.TrainingEnrollmentResponse;
import in.gov.sih.sih26135.entity.RefEnrollmentStatus;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.entity.TrainingBatch;
import in.gov.sih.sih26135.entity.User;
import in.gov.sih.sih26135.event.CommunicationTriggerEvent;
import in.gov.sih.sih26135.event.DataQualityAlertEvent;
import in.gov.sih.sih26135.event.EmploymentVerificationEvent;
import in.gov.sih.sih26135.event.PlacementLifecycleEvent;
import in.gov.sih.sih26135.event.TraineeLifecycleEvent;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.repository.RefEnrollmentStatusRepository;
import in.gov.sih.sih26135.repository.TraineeRepository;
import in.gov.sih.sih26135.repository.TrainingBatchRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import in.gov.sih.sih26135.security.UserPrincipal;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import in.gov.sih.sih26135.service.CandidateLifecycleOrchestratorService;
import in.gov.sih.sih26135.service.EmploymentLifecycleWorkflowService;
import in.gov.sih.sih26135.service.PlacementEmploymentWorkflowService;
import in.gov.sih.sih26135.service.SkillGapRemediationWorkflowService;
import in.gov.sih.sih26135.service.TraineeConsentService;
import in.gov.sih.sih26135.service.TrainingEnrollmentService;
import in.gov.sih.sih26135.service.TrainingOutcomeWorkflowService;
import in.gov.sih.sih26135.service.VerificationWorkflowService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default implementation of {@link CandidateLifecycleOrchestratorService}.
 * Coordinates authoritative domain services across registration, training, placement,
 * verification, exit, and remediation without violating transaction boundaries or duplicating logic.
 */
@Service
@Transactional(readOnly = true)
public class CandidateLifecycleOrchestratorServiceImpl implements CandidateLifecycleOrchestratorService {

  private static final Logger log = LoggerFactory.getLogger(CandidateLifecycleOrchestratorServiceImpl.class);

  private final TrainingOutcomeWorkflowService trainingOutcomeWorkflowService;
  private final PlacementEmploymentWorkflowService placementEmploymentWorkflowService;
  private final VerificationWorkflowService verificationWorkflowService;
  private final EmploymentLifecycleWorkflowService employmentLifecycleWorkflowService;
  private final SkillGapRemediationWorkflowService skillGapRemediationWorkflowService;
  private final TraineeConsentService traineeConsentService;
  private final TrainingEnrollmentService trainingEnrollmentService;
  private final TrainingBatchRepository trainingBatchRepository;
  private final TraineeRepository traineeRepository;
  private final RefEnrollmentStatusRepository refEnrollmentStatusRepository;
  private final ApplicationEventPublisher eventPublisher;
  private final WorkflowSecurityScopeValidator scopeValidator;

  private UserRepository userRepository;
  private AnalyticsCacheEvictionService evictionService;

  @Autowired
  public CandidateLifecycleOrchestratorServiceImpl(
      TrainingOutcomeWorkflowService trainingOutcomeWorkflowService,
      PlacementEmploymentWorkflowService placementEmploymentWorkflowService,
      VerificationWorkflowService verificationWorkflowService,
      EmploymentLifecycleWorkflowService employmentLifecycleWorkflowService,
      SkillGapRemediationWorkflowService skillGapRemediationWorkflowService,
      TraineeConsentService traineeConsentService,
      TrainingEnrollmentService trainingEnrollmentService,
      TrainingBatchRepository trainingBatchRepository,
      TraineeRepository traineeRepository,
      RefEnrollmentStatusRepository refEnrollmentStatusRepository,
      ApplicationEventPublisher eventPublisher,
      WorkflowSecurityScopeValidator scopeValidator) {
    this.trainingOutcomeWorkflowService = Objects.requireNonNull(trainingOutcomeWorkflowService, "TrainingOutcomeWorkflowService is required");
    this.placementEmploymentWorkflowService = Objects.requireNonNull(placementEmploymentWorkflowService, "PlacementEmploymentWorkflowService is required");
    this.verificationWorkflowService = Objects.requireNonNull(verificationWorkflowService, "VerificationWorkflowService is required");
    this.employmentLifecycleWorkflowService = Objects.requireNonNull(employmentLifecycleWorkflowService, "EmploymentLifecycleWorkflowService is required");
    this.skillGapRemediationWorkflowService = Objects.requireNonNull(skillGapRemediationWorkflowService, "SkillGapRemediationWorkflowService is required");
    this.traineeConsentService = Objects.requireNonNull(traineeConsentService, "TraineeConsentService is required");
    this.trainingEnrollmentService = Objects.requireNonNull(trainingEnrollmentService, "TrainingEnrollmentService is required");
    this.trainingBatchRepository = Objects.requireNonNull(trainingBatchRepository, "TrainingBatchRepository is required");
    this.traineeRepository = Objects.requireNonNull(traineeRepository, "TraineeRepository is required");
    this.refEnrollmentStatusRepository = Objects.requireNonNull(refEnrollmentStatusRepository, "RefEnrollmentStatusRepository is required");
    this.eventPublisher = Objects.requireNonNull(eventPublisher, "ApplicationEventPublisher is required");
    this.scopeValidator = scopeValidator;
  }

  @Autowired(required = false)
  public void setUserRepository(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Autowired(required = false)
  public void setEvictionService(AnalyticsCacheEvictionService evictionService) {
    this.evictionService = evictionService;
  }

  @Override
  @Transactional
  @AuditTrail(action = "CREATE", entityType = "TrainingEnrollment", summary = "Candidate enrolled with validated consent")
  public TraineeEnrollmentOrchestrationResponse enrollCandidateWithConsentValidation(
      TraineeEnrollmentOrchestrationRequest request) {
    if (request == null) {
      throw new BadRequestException("Request body cannot be null", "REQUEST_BODY_NULL");
    }
    if (request.getTraineeId() == null) {
      throw new BadRequestException("Trainee ID is required", "TRAINEE_ID_REQUIRED");
    }
    if (request.getBatchId() == null) {
      throw new BadRequestException("Training batch ID is required", "BATCH_ID_REQUIRED");
    }
    if (request.getEnrollmentDate() == null) {
      throw new BadRequestException("Enrollment date is required", "ENROLLMENT_DATE_REQUIRED");
    }

    Trainee trainee = traineeRepository.findById(request.getTraineeId())
        .orElseThrow(() -> new ResourceNotFoundException("Trainee", "traineeId"));
    if (trainee.getDeletedAt() != null) {
      throw new BadRequestException("Cannot create training enrollment for soft-deleted trainee", "TRAINEE_DELETED");
    }

    TrainingBatch batch = trainingBatchRepository.findById(request.getBatchId())
        .orElseThrow(() -> new ResourceNotFoundException("TrainingBatch", "batchId"));
    if (batch.getDeletedAt() != null) {
      throw new BadRequestException("Cannot create training enrollment for soft-deleted training batch", "BATCH_DELETED");
    }

    if (request.getTrainingProviderId() != null) {
      if (batch.getTrainingProvider() == null || !request.getTrainingProviderId().equals(batch.getTrainingProvider().getId())) {
        throw new BadRequestException("Training provider does not match training batch provider", "PROVIDER_BATCH_MISMATCH");
      }
    }
    if (request.getCourseId() != null) {
      if (batch.getCourse() == null || !request.getCourseId().equals(batch.getCourse().getId())) {
        throw new BadRequestException("Course does not match training batch course", "COURSE_BATCH_MISMATCH");
      }
    }
    if (request.getProgramId() != null) {
      if (batch.getProgram() == null || !request.getProgramId().equals(batch.getProgram().getId())) {
        throw new BadRequestException("Program does not match training batch program", "PROGRAM_BATCH_MISMATCH");
      }
    }
    if (request.getCenterId() != null) {
      if (batch.getTrainingCenter() == null || !request.getCenterId().equals(batch.getTrainingCenter().getId())) {
        throw new BadRequestException("Center does not match training batch center", "CENTER_BATCH_MISMATCH");
      }
    }

    ensureScopeValidator();
    if (batch.getTrainingProvider() != null && batch.getTrainingProvider().getId() != null) {
      scopeValidator.validateProviderScope(batch.getTrainingProvider().getId());
    }
    scopeValidator.validateTraineeScope(request.getTraineeId());

    if (request.getConsentTypeId() != null) {
      traineeConsentService.validateConsent(request.getTraineeId(), request.getConsentTypeId());
    } else {
      String consentCode = (request.getConsentCode() != null && !request.getConsentCode().isBlank())
          ? request.getConsentCode().trim()
          : "DATA_PROCESSING";
      traineeConsentService.validateConsent(request.getTraineeId(), consentCode);
    }

    Long enrollmentStatusId = request.getEnrollmentStatusId();
    if (enrollmentStatusId == null) {
      RefEnrollmentStatus defaultStatus = refEnrollmentStatusRepository.findByStatusCode("ENROLLED")
          .orElseThrow(() -> new ResourceNotFoundException("RefEnrollmentStatus", "statusCode"));
      enrollmentStatusId = defaultStatus.getId();
    }

    String enrollmentNumber = request.getEnrollmentNumber();
    if (enrollmentNumber == null || enrollmentNumber.isBlank()) {
      long suffix = Math.abs(System.currentTimeMillis() % 1000000L);
      enrollmentNumber = "ENR-" + batch.getId() + "-" + request.getTraineeId() + "-" + suffix;
      if (enrollmentNumber.length() > 32) {
        enrollmentNumber = enrollmentNumber.substring(0, 32);
      }
    } else {
      enrollmentNumber = enrollmentNumber.trim();
    }

    CreateTrainingEnrollmentRequest createReq = new CreateTrainingEnrollmentRequest();
    createReq.setTraineeId(request.getTraineeId());
    createReq.setBatchId(request.getBatchId());
    createReq.setEnrollmentDate(request.getEnrollmentDate());
    createReq.setEnrollmentNumber(enrollmentNumber);
    createReq.setEnrollmentStatusId(enrollmentStatusId);
    createReq.setProgramId(request.getProgramId() != null ? request.getProgramId() : (batch.getProgram() != null ? batch.getProgram().getId() : null));
    createReq.setCourseId(request.getCourseId() != null ? request.getCourseId() : (batch.getCourse() != null ? batch.getCourse().getId() : null));
    createReq.setProviderId(batch.getTrainingProvider() != null ? batch.getTrainingProvider().getId() : null);
    createReq.setCenterId(batch.getTrainingCenter() != null ? batch.getTrainingCenter().getId() : null);
    createReq.setStartDate(request.getStartDate() != null ? request.getStartDate() : batch.getStartDate());
    createReq.setExpectedCompletionDate(request.getExpectedCompletionDate() != null ? request.getExpectedCompletionDate() : batch.getEndDate());

    TrainingEnrollmentResponse enrollment = trainingEnrollmentService.createEnrollment(createReq);

    Long actorUserId = resolveCurrentActorUserId();
    String correlationId = CorrelationContextHolder.getCorrelationId();

    eventPublisher.publishEvent(new TraineeLifecycleEvent(
        UUID.randomUUID().toString(),
        "ENROLLMENT_CONFIRMED",
        request.getTraineeId(),
        actorUserId,
        correlationId,
        LocalDateTime.now(),
        batch.getTrainingCenter() != null ? batch.getTrainingCenter().getStateId() : null,
        batch.getTrainingCenter() != null ? batch.getTrainingCenter().getDistrictId() : null,
        batch.getTrainingProvider() != null ? batch.getTrainingProvider().getId() : null,
        batch.getTrainingCenter() != null ? batch.getTrainingCenter().getId() : null,
        batch.getId(),
        batch.getCourse() != null ? batch.getCourse().getId() : null,
        null,
        "ENROLLED",
        "{\"enrollmentId\":" + enrollment.getId() + ",\"enrollmentNumber\":\"" + enrollment.getEnrollmentNumber() + "\"}"
    ));

    return new TraineeEnrollmentOrchestrationResponse(enrollment, "VERIFIED", "COMPLETED", correlationId);
  }

  @Override
  @Transactional
  public RecordAssessmentAndIssueCertificateResponse recordAssessmentAndCertification(
      RecordAssessmentAndIssueCertificateRequest request) {
    RecordAssessmentAndIssueCertificateResponse response =
        trainingOutcomeWorkflowService.recordAssessmentResultAndIssueCertificate(request);

    if (response != null && response.certification() != null) {
      CertificationResponse cert = response.certification();
      Long actorUserId = resolveCurrentActorUserId();
      Long traineeId = cert.getTraineeId();
      if (traineeId == null && response.assessmentResult() != null) {
        traineeId = response.assessmentResult().getTraineeId();
      }
      eventPublisher.publishEvent(new TraineeLifecycleEvent(
          UUID.randomUUID().toString(),
          "CERTIFICATION_ISSUED",
          traineeId,
          actorUserId,
          CorrelationContextHolder.getCorrelationId(),
          LocalDateTime.now(),
          null,
          null,
          null,
          null,
          null,
          cert.getCourseId(),
          "IN_PROGRESS",
          "CERTIFIED",
          "{\"certificationId\":" + cert.getId() + ",\"certificateNumber\":\"" + cert.getCertificateNumber() + "\"}"
      ));
    }

    return response;
  }

  @Override
  @Transactional
  public PlacementToEmploymentTransitionResponse orchestratePlacementToEmployment(
      PlacementToEmploymentTransitionRequest request) {
    PlacementToEmploymentTransitionResponse response =
        placementEmploymentWorkflowService.transitionPlacementToEmployment(request);

    if (response != null && response.placement() != null && response.employmentRecord() != null) {
      Long actorUserId = resolveCurrentActorUserId();
      eventPublisher.publishEvent(new PlacementLifecycleEvent(
          UUID.randomUUID().toString(),
          "EMPLOYMENT_TRANSITION",
          response.placement().id(),
          response.placement().traineeId(),
          actorUserId,
          CorrelationContextHolder.getCorrelationId(),
          LocalDateTime.now(),
          response.employmentRecord().workStateId(),
          null,
          response.employmentRecord().employerId(),
          null,
          response.placement().placementStatusCode(),
          response.placement().joiningStatusCode(),
          response.placement().offeredSalary(),
          "{\"employmentRecordId\":" + response.employmentRecord().id() + "}"
      ));
    }

    return response;
  }

  @Override
  @Transactional
  public RecordVerificationVerdictResponse orchestrateEmploymentVerificationVerdict(
      RecordVerificationVerdictRequest request) {
    RecordVerificationVerdictResponse response =
        verificationWorkflowService.recordVerificationVerdictAndSynchronize(request);

    if (response != null && response.verification() != null) {
      Long actorUserId = resolveCurrentActorUserId();
      String vStatus = response.verification().recordVerificationStatusCode();

      eventPublisher.publishEvent(new EmploymentVerificationEvent(
          UUID.randomUUID().toString(),
          "VERIFICATION_VERDICT",
          response.verification().id(),
          response.verification().employmentRecordId(),
          response.verification().traineeId(),
          actorUserId,
          CorrelationContextHolder.getCorrelationId(),
          LocalDateTime.now(),
          response.verification().employerId(),
          null,
          response.verification().methodCode(),
          vStatus,
          null,
          null,
          "{\"verificationNumber\":\"" + response.verification().verificationNumber() + "\"}"
      ));

      if ("REJECTED".equalsIgnoreCase(vStatus) || "FAILED".equalsIgnoreCase(vStatus) || "UNVERIFIED".equalsIgnoreCase(vStatus)) {
        eventPublisher.publishEvent(new DataQualityAlertEvent(
            UUID.randomUUID().toString(),
            "ANOMALY_DETECTED",
            response.verification().id(),
            actorUserId,
            CorrelationContextHolder.getCorrelationId(),
            LocalDateTime.now(),
            "DQ-EMPLOYMENT-VERIFICATION-FAILED",
            "HIGH",
            "EmploymentVerification",
            response.verification().id(),
            null,
            null,
            "OPEN",
            "Employment verification rejected for employment record id=" + response.verification().employmentRecordId()
        ));
      }
    }

    return response;
  }

  @Override
  @Transactional
  public EmploymentExitAndUnemploymentResponse orchestrateEmploymentExitAndUnemployment(
      EmploymentExitAndUnemploymentRequest request) {
    EmploymentExitAndUnemploymentResponse response =
        employmentLifecycleWorkflowService.recordExitAndInitiateUnemployment(request);

    if (response != null && response.exitEvent() != null) {
      Long actorUserId = resolveCurrentActorUserId();
      eventPublisher.publishEvent(new TraineeLifecycleEvent(
          UUID.randomUUID().toString(),
          "EMPLOYMENT_EXIT",
          response.exitEvent().traineeId(),
          actorUserId,
          CorrelationContextHolder.getCorrelationId(),
          LocalDateTime.now(),
          null,
          null,
          null,
          null,
          null,
          null,
          "EMPLOYED",
          "UNEMPLOYED",
          "{\"employmentExitEventId\":" + response.exitEvent().id() + "}"
      ));
    }

    return response;
  }

  @Override
  @Transactional
  public RemediationTriggerResponse triggerSkillGapRemediation(
      RemediationTriggerRequest request) {
    if (request == null) {
      throw new BadRequestException("Request body cannot be null", "REQUEST_BODY_NULL");
    }
    if (request.getTraineeId() == null) {
      throw new BadRequestException("Trainee ID is required", "TRAINEE_ID_REQUIRED");
    }
    if (request.getTriggerSource() == null || request.getTriggerSource().isBlank()) {
      throw new BadRequestException("Trigger source is required", "TRIGGER_SOURCE_REQUIRED");
    }

    ensureScopeValidator();
    scopeValidator.validateTraineeScope(request.getTraineeId());

    if (!traineeRepository.existsById(request.getTraineeId())) {
      throw new ResourceNotFoundException("Trainee", "traineeId");
    }

    Long actorUserId = resolveCurrentActorUserId();
    String correlationId = CorrelationContextHolder.getCorrelationId();

    if (request.getAssessmentRequest() != null) {
      RecordSkillGapAssessmentWithGapsRequest assessmentReq = request.getAssessmentRequest();
      CreateSkillGapAssessmentRequest innerReq = assessmentReq.getAssessmentRequest();
      if (innerReq != null) {
        if (innerReq.getTraineeId() != null && !innerReq.getTraineeId().equals(request.getTraineeId())) {
          throw new BadRequestException(
              "Assessment request trainee ID does not match remediation target trainee",
              "TRAINEE_REMEDIATION_MISMATCH"
          );
        }
        if (innerReq.getTraineeId() == null) {
          innerReq.setTraineeId(request.getTraineeId());
        }
      }

      RecordSkillGapAssessmentWithGapsResponse assessmentResp =
          skillGapRemediationWorkflowService.recordAssessmentWithGapsAndRecommendations(assessmentReq);

      int gapCount = (assessmentResp != null && assessmentResp.skillGaps() != null) ? assessmentResp.skillGaps().size() : 0;
      Long assessmentId = (assessmentResp != null && assessmentResp.assessment() != null) ? assessmentResp.assessment().id() : null;

      eventPublisher.publishEvent(new TraineeLifecycleEvent(
          UUID.randomUUID().toString(),
          "REMEDIATION_EVALUATED",
          request.getTraineeId(),
          actorUserId,
          correlationId,
          LocalDateTime.now(),
          null,
          null,
          null,
          null,
          null,
          null,
          "UNEMPLOYED",
          "IN_REMEDIATION",
          "{\"assessmentId\":" + (assessmentId != null ? assessmentId : "null") + ",\"skillGapsCount\":" + gapCount + "}"
      ));

      return new RemediationTriggerResponse(
          request.getTraineeId(),
          request.getTriggerSource(),
          request.getReferenceEntityId(),
          "EVALUATED",
          assessmentResp,
          request.getRemarks(),
          correlationId
      );
    } else {
      eventPublisher.publishEvent(new CommunicationTriggerEvent(
          UUID.randomUUID().toString(),
          "REMEDIATION_ACTION_REQUIRED",
          request.getTraineeId(),
          actorUserId,
          correlationId,
          LocalDateTime.now(),
          "EMAIL",
          "trainee" + request.getTraineeId() + "@masked.domain",
          "SKILL_GAP_ASSESSMENT_PENDING",
          null,
          null,
          "MEDIUM",
          "Skill gap remediation pending assessment data for trainee id=" + request.getTraineeId()
      ));

      String remarks = (request.getRemarks() != null && !request.getRemarks().isBlank())
          ? request.getRemarks().trim()
          : "Trainee flagged for skill-gap evaluation following " + request.getTriggerSource() + "; awaiting assessment data";

      return new RemediationTriggerResponse(
          request.getTraineeId(),
          request.getTriggerSource(),
          request.getReferenceEntityId(),
          "PENDING_ASSESSMENT_DATA",
          null,
          remarks,
          correlationId
      );
    }
  }

  @Override
  public CandidateLifecycleSummaryResponse getCandidateLifecycleSummary(Long traineeId) {
    if (traineeId == null) {
      throw new BadRequestException("Trainee ID is required", "TRAINEE_ID_REQUIRED");
    }

    ensureScopeValidator();
    scopeValidator.validateTraineeScope(traineeId);

    if (!traineeRepository.existsById(traineeId)) {
      throw new ResourceNotFoundException("Trainee", "traineeId");
    }

    List<TraineeConsentResponse> consents = traineeConsentService.getConsentsByTraineeId(traineeId);
    List<TrainingEnrollmentResponse> enrollments = trainingEnrollmentService.getByTraineeId(traineeId);
    TraineePlacementEmploymentSummaryResponse placementEmploymentSummary =
        placementEmploymentWorkflowService.getTraineePlacementEmploymentSummary(traineeId);
    TraineeCareerTimelineResponse careerTimeline =
        employmentLifecycleWorkflowService.getTraineeCareerTimeline(traineeId);
    TraineeSkillGapProfileResponse skillGapProfile =
        skillGapRemediationWorkflowService.getTraineeSkillGapProfile(traineeId);

    return new CandidateLifecycleSummaryResponse(
        traineeId,
        consents,
        enrollments,
        placementEmploymentSummary,
        careerTimeline,
        skillGapProfile
    );
  }

  private void ensureScopeValidator() {
    if (this.scopeValidator == null) {
      throw new IllegalStateException("WorkflowSecurityScopeValidator is required for scope validation");
    }
  }

  private Long resolveCurrentActorUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
      return null;
    }
    Object principal = auth.getPrincipal();
    if (principal instanceof UserPrincipal up) {
      if (up.getId() != null) {
        return up.getId();
      }
      if (up.getUsername() != null && userRepository != null) {
        return userRepository.findByUsernameOrEmail(up.getUsername(), up.getUsername())
            .map(User::getId)
            .orElse(null);
      }
    } else if (principal instanceof org.springframework.security.core.userdetails.UserDetails ud && userRepository != null) {
      return userRepository.findByUsernameOrEmail(ud.getUsername(), ud.getUsername())
          .map(User::getId)
          .orElse(null);
    }
    return null;
  }
}
