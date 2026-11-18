package in.gov.sih.sih26135.integration;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 29.5 — Administrator Oversight Journey Integration Test.
 *
 * <p>Validates:
 * <ol>
 *   <li>National Executive Dashboard Overview: {@code GET /api/v1/dashboards/national} -> 200 OK</li>
 *   <li>System Health & Operational Status: {@code GET /api/v1/operational/system-health} -> 200 OK</li>
 *   <li>Data Quality Scorecard: {@code GET /api/v1/operational/data-quality-scorecard} -> 200 OK</li>
 *   <li>Geographic Drill-Down: {@code GET /api/v1/dashboards/states/101} -> 200 OK</li>
 *   <li>Streaming DSDP CSV Export: {@code GET /api/v1/analytics/export/dsdp/csv?districtId=1001} -> 200 OK (text/csv)</li>
 *   <li>Streaming QPR CSV Export: {@code GET /api/v1/analytics/export/qpr/csv?fiscalYear=FY2026-27&quarter=Q1} -> 200 OK (text/csv)</li>
 *   <li>State Admin Tenancy Boundary: State Admin A views State 101 -> 200 OK; views State B -> 403 Forbidden</li>
 *   <li>State Admin B views State B -> 200 OK</li>
 *   <li>National Overview Boundary: State Admin A calls National Dashboard -> 403 Forbidden</li>
 *   <li>MIS Export Boundary: State Admin A calls QPR export -> 403 Forbidden</li>
 *   <li>Cross-State District Boundary: State Admin A calls DSDP for district in State B -> 403 Forbidden</li>
 *   <li>Immutable Audit Timeline: {@code GET /api/v1/audit-logs/entities/User/{id}/timeline} -> 200 OK</li>
 *   <li>Audit Log Search: {@code GET /api/v1/audit-logs?size=10} -> 200 OK</li>
 * </ol>
 */
@DisplayName("Phase 29.5 — Administrator Oversight Journey Integration Test")
public class AdministratorOversightJourneyIntegrationTest extends BaseJourneyIntegrationTest {

  private Long fixtureNatAdminUserId;
  private Long fixtureStateAAdminUserId;
  private Long fixtureStateBAdminUserId;
  private Long fixtureStateBId;
  private Long fixtureDistrictBId;

  private String natAdminToken;
  private HttpHeaders natAdminHeaders;
  private String stateAAdminToken;
  private HttpHeaders stateAAdminHeaders;
  private String stateBAdminToken;
  private HttpHeaders stateBAdminHeaders;

