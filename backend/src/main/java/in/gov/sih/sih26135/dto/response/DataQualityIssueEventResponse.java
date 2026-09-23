package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public record DataQualityIssueEventResponse(
    Long id,
    Long dataQualityIssueId,
    Long dataQualityIssueStatusId,
    String dataQualityIssueStatusCode,
    String dataQualityIssueStatusName,
    Long changedByUserId,
    LocalDateTime changedAt,
    String remarks,
    LocalDateTime createdAt
) {}
