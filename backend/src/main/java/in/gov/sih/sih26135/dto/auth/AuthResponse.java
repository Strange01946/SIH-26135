package in.gov.sih.sih26135.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Authentication response payload returning access token, refresh token, and user identity metadata.
 */
@Schema(description = "Authentication token response containing access and refresh tokens")
public class AuthResponse {

  @Schema(description = "Signed JWT access token for API authorization", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
  private String accessToken;

  @Schema(description = "Signed JWT refresh token for renewing access tokens", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
  private String refreshToken;

  @Schema(description = "Token type identifier", example = "Bearer")
  private String tokenType;

  @Schema(description = "Access token lifespan in seconds", example = "900")
  private Long expiresIn;

  @Schema(description = "Unique user identifier", example = "1")
  private Long userId;

  @Schema(description = "Username of authenticated principal", example = "admin")
  private String username;

  @Schema(description = "List of granted role authorities", example = "[\"ROLE_super_admin\"]")
  private List<String> authorities;

  public AuthResponse() {
  }

  public AuthResponse(
      String accessToken,
      String refreshToken,
      String tokenType,
      Long expiresIn,
      Long userId,
      String username,
      List<String> authorities) {
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
    this.tokenType = tokenType != null ? tokenType : "Bearer";
    this.expiresIn = expiresIn;
    this.userId = userId;
    this.username = username;
    this.authorities = authorities;
  }

  public String getAccessToken() {
    return accessToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  public String getRefreshToken() {
    return refreshToken;
  }

  public void setRefreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
  }

  public String getTokenType() {
    return tokenType;
  }

  public void setTokenType(String tokenType) {
    this.tokenType = tokenType;
  }

  public Long getExpiresIn() {
    return expiresIn;
  }

  public void setExpiresIn(Long expiresIn) {
    this.expiresIn = expiresIn;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public List<String> getAuthorities() {
    return authorities;
  }

  public void setAuthorities(List<String> authorities) {
    this.authorities = authorities;
  }
}
