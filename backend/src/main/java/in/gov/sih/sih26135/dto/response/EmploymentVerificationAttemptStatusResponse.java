package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public record EmploymentVerificationAttemptStatusResponse(
    Long id,
    String statusCode,
    String statusName,
    Boolean isTerminalFlag,
    Boolean isSuccessFlag,
    Integer sortOrder,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
