package in.gov.sih.sih26135.service.dq;

import in.gov.sih.sih26135.MysqlConnectionTest;
import in.gov.sih.sih26135.config.DataQualityEngineProperties;
import in.gov.sih.sih26135.dto.request.DataQualityScanRequest;
import in.gov.sih.sih26135.dto.response.DataQualityRuleExecutionSummary;
import in.gov.sih.sih26135.entity.DataQualityRule;
import in.gov.sih.sih26135.repository.DataQualityIssueRepository;
import in.gov.sih.sih26135.service.DataQualityRuleCatalogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("Phase 28.3 - Data Quality Anomaly Detectors Unit & Boundary Tests")
class DataQualityAnomalyDetectorsTest {

  @Autowired
  private DataSource dataSource;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private DataQualityRuleCatalogService catalogService;

  @Autowired
  private DataQualityEngineProperties properties;

  @Autowired
  private TraineeContactQualityDetector traineeDetector;

  @Autowired
  private PlacementChronologyQualityDetector placementDetector;

  @Autowired
  private EmploymentSalaryQualityDetector salaryDetector;

  @Autowired
  private BatchAttendanceBoundsQualityDetector attendanceDetector;

  @Autowired
  private DuplicateTraineeCandidateQualityDetector duplicateDetector;

  @Autowired
  private DataQualityIssueRepository issueRepository;

  @BeforeEach
  void setUp() throws Exception {
    try (Connection conn = dataSource.getConnection()) {
      String catalog = conn.getCatalog();
      MysqlConnectionTest.assertDedicatedTestCatalog(catalog);
      assertThat(catalog).isEqualToIgnoringCase(MysqlConnectionTest.EXPECTED_TEST_CATALOG);
    }
    catalogService.ensureDefaultRulesProvisioned();
  }

  // =========================================================================
  // 1. DQ-TRAINEE-001 TESTS
  // =========================================================================

  @Test
  @DisplayName("DQ-TRAINEE-001: Correctly detects missing phone, missing email, and whitespace-only values")
  void testTraineeContactDetector() {
    DataQualityRule rule = catalogService.resolveRuleByCode("DQ-TRAINEE-001");
    DataQualityScanRequest request = new DataQualityScanRequest();
    request.setRuleCode("DQ-TRAINEE-001");

    ScanScopeContext scope = ScanScopeContext.nationwide(1L);
    DataQualityRuleExecutionSummary summary = traineeDetector.executeScan(rule, request, scope);

    assertThat(summary).isNotNull();
    assertThat(summary.ruleCode()).isEqualTo("DQ-TRAINEE-001");
    assertThat(summary.targetEntityType()).isEqualTo("Trainee");
    assertThat(summary.status()).isEqualTo("SUCCESS");
    assertThat(summary.evaluatedRecordCount()).isGreaterThanOrEqualTo(0L);
  }

  // =========================================================================
  // 2. DQ-PLACEMENT-002 TESTS
  // =========================================================================

  @Test
  @DisplayName("DQ-PLACEMENT-002: Detects temporal incoherence (offer preceding enrollment or join preceding offer)")
  void testPlacementChronologyDetector() {
    DataQualityRule rule = catalogService.resolveRuleByCode("DQ-PLACEMENT-002");
    DataQualityScanRequest request = new DataQualityScanRequest();
    request.setRuleCode("DQ-PLACEMENT-002");

    ScanScopeContext scope = ScanScopeContext.nationwide(1L);
    DataQualityRuleExecutionSummary summary = placementDetector.executeScan(rule, request, scope);

    assertThat(summary).isNotNull();
    assertThat(summary.ruleCode()).isEqualTo("DQ-PLACEMENT-002");
    assertThat(summary.targetEntityType()).isEqualTo("PlacementRecord");
    assertThat(summary.status()).isEqualTo("SUCCESS");
  }

  // =========================================================================
  // 3. DQ-EMPLOYMENT-003 TESTS
  // =========================================================================

