package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.request.CreateAssessmentRequest;
import in.gov.sih.sih26135.dto.response.AssessmentResponse;
import in.gov.sih.sih26135.entity.Assessment;
import in.gov.sih.sih26135.entity.Course;
import in.gov.sih.sih26135.entity.Program;
import in.gov.sih.sih26135.entity.RefAssessmentType;
import in.gov.sih.sih26135.entity.TrainingBatch;
import org.springframework.stereotype.Component;

@Component
public class AssessmentMapper {

  public AssessmentResponse toResponse(Assessment entity) {
    if (entity == null) {
      return null;
    }

    Long assessmentTypeId = null;
    String assessmentTypeCode = null;
    String assessmentTypeName = null;
    if (entity.getAssessmentType() != null) {
      assessmentTypeId = entity.getAssessmentType().getId();
      assessmentTypeCode = entity.getAssessmentType().getTypeCode();
      assessmentTypeName = entity.getAssessmentType().getTypeName();
    }

    Long courseId = null;
    String courseCode = null;
    String courseName = null;
    if (entity.getCourse() != null) {
      courseId = entity.getCourse().getId();
      courseCode = entity.getCourse().getCourseCode();
      courseName = entity.getCourse().getCourseName();
    }

    Long batchId = null;
    String batchCode = null;
    if (entity.getTrainingBatch() != null) {
      batchId = entity.getTrainingBatch().getId();
      batchCode = entity.getTrainingBatch().getBatchCode();
    }

    Long programId = null;
    String programCode = null;
    String programName = null;
    if (entity.getProgram() != null) {
      programId = entity.getProgram().getId();
      programCode = entity.getProgram().getProgramCode();
      programName = entity.getProgram().getProgramName();
    }

    return new AssessmentResponse(
        entity.getId(),
        entity.getAssessmentCode(),
        entity.getAssessmentName(),
        assessmentTypeId,
        assessmentTypeCode,
        assessmentTypeName,
        courseId,
        courseCode,
        courseName,
        batchId,
        batchCode,
        programId,
        programCode,
        programName,
        entity.getAssessmentDate(),
        entity.getMaximumScore(),
        entity.getPassScore(),
        entity.getEvaluatorUserId(),
        entity.getEvaluatorName(),
        entity.getLifecycleStatusId(),
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        entity.getDeletedAt()
    );
  }

  public Assessment toEntity(
      CreateAssessmentRequest request,
      RefAssessmentType assessmentType,
      Course course,
      TrainingBatch batch,
      Program program) {
    if (request == null) {
      return null;
    }

    Assessment entity = new Assessment();
    entity.setAssessmentCode(request.getAssessmentCode());
    entity.setAssessmentName(request.getAssessmentName());
    entity.setAssessmentType(assessmentType);
    entity.setCourse(course);
    entity.setTrainingBatch(batch);
    entity.setProgram(program);
    entity.setAssessmentDate(request.getAssessmentDate());
    entity.setMaximumScore(request.getMaximumScore());
    entity.setPassScore(request.getPassScore());
    entity.setEvaluatorUserId(request.getEvaluatorUserId());
    entity.setEvaluatorName(request.getEvaluatorName());
    entity.setLifecycleStatusId(request.getLifecycleStatusId() != null ? request.getLifecycleStatusId() : 1L);
    return entity;
  }
}
