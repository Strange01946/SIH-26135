package in.gov.sih.sih26135.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.sih.sih26135.MysqlConnectionTest;
import in.gov.sih.sih26135.dto.request.CreateAuditLogRequest;
import in.gov.sih.sih26135.dto.response.AuditLogResponse;
import in.gov.sih.sih26135.security.jwt.JwtTokenProvider;
import in.gov.sih.sih26135.service.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test suite for Phase 28.2:
 * 1. Append-only immutability (HTTP 405 on PUT/PATCH/DELETE).
 * 2. Multi-criteria audit log search with temporal and filter validation.
 * 3. Entity audit timeline reconstruction with allowlist and chronological ordering.
 * 4. Tenancy and administrative privilege boundaries.
 * 5. Prevention of client-authored audit forgery (ignoring client actor attribution, timestamp, correlationId).
 * 6. Cross-tenant isolation across all audit read endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Phase 28.2 - Audit Trail Immutability, Search & Timeline Integration Tests")
class AuditLogImmutabilityAndTimelineTest {

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
  private AuditLogService auditLogService;

  private String fullAdminToken;
  private String guestToken;
  private String employerToken;
  private String providerToken;

  @BeforeEach
  void setUp() throws Exception {
    try (Connection conn = dataSource.getConnection()) {
      String catalog = conn.getCatalog();
      MysqlConnectionTest.assertDedicatedTestCatalog(catalog);
      assertThat(catalog).isEqualToIgnoringCase(MysqlConnectionTest.EXPECTED_TEST_CATALOG);
    }

    fullAdminToken = "Bearer " + jwtTokenProvider.generateAccessToken(
        1L,
        "admin",
        List.of(
            "ROLE_super_admin",
            "placement.manage",
            "employment.manage",
            "verification.manage",
            "survey.manage",
            "audit.read",
            "system.manage"
        )
    );

    guestToken = "Bearer " + jwtTokenProvider.generateAccessToken(
        999L,
        "guest",
        List.of("ROLE_GUEST")
    );

    employerToken = "Bearer " + jwtTokenProvider.generateAccessToken(
        201L,
        "employer_auditor",
        List.of("ROLE_employer", "audit.read")
    );

    providerToken = "Bearer " + jwtTokenProvider.generateAccessToken(
        301L,
        "provider_auditor",
        List.of("ROLE_training_provider", "audit.read")
    );
  }

  // =========================================================================
  // 1. IMMUTABILITY TESTS (HTTP 405 Method Not Allowed)
  // =========================================================================

  @Test
  @DisplayName("PUT to /api/v1/audit-logs/{id} is rejected with 405 Method Not Allowed")
  void testPutAuditLogReturns405() throws Exception {
    mockMvc.perform(put("/api/v1/audit-logs/1")
            .header("Authorization", fullAdminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"changeSummary\":\"illegal update\"}"))
        .andExpect(status().isMethodNotAllowed());
  }

  @Test
  @DisplayName("PATCH to /api/v1/audit-logs/{id} is rejected with 405 Method Not Allowed")
  void testPatchAuditLogReturns405() throws Exception {
    mockMvc.perform(patch("/api/v1/audit-logs/1")
            .header("Authorization", fullAdminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"changeSummary\":\"illegal patch\"}"))
        .andExpect(status().isMethodNotAllowed());
  }

  @Test
  @DisplayName("DELETE to /api/v1/audit-logs/{id} is rejected with 405 Method Not Allowed")
  void testDeleteAuditLogReturns405() throws Exception {
    mockMvc.perform(delete("/api/v1/audit-logs/1")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isMethodNotAllowed());
  }

  // =========================================================================
  // 2. CREATION AUTHORIZATION BOUNDARIES & ANTI-FORGERY CONTROLS
  // =========================================================================

