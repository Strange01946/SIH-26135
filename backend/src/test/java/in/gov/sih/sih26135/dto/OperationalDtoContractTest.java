package in.gov.sih.sih26135.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import in.gov.sih.sih26135.dto.request.AssignDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.request.AuditLogFilterCriteria;
import in.gov.sih.sih26135.dto.request.BulkImportInitiateRequest;
import in.gov.sih.sih26135.dto.request.DataQualityScanRequest;
import in.gov.sih.sih26135.dto.request.OperationalEventFilterCriteria;
import in.gov.sih.sih26135.dto.request.ResolveDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.response.AuditTrailTimelineResponse;
import in.gov.sih.sih26135.dto.response.AuditTrailTimelineResponse.AuditTrailEntry;
import in.gov.sih.sih26135.dto.response.BulkImportSummaryResponse;
import in.gov.sih.sih26135.dto.response.DataQualityHealthScorecardResponse;
import in.gov.sih.sih26135.dto.response.DataQualityRuleExecutionSummary;
import in.gov.sih.sih26135.dto.response.OperationalDiagnosticsSummaryResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comprehensive contract, validation, and JSON serialization test suite
 * for Phase 28.1 operational request and response DTOs.
 */
class OperationalDtoContractTest {

  private static ObjectMapper objectMapper;
  private static Validator validator;

  @BeforeAll
  static void setUp() {
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());

    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  // =========================================================================
  // REQUEST DTO TESTS
  // =========================================================================

  @Nested
  @DisplayName("AuditLogFilterCriteria Tests")
  class AuditLogFilterCriteriaTests {

    @Test
    @DisplayName("Default criteria initializes with safe pagination and sort defaults")
    void testDefaults() {
      AuditLogFilterCriteria criteria = new AuditLogFilterCriteria();
      assertThat(criteria.getPage()).isEqualTo(0);
      assertThat(criteria.getSize()).isEqualTo(20);
      assertThat(criteria.getSort()).isEqualTo("occurredAt");
      assertThat(criteria.getDirection()).isEqualTo("DESC");
      assertThat(criteria.hasValidDateRange()).isTrue();
    }

    @Test
    @DisplayName("Valid criteria passes validation and serializes/deserializes cleanly")
    void testValidCriteriaSerialization() throws Exception {
      LocalDateTime now = LocalDateTime.now();
      AuditLogFilterCriteria criteria = new AuditLogFilterCriteria(
          101L, "CREATE", "Trainee", 5001L,
          now.minusDays(7), now, "CORR-12345",
          1, 50, "occurredAt", "ASC"
      );

      Set<ConstraintViolation<AuditLogFilterCriteria>> violations = validator.validate(criteria);
      assertThat(violations).isEmpty();
      assertThat(criteria.hasValidDateRange()).isTrue();

      String json = objectMapper.writeValueAsString(criteria);
      AuditLogFilterCriteria deserialized = objectMapper.readValue(json, AuditLogFilterCriteria.class);

      assertThat(deserialized.getActorUserId()).isEqualTo(101L);
      assertThat(deserialized.getAuditActionCode()).isEqualTo("CREATE");
      assertThat(deserialized.getEntityType()).isEqualTo("Trainee");
      assertThat(deserialized.getEntityId()).isEqualTo(5001L);
      assertThat(deserialized.getCorrelationId()).isEqualTo("CORR-12345");
      assertThat(deserialized.getPage()).isEqualTo(1);
      assertThat(deserialized.getSize()).isEqualTo(50);
      assertThat(deserialized.getDirection()).isEqualTo("ASC");
    }

    @Test
    @DisplayName("Validation fails when constraints are violated")
    void testValidationViolations() {
      AuditLogFilterCriteria criteria = new AuditLogFilterCriteria();
      criteria.setActorUserId(-5L); // negative
      criteria.setSize(200); // exceeds max 100
      criteria.setDirection("INVALID_DIR"); // pattern violation
      criteria.setAuditActionCode("A".repeat(65)); // exceeds 64

      Set<ConstraintViolation<AuditLogFilterCriteria>> violations = validator.validate(criteria);
      assertThat(violations).hasSize(4);
    }

