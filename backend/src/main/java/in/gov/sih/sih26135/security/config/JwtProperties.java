package in.gov.sih.sih26135.security.config;

import java.nio.charset.StandardCharsets;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for the JWT token infrastructure.
 *
 * <p>Secret key material is backed by environment variables (JWT_SECRET) and must provide
 * at least 256 bits (32 bytes) of cryptographic key material for HMAC-SHA256 signing.
 * No hardcoded fallback or default secrets are permitted in version control.
 */
@Configuration
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

  public static final int MIN_SECRET_LENGTH_BYTES = 32;

  private String secret;
  private String issuer = "in.gov.sih.sih26135";
  private long accessTokenExpirationMs = 900_000L; // 15 minutes default
  private long refreshTokenExpirationMs = 604_800_000L; // 7 days default

  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }

  public String getIssuer() {
    return issuer;
  }

  public void setIssuer(String issuer) {
    this.issuer = issuer;
  }

  public long getAccessTokenExpirationMs() {
    return accessTokenExpirationMs;
  }

  public void setAccessTokenExpirationMs(long accessTokenExpirationMs) {
    this.accessTokenExpirationMs = accessTokenExpirationMs;
  }

  public long getRefreshTokenExpirationMs() {
    return refreshTokenExpirationMs;
  }

  public void setRefreshTokenExpirationMs(long refreshTokenExpirationMs) {
    this.refreshTokenExpirationMs = refreshTokenExpirationMs;
  }

  /**
   * Validates that the secret key material is configured and meets the minimum cryptographic strength.
   *
   * @throws IllegalStateException if secret is null, blank, or fewer than 32 bytes (256 bits).
   */
  public void validateSecret() {
    if (secret == null || secret.isBlank()) {
      throw new IllegalStateException(
          "JWT secret is not configured. Environment variable JWT_SECRET must be provided with at least 32 bytes (256 bits) of cryptographic key material.");
    }
    byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
    if (keyBytes.length < MIN_SECRET_LENGTH_BYTES) {
      throw new IllegalStateException(
          "JWT secret is insufficiently secure for HMAC-SHA256: requires at least 256 bits (32 bytes), but provided key is only "
              + keyBytes.length + " bytes.");
    }
  }
}
