package in.gov.sih.sih26135.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import in.gov.sih.sih26135.security.config.CorsConfig;
import in.gov.sih.sih26135.security.config.SecurityConfig;
import in.gov.sih.sih26135.security.config.SecurityCorsProperties;
import in.gov.sih.sih26135.security.handler.RestAccessDeniedHandler;
import in.gov.sih.sih26135.security.handler.RestAuthenticationEntryPoint;
import in.gov.sih.sih26135.security.jwt.JwtAuthenticationFilter;
import in.gov.sih.sih26135.security.jwt.JwtTokenProvider;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * Focused integration test suite verifying Phase 24.9 Production Security Hardening:
 * <ul>
 *   <li>Explicit environment-backed CORS validation (allowed origin vs disallowed origin)</li>
 *   <li>CORS preflight OPTIONS handling and permitted headers/methods</li>
 *   <li>Mandatory HTTP security response headers: nosniff, DENY, Referrer-Policy, CSP</li>
 *   <li>Production HSTS under secure HTTPS vs plain HTTP</li>
 *   <li>Security headers retention on 401 Unauthorized and 403 Forbidden</li>
 *   <li>Public vs authenticated route authorization boundary</li>
 *   <li>Stateless session management enforcement</li>
 * </ul>
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
    ProductionSecurityHardeningTest.TestConfig.class,
    SecurityConfig.class,
    CorsConfig.class,
    RestAuthenticationEntryPoint.class,
    RestAccessDeniedHandler.class
})
@WebAppConfiguration
class ProductionSecurityHardeningTest {

