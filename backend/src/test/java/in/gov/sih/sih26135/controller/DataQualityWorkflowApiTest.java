package in.gov.sih.sih26135.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.sih.sih26135.MysqlConnectionTest;
import in.gov.sih.sih26135.dto.request.AssignDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.request.DismissDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.request.ReopenDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.request.ResolveDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.request.StartInvestigationRequest;
import in.gov.sih.sih26135.entity.DataQualityRule;
import in.gov.sih.sih26135.security.jwt.JwtTokenProvider;
import in.gov.sih.sih26135.service.DataQualityRuleCatalogService;
import in.gov.sih.sih26135.service.dq.DataQualityIssueRegistrationService;
import in.gov.sih.sih26135.service.dq.RegistrationResult;
import in.gov.sih.sih26135.service.dq.RegistrationStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Phase 28.4 - Data Quality Workflow, Lifecycle Transitions & Audit API Integration Tests")
class DataQualityWorkflowApiTest {

  @Autowired
  private DataSource dataSource;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private DataQualityRuleCatalogService catalogService;

  @Autowired
  private DataQualityIssueRegistrationService registrationService;

  private String adminToken;
  private String unauthenticatedToken;
  private Long testUserId;
  private Long testReviewerUserId;

  @BeforeEach
  void setUp() throws Exception {
    try (Connection conn = dataSource.getConnection()) {
      String catalog = conn.getCatalog();
      MysqlConnectionTest.assertDedicatedTestCatalog(catalog);
      assertThat(catalog).isEqualToIgnoringCase(MysqlConnectionTest.EXPECTED_TEST_CATALOG);
    }

    catalogService.ensureDefaultRulesProvisioned();

    // Clean up test users & issues if existing
    cleanUpTestData();

    // Insert an active admin user (user_id = 9980) and reviewer user (user_id = 9981)
    jdbcTemplate.update(
        "INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, created_at, updated_at) " +
            "VALUES (9980, 'wf_admin_test', 'wf_admin@test.gov.in', 'hash', 'argon2id', 1, NOW(), NOW())"
    );
    jdbcTemplate.update(
        "INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, created_at, updated_at) " +
            "VALUES (9981, 'wf_reviewer_test', 'wf_reviewer@test.gov.in', 'hash', 'argon2id', 1, NOW(), NOW())"
    );
    testUserId = 9980L;
    testReviewerUserId = 9981L;

    adminToken = "Bearer " + jwtTokenProvider.generateAccessToken(
        testUserId,
        "wf_admin_test",
        List.of("ROLE_super_admin", "audit.read", "system.manage")
    );

    unauthenticatedToken = "Bearer " + jwtTokenProvider.generateAccessToken(
        9999L,
        "unknown_user",
        List.of("ROLE_GUEST")
    );
  }

  @AfterEach
  void tearDown() {
    cleanUpTestData();
  }

  private void cleanUpTestData() {
    try {
      jdbcTemplate.update("DELETE FROM data_quality_issue_events WHERE changed_by_user_id IN (9980, 9981)");
      jdbcTemplate.update("DELETE FROM data_quality_issues WHERE issue_summary LIKE 'WF-TEST-%' OR issue_summary = 'Re-detected salary discrepancy'");
      jdbcTemplate.update("DELETE FROM users WHERE user_id IN (9980, 9981)");
    } catch (Exception ignored) {
    }
  }

  private Long insertTestIssue(String statusCode, String summary) {
    Long statusId = jdbcTemplate.queryForObject(
        "SELECT data_quality_issue_status_id FROM ref_data_quality_issue_status WHERE status_code = ?",
        Long.class, statusCode
    );
    Long ruleId = jdbcTemplate.queryForObject(
        "SELECT MIN(data_quality_rule_id) FROM data_quality_rules", Long.class
    );
    Long catId = jdbcTemplate.queryForObject(
        "SELECT MIN(data_quality_category_id) FROM ref_data_quality_category", Long.class
    );
    Long sevId = jdbcTemplate.queryForObject(
        "SELECT MIN(data_quality_severity_id) FROM ref_data_quality_severity", Long.class
    );
    Long srcId = jdbcTemplate.queryForObject(
        "SELECT MIN(data_quality_detection_source_id) FROM ref_data_quality_detection_source", Long.class
    );

    org.springframework.jdbc.support.KeyHolder keyHolder = new org.springframework.jdbc.support.GeneratedKeyHolder();
    jdbcTemplate.update(connection -> {
      java.sql.PreparedStatement ps = connection.prepareStatement(
          "INSERT INTO data_quality_issues (" +
              "data_quality_rule_id, data_quality_category_id, data_quality_severity_id, " +
              "data_quality_issue_status_id, data_quality_detection_source_id, entity_type, " +
              "entity_id, issue_summary, detected_at, created_at, updated_at) " +
              "VALUES (?, ?, ?, ?, ?, 'Trainee', 12345, ?, NOW() - INTERVAL 1 HOUR, NOW(), NOW())",
          java.sql.Statement.RETURN_GENERATED_KEYS
      );
      ps.setLong(1, ruleId);
      ps.setLong(2, catId);
      ps.setLong(3, sevId);
      ps.setLong(4, statusId);
      ps.setLong(5, srcId);
      ps.setString(6, summary);
      return ps;
    }, keyHolder);

    return keyHolder.getKey().longValue();
  }

