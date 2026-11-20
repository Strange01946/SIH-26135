package in.gov.sih.sih26135.actuator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import in.gov.sih.sih26135.MysqlConnectionTest;
import in.gov.sih.sih26135.actuator.metrics.CommunicationMetricsRecorder;
import in.gov.sih.sih26135.actuator.metrics.DataQualityMetricsHolder;
import in.gov.sih.sih26135.actuator.metrics.ImportMetricsRecorder;
import in.gov.sih.sih26135.actuator.metrics.WorkflowMetricsAspect;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import java.sql.Connection;
import java.util.Set;
import javax.sql.DataSource;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Phase 29.11 Custom Micrometer Application Metrics Verification:
 * Validates custom business timers, counters, gauges, bounded cardinality,
 * and zero PII exposure across all registered metrics.
 */
@SpringBootTest
@DisplayName("Phase 29.11 — Custom Observability Metrics Hardening Tests")
public class CustomObservabilityMetricsTest {

  private static final Set<String> FORBIDDEN_PII_TAG_KEYS = Set.of(
      "user", "userid", "user_id",
      "trainee", "traineeid", "trainee_id",
      "phone", "mobile", "msisdn",
      "email", "emailaddress", "email_address",
      "aadhaar", "aadhar", "ssn", "pan",
      "name", "firstname", "lastname",
      "filepath", "file_path", "filename"
  );

  @Autowired
  private DataSource dataSource;

  @Autowired
  private MeterRegistry meterRegistry;

  @Autowired
  private WorkflowMetricsAspect workflowMetricsAspect;

  @Autowired
  private ImportMetricsRecorder importMetricsRecorder;

  @Autowired
  private DataQualityMetricsHolder dataQualityMetricsHolder;

  @Autowired
  private CommunicationMetricsRecorder communicationMetricsRecorder;

  @BeforeEach
  void verifyDatabaseIsolation() throws Exception {
    assertThat(dataSource).as("DataSource bean must be present").isNotNull();
    try (Connection connection = dataSource.getConnection()) {
      assertThat(connection).isNotNull();
      String catalog = connection.getCatalog();
      MysqlConnectionTest.assertDedicatedTestCatalog(catalog);
      assertThat(catalog).isEqualToIgnoringCase(MysqlConnectionTest.EXPECTED_TEST_CATALOG);
    }
  }

  // =========================================================================
  // 1. Workflow Transition Duration Timer Tests
  // =========================================================================

  @Test
  @DisplayName("WorkflowMetricsAspect records workflow.transition.duration timer on success")
  void testWorkflowMetricsAspectRecordsSuccess() throws Throwable {
    ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    Signature signature = mock(Signature.class);

    class FakeWorkflowService {
      public String submitEnrollment() {
        return "OK";
      }
    }

    when(joinPoint.getTarget()).thenReturn(new FakeWorkflowService());
    when(joinPoint.getSignature()).thenReturn(signature);
    when(signature.getName()).thenReturn("submitEnrollment");
    when(joinPoint.proceed()).thenReturn("SUCCESS_RESULT");

    Object result = workflowMetricsAspect.timeWorkflowTransition(joinPoint);
    assertThat(result).isEqualTo("SUCCESS_RESULT");

    Timer timer = meterRegistry.find("workflow.transition.duration")
        .tag("workflow.type", "FAKE")
        .tag("transition", "SUBMIT_ENROLLMENT")
        .tag("outcome", "SUCCESS")
        .timer();

    assertThat(timer).as("Workflow timer must be registered and recorded").isNotNull();
    assertThat(timer.count()).isGreaterThanOrEqualTo(1L);
  }

  @Test
  @DisplayName("WorkflowMetricsAspect records workflow.transition.duration timer on failure and rethrows")
  void testWorkflowMetricsAspectRecordsFailureAndRethrows() throws Throwable {
    ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    Signature signature = mock(Signature.class);

    class FakeWorkflowService {
      public String approveVerification() {
        return "OK";
      }
    }

    when(joinPoint.getTarget()).thenReturn(new FakeWorkflowService());
    when(joinPoint.getSignature()).thenReturn(signature);
    when(signature.getName()).thenReturn("approveVerification");
    when(joinPoint.proceed()).thenThrow(new IllegalStateException("Workflow business rule failed"));

    assertThatThrownBy(() -> workflowMetricsAspect.timeWorkflowTransition(joinPoint))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Workflow business rule failed");

    Timer timer = meterRegistry.find("workflow.transition.duration")
        .tag("workflow.type", "FAKE")
        .tag("transition", "APPROVE_VERIFICATION")
        .tag("outcome", "FAILURE")
        .timer();

    assertThat(timer).as("Workflow failure timer must be recorded").isNotNull();
    assertThat(timer.count()).isGreaterThanOrEqualTo(1L);
  }

  // =========================================================================
  // 2. Bulk Import Metrics Recorder Tests
  // =========================================================================

