package in.gov.sih.sih26135.security.jwt;

import in.gov.sih.sih26135.security.config.JwtProperties;
import in.gov.sih.sih26135.security.jwt.JwtValidationException.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.IncorrectClaimException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.MissingClaimException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * Reusable component for generating, parsing, and validating JSON Web Tokens (JWT).
 *
 * <p>Uses HMAC SHA-256 (HS256) signing with a minimum 256-bit key sourced from {@link JwtProperties}.
 * Generates structurally distinguishable access tokens (short-lived, with roles/authorities)
 * and refresh tokens (longer-lived, minimal claims).
 *
 * <p>Enforces strict token validation: verifies signature, expiration, issuer, and all required
 * claims (sub, uid, token_type, jti, iss, iat, exp). Unconditionally validates secret key strength
 * during application startup.
 */
@Component
public class JwtTokenProvider {

  public static final String CLAIM_TOKEN_TYPE = "token_type";
  public static final String CLAIM_USER_ID = "uid";
  public static final String CLAIM_USERNAME = "username";
  public static final String CLAIM_AUTHORITIES = "authorities";

  public static final String TOKEN_TYPE_ACCESS = "access";
  public static final String TOKEN_TYPE_REFRESH = "refresh";

  private final JwtProperties jwtProperties;
  private volatile SecretKey signingKey;

  public JwtTokenProvider(JwtProperties jwtProperties) {
    this.jwtProperties = jwtProperties;
  }

  /**
   * Unconditionally validates the JWT secret and initializes the cached SecretKey on application startup.
   * Application startup fails immediately if JWT_SECRET is missing, blank, or fewer than 32 bytes.
   */
  @PostConstruct
  public void init() {
    jwtProperties.validateSecret();
    byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
    this.signingKey = Keys.hmacShaKeyFor(keyBytes);
  }

  private SecretKey getSigningKey() {
    if (this.signingKey == null) {
      jwtProperties.validateSecret();
      byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
      this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }
    return this.signingKey;
  }

