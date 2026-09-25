package in.gov.sih.sih26135.security.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SecurityCorsPropertiesTest {

  private SecurityCorsProperties properties;

  @BeforeEach
  void setUp() {
    properties = new SecurityCorsProperties();
  }

  @Test
  @DisplayName("Default properties enforce secure default-deny and appropriate API defaults")
  void testDefaults_areSecureAndAppropriateForApi() {
    assertThat(properties.getAllowedOrigins()).isEmpty();
    assertThat(properties.getAllowedMethods())
        .containsExactlyInAnyOrder("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    assertThat(properties.getAllowedHeaders())
        .contains("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With");
    assertThat(properties.getExposedHeaders()).contains("Authorization");
    assertThat(properties.isAllowCredentials()).isFalse();
    assertThat(properties.getMaxAge()).isEqualTo(Duration.ofSeconds(3600));

    // Default configuration must pass startup validation
    assertThatCode(() -> properties.validate()).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("Valid explicit HTTPS origins are accepted and pass validation")
  void testValidExplicitHttpsOrigins_passValidation() {
    properties.setAllowedOrigins(List.of("https://portal.sih.gov.in", "https://admin.sih.gov.in"));

    assertThat(properties.getAllowedOrigins()).containsExactly(
        "https://portal.sih.gov.in",
        "https://admin.sih.gov.in"
    );
    assertThatCode(() -> properties.validate()).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("Allowed origins are trimmed, normalized without trailing slashes, and blank entries removed")
  void testAllowedOrigins_normalization() {
    properties.setAllowedOrigins(List.of(
        "  https://portal.sih.gov.in/  ",
        "https://admin.sih.gov.in",
        "",
        "   "
    ));

    List<String> normalized = properties.getAllowedOrigins();
    assertThat(normalized).containsExactly(
        "https://portal.sih.gov.in",
        "https://admin.sih.gov.in"
    );
    assertThatCode(() -> properties.validate()).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("Wildcard origin '*' is strictly prohibited and fails validation")
  void testWildcardOrigin_failsValidation() {
    properties.setAllowedOrigins(List.of("*"));

    assertThatThrownBy(() -> properties.validate())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("CORS wildcard origin or pattern '*' is prohibited")
        .hasMessageContaining("Configure explicit origin URIs");
  }

  @Test
  @DisplayName("Wildcard pattern 'https://*.example.com' is strictly prohibited and fails validation")
  void testWildcardSubdomainPattern_failsValidation() {
    properties.setAllowedOrigins(List.of("https://*.example.com"));

    assertThatThrownBy(() -> properties.validate())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("CORS wildcard origin or pattern 'https://*.example.com' is prohibited")
        .hasMessageContaining("Configure explicit origin URIs");
  }

  @Test
  @DisplayName("Wildcard pattern 'http://*.example.com' is strictly prohibited and fails validation")
  void testHttpWildcardSubdomainPattern_failsValidation() {
    properties.setAllowedOrigins(List.of("http://*.example.com"));

    assertThatThrownBy(() -> properties.validate())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("CORS wildcard origin or pattern 'http://*.example.com' is prohibited")
        .hasMessageContaining("Configure explicit origin URIs");
  }

  @Test
  @DisplayName("Wildcard pattern 'https://example.*' is strictly prohibited and fails validation")
  void testWildcardTldPattern_failsValidation() {
    properties.setAllowedOrigins(List.of("https://example.*"));

    assertThatThrownBy(() -> properties.validate())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("CORS wildcard origin or pattern 'https://example.*' is prohibited")
        .hasMessageContaining("Configure explicit origin URIs");
  }

  @Test
  @DisplayName("Wildcard pattern 'http://example.*' is strictly prohibited and fails validation")
  void testHttpWildcardTldPattern_failsValidation() {
    properties.setAllowedOrigins(List.of("http://example.*"));

    assertThatThrownBy(() -> properties.validate())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("CORS wildcard origin or pattern 'http://example.*' is prohibited")
        .hasMessageContaining("Configure explicit origin URIs");
  }

  @Test
  @DisplayName("Wildcard origin '*' with allowCredentials=true fails validation")
  void testWildcardWithCredentials_failsValidation() {
    properties.setAllowCredentials(true);
    properties.setAllowedOrigins(List.of("*"));

    assertThatThrownBy(() -> properties.validate())
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  @DisplayName("Wildcard pattern with allowCredentials=true fails validation")
  void testWildcardPatternWithCredentials_failsValidation() {
    properties.setAllowCredentials(true);
    properties.setAllowedOrigins(List.of("https://*.example.com"));

    assertThatThrownBy(() -> properties.validate())
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  @DisplayName("Origins without http:// or https:// scheme fail validation")
  void testInvalidOriginScheme_failsValidation() {
    properties.setAllowedOrigins(List.of("ftp://ftp.example.com"));

    assertThatThrownBy(() -> properties.validate())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("must start with http:// or https://");

    properties.setAllowedOrigins(List.of("portal.sih.gov.in"));

    assertThatThrownBy(() -> properties.validate())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("must start with http:// or https://");
  }

  @Test
  @DisplayName("Allowed headers missing 'Authorization' fail validation")
  void testMissingAuthorizationHeader_failsValidation() {
    properties.setAllowedHeaders(List.of("Content-Type", "Accept"));

    assertThatThrownBy(() -> properties.validate())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("CORS allowed-headers must include 'Authorization'");
  }

  @Test
  @DisplayName("Allowed headers missing 'Content-Type' fail validation")
  void testMissingContentTypeHeader_failsValidation() {
    properties.setAllowedHeaders(List.of("Authorization", "Accept"));

    assertThatThrownBy(() -> properties.validate())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("CORS allowed-headers must include 'Content-Type'");
  }

  @Test
  @DisplayName("Non-positive maxAge fails validation")
  void testInvalidMaxAge_failsValidation() {
    properties.setMaxAge(Duration.ZERO);
    assertThatThrownBy(() -> properties.validate())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("CORS max-age must be a positive duration");

    properties.setMaxAge(Duration.ofSeconds(-10));
    assertThatThrownBy(() -> properties.validate())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("CORS max-age must be a positive duration");

    properties.setMaxAge(null);
    assertThatThrownBy(() -> properties.validate())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("CORS max-age must be a positive duration");
  }
}
