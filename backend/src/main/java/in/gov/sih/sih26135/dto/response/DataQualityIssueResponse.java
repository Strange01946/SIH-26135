package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public record DataQualityIssueResponse(
    Long id,
    Long dataQualityRuleId,
    String dataQualityRuleCode,
    String dataQualityRuleName,
    Long dataQualityCategoryId,
    String dataQualityCategoryCode,
    String dataQualityCategoryName,
    Long dataQualitySeverityId,
    String dataQualitySeverityCode,
    String dataQualitySeverityName,
    Long dataQualityIssueStatusId,
    String dataQualityIssueStatusCode,
    String dataQualityIssueStatusName,
    Boolean isOpenFlag,
    Boolean isTerminalFlag,
    Long dataQualityDetectionSourceId,
    String dataQualityDetectionSourceCode,
    String dataQualityDetectionSourceName,
    String entityType,
    Long entityId,
    String issueSummary,
    LocalDateTime detectedAt,
    Long assignedUserId,
    LocalDateTime resolvedAt,
    Long resolvedByUserId,
    String resolutionNotes,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
