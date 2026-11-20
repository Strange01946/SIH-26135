package in.gov.sih.sih26135.actuator.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Locale;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Micrometer metrics recorder for bulk data ingestion and staging pipelines.
 *
 * <p>Principles:
 * <ul>
 *   <li>Metric name: {@code bulk.import.rows.processed}.</li>
 *   <li>Tags: {@code entity.type} (e.g. TRAINEE, PLACEMENT), {@code outcome} (PROCESSED, SUCCESS, QUARANTINED, FAILED, SKIPPED, DUPLICATE).</li>
 *   <li>Strictly zero PII: no row contents, citizen IDs, batch codes, or file names.</li>
 *   <li>Deterministic counter increments aligned with transactional batch reconciliation.</li>
 * </ul>
 */
@Component
public class ImportMetricsRecorder {

  private final MeterRegistry meterRegistry;

  @Autowired
  public ImportMetricsRecorder(MeterRegistry meterRegistry) {
    this.meterRegistry = Objects.requireNonNull(meterRegistry, "MeterRegistry is required");
  }

  public void recordRowProcessed(String entityType, String outcome) {
    String safeEntity = (entityType != null && !entityType.isBlank())
        ? entityType.toUpperCase(Locale.ROOT)
        : "UNKNOWN";
    String safeOutcome = (outcome != null && !outcome.isBlank())
        ? outcome.toUpperCase(Locale.ROOT)
        : "UNKNOWN";

    Counter.builder("bulk.import.rows.processed")
        .description("Total number of bulk import rows processed across ingestion pipelines")
        .tag("entity.type", safeEntity)
        .tag("outcome", safeOutcome)
        .register(meterRegistry)
        .increment();
  }
}
