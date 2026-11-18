package in.gov.sih.sih26135.integration;

import in.gov.sih.sih26135.MysqlConnectionTest;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Phase 29.5 — Fixture Isolation & Data Safety Regression Test.
 *
 * <p>Verifies:
 * <ul>
 *   <li>Pre-existing records with IDs formerly used by fixed-ID tests are not modified or deleted.</li>
 *   <li>Unrelated trainees with old {@code REG-TP500-} prefix survive test execution and cleanup.</li>
 *   <li>Survey fixtures created by the current test are tracked and cleanly deleted respecting FKs.</li>
 *   <li>A deliberate cleanup failure is reported as a visible test failure rather than silently swallowed.</li>
 *   <li>All test operations strictly execute against the dedicated {@code SIH26135_TEST} catalog.</li>
 * </ul>
 */
@DisplayName("Phase 29.5 — Fixture Isolation & Data Safety Regression Test")
class FixtureIsolationAndDataSafetyRegressionTest extends BaseJourneyIntegrationTest {

  @Test
  @DisplayName("1. Untracked pre-existing records and formerly used IDs are not modified or deleted")
  void testPreExistingRecordsWithFormerlyUsedIdsSurvive() {
    String token = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

    // If fixed sector 9101 genuinely pre-exists in the database, verify it purely read-only
    Integer preExisting9101Count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM sectors WHERE sector_id = 9101", Integer.class);
    boolean sector9101PreExists = preExisting9101Count != null && preExisting9101Count > 0;

    // Dynamically create an exclusive untracked sector fixture representing a pre-existing record
    Long untrackedSectorId = insertAndGetId("""
        INSERT INTO sectors (sector_code, sector_name, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, 1, NOW(), NOW())
        """, "SEC-UNTRACKED-" + token, "Untracked PreExisting Sector " + token);

    // Also dynamically create a tracked sector so teardown has an active fixture to clean up
    Long trackedSectorId = insertAndGetId("""
        INSERT INTO sectors (sector_code, sector_name, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, 1, NOW(), NOW())
        """, "SEC-TRACKED-" + token, "Tracked Test Sector " + token);
    trackedSectorIds.add(trackedSectorId);

    try {
      // Execute teardown of tracked fixtures
      teardownTrackedFixtures();

      // Assert untracked record survived teardown
      Integer untrackedCount = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM sectors WHERE sector_id = ?",
          Integer.class, untrackedSectorId);
      assertThat(untrackedCount)
          .as("Untracked record must survive teardown intact")
          .isEqualTo(1);

      // Assert tracked record was cleaned up
      Integer trackedCount = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM sectors WHERE sector_id = ?",
          Integer.class, trackedSectorId);
      assertThat(trackedCount)
          .as("Tracked record must be cleaned up by teardown")
          .isZero();

