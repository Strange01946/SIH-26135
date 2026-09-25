package in.gov.sih.sih26135.security.config;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Cross-Origin Resource Sharing (CORS).
 *
 * <p>Enforces production-safe CORS boundaries:
 * <ul>
 *   <li>Origins must be explicitly configured via environment variable (default: empty / deny all cross-origin).</li>
 *   <li>Wildcard '*' origins are strictly prohibited in production.</li>
 *   <li>HTTP methods and headers are explicitly restricted.</li>
 *   <li>Credentials default to {@code false} as the API relies on stateless Bearer tokens in headers.</li>
 *   <li>Startup validation enforces strict origin URI format and credential safety.</li>
 * </ul>
 */
@Configuration
@ConfigurationProperties(prefix = "security.cors")
public class SecurityCorsProperties {

  /**
   * Explicit list of allowed origins. Backed by environment variable SECURITY_CORS_ALLOWED_ORIGINS.
   * Defaults to empty (cross-origin requests denied by default).
   */
  private List<String> allowedOrigins = new ArrayList<>();

  /**
   * Allowed HTTP methods. Default: GET, POST, PUT, PATCH, DELETE, OPTIONS.
   */
  private List<String> allowedMethods = new ArrayList<>(List.of(
      "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
  ));

  /**
   * Allowed HTTP request headers. Must include Authorization and Content-Type.
   */
  private List<String> allowedHeaders = new ArrayList<>(List.of(
      "Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"
  ));

  /**
   * Response headers exposed to browser clients.
   */
  private List<String> exposedHeaders = new ArrayList<>(List.of(
      "Authorization"
  ));

  /**
   * Whether to send Access-Control-Allow-Credentials.
   * Default: false (stateless Bearer token architecture does not use ambient cookie credentials).
   */
  private boolean allowCredentials = false;

  /**
   * Preflight cache duration. Default: 3600 seconds (1 hour).
   */
  private Duration maxAge = Duration.ofSeconds(3600);

  public List<String> getAllowedOrigins() {
    if (allowedOrigins == null) {
      return Collections.emptyList();
    }
    return allowedOrigins.stream()
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .map(this::normalizeOrigin)
        .collect(Collectors.toList());
  }

  public void setAllowedOrigins(List<String> allowedOrigins) {
    if (allowedOrigins == null) {
      this.allowedOrigins = new ArrayList<>();
    } else {
      this.allowedOrigins = allowedOrigins.stream()
          .map(String::trim)
          .filter(s -> !s.isEmpty())
          .map(this::normalizeOrigin)
          .collect(Collectors.toList());
    }
  }

  public List<String> getAllowedMethods() {
    return allowedMethods;
  }

  public void setAllowedMethods(List<String> allowedMethods) {
    this.allowedMethods = allowedMethods != null ? allowedMethods : new ArrayList<>();
  }

  public List<String> getAllowedHeaders() {
    return allowedHeaders;
  }

  public void setAllowedHeaders(List<String> allowedHeaders) {
    this.allowedHeaders = allowedHeaders != null ? allowedHeaders : new ArrayList<>();
  }

  public List<String> getExposedHeaders() {
    return exposedHeaders;
  }

  public void setExposedHeaders(List<String> exposedHeaders) {
    this.exposedHeaders = exposedHeaders != null ? exposedHeaders : new ArrayList<>();
  }

  public boolean isAllowCredentials() {
    return allowCredentials;
  }

  public void setAllowCredentials(boolean allowCredentials) {
    this.allowCredentials = allowCredentials;
  }

  public Duration getMaxAge() {
    return maxAge;
  }

  public void setMaxAge(Duration maxAge) {
    this.maxAge = maxAge;
  }

  private String normalizeOrigin(String origin) {
    if (origin == null) {
      return null;
    }
    String trimmed = origin.trim();
    if (trimmed.endsWith("/")) {
      trimmed = trimmed.substring(0, trimmed.length() - 1);
    }
    return trimmed;
  }

  /**
   * Validates CORS configuration at application startup.
   *
   * @throws IllegalStateException if configuration violates security contracts
   */
  @PostConstruct
  public void validate() {
    List<String> cleanOrigins = getAllowedOrigins();

    // Enforce no wildcard origins or patterns
    for (String origin : cleanOrigins) {
      if (origin.contains("*")) {
        throw new IllegalStateException(
            "CORS wildcard origin or pattern '" + origin + "' is prohibited in production security configuration. "
                + "Configure explicit origin URIs via SECURITY_CORS_ALLOWED_ORIGINS.");
      }
      if (!origin.startsWith("http://") && !origin.startsWith("https://")) {
        throw new IllegalStateException(
            "Invalid CORS allowed origin '" + origin + "': must start with http:// or https://");
      }
    }

    if (allowCredentials) {
      boolean hasWildcard = cleanOrigins.stream().anyMatch(o -> o.contains("*"));
      if (hasWildcard) {
        throw new IllegalStateException(
            "CORS wildcard origin '*' or pattern is prohibited when allowCredentials is true.");
      }
    }

    // Ensure required headers are allowed
    if (allowedHeaders != null) {
      boolean hasAuth = allowedHeaders.stream()
          .anyMatch(h -> "authorization".equalsIgnoreCase(h.trim()));
      boolean hasContentType = allowedHeaders.stream()
          .anyMatch(h -> "content-type".equalsIgnoreCase(h.trim()));

      if (!hasAuth) {
        throw new IllegalStateException(
            "CORS allowed-headers must include 'Authorization' for token-based authentication.");
      }
      if (!hasContentType) {
        throw new IllegalStateException(
            "CORS allowed-headers must include 'Content-Type' for JSON payloads.");
      }
    }

    if (maxAge == null || maxAge.isNegative() || maxAge.isZero()) {
      throw new IllegalStateException(
          "CORS max-age must be a positive duration.");
    }
  }
}
