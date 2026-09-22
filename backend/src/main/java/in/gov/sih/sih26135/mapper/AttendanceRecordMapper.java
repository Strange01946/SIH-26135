package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.request.CreateAttendanceRecordRequest;
import in.gov.sih.sih26135.dto.response.AttendanceRecordResponse;
import in.gov.sih.sih26135.entity.AttendanceRecord;
import in.gov.sih.sih26135.entity.RefAttendanceStatus;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.entity.TrainingBatch;
import in.gov.sih.sih26135.entity.TrainingEnrollment;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class AttendanceRecordMapper {

  public AttendanceRecordResponse toResponse(AttendanceRecord entity) {
    if (entity == null) {
      return null;
    }

    Long enrollmentId = null;
    String enrollmentNumber = null;
    if (entity.getTrainingEnrollment() != null) {
      enrollmentId = entity.getTrainingEnrollment().getId();
      enrollmentNumber = entity.getTrainingEnrollment().getEnrollmentNumber();
    }

    Long traineeId = null;
    if (entity.getTrainee() != null) {
      traineeId = entity.getTrainee().getId();
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
    Boolean countsAsPresent = null;
    if (entity.getAttendanceStatus() != null) {
      statusId = entity.getAttendanceStatus().getId();
      statusCode = entity.getAttendanceStatus().getStatusCode();
      statusName = entity.getAttendanceStatus().getStatusName();
      countsAsPresent = entity.getAttendanceStatus().getCountsAsPresent();
    }

    return new AttendanceRecordResponse(
        entity.getId(),
        enrollmentId,
        enrollmentNumber,
        traineeId,
        batchId,
        batchCode,
        entity.getSessionDate(),
        entity.getSessionSequence(),
        entity.getSessionStartTime(),
        entity.getSessionEndTime(),
        statusId,
        statusCode,
        statusName,
        countsAsPresent,
        entity.getMarkedAt(),
        entity.getMarkedByUserId(),
        entity.getRemarks(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }

  public AttendanceRecord toEntity(
      CreateAttendanceRecordRequest request,
      TrainingEnrollment enrollment,
      Trainee trainee,
      TrainingBatch batch,
      RefAttendanceStatus status) {
    if (request == null) {
      return null;
    }

    AttendanceRecord entity = new AttendanceRecord();
    entity.setTrainingEnrollment(enrollment);
    entity.setTrainee(trainee);
    entity.setTrainingBatch(batch);
    entity.setSessionDate(request.getSessionDate());
    entity.setSessionSequence(request.getSessionSequence() != null ? request.getSessionSequence() : 1);
    entity.setSessionStartTime(request.getSessionStartTime());
    entity.setSessionEndTime(request.getSessionEndTime());
    entity.setAttendanceStatus(status);
    entity.setMarkedAt(request.getMarkedAt() != null ? request.getMarkedAt() : LocalDateTime.now());
    entity.setMarkedByUserId(request.getMarkedByUserId());
    entity.setRemarks(request.getRemarks());
    return entity;
  }
}
