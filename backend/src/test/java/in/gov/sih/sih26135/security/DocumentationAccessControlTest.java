package in.gov.sih.sih26135.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Phase 29.9 Targeted Review Verification:
 * Validates that when Swagger UI and raw API docs are configured as disabled (the production default),
 * documentation routes are unavailable (HTTP 404 Not Found), while business endpoints remain strictly
 * secured under JWT authentication (HTTP 401 Unauthorized for unauthenticated requests).
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "springdoc.swagger-ui.enabled=false",
    "springdoc.api-docs.enabled=false"
})
@DisplayName("Phase 29.9 — OpenAPI & Swagger UI Disabled Access Control Tests")
public class DocumentationAccessControlTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  @DisplayName("Swagger UI is unavailable (404) when springdoc.swagger-ui.enabled=false")
  void testSwaggerUiDisabledReturnsNotFound() throws Exception {
    mockMvc.perform(get("/swagger-ui/index.html"))
        .andExpect(status().isNotFound());

    mockMvc.perform(get("/swagger-ui.html"))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Raw OpenAPI documentation is unavailable (404) when springdoc.api-docs.enabled=false")
  void testRawApiDocsDisabledReturnsNotFound() throws Exception {
    mockMvc.perform(get("/v3/api-docs"))
        .andExpect(status().isNotFound());

    mockMvc.perform(get("/v3/api-docs.yaml"))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Business endpoints remain strictly secured (401) when documentation is disabled")
  void testBusinessEndpointSecurityPreservedWhenDocsDisabled() throws Exception {
    // Trainees endpoint requires JWT bearer authentication
    mockMvc.perform(get("/api/v1/trainees"))
        .andExpect(status().isUnauthorized());

    // Analytics dashboard requires JWT bearer authentication
    mockMvc.perform(get("/api/v1/dashboards/geography"))
        .andExpect(status().isUnauthorized());
  }
}
