package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public record ImportBatchStatusResponse(
    Long id,
    String statusCode,
    String statusName,
    Boolean isTerminalFlag,
    Integer sortOrder,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
