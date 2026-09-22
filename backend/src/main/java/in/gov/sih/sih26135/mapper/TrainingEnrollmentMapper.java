package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.request.CreateTrainingEnrollmentRequest;
import in.gov.sih.sih26135.dto.response.TrainingEnrollmentResponse;
import in.gov.sih.sih26135.entity.Course;
import in.gov.sih.sih26135.entity.Program;
import in.gov.sih.sih26135.entity.RefEnrollmentStatus;
import in.gov.sih.sih26135.entity.RefTrainingDropoutReason;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.entity.TrainingBatch;
import in.gov.sih.sih26135.entity.TrainingCenter;
import in.gov.sih.sih26135.entity.TrainingEnrollment;
import in.gov.sih.sih26135.entity.TrainingProvider;
import org.springframework.stereotype.Component;

@Component
public class TrainingEnrollmentMapper {

  public TrainingEnrollmentResponse toResponse(TrainingEnrollment entity) {
    if (entity == null) {
      return null;
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

    Long programId = null;
    String programCode = null;
    String programName = null;
    if (entity.getProgram() != null) {
      programId = entity.getProgram().getId();
      programCode = entity.getProgram().getProgramCode();
      programName = entity.getProgram().getProgramName();
    }

    Long courseId = null;
    String courseCode = null;
    String courseName = null;
    if (entity.getCourse() != null) {
      courseId = entity.getCourse().getId();
      courseCode = entity.getCourse().getCourseCode();
      courseName = entity.getCourse().getCourseName();
    }

    Long providerId = null;
    String providerCode = null;
    String providerName = null;
    if (entity.getTrainingProvider() != null) {
      providerId = entity.getTrainingProvider().getId();
      providerCode = entity.getTrainingProvider().getProviderCode();
      providerName = entity.getTrainingProvider().getProviderName();
    }

    Long centerId = null;
    String centerCode = null;
    String centerName = null;
    if (entity.getTrainingCenter() != null) {
      centerId = entity.getTrainingCenter().getId();
      centerCode = entity.getTrainingCenter().getCenterCode();
      centerName = entity.getTrainingCenter().getCenterName();
    }

    Long batchId = null;
    String batchCode = null;
    if (entity.getTrainingBatch() != null) {
      batchId = entity.getTrainingBatch().getId();
      batchCode = entity.getTrainingBatch().getBatchCode();
    }

    Long statusId = null;
    String statusCode = null;
    String statusName = null;
    Boolean isTerminal = null;
    Boolean isCompletedFlag = null;
    if (entity.getEnrollmentStatus() != null) {
      statusId = entity.getEnrollmentStatus().getId();
      statusCode = entity.getEnrollmentStatus().getStatusCode();
      statusName = entity.getEnrollmentStatus().getStatusName();
      isTerminal = entity.getEnrollmentStatus().getIsTerminal();
      isCompletedFlag = entity.getEnrollmentStatus().getIsCompletedFlag();
    }

    Long dropoutReasonId = null;
    String dropoutReasonCode = null;
    String dropoutReasonName = null;
    if (entity.getDropoutReason() != null) {
      dropoutReasonId = entity.getDropoutReason().getId();
      dropoutReasonCode = entity.getDropoutReason().getReasonCode();
      dropoutReasonName = entity.getDropoutReason().getReasonName();
    }

    return new TrainingEnrollmentResponse(
        entity.getId(),
        entity.getEnrollmentNumber(),
        traineeId,
        traineeReg,
        traineeFullName,
        programId,
        programCode,
        programName,
        courseId,
        courseCode,
        courseName,
        providerId,
        providerCode,
        providerName,
        centerId,
        centerCode,
        centerName,
        batchId,
        batchCode,
        entity.getEnrollmentDate(),
        entity.getStartDate(),
        entity.getExpectedCompletionDate(),
        entity.getActualCompletionDate(),
        statusId,
        statusCode,
        statusName,
        isTerminal,
        isCompletedFlag,
        dropoutReasonId,
        dropoutReasonCode,
        dropoutReasonName,
        entity.getDropoutRemarks(),
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        entity.getDeletedAt()
    );
  }

  public TrainingEnrollment toEntity(
      CreateTrainingEnrollmentRequest request,
      Trainee trainee,
      Program program,
      Course course,
      TrainingProvider provider,
      TrainingCenter center,
      TrainingBatch batch,
      RefEnrollmentStatus status,
      RefTrainingDropoutReason dropoutReason) {
    if (request == null) {
      return null;
    }

    TrainingEnrollment entity = new TrainingEnrollment();
    entity.setEnrollmentNumber(request.getEnrollmentNumber());
    entity.setTrainee(trainee);
    entity.setProgram(program);
    entity.setCourse(course);
    entity.setTrainingProvider(provider);
    entity.setTrainingCenter(center);
    entity.setTrainingBatch(batch);
    entity.setEnrollmentDate(request.getEnrollmentDate());
    entity.setStartDate(request.getStartDate());
    entity.setExpectedCompletionDate(request.getExpectedCompletionDate());
    entity.setActualCompletionDate(request.getActualCompletionDate());
    entity.setEnrollmentStatus(status);
    entity.setDropoutReason(dropoutReason);
    entity.setDropoutRemarks(request.getDropoutRemarks());
    return entity;
  }
}