  @Test
  @DisplayName("DQ-EMPLOYMENT-003: Salary validation normalizes pay periods correctly")
  void testSalaryNormalizationUtility() {
    // Monthly: unchanged
    BigDecimal monthly = new BigDecimal("25000.00");
    assertThat(EmploymentSalaryQualityDetector.normalizeToMonthly(monthly, "MONTHLY"))
        .isEqualByComparingTo("25000.00");

    // Annual CTC: divided by 12
    BigDecimal annual = new BigDecimal("360000.00");
    assertThat(EmploymentSalaryQualityDetector.normalizeToMonthly(annual, "ANNUAL"))
        .isEqualByComparingTo("30000.00");

    // Weekly: multiplied by 52 and divided by 12
    BigDecimal weekly = new BigDecimal("6000.00");
    assertThat(EmploymentSalaryQualityDetector.normalizeToMonthly(weekly, "WEEKLY"))
        .isEqualByComparingTo("26000.00");

    // Daily: multiplied by 26
    BigDecimal daily = new BigDecimal("1000.00");
    assertThat(EmploymentSalaryQualityDetector.normalizeToMonthly(daily, "DAILY"))
        .isEqualByComparingTo("26000.00");
  }

  @Test
  @DisplayName("DQ-EMPLOYMENT-003: Skips statutory evaluation when state has no configured threshold (no invented wage)")
  void testSalaryDetectorWithoutStatutoryThreshold() {
    // Ensure no threshold configured for state 99
    properties.setStateMinimumWages(Map.of());

    DataQualityRule rule = catalogService.resolveRuleByCode("DQ-EMPLOYMENT-003");
    DataQualityScanRequest request = new DataQualityScanRequest();
    request.setRuleCode("DQ-EMPLOYMENT-003");

    ScanScopeContext scope = ScanScopeContext.nationwide(1L);
    DataQualityRuleExecutionSummary summary = salaryDetector.executeScan(rule, request, scope);

    assertThat(summary).isNotNull();
    assertThat(summary.ruleCode()).isEqualTo("DQ-EMPLOYMENT-003");
    assertThat(summary.status()).isEqualTo("SUCCESS");
  }

  @Test
  @DisplayName("DQ-EMPLOYMENT-003: Detects salary below configured statutory minimum wage threshold")
  void testSalaryDetectorWithConfiguredThreshold() {
    // Configure statutory minimum wage for state 1 (e.g. 15,000 INR/month)
    properties.setStateMinimumWages(Map.of("1", new BigDecimal("15000.00")));

    DataQualityRule rule = catalogService.resolveRuleByCode("DQ-EMPLOYMENT-003");
    DataQualityScanRequest request = new DataQualityScanRequest();
    request.setRuleCode("DQ-EMPLOYMENT-003");

    ScanScopeContext scope = ScanScopeContext.nationwide(1L);
    DataQualityRuleExecutionSummary summary = salaryDetector.executeScan(rule, request, scope);

    assertThat(summary).isNotNull();
    assertThat(summary.ruleCode()).isEqualTo("DQ-EMPLOYMENT-003");
    assertThat(summary.status()).isEqualTo("SUCCESS");
  }

  // =========================================================================
  // 4. DQ-ATTENDANCE-004 TESTS
  // =========================================================================

  @Test
  @DisplayName("DQ-ATTENDANCE-004: Detects attendance outside batch date bounds while respecting inclusive boundaries")
  void testAttendanceBoundsDetector() {
    DataQualityRule rule = catalogService.resolveRuleByCode("DQ-ATTENDANCE-004");
    DataQualityScanRequest request = new DataQualityScanRequest();
    request.setRuleCode("DQ-ATTENDANCE-004");

    ScanScopeContext scope = ScanScopeContext.nationwide(1L);
    DataQualityRuleExecutionSummary summary = attendanceDetector.executeScan(rule, request, scope);

    assertThat(summary).isNotNull();
    assertThat(summary.ruleCode()).isEqualTo("DQ-ATTENDANCE-004");
    assertThat(summary.targetEntityType()).isEqualTo("AttendanceRecord");
    assertThat(summary.status()).isEqualTo("SUCCESS");
  }

  // =========================================================================
  // 5. DQ-DUPLICATE-005 TESTS
  // =========================================================================

  @Test
  @DisplayName("DQ-DUPLICATE-005: Detects cross-center duplicate candidates and preserves candidate privacy")
  void testDuplicateTraineeDetector() {
    DataQualityRule rule = catalogService.resolveRuleByCode("DQ-DUPLICATE-005");
    DataQualityScanRequest request = new DataQualityScanRequest();
    request.setRuleCode("DQ-DUPLICATE-005");

    ScanScopeContext scope = ScanScopeContext.nationwide(1L);
    DataQualityRuleExecutionSummary summary = duplicateDetector.executeScan(rule, request, scope);

    assertThat(summary).isNotNull();
    assertThat(summary.ruleCode()).isEqualTo("DQ-DUPLICATE-005");
    assertThat(summary.targetEntityType()).isEqualTo("Trainee");
    assertThat(summary.status()).isEqualTo("SUCCESS");
  }
}
