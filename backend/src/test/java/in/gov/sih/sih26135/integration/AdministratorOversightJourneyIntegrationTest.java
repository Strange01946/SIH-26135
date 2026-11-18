package in.gov.sih.sih26135.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Phase 29.5 — End-to-End Administrator Oversight Journey Integration Test.
 *
 * <p>Exercises administrator oversight workflows across real HTTP endpoints:
 * <ol>
 *   <li>Setup: National Admin (nationwide, state/district=null), State Admin A (state 101), State Admin B (state 9988)</li>
 *   <li>National Overview: {@code GET /api/v1/dashboards/national} -> 200 OK for National Admin</li>
 *   <li>Operational System Health: {@code GET /api/v1/operational/system-health} -> 200 OK</li>
 *   <li>Data Quality Index / Scorecard: {@code GET /api/v1/operational/data-quality-scorecard} -> 200 OK</li>
 *   <li>Geographic Drill-Down: {@code GET /api/v1/dashboards/states/101} -> 200 OK for National Admin &amp; State Admin A</li>
 *   <li>Territorial Boundary Enforcement: State Admin A calls {@code /api/v1/dashboards/states/9988} -> 403 Forbidden</li>
 *   <li>Nationwide Dashboard Boundary: State Admin A calls {@code /api/v1/dashboards/national} -> 403 Forbidden</li>
 *   <li>Streaming MIS Export (DSDP): {@code GET /api/v1/analytics/export/dsdp/csv?districtId=1001} -> 200 OK (text/csv)</li>
 *   <li>Streaming MIS Export (QPR): {@code GET /api/v1/analytics/export/qpr/csv?fiscalYear=FY2026-27&quarter=Q1} -> 200 OK for National Admin</li>
 *   <li>MIS Export Boundary: State Admin A calls QPR export -> 403 Forbidden</li>
 *   <li>Cross-State District Boundary: State Admin A calls DSDP for district in State B (9988) -> 403 Forbidden</li>
 *   <li>Immutable Audit Timeline: {@code GET /api/v1/audit-logs/entities/User/9400/timeline} -> 200 OK</li>
 *   <li>Audit Log Search: {@code GET /api/v1/audit-logs?size=10} -> 200 OK</li>
 * </ol>
 */
@DisplayName("Phase 29.5 — Administrator Oversight Journey Integration Test")
public class AdministratorOversightJourneyIntegrationTest extends BaseJourneyIntegrationTest {

  private static final Long FIXTURE_NAT_ADMIN_USER_ID = 9400L;
  private static final Long FIXTURE_STATE_A_ADMIN_USER_ID = 9401L;
  private static final Long FIXTURE_STATE_B_ADMIN_USER_ID = 9402L;
  private static final Long FIXTURE_STATE_B_ID = 9988L;
  private static final Long FIXTURE_DISTRICT_B_ID = 9988L;

  private String natAdminToken;
  private HttpHeaders natAdminHeaders;
  private String stateAAdminToken;
  private HttpHeaders stateAAdminHeaders;
  private String stateBAdminToken;
  private HttpHeaders stateBAdminHeaders;

