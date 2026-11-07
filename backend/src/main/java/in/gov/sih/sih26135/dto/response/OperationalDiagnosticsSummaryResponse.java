package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

/**
 * Standardized response DTO representing consolidated operational event metrics and diagnostics across a time window.
 */
public record OperationalDiagnosticsSummaryResponse(
    LocalDateTime windowStart,
    LocalDateTime windowEnd,
    Long totalEvents,
    Long criticalEvents,
    Long errorEvents,
    Long warningEvents,
    Long infoEvents,
    Map<String, Long> categoryBreakdown,
    LocalDateTime lastEventTimestamp
) {

  public OperationalDiagnosticsSummaryResponse {
    categoryBreakdown = categoryBreakdown != null ? Collections.unmodifiableMap(categoryBreakdown) : Map.of();
  }
}
