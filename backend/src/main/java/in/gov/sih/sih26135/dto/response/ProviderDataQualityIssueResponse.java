package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

/**
 * Phase 30.5 standardized response DTO representing a data quality issue in the Training Provider Portal.
 */
public record ProviderDataQualityIssueResponse(
    Long id,
    Long ruleId,
    String ruleCode,
    String ruleName,
    Long categoryId,
    String categoryCode,
    String categoryName,
    Long severityId,
    String severityCode,
    String severityName,
    Long statusId,
    String statusCode,
    String statusName,
    boolean isOpen,
    String entityType,
    Long entityId,
    Long centerId,
    String centerName,
    Long batchId,
    String batchCode,
    String issueSummary,
    LocalDateTime detectedAt,
    LocalDateTime updatedAt,
    LocalDateTime resolvedAt,
    Long resolvedByUserId,
    String resolutionNotes,
    boolean remediationPermitted,
    String remediationReason
) {}
