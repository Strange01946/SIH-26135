package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public record SkillGapSeverityResponse(
    Long id,
    String severityCode,
    String severityName,
    Integer severityRank,
    Integer sortOrder,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