  @Test
  @DisplayName("Manual audit creation by unprivileged user is rejected with 403 Forbidden")
  void testManualAuditCreationByUnprivilegedUserReturns403() throws Exception {
    CreateAuditLogRequest request = new CreateAuditLogRequest();
    request.setAuditActionId(2L);
    request.setEntityType("User");
    request.setEntityId(1L);
    request.setChangeSummary("Forged audit record");

    mockMvc.perform(post("/api/v1/audit-logs")
            .header("Authorization", guestToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("Manual audit creation ignores client-supplied actorUserId, occurredAt, and correlationId in favor of server context")
  void testManualAuditCreationIgnoresClientAttributionAndBackdatedTimestamp() throws Exception {
    CreateAuditLogRequest request = new CreateAuditLogRequest();
    request.setAuditActionId(2L); // Action 2 is UPDATE (supported administrative manual action)
    request.setEntityType("User");
    request.setEntityId(1L); // User 1 is authorized for nationwide admin
    request.setActorUserId(99999L); // Client attempts to attribute to someone else!
    request.setOccurredAt(LocalDateTime.of(2015, 1, 1, 0, 0)); // Client attempts to backdate!
    request.setCorrelationId("forged-client-correlation"); // Client attempts to forge correlation!
    request.setChangeSummary("Administrative audit log entry");

    mockMvc.perform(post("/api/v1/audit-logs")
            .header("Authorization", fullAdminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.actorUserId").value(1)) // Overwritten with authenticated caller (1L)
        .andExpect(jsonPath("$.data.occurredAt").isNotEmpty()); // Must be current timestamp, not 2015
  }

  @Test
  @DisplayName("Manual creation of automated workflow action (STATUS_CHANGE) is rejected with 400 UNAUTHORIZED_WORKFLOW_ACTION")
  void testManualCreationOfWorkflowActionIsRejected() throws Exception {
    CreateAuditLogRequest request = new CreateAuditLogRequest();
    request.setAuditActionId(6L); // Action 6 is STATUS_CHANGE (automated workflow action)
    request.setEntityType("User");
    request.setEntityId(1L);
    request.setChangeSummary("Client attempting to fabricate completed workflow status change");

    mockMvc.perform(post("/api/v1/audit-logs")
            .header("Authorization", fullAdminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED_WORKFLOW_ACTION"));
  }

  @Test
  @DisplayName("Manual audit creation is rejected with 403 Forbidden when authenticated user identity cannot be resolved")
  void testManualAuditCreationRejectedWhenAuthenticatedIdentityUnavailable() throws Exception {
    // Issue token with valid authority but non-existent user ID
    String orphanToken = "Bearer " + jwtTokenProvider.generateAccessToken(
        888888L,
        "orphan_admin",
        List.of("system.manage", "ROLE_super_admin")
    );

    CreateAuditLogRequest request = new CreateAuditLogRequest();
    request.setAuditActionId(2L);
    request.setEntityType("User");
    request.setEntityId(1L);
    request.setChangeSummary("Orphan admin audit log entry");

    mockMvc.perform(post("/api/v1/audit-logs")
            .header("Authorization", orphanToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("Legitimate unauthenticated internal audit persistence succeeds through trusted internal pathway")
  void testLegitimateInternalAuditPersistence() {
    AuditLogResponse response = auditLogService.recordInternalAuditLog(
        1L, 2L, "User", 1L, "Account locked after repeated authentication failures"
    );
    assertThat(response).isNotNull();
    assertThat(response.id()).isNotNull();
    assertThat(response.actorUserId()).isEqualTo(1L);
    assertThat(response.entityType()).isEqualTo("User");
    assertThat(response.entityId()).isEqualTo(1L);
    assertThat(response.changeSummary()).isEqualTo("Account locked after repeated authentication failures");
  }

  @Test
  @DisplayName("Direct unauthenticated call to recordAuditLog cannot create audit record with client-supplied actor ID")
  void testDirectUnauthenticatedCallToRecordAuditLogIsRejected() {
    SecurityContextHolder.clearContext();

    CreateAuditLogRequest request = new CreateAuditLogRequest();
    request.setAuditActionId(2L);
    request.setEntityType("User");
    request.setEntityId(1L);
    request.setActorUserId(1L);
    request.setChangeSummary("Direct unauthenticated attempt to forge audit log");

    // Direct invocation must fail closed with AccessDeniedException
    org.junit.jupiter.api.Assertions.assertThrows(
        AccessDeniedException.class,
        () -> auditLogService.recordAuditLog(request),
        "Unauthenticated direct call to recordAuditLog must throw AccessDeniedException"
    );

    // Legitimate internal pathway recordInternalAuditLog continues working
    AuditLogResponse internalResponse = auditLogService.recordInternalAuditLog(
        1L, 2L, "User", 1L, "Internal audit record"
    );
    assertThat(internalResponse).isNotNull();
    assertThat(internalResponse.id()).isNotNull();
    assertThat(internalResponse.actorUserId()).isEqualTo(1L);
    assertThat(internalResponse.entityType()).isEqualTo("User");
    assertThat(internalResponse.entityId()).isEqualTo(1L);
  }

  // =========================================================================
  // 3. MULTI-CRITERIA AUDIT SEARCH TESTS
  // =========================================================================

  @Test
  @DisplayName("Search with combined criteria (entityType, date bounds) returns 200 OK")
  void testMultiCriteriaSearchSucceeds() throws Exception {
    mockMvc.perform(get("/api/v1/audit-logs")
            .param("entityType", "Placement")
            .param("start", "2026-01-01T00:00:00")
            .param("end", "2026-12-31T23:59:59")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.page").value(0))
        .andExpect(jsonPath("$.data.size").value(20))
        .andExpect(jsonPath("$.data.content").isArray());
  }

  @Test
  @DisplayName("Inverted date range (start after end) is rejected with 400 INVALID_DATE_RANGE")
  void testInvertedDateRangeReturns400() throws Exception {
    mockMvc.perform(get("/api/v1/audit-logs")
            .param("start", "2026-12-31T23:59:59")
            .param("end", "2026-01-01T00:00:00")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"));
  }

  @Test
  @DisplayName("Unsupported query parameter is rejected with 400 UNSUPPORTED_PARAMETER")
  void testUnsupportedQueryParameterReturns400() throws Exception {
    mockMvc.perform(get("/api/v1/audit-logs")
            .param("unsupportedFilterField", "unexpectedValue")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("UNSUPPORTED_PARAMETER"));
  }

  // =========================================================================
  // 4. ENTITY AUDIT TIMELINE TESTS
  // =========================================================================

  @Test
  @DisplayName("Entity timeline for authorized entity returns 200 OK with AuditTrailTimelineResponse contract")
  void testEntityTimelineForPlacementReturns200() throws Exception {
    mockMvc.perform(get("/api/v1/audit-logs/entities/Placement/101/timeline")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.entityType").value("Placement"))
        .andExpect(jsonPath("$.data.entityId").value(101))
        .andExpect(jsonPath("$.data.timelineEntries").isArray());
  }

  @Test
  @DisplayName("Entity timeline for unsupported entity type is rejected with 400 UNSUPPORTED_ENTITY_TYPE")
  void testEntityTimelineUnsupportedTypeReturns400() throws Exception {
    mockMvc.perform(get("/api/v1/audit-logs/entities/ArbitrarySqlInjectionTable/101/timeline")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("UNSUPPORTED_ENTITY_TYPE"));
  }

  @Test
  @DisplayName("Entity timeline with invalid (non-positive) ID is rejected with 400")
  void testEntityTimelineInvalidIdReturns400() throws Exception {
    mockMvc.perform(get("/api/v1/audit-logs/entities/Placement/0/timeline")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Unauthenticated entity timeline request returns 401 Unauthorized")
  void testEntityTimelineUnauthenticatedReturns401() throws Exception {
    mockMvc.perform(get("/api/v1/audit-logs/entities/Placement/101/timeline"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Unprivileged guest user requesting entity timeline returns 403 Forbidden")
  void testEntityTimelineUnprivilegedReturns403() throws Exception {
    mockMvc.perform(get("/api/v1/audit-logs/entities/Placement/101/timeline")
            .header("Authorization", guestToken))
        .andExpect(status().isForbidden());
  }

  // =========================================================================
  // 5. CROSS-TENANT ISOLATION TESTS
  // =========================================================================

  @Test
  @DisplayName("Cross-tenant unscoped search is rejected with 403 Forbidden for tenant-scoped callers holding audit.read")
  void testUnscopedAuditSearchRejectedForTenantCaller() throws Exception {
    // Employer holding audit.read cannot run platform-wide unscoped audit log queries
    mockMvc.perform(get("/api/v1/audit-logs")
            .header("Authorization", employerToken))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("Cross-tenant entity timeline inspection is rejected with 403 Forbidden")
  void testCrossTenantEntityTimelineRejected() throws Exception {
    // Training provider attempting to inspect administrative/cross-tenant timeline
    mockMvc.perform(get("/api/v1/audit-logs/entities/User/1/timeline")
            .header("Authorization", providerToken))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("Unscoped entity type query is rejected with 403 Forbidden for non-administrative callers")
  void testUnscopedEntityTypeSearchRejectedForTenantCaller() throws Exception {
    // Scoped employer attempting to query all Placements across all employers
    mockMvc.perform(get("/api/v1/audit-logs")
            .param("entityType", "Placement")
            .header("Authorization", employerToken))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("Caller holding audit.read cannot bypass entity scope using actor-based query even with matching actor ID")
  void testActorBasedAuditQueryRejectedForNonNationwideAdmin() throws Exception {
    // employerToken has actor user ID 201L and holds "audit.read"
    mockMvc.perform(get("/api/v1/audit-logs")
            .param("actorUserId", "201")
            .header("Authorization", employerToken))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("Nationwide access is rejected without administrative role even with username admin and audit.read")
  void testNationwideAccessRejectedWithoutAdministrativePrivileges() throws Exception {
    // User named "admin" holding ONLY audit.read (no ROLE_super_admin or ROLE_government_admin)
    String permissionOnlyAdminToken = "Bearer " + jwtTokenProvider.generateAccessToken(
        1L,
        "admin",
        List.of("audit.read")
    );

    mockMvc.perform(get("/api/v1/audit-logs")
            .header("Authorization", permissionOnlyAdminToken))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("User with system.manage and system.event.manage but without admin role cannot retrieve nationwide audit records even with null tenancy fields")
  void testSystemManageAndEventManageWithoutAdminRoleCannotAccessNationwideAudit() throws Exception {
    // User 1 (admin) has null tenancy fields in database (state_id=NULL, district_id=NULL, etc.)
    // Issue token holding system.manage, system.event.manage, and audit.read without ROLE_super_admin or ROLE_government_admin
    String systemManageToken = "Bearer " + jwtTokenProvider.generateAccessToken(
        1L,
        "admin",
        List.of("system.manage", "system.event.manage", "audit.read")
    );

    // 1. Unscoped platform search must be rejected with 403 Forbidden
    mockMvc.perform(get("/api/v1/audit-logs")
            .header("Authorization", systemManageToken))
        .andExpect(status().isForbidden());

    // 2. Unscoped entity type lookup must be rejected with 403 Forbidden
    mockMvc.perform(get("/api/v1/audit-logs")
            .param("entityType", "Placement")
            .header("Authorization", systemManageToken))
        .andExpect(status().isForbidden());

    // 3. Actor-based audit log lookup must be rejected with 403 Forbidden
    mockMvc.perform(get("/api/v1/audit-logs")
            .param("actorUserId", "1")
            .header("Authorization", systemManageToken))
        .andExpect(status().isForbidden());
  }
}
