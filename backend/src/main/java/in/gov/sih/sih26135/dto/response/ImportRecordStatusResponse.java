package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public record ImportRecordStatusResponse(
    Long id,
    String statusCode,
    String statusName,
    Integer sortOrder,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
