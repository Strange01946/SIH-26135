package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.EmploymentVerificationResponse;
import in.gov.sih.sih26135.entity.EmploymentVerification;
import org.springframework.stereotype.Component;

@Component
public class EmploymentVerificationMapper {

  public EmploymentVerificationResponse toResponse(EmploymentVerification entity) {
    if (entity == null) {
      return null;
    }

    Long requestId = entity.getEmploymentVerificationRequest() != null ? entity.getEmploymentVerificationRequest().getId() : null;
    String requestNumber = entity.getEmploymentVerificationRequest() != null ? entity.getEmploymentVerificationRequest().getRequestNumber() : null;

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

    Long recordVerificationStatusId = entity.getRecordVerificationStatus() != null ? entity.getRecordVerificationStatus().getId() : null;
    String recordVerificationStatusCode = entity.getRecordVerificationStatus() != null ? entity.getRecordVerificationStatus().getStatusCode() : null;
    String recordVerificationStatusName = entity.getRecordVerificationStatus() != null ? entity.getRecordVerificationStatus().getStatusName() : null;

    Long methodId = entity.getEmploymentVerificationMethod() != null ? entity.getEmploymentVerificationMethod().getId() : null;
    String methodCode = entity.getEmploymentVerificationMethod() != null ? entity.getEmploymentVerificationMethod().getMethodCode() : null;
    String methodName = entity.getEmploymentVerificationMethod() != null ? entity.getEmploymentVerificationMethod().getMethodName() : null;

    Long employmentInfoSourceId = entity.getEmploymentInfoSource() != null ? entity.getEmploymentInfoSource().getId() : null;
    String employmentInfoSourceCode = entity.getEmploymentInfoSource() != null ? entity.getEmploymentInfoSource().getSourceCode() : null;
    String employmentInfoSourceName = entity.getEmploymentInfoSource() != null ? entity.getEmploymentInfoSource().getSourceName() : null;

    Long rejectionReasonId = entity.getEmploymentVerificationRejectionReason() != null ? entity.getEmploymentVerificationRejectionReason().getId() : null;
    String rejectionReasonCode = entity.getEmploymentVerificationRejectionReason() != null ? entity.getEmploymentVerificationRejectionReason().getReasonCode() : null;
    String rejectionReasonName = entity.getEmploymentVerificationRejectionReason() != null ? entity.getEmploymentVerificationRejectionReason().getReasonName() : null;

    return new EmploymentVerificationResponse(
        entity.getId(),
        entity.getVerificationNumber(),
        requestId,
        requestNumber,
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
        entity.getIsCurrent(),
        entity.getCurrentVerificationKey(),
        recordVerificationStatusId,
        recordVerificationStatusCode,
        recordVerificationStatusName,
        methodId,
        methodCode,
        methodName,
        employmentInfoSourceId,
        employmentInfoSourceCode,
        employmentInfoSourceName,
        rejectionReasonId,
        rejectionReasonCode,
        rejectionReasonName,
        entity.getVerifiedByUserId(),
        entity.getEmployerRespondentUserId(),
        entity.getTraineeAttestedFlag(),
        entity.getEmployerConfirmedFlag(),
        entity.getRequestedAt(),
        entity.getVerifiedAt(),
        entity.getOutcomeRecordedAt(),
        entity.getNotes(),
        entity.getRejectionNotes(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
