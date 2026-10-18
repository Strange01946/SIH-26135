package in.gov.sih.sih26135.dto.auth;

/**
 * Test fixture demonstrating a package-spoofing attack in the auth DTO package:
 * - Declares the EXACT whitelisted package: in.gov.sih.sih26135.dto.auth
 * - Class simple name ends with the expected suffix "Request"
 * - NOT included in the authoritative TRUSTED_REQUEST_DTO_CLASSES allowlist
 *   (which only permits LoginRequest and RefreshTokenRequest in the auth package).
 *
 * Verifies that GlobalExceptionHandler fails closed to HTTP 500 when validating an unverified auth DTO.
 */
public class SpoofedAuthRequest {

  private String spoofedToken;

  public SpoofedAuthRequest() {}

  public SpoofedAuthRequest(String spoofedToken) {
    this.spoofedToken = spoofedToken;
  }

  public String getSpoofedToken() {
    return spoofedToken;
  }

  public void setSpoofedToken(String spoofedToken) {
    this.spoofedToken = spoofedToken;
  }
}
