package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

/**
 * Standardized response DTO representing the outcome of an automated data quality rule execution.
 */
public record DataQualityRuleExecutionSummary(
    Long ruleId,
    String ruleCode,
    String targetEntityType,
    Long evaluatedRecordCount,
    Long violationCount,
    Long newIssuesCreated,
    Long deduplicatedIssuesSkipped,
    LocalDateTime executedAt,
    Long executionDurationMs,
    String status
) {}
