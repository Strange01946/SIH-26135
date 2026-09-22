package in.gov.sih.sih26135.dto.request;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class UpdateCertificationRequest {

  private String issuingBody;
  private LocalDate expiryDate;
  private Long certificateStatusId;
  private Long certificateVerificationStatusId;
  private LocalDateTime verifiedAt;
  private Long verifiedByUserId;

  public UpdateCertificationRequest() {
  }

  public UpdateCertificationRequest(
      String issuingBody,
      LocalDate expiryDate,
      Long certificateStatusId,
      Long certificateVerificationStatusId,
      LocalDateTime verifiedAt,
      Long verifiedByUserId) {
    this.issuingBody = issuingBody;
    this.expiryDate = expiryDate;
    this.certificateStatusId = certificateStatusId;
    this.certificateVerificationStatusId = certificateVerificationStatusId;
    this.verifiedAt = verifiedAt;
    this.verifiedByUserId = verifiedByUserId;
  }

  public String getIssuingBody() {
    return issuingBody;
  }

  public void setIssuingBody(String issuingBody) {
    this.issuingBody = issuingBody;
  }

  public LocalDate getExpiryDate() {
    return expiryDate;
  }

  public void setExpiryDate(LocalDate expiryDate) {
    this.expiryDate = expiryDate;
  }

  public Long getCertificateStatusId() {
    return certificateStatusId;
  }

  public void setCertificateStatusId(Long certificateStatusId) {
    this.certificateStatusId = certificateStatusId;
  }

  public Long getCertificateVerificationStatusId() {
    return certificateVerificationStatusId;
  }

  public void setCertificateVerificationStatusId(Long certificateVerificationStatusId) {
    this.certificateVerificationStatusId = certificateVerificationStatusId;
  }

  public LocalDateTime getVerifiedAt() {
    return verifiedAt;
  }

  public void setVerifiedAt(LocalDateTime verifiedAt) {
    this.verifiedAt = verifiedAt;
  }

  public Long getVerifiedByUserId() {
    return verifiedByUserId;
  }

  public void setVerifiedByUserId(Long verifiedByUserId) {
    this.verifiedByUserId = verifiedByUserId;
  }
}
