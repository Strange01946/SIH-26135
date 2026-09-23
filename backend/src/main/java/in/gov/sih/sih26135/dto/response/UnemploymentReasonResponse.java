package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public record UnemploymentReasonResponse(
    Long id,
    String reasonCode,
    String reasonName,
    Integer sortOrder,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
