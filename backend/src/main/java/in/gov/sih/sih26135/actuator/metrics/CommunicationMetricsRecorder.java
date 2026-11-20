package in.gov.sih.sih26135.actuator.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Locale;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Micrometer metrics recorder for communication dispatch events (SMS, WhatsApp, Email).
 *
 * <p>Principles:
 * <ul>
 *   <li>Metric name: {@code communication.dispatch.total}.</li>
 *   <li>Bounded channel tags: {@code EMAIL}, {@code SMS}, {@code WHATSAPP}.</li>
 *   <li>Bounded outcome tags: {@code SUCCESS}, {@code FAILURE}, {@code BLOCKED_NO_CONSENT}, {@code QUEUED}.</li>
 *   <li>Strictly zero PII: no phone numbers, email addresses, message contents, or webhook URLs.</li>
 * </ul>
 */
@Component
public class CommunicationMetricsRecorder {

  private final MeterRegistry meterRegistry;

  @Autowired
  public CommunicationMetricsRecorder(MeterRegistry meterRegistry) {
    this.meterRegistry = Objects.requireNonNull(meterRegistry, "MeterRegistry is required");
  }

  public void recordDispatch(String channel, String outcome) {
    String safeChannel = (channel != null && !channel.isBlank())
        ? channel.toUpperCase(Locale.ROOT)
        : "UNKNOWN";
    String safeOutcome = (outcome != null && !outcome.isBlank())
        ? outcome.toUpperCase(Locale.ROOT)
        : "UNKNOWN";

    Counter.builder("communication.dispatch.total")
        .description("Total communication dispatch attempts partitioned by channel and outcome")
        .tag("channel", safeChannel)
        .tag("outcome", safeOutcome)
        .register(meterRegistry)
        .increment();
  }
}