  @Test
  @DisplayName("ImportMetricsRecorder increments bulk.import.rows.processed counter with bounded tags")
  void testImportMetricsRecorder() {
    double initialTraineeSuccess = getCounterCount("bulk.import.rows.processed", "entity.type", "TRAINEE", "outcome", "SUCCESS");
    double initialPlacementFailed = getCounterCount("bulk.import.rows.processed", "entity.type", "PLACEMENT", "outcome", "FAILED");

    importMetricsRecorder.recordRowProcessed("TRAINEE", "SUCCESS");
    importMetricsRecorder.recordRowProcessed("PLACEMENT", "FAILED");

    double updatedTraineeSuccess = getCounterCount("bulk.import.rows.processed", "entity.type", "TRAINEE", "outcome", "SUCCESS");
    double updatedPlacementFailed = getCounterCount("bulk.import.rows.processed", "entity.type", "PLACEMENT", "outcome", "FAILED");

    assertThat(updatedTraineeSuccess).isEqualTo(initialTraineeSuccess + 1.0);
    assertThat(updatedPlacementFailed).isEqualTo(initialPlacementFailed + 1.0);
  }

  // =========================================================================
  // 3. Data Quality Cleanliness Index Gauge Tests
  // =========================================================================

  @Test
  @DisplayName("DataQualityMetricsHolder maintains O(1) in-memory gauge clamped between 0.0 and 100.0")
  void testDataQualityCleanlinessIndexGauge() {
    dataQualityMetricsHolder.updateCleanlinessIndex(87.5);
    assertThat(dataQualityMetricsHolder.getCleanlinessIndex()).isEqualTo(87.5);

    Gauge gauge = meterRegistry.find("data.quality.cleanliness.index").gauge();
    assertThat(gauge).as("Cleanliness index gauge must be registered").isNotNull();
    assertThat(gauge.value()).isEqualTo(87.5);

    // Test upper bound clamp
    dataQualityMetricsHolder.updateCleanlinessIndex(150.0);
    assertThat(gauge.value()).isEqualTo(100.0);

    // Test lower bound clamp
    dataQualityMetricsHolder.updateCleanlinessIndex(-20.0);
    assertThat(gauge.value()).isEqualTo(0.0);

    // Reset to realistic normal
    dataQualityMetricsHolder.updateCleanlinessIndex(95.0);
    assertThat(gauge.value()).isEqualTo(95.0);
  }

  // =========================================================================
  // 4. Communication Dispatch Metrics Recorder Tests
  // =========================================================================

  @Test
  @DisplayName("CommunicationMetricsRecorder increments communication.dispatch.total counter with bounded channels and outcomes")
  void testCommunicationMetricsRecorder() {
    double initialSmsSuccess = getCounterCount("communication.dispatch.total", "channel", "SMS", "outcome", "SUCCESS");
    double initialWaBlocked = getCounterCount("communication.dispatch.total", "channel", "WHATSAPP", "outcome", "BLOCKED_NO_CONSENT");
    double initialEmailQueued = getCounterCount("communication.dispatch.total", "channel", "EMAIL", "outcome", "QUEUED");

    communicationMetricsRecorder.recordDispatch("SMS", "SUCCESS");
    communicationMetricsRecorder.recordDispatch("WHATSAPP", "BLOCKED_NO_CONSENT");
    communicationMetricsRecorder.recordDispatch("EMAIL", "QUEUED");

    double updatedSmsSuccess = getCounterCount("communication.dispatch.total", "channel", "SMS", "outcome", "SUCCESS");
    double updatedWaBlocked = getCounterCount("communication.dispatch.total", "channel", "WHATSAPP", "outcome", "BLOCKED_NO_CONSENT");
    double updatedEmailQueued = getCounterCount("communication.dispatch.total", "channel", "EMAIL", "outcome", "QUEUED");

    assertThat(updatedSmsSuccess).isEqualTo(initialSmsSuccess + 1.0);
    assertThat(updatedWaBlocked).isEqualTo(initialWaBlocked + 1.0);
    assertThat(updatedEmailQueued).isEqualTo(initialEmailQueued + 1.0);
  }

  // =========================================================================
  // 5. Zero PII Verification Across All Custom Meters
  // =========================================================================

  @Test
  @DisplayName("All custom application metrics strictly contain zero PII tag names or values")
  void testZeroPiiInApplicationMetrics() {
    for (Meter meter : meterRegistry.getMeters()) {
      String metricName = meter.getId().getName();
      if (metricName.startsWith("workflow.") || metricName.startsWith("bulk.import.")
          || metricName.startsWith("data.quality.") || metricName.startsWith("communication.")
          || metricName.startsWith("database.")) {
        for (Tag tag : meter.getId().getTags()) {
          String key = tag.getKey().toLowerCase();
          assertThat(FORBIDDEN_PII_TAG_KEYS)
              .as("Metric tag '%s' in '%s' must not contain PII identifiers", key, metricName)
              .doesNotContain(key);

          // Verify low cardinality / safe bounded values
          assertThat(tag.getValue())
              .as("Tag value in '%s' must not contain email or phone", metricName)
              .doesNotContain("@")
              .doesNotMatch("^[0-9]{10,12}$");
        }
      }
    }
  }

  private double getCounterCount(String name, String tag1Key, String tag1Val, String tag2Key, String tag2Val) {
    Counter counter = meterRegistry.find(name)
        .tag(tag1Key, tag1Val)
        .tag(tag2Key, tag2Val)
        .counter();
    return counter != null ? counter.count() : 0.0;
  }
}
