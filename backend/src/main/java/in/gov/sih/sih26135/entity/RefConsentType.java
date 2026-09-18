package in.gov.sih.sih26135.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "ref_consent_type")
public class RefConsentType {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "consent_type_id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "consent_code", length = 64, nullable = false, unique = true)
  private String consentCode;

  @Column(name = "consent_name", length = 150, nullable = false)
  private String consentName;

  @Column(name = "purpose", length = 500, nullable = false)
  private String purpose;

  @Column(name = "allows_employment_followup", nullable = false)
  private Boolean allowsEmploymentFollowup = false;

  @Column(name = "is_required", nullable = false)
  private Boolean isRequired = false;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public RefConsentType() {
  }

  public RefConsentType(String consentCode, String consentName, String purpose,
      Boolean allowsEmploymentFollowup, Boolean isRequired) {
    this.consentCode = consentCode;
    this.consentName = consentName;
    this.purpose = purpose;
    this.allowsEmploymentFollowup = allowsEmploymentFollowup;
    this.isRequired = isRequired;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof RefConsentType other)) {
      return false;
    }
    return id != null && id.equals(other.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  @Override
  public String toString() {
    return "RefConsentType{" +
        "id=" + id +
        ", consentCode='" + consentCode + '\'' +
        ", consentName='" + consentName + '\'' +
        ", allowsEmploymentFollowup=" + allowsEmploymentFollowup +
        ", isRequired=" + isRequired +
        '}';
  }
}
