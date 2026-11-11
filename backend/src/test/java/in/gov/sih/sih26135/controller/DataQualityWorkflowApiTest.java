package in.gov.sih.sih26135.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.sih.sih26135.MysqlConnectionTest;
import in.gov.sih.sih26135.dto.request.AssignDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.request.DismissDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.request.ReopenDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.request.ResolveDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.request.StartInvestigationRequest;
import in.gov.sih.sih26135.dto.request.UpdateDataQualityIssueRequest;
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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
  private String auditReadToken;
  private String reviewerToken;
  private String unrelatedReviewerToken;
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

    // 1. Resolve or provision valid geographic reference records
    Long validDistrictId = null;
    Long validStateId = null;
    try {
      validDistrictId = jdbcTemplate.queryForObject("SELECT MIN(district_id) FROM districts", Long.class);
      if (validDistrictId != null) {
        validStateId = jdbcTemplate.queryForObject("SELECT state_id FROM districts WHERE district_id = ?", Long.class, validDistrictId);
      }
    } catch (Exception ignored) {
    }

    if (validDistrictId == null || validStateId == null) {
      validStateId = 9985L;
      validDistrictId = 9985L;
      jdbcTemplate.update("INSERT INTO states (state_id, state_code, state_name, lifecycle_status_id, created_at, updated_at) VALUES (?, 'W1', 'WF State 1', 1, NOW(), NOW())", validStateId);
      jdbcTemplate.update("INSERT INTO districts (district_id, state_id, district_code, district_name, lifecycle_status_id, created_at, updated_at) VALUES (?, ?, 'WD1', 'WF District 1', 1, NOW(), NOW())", validDistrictId, validStateId);
    }

    Long otherStateId = null;
    try {
      otherStateId = jdbcTemplate.queryForObject("SELECT MIN(state_id) FROM states WHERE state_id != ?", Long.class, validStateId);
    } catch (Exception ignored) {
    }

    if (otherStateId == null) {
      otherStateId = 9986L;
      jdbcTemplate.update("INSERT INTO states (state_id, state_code, state_name, lifecycle_status_id, created_at, updated_at) VALUES (?, 'W2', 'WF State 2', 1, NOW(), NOW())", otherStateId);
    }

    // 2. Insert test trainee 9985 in validStateId, validDistrictId
    jdbcTemplate.update(
        "INSERT INTO trainees (trainee_id, registration_number, first_name, last_name, phone, email, date_of_birth, gender_id, state_id, district_id, current_employment_status_id, profile_status_id, created_at, updated_at) " +
            "VALUES (9985, 'WF-TR-9985', 'TestTrainee', 'ScopeCheck', '9876543210', 'tr9985@test.gov.in', '2000-01-01', 1, ?, ?, 1, 1, NOW(), NOW())",
        validStateId, validDistrictId
    );

    // 3. Insert test users
    // User 9980: Admin
    jdbcTemplate.update(
        "INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, created_at, updated_at) " +
            "VALUES (9980, 'wf_admin_test', 'wf_admin@test.gov.in', 'hash', 'argon2id', 1, NOW(), NOW())"
    );
    // User 9981: Reviewer in validStateId
    jdbcTemplate.update(
        "INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, state_id, created_at, updated_at) " +
            "VALUES (9981, 'wf_reviewer_test', 'wf_reviewer@test.gov.in', 'hash', 'argon2id', 1, ?, NOW(), NOW())",
        validStateId
    );
    // User 9982: Unscoped user (null tenancy fields)
    jdbcTemplate.update(
        "INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, created_at, updated_at) " +
            "VALUES (9982, 'wf_unscoped_test', 'wf_unscoped@test.gov.in', 'hash', 'argon2id', 1, NOW(), NOW())"
    );
    // User 9983: Reviewer in otherStateId
    jdbcTemplate.update(
        "INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, state_id, created_at, updated_at) " +
            "VALUES (9983, 'wf_state2_test', 'wf_state2@test.gov.in', 'hash', 'argon2id', 1, ?, NOW(), NOW())",
        otherStateId
    );
    // User 9984: Auditor in validStateId (has audit.read only)
    jdbcTemplate.update(
        "INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, state_id, created_at, updated_at) " +
            "VALUES (9984, 'wf_auditor_test', 'wf_auditor@test.gov.in', 'hash', 'argon2id', 1, ?, NOW(), NOW())",
        validStateId
    );

    // Grant active workflow management role ('system.manage') to test reviewers (9981, 9983) and super_admin to 9980
    jdbcTemplate.update(
        "INSERT INTO roles (role_id, role_code, role_name, is_system_role, lifecycle_status_id, created_at, updated_at) " +
            "VALUES (9981, 'WF_TEST_MGR', 'WF Test Manager', 0, 1, NOW(), NOW())"
    );
    jdbcTemplate.update(
        "INSERT INTO role_permissions (role_id, permission_id) " +
            "SELECT 9981, permission_id FROM permissions WHERE permission_code = 'system.manage'"
    );
    jdbcTemplate.update(
        "INSERT INTO user_roles (user_id, role_id, assigned_at) " +
            "SELECT 9980, role_id, NOW() FROM roles WHERE role_code = 'super_admin' LIMIT 1"
    );
    jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id, assigned_at) VALUES (9981, 9981, NOW())");
    jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id, assigned_at) VALUES (9983, 9981, NOW())");

    testUserId = 9980L;
    testReviewerUserId = 9981L;

    adminToken = "Bearer " + jwtTokenProvider.generateAccessToken(
        testUserId,
        "wf_admin_test",
        List.of("ROLE_super_admin", "audit.read", "system.manage")
    );

    auditReadToken = "Bearer " + jwtTokenProvider.generateAccessToken(
        9984L,
        "wf_auditor_test",
        List.of("audit.read")
    );

    reviewerToken = "Bearer " + jwtTokenProvider.generateAccessToken(
        testReviewerUserId,
        "wf_reviewer_test",
        List.of("ROLE_state_admin", "system.manage")
    );

    unrelatedReviewerToken = "Bearer " + jwtTokenProvider.generateAccessToken(
        9983L,
        "wf_state2_test",
        List.of("ROLE_state_admin", "system.manage")
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
      jdbcTemplate.update("DELETE FROM data_quality_issue_events WHERE changed_by_user_id IN (9980, 9981, 9982, 9983, 9984)");
      jdbcTemplate.update("DELETE FROM data_quality_issues WHERE issue_summary LIKE 'WF-TEST-%' OR issue_summary = 'Re-detected salary discrepancy'");
      jdbcTemplate.update("DELETE FROM trainees WHERE trainee_id = 9985");
      jdbcTemplate.update("DELETE FROM user_roles WHERE user_id IN (9980, 9981, 9982, 9983, 9984)");
      jdbcTemplate.update("DELETE FROM role_permissions WHERE role_id = 9981");
      jdbcTemplate.update("DELETE FROM roles WHERE role_id = 9981");
      jdbcTemplate.update("DELETE FROM users WHERE user_id IN (9980, 9981, 9982, 9983, 9984)");
      jdbcTemplate.update("DELETE FROM districts WHERE district_id = 9985");
      jdbcTemplate.update("DELETE FROM states WHERE state_id IN (9985, 9986)");
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
              "VALUES (?, ?, ?, ?, ?, 'Trainee', 9985, ?, NOW() - INTERVAL 1 HOUR, NOW(), NOW())",
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
    @DisplayName("Should successfully assign an OPEN issue using query parameter")
    void shouldAssignWithQueryParam() throws Exception {
      Long issueId = insertTestIssue("OPEN", "WF-TEST-ASSIGN-PARAM");

      mockMvc.perform(post("/api/v1/data-quality-issues/{id}/assign", issueId)
              .header("Authorization", adminToken)
              .param("assignedUserId", testReviewerUserId.toString()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success", is(true)))
          .andExpect(jsonPath("$.data.assignedUserId", is(testReviewerUserId.intValue())));
    }

    @Test
    @DisplayName("Should reject nationwide admin assigning to unrelated assignee outside issue state scope")
    void shouldRejectNationwideAdminAssigningToUnrelatedAssignee() throws Exception {
      Long issueId = insertTestIssue("OPEN", "WF-TEST-ASSIGN-UNRELATED");
      // Assignee 9983 is in State 2, but Trainee 9985 is in State 1
      AssignDataQualityIssueRequest request = new AssignDataQualityIssueRequest(9983L);

      mockMvc.perform(post("/api/v1/data-quality-issues/{id}/assign", issueId)
              .header("Authorization", adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code", is("ASSIGNEE_SCOPE_MISMATCH")));
    }

    @Test
    @DisplayName("Should reject assigning to unscoped user (null tenancy fields)")
    void shouldRejectAssigningToUnscopedUser() throws Exception {
      Long issueId = insertTestIssue("OPEN", "WF-TEST-ASSIGN-UNSCOPED");
      // Assignee 9982 has null state, provider, and employer
      AssignDataQualityIssueRequest request = new AssignDataQualityIssueRequest(9982L);

      mockMvc.perform(post("/api/v1/data-quality-issues/{id}/assign", issueId)
              .header("Authorization", adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code", is("ASSIGNEE_SCOPE_MISMATCH")));
    }

    @Test
    @DisplayName("Should reject assignment when unauthenticated")
    void shouldRejectUnauthenticatedAssignment() throws Exception {
      Long issueId = insertTestIssue("OPEN", "WF-TEST-UNAUTH");
      AssignDataQualityIssueRequest request = new AssignDataQualityIssueRequest(testReviewerUserId);

      mockMvc.perform(post("/api/v1/data-quality-issues/{id}/assign", issueId)
              .header("Authorization", unauthenticatedToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("2. Investigation API (POST /{id}/start-investigation)")
  class InvestigationApiTests {

    @Test
    @DisplayName("Should transition ASSIGNED issue to IN_PROGRESS and record event")
    void shouldStartInvestigationSuccessfully() throws Exception {
      Long issueId = insertTestIssue("ASSIGNED", "WF-TEST-START-INVESTIGATION");
      // Assign to testReviewerUserId
      jdbcTemplate.update("UPDATE data_quality_issues SET assigned_user_id = ? WHERE data_quality_issue_id = ?",
          testReviewerUserId, issueId);

      StartInvestigationRequest request = new StartInvestigationRequest("Beginning root cause analysis");

      mockMvc.perform(post("/api/v1/data-quality-issues/{id}/start-investigation", issueId)
              .header("Authorization", reviewerToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success", is(true)))
          .andExpect(jsonPath("$.data.dataQualityIssueStatusCode", is("IN_PROGRESS")));

      // Verify event was logged
      List<Map<String, Object>> events = jdbcTemplate.queryForList(
          "SELECT * FROM data_quality_issue_events WHERE data_quality_issue_id = ?", issueId
      );
      assertThat(events).hasSize(1);
      assertThat(events.get(0).get("remarks")).isEqualTo("Beginning root cause analysis");
    }

    @Test
    @DisplayName("Should reject unauthorized investigator starting investigation with 403 Forbidden")
    void shouldRejectUnauthorizedInvestigatorStartingInvestigation() throws Exception {
      Long issueId = insertTestIssue("ASSIGNED", "WF-TEST-START-UNAUTHORIZED");
      jdbcTemplate.update("UPDATE data_quality_issues SET assigned_user_id = ? WHERE data_quality_issue_id = ?",
          testReviewerUserId, issueId);

      StartInvestigationRequest request = new StartInvestigationRequest("Unauthorized attempt");

      // Caller 9983 is not assigned to this issue and not administrative supervisor
      mockMvc.perform(post("/api/v1/data-quality-issues/{id}/start-investigation", issueId)
              .header("Authorization", unrelatedReviewerToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should reject starting investigation directly on OPEN issue with 409 Conflict")
    void shouldRejectDirectInvestigationOnOpen() throws Exception {
      Long issueId = insertTestIssue("OPEN", "WF-TEST-START-CONFLICT");
      StartInvestigationRequest request = new StartInvestigationRequest("Premature start");

      mockMvc.perform(post("/api/v1/data-quality-issues/{id}/start-investigation", issueId)
              .header("Authorization", adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code", is("INVALID_DQ_TRANSITION")));
    }
  }

  @Nested
  @DisplayName("3. Resolution API (POST /{id}/resolve)")
  class ResolveApiTests {

    @Test
    @DisplayName("Should resolve an IN_PROGRESS issue with server attribution, notes and event")
    void shouldResolveInProgressIssueSuccessfully() throws Exception {
      Long issueId = insertTestIssue("IN_PROGRESS", "WF-TEST-RESOLVE-SUCCESS");
      jdbcTemplate.update("UPDATE data_quality_issues SET assigned_user_id = ? WHERE data_quality_issue_id = ?",
          testReviewerUserId, issueId);

      ResolveDataQualityIssueRequest request = new ResolveDataQualityIssueRequest("Discrepancy remediated in enrollment records");

      mockMvc.perform(post("/api/v1/data-quality-issues/{id}/resolve", issueId)
              .header("Authorization", reviewerToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success", is(true)))
          .andExpect(jsonPath("$.data.dataQualityIssueStatusCode", is("RESOLVED")))
          .andExpect(jsonPath("$.data.resolvedByUserId", is(testReviewerUserId.intValue())))
          .andExpect(jsonPath("$.data.resolvedAt", is(org.hamcrest.Matchers.notNullValue())))
          .andExpect(jsonPath("$.data.resolutionNotes", is("Discrepancy remediated in enrollment records")));

      // Check event
      List<Map<String, Object>> events = jdbcTemplate.queryForList(
          "SELECT * FROM data_quality_issue_events WHERE data_quality_issue_id = ?", issueId
      );
      assertThat(events).hasSize(1);
      assertThat(events.get(0).get("remarks")).isEqualTo("Discrepancy remediated in enrollment records");
    }

    @Test
    @DisplayName("Should reject resolving an OPEN issue directly with 409 Conflict")
    void shouldRejectResolvingOpenIssueDirectly() throws Exception {
      Long issueId = insertTestIssue("OPEN", "WF-TEST-RESOLVE-OPEN-CONFLICT");
      ResolveDataQualityIssueRequest request = new ResolveDataQualityIssueRequest("Skipping to resolve");

      mockMvc.perform(post("/api/v1/data-quality-issues/{id}/resolve", issueId)
              .header("Authorization", adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code", is("INVALID_DQ_TRANSITION")));
    }

    @Test
    @DisplayName("Should reject resolving an ASSIGNED issue directly with 409 Conflict")
    void shouldRejectResolvingAssignedIssueDirectly() throws Exception {
      Long issueId = insertTestIssue("ASSIGNED", "WF-TEST-RESOLVE-ASSIGNED-CONFLICT");
      ResolveDataQualityIssueRequest request = new ResolveDataQualityIssueRequest("Skipping investigation");

      mockMvc.perform(post("/api/v1/data-quality-issues/{id}/resolve", issueId)
              .header("Authorization", adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code", is("INVALID_DQ_TRANSITION")));
    }
  }

  @Nested
  @DisplayName("4. Dismissal API (POST /{id}/dismiss)")
  class DismissApiTests {

    @Test
    @DisplayName("Should dismiss an OPEN issue with remarks and log DISMISSAL event")
    void shouldDismissOpenIssueSuccessfully() throws Exception {
      Long issueId = insertTestIssue("OPEN", "WF-TEST-DISMISS-OPEN");
      DismissDataQualityIssueRequest request = new DismissDataQualityIssueRequest("Confirmed false positive");

      mockMvc.perform(post("/api/v1/data-quality-issues/{id}/dismiss", issueId)
              .header("Authorization", adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success", is(true)))
          .andExpect(jsonPath("$.data.dataQualityIssueStatusCode", is("DISMISSED")))
          .andExpect(jsonPath("$.data.resolutionNotes", is("Confirmed false positive")));

      // Check event
      List<Map<String, Object>> events = jdbcTemplate.queryForList(
          "SELECT * FROM data_quality_issue_events WHERE data_quality_issue_id = ?", issueId
      );
      assertThat(events).hasSize(1);
      assertThat(events.get(0).get("remarks")).isEqualTo("Confirmed false positive");
    }

    @Test
    @DisplayName("Should explicitly reject ASSIGNED -> DISMISSED with 409 Conflict")
    void shouldRejectDismissingAssignedIssueWith409() throws Exception {
      Long issueId = insertTestIssue("ASSIGNED", "WF-TEST-DISMISS-ASSIGNED-409");
      DismissDataQualityIssueRequest request = new DismissDataQualityIssueRequest("Direct dismissal of assigned");

      mockMvc.perform(post("/api/v1/data-quality-issues/{id}/dismiss", issueId)
              .header("Authorization", adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code", is("INVALID_DQ_TRANSITION")));
    }

    @Test
    @DisplayName("Should explicitly reject REOPENED -> DISMISSED with 409 Conflict")
    void shouldRejectDismissingReopenedIssueWith409() throws Exception {
      Long issueId = insertTestIssue("REOPENED", "WF-TEST-DISMISS-REOPENED-409");
      DismissDataQualityIssueRequest request = new DismissDataQualityIssueRequest("Direct dismissal of reopened");

      mockMvc.perform(post("/api/v1/data-quality-issues/{id}/dismiss", issueId)
              .header("Authorization", adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code", is("INVALID_DQ_TRANSITION")));
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
  @DisplayName("7. Read and Write Permission Separation Tests (audit.read vs system.manage)")
  class ReadAndWritePermissionSeparationTests {

    @Test
    @DisplayName("Caller with audit.read alone cannot authorize lifecycle mutations (403 Forbidden)")
    void shouldRejectCallerWithAuditReadAloneFromAllLifecycleMutations() throws Exception {
      Long issueId = insertTestIssue("OPEN", "WF-TEST-PERM-MUTATION");

      // 1. assign
      mockMvc.perform(post("/api/v1/data-quality-issues/{id}/assign", issueId)
              .header("Authorization", auditReadToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(new AssignDataQualityIssueRequest(testReviewerUserId))))
          .andExpect(status().isForbidden());

      // 2. start-investigation
      mockMvc.perform(post("/api/v1/data-quality-issues/{id}/start-investigation", issueId)
              .header("Authorization", auditReadToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(new StartInvestigationRequest())))
          .andExpect(status().isForbidden());

      // 3. resolve
      mockMvc.perform(post("/api/v1/data-quality-issues/{id}/resolve", issueId)
              .header("Authorization", auditReadToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(new ResolveDataQualityIssueRequest("Resolution notes"))))
          .andExpect(status().isForbidden());

      // 4. dismiss
      mockMvc.perform(post("/api/v1/data-quality-issues/{id}/dismiss", issueId)
              .header("Authorization", auditReadToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(new DismissDataQualityIssueRequest("Dismiss notes"))))
          .andExpect(status().isForbidden());

      // 5. reopen
      mockMvc.perform(post("/api/v1/data-quality-issues/{id}/reopen", issueId)
              .header("Authorization", auditReadToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(new ReopenDataQualityIssueRequest("Reopen reason"))))
          .andExpect(status().isForbidden());

      // 6. generic PUT
      UpdateDataQualityIssueRequest updateReq = new UpdateDataQualityIssueRequest();
      updateReq.setIssueSummary("Attempted update with audit.read");
      mockMvc.perform(put("/api/v1/data-quality-issues/{id}", issueId)
              .header("Authorization", auditReadToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(updateReq)))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Caller with audit.read alone CAN read issue events (200 OK)")
    void shouldAllowCallerWithAuditReadToViewEvents() throws Exception {
      Long issueId = insertTestIssue("OPEN", "WF-TEST-PERM-READ-EVENTS");

      mockMvc.perform(get("/api/v1/data-quality-issues/{id}/events", issueId)
              .header("Authorization", auditReadToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success", is(true)));
    }
  }

  @Nested
  @DisplayName("8. Generic PUT Lifecycle History Protection Tests")
  class GenericUpdateProtectionTests {

    @Test
    @DisplayName("Should reject direct status alteration via generic PUT")
    void shouldRejectDirectStatusAlterationViaGenericPut() throws Exception {
      Long issueId = insertTestIssue("OPEN", "WF-TEST-PUT-STATUS");
      UpdateDataQualityIssueRequest request = new UpdateDataQualityIssueRequest();
      request.setDataQualityIssueStatusId(2L); // Trying to directly change status

      mockMvc.perform(put("/api/v1/data-quality-issues/{id}", issueId)
              .header("Authorization", adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code", is("LIFECYCLE_BYPASS_PROHIBITED")));
    }

    @Test
    @DisplayName("Should reject direct assignment alteration via generic PUT")
    void shouldRejectDirectAssignmentAlterationViaGenericPut() throws Exception {
      Long issueId = insertTestIssue("OPEN", "WF-TEST-PUT-ASSIGN");
      UpdateDataQualityIssueRequest request = new UpdateDataQualityIssueRequest();
      request.setAssignedUserId(testReviewerUserId); // Trying to directly change assignee

      mockMvc.perform(put("/api/v1/data-quality-issues/{id}", issueId)
              .header("Authorization", adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code", is("ASSIGNMENT_BYPASS_PROHIBITED")));
    }

    @Test
    @DisplayName("Should reject direct resolution alteration via generic PUT")
    void shouldRejectDirectResolutionAlterationViaGenericPut() throws Exception {
      Long issueId = insertTestIssue("OPEN", "WF-TEST-PUT-RESOLVE");
      UpdateDataQualityIssueRequest request = new UpdateDataQualityIssueRequest();
      request.setResolutionNotes("Bypassing resolve workflow");

      mockMvc.perform(put("/api/v1/data-quality-issues/{id}", issueId)
              .header("Authorization", adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code", is("RESOLUTION_BYPASS_PROHIBITED")));
    }

    @Test
    @DisplayName("Should permit unrelated updates via generic PUT without modifying lifecycle metadata")
    void shouldPermitUnrelatedUpdatesViaGenericPut() throws Exception {
      Long issueId = insertTestIssue("OPEN", "WF-TEST-PUT-PERMITTED");
      UpdateDataQualityIssueRequest request = new UpdateDataQualityIssueRequest();
      request.setIssueSummary("Updated issue summary through permitted generic update");

      mockMvc.perform(put("/api/v1/data-quality-issues/{id}", issueId)
              .header("Authorization", adminToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success", is(true)))
          .andExpect(jsonPath("$.data.issueSummary", is("Updated issue summary through permitted generic update")))
          .andExpect(jsonPath("$.data.dataQualityIssueStatusCode", is("OPEN")))
          .andExpect(jsonPath("$.data.assignedUserId", nullValue()));
    }
  }

  @Nested
  @DisplayName("9. Database Check Constraint & Deduplication Engine Synergy")
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
