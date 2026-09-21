package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public class TraineeConsentResponse {

  private Long id;
  private Long traineeId;
  private String traineeRegistrationNumber;
  private Long consentTypeId;
  private String consentTypeCode;
  private String consentTypeName;
  private Long consentStatusId;
  private String consentStatusCode;
  private String consentStatusName;
  private String purpose;
  private String policyVersion;
  private LocalDateTime grantedAt;
  private LocalDateTime revokedAt;
  private LocalDateTime expiresAt;
  private String capturedChannel;
  private Long capturedByUserId;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public TraineeConsentResponse() {
  }

  public TraineeConsentResponse(
      Long id,
      Long traineeId,
      String traineeRegistrationNumber,
      Long consentTypeId,
      String consentTypeCode,
      String consentTypeName,
      Long consentStatusId,
      String consentStatusCode,
      String consentStatusName,
      String purpose,
      String policyVersion,
      LocalDateTime grantedAt,
      LocalDateTime revokedAt,
      LocalDateTime expiresAt,
      String capturedChannel,
      Long capturedByUserId,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.traineeId = traineeId;
    this.traineeRegistrationNumber = traineeRegistrationNumber;
    this.consentTypeId = consentTypeId;
    this.consentTypeCode = consentTypeCode;
    this.consentTypeName = consentTypeName;
    this.consentStatusId = consentStatusId;
    this.consentStatusCode = consentStatusCode;
    this.consentStatusName = consentStatusName;
    this.purpose = purpose;
    this.policyVersion = policyVersion;
    this.grantedAt = grantedAt;
    this.revokedAt = revokedAt;
    this.expiresAt = expiresAt;
    this.capturedChannel = capturedChannel;
    this.capturedByUserId = capturedByUserId;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getTraineeId() {
    return traineeId;
  }

  public void setTraineeId(Long traineeId) {
    this.traineeId = traineeId;
  }

  public String getTraineeRegistrationNumber() {
    return traineeRegistrationNumber;
  }

  public void setTraineeRegistrationNumber(String traineeRegistrationNumber) {
    this.traineeRegistrationNumber = traineeRegistrationNumber;
  }

  public Long getConsentTypeId() {
    return consentTypeId;
  }

  public void setConsentTypeId(Long consentTypeId) {
    this.consentTypeId = consentTypeId;
  }

  public String getConsentTypeCode() {
    return consentTypeCode;
  }

  public void setConsentTypeCode(String consentTypeCode) {
    this.consentTypeCode = consentTypeCode;
  }

  public String getConsentTypeName() {
    return consentTypeName;
  }

  public void setConsentTypeName(String consentTypeName) {
    this.consentTypeName = consentTypeName;
  }

  public Long getConsentStatusId() {
    return consentStatusId;
  }

  public void setConsentStatusId(Long consentStatusId) {
    this.consentStatusId = consentStatusId;
  }

  public String getConsentStatusCode() {
    return consentStatusCode;
  }

  public void setConsentStatusCode(String consentStatusCode) {
    this.consentStatusCode = consentStatusCode;
  }

  public String getConsentStatusName() {
    return consentStatusName;
  }

  public void setConsentStatusName(String consentStatusName) {
    this.consentStatusName = consentStatusName;
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

  public LocalDateTime getRevokedAt() {
    return revokedAt;
  }

  public void setRevokedAt(LocalDateTime revokedAt) {
    this.revokedAt = revokedAt;
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

  public Long getCapturedByUserId() {
    return capturedByUserId;
  }

  public void setCapturedByUserId(Long capturedByUserId) {
    this.capturedByUserId = capturedByUserId;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
