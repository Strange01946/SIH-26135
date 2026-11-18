package in.gov.sih.sih26135.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.sih.sih26135.MysqlConnectionTest;
import in.gov.sih.sih26135.security.jwt.JwtTokenProvider;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 29.5 — Shared Base Integration Test Infrastructure.
 *
 * <p>Enforces:
 * <ul>
 *   <li>Real HTTP server running on a randomized port with full Spring Security filter chain.</li>
 *   <li>Dedicated test database catalog guard (strictly asserts {@code SIH26135_TEST}).</li>
 *   <li>Preservation of authoritative test trainee {@code 1005143L} and its 5 active GRANTED consents.</li>
 *   <li>Exact, deterministic fixture ownership tracking and safe reverse-dependency teardown.</li>
 *   <li>Visible, failing teardown cleanup validation (zero silent exception swallowing).</li>
 *   <li>Zero mocking of business services, repositories, or application controllers.</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseJourneyIntegrationTest {

  protected static final Logger log = LoggerFactory.getLogger(BaseJourneyIntegrationTest.class);

  public static final Long AUTHORITATIVE_TEST_TRAINEE_ID = 1005143L;
  public static final Long BASE_STATE_ID = 101L;
  public static final Long BASE_DISTRICT_ID = 1001L;

  @LocalServerPort
  protected int port;

  @Autowired
  protected TestRestTemplate restTemplate;

  @Autowired
  protected JdbcTemplate jdbcTemplate;

  @Autowired
  protected DataSource dataSource;

  @Autowired
  protected JwtTokenProvider jwtTokenProvider;

  @Autowired
  protected ObjectMapper objectMapper;

  // Tracked fixture collections for safe, isolated reverse teardown
  protected final List<Long> trackedCommunicationJobIds = new CopyOnWriteArrayList<>();
  protected final List<Long> trackedCommunicationLogIds = new CopyOnWriteArrayList<>();
  protected final List<Long> trackedDqIssueIds = new CopyOnWriteArrayList<>();
  protected final List<Long> trackedImportBatchIds = new CopyOnWriteArrayList<>();
  protected final List<Long> trackedSurveyResponseIds = new CopyOnWriteArrayList<>();
  protected final List<Long> trackedSurveyIds = new CopyOnWriteArrayList<>();
  protected final List<Long> trackedSurveyTemplateVersionIds = new CopyOnWriteArrayList<>();
  protected final List<Long> trackedSurveyTemplateIds = new CopyOnWriteArrayList<>();
  protected final List<Long> trackedSalaryHistoryIds = new CopyOnWriteArrayList<>();
  protected final List<Long> trackedVerificationIds = new CopyOnWriteArrayList<>();
  protected final List<Long> trackedExitEventIds = new CopyOnWriteArrayList<>();
  protected final List<Long> trackedUnemploymentEventIds = new CopyOnWriteArrayList<>();
  protected final List<Long> trackedEmploymentIds = new CopyOnWriteArrayList<>();
  protected final List<Long> trackedPlacementIds = new CopyOnWriteArrayList<>();
  protected final List<Long> trackedJobPostingIds = new CopyOnWriteArrayList<>();
  protected final List<Long> trackedJobRoleIds = new CopyOnWriteArrayList<>();
  protected final List<Long> trackedCertificationIds = new CopyOnWriteArrayList<>();
  protected final List<Long> trackedAssessmentResultIds = new CopyOnWriteArrayList<>();
  protected final List<Long> trackedTraineeAssessmentIds = new CopyOnWriteArrayList<>();
  protected final List<Long> trackedAssessmentIds = new CopyOnWriteArrayList<>();
  protected final List<Long> trackedAttendanceIds = new CopyOnWriteArrayList<>();
  protected final List<Long> trackedEnrollmentIds = new CopyOnWriteArrayList<>();
  protected final List<Long> trackedBatchIds = new CopyOnWriteArrayList<>();
  protected final List<Long> trackedCenterIds = new CopyOnWriteArrayList<>();
  protected final List<Long> trackedCourseIds = new CopyOnWriteArrayList<>();
  protected final List<Long> trackedSectorIds = new CopyOnWriteArrayList<>();
  protected final List<Long> trackedProgramIds = new CopyOnWriteArrayList<>();
  protected final List<Long> trackedSchemeIds = new CopyOnWriteArrayList<>();
  protected final List<Long> trackedDeptIds = new CopyOnWriteArrayList<>();
  protected final List<Long> trackedOrgIds = new CopyOnWriteArrayList<>();
  protected final List<Long> trackedConsentIds = new CopyOnWriteArrayList<>();
  protected final List<Long> trackedTraineeIds = new CopyOnWriteArrayList<>();
  protected final List<Long> trackedEmployerIds = new CopyOnWriteArrayList<>();
  protected final List<Long> trackedProviderIds = new CopyOnWriteArrayList<>();
  protected final List<Long> trackedUserIds = new CopyOnWriteArrayList<>();
  protected final List<Long> trackedDistrictIds = new CopyOnWriteArrayList<>();
  protected final List<Long> trackedStateIds = new CopyOnWriteArrayList<>();

  @BeforeEach
  void enforceCatalogAndAuthoritativeInvariants() throws Exception {
    try (Connection connection = dataSource.getConnection()) {
      MysqlConnectionTest.assertDedicatedTestCatalog(connection.getCatalog());
      assertAuthoritativeTraineeAndConsentsPreserved(connection);
    }
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void cleanUpTrackedFixtures() {
    try {
      SecurityContextHolder.clearContext();
      teardownTrackedFixtures();
      try (Connection connection = dataSource.getConnection()) {
        assertAuthoritativeTraineeAndConsentsPreserved(connection);
      }
    } catch (Exception e) {
      log.error("[TestCleanup] Error during fixture teardown: {}", e.getMessage(), e);
      throw (e instanceof RuntimeException re) ? re : new RuntimeException("Test fixture teardown failed", e);
    }
  }

  protected void assertAuthoritativeTraineeAndConsentsPreserved(Connection connection) throws Exception {
    try (Statement stmt = connection.createStatement()) {
      try (ResultSet rs = stmt.executeQuery(
          "SELECT COUNT(*) FROM trainees WHERE trainee_id = " + AUTHORITATIVE_TEST_TRAINEE_ID + " AND deleted_at IS NULL")) {
        assertThat(rs.next()).isTrue();
        assertThat(rs.getInt(1))
            .as("Authoritative test trainee 1005143L must remain present and intact in SIH26135_TEST")
            .isEqualTo(1);
      }
      try (ResultSet rs = stmt.executeQuery(
          "SELECT COUNT(*) FROM trainee_consents WHERE trainee_id = " + AUTHORITATIVE_TEST_TRAINEE_ID + " AND consent_status_id = 1")) {
        assertThat(rs.next()).isTrue();
        assertThat(rs.getInt(1))
            .as("Authoritative test trainee 1005143L must retain exactly 5 active GRANTED DPDP consents")
            .isEqualTo(5);
      }
    }
  }

  /**
   * Safe helper to insert a record with auto-increment primary key and return the generated ID.
   */
  protected Long insertAndGetId(String sql, Object... params) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(connection -> {
      PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
      for (int i = 0; i < params.length; i++) {
        ps.setObject(i + 1, params[i]);
      }
      return ps;
    }, keyHolder);
    Number key = keyHolder.getKey();
    if (key == null && !keyHolder.getKeyList().isEmpty()) {
      Object firstVal = keyHolder.getKeyList().get(0).values().iterator().next();
      if (firstVal instanceof Number num) {
        return num.longValue();
      }
    }
    if (key == null) {
      throw new IllegalStateException("Failed to retrieve generated primary key for SQL: " + sql);
    }
    return key.longValue();
  }

  /**
   * Helper to delete tracked records by primary key, verifying removal and recording errors.
   */
  protected void deleteAndVerify(String tableName, String idColumn, Collection<Long> ids, List<String> errors) {
    if (ids == null || ids.isEmpty()) {
      return;
    }
    for (Long id : ids) {
      if (id == null) continue;
      try {
        jdbcTemplate.update("DELETE FROM " + tableName + " WHERE " + idColumn + " = ?", id);
        Integer remaining = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM " + tableName + " WHERE " + idColumn + " = ?",
            Integer.class, id);
        if (remaining != null && remaining > 0) {
          errors.add("Record still exists in " + tableName + " where " + idColumn + " = " + id);
        }
      } catch (Exception ex) {
        errors.add("Error deleting from " + tableName + " where " + idColumn + " = " + id + ": " + ex.getMessage());
      }
    }
  }

  /**
   * Helper to verify that test-owned records have actually been deleted.
   */
  protected void verifyRecordsRemoved(String tableName, String idColumn, Collection<Long> ids, Long preservedId, List<String> errors) {
    if (ids == null || ids.isEmpty()) {
      return;
    }
    for (Long id : ids) {
      if (id == null || (preservedId != null && preservedId.equals(id))) {
        continue;
      }
      try {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM " + tableName + " WHERE " + idColumn + " = ?",
            Integer.class, id);
        if (count != null && count > 0) {
          errors.add("Uncleaned record detected in " + tableName + " where " + idColumn + " = " + id);
        }
      } catch (Exception ex) {
        errors.add("Error verifying removal from " + tableName + " where " + idColumn + " = " + id + ": " + ex.getMessage());
      }
    }
  }

  protected void teardownTrackedFixtures() {
    List<String> cleanupErrors = new ArrayList<>();

    // 1. Communication Queue, Logs & Rate Limit Events
    for (Long jobId : trackedCommunicationJobIds) {
      try {
        jdbcTemplate.update("DELETE FROM communication_dispatch_attempts WHERE job_id = ?", jobId);
        jdbcTemplate.update("DELETE FROM communication_dispatch_jobs WHERE job_id = ?", jobId);
      } catch (Exception ex) {
        cleanupErrors.add("Error deleting communication dispatch jobs/attempts for job " + jobId + ": " + ex.getMessage());
      }
    }
    for (Long logId : trackedCommunicationLogIds) {
      try {
        jdbcTemplate.update("DELETE FROM communication_logs WHERE communication_log_id = ?", logId);
      } catch (Exception ex) {
        cleanupErrors.add("Error deleting communication log " + logId + ": " + ex.getMessage());
      }
    }
    for (Long traineeId : trackedTraineeIds) {
      if (!AUTHORITATIVE_TEST_TRAINEE_ID.equals(traineeId)) {
        try {
          jdbcTemplate.update("DELETE FROM communication_logs WHERE trainee_id = ?", traineeId);
          jdbcTemplate.update("DELETE FROM communication_rate_limit_events WHERE limiter_key LIKE ? OR limiter_key LIKE ?",
              "TRAINEE:%:" + traineeId, "DEDUP:" + traineeId + ":%");
        } catch (Exception ex) {
          cleanupErrors.add("Error deleting communication logs/rate-limits for trainee " + traineeId + ": " + ex.getMessage());
        }
      }
    }
    for (Long providerId : trackedProviderIds) {
      try {
        jdbcTemplate.update("DELETE FROM communication_rate_limit_events WHERE limiter_key = ?", "PROVIDER:" + providerId);
      } catch (Exception ex) {
        cleanupErrors.add("Error deleting communication rate-limits for provider " + providerId + ": " + ex.getMessage());
      }
    }

    // 2. Data Quality Issues & Events (data_quality_issue_id)
    for (Long issueId : trackedDqIssueIds) {
      try {
        jdbcTemplate.update("DELETE FROM data_quality_issue_events WHERE data_quality_issue_id = ?", issueId);
        jdbcTemplate.update("DELETE FROM data_quality_issues WHERE data_quality_issue_id = ?", issueId);
      } catch (Exception ex) {
        cleanupErrors.add("Error deleting DQ issue " + issueId + ": " + ex.getMessage());
      }
    }

    // 3. Import Batches & Quarantined Records
    for (Long batchId : trackedImportBatchIds) {
      try {
        jdbcTemplate.update("DELETE FROM import_records WHERE import_batch_id = ?", batchId);
        jdbcTemplate.update("DELETE FROM import_batches WHERE import_batch_id = ?", batchId);
      } catch (Exception ex) {
        cleanupErrors.add("Error deleting import batch " + batchId + ": " + ex.getMessage());
      }
    }

    // 4. Surveys Hierarchy (Answers -> Responses -> Surveys -> Versions -> Templates)
    for (Long responseId : trackedSurveyResponseIds) {
      try {
        jdbcTemplate.update("DELETE FROM survey_response_answer_options WHERE survey_response_answer_id IN (SELECT survey_response_answer_id FROM survey_response_answers WHERE survey_response_id = ?)", responseId);
        jdbcTemplate.update("DELETE FROM survey_response_answers WHERE survey_response_id = ?", responseId);
        jdbcTemplate.update("DELETE FROM survey_responses WHERE survey_response_id = ?", responseId);
      } catch (Exception ex) {
        cleanupErrors.add("Error deleting survey response " + responseId + ": " + ex.getMessage());
      }
    }
    for (Long traineeId : trackedTraineeIds) {
      if (!AUTHORITATIVE_TEST_TRAINEE_ID.equals(traineeId)) {
        try {
          jdbcTemplate.update("DELETE FROM survey_response_answers WHERE survey_response_id IN (SELECT survey_response_id FROM survey_responses WHERE trainee_id = ?)", traineeId);
          jdbcTemplate.update("DELETE FROM survey_responses WHERE trainee_id = ?", traineeId);
        } catch (Exception ex) {
          cleanupErrors.add("Error deleting survey responses for trainee " + traineeId + ": " + ex.getMessage());
        }
      }
    }
    deleteAndVerify("surveys", "survey_id", trackedSurveyIds, cleanupErrors);
    deleteAndVerify("survey_template_versions", "survey_template_version_id", trackedSurveyTemplateVersionIds, cleanupErrors);
    deleteAndVerify("survey_templates", "survey_template_id", trackedSurveyTemplateIds, cleanupErrors);

    // 5. Employment Verifications & Evidence
    for (Long verifId : trackedVerificationIds) {
      try {
        jdbcTemplate.update("DELETE FROM employment_verification_evidence WHERE employment_verification_id = ?", verifId);
        jdbcTemplate.update("DELETE FROM employment_verifications WHERE employment_verification_id = ?", verifId);
      } catch (Exception ex) {
        cleanupErrors.add("Error deleting verification " + verifId + ": " + ex.getMessage());
      }
    }
    for (Long empId : trackedEmploymentIds) {
      try {
        jdbcTemplate.update("DELETE FROM employment_verification_evidence WHERE employment_verification_id IN (SELECT employment_verification_id FROM employment_verifications WHERE employment_id = ?)", empId);
        jdbcTemplate.update("DELETE FROM employment_verification_evidence WHERE employment_verification_attempt_id IN (SELECT employment_verification_attempt_id FROM employment_verification_attempts WHERE employment_verification_request_id IN (SELECT employment_verification_request_id FROM employment_verification_requests WHERE employment_id = ?))", empId);
        jdbcTemplate.update("DELETE FROM employment_verifications WHERE employment_id = ?", empId);
        jdbcTemplate.update("DELETE FROM employment_verification_attempts WHERE employment_verification_request_id IN (SELECT employment_verification_request_id FROM employment_verification_requests WHERE employment_id = ?)", empId);
        jdbcTemplate.update("DELETE FROM employment_verification_requests WHERE employment_id = ?", empId);
      } catch (Exception ex) {
        cleanupErrors.add("Error deleting verifications for employment " + empId + ": " + ex.getMessage());
      }
    }
    for (Long traineeId : trackedTraineeIds) {
      if (!AUTHORITATIVE_TEST_TRAINEE_ID.equals(traineeId)) {
        try {
          jdbcTemplate.update("DELETE FROM employment_verification_evidence WHERE employment_verification_id IN (SELECT employment_verification_id FROM employment_verifications WHERE trainee_id = ?)", traineeId);
          jdbcTemplate.update("DELETE FROM employment_verification_evidence WHERE employment_verification_attempt_id IN (SELECT employment_verification_attempt_id FROM employment_verification_attempts WHERE employment_verification_request_id IN (SELECT employment_verification_request_id FROM employment_verification_requests WHERE trainee_id = ?))", traineeId);
          jdbcTemplate.update("DELETE FROM employment_verifications WHERE trainee_id = ?", traineeId);
          jdbcTemplate.update("DELETE FROM employment_verification_attempts WHERE employment_verification_request_id IN (SELECT employment_verification_request_id FROM employment_verification_requests WHERE trainee_id = ?)", traineeId);
          jdbcTemplate.update("DELETE FROM employment_verification_requests WHERE trainee_id = ?", traineeId);
        } catch (Exception ex) {
          cleanupErrors.add("Error deleting verifications for trainee " + traineeId + ": " + ex.getMessage());
        }
      }
    }

    // 6. Salary History
    deleteAndVerify("salary_history", "salary_history_id", trackedSalaryHistoryIds, cleanupErrors);
    for (Long empId : trackedEmploymentIds) {
      try {
        jdbcTemplate.update("DELETE FROM salary_history WHERE employment_id = ?", empId);
      } catch (Exception ex) {
        cleanupErrors.add("Error deleting salary history for employment " + empId + ": " + ex.getMessage());
      }
    }

    // 7. Unemployment & Exit Events
    for (Long empId : trackedEmploymentIds) {
      try {
        jdbcTemplate.update("DELETE FROM trainee_unemployment_events WHERE preceding_employment_id = ? OR succeeding_employment_id = ?", empId, empId);
        jdbcTemplate.update("DELETE FROM trainee_unemployment_events WHERE employment_exit_event_id IN (SELECT employment_exit_event_id FROM employment_exit_events WHERE employment_id = ?)", empId);
        jdbcTemplate.update("DELETE FROM employment_exit_events WHERE employment_id = ?", empId);
      } catch (Exception ex) {
        cleanupErrors.add("Error deleting exit/unemployment events for employment " + empId + ": " + ex.getMessage());
      }
    }
    for (Long exitId : trackedExitEventIds) {
      try {
        jdbcTemplate.update("DELETE FROM trainee_unemployment_events WHERE employment_exit_event_id = ?", exitId);
        jdbcTemplate.update("DELETE FROM employment_exit_events WHERE employment_exit_event_id = ?", exitId);
      } catch (Exception ex) {
        cleanupErrors.add("Error deleting exit event " + exitId + ": " + ex.getMessage());
      }
    }
    deleteAndVerify("trainee_unemployment_events", "unemployment_event_id", trackedUnemploymentEventIds, cleanupErrors);
    deleteAndVerify("employment_exit_events", "employment_exit_event_id", trackedExitEventIds, cleanupErrors);
    for (Long traineeId : trackedTraineeIds) {
      if (!AUTHORITATIVE_TEST_TRAINEE_ID.equals(traineeId)) {
        try {
          jdbcTemplate.update("DELETE FROM trainee_unemployment_events WHERE trainee_id = ?", traineeId);
          jdbcTemplate.update("DELETE FROM employment_exit_events WHERE trainee_id = ?", traineeId);
        } catch (Exception ex) {
          cleanupErrors.add("Error deleting exit/unemployment events for trainee " + traineeId + ": " + ex.getMessage());
        }
      }
    }

    // 8. Employment Records
    for (Long plId : trackedPlacementIds) {
      try {
        jdbcTemplate.update("DELETE FROM employment_records WHERE placement_id = ?", plId);
      } catch (Exception ex) {
        cleanupErrors.add("Error clearing employment records for placement " + plId + ": " + ex.getMessage());
      }
    }
    for (Long traineeId : trackedTraineeIds) {
      if (!AUTHORITATIVE_TEST_TRAINEE_ID.equals(traineeId)) {
        try {
          jdbcTemplate.update("DELETE FROM employment_records WHERE trainee_id = ?", traineeId);
        } catch (Exception ex) {
          cleanupErrors.add("Error clearing employment records for trainee " + traineeId + ": " + ex.getMessage());
        }
      }
    }
    deleteAndVerify("employment_records", "employment_id", trackedEmploymentIds, cleanupErrors);

    // 9. Placement Records
    for (Long plId : trackedPlacementIds) {
      try {
        jdbcTemplate.update("DELETE FROM trainee_unemployment_events WHERE placement_id = ?", plId);
      } catch (Exception ex) {
        cleanupErrors.add("Error clearing unemployment events for placement " + plId + ": " + ex.getMessage());
      }
    }
    for (Long traineeId : trackedTraineeIds) {
      if (!AUTHORITATIVE_TEST_TRAINEE_ID.equals(traineeId)) {
        try {
          jdbcTemplate.update("DELETE FROM placement_records WHERE trainee_id = ?", traineeId);
        } catch (Exception ex) {
          cleanupErrors.add("Error clearing placement records for trainee " + traineeId + ": " + ex.getMessage());
        }
      }
    }
    deleteAndVerify("placement_records", "placement_id", trackedPlacementIds, cleanupErrors);

    // 10. Job Postings, Roles & Applications
    for (Long jobId : trackedJobPostingIds) {
      try {
        jdbcTemplate.update("DELETE FROM job_applications WHERE job_posting_id = ?", jobId);
        jdbcTemplate.update("DELETE FROM job_posting_skills WHERE job_posting_id = ?", jobId);
        jdbcTemplate.update("DELETE FROM job_postings WHERE job_posting_id = ?", jobId);
      } catch (Exception ex) {
        cleanupErrors.add("Error deleting job posting " + jobId + ": " + ex.getMessage());
      }
    }
    for (Long jrId : trackedJobRoleIds) {
      try {
        jdbcTemplate.update("DELETE FROM job_role_skills WHERE job_role_id = ?", jrId);
        jdbcTemplate.update("DELETE FROM job_roles WHERE job_role_id = ?", jrId);
      } catch (Exception ex) {
        cleanupErrors.add("Error deleting job role " + jrId + ": " + ex.getMessage());
      }
    }

    // 11. Assessments & Certifications
    deleteAndVerify("certifications", "certification_id", trackedCertificationIds, cleanupErrors);
    for (Long traineeId : trackedTraineeIds) {
      if (!AUTHORITATIVE_TEST_TRAINEE_ID.equals(traineeId)) {
        try {
          jdbcTemplate.update("DELETE FROM certifications WHERE trainee_id = ?", traineeId);
          jdbcTemplate.update("DELETE FROM assessment_results WHERE trainee_id = ?", traineeId);
        } catch (Exception ex) {
          cleanupErrors.add("Error deleting certifications for trainee " + traineeId + ": " + ex.getMessage());
        }
      }
    }
    for (Long enrId : trackedEnrollmentIds) {
      try {
        jdbcTemplate.update("DELETE FROM certifications WHERE enrollment_id = ?", enrId);
        jdbcTemplate.update("DELETE FROM assessment_results WHERE trainee_assessment_id IN (SELECT trainee_assessment_id FROM trainee_assessments WHERE enrollment_id = ?)", enrId);
        jdbcTemplate.update("DELETE FROM trainee_assessments WHERE enrollment_id = ?", enrId);
        jdbcTemplate.update("DELETE FROM skill_gap_assessments WHERE enrollment_id = ?", enrId);
      } catch (Exception ex) {
        cleanupErrors.add("Error deleting certifications/assessments for enrollment " + enrId + ": " + ex.getMessage());
      }
    }
    deleteAndVerify("assessment_results", "assessment_result_id", trackedAssessmentResultIds, cleanupErrors);
    deleteAndVerify("trainee_assessments", "trainee_assessment_id", trackedTraineeAssessmentIds, cleanupErrors);
    for (Long asmId : trackedAssessmentIds) {
      try {
        jdbcTemplate.update("DELETE FROM assessment_results WHERE trainee_assessment_id IN (SELECT trainee_assessment_id FROM trainee_assessments WHERE assessment_id = ?)", asmId);
        jdbcTemplate.update("DELETE FROM trainee_assessments WHERE assessment_id = ?", asmId);
        jdbcTemplate.update("DELETE FROM assessments WHERE assessment_id = ?", asmId);
      } catch (Exception ex) {
        cleanupErrors.add("Error deleting assessment " + asmId + ": " + ex.getMessage());
      }
    }
    for (Long batchId : trackedBatchIds) {
      try {
        jdbcTemplate.update("DELETE FROM assessment_results WHERE trainee_assessment_id IN (SELECT trainee_assessment_id FROM trainee_assessments WHERE assessment_id IN (SELECT assessment_id FROM assessments WHERE batch_id = ?))", batchId);
        jdbcTemplate.update("DELETE FROM trainee_assessments WHERE assessment_id IN (SELECT assessment_id FROM assessments WHERE batch_id = ?)", batchId);
        jdbcTemplate.update("DELETE FROM assessments WHERE batch_id = ?", batchId);
        jdbcTemplate.update("DELETE FROM skill_gap_assessments WHERE batch_id = ?", batchId);
      } catch (Exception ex) {
        cleanupErrors.add("Error clearing batch assessments for batch " + batchId + ": " + ex.getMessage());
      }
    }

    // 12. Attendance & Enrollments
    deleteAndVerify("attendance_records", "attendance_id", trackedAttendanceIds, cleanupErrors);
    for (Long batchId : trackedBatchIds) {
      try {
        jdbcTemplate.update("DELETE FROM attendance_records WHERE batch_id = ?", batchId);
      } catch (Exception ex) {
        cleanupErrors.add("Error clearing attendance records for batch " + batchId + ": " + ex.getMessage());
      }
    }
    for (Long enrId : trackedEnrollmentIds) {
      try {
        jdbcTemplate.update("DELETE FROM attendance_records WHERE enrollment_id = ?", enrId);
        jdbcTemplate.update("DELETE FROM trainee_unemployment_events WHERE enrollment_id = ?", enrId);
        jdbcTemplate.update("DELETE FROM job_applications WHERE enrollment_id = ?", enrId);
        jdbcTemplate.update("DELETE FROM followup_tasks WHERE enrollment_id = ?", enrId);
        jdbcTemplate.update("DELETE FROM training_enrollments WHERE enrollment_id = ?", enrId);
      } catch (Exception ex) {
        cleanupErrors.add("Error deleting enrollment " + enrId + ": " + ex.getMessage());
      }
    }
    for (Long batchId : trackedBatchIds) {
      try {
        jdbcTemplate.update("DELETE FROM attendance_records WHERE enrollment_id IN (SELECT enrollment_id FROM training_enrollments WHERE batch_id = ?)", batchId);
        jdbcTemplate.update("DELETE FROM trainee_unemployment_events WHERE enrollment_id IN (SELECT enrollment_id FROM training_enrollments WHERE batch_id = ?)", batchId);
        jdbcTemplate.update("DELETE FROM job_applications WHERE enrollment_id IN (SELECT enrollment_id FROM training_enrollments WHERE batch_id = ?)", batchId);
        jdbcTemplate.update("DELETE FROM followup_tasks WHERE enrollment_id IN (SELECT enrollment_id FROM training_enrollments WHERE batch_id = ?)", batchId);
        jdbcTemplate.update("DELETE FROM training_enrollments WHERE batch_id = ?", batchId);
      } catch (Exception ex) {
        cleanupErrors.add("Error clearing enrollments for batch " + batchId + ": " + ex.getMessage());
      }
    }
    for (Long traineeId : trackedTraineeIds) {
      if (!AUTHORITATIVE_TEST_TRAINEE_ID.equals(traineeId)) {
        try {
          jdbcTemplate.update("DELETE FROM attendance_records WHERE enrollment_id IN (SELECT enrollment_id FROM training_enrollments WHERE trainee_id = ?)", traineeId);
          jdbcTemplate.update("DELETE FROM trainee_unemployment_events WHERE enrollment_id IN (SELECT enrollment_id FROM training_enrollments WHERE trainee_id = ?)", traineeId);
          jdbcTemplate.update("DELETE FROM job_applications WHERE enrollment_id IN (SELECT enrollment_id FROM training_enrollments WHERE trainee_id = ?)", traineeId);
          jdbcTemplate.update("DELETE FROM followup_tasks WHERE trainee_id = ?", traineeId);
          jdbcTemplate.update("DELETE FROM training_enrollments WHERE trainee_id = ?", traineeId);
        } catch (Exception ex) {
          cleanupErrors.add("Error clearing enrollments for trainee " + traineeId + ": " + ex.getMessage());
        }
      }
    }

    // 13. Training Batches & Training Centers
    deleteAndVerify("training_batches", "batch_id", trackedBatchIds, cleanupErrors);
    for (Long centerId : trackedCenterIds) {
      try {
        jdbcTemplate.update("DELETE FROM training_batches WHERE center_id = ?", centerId);
      } catch (Exception ex) {
        cleanupErrors.add("Error clearing batches for center " + centerId + ": " + ex.getMessage());
      }
    }
    deleteAndVerify("training_centers", "center_id", trackedCenterIds, cleanupErrors);
    for (Long provId : trackedProviderIds) {
      try {
        jdbcTemplate.update("DELETE FROM training_centers WHERE provider_id = ?", provId);
      } catch (Exception ex) {
        cleanupErrors.add("Error clearing training centers for provider " + provId + ": " + ex.getMessage());
      }
    }

    // 14. Courses
    for (Long courseId : trackedCourseIds) {
      try {
        jdbcTemplate.update("DELETE FROM course_skills WHERE course_id = ?", courseId);
        jdbcTemplate.update("DELETE FROM course_programs WHERE course_id = ?", courseId);
        jdbcTemplate.update("DELETE FROM courses WHERE course_id = ?", courseId);
      } catch (Exception ex) {
        cleanupErrors.add("Error deleting course " + courseId + ": " + ex.getMessage());
      }
    }

    // 15. Consents & Trainees (strictly preserving authoritative trainee 1005143L)
    for (Long consentId : trackedConsentIds) {
      try {
        jdbcTemplate.update("DELETE FROM trainee_consents WHERE trainee_consent_id = ? AND trainee_id != ?",
            consentId, AUTHORITATIVE_TEST_TRAINEE_ID);
      } catch (Exception ex) {
        cleanupErrors.add("Error deleting consent " + consentId + ": " + ex.getMessage());
      }
    }
    for (Long traineeId : trackedTraineeIds) {
      if (!AUTHORITATIVE_TEST_TRAINEE_ID.equals(traineeId)) {
        try {
          jdbcTemplate.update("DELETE FROM trainee_channel_preferences WHERE trainee_id = ?", traineeId);
          jdbcTemplate.update("DELETE FROM trainee_identity_hashes WHERE trainee_id = ?", traineeId);
          jdbcTemplate.update("DELETE FROM trainee_consents WHERE trainee_id = ?", traineeId);
          jdbcTemplate.update("DELETE FROM trainees WHERE trainee_id = ?", traineeId);
        } catch (Exception ex) {
          cleanupErrors.add("Error deleting trainee " + traineeId + ": " + ex.getMessage());
        }
      }
    }

    // 16. Users & User Roles (strictly preserving user 1L)
    // Must be deleted BEFORE employers, training providers, departments, organizations, states, districts!
    for (Long userId : trackedUserIds) {
      if (userId != null && !userId.equals(1L)) {
        try {
          jdbcTemplate.update("DELETE FROM user_roles WHERE user_id = ?", userId);
          jdbcTemplate.update("DELETE FROM users WHERE user_id = ?", userId);
        } catch (Exception ex) {
          cleanupErrors.add("Error deleting user " + userId + ": " + ex.getMessage());
        }
      }
    }

    // 17. Employers & Training Providers
    for (Long empId : trackedEmployerIds) {
      try {
        jdbcTemplate.update("DELETE FROM users WHERE employer_id = ? AND user_id != 1", empId);
        jdbcTemplate.update("DELETE FROM employer_branches WHERE employer_id = ?", empId);
        jdbcTemplate.update("DELETE FROM job_postings WHERE employer_id = ?", empId);
        jdbcTemplate.update("DELETE FROM employers WHERE employer_id = ?", empId);
      } catch (Exception ex) {
        cleanupErrors.add("Error deleting employer " + empId + ": " + ex.getMessage());
      }
    }
    for (Long provId : trackedProviderIds) {
      try {
        jdbcTemplate.update("DELETE FROM users WHERE training_provider_id = ? AND user_id != 1", provId);
        jdbcTemplate.update("DELETE FROM program_training_providers WHERE provider_id = ?", provId);
        jdbcTemplate.update("DELETE FROM training_providers WHERE provider_id = ?", provId);
      } catch (Exception ex) {
        cleanupErrors.add("Error deleting provider " + provId + ": " + ex.getMessage());
      }
    }

    // 18. Programs, Schemes, Departments, Organizations
    for (Long prgId : trackedProgramIds) {
      try {
        jdbcTemplate.update("DELETE FROM program_training_providers WHERE program_id = ?", prgId);
        jdbcTemplate.update("DELETE FROM course_programs WHERE program_id = ?", prgId);
        jdbcTemplate.update("DELETE FROM training_batches WHERE program_id = ?", prgId);
        jdbcTemplate.update("DELETE FROM programs WHERE program_id = ?", prgId);
      } catch (Exception ex) {
        cleanupErrors.add("Error deleting program " + prgId + ": " + ex.getMessage());
      }
    }
    deleteAndVerify("schemes", "scheme_id", trackedSchemeIds, cleanupErrors);
    for (Long deptId : trackedDeptIds) {
      try {
        jdbcTemplate.update("DELETE FROM users WHERE department_id = ? AND user_id != 1", deptId);
        jdbcTemplate.update("DELETE FROM programs WHERE department_id = ?", deptId);
        jdbcTemplate.update("DELETE FROM schemes WHERE department_id = ?", deptId);
        jdbcTemplate.update("DELETE FROM departments WHERE department_id = ?", deptId);
      } catch (Exception ex) {
        cleanupErrors.add("Error deleting department " + deptId + ": " + ex.getMessage());
      }
    }
    for (Long orgId : trackedOrgIds) {
      try {
        jdbcTemplate.update("DELETE FROM users WHERE organization_id = ? AND user_id != 1", orgId);
        jdbcTemplate.update("DELETE FROM training_providers WHERE organization_id = ?", orgId);
        jdbcTemplate.update("DELETE FROM employers WHERE organization_id = ?", orgId);
        jdbcTemplate.update("DELETE FROM departments WHERE organization_id = ?", orgId);
        jdbcTemplate.update("DELETE FROM organizations WHERE organization_id = ?", orgId);
      } catch (Exception ex) {
        cleanupErrors.add("Error deleting organization " + orgId + ": " + ex.getMessage());
      }
    }

    // 19. Sectors
    for (Long sectorId : trackedSectorIds) {
      try {
        jdbcTemplate.update("DELETE FROM job_roles WHERE sector_id = ?", sectorId);
        jdbcTemplate.update("DELETE FROM courses WHERE sector_id = ?", sectorId);
      } catch (Exception ex) {
        cleanupErrors.add("Error clearing courses/roles for sector " + sectorId + ": " + ex.getMessage());
      }
    }
    deleteAndVerify("sectors", "sector_id", trackedSectorIds, cleanupErrors);

    // 20. Geographic Fixtures (strictly preserving State 101, District 1001)
    for (Long distId : trackedDistrictIds) {
      if (!BASE_DISTRICT_ID.equals(distId)) {
        try {
          jdbcTemplate.update("DELETE FROM users WHERE district_id = ? AND user_id != 1", distId);
          jdbcTemplate.update("DELETE FROM trainees WHERE district_id = ? AND trainee_id != ?", distId, AUTHORITATIVE_TEST_TRAINEE_ID);
          jdbcTemplate.update("DELETE FROM employer_branches WHERE district_id = ?", distId);
          jdbcTemplate.update("DELETE FROM training_centers WHERE district_id = ?", distId);
          jdbcTemplate.update("DELETE FROM districts WHERE district_id = ?", distId);
        } catch (Exception ex) {
          cleanupErrors.add("Error deleting district " + distId + ": " + ex.getMessage());
        }
      }
    }
    for (Long stId : trackedStateIds) {
      if (!BASE_STATE_ID.equals(stId)) {
        try {
          jdbcTemplate.update("DELETE FROM users WHERE state_id = ? AND user_id != 1", stId);
          jdbcTemplate.update("DELETE FROM trainees WHERE state_id = ? AND trainee_id != ?", stId, AUTHORITATIVE_TEST_TRAINEE_ID);
          jdbcTemplate.update("DELETE FROM employer_branches WHERE state_id = ?", stId);
          jdbcTemplate.update("DELETE FROM training_centers WHERE state_id = ?", stId);
          jdbcTemplate.update("DELETE FROM districts WHERE state_id = ?", stId);
          jdbcTemplate.update("DELETE FROM states WHERE state_id = ?", stId);
        } catch (Exception ex) {
          cleanupErrors.add("Error deleting state " + stId + ": " + ex.getMessage());
        }
      }
    }

    // 16. Post-Deletion Verification: Check that test-owned records have been removed
    verifyRecordsRemoved("trainees", "trainee_id", trackedTraineeIds, AUTHORITATIVE_TEST_TRAINEE_ID, cleanupErrors);
    verifyRecordsRemoved("employment_records", "employment_id", trackedEmploymentIds, null, cleanupErrors);
    verifyRecordsRemoved("placement_records", "placement_id", trackedPlacementIds, null, cleanupErrors);
    verifyRecordsRemoved("training_enrollments", "enrollment_id", trackedEnrollmentIds, null, cleanupErrors);
    verifyRecordsRemoved("training_batches", "batch_id", trackedBatchIds, null, cleanupErrors);
    verifyRecordsRemoved("surveys", "survey_id", trackedSurveyIds, null, cleanupErrors);
    verifyRecordsRemoved("survey_template_versions", "survey_template_version_id", trackedSurveyTemplateVersionIds, null, cleanupErrors);
    verifyRecordsRemoved("survey_templates", "survey_template_id", trackedSurveyTemplateIds, null, cleanupErrors);
    verifyRecordsRemoved("users", "user_id", trackedUserIds, 1L, cleanupErrors);

    // 17. Report cleanup failures: Fail test when mandatory cleanup fails
    if (!cleanupErrors.isEmpty()) {
      throw new AssertionError("Test fixture teardown cleanup failed with " + cleanupErrors.size() + " error(s):\n - " +
          String.join("\n - ", cleanupErrors));
    }

    // 18. ONLY clear tracking collections if cleanup has been verified
    clearAllTrackingCollections();
  }

  protected void clearAllTrackingCollections() {
    trackedCommunicationJobIds.clear();
    trackedCommunicationLogIds.clear();
    trackedDqIssueIds.clear();
    trackedImportBatchIds.clear();
    trackedSurveyResponseIds.clear();
    trackedSurveyIds.clear();
    trackedSurveyTemplateVersionIds.clear();
    trackedSurveyTemplateIds.clear();
    trackedSalaryHistoryIds.clear();
    trackedVerificationIds.clear();
    trackedExitEventIds.clear();
    trackedUnemploymentEventIds.clear();
    trackedEmploymentIds.clear();
    trackedPlacementIds.clear();
    trackedJobPostingIds.clear();
    trackedJobRoleIds.clear();
    trackedCertificationIds.clear();
    trackedAssessmentResultIds.clear();
    trackedTraineeAssessmentIds.clear();
    trackedAssessmentIds.clear();
    trackedAttendanceIds.clear();
    trackedEnrollmentIds.clear();
    trackedBatchIds.clear();
    trackedCenterIds.clear();
    trackedCourseIds.clear();
    trackedSectorIds.clear();
    trackedProgramIds.clear();
    trackedSchemeIds.clear();
    trackedDeptIds.clear();
    trackedOrgIds.clear();
    trackedConsentIds.clear();
    trackedTraineeIds.clear();
    trackedEmployerIds.clear();
    trackedProviderIds.clear();
    trackedUserIds.clear();
    trackedDistrictIds.clear();
    trackedStateIds.clear();
  }

  // --- Security & HTTP Helpers ---

  protected String generateBearerToken(Long userId, String username, Collection<String> authorities) {
    return "Bearer " + jwtTokenProvider.generateAccessToken(userId, username, authorities);
  }

  protected HttpHeaders createAuthHeaders(String bearerToken) {
    HttpHeaders headers = new HttpHeaders();
    headers.set(HttpHeaders.AUTHORIZATION, bearerToken);
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }

  protected HttpHeaders createAuthMultipartHeaders(String bearerToken) {
    HttpHeaders headers = new HttpHeaders();
    headers.set(HttpHeaders.AUTHORIZATION, bearerToken);
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    return headers;
  }

  protected String getBaseUrl(String relativePath) {
    if (!relativePath.startsWith("/")) {
      relativePath = "/" + relativePath;
    }
    return "http://localhost:" + port + relativePath;
  }

  protected JsonNode parseResponseJson(String responseBody) throws Exception {
    return objectMapper.readTree(responseBody);
  }
}
