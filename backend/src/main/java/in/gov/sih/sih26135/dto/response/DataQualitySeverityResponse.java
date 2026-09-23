package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public record DataQualitySeverityResponse(
    Long id,
    String severityCode,
    String severityName,
    Integer sortOrder,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
