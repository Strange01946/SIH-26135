package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public class ConsentTypeResponse {

  private Long id;
  private String consentCode;
  private String consentName;
  private String purpose;
  private Boolean allowsEmploymentFollowup;
  private Boolean isRequired;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public ConsentTypeResponse() {
  }

  public ConsentTypeResponse(
      Long id,
      String consentCode,
      String consentName,
      String purpose,
      Boolean allowsEmploymentFollowup,
      Boolean isRequired,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.consentCode = consentCode;
    this.consentName = consentName;
    this.purpose = purpose;
    this.allowsEmploymentFollowup = allowsEmploymentFollowup;
    this.isRequired = isRequired;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getConsentCode() {
    return consentCode;
  }

  public void setConsentCode(String consentCode) {
    this.consentCode = consentCode;
  }

  public String getConsentName() {
    return consentName;
  }

  public void setConsentName(String consentName) {
    this.consentName = consentName;
  }

  public String getPurpose() {
    return purpose;
  }

  public void setPurpose(String purpose) {
    this.purpose = purpose;
  }

  public Boolean getAllowsEmploymentFollowup() {
    return allowsEmploymentFollowup;
  }

  public void setAllowsEmploymentFollowup(Boolean allowsEmploymentFollowup) {
    this.allowsEmploymentFollowup = allowsEmploymentFollowup;
  }

  public Boolean getIsRequired() {
    return isRequired;
  }

  public void setIsRequired(Boolean isRequired) {
    this.isRequired = isRequired;
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
