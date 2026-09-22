package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public record RecordVerificationStatusResponse(
    Long id,
    String statusCode,
    String statusName,
    Boolean isVerifiedFlag,
    Integer sortOrder,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
