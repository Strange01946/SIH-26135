package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.request.CreateSurveyResponseRequest;
import in.gov.sih.sih26135.dto.response.SurveyResponseResponse;
import in.gov.sih.sih26135.entity.FollowupTask;
import in.gov.sih.sih26135.entity.SurveyResponse;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.entity.TrainingEnrollment;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class SurveyResponseMapper {

  public SurveyResponseResponse toResponse(SurveyResponse entity) {
    if (entity == null) {
      return null;
    }

    Long traineeId = null;
    String traineeReg = null;
    String traineeFirst = null;
    String traineeLast = null;
    if (entity.getTrainee() != null) {
      traineeId = entity.getTrainee().getId();
      traineeReg = entity.getTrainee().getRegistrationNumber();
      traineeFirst = entity.getTrainee().getFirstName();
      traineeLast = entity.getTrainee().getLastName();
    }

    Long enrollmentId = null;
    String enrollmentNumber = null;
    if (entity.getEnrollment() != null) {
      enrollmentId = entity.getEnrollment().getId();
      enrollmentNumber = entity.getEnrollment().getEnrollmentNumber();
    }

    Long followupTaskId = null;
    if (entity.getFollowupTask() != null) {
      followupTaskId = entity.getFollowupTask().getId();
    }

    return new SurveyResponseResponse(
        entity.getId(),
        entity.getSurveyId(),
        entity.getSurveyTemplateVersionId(),
        traineeId,
        traineeReg,
        traineeFirst,
        traineeLast,
        enrollmentId,
        enrollmentNumber,
        followupTaskId,
        entity.getAttemptNumber(),
        entity.getSurveyResponseStatusId(),
        entity.getStartedAt(),
        entity.getSubmittedAt(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }

  public SurveyResponse toEntity(
      CreateSurveyResponseRequest request,
      Trainee trainee,
      TrainingEnrollment enrollment,
      FollowupTask followupTask) {
    if (request == null) {
      return null;
    }

    SurveyResponse entity = new SurveyResponse();
    entity.setSurveyId(request.getSurveyId());
    entity.setSurveyTemplateVersionId(request.getSurveyTemplateVersionId());
    entity.setTrainee(trainee);
    entity.setEnrollment(enrollment);
    entity.setFollowupTask(followupTask);
    entity.setAttemptNumber(request.getAttemptNumber() != null ? request.getAttemptNumber() : 1);
    entity.setSurveyResponseStatusId(request.getSurveyResponseStatusId());
    entity.setStartedAt(request.getStartedAt() != null ? request.getStartedAt() : LocalDateTime.now());
    entity.setSubmittedAt(request.getSubmittedAt());
    return entity;
  }
}