    @Test
    @DisplayName("hasValidDateRange detects inverted date ranges")
    void testDateRangeValidation() {
      AuditLogFilterCriteria criteria = new AuditLogFilterCriteria();
      LocalDateTime now = LocalDateTime.now();
      criteria.setStart(now);
      criteria.setEnd(now.minusDays(1)); // start after end
      assertThat(criteria.hasValidDateRange()).isFalse();

      criteria.setEnd(now.plusDays(1));
      assertThat(criteria.hasValidDateRange()).isTrue();
    }
  }

  @Nested
  @DisplayName("DataQualityScanRequest Tests")
  class DataQualityScanRequestTests {

    @Test
    @DisplayName("Valid scan request passes validation and serializes correctly")
    void testValidScanRequest() throws Exception {
      DataQualityScanRequest request = new DataQualityScanRequest(
          1L, "DQ_TRAINEE_AADHAAR_FMT", "Trainee", 1001L, 2L, 3L
      );

      Set<ConstraintViolation<DataQualityScanRequest>> violations = validator.validate(request);
      assertThat(violations).isEmpty();

      String json = objectMapper.writeValueAsString(request);
      DataQualityScanRequest deserialized = objectMapper.readValue(json, DataQualityScanRequest.class);
      assertThat(deserialized).isEqualTo(request);
      assertThat(deserialized.getRuleCode()).isEqualTo("DQ_TRAINEE_AADHAAR_FMT");
    }

    @Test
    @DisplayName("Negative IDs or excessive string lengths violate validation")
    void testValidationViolations() {
      DataQualityScanRequest request = new DataQualityScanRequest(
          -1L, "A".repeat(65), "B".repeat(65), -10L, -2L, -3L
      );

      Set<ConstraintViolation<DataQualityScanRequest>> violations = validator.validate(request);
      assertThat(violations).hasSize(6);
    }
  }

  @Nested
  @DisplayName("AssignDataQualityIssueRequest Tests")
  class AssignDataQualityIssueRequestTests {

    @Test
    @DisplayName("Valid assignment passes validation and serializes cleanly")
    void testValidAssignment() throws Exception {
      AssignDataQualityIssueRequest request = new AssignDataQualityIssueRequest(42L, "Please investigate duplicate PAN record.");

      Set<ConstraintViolation<AssignDataQualityIssueRequest>> violations = validator.validate(request);
      assertThat(violations).isEmpty();

      String json = objectMapper.writeValueAsString(request);
      AssignDataQualityIssueRequest deserialized = objectMapper.readValue(json, AssignDataQualityIssueRequest.class);
      assertThat(deserialized).isEqualTo(request);
      assertThat(deserialized.getAssignedUserId()).isEqualTo(42L);
      assertThat(deserialized.getRemarks()).isEqualTo("Please investigate duplicate PAN record.");
    }

    @Test
    @DisplayName("Null or non-positive assignedUserId fails validation")
    void testValidationViolations() {
      AssignDataQualityIssueRequest nullUser = new AssignDataQualityIssueRequest();
      Set<ConstraintViolation<AssignDataQualityIssueRequest>> violations = validator.validate(nullUser);
      assertThat(violations).isNotEmpty();

      AssignDataQualityIssueRequest negativeUser = new AssignDataQualityIssueRequest(-1L, "A".repeat(501));
      Set<ConstraintViolation<AssignDataQualityIssueRequest>> negativeViolations = validator.validate(negativeUser);
      assertThat(negativeViolations).hasSize(2);
    }
  }

  @Nested
  @DisplayName("ResolveDataQualityIssueRequest Tests")
  class ResolveDataQualityIssueRequestTests {

    @Test
    @DisplayName("Valid resolution passes validation and preserves backward-compatible defaults")
    void testValidResolution() throws Exception {
      LocalDateTime now = LocalDateTime.now();
      ResolveDataQualityIssueRequest request = new ResolveDataQualityIssueRequest(
          15L, "Corrected typographical error in trainee father name.", now, false
      );

      Set<ConstraintViolation<ResolveDataQualityIssueRequest>> violations = validator.validate(request);
      assertThat(violations).isEmpty();
      assertThat(request.getIsDismissed()).isFalse();

      String json = objectMapper.writeValueAsString(request);
      ResolveDataQualityIssueRequest deserialized = objectMapper.readValue(json, ResolveDataQualityIssueRequest.class);
      assertThat(deserialized.getResolvedByUserId()).isEqualTo(15L);
      assertThat(deserialized.getResolutionNotes()).isEqualTo("Corrected typographical error in trainee father name.");
      assertThat(deserialized.getIsDismissed()).isFalse();
    }