  @Nested
  @DisplayName("1. Assignment Lifecycle API (POST /{id}/assign)")
  class AssignApiTests {

    @Test
    @DisplayName("Should successfully assign an OPEN issue using request body")
    void shouldAssignWithRequestBody() throws Exception {
      Long issueId = insertTestIssue("OPEN", "WF-TEST-ASSIGN-BODY");
      AssignDataQualityIssueRequest request = new AssignDataQualityIssueRequest(testReviewerUserId, "Assigning to reviewer");

      mockMvc.perform(post("/api/v1/data-quality-issues/{id}/assign", issueId)
              .header("Authorization", adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success", is(true)))
          .andExpect(jsonPath("$.message", is("Data quality issue assigned successfully")))
          .andExpect(jsonPath("$.data.id", is(issueId.intValue())))
          .andExpect(jsonPath("$.data.dataQualityIssueStatusCode", is("ASSIGNED")))
          .andExpect(jsonPath("$.data.assignedUserId", is(testReviewerUserId.intValue())));

      // Verify event was inserted into data_quality_issue_events
      Integer eventCount = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM data_quality_issue_events WHERE data_quality_issue_id = ? AND changed_by_user_id = ?",
          Integer.class, issueId, testUserId
      );
      assertThat(eventCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Should successfully assign an OPEN issue using backward-compatible query param")
    void shouldAssignWithQueryParam() throws Exception {
      Long issueId = insertTestIssue("OPEN", "WF-TEST-ASSIGN-PARAM");

      mockMvc.perform(post("/api/v1/data-quality-issues/{id}/assign", issueId)
              .header("Authorization", adminToken)
              .param("assignedUserId", testReviewerUserId.toString()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success", is(true)))
          .andExpect(jsonPath("$.data.dataQualityIssueStatusCode", is("ASSIGNED")))
          .andExpect(jsonPath("$.data.assignedUserId", is(testReviewerUserId.intValue())));
    }

    @Test
    @DisplayName("Should reject assigning an IN_PROGRESS issue with 409 Conflict")
    void shouldRejectAssigningInProgress() throws Exception {
      Long issueId = insertTestIssue("IN_PROGRESS", "WF-TEST-ASSIGN-CONFLICT");
      AssignDataQualityIssueRequest request = new AssignDataQualityIssueRequest(testReviewerUserId);

      mockMvc.perform(post("/api/v1/data-quality-issues/{id}/assign", issueId)
              .header("Authorization", adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code", is("INVALID_DQ_TRANSITION")));
    }
  }

  @Nested
  @DisplayName("2. Start Investigation API (POST /{id}/start-investigation)")
  class StartInvestigationApiTests {

    @Test
    @DisplayName("Should successfully transition ASSIGNED issue to IN_PROGRESS")
    void shouldStartInvestigationSuccessfully() throws Exception {
      Long issueId = insertTestIssue("ASSIGNED", "WF-TEST-INVESTIGATE-SUCCESS");
      StartInvestigationRequest request = new StartInvestigationRequest("Forensic investigation started");

      mockMvc.perform(post("/api/v1/data-quality-issues/{id}/start-investigation", issueId)
              .header("Authorization", adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success", is(true)))
          .andExpect(jsonPath("$.data.dataQualityIssueStatusCode", is("IN_PROGRESS")));

      // Verify event was saved
      List<Map<String, Object>> events = jdbcTemplate.queryForList(
          "SELECT * FROM data_quality_issue_events WHERE data_quality_issue_id = ?", issueId
      );
      assertThat(events).hasSize(1);
      assertThat(events.get(0).get("remarks")).isEqualTo("Forensic investigation started");
    }

    @Test
    @DisplayName("Should reject transitioning OPEN issue to IN_PROGRESS directly with 409 Conflict")
    void shouldRejectDirectStartFromOpen() throws Exception {
      Long issueId = insertTestIssue("OPEN", "WF-TEST-INVESTIGATE-DIRECT-OPEN");

      mockMvc.perform(post("/api/v1/data-quality-issues/{id}/start-investigation", issueId)
              .header("Authorization", adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code", is("INVALID_DQ_TRANSITION")));
    }
  }

  @Nested
  @DisplayName("3. Resolution API (POST /{id}/resolve)")
  class ResolveApiTests {

    @Test
    @DisplayName("Should successfully resolve an IN_PROGRESS issue and populate server attribution")
    void shouldResolveInProgressIssueSuccessfully() throws Exception {
      Long issueId = insertTestIssue("IN_PROGRESS", "WF-TEST-RESOLVE-SUCCESS");
      ResolveDataQualityIssueRequest request = new ResolveDataQualityIssueRequest("Remediated in source trainee directory");

      mockMvc.perform(post("/api/v1/data-quality-issues/{id}/resolve", issueId)
              .header("Authorization", adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success", is(true)))
          .andExpect(jsonPath("$.data.dataQualityIssueStatusCode", is("RESOLVED")))
          .andExpect(jsonPath("$.data.resolvedByUserId", is(testUserId.intValue())))
          .andExpect(jsonPath("$.data.resolvedAt", notNullValue()))
          .andExpect(jsonPath("$.data.resolutionNotes", is("Remediated in source trainee directory")));

      // Verify database state directly
      Map<String, Object> row = jdbcTemplate.queryForMap(
          "SELECT resolved_by_user_id, resolution_notes FROM data_quality_issues WHERE data_quality_issue_id = ?", issueId
      );
      assertThat(((Number) row.get("resolved_by_user_id")).longValue()).isEqualTo(testUserId);
      assertThat(row.get("resolution_notes")).isEqualTo("Remediated in source trainee directory");
    }

    @Test
    @DisplayName("Should reject resolution from OPEN with 409 Conflict")
    void shouldRejectPrematureResolutionFromOpen() throws Exception {
      Long issueId = insertTestIssue("OPEN", "WF-TEST-RESOLVE-PREMATURE");
      ResolveDataQualityIssueRequest request = new ResolveDataQualityIssueRequest("Premature attempt");

      mockMvc.perform(post("/api/v1/data-quality-issues/{id}/resolve", issueId)
              .header("Authorization", adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code", is("INVALID_DQ_TRANSITION")));
    }

    @Test
    @DisplayName("Should reject blank resolution notes with 400 Bad Request")
    void shouldRejectBlankResolutionNotes() throws Exception {
      Long issueId = insertTestIssue("IN_PROGRESS", "WF-TEST-RESOLVE-BLANK");
      String payload = "{\"resolutionNotes\": \"   \"}";

      mockMvc.perform(post("/api/v1/data-quality-issues/{id}/resolve", issueId)
              .header("Authorization", adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(payload))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("4. Dismissal API (POST /{id}/dismiss)")
  class DismissApiTests {

    @Test
    @DisplayName("Should successfully dismiss an OPEN issue with mandatory remarks")
    void shouldDismissOpenIssueSuccessfully() throws Exception {
      Long issueId = insertTestIssue("OPEN", "WF-TEST-DISMISS-OPEN");
      DismissDataQualityIssueRequest request = new DismissDataQualityIssueRequest("Verified as expected business exception");

      mockMvc.perform(post("/api/v1/data-quality-issues/{id}/dismiss", issueId)
              .header("Authorization", adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success", is(true)))
          .andExpect(jsonPath("$.data.dataQualityIssueStatusCode", is("DISMISSED")))
          .andExpect(jsonPath("$.data.resolutionNotes", is("Verified as expected business exception")));
    }

    @Test
    @DisplayName("Should reject dismissing an already RESOLVED issue with 409 Conflict")
    void shouldRejectDismissingResolvedIssue() throws Exception {
      Long issueId = insertTestIssue("RESOLVED", "WF-TEST-DISMISS-CONFLICT");
      DismissDataQualityIssueRequest request = new DismissDataQualityIssueRequest("Cannot dismiss resolved issue");

      mockMvc.perform(post("/api/v1/data-quality-issues/{id}/dismiss", issueId)
              .header("Authorization", adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code", is("INVALID_DQ_TRANSITION")));
    }
  }

  @Nested
  @DisplayName("5. Reopening API (POST /{id}/reopen)")
  class ReopenApiTests {

    @Test
    @DisplayName("Should successfully reopen a RESOLVED issue, reset attribution, and log REOPEN event")
    void shouldReopenResolvedIssueSuccessfully() throws Exception {
      Long issueId = insertTestIssue("RESOLVED", "WF-TEST-REOPEN-SUCCESS");
      // Set initial resolution attribution
      jdbcTemplate.update(
          "UPDATE data_quality_issues SET resolved_at = NOW(), resolved_by_user_id = ?, resolution_notes = 'Old note' WHERE data_quality_issue_id = ?",
          testUserId, issueId
      );

      ReopenDataQualityIssueRequest request = new ReopenDataQualityIssueRequest("Anomaly re-detected after data sync");

      mockMvc.perform(post("/api/v1/data-quality-issues/{id}/reopen", issueId)
              .header("Authorization", adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success", is(true)))
          .andExpect(jsonPath("$.data.dataQualityIssueStatusCode", is("REOPENED")))
          .andExpect(jsonPath("$.data.resolvedByUserId", nullValue()))
          .andExpect(jsonPath("$.data.resolvedAt", nullValue()))
          .andExpect(jsonPath("$.data.resolutionNotes", nullValue()));

      // Check database state
      Map<String, Object> issueRow = jdbcTemplate.queryForMap(
          "SELECT resolved_at, resolved_by_user_id, resolution_notes FROM data_quality_issues WHERE data_quality_issue_id = ?", issueId
      );
      assertThat(issueRow.get("resolved_at")).isNull();
      assertThat(issueRow.get("resolved_by_user_id")).isNull();
      assertThat(issueRow.get("resolution_notes")).isNull();

      // Check event history
      List<Map<String, Object>> events = jdbcTemplate.queryForList(
          "SELECT * FROM data_quality_issue_events WHERE data_quality_issue_id = ?", issueId
      );
      assertThat(events).hasSize(1);
      assertThat(events.get(0).get("remarks")).isEqualTo("Anomaly re-detected after data sync");
    }

    @Test
    @DisplayName("Should reject reopening a DISMISSED issue with 409 Conflict")
    void shouldRejectReopeningDismissedIssue() throws Exception {
      Long issueId = insertTestIssue("DISMISSED", "WF-TEST-REOPEN-DISMISSED");
      ReopenDataQualityIssueRequest request = new ReopenDataQualityIssueRequest("Cannot reopen dismissed");

      mockMvc.perform(post("/api/v1/data-quality-issues/{id}/reopen", issueId)
              .header("Authorization", adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code", is("INVALID_DQ_TRANSITION")));
    }
  }

  @Nested
  @DisplayName("6. Events Audit Trail API (GET /{id}/events)")
  class EventsAuditTrailApiTests {

    @Test
    @DisplayName("Should retrieve full audit history of lifecycle transitions")
    void shouldRetrieveIssueEventsSuccessfully() throws Exception {
      Long issueId = insertTestIssue("OPEN", "WF-TEST-EVENTS-TRAIL");

      // Step 1: Assign
      mockMvc.perform(post("/api/v1/data-quality-issues/{id}/assign", issueId)
              .header("Authorization", adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(new AssignDataQualityIssueRequest(testReviewerUserId, "Step 1: Assign"))))
          .andExpect(status().isOk());

      // Step 2: Start Investigation
      mockMvc.perform(post("/api/v1/data-quality-issues/{id}/start-investigation", issueId)
              .header("Authorization", adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(new StartInvestigationRequest("Step 2: Investigate"))))
          .andExpect(status().isOk());

      // Step 3: Resolve
      mockMvc.perform(post("/api/v1/data-quality-issues/{id}/resolve", issueId)
              .header("Authorization", adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(new ResolveDataQualityIssueRequest("Step 3: Resolve"))))
          .andExpect(status().isOk());

      // Step 4: Reopen
      mockMvc.perform(post("/api/v1/data-quality-issues/{id}/reopen", issueId)
              .header("Authorization", adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(new ReopenDataQualityIssueRequest("Step 4: Reopen"))))
          .andExpect(status().isOk());

      // Retrieve events
      mockMvc.perform(get("/api/v1/data-quality-issues/{id}/events", issueId)
              .header("Authorization", adminToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success", is(true)))
          .andExpect(jsonPath("$.data", hasSize(4)))
          .andExpect(jsonPath("$.data[0].dataQualityIssueStatusCode", is("ASSIGNED")))
          .andExpect(jsonPath("$.data[0].remarks", is("Step 1: Assign")))
          .andExpect(jsonPath("$.data[1].dataQualityIssueStatusCode", is("IN_PROGRESS")))
          .andExpect(jsonPath("$.data[1].remarks", is("Step 2: Investigate")))
          .andExpect(jsonPath("$.data[2].dataQualityIssueStatusCode", is("RESOLVED")))
          .andExpect(jsonPath("$.data[2].remarks", is("Step 3: Resolve")))
          .andExpect(jsonPath("$.data[3].dataQualityIssueStatusCode", is("REOPENED")))
          .andExpect(jsonPath("$.data[3].remarks", is("Step 4: Reopen")));
    }
  }

  @Nested
  @DisplayName("7. Database Check Constraint & Deduplication Engine Synergy")
  class CheckConstraintAndDeduplicationSynergy {

    @Test
    @DisplayName("Should prove MySQL database check constraint ck_dq_issues_resolved_at actively rejects resolved_at < detected_at")
    void shouldProveDatabaseCheckConstraintEnforced() {
      Long issueId = insertTestIssue("IN_PROGRESS", "WF-TEST-DB-CHECK");

      // Attempt raw SQL update violating ck_dq_issues_resolved_at: resolved_at < detected_at
      assertThatThrownBy(() -> {
        jdbcTemplate.update(
            "UPDATE data_quality_issues SET resolved_at = detected_at - INTERVAL 1 DAY WHERE data_quality_issue_id = ?",
            issueId
        );
      }).hasMessageContaining("ck_dq_issues_resolved_at");
    }

    @Test
    @DisplayName("Should prove Phase 28.3 scan deduplication treats ASSIGNED, IN_PROGRESS and REOPENED as active issues")
    void shouldProveDeduplicationSynergyAcrossActiveStatuses() {
      catalogService.ensureDefaultRulesProvisioned();
      DataQualityRule rule = catalogService.getEnabledRules().get(0);

      // 1. Create an issue with identical summary and transition to ASSIGNED
      Long issueId = insertTestIssue("OPEN", "Re-detected salary discrepancy");
      jdbcTemplate.update(
          "UPDATE data_quality_issues SET data_quality_rule_id = ?, entity_type = 'EmploymentRecord', entity_id = 7771, " +
              "data_quality_issue_status_id = (SELECT data_quality_issue_status_id FROM ref_data_quality_issue_status WHERE status_code = 'ASSIGNED') " +
              "WHERE data_quality_issue_id = ?",
          rule.getId(), issueId
      );

      // 2. Scan engine attempts registration for same rule, entity, and identical summary
      RegistrationResult result = registrationService.registerOrUpdateIssue(
          rule, "EmploymentRecord", 7771L, "Re-detected salary discrepancy"
      );

      // Verify deduplication recognized the active ASSIGNED issue and skipped creating duplicate
      assertThat(result.status()).isEqualTo(RegistrationStatus.DEDUPLICATED);
      assertThat(result.issueId()).isEqualTo(issueId);

      // 3. Transition to IN_PROGRESS and re-verify deduplication with identical summary
      jdbcTemplate.update(
          "UPDATE data_quality_issues SET data_quality_issue_status_id = " +
              "(SELECT data_quality_issue_status_id FROM ref_data_quality_issue_status WHERE status_code = 'IN_PROGRESS') " +
              "WHERE data_quality_issue_id = ?",
          issueId
      );

      RegistrationResult result2 = registrationService.registerOrUpdateIssue(
          rule, "EmploymentRecord", 7771L, "Re-detected salary discrepancy"
      );
      assertThat(result2.status()).isEqualTo(RegistrationStatus.DEDUPLICATED);
      assertThat(result2.issueId()).isEqualTo(issueId);

      // 4. Transition to REOPENED and re-verify deduplication with identical summary
      jdbcTemplate.update(
          "UPDATE data_quality_issues SET data_quality_issue_status_id = " +
              "(SELECT data_quality_issue_status_id FROM ref_data_quality_issue_status WHERE status_code = 'REOPENED') " +
              "WHERE data_quality_issue_id = ?",
          issueId
      );

      RegistrationResult result3 = registrationService.registerOrUpdateIssue(
          rule, "EmploymentRecord", 7771L, "Re-detected salary discrepancy"
      );
      assertThat(result3.status()).isEqualTo(RegistrationStatus.DEDUPLICATED);
      assertThat(result3.issueId()).isEqualTo(issueId);

      // Verify strictly 1 issue row exists in database for this entity and rule
      Integer totalIssues = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM data_quality_issues WHERE data_quality_rule_id = ? AND entity_type = 'EmploymentRecord' AND entity_id = 7771",
          Integer.class, rule.getId()
      );
      assertThat(totalIssues).isEqualTo(1);
    }
  }
}
