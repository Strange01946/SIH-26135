package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.TraineeUnemploymentEventResponse;
import in.gov.sih.sih26135.entity.TraineeUnemploymentEvent;
import org.springframework.stereotype.Component;

@Component
public class TraineeUnemploymentEventMapper {

  public TraineeUnemploymentEventResponse toResponse(TraineeUnemploymentEvent entity) {
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

    Long reasonId = null;
    String reasonCode = null;
    String reasonName = null;
    if (entity.getUnemploymentReason() != null) {
      reasonId = entity.getUnemploymentReason().getId();
      reasonCode = entity.getUnemploymentReason().getReasonCode();
      reasonName = entity.getUnemploymentReason().getReasonName();
    }

    Long precedingEmpId = null;
    String precedingEmpNumber = null;
    if (entity.getPrecedingEmployment() != null) {
      precedingEmpId = entity.getPrecedingEmployment().getId();
      precedingEmpNumber = entity.getPrecedingEmployment().getEmploymentNumber();
    }

    Long exitEventId = null;
    if (entity.getEmploymentExitEvent() != null) {
      exitEventId = entity.getEmploymentExitEvent().getId();
    }

    Long succeedingEmpId = null;
    String succeedingEmpNumber = null;
    if (entity.getSucceedingEmployment() != null) {
      succeedingEmpId = entity.getSucceedingEmployment().getId();
      succeedingEmpNumber = entity.getSucceedingEmployment().getEmploymentNumber();
    }

    Long enrollmentId = null;
    String enrollmentNumber = null;
    if (entity.getEnrollment() != null) {
      enrollmentId = entity.getEnrollment().getId();
      enrollmentNumber = entity.getEnrollment().getEnrollmentNumber();
    }

    Long placementId = null;
    String placementNumber = null;
    if (entity.getPlacementRecord() != null) {
      placementId = entity.getPlacementRecord().getId();
      placementNumber = entity.getPlacementRecord().getPlacementNumber();
    }

    Long followupTaskId = null;
    if (entity.getFollowupTask() != null) {
      followupTaskId = entity.getFollowupTask().getId();
    }

    Long surveyResponseId = null;
    if (entity.getSurveyResponse() != null) {
      surveyResponseId = entity.getSurveyResponse().getId();
    }

    Long infoSourceId = null;
    String infoSourceCode = null;
    String infoSourceName = null;
    if (entity.getEmploymentInfoSource() != null) {
      infoSourceId = entity.getEmploymentInfoSource().getId();
      infoSourceCode = entity.getEmploymentInfoSource().getSourceCode();
      infoSourceName = entity.getEmploymentInfoSource().getSourceName();
    }

    Long verificationStatusId = null;
    String verificationStatusCode = null;
    String verificationStatusName = null;
    if (entity.getRecordVerificationStatus() != null) {
      verificationStatusId = entity.getRecordVerificationStatus().getId();
      verificationStatusCode = entity.getRecordVerificationStatus().getStatusCode();
      verificationStatusName = entity.getRecordVerificationStatus().getStatusName();
    }

    return new TraineeUnemploymentEventResponse(
        entity.getId(),
        traineeId,
        traineeReg,
        traineeFirst,
        traineeLast,
        entity.getPeriodNumber(),
        entity.getStartDate(),
        entity.getEndDate(),
        entity.getIsCurrent(),
        entity.getCurrentPeriodKey(),
        entity.getLabourStatusId(),
        reasonId,
        reasonCode,
        reasonName,
        precedingEmpId,
        precedingEmpNumber,
        exitEventId,
        succeedingEmpId,
        succeedingEmpNumber,
        enrollmentId,
        enrollmentNumber,
        placementId,
        placementNumber,
        followupTaskId,
        surveyResponseId,
        infoSourceId,
        infoSourceCode,
        infoSourceName,
        verificationStatusId,
        verificationStatusCode,
        verificationStatusName,
        entity.getVerifiedAt(),
        entity.getVerifiedByUserId(),
        entity.getRemarks(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
