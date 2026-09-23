package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public record EmploymentVerificationRequestStatusResponse(
    Long id,
    String statusCode,
    String statusName,
    Boolean isOpenFlag,
    Boolean isCompletedFlag,
    Integer sortOrder,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
