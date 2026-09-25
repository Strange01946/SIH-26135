package in.gov.sih.sih26135.dto.auth;

/**
 * Request payload for renewing access tokens using a valid refresh token.
 */
public class RefreshTokenRequest {

  private String refreshToken;

  public RefreshTokenRequest() {
  }

  public RefreshTokenRequest(String refreshToken) {
    this.refreshToken = refreshToken;
  }

  public String getRefreshToken() {
    return refreshToken;
  }

  public void setRefreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
  }

  @Override
  public String toString() {
    return "RefreshTokenRequest{[PROTECTED]}";
  }
}
