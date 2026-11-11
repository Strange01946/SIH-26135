package in.gov.sih.sih26135.service.dq;

import in.gov.sih.sih26135.MysqlConnectionTest;
import in.gov.sih.sih26135.config.DataQualityEngineProperties;
import in.gov.sih.sih26135.dto.request.DataQualityScanRequest;
import in.gov.sih.sih26135.dto.response.DataQualityRuleExecutionSummary;
import in.gov.sih.sih26135.entity.DataQualityRule;
import in.gov.sih.sih26135.repository.DataQualityIssueRepository;
import in.gov.sih.sih26135.service.DataQualityRuleCatalogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
  private DataQualityIssueRegistrationService registrationService;

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

  @AfterEach
  void tearDown() {
    cleanUpDuplicateTraineeFixtures();
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
  @DisplayName("DQ-EMPLOYMENT-003: Salary validation normalizes pay periods correctly and handles unsupported frequency safely")
  void testSalaryNormalizationUtility() {
    // Monthly: unchanged
    BigDecimal monthly = new BigDecimal("25000.00");
    assertThat(EmploymentSalaryQualityDetector.normalizeToMonthly(monthly, "MONTHLY"))
        .isEqualByComparingTo("25000.00");

    // Annual CTC: divided by 12
    BigDecimal annual = new BigDecimal("360000.00");
    assertThat(EmploymentSalaryQualityDetector.normalizeToMonthly(annual, "ANNUAL"))
        .isEqualByComparingTo("30000.00");

    // Yearly: divided by 12
    BigDecimal yearly = new BigDecimal("240000.00");
    assertThat(EmploymentSalaryQualityDetector.normalizeToMonthly(yearly, "YEARLY"))
        .isEqualByComparingTo("20000.00");

    // Weekly: multiplied by 52 and divided by 12
    BigDecimal weekly = new BigDecimal("6000.00");
    assertThat(EmploymentSalaryQualityDetector.normalizeToMonthly(weekly, "WEEKLY"))
        .isEqualByComparingTo("26000.00");

    // Daily: multiplied by 26
    BigDecimal daily = new BigDecimal("1000.00");
    assertThat(EmploymentSalaryQualityDetector.normalizeToMonthly(daily, "DAILY"))
        .isEqualByComparingTo("26000.00");

    // Hourly: multiplied by 208 (8 hours/day * 26 days/month)
    BigDecimal hourly = new BigDecimal("100.00");
    assertThat(EmploymentSalaryQualityDetector.normalizeToMonthly(hourly, "HOURLY"))
        .isEqualByComparingTo("20800.00");

    // Unsupported frequency codes safely return null rather than defaulting to MONTHLY
    assertThat(EmploymentSalaryQualityDetector.normalizeToMonthly(new BigDecimal("25000.00"), "PIECEWORK")).isNull();
    assertThat(EmploymentSalaryQualityDetector.normalizeToMonthly(new BigDecimal("25000.00"), "")).isNull();
    assertThat(EmploymentSalaryQualityDetector.normalizeToMonthly(new BigDecimal("25000.00"), "   ")).isNull();
    assertThat(EmploymentSalaryQualityDetector.normalizeToMonthly(new BigDecimal("25000.00"), null)).isNull();
    assertThat(EmploymentSalaryQualityDetector.normalizeToMonthly(null, "MONTHLY")).isEqualTo(BigDecimal.ZERO);
  }

  @Test
  @DisplayName("DQ-EMPLOYMENT-003: Skips statutory evaluation when state has no configured threshold (no invented wage)")
  void testSalaryDetectorWithoutStatutoryThreshold() {
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
  // 5. DQ-DUPLICATE-005 & CROSS-CUTTING DETECTOR TESTS
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

  @Test
  @DisplayName("DQ-DUPLICATE-005: Preserves >5 candidate IDs in aggregate issue, enforces tenant isolation, and deduplicates repeated scans")
  void testDuplicateDetectorBoundedPaginationAndAggregateStrategyWithManyCandidates() {
    setupDuplicateTraineeFixtures();

    DataQualityRule rule = catalogService.resolveRuleByCode("DQ-DUPLICATE-005");
    DataQualityScanRequest request = new DataQualityScanRequest();
    request.setRuleCode("DQ-DUPLICATE-005");
    request.setEntityId(888701L);

    // Scan as State 8871 Admin: strictly isolates to State 8871
    ScanScopeContext scope = ScanScopeContext.stateScoped(10L, 8871L);
    DataQualityRuleExecutionSummary summary = duplicateDetector.executeScan(rule, request, scope);

    assertThat(summary.status()).isEqualTo("SUCCESS");
    // 7 candidate matches in state 8871 (exceeds 5-candidate limit!)
    assertThat(summary.violationCount()).isEqualTo(7L);
    assertThat(summary.newIssuesCreated()).isEqualTo(1L);
    assertThat(summary.deduplicatedIssuesSkipped()).isEqualTo(0L);

    // Verify aggregate issue in DB
    String issueSummary = jdbcTemplate.queryForObject(
        "SELECT issue_summary FROM data_quality_issues WHERE data_quality_rule_id = ? AND entity_type = 'Trainee' AND entity_id = ?",
        String.class, rule.getId(), 888701L
    );
    assertThat(issueSummary).isNotNull();
    assertThat(issueSummary).contains("888702", "888703", "888704", "888705", "888706", "888707", "888708");
    // Cross-state candidate 888709 in State 8872 must NOT be inspected or matched (tenant isolation)
    assertThat(issueSummary).doesNotContain("888709");
    // Raw PII must NOT be leaked in issue summary
    assertThat(issueSummary).doesNotContain("9876543210");
    assertThat(issueSummary).doesNotContain("1995-05-15");

    // Repeated scan on same scope must detect existing open issue and skip without duplicating
    DataQualityRuleExecutionSummary repeatSummary = duplicateDetector.executeScan(rule, request, scope);
    assertThat(repeatSummary.violationCount()).isEqualTo(7L);
    assertThat(repeatSummary.newIssuesCreated()).isEqualTo(0L);
    assertThat(repeatSummary.deduplicatedIssuesSkipped()).isEqualTo(1L);
  }

  @Test
  @DisplayName("DQ-DUPLICATE-005: More than 50 candidate matches accurately reports count and states schema truncation disclaimer")
  void testMoreThan50DuplicateCandidatesAndRepresentationLimit() {
    setupLargeDuplicateTraineeFixtures(55);

    DataQualityRule rule = catalogService.resolveRuleByCode("DQ-DUPLICATE-005");
    DataQualityScanRequest request = new DataQualityScanRequest();
    request.setRuleCode("DQ-DUPLICATE-005");
    request.setEntityId(888701L);

    ScanScopeContext scope = ScanScopeContext.nationwide(1L);
    DataQualityRuleExecutionSummary summary = duplicateDetector.executeScan(rule, request, scope);

    assertThat(summary.status()).isEqualTo("SUCCESS");
    assertThat(summary.violationCount()).isEqualTo(55L);
    assertThat(summary.newIssuesCreated()).isEqualTo(1L);
    assertThat(summary.deduplicatedIssuesSkipped()).isEqualTo(0L);

    String issueSummary = jdbcTemplate.queryForObject(
        "SELECT issue_summary FROM data_quality_issues WHERE data_quality_rule_id = ? AND entity_type = 'Trainee' AND entity_id = 888701",
        String.class, rule.getId()
    );
    assertThat(issueSummary).isNotNull();
    // Invariant: issue_summary MUST fit within VARCHAR(500)
    assertThat(issueSummary.length()).isLessThanOrEqualTo(500);

    // Accurately reports total candidate count without claiming complete ID enumeration
    assertThat(issueSummary).contains("Total matching candidates detected: 55");
    // Explicitly states schema representation limitation
    assertThat(issueSummary).contains("Representation limit reached (schema max 500 chars): complete candidate enumeration truncated");
    assertThat(issueSummary).endsWith("]...");

    // Raw PII must never be exposed
    assertThat(issueSummary).doesNotContain("9876543210");
    assertThat(issueSummary).doesNotContain("1995-05-15");
  }

  @Test
  @DisplayName("DQ-DUPLICATE-005: Newly discovered candidate after OPEN issue already exists updates issue summary without losing relationships")
  void testNewlyDiscoveredCandidateAfterOpenIssueExistsUpdatesSummary() {
    // 1. Initial setup with exactly 1 candidate (888702)
    setupLargeDuplicateTraineeFixtures(1);

    DataQualityRule rule = catalogService.resolveRuleByCode("DQ-DUPLICATE-005");
    DataQualityScanRequest request = new DataQualityScanRequest();
    request.setRuleCode("DQ-DUPLICATE-005");
    request.setEntityId(888701L);

    ScanScopeContext scope = ScanScopeContext.nationwide(1L);
    DataQualityRuleExecutionSummary summary1 = duplicateDetector.executeScan(rule, request, scope);

    assertThat(summary1.status()).isEqualTo("SUCCESS");
    assertThat(summary1.newIssuesCreated()).isEqualTo(1L);
    assertThat(summary1.deduplicatedIssuesSkipped()).isEqualTo(0L);

    String initialSummary = jdbcTemplate.queryForObject(
        "SELECT issue_summary FROM data_quality_issues WHERE data_quality_rule_id = ? AND entity_type = 'Trainee' AND entity_id = 888701",
        String.class, rule.getId()
    );
    assertThat(initialSummary).contains("888702");
    assertThat(initialSummary).doesNotContain("888703");

    // 2. A new duplicate candidate (888703) is enrolled in Center 8872
    jdbcTemplate.update("INSERT INTO trainees (trainee_id, registration_number, first_name, last_name, phone, email, date_of_birth, gender_id, state_id, district_id, current_employment_status_id, profile_status_id, created_at, updated_at) " +
        "VALUES (888703, 'REG-DQ-888703', 'Aditya', 'Sharma', '9876543210', 'aditya888703@example.gov.in', '1995-05-15', 1, 8871, 8871, 1, 1, NOW(), NOW())");
    jdbcTemplate.update("INSERT INTO training_enrollments (enrollment_id, enrollment_number, trainee_id, program_id, course_id, provider_id, center_id, batch_id, enrollment_date, enrollment_status_id, created_at, updated_at) " +
        "VALUES (888703, 'ENR-DQ-888703', 888703, 8871, 8871, 8871, 8872, 8872, '2026-01-01', 1, NOW(), NOW())");

    // 3. Re-run scan: must not create a duplicate row, but MUST update existing OPEN issue with new candidate 888703
    DataQualityRuleExecutionSummary summary2 = duplicateDetector.executeScan(rule, request, scope);

    assertThat(summary2.status()).isEqualTo("SUCCESS");
    assertThat(summary2.violationCount()).isEqualTo(2L);
    assertThat(summary2.newIssuesCreated()).isEqualTo(0L);
    assertThat(summary2.deduplicatedIssuesSkipped()).isEqualTo(1L);

    Integer issueRowCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM data_quality_issues WHERE data_quality_rule_id = ? AND entity_type = 'Trainee' AND entity_id = 888701",
        Integer.class, rule.getId()
    );
    assertThat(issueRowCount).isEqualTo(1);

    String updatedSummary = jdbcTemplate.queryForObject(
        "SELECT issue_summary FROM data_quality_issues WHERE data_quality_rule_id = ? AND entity_type = 'Trainee' AND entity_id = 888701",
        String.class, rule.getId()
    );
    // Newly discovered candidate 888703 must NOT be lost!
    assertThat(updatedSummary).contains("888702");
    assertThat(updatedSummary).contains("888703");
    assertThat(updatedSummary).contains("Total matching candidates detected: 2");
  }

  @Test
  @DisplayName("DQ-DUPLICATE-005: Candidate SQL query failure does not produce a misleading SUCCESS summary and stops scan")
  void testCandidateQueryFailureDoesNotProduceMisleadingSuccessSummary() {
    DataQualityRule rule = catalogService.resolveRuleByCode("DQ-DUPLICATE-005");
    DataQualityScanRequest request = new DataQualityScanRequest();
    request.setRuleCode("DQ-DUPLICATE-005");

    JdbcTemplate mockJdbc = mock(JdbcTemplate.class);
    // 1. First call for chunk returns 1 trainee
    when(mockJdbc.queryForList(anyString(), any(Object[].class)))
        .thenReturn(List.of(Map.of(
            "trainee_id", 888701L,
            "phone", "9876543210",
            "email", "candidate.test@example.gov.in",
            "first_name", "Test",
            "last_name", "User"
        )));

    // 2. Candidate duplicate lookup query fails with SQL error
    when(mockJdbc.queryForList(anyString(), eq(Long.class), any(Object[].class)))
        .thenThrow(new BadSqlGrammarException("Candidate query failed", "SELECT ...", new SQLException("Simulated database table corruption")));

    DuplicateTraineeCandidateQualityDetector failingDetector = new DuplicateTraineeCandidateQualityDetector(
        mockJdbc,
        registrationService,
        properties
    );

    ScanScopeContext scope = ScanScopeContext.nationwide(1L);
    DataQualityRuleExecutionSummary summary = failingDetector.executeScan(rule, request, scope);

    assertThat(summary).isNotNull();
    assertThat(summary.status()).isEqualTo("FAILED");
    assertThat(summary.status()).isNotEqualTo("SUCCESS");
    // Evaluated count must NOT claim record was successfully evaluated
    assertThat(summary.evaluatedRecordCount()).isEqualTo(0L);
    assertThat(summary.newIssuesCreated()).isEqualTo(0L);
    assertThat(summary.deduplicatedIssuesSkipped()).isEqualTo(0L);
  }

  @Test
  @DisplayName("All detectors reject employer persona on unauthorized domain entities with AccessDeniedException")
  void testDetectorsRejectEmployerOnUnauthorizedRules() {
    ScanScopeContext employerScope = ScanScopeContext.employerScoped(10L, 8871L);

    DataQualityRule attendanceRule = catalogService.resolveRuleByCode("DQ-ATTENDANCE-004");
    assertThatThrownBy(() -> attendanceDetector.executeScan(attendanceRule, null, employerScope))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("Employers are not authorized");

    DataQualityRule traineeRule = catalogService.resolveRuleByCode("DQ-TRAINEE-001");
    assertThatThrownBy(() -> traineeDetector.executeScan(traineeRule, null, employerScope))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("Employers are not authorized");

    DataQualityRule duplicateRule = catalogService.resolveRuleByCode("DQ-DUPLICATE-005");
    assertThatThrownBy(() -> duplicateDetector.executeScan(duplicateRule, null, employerScope))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("Employers are not authorized");
  }

  @Test
  @DisplayName("All detectors fail closed with AccessDeniedException when non-nationwide caller has null scope ID")
  void testDetectorsRejectNullScopeIdOnNonNationwideScans() {
    ScanScopeContext nullStateScope = new ScanScopeContext(10L, false, true, null, false, null, false, null);
    ScanScopeContext nullProviderScope = new ScanScopeContext(10L, false, false, null, true, null, false, null);
    ScanScopeContext nullEmployerScope = new ScanScopeContext(10L, false, false, null, false, null, true, null);

    DataQualityRule traineeRule = catalogService.resolveRuleByCode("DQ-TRAINEE-001");
    assertThatThrownBy(() -> traineeDetector.executeScan(traineeRule, null, nullStateScope))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> traineeDetector.executeScan(traineeRule, null, nullProviderScope))
        .isInstanceOf(AccessDeniedException.class);

    DataQualityRule placementRule = catalogService.resolveRuleByCode("DQ-PLACEMENT-002");
    assertThatThrownBy(() -> placementDetector.executeScan(placementRule, null, nullStateScope))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> placementDetector.executeScan(placementRule, null, nullProviderScope))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> placementDetector.executeScan(placementRule, null, nullEmployerScope))
        .isInstanceOf(AccessDeniedException.class);

    DataQualityRule salaryRule = catalogService.resolveRuleByCode("DQ-EMPLOYMENT-003");
    assertThatThrownBy(() -> salaryDetector.executeScan(salaryRule, null, nullStateScope))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> salaryDetector.executeScan(salaryRule, null, nullProviderScope))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> salaryDetector.executeScan(salaryRule, null, nullEmployerScope))
        .isInstanceOf(AccessDeniedException.class);

    DataQualityRule attendanceRule = catalogService.resolveRuleByCode("DQ-ATTENDANCE-004");
    assertThatThrownBy(() -> attendanceDetector.executeScan(attendanceRule, null, nullStateScope))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> attendanceDetector.executeScan(attendanceRule, null, nullProviderScope))
        .isInstanceOf(AccessDeniedException.class);

    DataQualityRule duplicateRule = catalogService.resolveRuleByCode("DQ-DUPLICATE-005");
    assertThatThrownBy(() -> duplicateDetector.executeScan(duplicateRule, null, nullStateScope))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> duplicateDetector.executeScan(duplicateRule, null, nullProviderScope))
        .isInstanceOf(AccessDeniedException.class);
  }

  // =========================================================================
  // FIXTURE MANAGEMENT
  // =========================================================================

  private void setupDuplicateTraineeFixtures() {
    setupLargeDuplicateTraineeFixtures(7);

    // Cross-state candidate T9 (888709) in State 8872 with identical phone and DOB for isolation test
    jdbcTemplate.update("INSERT INTO states (state_id, state_code, state_name, lifecycle_status_id, created_at, updated_at) " +
        "VALUES (8872, 'S2', 'DQ State 2', 1, NOW(), NOW())");
    jdbcTemplate.update("INSERT INTO districts (district_id, state_id, district_code, district_name, lifecycle_status_id, created_at, updated_at) " +
        "VALUES (8872, 8872, 'D8872', 'DQ District 2', 1, NOW(), NOW())");
    jdbcTemplate.update("INSERT INTO training_centers (center_id, provider_id, center_code, center_name, state_id, district_id, lifecycle_status_id, created_at, updated_at) " +
        "VALUES (8873, 8871, 'CTR-DQ-8873', 'Center 8873', 8872, 8872, 1, NOW(), NOW())");
    jdbcTemplate.update("INSERT INTO training_batches (batch_id, batch_code, course_id, provider_id, center_id, program_id, start_date, capacity, batch_status_id, created_at, updated_at) " +
        "VALUES (8873, 'BAT-DQ-8873', 8871, 8871, 8873, 8871, '2026-01-01', 30, 1, NOW(), NOW())");

    jdbcTemplate.update("INSERT INTO trainees (trainee_id, registration_number, first_name, last_name, phone, email, date_of_birth, gender_id, state_id, district_id, current_employment_status_id, profile_status_id, created_at, updated_at) " +
        "VALUES (888709, 'REG-DQ-888709', 'Aditya', 'Sharma', '9876543210', 'aditya888709@example.gov.in', '1995-05-15', 1, 8872, 8872, 1, 1, NOW(), NOW())");
    jdbcTemplate.update("INSERT INTO training_enrollments (enrollment_id, enrollment_number, trainee_id, program_id, course_id, provider_id, center_id, batch_id, enrollment_date, enrollment_status_id, created_at, updated_at) " +
        "VALUES (888709, 'ENR-DQ-888709', 888709, 8871, 8871, 8871, 8873, 8873, '2026-01-01', 1, NOW(), NOW())");
  }

  private void setupLargeDuplicateTraineeFixtures(int candidateCount) {
    cleanUpDuplicateTraineeFixtures();

    // 1. States & Districts
    jdbcTemplate.update("INSERT INTO states (state_id, state_code, state_name, lifecycle_status_id, created_at, updated_at) " +
        "VALUES (8871, 'S1', 'DQ State 1', 1, NOW(), NOW())");
    jdbcTemplate.update("INSERT INTO districts (district_id, state_id, district_code, district_name, lifecycle_status_id, created_at, updated_at) " +
        "VALUES (8871, 8871, 'D8871', 'DQ District 1', 1, NOW(), NOW())");

    // 2. Training Provider & Centers
    jdbcTemplate.update("INSERT INTO training_providers (provider_id, provider_code, provider_name, registration_number, organization_type_id, state_id, district_id, accreditation_status_id, lifecycle_status_id, created_at, updated_at) " +
        "VALUES (8871, 'PRV-DQ-8871', 'DQ Provider 8871', 'REG-PRV-8871', 1, 8871, 8871, 1, 1, NOW(), NOW())");

    jdbcTemplate.update("INSERT INTO training_centers (center_id, provider_id, center_code, center_name, state_id, district_id, lifecycle_status_id, created_at, updated_at) " +
        "VALUES (8871, 8871, 'CTR-DQ-8871', 'Center 8871', 8871, 8871, 1, NOW(), NOW())");
    jdbcTemplate.update("INSERT INTO training_centers (center_id, provider_id, center_code, center_name, state_id, district_id, lifecycle_status_id, created_at, updated_at) " +
        "VALUES (8872, 8871, 'CTR-DQ-8872', 'Center 8872', 8871, 8871, 1, NOW(), NOW())");

    // 3. Organization, Department, Scheme, Program, Sector, Course, Batch
    jdbcTemplate.update("INSERT INTO organizations (organization_id, organization_code, organization_name, organization_type_id, lifecycle_status_id, created_at, updated_at) " +
        "VALUES (8871, 'ORG-DQ-8871', 'DQ Org 8871', 1, 1, NOW(), NOW())");
    jdbcTemplate.update("INSERT INTO departments (department_id, organization_id, department_code, department_name, lifecycle_status_id, created_at, updated_at) " +
        "VALUES (8871, 8871, 'DEP-DQ-8871', 'DQ Dept 8871', 1, NOW(), NOW())");
    jdbcTemplate.update("INSERT INTO schemes (scheme_id, scheme_code, scheme_name, department_id, currency_code, lifecycle_status_id, created_at, updated_at) " +
        "VALUES (8871, 'SCH-DQ-8871', 'DQ Scheme 8871', 8871, 'INR', 1, NOW(), NOW())");
    jdbcTemplate.update("INSERT INTO programs (program_id, program_code, program_name, department_id, scheme_id, lifecycle_status_id, created_at, updated_at) " +
        "VALUES (8871, 'PRG-DQ-8871', 'DQ Program 8871', 8871, 8871, 1, NOW(), NOW())");
    jdbcTemplate.update("INSERT INTO sectors (sector_id, sector_code, sector_name, lifecycle_status_id, created_at, updated_at) " +
        "VALUES (8871, 'SEC-DQ-8871', 'DQ Sector 8871', 1, NOW(), NOW())");
    jdbcTemplate.update("INSERT INTO courses (course_id, course_code, course_name, sector_id, delivery_mode_id, duration_hours, lifecycle_status_id, created_at, updated_at) " +
        "VALUES (8871, 'CRS-DQ-8871', 'DQ Course 8871', 8871, 1, 120, 1, NOW(), NOW())");

    jdbcTemplate.update("INSERT INTO training_batches (batch_id, batch_code, course_id, provider_id, center_id, program_id, start_date, capacity, batch_status_id, created_at, updated_at) " +
        "VALUES (8871, 'BAT-DQ-8871', 8871, 8871, 8871, 8871, '2026-01-01', 30, 1, NOW(), NOW())");
    jdbcTemplate.update("INSERT INTO training_batches (batch_id, batch_code, course_id, provider_id, center_id, program_id, start_date, capacity, batch_status_id, created_at, updated_at) " +
        "VALUES (8872, 'BAT-DQ-8872', 8871, 8871, 8872, 8871, '2026-01-01', 100, 1, NOW(), NOW())");

    // 4. Primary Trainee T1 (888701) in State 8871
    jdbcTemplate.update("INSERT INTO trainees (trainee_id, registration_number, first_name, last_name, phone, email, date_of_birth, gender_id, state_id, district_id, current_employment_status_id, profile_status_id, created_at, updated_at) " +
        "VALUES (888701, 'REG-DQ-888701', 'Aditya', 'Sharma', '9876543210', 'aditya.sharma@example.gov.in', '1995-05-15', 1, 8871, 8871, 1, 1, NOW(), NOW())");

    // T1 enrolled in Center 8871
    jdbcTemplate.update("INSERT INTO training_enrollments (enrollment_id, enrollment_number, trainee_id, program_id, course_id, provider_id, center_id, batch_id, enrollment_date, enrollment_status_id, created_at, updated_at) " +
        "VALUES (888701, 'ENR-DQ-888701', 888701, 8871, 8871, 8871, 8871, 8871, '2026-01-01', 1, NOW(), NOW())");

    // 5. Seed Candidate Trainees (888702 .. 888701 + candidateCount) with matching phone & DOB
    for (int i = 1; i <= candidateCount; i++) {
      long tid = 888701L + i;
      jdbcTemplate.update("INSERT INTO trainees (trainee_id, registration_number, first_name, last_name, phone, email, date_of_birth, gender_id, state_id, district_id, current_employment_status_id, profile_status_id, created_at, updated_at) " +
          "VALUES (?, ?, 'Aditya', 'Sharma', '9876543210', ?, '1995-05-15', 1, 8871, 8871, 1, 1, NOW(), NOW())",
          tid, "REG-DQ-" + tid, "aditya" + tid + "@example.gov.in");

      // Candidate enrolled in Center 8872 (Center 8872 != 8871!)
      jdbcTemplate.update("INSERT INTO training_enrollments (enrollment_id, enrollment_number, trainee_id, program_id, course_id, provider_id, center_id, batch_id, enrollment_date, enrollment_status_id, created_at, updated_at) " +
          "VALUES (?, ?, ?, 8871, 8871, 8871, 8872, 8872, '2026-01-01', 1, NOW(), NOW())",
          tid, "ENR-DQ-" + tid, tid);
    }
  }

  private void cleanUpDuplicateTraineeFixtures() {
    jdbcTemplate.update("DELETE FROM data_quality_issues WHERE entity_id BETWEEN 888701 AND 888799 AND entity_type = 'Trainee'");
    jdbcTemplate.update("DELETE FROM training_enrollments WHERE enrollment_id BETWEEN 888701 AND 888799");
    jdbcTemplate.update("DELETE FROM trainees WHERE trainee_id BETWEEN 888701 AND 888799");
    jdbcTemplate.update("DELETE FROM training_batches WHERE batch_id IN (8871, 8872, 8873)");
    jdbcTemplate.update("DELETE FROM training_centers WHERE center_id IN (8871, 8872, 8873)");
    jdbcTemplate.update("DELETE FROM training_providers WHERE provider_id = 8871");
    jdbcTemplate.update("DELETE FROM programs WHERE program_id = 8871");
    jdbcTemplate.update("DELETE FROM schemes WHERE scheme_id = 8871");
    jdbcTemplate.update("DELETE FROM departments WHERE department_id = 8871");
    jdbcTemplate.update("DELETE FROM organizations WHERE organization_id = 8871");
    jdbcTemplate.update("DELETE FROM courses WHERE course_id = 8871");
    jdbcTemplate.update("DELETE FROM sectors WHERE sector_id = 8871");
    jdbcTemplate.update("DELETE FROM districts WHERE district_id IN (8871, 8872)");
    jdbcTemplate.update("DELETE FROM states WHERE state_id IN (8871, 8872)");
  }
}
