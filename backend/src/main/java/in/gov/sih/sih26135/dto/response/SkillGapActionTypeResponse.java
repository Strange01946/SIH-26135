package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public record SkillGapActionTypeResponse(
    Long id,
    String actionCode,
    String actionName,
    Boolean requiresCourseFlag,
    Integer sortOrder,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
