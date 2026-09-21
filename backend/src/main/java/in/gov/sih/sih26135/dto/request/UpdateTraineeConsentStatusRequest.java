package in.gov.sih.sih26135.dto.request;

import java.time.LocalDateTime;

public class UpdateTraineeConsentStatusRequest {

  private Long consentStatusId;
  private LocalDateTime revokedAt;

  public UpdateTraineeConsentStatusRequest() {
  }

  public UpdateTraineeConsentStatusRequest(Long consentStatusId, LocalDateTime revokedAt) {
    this.consentStatusId = consentStatusId;
    this.revokedAt = revokedAt;
  }

  public Long getConsentStatusId() {
    return consentStatusId;
  }

  public void setConsentStatusId(Long consentStatusId) {
    this.consentStatusId = consentStatusId;
  }

  public LocalDateTime getRevokedAt() {
    return revokedAt;
  }

  public void setRevokedAt(LocalDateTime revokedAt) {
    this.revokedAt = revokedAt;
  }
}
