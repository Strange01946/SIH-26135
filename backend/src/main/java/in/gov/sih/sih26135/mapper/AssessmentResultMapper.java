package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.request.CreateAssessmentResultRequest;
import in.gov.sih.sih26135.dto.response.AssessmentResultResponse;
import in.gov.sih.sih26135.entity.AssessmentResult;
import in.gov.sih.sih26135.entity.RefAssessmentOutcome;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.entity.TraineeAssessment;
import org.springframework.stereotype.Component;

@Component
public class AssessmentResultMapper {

  public AssessmentResultResponse toResponse(AssessmentResult entity) {
    if (entity == null) {
      return null;
    }

    Long traineeAssessmentId = null;
    if (entity.getTraineeAssessment() != null) {
      traineeAssessmentId = entity.getTraineeAssessment().getId();
    }

    Long traineeId = null;
    String traineeReg = null;
    String traineeFullName = null;
    if (entity.getTrainee() != null) {
      traineeId = entity.getTrainee().getId();
      traineeReg = entity.getTrainee().getRegistrationNumber();
      String first = entity.getTrainee().getFirstName() != null ? entity.getTrainee().getFirstName() : "";
      String last = entity.getTrainee().getLastName() != null ? entity.getTrainee().getLastName() : "";
      traineeFullName = (first + " " + last).trim();
      if (traineeFullName.isEmpty()) {
        traineeFullName = null;
      }
    }

    Long outcomeId = null;
    String outcomeCode = null;
    String outcomeName = null;
    Boolean isPassFlag = null;
    if (entity.getAssessmentOutcome() != null) {
      outcomeId = entity.getAssessmentOutcome().getId();
      outcomeCode = entity.getAssessmentOutcome().getOutcomeCode();
      outcomeName = entity.getAssessmentOutcome().getOutcomeName();
      isPassFlag = entity.getAssessmentOutcome().getIsPassFlag();
    }

    return new AssessmentResultResponse(
        entity.getId(),
        traineeAssessmentId,
        traineeId,
        traineeReg,
        traineeFullName,
        entity.getMaximumScore(),
        entity.getObtainedScore(),
        entity.getScorePercentage(),
        outcomeId,
        outcomeCode,
        outcomeName,
        isPassFlag,
        entity.getResultDeclaredAt(),
        entity.getRemarks(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }

  public AssessmentResult toEntity(
      CreateAssessmentResultRequest request,
      TraineeAssessment traineeAssessment,
      Trainee trainee,
      RefAssessmentOutcome outcome) {
    if (request == null) {
      return null;
    }

    AssessmentResult entity = new AssessmentResult();
    entity.setTraineeAssessment(traineeAssessment);
    entity.setTrainee(trainee);
    entity.setMaximumScore(request.getMaximumScore());
    entity.setObtainedScore(request.getObtainedScore());
    entity.setScorePercentage(request.getScorePercentage());
    entity.setAssessmentOutcome(outcome);
    entity.setResultDeclaredAt(request.getResultDeclaredAt());
    entity.setRemarks(request.getRemarks());
    return entity;
  }
}
