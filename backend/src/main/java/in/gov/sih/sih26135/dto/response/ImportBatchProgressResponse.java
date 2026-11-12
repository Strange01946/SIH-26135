package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

/**
 * Progress and reconciled outcome summary for an import batch execution, exposing full row-level
 * breakdown across SUCCESS, FAILED, DUPLICATE, and SKIPPED records.
 */
public record ImportBatchProgressResponse(
    Long importBatchId,
    String batchCode,
    String sourceSystem,
    String entityType,
    String status,
    Boolean isTerminal,
    Long initiatedByUserId,
    Integer rowCount,
    Integer successCount,
    Integer failureCount,
    Integer duplicateCount,
    Integer skippedCount,
    LocalDateTime startedAt,
    LocalDateTime completedAt,
    Long durationMs
) {}
