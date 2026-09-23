package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public record SystemEventCategoryResponse(
    Long id,
    String categoryCode,
    String categoryName,
    Integer sortOrder,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
