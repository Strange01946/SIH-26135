package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public record DataQualityRuleResponse(
    Long id,
    String ruleCode,
    String ruleName,
    String description,
    String targetEntityType,
    Long dataQualityCategoryId,
    String dataQualityCategoryCode,
    String dataQualityCategoryName,
    Long dataQualitySeverityId,
    String dataQualitySeverityCode,
    String dataQualitySeverityName,
    Long lifecycleStatusId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
