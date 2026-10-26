package in.gov.sih.sih26135.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.gov.sih.sih26135.MysqlConnectionTest;
import in.gov.sih.sih26135.repository.analytics.TraineeOutcomeSummaryRepository;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Phase 25.14 — Business Workflow Read Model Integration Tests (SIH26135_TEST)")
public class BusinessWorkflowReadModelIntegrationTest {

  @Autowired
  private DataSource dataSource;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private TraineeOutcomeSummaryRepository traineeOutcomeSummaryRepository;

  // The 21 analytical views explicitly declared in database/phase-16-analytical-views.sql
  private final List<String> expectedAnalyticsViewNames = List.of(
      "vw_enrollment_outcome_fact",
      "vw_placement_fact",
      "vw_employment_fact",
      "vw_salary_progression_fact",
      "vw_employment_retention_fact",
      "vw_skill_gap_fact",
      "vw_unemployment_fact",
      "vw_employment_exit_fact",
      "vw_followup_fact",
      "vw_survey_response_fact",
      "vw_employment_verification_fact",
      "vw_trainee_outcome_summary",
      "vw_program_outcome_summary",
      "vw_provider_outcome_summary",
      "vw_course_outcome_summary",
      "vw_district_outcome_summary",
      "vw_employer_hiring_summary",
      "vw_unemployment_reason_summary",
      "vw_attrition_reason_summary",
      "vw_skill_gap_summary",
      "vw_followup_outcome_summary"
  );

  @BeforeEach
  void setUp() throws Exception {
    try (Connection connection = dataSource.getConnection()) {
      MysqlConnectionTest.assertDedicatedTestCatalog(connection.getCatalog());
    }
  }

  @Test
  @DisplayName("1. Catalog Isolation Guard confirms test connection strictly targets SIH26135_TEST")
  void testCatalogIsolationGuard() throws Exception {
    try (Connection connection = dataSource.getConnection()) {
      String catalog = connection.getCatalog();
      assertThat(catalog).isEqualToIgnoringCase(MysqlConnectionTest.EXPECTED_TEST_CATALOG);
      assertThat(catalog).isNotEqualToIgnoringCase(MysqlConnectionTest.FORBIDDEN_PRODUCTION_CATALOG);
    }
  }

