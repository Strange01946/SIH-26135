package in.gov.sih.sih26135.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/**
 * Request payload for renewing access tokens using a valid refresh token.
 */
@Schema(description = "Refresh token renewal payload")
public class RefreshTokenRequest {

  @Schema(description = "Cryptographically signed JWT refresh token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "Refresh token is required")
  @Size(max = 512, message = "Refresh token cannot exceed 512 characters")
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
