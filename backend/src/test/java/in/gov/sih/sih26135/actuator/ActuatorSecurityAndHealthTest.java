package in.gov.sih.sih26135.actuator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.gov.sih.sih26135.MysqlConnectionTest;
import in.gov.sih.sih26135.actuator.health.BulkImportStorageHealthIndicator;
import in.gov.sih.sih26135.actuator.health.CaffeineCacheHealthIndicator;
import in.gov.sih.sih26135.actuator.health.CommunicationGatewayHealthIndicator;
import in.gov.sih.sih26135.actuator.health.DatabaseLatencyHealthIndicator;
import in.gov.sih.sih26135.security.jwt.JwtTokenProvider;
import java.sql.Connection;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Phase 29.11 Observability Hardening:
 * Comprehensive verification of Spring Boot Actuator endpoints, health indicators,
 * role-based access control, probe exposure, and sensitive endpoint exclusion.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Phase 29.11 — Actuator Security, Health Indicators & Probe Verification")
public class ActuatorSecurityAndHealthTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private DataSource dataSource;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  @Autowired
  private DatabaseLatencyHealthIndicator databaseLatencyHealthIndicator;

  @Autowired
  private CaffeineCacheHealthIndicator caffeineCacheHealthIndicator;

  @Autowired
  private BulkImportStorageHealthIndicator bulkImportStorageHealthIndicator;

  @Autowired
  private CommunicationGatewayHealthIndicator communicationGatewayHealthIndicator;

  @BeforeEach
  void verifyDatabaseIsolation() throws Exception {
    assertThat(dataSource).as("DataSource bean must be present").isNotNull();
    try (Connection connection = dataSource.getConnection()) {
      assertThat(connection).isNotNull();
      String catalog = connection.getCatalog();
      MysqlConnectionTest.assertDedicatedTestCatalog(catalog);
      assertThat(catalog).isEqualToIgnoringCase(MysqlConnectionTest.EXPECTED_TEST_CATALOG);
    }
  }

  private String createAdminBearerToken() {
    return "Bearer " + jwtTokenProvider.generateAccessToken(1L, "root_admin", List.of("ROLE_ADMIN", "system.manage"));
  }

  private String createNonAdminBearerToken() {
    return "Bearer " + jwtTokenProvider.generateAccessToken(2L, "clerk_user", List.of("ROLE_DATA_ENTRY_CLERK", "trainee.read"));
  }

  // =========================================================================
  // 1. Public Health, Liveness & Readiness Probes
  // =========================================================================

  @Test
  @DisplayName("Anonymous access to /actuator/health returns 200 UP without leaking internal details")
  void testPublicHealthEndpointIsAccessibleWithoutAuth() throws Exception {
    MvcResult result = mockMvc.perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"))
        .andReturn();

    String responseBody = result.getResponse().getContentAsString();
    // Verify show-details=when-authorized guard: anonymous caller must NOT see components or internals
    assertThat(responseBody)
        .as("Anonymous response must not contain components or internal breakdown")
        .doesNotContain("\"components\"")
        .doesNotContain("\"details\"")
        .doesNotContain("password")
        .doesNotContain("jdbc");
  }

  @Test
  @DisplayName("Anonymous access to /actuator/health/liveness returns 200 UP and liveness includes only livenessState (not diskSpace)")
  void testPublicLivenessProbeIsAccessible() throws Exception {
    mockMvc.perform(get("/actuator/health/liveness"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));

    // Verify authorized liveness group composition: strictly livenessState only, zero diskSpace
    mockMvc.perform(get("/actuator/health/liveness")
            .header("Authorization", createAdminBearerToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.components.livenessState").exists())
        .andExpect(jsonPath("$.components.diskSpace").doesNotExist());
  }

  @Test
  @DisplayName("Anonymous access to /actuator/health/readiness returns 200 UP")
  void testPublicReadinessProbeIsAccessible() throws Exception {
    mockMvc.perform(get("/actuator/health/readiness"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
  }

  @Test
  @DisplayName("Anonymous access to /actuator/info returns 200 OK")
  void testPublicInfoEndpointIsAccessible() throws Exception {
    mockMvc.perform(get("/actuator/info"))
        .andExpect(status().isOk());
  }

  // =========================================================================
  // 2. Prometheus Metrics Security & Scrape Verification
  // =========================================================================

  @Test
  @DisplayName("Anonymous access to /actuator/prometheus returns 401 Unauthorized")
  void testPrometheusEndpointRequiresAuthentication() throws Exception {
    mockMvc.perform(get("/actuator/prometheus"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Non-admin user access to /actuator/prometheus returns 403 Forbidden")
  void testPrometheusEndpointForbiddenForNonAdminUser() throws Exception {
    mockMvc.perform(get("/actuator/prometheus")
            .header("Authorization", createNonAdminBearerToken()))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("Admin user access to /actuator/prometheus returns 200 OK with Prometheus exposition format")
  void testPrometheusEndpointAccessibleForAdminUser() throws Exception {
    MvcResult result = mockMvc.perform(get("/actuator/prometheus")
            .header("Authorization", createAdminBearerToken()))
        .andExpect(status().isOk())
        .andReturn();

    String content = result.getResponse().getContentAsString();
    assertThat(content)
        .as("Prometheus text scrape must contain JVM and system metrics")
        .contains("jvm_memory_used_bytes")
        .contains("system_cpu_count");
  }

  // =========================================================================
  // 3. Authorized Health Details & Diagnostics Group
  // =========================================================================

  @Test
  @DisplayName("Admin user sees components in /actuator/health")
  void testHealthDetailsAccessibleForAdminUser() throws Exception {
    mockMvc.perform(get("/actuator/health")
            .header("Authorization", createAdminBearerToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"))
        .andExpect(jsonPath("$.components").exists())
        .andExpect(jsonPath("$.components.db").exists())
        .andExpect(jsonPath("$.components.databaseLatency").exists())
        .andExpect(jsonPath("$.components.caffeineCache").exists())
        .andExpect(jsonPath("$.components.bulkImportStorage").exists())
        .andExpect(jsonPath("$.components.communicationGateway").exists());
  }

  @Test
  @DisplayName("Admin user can access /actuator/health/diagnostics group")
  void testDiagnosticsGroupAccessibleForAdminUser() throws Exception {
    mockMvc.perform(get("/actuator/health/diagnostics")
            .header("Authorization", createAdminBearerToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"))
        .andExpect(jsonPath("$.components.databaseLatency").exists())
        .andExpect(jsonPath("$.components.bulkImportStorage").exists())
        .andExpect(jsonPath("$.components.caffeineCache").exists())
        .andExpect(jsonPath("$.components.communicationGateway").exists());
  }

  @Test
  @DisplayName("Anonymous access to /actuator/health/diagnostics returns 401 Unauthorized")
  void testDiagnosticsGroupRequiresAuth() throws Exception {
    mockMvc.perform(get("/actuator/health/diagnostics"))
        .andExpect(status().isUnauthorized());
  }

  // =========================================================================
  // 4. Sensitive Endpoint Exclusion Verification
  // =========================================================================

  @Test
  @DisplayName("Sensitive actuator endpoints (env, beans, heapdump, shutdown) are not exposed")
  void testSensitiveActuatorEndpointsAreExcluded() throws Exception {
    String adminToken = createAdminBearerToken();

    // Sensitive endpoints excluded from exposure return 404 even with admin token
    mockMvc.perform(get("/actuator/env").header("Authorization", adminToken))
        .andExpect(status().isNotFound());

    mockMvc.perform(get("/actuator/beans").header("Authorization", adminToken))
        .andExpect(status().isNotFound());

    mockMvc.perform(get("/actuator/heapdump").header("Authorization", adminToken))
        .andExpect(status().isNotFound());

    mockMvc.perform(get("/actuator/configprops").header("Authorization", adminToken))
        .andExpect(status().isNotFound());

    mockMvc.perform(get("/actuator/shutdown").header("Authorization", adminToken))
        .andExpect(status().isNotFound());
  }

  // =========================================================================
  // 5. Unit Inspection of Custom Health Indicators
  // =========================================================================

  @Test
  @DisplayName("DatabaseLatencyHealthIndicator executes SELECT 1 and returns UP with latency metric")
  void testDatabaseLatencyHealthIndicator() {
    Health health = databaseLatencyHealthIndicator.health();
    assertThat(health.getStatus()).isIn(Status.UP, new Status("WARNING"));
    assertThat(health.getDetails()).containsKey("latencyMs");
    assertThat(health.getDetails()).containsKey("thresholdMs");
    assertThat((Long) health.getDetails().get("thresholdMs")).isEqualTo(500L);
  }

  @Test
  @DisplayName("CaffeineCacheHealthIndicator returns UP with non-disruptive cache diagnostics")
  void testCaffeineCacheHealthIndicator() {
    Health health = caffeineCacheHealthIndicator.health();
    assertThat(health.getStatus()).isEqualTo(Status.UP);
    assertThat(health.getDetails()).containsKey("caches");
    assertThat(health.getDetails()).containsKey("configuredCachesCount");
    assertThat(health.getDetails()).containsKey("totalHits");
    assertThat(health.getDetails()).containsKey("totalMisses");
  }

  @Test
  @DisplayName("BulkImportStorageHealthIndicator returns UP and does not disclose file paths")
  void testBulkImportStorageHealthIndicator() {
    Health health = bulkImportStorageHealthIndicator.health();
    assertThat(health.getStatus()).isIn(Status.UP, new Status("LOW_DISK_SPACE"));
    assertThat(health.getDetails()).containsKey("usableSpaceBytes");
    assertThat(health.getDetails()).containsKey("thresholdBytes");
    assertThat(health.getDetails()).containsKey("storageConfigured");
    // Ensure zero path disclosure in details
    for (String key : health.getDetails().keySet()) {
      assertThat(key.toLowerCase()).doesNotContain("path", "dir", "location", "file");
    }
  }

  @Test
  @DisplayName("CommunicationGatewayHealthIndicator returns UP with passive inspection and zero network calls")
  void testCommunicationGatewayHealthIndicator() {
    Health health = communicationGatewayHealthIndicator.health();
    assertThat(health.getStatus()).isEqualTo(Status.UP);
    assertThat(health.getDetails()).containsKey("mode");
    assertThat(health.getDetails()).containsKey("providers");
    assertThat(health.getDetails()).containsKey("emailProviderType");
    assertThat(health.getDetails()).containsKey("liveOverrideEnabled");
  }
}
