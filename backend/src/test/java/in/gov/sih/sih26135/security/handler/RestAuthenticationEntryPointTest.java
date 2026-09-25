package in.gov.sih.sih26135.security.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;

class RestAuthenticationEntryPointTest {

  private RestAuthenticationEntryPoint entryPoint;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    entryPoint = new RestAuthenticationEntryPoint(objectMapper);
  }

  @Test
  void commence_returns401WithStandardizedApiResponse() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/trainees");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AuthenticationException exception = new BadCredentialsException("Bad credentials");

    entryPoint.commence(request, response, exception);

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getContentType()).startsWith("application/json");

    String responseBody = response.getContentAsString();
    JsonNode root = objectMapper.readTree(responseBody);

    assertThat(root.get("success").asBoolean()).isFalse();
    assertThat(root.get("code").asText()).isEqualTo("UNAUTHORIZED");
    assertThat(root.get("message").asText()).isEqualTo("Authentication is required to access this resource");
    assertThat(root.get("data").isNull()).isTrue();
    assertThat(root.get("path").asText()).isEqualTo("/api/v1/trainees");
    assertThat(root.has("timestamp")).isTrue();
  }
}