      // If sector 9101 pre-existed, assert it survived in a purely read-only manner
      if (sector9101PreExists) {
        Integer count9101 = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM sectors WHERE sector_id = 9101", Integer.class);
        assertThat(count9101)
            .as("Pre-existing sector 9101 must remain completely untouched")
            .isEqualTo(1);
      }
    } finally {
      // Only delete the untracked sector demonstrably created and owned by this test
      if (untrackedSectorId != null) {
        jdbcTemplate.update("DELETE FROM sectors WHERE sector_id = ?", untrackedSectorId);
      }
      // Never delete or modify sector 9101
    }
  }

  @Test
  @DisplayName("2. Unrelated trainees with old REG-TP500- prefix survive")
  void testUnrelatedTraineesWithOldPrefixSurvive() {
    String survivorRegNo = "REG-TP500-OLD-SURVIVOR-" + UUID.randomUUID().toString().substring(0, 6);
    Long survivorTraineeId = insertAndGetId("""
        INSERT INTO trainees (registration_number, first_name, gender_id, state_id, district_id, current_employment_status_id, profile_status_id, created_at, updated_at)
        VALUES (?, 'SurvivorCandidate', 1, 101, 1001, 1, 1, NOW(), NOW())
        """, survivorRegNo);

    try {
      // Simulate teardown of tracked fixtures
      teardownTrackedFixtures();

      // Assert unrelated trainee with REG-TP500- prefix survived teardown
      Integer count = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM trainees WHERE trainee_id = ? AND registration_number = ?",
          Integer.class, survivorTraineeId, survivorRegNo);
      assertThat(count)
          .as("Unrelated trainee with REG-TP500- prefix must survive teardown")
          .isEqualTo(1);
    } finally {
      // Clean up survivor trainee
      jdbcTemplate.update("DELETE FROM trainees WHERE trainee_id = ?", survivorTraineeId);
    }
  }

  @Test
  @DisplayName("3. Survey fixtures created by current test are tracked and cleaned up")
  void testSurveyFixturesTrackedAndCleanedUp() {
    String token = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

    Long surveyTemplateId = insertAndGetId("""
        INSERT INTO survey_templates (template_code, template_name, survey_purpose_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, 'Survey Template Regression', 1, 1, NOW(), NOW())
        """, "TPL-REG-" + token);
    trackedSurveyTemplateIds.add(surveyTemplateId);

    Long surveyTemplateVersionId = insertAndGetId("""
        INSERT INTO survey_template_versions (survey_template_id, version_number, version_label, effective_from, lifecycle_status_id, created_at, updated_at)
        VALUES (?, 1, 'v1.0', '2026-01-01', 1, NOW(), NOW())
        """, surveyTemplateId);
    trackedSurveyTemplateVersionIds.add(surveyTemplateVersionId);

    Long surveyId = insertAndGetId("""
        INSERT INTO surveys (survey_code, survey_name, survey_template_version_id, survey_purpose_id, scheduled_start_date, survey_instance_status_id, created_at, updated_at)
        VALUES (?, 'Survey Regression', ?, 1, '2026-01-01', 1, NOW(), NOW())
        """, "SRV-REG-" + token, surveyTemplateVersionId);
    trackedSurveyIds.add(surveyId);

    Long dummyTraineeId = insertAndGetId("""
        INSERT INTO trainees (registration_number, first_name, gender_id, state_id, district_id, current_employment_status_id, profile_status_id, created_at, updated_at)
        VALUES (?, 'SurveyTrainee', 1, 101, 1001, 1, 1, NOW(), NOW())
        """, "REG-SRV-" + token);
    trackedTraineeIds.add(dummyTraineeId);

    Long surveyResponseId = insertAndGetId("""
        INSERT INTO survey_responses (survey_id, survey_template_version_id, trainee_id, attempt_number, survey_response_status_id, started_at, submitted_at, created_at, updated_at)
        VALUES (?, ?, ?, 1, 1, NOW(), NOW(), NOW(), NOW())
        """, surveyId, surveyTemplateVersionId, dummyTraineeId);
    trackedSurveyResponseIds.add(surveyResponseId);

    // Execute teardown
    teardownTrackedFixtures();

    // Verify all survey elements have been completely removed
    Integer respCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM survey_responses WHERE survey_response_id = ?",
        Integer.class, surveyResponseId);
    assertThat(respCount).isZero();

    Integer srvCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM surveys WHERE survey_id = ?",
        Integer.class, surveyId);
    assertThat(srvCount).isZero();

    Integer verCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM survey_template_versions WHERE survey_template_version_id = ?",
        Integer.class, surveyTemplateVersionId);
    assertThat(verCount).isZero();

    Integer tplCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM survey_templates WHERE survey_template_id = ?",
        Integer.class, surveyTemplateId);
    assertThat(tplCount).isZero();
  }

  @Test
  @DisplayName("4. Deliberate cleanup failure is reported rather than silently ignored")
  void testDeliberateCleanupFailureIsReportedRatherThanSwallowed() {
    String token = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

    Long surveyTemplateId = insertAndGetId("""
        INSERT INTO survey_templates (template_code, template_name, survey_purpose_id, lifecycle_status_id, created_at, updated_at)
        VALUES (?, 'Blocking Survey Template', 1, 1, NOW(), NOW())
        """, "TPL-BLOCK-" + token);
    trackedSurveyTemplateIds.add(surveyTemplateId);

    Long surveyTemplateVersionId = insertAndGetId("""
        INSERT INTO survey_template_versions (survey_template_id, version_number, version_label, effective_from, lifecycle_status_id, created_at, updated_at)
        VALUES (?, 1, 'v1.0', '2026-01-01', 1, NOW(), NOW())
        """, surveyTemplateId);
    trackedSurveyTemplateVersionIds.add(surveyTemplateVersionId);

    Long blockingSurveyId = insertAndGetId("""
        INSERT INTO surveys (survey_code, survey_name, survey_template_version_id, survey_purpose_id, scheduled_start_date, survey_instance_status_id, created_at, updated_at)
        VALUES (?, 'Blocking Survey', ?, 1, '2026-01-01', 1, NOW(), NOW())
        """, "SRV-BLOCK-" + token, surveyTemplateVersionId);
    trackedSurveyIds.add(blockingSurveyId);

    // Create an UNTRACKED child survey response referencing blockingSurveyId
    Long blockingResponseId = insertAndGetId("""
        INSERT INTO survey_responses (survey_id, survey_template_version_id, trainee_id, attempt_number, survey_response_status_id, started_at, created_at, updated_at)
        VALUES (?, ?, ?, 1, 1, NOW(), NOW(), NOW())
        """, blockingSurveyId, surveyTemplateVersionId, AUTHORITATIVE_TEST_TRAINEE_ID);

    try {
      // teardownTrackedFixtures() will attempt to delete the survey, but survey_responses references it.
      // Because silent catch blocks have been removed, teardown MUST fail with AssertionError!
      assertThatThrownBy(this::teardownTrackedFixtures)
          .isInstanceOf(AssertionError.class)
          .hasMessageContaining("Test fixture teardown cleanup failed")
          .hasMessageContaining("surveys");
    } finally {
      // Clean up manually, then clear tracked collections
      jdbcTemplate.update("DELETE FROM survey_responses WHERE survey_response_id = ?", blockingResponseId);
      jdbcTemplate.update("DELETE FROM surveys WHERE survey_id = ?", blockingSurveyId);
      jdbcTemplate.update("DELETE FROM survey_template_versions WHERE survey_template_version_id = ?", surveyTemplateVersionId);
      jdbcTemplate.update("DELETE FROM survey_templates WHERE survey_template_id = ?", surveyTemplateId);
      trackedSurveyIds.remove(blockingSurveyId);
      trackedSurveyTemplateVersionIds.remove(surveyTemplateVersionId);
      trackedSurveyTemplateIds.remove(surveyTemplateId);
    }
  }

  @Test
  @DisplayName("5. Dedicated test catalog SIH26135_TEST and authoritative invariants verified")
  void testDedicatedTestCatalogAndAuthoritativeInvariants() throws Exception {
    try (Connection connection = dataSource.getConnection()) {
      MysqlConnectionTest.assertDedicatedTestCatalog(connection.getCatalog());
      assertThat(connection.getCatalog()).isEqualToIgnoringCase("SIH26135_TEST");
      assertAuthoritativeTraineeAndConsentsPreserved(connection);
    }
  }
}
