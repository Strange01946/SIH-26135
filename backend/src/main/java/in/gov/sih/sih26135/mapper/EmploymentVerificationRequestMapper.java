package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.EmploymentVerificationRequestResponse;
import in.gov.sih.sih26135.entity.EmploymentVerificationRequest;
import org.springframework.stereotype.Component;

@Component
public class EmploymentVerificationRequestMapper {

  public EmploymentVerificationRequestResponse toResponse(EmploymentVerificationRequest entity) {
    if (entity == null) {
      return null;
    }

    Long employmentRecordId = entity.getEmploymentRecord() != null ? entity.getEmploymentRecord().getId() : null;
    String employmentRecordNumber = entity.getEmploymentRecord() != null ? entity.getEmploymentRecord().getEmploymentNumber() : null;

    Long traineeId = entity.getTrainee() != null ? entity.getTrainee().getId() : null;
    String traineeRegistrationNumber = entity.getTrainee() != null ? entity.getTrainee().getRegistrationNumber() : null;
    String traineeFirstName = entity.getTrainee() != null ? entity.getTrainee().getFirstName() : null;
    String traineeLastName = entity.getTrainee() != null ? entity.getTrainee().getLastName() : null;

    Long placementRecordId = entity.getPlacementRecord() != null ? entity.getPlacementRecord().getId() : null;
    String placementRecordNumber = entity.getPlacementRecord() != null ? entity.getPlacementRecord().getPlacementNumber() : null;

    Long employerId = entity.getEmployer() != null ? entity.getEmployer().getId() : null;
    String employerName = entity.getEmployer() != null ? entity.getEmployer().getEmployerName() : null;

    Long statusId = entity.getEmploymentVerificationRequestStatus() != null ? entity.getEmploymentVerificationRequestStatus().getId() : null;
    String statusCode = entity.getEmploymentVerificationRequestStatus() != null ? entity.getEmploymentVerificationRequestStatus().getStatusCode() : null;
    String statusName = entity.getEmploymentVerificationRequestStatus() != null ? entity.getEmploymentVerificationRequestStatus().getStatusName() : null;
    Boolean isOpenFlag = entity.getEmploymentVerificationRequestStatus() != null ? entity.getEmploymentVerificationRequestStatus().getIsOpenFlag() : null;
    Boolean isCompletedFlag = entity.getEmploymentVerificationRequestStatus() != null ? entity.getEmploymentVerificationRequestStatus().getIsCompletedFlag() : null;

    Long preferredMethodId = entity.getPreferredVerificationMethod() != null ? entity.getPreferredVerificationMethod().getId() : null;
    String preferredMethodCode = entity.getPreferredVerificationMethod() != null ? entity.getPreferredVerificationMethod().getMethodCode() : null;
    String preferredMethodName = entity.getPreferredVerificationMethod() != null ? entity.getPreferredVerificationMethod().getMethodName() : null;

    Long employmentInfoSourceId = entity.getEmploymentInfoSource() != null ? entity.getEmploymentInfoSource().getId() : null;
    String employmentInfoSourceCode = entity.getEmploymentInfoSource() != null ? entity.getEmploymentInfoSource().getSourceCode() : null;
    String employmentInfoSourceName = entity.getEmploymentInfoSource() != null ? entity.getEmploymentInfoSource().getSourceName() : null;

    Long followupTaskId = entity.getFollowupTask() != null ? entity.getFollowupTask().getId() : null;
    Long surveyResponseId = entity.getSurveyResponse() != null ? entity.getSurveyResponse().getId() : null;

    return new EmploymentVerificationRequestResponse(
        entity.getId(),
        entity.getRequestNumber(),
        employmentRecordId,
        employmentRecordNumber,
        traineeId,
        traineeRegistrationNumber,
        traineeFirstName,
        traineeLastName,
        placementRecordId,
        placementRecordNumber,
        employerId,
        employerName,
        entity.getCycleNumber(),
        entity.getIsReverification(),
        statusId,
        statusCode,
        statusName,
        isOpenFlag,
        isCompletedFlag,
        preferredMethodId,
        preferredMethodCode,
        preferredMethodName,
        employmentInfoSourceId,
        employmentInfoSourceCode,
        employmentInfoSourceName,
        entity.getRequestedByUserId(),
        entity.getAssignedVerifierUserId(),
        followupTaskId,
        surveyResponseId,
        entity.getRequestedAt(),
        entity.getDueAt(),
        entity.getFirstAttemptAt(),
        entity.getCompletedAt(),
        entity.getOpenRequestKey(),
        entity.getRemarks(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