  private static final String ALLOWED_ORIGIN = "https://portal.sih.gov.in";
  private static final String DISALLOWED_ORIGIN = "https://malicious.evil.com";

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private Filter springSecurityFilterChain;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    SecurityContextHolder.clearContext();
    mockMvc = MockMvcBuilders.webAppContextSetup(context)
        .addFilters(springSecurityFilterChain)
        .build();
  }

  // =========================================================================
  // 1. CORS Production Hardening Tests
  // =========================================================================

  @Test
  @DisplayName("1. CORS allowed configured origin succeeds in preflight OPTIONS")
  void testCors_allowedOrigin_preflightSucceeds() throws Exception {
    mockMvc.perform(options("/api/v1/auth/login")
            .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization,Content-Type"))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN))
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("POST")))
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600"));
  }

  @Test
  @DisplayName("2. CORS unconfigured/disallowed origin is rejected with 403 Forbidden")
  void testCors_disallowedOrigin_preflightRejected() throws Exception {
    mockMvc.perform(options("/api/v1/auth/login")
            .header(HttpHeaders.ORIGIN, DISALLOWED_ORIGIN)
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization,Content-Type"))
        .andExpect(status().isForbidden())
        .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
  }

  @Test
  @DisplayName("3. CORS actual request from allowed origin returns Access-Control-Allow-Origin")
  void testCors_allowedOrigin_actualRequestIncludesHeader() throws Exception {
    mockMvc.perform(post("/api/v1/auth/login")
            .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN));
  }

  @Test
  @DisplayName("4. CORS preflight permits Authorization and Content-Type request headers")
  void testCors_allowedHeaders_supportedInPreflight() throws Exception {
    mockMvc.perform(options("/api/v1/auth/me")
            .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization,Content-Type"))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsString("Authorization")));
  }

  // =========================================================================
  // 2. HTTP Security Response Headers Tests
  // =========================================================================

  @Test
  @DisplayName("5. X-Content-Type-Options: nosniff is present on responses")
  void testSecurityHeaders_contentTypeOptionsNosniff() throws Exception {
    mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(header().string("X-Content-Type-Options", "nosniff"));
  }

  @Test
  @DisplayName("6. X-Frame-Options: DENY is present on responses")
  void testSecurityHeaders_frameOptionsDeny() throws Exception {
    mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(header().string("X-Frame-Options", "DENY"));
  }

  @Test
  @DisplayName("7. Referrer-Policy is present and configured to strict-origin-when-cross-origin")
  void testSecurityHeaders_referrerPolicy() throws Exception {
    mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"));
  }

  @Test
  @DisplayName("8. Content-Security-Policy is present and restrictive (default-src 'none'; frame-ancestors 'none'; base-uri 'none')")
  void testSecurityHeaders_contentSecurityPolicy() throws Exception {
    mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(header().string("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'; base-uri 'none'"));
  }

  @Test
  @DisplayName("9. HSTS is present under HTTPS connections (max-age=31536000; includeSubDomains)")
  void testSecurityHeaders_hstsOnHttps() throws Exception {
    mockMvc.perform(post("/api/v1/auth/login")
            .secure(true)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(header().string("Strict-Transport-Security", containsString("max-age=31536000")))
        .andExpect(header().string("Strict-Transport-Security", containsString("includeSubDomains")));
  }

  @Test
  @DisplayName("9b. HSTS is omitted under plain HTTP connections per RFC 6797")
  void testSecurityHeaders_hstsOmittedOnPlainHttp() throws Exception {
    mockMvc.perform(post("/api/v1/auth/login")
            .secure(false)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(header().doesNotExist("Strict-Transport-Security"));
  }

  // =========================================================================
  // 3. Security Headers on Error Responses (401 & 403)
  // =========================================================================

  @Test
  @DisplayName("10. 401 Unauthorized responses retain all production security headers")
  void testErrorResponse_401_retainsSecurityHeaders() throws Exception {
    mockMvc.perform(get("/api/v1/trainees")
            .secure(true))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
        .andExpect(jsonPath("$.message").value("Authentication is required to access this resource"))
        .andExpect(header().string("X-Content-Type-Options", "nosniff"))
        .andExpect(header().string("X-Frame-Options", "DENY"))
        .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
        .andExpect(header().string("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'; base-uri 'none'"))
        .andExpect(header().string("Strict-Transport-Security", containsString("max-age=31536000")));
  }

  @Test
  @DisplayName("11. 403 Forbidden responses retain all production security headers")
  void testErrorResponse_403_retainsSecurityHeaders() throws Exception {
    // Authenticate as a user lacking the ROLE_ADMIN authority
    mockMvc.perform(get("/api/v1/admin/resource")
            .secure(true)
            .header("X-Test-Role", "ROLE_USER"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("FORBIDDEN"))
        .andExpect(jsonPath("$.message").value("Access denied"))
        .andExpect(header().string("X-Content-Type-Options", "nosniff"))
        .andExpect(header().string("X-Frame-Options", "DENY"))
        .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
        .andExpect(header().string("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'; base-uri 'none'"))
        .andExpect(header().string("Strict-Transport-Security", containsString("max-age=31536000")));
  }

  // =========================================================================
  // 4. Request Authorization Boundary Tests
  // =========================================================================

  @Test
  @DisplayName("12. Public login endpoint is accessible without authentication")
  void testPublicEndpoint_loginIsAccessible() throws Exception {
    mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("login success"));
  }

  @Test
  @DisplayName("13. Public refresh endpoint is accessible without authentication")
  void testPublicEndpoint_refreshIsAccessible() throws Exception {
    mockMvc.perform(post("/api/v1/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("refresh success"));
  }

  @Test
  @DisplayName("13b. Public health endpoint is accessible without authentication")
  void testPublicEndpoint_healthIsAccessible() throws Exception {
    mockMvc.perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
  }

  @Test
  @DisplayName("14. /api/v1/auth/me is protected and requires authentication")
  void testProtectedEndpoint_authMe_requiresAuthentication() throws Exception {
    mockMvc.perform(get("/api/v1/auth/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
  }

  @Test
  @DisplayName("15. Business endpoints require authentication")
  void testProtectedEndpoint_trainees_requiresAuthentication() throws Exception {
    mockMvc.perform(get("/api/v1/trainees"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
  }

  // =========================================================================
  // 5. Stateless Session Policy Test
  // =========================================================================

  @Test
  @DisplayName("16. Stateless session policy: no HttpSession is created during request execution")
  void testStatelessSessionPolicy_noSessionCreated() throws Exception {
    MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn();

    MockHttpServletRequest request = result.getRequest();
    assertThat(request.getSession(false)).isNull();
  }

  // =========================================================================
  // Test Configuration Infrastructure
  // =========================================================================

  @Configuration
  @EnableWebMvc
  static class TestConfig {

    @Bean
    public ObjectMapper objectMapper() {
      return new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Bean
    public SecurityCorsProperties securityCorsProperties() {
      SecurityCorsProperties properties = new SecurityCorsProperties();
      properties.setAllowedOrigins(List.of(ALLOWED_ORIGIN, "https://admin.sih.gov.in"));
      properties.validate();
      return properties;
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
      // Pass-through filter with test hook for simulating authenticated roles
      return new JwtAuthenticationFilter(mock(JwtTokenProvider.class)) {
        @Override
        protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

          String testRole = request.getHeader("X-Test-Role");
          if (testRole != null) {
            UserPrincipal principal = new UserPrincipal(1L, "testuser");
            UsernamePasswordAuthenticationToken auth =
                UsernamePasswordAuthenticationToken.authenticated(
                    principal, null, List.of(new SimpleGrantedAuthority(testRole)));
            SecurityContextHolder.getContext().setAuthentication(auth);
          }
          filterChain.doFilter(request, response);
        }
      };
    }

    @RestController
    static class TestApiController {

      @PostMapping("/api/v1/auth/login")
      public ResponseEntity<Map<String, String>> login() {
        return ResponseEntity.ok(Map.of("message", "login success"));
      }

      @PostMapping("/api/v1/auth/refresh")
      public ResponseEntity<Map<String, String>> refresh() {
        return ResponseEntity.ok(Map.of("message", "refresh success"));
      }

      @GetMapping("/api/v1/auth/me")
      public ResponseEntity<Map<String, String>> me() {
        return ResponseEntity.ok(Map.of("user", "me"));
      }

      @GetMapping("/actuator/health")
      public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
      }

      @GetMapping("/api/v1/trainees")
      public ResponseEntity<Map<String, String>> trainees() {
        return ResponseEntity.ok(Map.of("data", "trainees"));
      }

      @GetMapping("/api/v1/admin/resource")
      @PreAuthorize("hasRole('ADMIN')")
      public ResponseEntity<Map<String, String>> adminOnly() {
        return ResponseEntity.ok(Map.of("admin", "data"));
      }
    }
  }
}
