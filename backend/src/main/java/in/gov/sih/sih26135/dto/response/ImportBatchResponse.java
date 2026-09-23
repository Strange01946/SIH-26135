package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public record ImportBatchResponse(
    Long id,
    String batchCode,
    String sourceSystem,
    String entityType,
    Long importBatchStatusId,
    String importBatchStatusCode,
    String importBatchStatusName,
    Boolean isTerminalFlag,
    Long initiatedByUserId,
    Integer rowCount,
    Integer successCount,
    Integer failureCount,
    LocalDateTime startedAt,
    LocalDateTime completedAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
