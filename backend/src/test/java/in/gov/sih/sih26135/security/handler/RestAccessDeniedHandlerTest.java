package in.gov.sih.sih26135.security.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

class RestAccessDeniedHandlerTest {

  private RestAccessDeniedHandler accessDeniedHandler;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    accessDeniedHandler = new RestAccessDeniedHandler(objectMapper);
  }

  @Test
  void handle_returns403WithStandardizedApiResponse() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin/users");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AccessDeniedException exception = new AccessDeniedException("Forbidden");

    accessDeniedHandler.handle(request, response, exception);

    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(response.getContentType()).startsWith("application/json");

    String responseBody = response.getContentAsString();
    JsonNode root = objectMapper.readTree(responseBody);

    assertThat(root.get("success").asBoolean()).isFalse();
    assertThat(root.get("code").asText()).isEqualTo("FORBIDDEN");
    assertThat(root.get("message").asText()).isEqualTo("Access denied");
    assertThat(root.get("data").isNull()).isTrue();
    assertThat(root.get("path").asText()).isEqualTo("/api/v1/admin/users");
    assertThat(root.has("timestamp")).isTrue();
  }
}
