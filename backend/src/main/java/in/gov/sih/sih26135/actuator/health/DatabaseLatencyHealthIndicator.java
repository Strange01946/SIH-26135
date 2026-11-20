package in.gov.sih.sih26135.actuator.health;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Health indicator measuring non-intrusive database round-trip query latency using a monotonic clock.
 *
 * <p>Execution Principles:
 * <ul>
 *   <li>Lightweight, read-only verification query ({@code SELECT 1}).</li>
 *   <li>High-precision monotonic clock measurement ({@link System#nanoTime()}).</li>
 *   <li>Publishes duration to Micrometer metric {@code database.query.latency}.</li>
 *   <li>Configurable SLA threshold (default 500 ms).</li>
 *   <li>Distinguishes healthy latency, slow query warning (without bringing the entire app DOWN), and genuine connectivity failure.</li>
 *   <li>Strictly zero mutating operations, zero connection starvation.</li>
 * </ul>
 */
@Component("databaseLatency")
public class DatabaseLatencyHealthIndicator implements HealthIndicator {

  private static final Logger log = LoggerFactory.getLogger(DatabaseLatencyHealthIndicator.class);

  private final JdbcTemplate jdbcTemplate;
  private final Timer latencyTimer;
  private final long latencyThresholdMs;

  @Autowired
  public DatabaseLatencyHealthIndicator(
      JdbcTemplate jdbcTemplate,
      MeterRegistry meterRegistry,
      @Value("${management.health.database.latency-threshold-ms:500}") long latencyThresholdMs) {
    this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "JdbcTemplate is required");
    this.latencyThresholdMs = latencyThresholdMs;
    this.latencyTimer = Timer.builder("database.query.latency")
        .description("Round-trip latency of database ping verification query")
        .register(meterRegistry);
  }

  @Override
  public Health health() {
    long startNanos = System.nanoTime();
    try {
      Integer pingResult = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
      long elapsedNanos = System.nanoTime() - startNanos;
      long elapsedMs = TimeUnit.NANOSECONDS.toMillis(elapsedNanos);

      latencyTimer.record(Duration.ofNanos(elapsedNanos));

      if (elapsedMs > latencyThresholdMs) {
        log.warn("Database ping query exceeded latency threshold: {} ms > {} ms", elapsedMs, latencyThresholdMs);
        return Health.up()
            .withDetail("database", "MySQL")
            .withDetail("status", "SLOW_LATENCY_WARNING")
            .withDetail("latencyMs", elapsedMs)
            .withDetail("thresholdMs", latencyThresholdMs)
            .withDetail("message", "Database reachable but query latency exceeds configured SLA threshold")
            .build();
      }

      return Health.up()
          .withDetail("database", "MySQL")
          .withDetail("status", "OPTIMAL")
          .withDetail("latencyMs", elapsedMs)
          .withDetail("thresholdMs", latencyThresholdMs)
          .build();

    } catch (Exception ex) {
      log.error("Database health check ping query failed: {}", ex.getMessage());
      return Health.down(ex)
          .withDetail("database", "MySQL")
          .withDetail("error", "Database connection or ping query failed")
          .build();
    }
  }

  public long getLatencyThresholdMs() {
    return latencyThresholdMs;
  }
}