  @BeforeEach
  void setUpAdministratorOversightFixtures() {
    // 1. Fixture State B and District B for Cross-State Boundary Testing
    jdbcTemplate.update("""
        INSERT INTO states (state_id, state_code, state_name, lgd_code, lifecycle_status_id, created_at, updated_at)
        VALUES (?, 'KA', 'Karnataka Test State', 'LGD-KA-9988', 1, NOW(), NOW())
        ON DUPLICATE KEY UPDATE updated_at = NOW()
        """, FIXTURE_STATE_B_ID);
    trackedStateIds.add(FIXTURE_STATE_B_ID);

    jdbcTemplate.update("""
        INSERT INTO districts (district_id, state_id, district_code, district_name, lgd_code, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, 'DIS-KA-9988', 'Bengaluru Urban Test', 'LGD-DIS-9988', 1, NOW(), NOW())
        ON DUPLICATE KEY UPDATE updated_at = NOW()
        """, FIXTURE_DISTRICT_B_ID, FIXTURE_STATE_B_ID);
    trackedDistrictIds.add(FIXTURE_DISTRICT_B_ID);

    // 2. National Admin: Super Admin with Nationwide Scope (state_id = null, district_id = null)
    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, state_id, district_id, created_at, updated_at)
        VALUES (?, 'national_superadmin', 'national_admin@test.gov.in', 'hash', 'argon2id', 2, NULL, NULL, NOW(), NOW())
        ON DUPLICATE KEY UPDATE user_status_id = 2, updated_at = NOW()
        """, FIXTURE_NAT_ADMIN_USER_ID);
    trackedUserIds.add(FIXTURE_NAT_ADMIN_USER_ID);

    jdbcTemplate.update("""
        INSERT INTO user_roles (user_id, role_id, assigned_at)
        SELECT ?, role_id, NOW() FROM roles WHERE role_code = 'super_admin' LIMIT 1
        ON DUPLICATE KEY UPDATE assigned_at = NOW()
        """, FIXTURE_NAT_ADMIN_USER_ID);

    natAdminToken = generateBearerToken(
        FIXTURE_NAT_ADMIN_USER_ID,
        "national_superadmin",
        List.of(
            "ROLE_super_admin",
            "analytics.read",
            "system.manage",
            "audit.read",
            "system.event.read",
            "system.event.manage"
        )
    );
    natAdminHeaders = createAuthHeaders(natAdminToken);

    // 3. State Admin A: Restricted to State 101 (Delhi)
    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, state_id, district_id, created_at, updated_at)
        VALUES (?, 'stateadmin_101', 'stateadmin_101@test.gov.in', 'hash', 'argon2id', 2, ?, NULL, NOW(), NOW())
        ON DUPLICATE KEY UPDATE user_status_id = 2, state_id = VALUES(state_id), updated_at = NOW()
        """, FIXTURE_STATE_A_ADMIN_USER_ID, BASE_STATE_ID);
    trackedUserIds.add(FIXTURE_STATE_A_ADMIN_USER_ID);

    jdbcTemplate.update("""
        INSERT INTO user_roles (user_id, role_id, assigned_at)
        SELECT ?, role_id, NOW() FROM roles WHERE role_code = 'government_admin' LIMIT 1
        ON DUPLICATE KEY UPDATE assigned_at = NOW()
        """, FIXTURE_STATE_A_ADMIN_USER_ID);

    stateAAdminToken = generateBearerToken(
        FIXTURE_STATE_A_ADMIN_USER_ID,
        "stateadmin_101",
        List.of(
            "ROLE_state_admin",
            "analytics.read",
            "audit.read"
        )
    );
    stateAAdminHeaders = createAuthHeaders(stateAAdminToken);

    // 4. State Admin B: Restricted to State 9988 (Karnataka)
    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, state_id, district_id, created_at, updated_at)
        VALUES (?, 'stateadmin_9988', 'stateadmin_9988@test.gov.in', 'hash', 'argon2id', 2, ?, NULL, NOW(), NOW())
        ON DUPLICATE KEY UPDATE user_status_id = 2, state_id = VALUES(state_id), updated_at = NOW()
        """, FIXTURE_STATE_B_ADMIN_USER_ID, FIXTURE_STATE_B_ID);
    trackedUserIds.add(FIXTURE_STATE_B_ADMIN_USER_ID);

    jdbcTemplate.update("""
        INSERT INTO user_roles (user_id, role_id, assigned_at)
        SELECT ?, role_id, NOW() FROM roles WHERE role_code = 'government_admin' LIMIT 1
        ON DUPLICATE KEY UPDATE assigned_at = NOW()
        """, FIXTURE_STATE_B_ADMIN_USER_ID);

    stateBAdminToken = generateBearerToken(
        FIXTURE_STATE_B_ADMIN_USER_ID,
        "stateadmin_9988",
        List.of(
            "ROLE_state_admin",
            "analytics.read",
            "audit.read"
        )
    );
    stateBAdminHeaders = createAuthHeaders(stateBAdminToken);
  }

  @Test
  @DisplayName("Journey 4: Complete National Administrator Oversight & MIS Reporting End-to-End")
  void testNationalAdministratorOversightEndToEnd() throws Exception {
    // -------------------------------------------------------------------------
    // Step 1: National Overview Dashboard via GET /api/v1/dashboards/national
    // -------------------------------------------------------------------------
    ResponseEntity<String> nationalResp = restTemplate.exchange(
        getBaseUrl("/api/v1/dashboards/national"),
        HttpMethod.GET,
        new HttpEntity<>(natAdminHeaders),
        String.class
    );
    assertThat(nationalResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    JsonNode nationalJson = parseResponseJson(nationalResp.getBody());
    assertThat(nationalJson.get("success").asBoolean()).isTrue();
    assertThat(nationalJson.has("data")).isTrue();

    // -------------------------------------------------------------------------
    // Step 2: Operational System Health via GET /api/v1/operational/system-health
    // -------------------------------------------------------------------------
    ResponseEntity<String> sysHealthResp = restTemplate.exchange(
        getBaseUrl("/api/v1/operational/system-health"),
        HttpMethod.GET,
        new HttpEntity<>(natAdminHeaders),
        String.class
    );
    assertThat(sysHealthResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    JsonNode sysHealthJson = parseResponseJson(sysHealthResp.getBody());
    assertThat(sysHealthJson.get("success").asBoolean()).isTrue();
    assertThat(sysHealthJson.has("data")).isTrue();

    // -------------------------------------------------------------------------
    // Step 3: Operational Data Quality Scorecard via GET /api/v1/operational/data-quality-scorecard
    // -------------------------------------------------------------------------
    ResponseEntity<String> dqScorecardResp = restTemplate.exchange(
        getBaseUrl("/api/v1/operational/data-quality-scorecard"),
        HttpMethod.GET,
        new HttpEntity<>(natAdminHeaders),
        String.class
    );
    assertThat(dqScorecardResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    JsonNode dqJson = parseResponseJson(dqScorecardResp.getBody());
    assertThat(dqJson.get("success").asBoolean()).isTrue();
    assertThat(dqJson.has("data")).isTrue();

    // -------------------------------------------------------------------------
    // Step 4: Geographic State Drill-Down via GET /api/v1/dashboards/states/101
    // -------------------------------------------------------------------------
    ResponseEntity<String> state101Resp = restTemplate.exchange(
        getBaseUrl("/api/v1/dashboards/states/" + BASE_STATE_ID),
        HttpMethod.GET,
        new HttpEntity<>(natAdminHeaders),
        String.class
    );
    assertThat(state101Resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    JsonNode state101Json = parseResponseJson(state101Resp.getBody());
    assertThat(state101Json.get("success").asBoolean()).isTrue();
    assertThat(state101Json.get("data").get("stateId").asLong()).isEqualTo(BASE_STATE_ID);

    // -------------------------------------------------------------------------
    // Step 5: Streaming MIS Export - DSDP CSV via GET /api/v1/analytics/export/dsdp/csv
    // -------------------------------------------------------------------------
    ResponseEntity<String> dsdpResp = restTemplate.exchange(
        getBaseUrl("/api/v1/analytics/export/dsdp/csv?districtId=" + BASE_DISTRICT_ID),
        HttpMethod.GET,
        new HttpEntity<>(natAdminHeaders),
        String.class
    );
    assertThat(dsdpResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(dsdpResp.getHeaders().getContentType().toString()).contains("text/csv");
    assertThat(dsdpResp.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
        .contains("attachment")
        .contains("dsdp-district-1001.csv");
    assertThat(dsdpResp.getBody()).isNotNull();

    // -------------------------------------------------------------------------
    // Step 6: Streaming MIS Export - QPR CSV via GET /api/v1/analytics/export/qpr/csv
    // -------------------------------------------------------------------------
    ResponseEntity<String> qprResp = restTemplate.exchange(
        getBaseUrl("/api/v1/analytics/export/qpr/csv?fiscalYear=FY2026-27&quarter=Q1"),
        HttpMethod.GET,
        new HttpEntity<>(natAdminHeaders),
        String.class
    );
    assertThat(qprResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(qprResp.getHeaders().getContentType().toString()).contains("text/csv");
    assertThat(qprResp.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
        .contains("attachment")
        .contains("qpr-FY2026-27-Q1.csv");
    assertThat(qprResp.getBody()).isNotNull();

    // -------------------------------------------------------------------------
    // Step 7: Immutable Audit Trail Timeline via GET /api/v1/audit-logs/entities/User/{id}/timeline
    // -------------------------------------------------------------------------
    ResponseEntity<String> timelineResp = restTemplate.exchange(
        getBaseUrl("/api/v1/audit-logs/entities/User/" + FIXTURE_NAT_ADMIN_USER_ID + "/timeline"),
        HttpMethod.GET,
        new HttpEntity<>(natAdminHeaders),
        String.class
    );
    assertThat(timelineResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    JsonNode timelineJson = parseResponseJson(timelineResp.getBody());
    assertThat(timelineJson.get("success").asBoolean()).isTrue();
    assertThat(timelineJson.get("data").get("entityType").asText()).isEqualTo("User");
    assertThat(timelineJson.get("data").get("entityId").asLong()).isEqualTo(FIXTURE_NAT_ADMIN_USER_ID);

    // -------------------------------------------------------------------------
    // Step 8: Audit Log Search via GET /api/v1/audit-logs
    // -------------------------------------------------------------------------
    ResponseEntity<String> auditLogsResp = restTemplate.exchange(
        getBaseUrl("/api/v1/audit-logs?size=10"),
        HttpMethod.GET,
        new HttpEntity<>(natAdminHeaders),
        String.class
    );
    assertThat(auditLogsResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    JsonNode auditLogsJson = parseResponseJson(auditLogsResp.getBody());
    assertThat(auditLogsJson.get("success").asBoolean()).isTrue();
    assertThat(auditLogsJson.get("data").has("content")).isTrue();
  }

  @Test
  @DisplayName("Negative Security: State Administrator Territorial Boundaries and Tenancy Enforcement")
  void testStateAdministratorTerritorialBoundariesAndEnforcement() throws Exception {
    // -------------------------------------------------------------------------
    // 1. State Admin A can view State 101 Dashboard
    // -------------------------------------------------------------------------
    ResponseEntity<String> allowedStateResp = restTemplate.exchange(
        getBaseUrl("/api/v1/dashboards/states/" + BASE_STATE_ID),
        HttpMethod.GET,
        new HttpEntity<>(stateAAdminHeaders),
        String.class
    );
    assertThat(allowedStateResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    JsonNode allowedStateJson = parseResponseJson(allowedStateResp.getBody());
    assertThat(allowedStateJson.get("data").get("stateId").asLong()).isEqualTo(BASE_STATE_ID);

    // -------------------------------------------------------------------------
    // 2. State Admin A cannot view State 9988 (Karnataka) Dashboard -> 403 Forbidden
    // -------------------------------------------------------------------------
    ResponseEntity<String> forbiddenStateResp = restTemplate.exchange(
        getBaseUrl("/api/v1/dashboards/states/" + FIXTURE_STATE_B_ID),
        HttpMethod.GET,
        new HttpEntity<>(stateAAdminHeaders),
        String.class
    );
    assertThat(forbiddenStateResp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    // State Admin B can view State 9988 Dashboard
    ResponseEntity<String> stateBResp = restTemplate.exchange(
        getBaseUrl("/api/v1/dashboards/states/" + FIXTURE_STATE_B_ID),
        HttpMethod.GET,
        new HttpEntity<>(stateBAdminHeaders),
        String.class
    );
    assertThat(stateBResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    JsonNode stateBJson = parseResponseJson(stateBResp.getBody());
    assertThat(stateBJson.get("data").get("stateId").asLong()).isEqualTo(FIXTURE_STATE_B_ID);

    // -------------------------------------------------------------------------
    // 3. State Admin A cannot view National Overview Dashboard -> 403 Forbidden
    // -------------------------------------------------------------------------
    ResponseEntity<String> forbiddenNatResp = restTemplate.exchange(
        getBaseUrl("/api/v1/dashboards/national"),
        HttpMethod.GET,
        new HttpEntity<>(stateAAdminHeaders),
        String.class
    );
    assertThat(forbiddenNatResp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    // -------------------------------------------------------------------------
    // 4. State Admin A cannot trigger Nationwide QPR CSV Export -> 403 Forbidden
    // -------------------------------------------------------------------------
    ResponseEntity<String> forbiddenQprResp = restTemplate.exchange(
        getBaseUrl("/api/v1/analytics/export/qpr/csv?fiscalYear=FY2026-27&quarter=Q1"),
        HttpMethod.GET,
        new HttpEntity<>(stateAAdminHeaders),
        String.class
    );
    assertThat(forbiddenQprResp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    // -------------------------------------------------------------------------
    // 5. State Admin A can trigger DSDP Export for constituent District 1001 (in State 101)
    // -------------------------------------------------------------------------
    ResponseEntity<String> allowedDsdpResp = restTemplate.exchange(
        getBaseUrl("/api/v1/analytics/export/dsdp/csv?districtId=" + BASE_DISTRICT_ID),
        HttpMethod.GET,
        new HttpEntity<>(stateAAdminHeaders),
        String.class
    );
    assertThat(allowedDsdpResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(allowedDsdpResp.getHeaders().getContentType().toString()).contains("text/csv");

    // -------------------------------------------------------------------------
    // 6. State Admin A cannot trigger DSDP Export for District 9988 (in State B) -> 403 Forbidden
    // -------------------------------------------------------------------------
    ResponseEntity<String> forbiddenDsdpResp = restTemplate.exchange(
        getBaseUrl("/api/v1/analytics/export/dsdp/csv?districtId=" + FIXTURE_DISTRICT_B_ID),
        HttpMethod.GET,
        new HttpEntity<>(stateAAdminHeaders),
        String.class
    );
    assertThat(forbiddenDsdpResp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }
}
