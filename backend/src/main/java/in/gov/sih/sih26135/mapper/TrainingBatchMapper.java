package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.request.CreateTrainingBatchRequest;
import in.gov.sih.sih26135.dto.response.TrainingBatchResponse;
import in.gov.sih.sih26135.entity.Course;
import in.gov.sih.sih26135.entity.Program;
import in.gov.sih.sih26135.entity.RefBatchStatus;
import in.gov.sih.sih26135.entity.TrainingBatch;
import in.gov.sih.sih26135.entity.TrainingCenter;
import in.gov.sih.sih26135.entity.TrainingProvider;
import org.springframework.stereotype.Component;

@Component
public class TrainingBatchMapper {

  public TrainingBatchResponse toResponse(TrainingBatch entity) {
    if (entity == null) {
      return null;
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

    Long programId = null;
    String programCode = null;
    String programName = null;
    if (entity.getProgram() != null) {
      programId = entity.getProgram().getId();
      programCode = entity.getProgram().getProgramCode();
      programName = entity.getProgram().getProgramName();
    }

    Long batchStatusId = null;
    String batchStatusCode = null;
    String batchStatusName = null;
    if (entity.getBatchStatus() != null) {
      batchStatusId = entity.getBatchStatus().getId();
      batchStatusCode = entity.getBatchStatus().getStatusCode();
      batchStatusName = entity.getBatchStatus().getStatusName();
    }

    return new TrainingBatchResponse(
        entity.getId(),
        entity.getBatchCode(),
        courseId,
        courseCode,
        courseName,
        providerId,
        providerCode,
        providerName,
        centerId,
        centerCode,
        centerName,
        programId,
        programCode,
        programName,
        entity.getStartDate(),
        entity.getEndDate(),
        entity.getCapacity(),
        batchStatusId,
        batchStatusCode,
        batchStatusName,
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        entity.getDeletedAt()
    );
  }

  public TrainingBatch toEntity(
      CreateTrainingBatchRequest request,
      Course course,
      TrainingProvider provider,
      TrainingCenter center,
      Program program,
      RefBatchStatus status) {
    if (request == null) {
      return null;
    }

    TrainingBatch batch = new TrainingBatch();
    batch.setBatchCode(request.getBatchCode());
    batch.setCourse(course);
    batch.setTrainingProvider(provider);
    batch.setTrainingCenter(center);
    batch.setProgram(program);
    batch.setStartDate(request.getStartDate());
    batch.setEndDate(request.getEndDate());
    batch.setCapacity(request.getCapacity());
    batch.setBatchStatus(status);
    return batch;
  }
}