    @Test
    @DisplayName("Excessive notes length or negative user ID fails validation")
    void testValidationViolations() {
      ResolveDataQualityIssueRequest request = new ResolveDataQualityIssueRequest(
          -10L, "X".repeat(501), true
      );

      Set<ConstraintViolation<ResolveDataQualityIssueRequest>> violations = validator.validate(request);
      assertThat(violations).hasSize(2);
    }
  }

  @Nested
  @DisplayName("BulkImportInitiateRequest Tests")
  class BulkImportInitiateRequestTests {

    @Test
    @DisplayName("Valid import initiation passes validation and defaults dryRun to false")
    void testValidBulkImportRequest() throws Exception {
      BulkImportInitiateRequest request = new BulkImportInitiateRequest("STATE_PORTAL_CSV", "Trainee", false, "Monthly intake batch");

      Set<ConstraintViolation<BulkImportInitiateRequest>> violations = validator.validate(request);
      assertThat(violations).isEmpty();
      assertThat(request.getDryRun()).isFalse();

      String json = objectMapper.writeValueAsString(request);
      BulkImportInitiateRequest deserialized = objectMapper.readValue(json, BulkImportInitiateRequest.class);
      assertThat(deserialized.getSourceSystem()).isEqualTo("STATE_PORTAL_CSV");
      assertThat(deserialized.getEntityType()).isEqualTo("Trainee");
      assertThat(deserialized.getDryRun()).isFalse();
    }

    @Test
    @DisplayName("Blank required fields fail validation")
    void testValidationViolations() {
      BulkImportInitiateRequest request = new BulkImportInitiateRequest("", "   ", false, "X".repeat(501));

      Set<ConstraintViolation<BulkImportInitiateRequest>> violations = validator.validate(request);
      assertThat(violations).hasSize(3); // blank sourceSystem, blank entityType, notes > 500
    }
  }

  @Nested
  @DisplayName("OperationalEventFilterCriteria Tests")
  class OperationalEventFilterCriteriaTests {

    @Test
    @DisplayName("Default criteria initializes with safe pagination and sort defaults")
    void testDefaults() {
      OperationalEventFilterCriteria criteria = new OperationalEventFilterCriteria();
      assertThat(criteria.getPage()).isEqualTo(0);
      assertThat(criteria.getSize()).isEqualTo(20);
      assertThat(criteria.getSort()).isEqualTo("occurredAt");
      assertThat(criteria.getDirection()).isEqualTo("DESC");
      assertThat(criteria.hasValidDateRange()).isTrue();
    }

    @Test
    @DisplayName("Valid event filter serializes and validates cleanly")
    void testValidEventFilter() throws Exception {
      LocalDateTime now = LocalDateTime.now();
      OperationalEventFilterCriteria criteria = new OperationalEventFilterCriteria(
          "SECURITY", "HIGH", "SEC_AUTH_FAIL", 99L, "User", 99L,
          now.minusHours(24), now, "TRACE-9999", 0, 50, "occurredAt", "DESC"
      );

      Set<ConstraintViolation<OperationalEventFilterCriteria>> violations = validator.validate(criteria);
      assertThat(violations).isEmpty();
      assertThat(criteria.hasValidDateRange()).isTrue();

      String json = objectMapper.writeValueAsString(criteria);
      OperationalEventFilterCriteria deserialized = objectMapper.readValue(json, OperationalEventFilterCriteria.class);
      assertThat(deserialized.getCategoryCode()).isEqualTo("SECURITY");
      assertThat(deserialized.getSeverityCode()).isEqualTo("HIGH");
      assertThat(deserialized.getEventCode()).isEqualTo("SEC_AUTH_FAIL");
    }

    @Test
    @DisplayName("Invalid parameters trigger validation errors")
    void testValidationErrors() {
      OperationalEventFilterCriteria criteria = new OperationalEventFilterCriteria();
      criteria.setActorUserId(-10L);
      criteria.setSize(101);
      criteria.setDirection("FORWARD");

      Set<ConstraintViolation<OperationalEventFilterCriteria>> violations = validator.validate(criteria);
      assertThat(violations).hasSize(3);
    }
  }

  // =========================================================================
  // RESPONSE DTO TESTS
  // =========================================================================

