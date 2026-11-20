package in.gov.sih.sih26135.actuator.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Metric holder exposing the system-wide Data Quality Cleanliness Index via a Micrometer Gauge.
 *
 * <p>Principles:
 * <ul>
 *   <li>Metric name: {@code data.quality.cleanliness.index}.</li>
 *   <li>Backed by an in-memory atomic reference updated when DQI evaluations occur.</li>
 *   <li>Zero database aggregation or recalculation during Prometheus scrapes (O(1) read).</li>
 *   <li>System-wide aggregate percentage ([0.0, 100.0]): zero tenant or district cardinality leaks.</li>
 * </ul>
 */
@Component
public class DataQualityMetricsHolder {

  private final AtomicReference<Double> cleanlinessIndex = new AtomicReference<>(100.0);

  @Autowired
  public DataQualityMetricsHolder(MeterRegistry meterRegistry) {
    Objects.requireNonNull(meterRegistry, "MeterRegistry is required");
    Gauge.builder("data.quality.cleanliness.index", cleanlinessIndex, AtomicReference::get)
        .description("Overall system Data Quality Cleanliness Index percentage (0.0 - 100.0)")
        .register(meterRegistry);
  }

  public void updateCleanlinessIndex(double value) {
    double clamped = Math.max(0.0, Math.min(100.0, value));
    cleanlinessIndex.set(clamped);
  }

  public double getCleanlinessIndex() {
    return cleanlinessIndex.get();
  }
}
