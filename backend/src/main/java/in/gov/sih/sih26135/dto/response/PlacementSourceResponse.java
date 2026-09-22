package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public record PlacementSourceResponse(
    Long id,
    String sourceCode,
    String sourceName,
    Integer sortOrder,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