  @Nested
  @DisplayName("AuditTrailTimelineResponse Tests")
  class AuditTrailTimelineResponseTests {

    @Test
    @DisplayName("Timeline response constructs correctly and entries list is unmodifiable")
    void testTimelineResponse() throws Exception {
      LocalDateTime now = LocalDateTime.now();
      AuditTrailEntry entry1 = new AuditTrailEntry(
          1L, 101L, "CREATE", "Create Record", now.minusHours(2), "CORR-1", "Initial record creation"
      );
      AuditTrailEntry entry2 = new AuditTrailEntry(
          2L, 102L, "UPDATE", "Update Status", now.minusHours(1), "CORR-2", "Status changed to ACTIVE"
      );

      AuditTrailTimelineResponse response = AuditTrailTimelineResponse.of("TrainingBatch", 501L, List.of(entry1, entry2));

      assertThat(response.entityType()).isEqualTo("TrainingBatch");
      assertThat(response.entityId()).isEqualTo(501L);
      assertThat(response.totalEntries()).isEqualTo(2);
      assertThat(response.timelineEntries()).hasSize(2);

      // Verify immutability
      assertThatThrownBy(() -> response.timelineEntries().add(entry1))
          .isInstanceOf(UnsupportedOperationException.class);

      String json = objectMapper.writeValueAsString(response);
      AuditTrailTimelineResponse deserialized = objectMapper.readValue(json, AuditTrailTimelineResponse.class);
      assertThat(deserialized.entityType()).isEqualTo("TrainingBatch");
      assertThat(deserialized.totalEntries()).isEqualTo(2);
      assertThat(deserialized.timelineEntries().get(0).actionCode()).isEqualTo("CREATE");
    }

    @Test
    @DisplayName("Null entries list is safely handled as empty immutable list")
    void testNullEntriesHandled() {
      AuditTrailTimelineResponse response = AuditTrailTimelineResponse.of("User", 1L, null);
      assertThat(response.timelineEntries()).isEmpty();
      assertThat(response.totalEntries()).isEqualTo(0);
    }
  }

  @Nested
  @DisplayName("DataQualityHealthScorecardResponse Tests")
  class DataQualityHealthScorecardResponseTests {

