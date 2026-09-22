package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public record EmploymentInfoSourceResponse(
    Long id,
    String sourceCode,
    String sourceName,
    Boolean isSelfReportedFlag,
    Integer sortOrder,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
