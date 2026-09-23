package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.CreateSkillGapRecommendationRequest;
import in.gov.sih.sih26135.dto.request.CreateSkillGapRequest;
import in.gov.sih.sih26135.dto.request.CreateTrainingEnrollmentRequest;
import in.gov.sih.sih26135.dto.request.RecordSkillGapAssessmentWithGapsRequest;
import in.gov.sih.sih26135.dto.request.SkillGapRemediationEnrollmentRequest;
import in.gov.sih.sih26135.dto.response.RecordSkillGapAssessmentWithGapsResponse;
import in.gov.sih.sih26135.dto.response.SkillGapAssessmentResponse;
import in.gov.sih.sih26135.dto.response.SkillGapRecommendationResponse;
import in.gov.sih.sih26135.dto.response.SkillGapRemediationEnrollmentResponse;
import in.gov.sih.sih26135.dto.response.SkillGapResponse;
import in.gov.sih.sih26135.dto.response.TraineeSkillGapProfileResponse;
import in.gov.sih.sih26135.dto.response.TrainingEnrollmentResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.service.SkillGapAssessmentService;
import in.gov.sih.sih26135.service.SkillGapRecommendationService;
import in.gov.sih.sih26135.service.SkillGapRemediationWorkflowService;
import in.gov.sih.sih26135.service.SkillGapService;
import in.gov.sih.sih26135.service.TrainingEnrollmentService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SkillGapRemediationWorkflowServiceImpl implements SkillGapRemediationWorkflowService {

  private final SkillGapAssessmentService skillGapAssessmentService;
  private final SkillGapService skillGapService;
  private final SkillGapRecommendationService skillGapRecommendationService;
  private final TrainingEnrollmentService trainingEnrollmentService;

  public SkillGapRemediationWorkflowServiceImpl(
      SkillGapAssessmentService skillGapAssessmentService,
      SkillGapService skillGapService,
      SkillGapRecommendationService skillGapRecommendationService,
      TrainingEnrollmentService trainingEnrollmentService) {
    this.skillGapAssessmentService = skillGapAssessmentService;
    this.skillGapService = skillGapService;
    this.skillGapRecommendationService = skillGapRecommendationService;
    this.trainingEnrollmentService = trainingEnrollmentService;
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
        if (recReq.getSkillGapId() == null) {
          recReq.setSkillGapId(createdGaps.get(0).id());
        }
        createdRecs.add(skillGapRecommendationService.createRecommendation(recReq));
      }
    }

    return new RecordSkillGapAssessmentWithGapsResponse(assessment, createdGaps, createdRecs);
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

    SkillGapRecommendationResponse rec = skillGapRecommendationService.getRecommendationById(
        request.getRecommendationId());

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

    TrainingEnrollmentResponse createdEnrollment = trainingEnrollmentService.createEnrollment(enrolReq);

    if (request.getRecommendationStatusUpdate() != null) {
      rec = skillGapRecommendationService.updateRecommendation(rec.id(), request.getRecommendationStatusUpdate());
    }

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
}