  @Test
  @DisplayName("2. All 21 analytical views exist in SIH26135_TEST and can be queried")
  void testAll21AnalyticalViewsExistAndSelectable() {
    for (String viewName : expectedAnalyticsViewNames) {
      Integer count = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM information_schema.VIEWS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
          Integer.class,
          viewName
      );
      assertThat(count)
          .as("Analytical view %s must exist in SIH26135_TEST", viewName)
          .isEqualTo(1);

      // Verify view is valid and selectable
      assertThat(jdbcTemplate.queryForList("SELECT * FROM " + viewName + " LIMIT 1"))
          .isNotNull();
    }
  }

  static int toIntFlag(Object val) {
    if (val == null) {
      throw new IllegalArgumentException("Flag value must not be null");
    }
    if (val instanceof Boolean b) {
      return b ? 1 : 0;
    }
    if (val instanceof BigDecimal bd) {
      if (bd.compareTo(BigDecimal.ZERO) == 0) return 0;
      if (bd.compareTo(BigDecimal.ONE) == 0) return 1;
      throw new IllegalArgumentException("Numeric flag value must be mathematically 0 or 1, got: " + bd);
    }
    if (val instanceof BigInteger bi) {
      if (BigInteger.ZERO.equals(bi)) return 0;
      if (BigInteger.ONE.equals(bi)) return 1;
      throw new IllegalArgumentException("Numeric flag value must be mathematically 0 or 1, got: " + bi);
    }
    if (val instanceof Number n) {
      double d = n.doubleValue();
      long l = n.longValue();
      // Ensure the value is an exact integer with no fractional part
      if (Double.compare(d, l) == 0) {
        if (l == 0L) return 0;
        if (l == 1L) return 1;
      }
      throw new IllegalArgumentException("Numeric flag value must be mathematically 0 or 1, got: " + n);
    }
    throw new IllegalArgumentException("Unsupported flag value type: " + val.getClass().getName());
  }

  @Test
  @DisplayName("toIntFlag strictly validates Boolean and numeric flags and rejects invalid states")
  void testToIntFlag_validation() {
    assertThat(toIntFlag(Boolean.TRUE)).isEqualTo(1);
    assertThat(toIntFlag(Boolean.FALSE)).isEqualTo(0);
    assertThat(toIntFlag(1)).isEqualTo(1);
    assertThat(toIntFlag(0)).isEqualTo(0);
    assertThat(toIntFlag((byte) 1)).isEqualTo(1);
    assertThat(toIntFlag((byte) 0)).isEqualTo(0);
    assertThat(toIntFlag(1L)).isEqualTo(1);
    assertThat(toIntFlag(0L)).isEqualTo(0);
    assertThat(toIntFlag(new BigDecimal("1.0"))).isEqualTo(1);
    assertThat(toIntFlag(new BigDecimal("0.0"))).isEqualTo(0);
    assertThat(toIntFlag(1.0)).isEqualTo(1);
    assertThat(toIntFlag(0.0)).isEqualTo(0);

    assertThatThrownBy(() -> toIntFlag(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("must not be null");

    assertThatThrownBy(() -> toIntFlag(1.5))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("must be mathematically 0 or 1");

    assertThatThrownBy(() -> toIntFlag(0.5))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("must be mathematically 0 or 1");

    assertThatThrownBy(() -> toIntFlag(new BigDecimal("1.5")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("must be mathematically 0 or 1");

    assertThatThrownBy(() -> toIntFlag(2))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("must be mathematically 0 or 1");

    assertThatThrownBy(() -> toIntFlag(-1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("must be mathematically 0 or 1");

    assertThatThrownBy(() -> toIntFlag("1"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unsupported flag value type");
  }

  @Test
  @DisplayName("3. ref_record_verification_status enforces distinct Unknown/Pending/Self-Reported/Verified states")
  void testRefRecordVerificationStatus_stateDistinctions() {
    List<Map<String, Object>> rows = jdbcTemplate.queryForList(
        "SELECT status_code, is_verified_flag FROM ref_record_verification_status ORDER BY record_verification_status_id"
    );

    assertThat(rows).hasSize(5);

    Map<String, Object> unverified = rows.stream()
        .filter(r -> "UNVERIFIED".equals(r.get("status_code")))
        .findFirst().orElseThrow();
    assertThat(toIntFlag(unverified.get("is_verified_flag"))).isEqualTo(0);

    Map<String, Object> selfReported = rows.stream()
        .filter(r -> "SELF_REPORTED".equals(r.get("status_code")))
        .findFirst().orElseThrow();
    assertThat(toIntFlag(selfReported.get("is_verified_flag"))).isEqualTo(0);

    Map<String, Object> pending = rows.stream()
        .filter(r -> "PENDING".equals(r.get("status_code")))
        .findFirst().orElseThrow();
    assertThat(toIntFlag(pending.get("is_verified_flag"))).isEqualTo(0);

    Map<String, Object> verified = rows.stream()
        .filter(r -> "VERIFIED".equals(r.get("status_code")))
        .findFirst().orElseThrow();
    assertThat(toIntFlag(verified.get("is_verified_flag"))).isEqualTo(1);

    Map<String, Object> rejected = rows.stream()
        .filter(r -> "REJECTED".equals(r.get("status_code")))
        .findFirst().orElseThrow();
    assertThat(toIntFlag(rejected.get("is_verified_flag"))).isEqualTo(0);
  }

  @Test
  @DisplayName("4. ref_employment_info_source explicitly separates self-reported from official sources")
  void testRefEmploymentInfoSource_selfReportedDistinction() {
    List<Map<String, Object>> rows = jdbcTemplate.queryForList(
        "SELECT source_code, is_self_reported_flag FROM ref_employment_info_source"
    );

    Map<String, Object> traineeDeclaration = rows.stream()
        .filter(r -> "TRAINEE_DECLARATION".equals(r.get("source_code")))
        .findFirst().orElseThrow();
    assertThat(toIntFlag(traineeDeclaration.get("is_self_reported_flag"))).isEqualTo(1);

    Map<String, Object> surveyFollowup = rows.stream()
        .filter(r -> "SURVEY_FOLLOWUP".equals(r.get("source_code")))
        .findFirst().orElseThrow();
    assertThat(toIntFlag(surveyFollowup.get("is_self_reported_flag"))).isEqualTo(1);

    Map<String, Object> placementConversion = rows.stream()
        .filter(r -> "PLACEMENT_CONVERSION".equals(r.get("source_code")))
        .findFirst().orElseThrow();
    assertThat(toIntFlag(placementConversion.get("is_self_reported_flag"))).isEqualTo(0);

    Map<String, Object> employerConfirmation = rows.stream()
        .filter(r -> "EMPLOYER_CONFIRMATION".equals(r.get("source_code")))
        .findFirst().orElseThrow();
    assertThat(toIntFlag(employerConfirmation.get("is_self_reported_flag"))).isEqualTo(0);
  }

  @Test
  @DisplayName("5. ref_employment_verification_method separates trainee self-attestation from official verification")
  void testRefEmploymentVerificationMethod_selfReportedVsOfficial() {
    List<Map<String, Object>> rows = jdbcTemplate.queryForList(
        "SELECT method_code, is_trainee_self_reported, is_official_flag FROM ref_employment_verification_method"
    );

    Map<String, Object> selfAttestation = rows.stream()
        .filter(r -> "TRAINEE_SELF_ATTESTATION".equals(r.get("method_code")))
        .findFirst().orElseThrow();
    assertThat(toIntFlag(selfAttestation.get("is_trainee_self_reported"))).isEqualTo(1);
    assertThat(toIntFlag(selfAttestation.get("is_official_flag"))).isEqualTo(0);

    Map<String, Object> docReview = rows.stream()
        .filter(r -> "DOCUMENT_REVIEW".equals(r.get("method_code")))
        .findFirst().orElseThrow();
    assertThat(toIntFlag(docReview.get("is_trainee_self_reported"))).isEqualTo(0);
    assertThat(toIntFlag(docReview.get("is_official_flag"))).isEqualTo(1);

    Map<String, Object> employerPortal = rows.stream()
        .filter(r -> "EMPLOYER_PORTAL".equals(r.get("method_code")))
        .findFirst().orElseThrow();
    assertThat(toIntFlag(employerPortal.get("is_trainee_self_reported"))).isEqualTo(0);
    assertThat(toIntFlag(employerPortal.get("is_official_flag"))).isEqualTo(0);

    Map<String, Object> govDb = rows.stream()
        .filter(r -> "GOVERNMENT_DATABASE".equals(r.get("method_code")))
        .findFirst().orElseThrow();
    assertThat(toIntFlag(govDb.get("is_trainee_self_reported"))).isEqualTo(0);
    assertThat(toIntFlag(govDb.get("is_official_flag"))).isEqualTo(1);

    // Assert DIRECT_API_INTEGRATION flags if row exists in reference catalog
    rows.stream()
        .filter(r -> "DIRECT_API_INTEGRATION".equals(r.get("method_code")))
        .findFirst()
        .ifPresent(directApi -> {
          assertThat(toIntFlag(directApi.get("is_trainee_self_reported"))).isEqualTo(0);
          assertThat(toIntFlag(directApi.get("is_official_flag"))).isEqualTo(1);
        });
  }

  @Test
  @DisplayName("6. Analytical views definition check: vw_trainee_outcome_summary excludes soft-deleted trainees")
  void testViewDefinition_excludesSoftDeleted() {
    String viewDef = jdbcTemplate.queryForObject(
        "SELECT VIEW_DEFINITION FROM information_schema.VIEWS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'vw_trainee_outcome_summary'",
        String.class
    );
    assertThat(viewDef).isNotNull();
    assertThat(viewDef.replace("`", "").toLowerCase()).contains("deleted_at is null");
  }

  @Test
  @DisplayName("7. Spring Data JPA repository for TraineeOutcomeSummary directly queries database view")
  void testTraineeOutcomeSummaryRepository_queriesViewSuccessfully() {
    assertThat(traineeOutcomeSummaryRepository).isNotNull();
    // Repository findAll executes SELECT on vw_trainee_outcome_summary
    List<?> summaries = traineeOutcomeSummaryRepository.findAll();
    assertThat(summaries).isNotNull();
  }
}
