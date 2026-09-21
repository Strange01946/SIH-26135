package in.gov.sih.sih26135.dto.request;

import java.time.LocalDateTime;

public class CreateTraineeConsentRequest {

  private Long traineeId;
  private Long consentTypeId;
  private Long consentStatusId;
  private String purpose;
  private String policyVersion;
  private LocalDateTime grantedAt;
  private LocalDateTime expiresAt;
  private String capturedChannel;

  public CreateTraineeConsentRequest() {
  }

  public CreateTraineeConsentRequest(
      Long traineeId,
      Long consentTypeId,
      Long consentStatusId,
      String purpose,
      String policyVersion,
      LocalDateTime grantedAt,
      LocalDateTime expiresAt,
      String capturedChannel) {
    this.traineeId = traineeId;
    this.consentTypeId = consentTypeId;
    this.consentStatusId = consentStatusId;
    this.purpose = purpose;
    this.policyVersion = policyVersion;
    this.grantedAt = grantedAt;
    this.expiresAt = expiresAt;
    this.capturedChannel = capturedChannel;
  }

  public Long getTraineeId() {
    return traineeId;
  }

  public void setTraineeId(Long traineeId) {
    this.traineeId = traineeId;
  }

  public Long getConsentTypeId() {
    return consentTypeId;
  }

  public void setConsentTypeId(Long consentTypeId) {
    this.consentTypeId = consentTypeId;
  }

  public Long getConsentStatusId() {
    return consentStatusId;
  }

  public void setConsentStatusId(Long consentStatusId) {
    this.consentStatusId = consentStatusId;
  }

  public String getPurpose() {
    return purpose;
  }

  public void setPurpose(String purpose) {
    this.purpose = purpose;
  }

  public String getPolicyVersion() {
    return policyVersion;
  }

  public void setPolicyVersion(String policyVersion) {
    this.policyVersion = policyVersion;
  }

  public LocalDateTime getGrantedAt() {
    return grantedAt;
  }

  public void setGrantedAt(LocalDateTime grantedAt) {
    this.grantedAt = grantedAt;
  }

  public LocalDateTime getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(LocalDateTime expiresAt) {
    this.expiresAt = expiresAt;
  }

  public String getCapturedChannel() {
    return capturedChannel;
  }

  public void setCapturedChannel(String capturedChannel) {
    this.capturedChannel = capturedChannel;
  }
}