    @Test
    @DisplayName("Cleanliness score is normalized to scale 2 and serialized properly")
    void testHealthScorecard() throws Exception {
      LocalDateTime now = LocalDateTime.now();
      DataQualityHealthScorecardResponse response = new DataQualityHealthScorecardResponse(
          10000L, 25L, 2L, 5L, 10L, 8L,
          new BigDecimal("99.75432"), now
      );

      assertThat(response.evaluatedRecordCount()).isEqualTo(10000L);
      assertThat(response.totalActiveIssues()).isEqualTo(25L);
      assertThat(response.criticalIssueCount()).isEqualTo(2L);
      assertThat(response.cleanlinessScore()).isEqualTo(new BigDecimal("99.75"));

      String json = objectMapper.writeValueAsString(response);
      DataQualityHealthScorecardResponse deserialized = objectMapper.readValue(json, DataQualityHealthScorecardResponse.class);
      assertThat(deserialized.cleanlinessScore()).isEqualTo(new BigDecimal("99.75"));
      assertThat(deserialized.criticalIssueCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("Null cleanliness score defaults to ZERO with scale 2")
    void testNullScoreDefaultsToZero() {
      DataQualityHealthScorecardResponse response = new DataQualityHealthScorecardResponse(
          0L, 0L, 0L, 0L, 0L, 0L, null, LocalDateTime.now()
      );
      assertThat(response.cleanlinessScore()).isEqualTo(new BigDecimal("0.00"));
    }
  }

  @Nested
  @DisplayName("DataQualityRuleExecutionSummary Tests")
  class DataQualityRuleExecutionSummaryTests {

    @Test
    @DisplayName("Summary correctly represents rule execution outcome")
    void testRuleExecutionSummary() throws Exception {
      LocalDateTime now = LocalDateTime.now();
      DataQualityRuleExecutionSummary summary = new DataQualityRuleExecutionSummary(
          12L, "RULE_EMAIL_SYNTAX", "Trainee", 5000L, 14L, 10L, 4L, now, 1250L, "SUCCESS"
      );

      assertThat(summary.ruleId()).isEqualTo(12L);
      assertThat(summary.ruleCode()).isEqualTo("RULE_EMAIL_SYNTAX");
      assertThat(summary.evaluatedRecordCount()).isEqualTo(5000L);
      assertThat(summary.violationCount()).isEqualTo(14L);
      assertThat(summary.newIssuesCreated()).isEqualTo(10L);
      assertThat(summary.deduplicatedIssuesSkipped()).isEqualTo(4L);
      assertThat(summary.executionDurationMs()).isEqualTo(1250L);
      assertThat(summary.status()).isEqualTo("SUCCESS");

      String json = objectMapper.writeValueAsString(summary);
      DataQualityRuleExecutionSummary deserialized = objectMapper.readValue(json, DataQualityRuleExecutionSummary.class);
      assertThat(deserialized).isEqualTo(summary);
    }
  }

  @Nested
  @DisplayName("BulkImportSummaryResponse Tests")
  class BulkImportSummaryResponseTests {

    @Test
    @DisplayName("Summary correctly models batch ingestion results")
    void testBulkImportSummary() throws Exception {
      LocalDateTime start = LocalDateTime.now().minusMinutes(2);
      LocalDateTime end = LocalDateTime.now();
      BulkImportSummaryResponse summary = new BulkImportSummaryResponse(
          88L, "BATCH-202611-001", "PORTAL_SYNC", "Trainee", "COMPLETED",
          1500, 1480, 15, 5, start, end, 120000L
      );

      assertThat(summary.importBatchId()).isEqualTo(88L);
      assertThat(summary.batchCode()).isEqualTo("BATCH-202611-001");
      assertThat(summary.rowCount()).isEqualTo(1500);
      assertThat(summary.successCount()).isEqualTo(1480);
      assertThat(summary.failureCount()).isEqualTo(15);
      assertThat(summary.skippedCount()).isEqualTo(5);
      assertThat(summary.status()).isEqualTo("COMPLETED");

      String json = objectMapper.writeValueAsString(summary);
      BulkImportSummaryResponse deserialized = objectMapper.readValue(json, BulkImportSummaryResponse.class);
      assertThat(deserialized).isEqualTo(summary);
    }
  }

  @Nested
  @DisplayName("OperationalDiagnosticsSummaryResponse Tests")
  class OperationalDiagnosticsSummaryResponseTests {

    @Test
    @DisplayName("Diagnostics summary models time-window operational metrics safely")
    void testDiagnosticsSummary() throws Exception {
      LocalDateTime now = LocalDateTime.now();
      Map<String, Long> categoryMap = Map.of(
          "SECURITY", 12L,
          "IMPORT", 45L,
          "DATA_QUALITY", 8L,
          "SYSTEM", 110L
      );

      OperationalDiagnosticsSummaryResponse response = new OperationalDiagnosticsSummaryResponse(
          now.minusHours(6), now, 175L, 2L, 10L, 25L, 138L, categoryMap, now.minusSeconds(15)
      );

      assertThat(response.totalEvents()).isEqualTo(175L);
      assertThat(response.criticalEvents()).isEqualTo(2L);
      assertThat(response.errorEvents()).isEqualTo(10L);
      assertThat(response.warningEvents()).isEqualTo(25L);
      assertThat(response.infoEvents()).isEqualTo(138L);
      assertThat(response.categoryBreakdown()).containsEntry("SECURITY", 12L);

      // Verify unmodifiable map
      assertThatThrownBy(() -> response.categoryBreakdown().put("NEW", 1L))
          .isInstanceOf(UnsupportedOperationException.class);

      String json = objectMapper.writeValueAsString(response);
      OperationalDiagnosticsSummaryResponse deserialized = objectMapper.readValue(json, OperationalDiagnosticsSummaryResponse.class);
      assertThat(deserialized.totalEvents()).isEqualTo(175L);
      assertThat(deserialized.categoryBreakdown()).containsEntry("IMPORT", 45L);
    }

    @Test
    @DisplayName("Null categoryBreakdown defaults safely to empty map")
    void testNullBreakdownDefaultsToEmpty() {
      OperationalDiagnosticsSummaryResponse response = new OperationalDiagnosticsSummaryResponse(
          LocalDateTime.now().minusHours(1), LocalDateTime.now(), 0L, 0L, 0L, 0L, 0L, null, null
      );
      assertThat(response.categoryBreakdown()).isEmpty();
    }
  }
}
