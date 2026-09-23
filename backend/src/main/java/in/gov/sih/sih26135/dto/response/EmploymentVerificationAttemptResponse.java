package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public record EmploymentVerificationAttemptResponse(
    Long id,
    Long employmentVerificationRequestId,
    String requestNumber,
    Integer attemptNumber,
    Long methodId,
    String methodCode,
    String methodName,
    Long statusId,
    String statusCode,
    String statusName,
    Boolean isTerminalFlag,
    Boolean isSuccessFlag,
    Long attemptedByUserId,
    Long employerRespondentUserId,
    Long communicationLogId,
    LocalDateTime attemptedAt,
    LocalDateTime completedAt,
    String notes,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
