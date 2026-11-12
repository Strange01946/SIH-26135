package in.gov.sih.sih26135.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

/**
 * Phase 28.7 response DTO for {@code GET /api/v1/operational/import-metrics}.
 *
 * <p>Clearly separates:
 * <ul>
 *   <li><b>Creation-cohort metrics</b> ({@code totalBatches}, {@code batchesByStatus},
 *       {@code totalProcessedRecords}, and record outcome counts/ratios): batches whose
 *       {@code created_at} falls in {@code [windowStart, windowEndExclusive)} and their committed
 *       rows.</li>
 *   <li><b>Operational throughput metrics</b>:
 *       {@code completedBatchesInWindow} / {@code batchesPerWindowHour} anchored to
 *       {@code import_batches.completed_at} inside {@code [windowStart, windowEndExclusive)};
 *       {@code successfulRowsProcessedInWindow} / {@code successfulRowsPerWindowHour} anchored to
 *       authoritative row processing timestamp {@code import_records.created_at} inside
 *       {@code [windowStart, windowEndExclusive)}; and {@code processedRowsPerExecutionHour}
 *       computed over committed records divided by total execution seconds
 *       ({@code completed_at - started_at}) for timed {@code COMPLETED} batches finished in the
 *       window.</li>
 * </ul>
 */
public record OperationalImportMetricsResponse(
    Long totalBatches,
    Long activeBatches,
    Long terminalBatches,
    Long pendingBatches,
    Long runningBatches,
    Long completedBatches,
    Long failedBatches,
    Long cancelledBatches,
    Map<String, Long> batchesByStatus,
    Long totalProcessedRecords,
    Long successRecordCount,
    Long failedRecordCount,
    Long duplicateRecordCount,
    Long skippedRecordCount,
    BigDecimal successRatioPct,
    BigDecimal failureRatioPct,
    BigDecimal duplicateRatioPct,
    BigDecimal skippedRatioPct,
    Long completedBatchesInWindow,
    Long successfulRowsProcessedInWindow,
    Long timedCompletedBatchesCount,
    Long timedCompletedRecordsCount,
    BigDecimal batchesPerWindowHour,
    BigDecimal successfulRowsPerWindowHour,
    BigDecimal processedRowsPerExecutionHour,
    String throughputUnit,
    LocalDateTime windowStart,
    LocalDateTime windowEndExclusive,
    BigDecimal windowDurationHours,
    LocalDateTime evaluatedAt
) {

  public OperationalImportMetricsResponse {
    batchesByStatus = batchesByStatus != null
        ? Collections.unmodifiableMap(batchesByStatus)
        : Map.of();
  }
}
