package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.EmploymentExitEventResponse;
import in.gov.sih.sih26135.entity.EmploymentExitEvent;
import org.springframework.stereotype.Component;

@Component
public class EmploymentExitEventMapper {

  public EmploymentExitEventResponse toResponse(EmploymentExitEvent entity) {
    if (entity == null) {
      return null;
    }

    Long employmentId = null;
    String employmentNumber = null;
    if (entity.getEmploymentRecord() != null) {
      employmentId = entity.getEmploymentRecord().getId();
      employmentNumber = entity.getEmploymentRecord().getEmploymentNumber();
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

    Long exitReasonId = null;
    String exitReasonCode = null;
    String exitReasonName = null;
    if (entity.getEmploymentExitReason() != null) {
      exitReasonId = entity.getEmploymentExitReason().getId();
      exitReasonCode = entity.getEmploymentExitReason().getReasonCode();
      exitReasonName = entity.getEmploymentExitReason().getReasonName();
    }

    Long separationNatureId = null;
    String separationNatureCode = null;
    String separationNatureName = null;
    Boolean isVoluntaryFlag = null;
    Boolean isInvoluntaryFlag = null;
    if (entity.getSeparationNature() != null) {
      separationNatureId = entity.getSeparationNature().getId();
      separationNatureCode = entity.getSeparationNature().getNatureCode();
      separationNatureName = entity.getSeparationNature().getNatureName();
      isVoluntaryFlag = entity.getSeparationNature().getIsVoluntaryFlag();
      isInvoluntaryFlag = entity.getSeparationNature().getIsInvoluntaryFlag();
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

    return new EmploymentExitEventResponse(
        entity.getId(),
        employmentId,
        employmentNumber,
        traineeId,
        traineeReg,
        traineeFirst,
        traineeLast,
        enrollmentId,
        enrollmentNumber,
        entity.getSeparationDate(),
        exitReasonId,
        exitReasonCode,
        exitReasonName,
        separationNatureId,
        separationNatureCode,
        separationNatureName,
        isVoluntaryFlag,
        isInvoluntaryFlag,
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
