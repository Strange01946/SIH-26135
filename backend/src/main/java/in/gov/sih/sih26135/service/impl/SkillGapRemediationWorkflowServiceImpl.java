package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.CreateSkillGapRecommendationRequest;
import in.gov.sih.sih26135.dto.request.CreateSkillGapRequest;
import in.gov.sih.sih26135.dto.request.CreateTrainingEnrollmentRequest;
import in.gov.sih.sih26135.dto.request.RecordSkillGapAssessmentWithGapsRequest;
import in.gov.sih.sih26135.dto.request.SkillGapRemediationEnrollmentRequest;
import in.gov.sih.sih26135.dto.request.TraineeRecommendationDecisionRequest;
import in.gov.sih.sih26135.dto.response.RecordSkillGapAssessmentWithGapsResponse;
import in.gov.sih.sih26135.dto.response.SkillGapAssessmentResponse;
import in.gov.sih.sih26135.dto.response.SkillGapRecommendationResponse;
import in.gov.sih.sih26135.dto.response.SkillGapRemediationEnrollmentResponse;
import in.gov.sih.sih26135.dto.response.SkillGapResponse;
import in.gov.sih.sih26135.dto.response.TraineeSkillGapProfileResponse;
import in.gov.sih.sih26135.dto.response.TrainingEnrollmentResponse;
import in.gov.sih.sih26135.entity.RefSkillGapStatus;
import in.gov.sih.sih26135.entity.SkillGap;
import in.gov.sih.sih26135.entity.SkillGapRecommendation;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.InvalidCredentialsException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.SkillGapRecommendationMapper;
import in.gov.sih.sih26135.repository.CourseSkillRepository;
import in.gov.sih.sih26135.repository.RefSkillGapStatusRepository;
import in.gov.sih.sih26135.repository.SkillGapRecommendationRepository;
import in.gov.sih.sih26135.repository.SkillGapRepository;
import in.gov.sih.sih26135.repository.TraineeRepository;
import in.gov.sih.sih26135.security.UserPrincipal;
import in.gov.sih.sih26135.service.SkillGapAssessmentService;
import in.gov.sih.sih26135.service.SkillGapRecommendationService;
import in.gov.sih.sih26135.service.SkillGapRemediationWorkflowService;
import in.gov.sih.sih26135.service.SkillGapService;
import in.gov.sih.sih26135.service.TrainingEnrollmentService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SkillGapRemediationWorkflowServiceImpl implements SkillGapRemediationWorkflowService {

  private static final Logger log = LoggerFactory.getLogger(SkillGapRemediationWorkflowServiceImpl.class);

  private final SkillGapAssessmentService skillGapAssessmentService;
  private final SkillGapService skillGapService;
  private final SkillGapRecommendationService skillGapRecommendationService;
  private final TrainingEnrollmentService trainingEnrollmentService;
  private final CourseSkillRepository courseSkillRepository;
  private final SkillGapRepository skillGapRepository;
  private final SkillGapRecommendationRepository skillGapRecommendationRepository;
  private final RefSkillGapStatusRepository refSkillGapStatusRepository;
  private final TraineeRepository traineeRepository;
  private final SkillGapRecommendationMapper skillGapRecommendationMapper;

  public SkillGapRemediationWorkflowServiceImpl(
      SkillGapAssessmentService skillGapAssessmentService,
      SkillGapService skillGapService,
      SkillGapRecommendationService skillGapRecommendationService,
      TrainingEnrollmentService trainingEnrollmentService,
      CourseSkillRepository courseSkillRepository,
      SkillGapRepository skillGapRepository,
      SkillGapRecommendationRepository skillGapRecommendationRepository,
      RefSkillGapStatusRepository refSkillGapStatusRepository,
      TraineeRepository traineeRepository,
      SkillGapRecommendationMapper skillGapRecommendationMapper) {
    this.skillGapAssessmentService = skillGapAssessmentService;
    this.skillGapService = skillGapService;
    this.skillGapRecommendationService = skillGapRecommendationService;
    this.trainingEnrollmentService = trainingEnrollmentService;
    this.courseSkillRepository = courseSkillRepository;
    this.skillGapRepository = skillGapRepository;
    this.skillGapRecommendationRepository = skillGapRecommendationRepository;
    this.refSkillGapStatusRepository = refSkillGapStatusRepository;
    this.traineeRepository = traineeRepository;
    this.skillGapRecommendationMapper = skillGapRecommendationMapper;
  }

  @Override
  @Transactional
  public RecordSkillGapAssessmentWithGapsResponse recordAssessmentWithGapsAndRecommendations(
      RecordSkillGapAssessmentWithGapsRequest request) {
    if (request == null) {
      throw new BadRequestException("Request body cannot be null", "REQUEST_BODY_NULL");
    }
    if (request.getAssessmentRequest() == null) {
      throw new BadRequestException("Assessment request is required", "ASSESSMENT_REQUEST_REQUIRED");
    }
    if (request.getGapRequests() == null || request.getGapRequests().isEmpty()) {
      throw new BadRequestException("At least one skill gap is required", "SKILL_GAPS_REQUIRED");
    }

    SkillGapAssessmentResponse assessment = skillGapAssessmentService.createAssessment(request.getAssessmentRequest());

    List<SkillGapResponse> createdGaps = new ArrayList<>();
    for (CreateSkillGapRequest gapReq : request.getGapRequests()) {
      gapReq.setSkillGapAssessmentId(assessment.id());
      if (gapReq.getTraineeId() == null) {
        gapReq.setTraineeId(assessment.traineeId());
      } else if (!gapReq.getTraineeId().equals(assessment.traineeId())) {
        throw new BadRequestException(
            "Skill gap trainee does not match assessment trainee",
            "SKILL_GAP_TRAINEE_MISMATCH"
        );
      }
      createdGaps.add(skillGapService.createSkillGap(gapReq));
    }

    List<SkillGapRecommendationResponse> createdRecs = new ArrayList<>();
    if (request.getRecommendationRequests() != null && !request.getRecommendationRequests().isEmpty()) {
      for (CreateSkillGapRecommendationRequest recReq : request.getRecommendationRequests()) {
        if (recReq == null) {
          throw new BadRequestException("Recommendation request cannot be null", "RECOMMENDATION_REQUEST_NULL");
        }

        Long targetGapId = recReq.getSkillGapId();

        if (targetGapId == null) {
          if (createdGaps.size() == 1) {
            targetGapId = createdGaps.get(0).id();
            recReq.setSkillGapId(targetGapId);
          } else {
            throw new BadRequestException(
                "Skill gap ID is required for recommendation when multiple skill gaps are created",
                "SKILL_GAP_ID_REQUIRED"
            );
          }
        }

        final Long resolvedGapId = targetGapId;
        SkillGapResponse targetGap = createdGaps.stream()
            .filter(g -> g.id().equals(resolvedGapId))
            .findFirst()
            .orElse(null);

        if (targetGap == null) {
          targetGap = skillGapService.getSkillGapById(resolvedGapId);
        }

        if (targetGap == null) {
          throw new ResourceNotFoundException("SkillGap", "skillGapId");
        }

        if (targetGap.traineeId() == null || !targetGap.traineeId().equals(assessment.traineeId())) {
          throw new BadRequestException(
              "Skill gap does not belong to assessment trainee",
              "SKILL_GAP_TRAINEE_MISMATCH"
          );
        }

        if (targetGap.skillGapAssessmentId() == null || !targetGap.skillGapAssessmentId().equals(assessment.id())) {
          throw new BadRequestException(
              "Skill gap does not belong to the current assessment",
              "SKILL_GAP_ASSESSMENT_MISMATCH"
          );
        }

        // Validate Course-Skill relevance when a course is explicitly recommended - FAIL CLOSED
        if (recReq.getRecommendedCourseId() != null) {
          if (targetGap.skillId() == null) {
            throw new BadRequestException(
                "Skill gap does not specify an identified skill",
                "SKILL_REQUIRED"
            );
          }
          if (courseSkillRepository == null) {
            throw new IllegalStateException("CourseSkillRepository is required for course-skill validation");
          }
          if (!courseSkillRepository.existsByCourseIdAndSkillId(
              recReq.getRecommendedCourseId(), targetGap.skillId())) {
            throw new BadRequestException(
                "Recommended course does not teach the identified gap skill",
                "COURSE_SKILL_MISMATCH"
            );
          }
        }

        createdRecs.add(skillGapRecommendationService.createRecommendation(recReq));
      }
    }

    return new RecordSkillGapAssessmentWithGapsResponse(assessment, createdGaps, createdRecs);
  }

  @Override
  @Transactional
  public SkillGapRecommendationResponse acceptRecommendation(
      Long recommendationId, TraineeRecommendationDecisionRequest request) {
    if (recommendationId == null) {
      throw new BadRequestException("Recommendation ID is required", "RECOMMENDATION_ID_REQUIRED");
    }
    if (skillGapRecommendationRepository == null) {
      throw new IllegalStateException("SkillGapRecommendationRepository is required");
    }

    // 1. Resolve trusted authenticated identity - FAIL CLOSED (never fallback to request body)
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    Long authUserId = resolveAuthenticatedUserId(authentication);
    boolean isAdmin = isAdministrativeCaller(authentication);

    SkillGapRecommendation rec = skillGapRecommendationRepository.findById(recommendationId)
        .orElseThrow(() -> new ResourceNotFoundException("SkillGapRecommendation", "recommendationId"));

    if (rec.getSkillGap() == null || rec.getSkillGap().getTrainee() == null) {
      throw new BadRequestException(
          "Recommendation is not linked to a valid skill gap trainee",
          "TRAINEE_REQUIRED"
      );
    }

    Long recTraineeId = rec.getSkillGap().getTrainee().getId();

    // 2. Trainee ownership verification - FAIL CLOSED
    if (traineeRepository == null) {
      throw new IllegalStateException("TraineeRepository is required for trainee ownership verification");
    }
    Optional<Trainee> authTrainee = traineeRepository.findByUserId(authUserId);
    if (authTrainee.isPresent()) {
      if (!authTrainee.get().getId().equals(recTraineeId)) {
        throw new BadRequestException(
            "Authenticated trainee does not match recommendation trainee",
            "TRAINEE_MISMATCH"
        );
      }
    } else if (!isAdmin) {
      throw new BadRequestException(
          "User is not authorized to act on this trainee recommendation",
          "TRAINEE_MISMATCH"
      );
    }

    // 3. Client-supplied trainee check if provided
    if (request != null && request.getTraineeId() != null) {
      if (!request.getTraineeId().equals(recTraineeId)) {
        throw new BadRequestException(
            "Trainee ID does not match recommendation trainee",
            "TRAINEE_MISMATCH"
        );
      }
    }

    // Check if recommendation is already accepted
    if (Boolean.TRUE.equals(rec.getIsAcceptedFlag())) {
      throw new BadRequestException(
          "Recommendation has already been accepted",
          "ALREADY_ACCEPTED"
      );
    }

    // Check if linked skill gap is already resolved or closed - FAIL CLOSED
    if (rec.getSkillGap().getSkillGapStatus() == null || rec.getSkillGap().getSkillGapStatus().getStatusCode() == null) {
      throw new BadRequestException(
          "Skill gap status is missing or invalid",
          "INVALID_GAP_STATUS"
      );
    }

    String statusCode = rec.getSkillGap().getSkillGapStatus().getStatusCode();
    if ("RESOLVED".equals(statusCode) || "CLOSED_UNRESOLVED".equals(statusCode)
        || "SUPERSEDED".equals(statusCode)) {
      throw new BadRequestException(
          "Cannot accept recommendation for a closed or resolved skill gap",
          "INVALID_GAP_STATUS"
      );
    }

    rec.setIsAcceptedFlag(true);
    rec.setAcceptedOn(LocalDate.now());
    if (request != null && request.getRemarks() != null && !request.getRemarks().isBlank()) {
      rec.setRemarks(request.getRemarks().trim());
    }

    // Preserve original creator if already present; only populate from authenticated user if null
    if (rec.getCreatedByUserId() == null) {
      rec.setCreatedByUserId(authUserId);
    }
    rec.setUpdatedAt(LocalDateTime.now());
    log.info("Skill gap recommendation id={} accepted by user id={}", recommendationId, authUserId);

    SkillGapRecommendation saved = skillGapRecommendationRepository.save(rec);
    return skillGapRecommendationMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public SkillGapRecommendationResponse rejectRecommendation(
      Long recommendationId, TraineeRecommendationDecisionRequest request) {
    if (recommendationId == null) {
      throw new BadRequestException("Recommendation ID is required", "RECOMMENDATION_ID_REQUIRED");
    }

    // Non-blank remarks explaining rejection are mandatory
    if (request == null || request.getRemarks() == null || request.getRemarks().trim().isEmpty()) {
      throw new BadRequestException(
          "Rejection reason remarks are required",
          "REJECTION_REMARKS_REQUIRED"
      );
    }

    if (skillGapRecommendationRepository == null) {
      throw new IllegalStateException("SkillGapRecommendationRepository is required");
    }

    // 1. Resolve trusted authenticated identity - FAIL CLOSED (never fallback to request body)
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    Long authUserId = resolveAuthenticatedUserId(authentication);
    boolean isAdmin = isAdministrativeCaller(authentication);

    SkillGapRecommendation rec = skillGapRecommendationRepository.findById(recommendationId)
        .orElseThrow(() -> new ResourceNotFoundException("SkillGapRecommendation", "recommendationId"));

    if (rec.getSkillGap() == null || rec.getSkillGap().getTrainee() == null) {
      throw new BadRequestException(
          "Recommendation is not linked to a valid skill gap trainee",
          "TRAINEE_REQUIRED"
      );
    }

    Long recTraineeId = rec.getSkillGap().getTrainee().getId();

    // 2. Trainee ownership verification - FAIL CLOSED
    if (traineeRepository == null) {
      throw new IllegalStateException("TraineeRepository is required for trainee ownership verification");
    }
    Optional<Trainee> authTrainee = traineeRepository.findByUserId(authUserId);
    if (authTrainee.isPresent()) {
      if (!authTrainee.get().getId().equals(recTraineeId)) {
        throw new BadRequestException(
            "Authenticated trainee does not match recommendation trainee",
            "TRAINEE_MISMATCH"
        );
      }
    } else if (!isAdmin) {
      throw new BadRequestException(
          "User is not authorized to act on this trainee recommendation",
          "TRAINEE_MISMATCH"
      );
    }

    // 3. Client-supplied trainee check if provided
    if (request.getTraineeId() != null) {
      if (!request.getTraineeId().equals(recTraineeId)) {
        throw new BadRequestException(
            "Trainee ID does not match recommendation trainee",
            "TRAINEE_MISMATCH"
        );
      }
    }

    // Cannot reject a recommendation that is already accepted - FAIL CLOSED
    if (Boolean.TRUE.equals(rec.getIsAcceptedFlag())) {
      throw new BadRequestException(
          "Cannot reject an already accepted recommendation",
          "INVALID_STATE_TRANSITION"
      );
    }

    // Prevent rejecting a recommendation whose linked gap is already in active remediation - FAIL CLOSED
    if (rec.getSkillGap().getSkillGapStatus() == null || rec.getSkillGap().getSkillGapStatus().getStatusCode() == null) {
      throw new BadRequestException(
          "Skill gap status is missing or invalid",
          "INVALID_GAP_STATUS"
      );
    }

    String statusCode = rec.getSkillGap().getSkillGapStatus().getStatusCode();
    if ("IN_REMEDIATION".equals(statusCode)) {
      throw new BadRequestException(
          "Cannot reject recommendation that is already in active remediation",
          "INVALID_STATE_TRANSITION"
      );
    }

    rec.setIsAcceptedFlag(false);
    rec.setAcceptedOn(null);
    rec.setRemarks(request.getRemarks().trim());

    // Preserve original creator if already present; only populate from authenticated user if null
    if (rec.getCreatedByUserId() == null) {
      rec.setCreatedByUserId(authUserId);
    }
    rec.setUpdatedAt(LocalDateTime.now());
    log.info("Skill gap recommendation id={} rejected by user id={}", recommendationId, authUserId);

    SkillGapRecommendation saved = skillGapRecommendationRepository.save(rec);
    return skillGapRecommendationMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public SkillGapRemediationEnrollmentResponse enrollTraineeInRemediationCourse(
      SkillGapRemediationEnrollmentRequest request) {
    if (request == null) {
      throw new BadRequestException("Request body cannot be null", "REQUEST_BODY_NULL");
    }
    if (request.getRecommendationId() == null) {
      throw new BadRequestException("Recommendation ID is required", "RECOMMENDATION_ID_REQUIRED");
    }
    if (request.getEnrollmentRequest() == null) {
      throw new BadRequestException("Enrollment request is required", "ENROLLMENT_REQUEST_REQUIRED");
    }

    // 1. Resolve trusted authenticated identity - FAIL CLOSED
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    Long authUserId = resolveAuthenticatedUserId(authentication);
    boolean isAdmin = isAdministrativeCaller(authentication);

    SkillGapRecommendationResponse rec = skillGapRecommendationService.getRecommendationById(
        request.getRecommendationId());
    if (rec == null) {
      throw new ResourceNotFoundException("SkillGapRecommendation", "recommendationId");
    }

    // 2. Strict Persisted Acceptance Requirement - NO INLINE ACCEPTANCE BYPASS
    if (!Boolean.TRUE.equals(rec.isAcceptedFlag())) {
      throw new BadRequestException(
          "Remediation course enrollment requires an accepted recommendation",
          "RECOMMENDATION_NOT_ACCEPTED"
      );
    }

    if (rec.recommendedCourseId() == null) {
      throw new BadRequestException(
          "Skill gap recommendation does not specify a recommended course",
          "RECOMMENDED_COURSE_MISSING"
      );
    }

    CreateTrainingEnrollmentRequest enrolReq = request.getEnrollmentRequest();
    if (enrolReq.getCourseId() == null) {
      enrolReq.setCourseId(rec.recommendedCourseId());
    } else if (!enrolReq.getCourseId().equals(rec.recommendedCourseId())) {
      throw new BadRequestException(
          "Enrollment course does not match recommended remediation course",
          "REMEDIATION_COURSE_MISMATCH"
      );
    }

    // 3. Fetch the linked skill gap entity - FAIL CLOSED
    if (rec.skillGapId() == null) {
      throw new BadRequestException(
          "Skill gap recommendation is not linked to a skill gap",
          "SKILL_GAP_ID_REQUIRED"
      );
    }
    if (skillGapRepository == null) {
      throw new IllegalStateException("SkillGapRepository is required for remediation workflow");
    }

    SkillGap gapEntity = skillGapRepository.findById(rec.skillGapId())
        .orElseThrow(() -> new ResourceNotFoundException("SkillGap", "skillGapId"));

    // 4. Trainee consistency verification - FAIL CLOSED
    if (gapEntity.getTrainee() == null || gapEntity.getTrainee().getId() == null) {
      throw new BadRequestException(
          "Skill gap does not have an associated trainee",
          "TRAINEE_REQUIRED"
      );
    }

    Long gapTraineeId = gapEntity.getTrainee().getId();
    if (enrolReq.getTraineeId() == null) {
      enrolReq.setTraineeId(gapTraineeId);
    } else if (!enrolReq.getTraineeId().equals(gapTraineeId)) {
      throw new BadRequestException(
          "Enrollment trainee does not match recommendation trainee",
          "TRAINEE_MISMATCH"
      );
    }

    // Server-side authenticated trainee ownership verification - FAIL CLOSED
    if (traineeRepository == null) {
      throw new IllegalStateException("TraineeRepository is required for trainee ownership verification");
    }
    Optional<Trainee> authTrainee = traineeRepository.findByUserId(authUserId);
    if (authTrainee.isPresent()) {
      if (!authTrainee.get().getId().equals(gapTraineeId)) {
        throw new BadRequestException(
            "Authenticated trainee does not match recommendation trainee",
            "TRAINEE_MISMATCH"
        );
      }
    } else if (!isAdmin) {
      throw new BadRequestException(
          "User is not authorized to act on this trainee enrollment",
          "TRAINEE_MISMATCH"
      );
    }

    // 5. Course-to-Skill relevance check - FAIL CLOSED
    if (gapEntity.getSkill() == null || gapEntity.getSkill().getId() == null) {
      throw new BadRequestException(
          "Skill gap does not specify an identified skill",
          "SKILL_REQUIRED"
      );
    }
    if (courseSkillRepository == null) {
      throw new IllegalStateException("CourseSkillRepository is required for course-skill validation");
    }
    if (!courseSkillRepository.existsByCourseIdAndSkillId(
        enrolReq.getCourseId(), gapEntity.getSkill().getId())) {
      throw new BadRequestException(
          "Course does not teach the skill associated with this recommendation",
          "COURSE_SKILL_MISMATCH"
      );
    }

    // 6. Active state and non-remediable status prevention - FAIL CLOSED
    if (!Boolean.TRUE.equals(gapEntity.getIsCurrent())) {
      throw new BadRequestException(
          "Cannot enroll in remediation for an inactive skill gap",
          "SKILL_GAP_NOT_ACTIVE"
      );
    }

    if (gapEntity.getSkillGapStatus() == null || gapEntity.getSkillGapStatus().getStatusCode() == null) {
      throw new BadRequestException(
          "Skill gap status is missing or invalid",
          "INVALID_GAP_STATUS"
      );
    }

    String currentStatusCode = gapEntity.getSkillGapStatus().getStatusCode();
    if ("RESOLVED".equals(currentStatusCode)
        || "CLOSED_UNRESOLVED".equals(currentStatusCode)
        || "SUPERSEDED".equals(currentStatusCode)) {
      throw new BadRequestException(
          "Cannot enroll in remediation for a closed, resolved, or superseded skill gap",
          "SKILL_GAP_NOT_ACTIVE"
      );
    }

    if ("IN_REMEDIATION".equals(currentStatusCode)) {
      throw new BadRequestException(
          "Skill gap is already in active remediation",
          "DUPLICATE_REMEDIATION_ENROLLMENT"
      );
    }

    if (!"OPEN".equals(currentStatusCode)) {
      throw new BadRequestException(
          "Only OPEN skill gaps can be transitioned to remediation",
          "INVALID_GAP_STATUS"
      );
    }

    // 7. Resolve IN_REMEDIATION status reference - FAIL CLOSED
    if (refSkillGapStatusRepository == null) {
      throw new IllegalStateException("RefSkillGapStatusRepository is required for status transition");
    }
    RefSkillGapStatus inRemediationStatus = refSkillGapStatusRepository
        .findByStatusCode("IN_REMEDIATION")
        .orElseThrow(() -> new ResourceNotFoundException("RefSkillGapStatus", "statusCode"));

    // 8. UPFRONT VALIDATION OF RECOMMENDATION STATUS UPDATE BEFORE ANY SIDE EFFECTS
    if (request.getRecommendationStatusUpdate() != null) {
      if (request.getRecommendationStatusUpdate().getIsAcceptedFlag() != null) {
        throw new BadRequestException(
            "Recommendation acceptance status cannot be mutated during remediation enrollment",
            "INVALID_STATE_TRANSITION"
        );
      }
    }

    // 9. Create the training enrollment
    TrainingEnrollmentResponse createdEnrollment = trainingEnrollmentService.createEnrollment(enrolReq);

    // 10. Transition linked SkillGap to IN_REMEDIATION status in same transaction
    gapEntity.setSkillGapStatus(inRemediationStatus);
    gapEntity.setUpdatedAt(LocalDateTime.now());
    skillGapRepository.save(gapEntity);

    // 11. Apply non-acceptance recommendation metadata updates if supplied
    if (request.getRecommendationStatusUpdate() != null) {
      rec = skillGapRecommendationService.updateRecommendation(rec.id(), request.getRecommendationStatusUpdate());
    }

    log.info("Enrolled trainee id={} in remediation course id={} for skill gap id={}",
        gapTraineeId, enrolReq.getCourseId(), gapEntity.getId());
    return new SkillGapRemediationEnrollmentResponse(rec, createdEnrollment);
  }

  @Override
  public TraineeSkillGapProfileResponse getTraineeSkillGapProfile(Long traineeId) {
    if (traineeId == null) {
      throw new BadRequestException("Trainee ID is required", "TRAINEE_ID_REQUIRED");
    }

    List<SkillGapAssessmentResponse> assessments = skillGapAssessmentService.getAssessmentsByTrainee(traineeId);
    List<SkillGapResponse> currentGaps = skillGapService.getCurrentSkillGapsByTrainee(traineeId);

    List<SkillGapRecommendationResponse> recs = new ArrayList<>();
    for (SkillGapResponse gap : currentGaps) {
      recs.addAll(skillGapRecommendationService.getRecommendationsBySkillGap(gap.id()));
    }

    return new TraineeSkillGapProfileResponse(traineeId, assessments, currentGaps, recs);
  }

  private Long resolveAuthenticatedUserId(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
      throw new InvalidCredentialsException("User is not authenticated");
    }
    Object principal = authentication.getPrincipal();
    if (principal instanceof UserPrincipal up && up.getId() != null) {
      return up.getId();
    }
    throw new InvalidCredentialsException("Unable to resolve authenticated user identity");
  }

  private boolean isAdministrativeCaller(Authentication authentication) {
    if (authentication == null || authentication.getAuthorities() == null) {
      return false;
    }
    return authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch(auth -> "assessment.manage".equals(auth)
            || "enrollment.manage".equals(auth)
            || "admin".equalsIgnoreCase(auth)
            || "ROLE_ADMIN".equalsIgnoreCase(auth));
  }
}