  @BeforeEach
  void setUpAdministratorOversightFixtures() {
    String runToken = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

    // 1. Fixture State B and District B for Cross-State Boundary Testing (dynamically allocated)
    String stateCodeB = null;
    for (char c1 = 'Z'; c1 >= 'A'; c1--) {
      for (char c2 = 'Z'; c2 >= 'A'; c2--) {
        String candidate = "" + c1 + c2;
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM states WHERE state_code = ?", Integer.class, candidate);
        if (count == null || count == 0) {
          stateCodeB = candidate;
          break;
        }
      }
      if (stateCodeB != null) break;
    }

    fixtureStateBId = insertAndGetId("""
        INSERT INTO states (state_code, state_name, lgd_code, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, ?, 1, NOW(), NOW())
        """, stateCodeB, "Karnataka Test State " + runToken, "LG" + runToken.substring(0, 4));
    trackedStateIds.add(fixtureStateBId);

    fixtureDistrictBId = insertAndGetId("""
        INSERT INTO districts (state_id, district_code, district_name, lgd_code, lifecycle_status_id, created_at, updated_at)
        VALUES (?, ?, ?, ?, 1, NOW(), NOW())
        """, fixtureStateBId, "DK" + runToken.substring(0, 5), "Bengaluru Urban Test " + runToken, "LGD" + runToken.substring(0, 4));
    trackedDistrictIds.add(fixtureDistrictBId);

    // 2. National Admin: Super Admin with Nationwide Scope (state_id = null, district_id = null)
    fixtureNatAdminUserId = insertAndGetId("""
        INSERT INTO users (username, email, password_hash, password_algo, user_status_id, state_id, district_id, created_at, updated_at)
        VALUES (?, ?, 'hash', 'argon2id', 2, NULL, NULL, NOW(), NOW())
        """, "national_superadmin_" + runToken, "national_admin_" + runToken + "@test.gov.in");
    trackedUserIds.add(fixtureNatAdminUserId);

    jdbcTemplate.update("""
        INSERT INTO user_roles (user_id, role_id, assigned_at)
        SELECT ?, role_id, NOW() FROM roles WHERE role_code = 'super_admin' LIMIT 1
        """, fixtureNatAdminUserId);

    natAdminToken = generateBearerToken(
        fixtureNatAdminUserId,
        "national_superadmin_" + runToken,
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
    fixtureStateAAdminUserId = insertAndGetId("""
        INSERT INTO users (username, email, password_hash, password_algo, user_status_id, state_id, district_id, created_at, updated_at)
        VALUES (?, ?, 'hash', 'argon2id', 2, ?, NULL, NOW(), NOW())
        """, "stateadmin_101_" + runToken, "stateadmin_101_" + runToken + "@test.gov.in", BASE_STATE_ID);
    trackedUserIds.add(fixtureStateAAdminUserId);

    jdbcTemplate.update("""
        INSERT INTO user_roles (user_id, role_id, assigned_at)
        SELECT ?, role_id, NOW() FROM roles WHERE role_code = 'government_admin' LIMIT 1
        """, fixtureStateAAdminUserId);

    stateAAdminToken = generateBearerToken(
        fixtureStateAAdminUserId,
        "stateadmin_101_" + runToken,
        List.of(
            "ROLE_state_admin",
            "analytics.read",
            "audit.read"
        )
    );
    stateAAdminHeaders = createAuthHeaders(stateAAdminToken);

    // 4. State Admin B: Restricted to State B (Karnataka)
    fixtureStateBAdminUserId = insertAndGetId("""
        INSERT INTO users (username, email, password_hash, password_algo, user_status_id, state_id, district_id, created_at, updated_at)
        VALUES (?, ?, 'hash', 'argon2id', 2, ?, NULL, NOW(), NOW())
        """, "stateadmin_b_" + runToken, "stateadmin_b_" + runToken + "@test.gov.in", fixtureStateBId);
    trackedUserIds.add(fixtureStateBAdminUserId);

    jdbcTemplate.update("""
        INSERT INTO user_roles (user_id, role_id, assigned_at)
        SELECT ?, role_id, NOW() FROM roles WHERE role_code = 'government_admin' LIMIT 1
        """, fixtureStateBAdminUserId);

    stateBAdminToken = generateBearerToken(
        fixtureStateBAdminUserId,
        "stateadmin_b_" + runToken,
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
    // Step 1: National Executive Dashboard Overview via GET /api/v1/dashboards/national
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
    JsonNode nationalData = nationalJson.get("data");
    assertThat(nationalData.has("kpiStrip")).isTrue();
    assertThat(nationalData.has("pipelineFunnel")).isTrue();

    // -------------------------------------------------------------------------
    // Step 2: System Health Status via GET /api/v1/operational/system-health
    // -------------------------------------------------------------------------
    ResponseEntity<String> healthResp = restTemplate.exchange(
        getBaseUrl("/api/v1/operational/system-health"),
        HttpMethod.GET,
        new HttpEntity<>(natAdminHeaders),
        String.class
    );
    assertThat(healthResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    JsonNode healthJson = parseResponseJson(healthResp.getBody());
    assertThat(healthJson.get("success").asBoolean()).isTrue();
    assertThat(healthJson.get("data").has("totalIncidents")).isTrue();

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
    JsonNode dqScorecardJson = parseResponseJson(dqScorecardResp.getBody());
    assertThat(dqScorecardJson.get("success").asBoolean()).isTrue();
    assertThat(dqScorecardJson.get("data").has("dataCleanlinessIndexPct")).isTrue();

    // -------------------------------------------------------------------------
    // Step 4: Geographic State Drill-Down via GET /api/v1/dashboards/states/{stateId}
    // -------------------------------------------------------------------------
    ResponseEntity<String> stateDrilldownResp = restTemplate.exchange(
        getBaseUrl("/api/v1/dashboards/states/" + BASE_STATE_ID),
        HttpMethod.GET,
        new HttpEntity<>(natAdminHeaders),
        String.class
    );
    assertThat(stateDrilldownResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    JsonNode stateDrilldownJson = parseResponseJson(stateDrilldownResp.getBody());
    assertThat(stateDrilldownJson.get("success").asBoolean()).isTrue();
    assertThat(stateDrilldownJson.get("data").get("stateId").asLong()).isEqualTo(BASE_STATE_ID);

    // -------------------------------------------------------------------------
    // Step 5: Streaming DSDP District CSV Export via GET /api/v1/analytics/export/dsdp/csv?districtId=1001
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
    // Step 6: Streaming QPR National CSV Export via GET /api/v1/analytics/export/qpr/csv?fiscalYear=FY2026-27&quarter=Q1
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
        getBaseUrl("/api/v1/audit-logs/entities/User/" + fixtureNatAdminUserId + "/timeline"),
        HttpMethod.GET,
        new HttpEntity<>(natAdminHeaders),
        String.class
    );
    assertThat(timelineResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    JsonNode timelineJson = parseResponseJson(timelineResp.getBody());
    assertThat(timelineJson.get("success").asBoolean()).isTrue();
    assertThat(timelineJson.get("data").get("entityType").asText()).isEqualTo("User");
    assertThat(timelineJson.get("data").get("entityId").asLong()).isEqualTo(fixtureNatAdminUserId);

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
    // 2. State Admin A cannot view State B Dashboard -> 403 Forbidden
    // -------------------------------------------------------------------------
    ResponseEntity<String> forbiddenStateResp = restTemplate.exchange(
        getBaseUrl("/api/v1/dashboards/states/" + fixtureStateBId),
        HttpMethod.GET,
        new HttpEntity<>(stateAAdminHeaders),
        String.class
    );
    assertThat(forbiddenStateResp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    // State Admin B can view State B Dashboard
    ResponseEntity<String> stateBResp = restTemplate.exchange(
        getBaseUrl("/api/v1/dashboards/states/" + fixtureStateBId),
        HttpMethod.GET,
        new HttpEntity<>(stateBAdminHeaders),
        String.class
    );
    assertThat(stateBResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    JsonNode stateBJson = parseResponseJson(stateBResp.getBody());
    assertThat(stateBJson.get("data").get("stateId").asLong()).isEqualTo(fixtureStateBId);

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
    // 6. State Admin A cannot trigger DSDP Export for District B (in State B) -> 403 Forbidden
    // -------------------------------------------------------------------------
    ResponseEntity<String> forbiddenDsdpResp = restTemplate.exchange(
        getBaseUrl("/api/v1/analytics/export/dsdp/csv?districtId=" + fixtureDistrictBId),
        HttpMethod.GET,
        new HttpEntity<>(stateAAdminHeaders),
        String.class
    );
    assertThat(forbiddenDsdpResp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }
}
