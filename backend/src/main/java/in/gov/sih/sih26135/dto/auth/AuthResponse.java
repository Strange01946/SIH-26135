package in.gov.sih.sih26135.dto.auth;

import java.util.List;

/**
 * Authentication response payload returning access token, refresh token, and user identity metadata.
 */
public class AuthResponse {

  private String accessToken;
  private String refreshToken;
  private String tokenType;
  private Long expiresIn;
  private Long userId;
  private String username;
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
