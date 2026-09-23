package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public record DataQualityIssueStatusResponse(
    Long id,
    String statusCode,
    String statusName,
    Boolean isOpenFlag,
    Boolean isTerminalFlag,
    Integer sortOrder,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
