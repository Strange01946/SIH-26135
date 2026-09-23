package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.EmploymentVerificationAttemptResponse;
import in.gov.sih.sih26135.entity.EmploymentVerificationAttempt;
import org.springframework.stereotype.Component;

@Component
public class EmploymentVerificationAttemptMapper {

  public EmploymentVerificationAttemptResponse toResponse(EmploymentVerificationAttempt entity) {
    if (entity == null) {
      return null;
    }

    Long requestId = entity.getEmploymentVerificationRequest() != null ? entity.getEmploymentVerificationRequest().getId() : null;
    String requestNumber = entity.getEmploymentVerificationRequest() != null ? entity.getEmploymentVerificationRequest().getRequestNumber() : null;

    Long methodId = entity.getEmploymentVerificationMethod() != null ? entity.getEmploymentVerificationMethod().getId() : null;
    String methodCode = entity.getEmploymentVerificationMethod() != null ? entity.getEmploymentVerificationMethod().getMethodCode() : null;
    String methodName = entity.getEmploymentVerificationMethod() != null ? entity.getEmploymentVerificationMethod().getMethodName() : null;

    Long statusId = entity.getEmploymentVerificationAttemptStatus() != null ? entity.getEmploymentVerificationAttemptStatus().getId() : null;
    String statusCode = entity.getEmploymentVerificationAttemptStatus() != null ? entity.getEmploymentVerificationAttemptStatus().getStatusCode() : null;
    String statusName = entity.getEmploymentVerificationAttemptStatus() != null ? entity.getEmploymentVerificationAttemptStatus().getStatusName() : null;
    Boolean isTerminalFlag = entity.getEmploymentVerificationAttemptStatus() != null ? entity.getEmploymentVerificationAttemptStatus().getIsTerminalFlag() : null;
    Boolean isSuccessFlag = entity.getEmploymentVerificationAttemptStatus() != null ? entity.getEmploymentVerificationAttemptStatus().getIsSuccessFlag() : null;

    Long communicationLogId = entity.getCommunicationLog() != null ? entity.getCommunicationLog().getId() : null;

    return new EmploymentVerificationAttemptResponse(
        entity.getId(),
        requestId,
        requestNumber,
        entity.getAttemptNumber(),
        methodId,
        methodCode,
        methodName,
        statusId,
        statusCode,
        statusName,
        isTerminalFlag,
        isSuccessFlag,
        entity.getAttemptedByUserId(),
        entity.getEmployerRespondentUserId(),
        communicationLogId,
        entity.getAttemptedAt(),
        entity.getCompletedAt(),
        entity.getNotes(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
