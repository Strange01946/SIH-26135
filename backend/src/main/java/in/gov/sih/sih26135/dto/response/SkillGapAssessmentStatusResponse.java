package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public record SkillGapAssessmentStatusResponse(
    Long id,
    String statusCode,
    String statusName,
    Boolean isCompletedFlag,
    Integer sortOrder,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