  /**
   * Generates a short-lived access token for authenticated API requests.
   *
   * @param userId primary key of the user (used as subject and uid claim)
   * @param username user login handle
   * @param authorities collection of granted authorities (e.g., ROLE_super_admin, trainee.read)
   * @return compact URL-safe JWT string
   */
  public String generateAccessToken(Long userId, String username, Collection<String> authorities) {
    if (userId == null) {
      throw new IllegalArgumentException("User ID cannot be null when generating access token");
    }
    if (username == null || username.isBlank()) {
      throw new IllegalArgumentException("Username cannot be blank when generating access token");
    }

    Instant now = Instant.now();
    Instant expiry = now.plusMillis(jwtProperties.getAccessTokenExpirationMs());
    List<String> authorityList = authorities != null ? authorities.stream().toList() : List.of();

    return Jwts.builder()
        .id(UUID.randomUUID().toString())
        .issuer(jwtProperties.getIssuer())
        .subject(String.valueOf(userId))
        .issuedAt(Date.from(now))
        .expiration(Date.from(expiry))
        .claim(CLAIM_USER_ID, userId)
        .claim(CLAIM_USERNAME, username.trim())
        .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS)
        .claim(CLAIM_AUTHORITIES, authorityList)
        .signWith(getSigningKey(), Jwts.SIG.HS256)
        .compact();
  }

  /**
   * Generates a long-lived refresh token strictly for renewing access tokens.
   *
   * <p>Intentionally does not include authorities or username claims to prevent stale authorization data.
   *
   * @param userId primary key of the user
   * @return compact URL-safe JWT string
   */
  public String generateRefreshToken(Long userId) {
    if (userId == null) {
      throw new IllegalArgumentException("User ID cannot be null when generating refresh token");
    }

    Instant now = Instant.now();
    Instant expiry = now.plusMillis(jwtProperties.getRefreshTokenExpirationMs());

    return Jwts.builder()
        .id(UUID.randomUUID().toString())
        .issuer(jwtProperties.getIssuer())
        .subject(String.valueOf(userId))
        .issuedAt(Date.from(now))
        .expiration(Date.from(expiry))
        .claim(CLAIM_USER_ID, userId)
        .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_REFRESH)
        .signWith(getSigningKey(), Jwts.SIG.HS256)
        .compact();
  }

  /**
   * Parses and validates a JWT token against signature, issuer, expiration, and all required claims.
   *
   * @param token compact JWT string
   * @return parsed Claims payload
   * @throws JwtValidationException if token is invalid, expired, malformed, signature mismatch, or missing required claims
   */
  public Claims parseAndValidateToken(String token) {
    if (token == null || token.isBlank()) {
      throw new JwtValidationException("JWT token string cannot be null or blank", ErrorCode.MALFORMED);
    }

    Claims claims;
    try {
      claims = Jwts.parser()
          .verifyWith(getSigningKey())
          .requireIssuer(jwtProperties.getIssuer())
          .build()
          .parseSignedClaims(token.trim())
          .getPayload();
    } catch (ExpiredJwtException e) {
      throw new JwtValidationException("JWT token has expired", ErrorCode.EXPIRED, e);
    } catch (SecurityException e) {
      throw new JwtValidationException("JWT signature validation failed", ErrorCode.INVALID_SIGNATURE, e);
    } catch (MalformedJwtException e) {
      throw new JwtValidationException("JWT token is malformed", ErrorCode.MALFORMED, e);
    } catch (UnsupportedJwtException e) {
      throw new JwtValidationException("JWT token format is unsupported", ErrorCode.UNSUPPORTED, e);
    } catch (IncorrectClaimException e) {
      throw new JwtValidationException("JWT claim validation failed: " + e.getMessage(), ErrorCode.INVALID_ISSUER, e);
    } catch (MissingClaimException e) {
      throw new JwtValidationException("JWT missing required claim: " + e.getMessage(), ErrorCode.MISSING_REQUIRED_CLAIM, e);
    } catch (IllegalArgumentException e) {
      throw new JwtValidationException("JWT token argument is invalid", ErrorCode.MALFORMED, e);
    } catch (JwtException e) {
      throw new JwtValidationException("JWT validation error: " + e.getMessage(), ErrorCode.GENERAL_ERROR, e);
    }

    validateRequiredClaims(claims);
    return claims;
  }

  /**
   * Validates that all application-required claims are present and non-blank.
   * Required claims: sub, uid, token_type, jti, iss, iat, exp.
   *
   * @param claims parsed token claims
   * @throws JwtValidationException if any required claim is missing or invalid
   */
  private void validateRequiredClaims(Claims claims) {
    if (claims.getSubject() == null || claims.getSubject().isBlank()) {
      throw new JwtValidationException("JWT is missing required 'sub' (subject) claim", ErrorCode.MISSING_REQUIRED_CLAIM);
    }
    Object uid = claims.get(CLAIM_USER_ID);
    if (uid == null) {
      throw new JwtValidationException("JWT is missing required 'uid' claim", ErrorCode.MISSING_REQUIRED_CLAIM);
    }
    String tokenType = claims.get(CLAIM_TOKEN_TYPE, String.class);
    if (tokenType == null || tokenType.isBlank()) {
      throw new JwtValidationException("JWT is missing required 'token_type' claim", ErrorCode.MISSING_REQUIRED_CLAIM);
    }
    if (!TOKEN_TYPE_ACCESS.equalsIgnoreCase(tokenType) && !TOKEN_TYPE_REFRESH.equalsIgnoreCase(tokenType)) {
      throw new JwtValidationException(
          String.format("JWT contains invalid 'token_type' claim: '%s'", tokenType), ErrorCode.INVALID_TOKEN_TYPE);
    }
    if (claims.getId() == null || claims.getId().isBlank()) {
      throw new JwtValidationException("JWT is missing required 'jti' claim", ErrorCode.MISSING_REQUIRED_CLAIM);
    }
    if (claims.getIssuer() == null || claims.getIssuer().isBlank()) {
      throw new JwtValidationException("JWT is missing required 'iss' claim", ErrorCode.MISSING_REQUIRED_CLAIM);
    }
    if (claims.getIssuedAt() == null) {
      throw new JwtValidationException("JWT is missing required 'iat' claim", ErrorCode.MISSING_REQUIRED_CLAIM);
    }
    if (claims.getExpiration() == null) {
      throw new JwtValidationException("JWT is missing required 'exp' claim", ErrorCode.MISSING_REQUIRED_CLAIM);
    }
  }

  /**
   * Parses and validates a JWT token and verifies that it matches the expected token type (e.g. access vs refresh).
   *
   * @param token compact JWT string
   * @param expectedTokenType expected token type ("access" or "refresh")
   * @return parsed Claims payload
   * @throws JwtValidationException if token validation fails or token type does not match
   */
  public Claims parseAndValidateToken(String token, String expectedTokenType) {
    if (expectedTokenType == null || expectedTokenType.isBlank()) {
      throw new IllegalArgumentException("Expected token type cannot be null or blank");
    }
    Claims claims = parseAndValidateToken(token);
    String actualTokenType = claims.get(CLAIM_TOKEN_TYPE, String.class);

    if (actualTokenType == null || actualTokenType.isBlank()) {
      throw new JwtValidationException(
          "JWT is missing required 'token_type' claim", ErrorCode.MISSING_REQUIRED_CLAIM);
    }
    if (!actualTokenType.equalsIgnoreCase(expectedTokenType)) {
      throw new JwtValidationException(
          String.format("Invalid JWT token type: expected '%s' but found '%s'", expectedTokenType, actualTokenType),
          ErrorCode.INVALID_TOKEN_TYPE);
    }
    return claims;
  }

  /**
   * Verifies if a JWT token is valid without throwing checked exceptions.
   *
   * @param token compact JWT string
   * @return true if valid, unexpired, and all required claims are present; false otherwise
   */
  public boolean validateToken(String token) {
    try {
      parseAndValidateToken(token);
      return true;
    } catch (JwtValidationException e) {
      return false;
    }
  }

  /**
   * Verifies if a JWT token is valid and matches the expected token type.
   *
   * @param token compact JWT string
   * @param expectedTokenType expected token type ("access" or "refresh")
   * @return true if valid, unexpired, and matches expected token type; false otherwise
   */
  public boolean validateToken(String token, String expectedTokenType) {
    try {
      parseAndValidateToken(token, expectedTokenType);
      return true;
    } catch (JwtValidationException e) {
      return false;
    }
  }

  /**
   * Extracts the user primary key from the token.
   *
   * @param token compact JWT string
   * @return user ID
   */
  public Long extractUserId(String token) {
    Claims claims = parseAndValidateToken(token);
    Object uid = claims.get(CLAIM_USER_ID);
    if (uid instanceof Number number) {
      return number.longValue();
    }
    return Long.parseLong(claims.getSubject());
  }

  /**
   * Extracts the subject from the token.
   *
   * @param token compact JWT string
   * @return subject string (numeric user ID)
   */
  public String extractSubject(String token) {
    return parseAndValidateToken(token).getSubject();
  }

  /**
   * Extracts the username claim from an access token.
   *
   * @param token compact JWT access token string
   * @return username
   */
  public String extractUsername(String token) {
    Claims claims = parseAndValidateToken(token, TOKEN_TYPE_ACCESS);
    String username = claims.get(CLAIM_USERNAME, String.class);
    if (username == null || username.isBlank()) {
      throw new JwtValidationException("Access token is missing required 'username' claim", ErrorCode.MISSING_REQUIRED_CLAIM);
    }
    return username;
  }

  /**
   * Extracts granted authorities from an access token.
   *
   * @param token compact JWT access token string
   * @return list of authority strings
   */
  public List<String> extractAuthorities(String token) {
    Claims claims = parseAndValidateToken(token, TOKEN_TYPE_ACCESS);
    Object authoritiesObj = claims.get(CLAIM_AUTHORITIES);
    if (authoritiesObj instanceof List<?> list) {
      return list.stream().map(Object::toString).toList();
    }
    return List.of();
  }

  /**
   * Extracts the token type ("access" or "refresh").
   *
   * @param token compact JWT string
   * @return token type
   */
  public String extractTokenType(String token) {
    return parseAndValidateToken(token).get(CLAIM_TOKEN_TYPE, String.class);
  }

  /**
   * Extracts the unique JWT ID (jti).
   *
   * @param token compact JWT string
   * @return unique token ID UUID string
   */
  public String extractTokenId(String token) {
    return parseAndValidateToken(token).getId();
  }

  /**
   * Extracts the expiration instant of the token.
   *
   * @param token compact JWT string
   * @return expiration instant
   */
  public Instant extractExpiration(String token) {
    return parseAndValidateToken(token).getExpiration().toInstant();
  }

  /**
   * Checks if a token is expired.
   *
   * @param token compact JWT string
   * @return true if expired; false if still valid
   */
  public boolean isTokenExpired(String token) {
    try {
      Claims claims = parseAndValidateToken(token);
      return claims.getExpiration().before(new Date());
    } catch (JwtValidationException e) {
      if (e.getErrorCode() == ErrorCode.EXPIRED) {
        return true;
      }
      throw e;
    }
  }
}
