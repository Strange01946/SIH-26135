package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public record SkillGapStatusResponse(
    Long id,
    String statusCode,
    String statusName,
    Boolean isOpenFlag,
    Boolean isResolvedFlag,
    Integer sortOrder,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
