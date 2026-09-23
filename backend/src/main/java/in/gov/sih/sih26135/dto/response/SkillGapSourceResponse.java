package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public record SkillGapSourceResponse(
    Long id,
    String sourceCode,
    String sourceName,
    Boolean isSelfReportedFlag,
    Boolean isEmployerFlag,
    Boolean isOfficialFlag,
    Integer sortOrder,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
