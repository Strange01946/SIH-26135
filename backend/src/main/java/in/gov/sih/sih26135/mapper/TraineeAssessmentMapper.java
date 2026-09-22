package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.request.CreateTraineeAssessmentRequest;
import in.gov.sih.sih26135.dto.response.TraineeAssessmentResponse;
import in.gov.sih.sih26135.entity.Assessment;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.entity.TraineeAssessment;
import in.gov.sih.sih26135.entity.TrainingEnrollment;
import org.springframework.stereotype.Component;

@Component
public class TraineeAssessmentMapper {

  public TraineeAssessmentResponse toResponse(TraineeAssessment entity) {
    if (entity == null) {
      return null;
    }

    Long assessmentId = null;
    String assessmentCode = null;
    String assessmentName = null;
    if (entity.getAssessment() != null) {
      assessmentId = entity.getAssessment().getId();
      assessmentCode = entity.getAssessment().getAssessmentCode();
      assessmentName = entity.getAssessment().getAssessmentName();
    }

    Long enrollmentId = null;
    String enrollmentNumber = null;
    if (entity.getTrainingEnrollment() != null) {
      enrollmentId = entity.getTrainingEnrollment().getId();
      enrollmentNumber = entity.getTrainingEnrollment().getEnrollmentNumber();
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

    return new TraineeAssessmentResponse(
        entity.getId(),
        assessmentId,
        assessmentCode,
        assessmentName,
        enrollmentId,
        enrollmentNumber,
        traineeId,
        traineeReg,
        traineeFullName,
        entity.getAttemptNumber(),
        entity.getAppearedFlag(),
        entity.getAssessmentDate(),
        entity.getEvaluatorUserId(),
        entity.getEvaluatorName(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }

  public TraineeAssessment toEntity(
      CreateTraineeAssessmentRequest request,
      Assessment assessment,
      TrainingEnrollment enrollment,
      Trainee trainee) {
    if (request == null) {
      return null;
    }

    TraineeAssessment entity = new TraineeAssessment();
    entity.setAssessment(assessment);
    entity.setTrainingEnrollment(enrollment);
    entity.setTrainee(trainee);
    entity.setAttemptNumber(request.getAttemptNumber() != null ? request.getAttemptNumber() : 1);
    entity.setAppearedFlag(request.getAppearedFlag() != null ? request.getAppearedFlag() : true);
    entity.setAssessmentDate(request.getAssessmentDate());
    entity.setEvaluatorUserId(request.getEvaluatorUserId());
    entity.setEvaluatorName(request.getEvaluatorName());
    return entity;
  }
}
