package in.gov.sih.sih26135.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.sih.sih26135.MysqlConnectionTest;
import in.gov.sih.sih26135.security.jwt.JwtTokenProvider;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
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
      throw new RuntimeException("Test fixture teardown failed", e);
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

  protected void teardownTrackedFixtures() {
    // 1. Communication Queue, Logs & Rate Limit Events
    for (Long jobId : trackedCommunicationJobIds) {
      try {
        jdbcTemplate.update("DELETE FROM communication_dispatch_attempts WHERE job_id = ?", jobId);
        jdbcTemplate.update("DELETE FROM communication_dispatch_jobs WHERE job_id = ?", jobId);
      } catch (Exception ignored) {}
    }
    for (Long logId : trackedCommunicationLogIds) {
      try {
        jdbcTemplate.update("DELETE FROM communication_logs WHERE communication_log_id = ?", logId);
      } catch (Exception ignored) {}
    }
    for (Long traineeId : trackedTraineeIds) {
      if (!AUTHORITATIVE_TEST_TRAINEE_ID.equals(traineeId)) {
        try {
          jdbcTemplate.update("DELETE FROM communication_rate_limit_events WHERE limiter_key LIKE ? OR limiter_key LIKE ?",
              "TRAINEE:%:" + traineeId, "DEDUP:" + traineeId + ":%");
        } catch (Exception ignored) {}
      }
    }
    for (Long providerId : trackedProviderIds) {
      try {
        jdbcTemplate.update("DELETE FROM communication_rate_limit_events WHERE limiter_key = ?", "PROVIDER:" + providerId);
      } catch (Exception ignored) {}
    }

    // 2. Data Quality Issues & Events
    for (Long issueId : trackedDqIssueIds) {
      try {
        jdbcTemplate.update("DELETE FROM data_quality_issue_events WHERE issue_id = ?", issueId);
        jdbcTemplate.update("DELETE FROM data_quality_issues WHERE issue_id = ?", issueId);
      } catch (Exception ignored) {}
    }

    // 3. Import Batches & Quarantined Records
    for (Long batchId : trackedImportBatchIds) {
      try {
        jdbcTemplate.update("DELETE FROM import_records WHERE import_batch_id = ?", batchId);
        jdbcTemplate.update("DELETE FROM import_batches WHERE import_batch_id = ?", batchId);
      } catch (Exception ignored) {}
    }

    // 4. Surveys
    for (Long responseId : trackedSurveyResponseIds) {
      try {
        jdbcTemplate.update("DELETE FROM survey_response_answers WHERE survey_response_id = ?", responseId);
        jdbcTemplate.update("DELETE FROM survey_responses WHERE survey_response_id = ?", responseId);
      } catch (Exception ignored) {}
    }

    // 5. Employment Verifications & Salary History
    for (Long salaryId : trackedSalaryHistoryIds) {
      try {
        jdbcTemplate.update("DELETE FROM salary_history WHERE salary_history_id = ?", salaryId);
      } catch (Exception ignored) {}
    }
    for (Long verifId : trackedVerificationIds) {
      try {
        jdbcTemplate.update("DELETE FROM employment_verification_evidence WHERE employment_verification_id = ?", verifId);
        jdbcTemplate.update("DELETE FROM employment_verifications WHERE employment_verification_id = ?", verifId);
      } catch (Exception ignored) {}
    }
    for (Long empId : trackedEmploymentIds) {
      try {
        jdbcTemplate.update("DELETE FROM salary_history WHERE employment_id = ?", empId);
        jdbcTemplate.update("DELETE FROM employment_verification_requests WHERE employment_id = ?", empId);
        jdbcTemplate.update("DELETE FROM employment_verifications WHERE employment_id = ?", empId);
      } catch (Exception ignored) {}
    }

    // 6. Unemployment & Exit Events
    for (Long unempId : trackedUnemploymentEventIds) {
      try {
        jdbcTemplate.update("DELETE FROM trainee_unemployment_events WHERE unemployment_event_id = ?", unempId);
      } catch (Exception ignored) {}
    }
    for (Long exitId : trackedExitEventIds) {
      try {
        jdbcTemplate.update("DELETE FROM employment_exit_events WHERE employment_exit_event_id = ?", exitId);
      } catch (Exception ignored) {}
    }

    // 7. Employment & Placement Records
    for (Long empId : trackedEmploymentIds) {
      try {
        jdbcTemplate.update("DELETE FROM employment_records WHERE employment_id = ?", empId);
      } catch (Exception ignored) {}
    }
    for (Long placementId : trackedPlacementIds) {
      try {
        jdbcTemplate.update("DELETE FROM placement_records WHERE placement_id = ?", placementId);
      } catch (Exception ignored) {}
    }
    for (Long jobId : trackedJobPostingIds) {
      try {
        jdbcTemplate.update("DELETE FROM job_postings WHERE job_posting_id = ?", jobId);
      } catch (Exception ignored) {}
    }

    // 8. Assessments & Certifications
    for (Long certId : trackedCertificationIds) {
      try {
        jdbcTemplate.update("DELETE FROM certifications WHERE certification_id = ?", certId);
      } catch (Exception ignored) {}
    }
    for (Long resId : trackedAssessmentResultIds) {
      try {
        jdbcTemplate.update("DELETE FROM assessment_results WHERE assessment_result_id = ?", resId);
      } catch (Exception ignored) {}
    }
    for (Long taId : trackedTraineeAssessmentIds) {
      try {
        jdbcTemplate.update("DELETE FROM trainee_assessments WHERE trainee_assessment_id = ?", taId);
      } catch (Exception ignored) {}
    }
    for (Long asmId : trackedAssessmentIds) {
      try {
        jdbcTemplate.update("DELETE FROM assessments WHERE assessment_id = ?", asmId);
      } catch (Exception ignored) {}
    }

    // 9. Attendance & Enrollments
    for (Long attId : trackedAttendanceIds) {
      try {
        jdbcTemplate.update("DELETE FROM attendance_records WHERE attendance_id = ?", attId);
      } catch (Exception ignored) {}
    }
    for (Long enrId : trackedEnrollmentIds) {
      try {
        jdbcTemplate.update("DELETE FROM training_enrollments WHERE enrollment_id = ?", enrId);
      } catch (Exception ignored) {}
    }

    // 10. Training Batches, Centers, Courses
    for (Long batchId : trackedBatchIds) {
      try {
        jdbcTemplate.update("DELETE FROM training_batches WHERE batch_id = ?", batchId);
      } catch (Exception ignored) {}
    }
    for (Long centerId : trackedCenterIds) {
      try {
        jdbcTemplate.update("DELETE FROM training_centers WHERE center_id = ?", centerId);
      } catch (Exception ignored) {}
    }
    for (Long courseId : trackedCourseIds) {
      try {
        jdbcTemplate.update("DELETE FROM course_skills WHERE course_id = ?", courseId);
        jdbcTemplate.update("DELETE FROM courses WHERE course_id = ?", courseId);
      } catch (Exception ignored) {}
    }

    // 11. Consents & Trainees (strictly preserve 1005143L)
    for (Long consentId : trackedConsentIds) {
      try {
        jdbcTemplate.update("DELETE FROM trainee_consents WHERE trainee_consent_id = ? AND trainee_id != ?",
            consentId, AUTHORITATIVE_TEST_TRAINEE_ID);
      } catch (Exception ignored) {}
    }
    for (Long traineeId : trackedTraineeIds) {
      if (!AUTHORITATIVE_TEST_TRAINEE_ID.equals(traineeId)) {
        try {
          jdbcTemplate.update("DELETE FROM trainee_consents WHERE trainee_id = ?", traineeId);
          jdbcTemplate.update("DELETE FROM trainees WHERE trainee_id = ?", traineeId);
        } catch (Exception ignored) {}
      }
    }

    // 12. Programs, Schemes, Depts, Orgs, Sectors
    for (Long prgId : trackedProgramIds) {
      try {
        jdbcTemplate.update("DELETE FROM programs WHERE program_id = ?", prgId);
      } catch (Exception ignored) {}
    }
    for (Long schId : trackedSchemeIds) {
      try {
        jdbcTemplate.update("DELETE FROM schemes WHERE scheme_id = ?", schId);
      } catch (Exception ignored) {}
    }
    for (Long deptId : trackedDeptIds) {
      try {
        jdbcTemplate.update("DELETE FROM departments WHERE department_id = ?", deptId);
      } catch (Exception ignored) {}
    }
    for (Long orgId : trackedOrgIds) {
      try {
        jdbcTemplate.update("DELETE FROM organizations WHERE organization_id = ?", orgId);
      } catch (Exception ignored) {}
    }
    for (Long jrId : trackedJobRoleIds) {
      try {
        jdbcTemplate.update("DELETE FROM job_roles WHERE job_role_id = ?", jrId);
      } catch (Exception ignored) {}
    }
    for (Long secId : trackedSectorIds) {
      try {
        jdbcTemplate.update("DELETE FROM sectors WHERE sector_id = ?", secId);
      } catch (Exception ignored) {}
    }

    // 13. Employers, Providers, Users (strictly preserve user 1)
    for (Long empId : trackedEmployerIds) {
      try {
        jdbcTemplate.update("DELETE FROM employers WHERE employer_id = ?", empId);
      } catch (Exception ignored) {}
    }
    for (Long provId : trackedProviderIds) {
      try {
        jdbcTemplate.update("DELETE FROM training_providers WHERE provider_id = ?", provId);
      } catch (Exception ignored) {}
    }
    for (Long userId : trackedUserIds) {
      if (userId != null && !userId.equals(1L)) {
        try {
          jdbcTemplate.update("DELETE FROM user_roles WHERE user_id = ?", userId);
          jdbcTemplate.update("DELETE FROM users WHERE user_id = ?", userId);
        } catch (Exception ignored) {}
      }
    }

    // 14. Geographic Fixtures (strictly preserve state 101, district 1001)
    for (Long distId : trackedDistrictIds) {
      if (!BASE_DISTRICT_ID.equals(distId)) {
        try {
          jdbcTemplate.update("DELETE FROM districts WHERE district_id = ?", distId);
        } catch (Exception ignored) {}
      }
    }
    for (Long stId : trackedStateIds) {
      if (!BASE_STATE_ID.equals(stId)) {
        try {
          jdbcTemplate.update("DELETE FROM states WHERE state_id = ?", stId);
        } catch (Exception ignored) {}
      }
    }

    // Clear tracking collections
    trackedCommunicationJobIds.clear();
    trackedCommunicationLogIds.clear();
    trackedDqIssueIds.clear();
    trackedImportBatchIds.clear();
    trackedSurveyResponseIds.clear();
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
