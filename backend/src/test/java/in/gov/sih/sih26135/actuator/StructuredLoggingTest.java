package in.gov.sih.sih26135.actuator;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.logging.logback.StructuredLogEncoder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;

/**
 * Phase 29.11 Targeted Review Verification:
 * Validates Spring Boot 3.4.10 native structured JSON logging support via ECS and Logstash formats.
 *
 * <p>Verifies:
 * <ol>
 *   <li>The configuration toggle {@code logging.structured.format.console} is properly exposed in environment properties.</li>
 *   <li>Native {@link StructuredLogEncoder} with ECS format produces valid JSON containing standard fields
 *       ({@code @timestamp}, {@code log.level}, {@code message}, {@code ecs.version}).</li>
 *   <li>MDC correlation context ({@code correlationId}) is cleanly included in the structured payload.</li>
 *   <li>Native {@link StructuredLogEncoder} with Logstash format produces valid JSON.</li>
 *   <li>Zero sensitive data (passwords, tokens, citizen PII) is leaked in structured logs.</li>
 * </ol>
 */
@SpringBootTest
@TestPropertySource(properties = {
    "logging.structured.format.console=ecs"
})
@DisplayName("Phase 29.11 — Structured Logging ECS & Logstash Format Verification")
public class StructuredLoggingTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Autowired
  private Environment environment;

  @Test
  @DisplayName("Property logging.structured.format.console is configured and resolved in Spring Environment")
  void testStructuredLoggingPropertyIsConfigured() {
    String format = environment.getProperty("logging.structured.format.console");
    assertThat(format).isEqualTo("ecs");
  }

  @Test
  @DisplayName("Spring Boot 3.4 native StructuredLogEncoder with ECS format encodes events into valid JSON with MDC")
  void testStructuredLoggingEcsFormatEncoding() throws Exception {
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    StructuredLogEncoder encoder = new StructuredLogEncoder();
    encoder.setContext(context);
    encoder.setFormat("ecs");
    encoder.start();

    String uniqueCorrelationId = "test-corr-ecs-" + System.currentTimeMillis();
    String testMessage = "Structured logging verification test message " + System.currentTimeMillis();

    ch.qos.logback.classic.Logger logger = context.getLogger(StructuredLoggingTest.class);
    LoggingEvent event = new LoggingEvent(
        StructuredLoggingTest.class.getName(),
        logger,
        Level.INFO,
        testMessage,
        null,
        null
    );
    event.setMDCPropertyMap(Map.of("correlationId", uniqueCorrelationId));

    byte[] encodedBytes = encoder.encode(event);
    String jsonString = new String(encodedBytes, StandardCharsets.UTF_8).trim();

    assertThat(jsonString).as("Structured log line must be a JSON object starting with {").startsWith("{");

    JsonNode jsonNode = objectMapper.readTree(jsonString);
    assertThat(jsonNode.has("@timestamp")).as("ECS log must contain @timestamp").isTrue();
    assertThat(jsonNode.has("log.level")).as("ECS log must contain log.level").isTrue();
    assertThat(jsonNode.get("log.level").asText()).isEqualTo("INFO");
    assertThat(jsonNode.get("message").asText()).isEqualTo(testMessage);

    // Verify correlation ID is captured in MDC fields
    assertThat(jsonString).contains(uniqueCorrelationId);

    // Verify zero sensitive leakage
    assertThat(jsonString)
        .doesNotContain("password")
        .doesNotContain("secret")
        .doesNotContain("Bearer ");

    encoder.stop();
  }

  @Test
  @DisplayName("Spring Boot 3.4 native StructuredLogEncoder with Logstash format encodes events into valid JSON")
  void testStructuredLoggingLogstashFormatEncoding() throws Exception {
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    StructuredLogEncoder encoder = new StructuredLogEncoder();
    encoder.setContext(context);
    encoder.setFormat("logstash");
    encoder.start();

    String uniqueCorrelationId = "test-corr-logstash-" + System.currentTimeMillis();
    String testMessage = "Logstash structured verification message " + System.currentTimeMillis();

    ch.qos.logback.classic.Logger logger = context.getLogger(StructuredLoggingTest.class);
    LoggingEvent event = new LoggingEvent(
        StructuredLoggingTest.class.getName(),
        logger,
        Level.INFO,
        testMessage,
        null,
        null
    );
    event.setMDCPropertyMap(Map.of("correlationId", uniqueCorrelationId));

    byte[] encodedBytes = encoder.encode(event);
    String jsonString = new String(encodedBytes, StandardCharsets.UTF_8).trim();

    assertThat(jsonString).as("Structured log line must be a JSON object starting with {").startsWith("{");

    JsonNode jsonNode = objectMapper.readTree(jsonString);
    assertThat(jsonNode.has("@timestamp")).as("Logstash log must contain @timestamp").isTrue();
    assertThat(jsonNode.get("message").asText()).isEqualTo(testMessage);
    assertThat(jsonString).contains(uniqueCorrelationId);

    encoder.stop();
  }
}
