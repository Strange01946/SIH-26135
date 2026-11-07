package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

/**
 * Standardized response DTO representing consolidated ingestion metrics and outcomes for a bulk import batch.
 */
public record BulkImportSummaryResponse(
    Long importBatchId,
    String batchCode,
    String sourceSystem,
    String entityType,
    String status,
    Integer rowCount,
    Integer successCount,
    Integer failureCount,
    Integer skippedCount,
    LocalDateTime startedAt,
    LocalDateTime completedAt,
    Long durationMs
) {}
