package in.gov.sih.sih26135.dto.request;

/**
 * Test fixture demonstrating a package-spoofing attack against request DTO trust:
 * - Declares the EXACT whitelisted package: in.gov.sih.sih26135.dto.request
 * - Class simple name ends with the expected suffix "Request"
 * - NOT included in the authoritative TRUSTED_REQUEST_DTO_CLASSES allowlist.
 *
 * Verifies that GlobalExceptionHandler fails closed to HTTP 500 when validating an unverified DTO.
 */
public class SpoofedWhitelistedPackageRequestDto {

  private String spoofedPayload;

  public SpoofedWhitelistedPackageRequestDto() {}

  public SpoofedWhitelistedPackageRequestDto(String spoofedPayload) {
    this.spoofedPayload = spoofedPayload;
  }

  public String getSpoofedPayload() {
    return spoofedPayload;
  }

  public void setSpoofedPayload(String spoofedPayload) {
    this.spoofedPayload = spoofedPayload;
  }
}
