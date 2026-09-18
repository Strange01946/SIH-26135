package in.gov.sih.sih26135.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "trainee_consents")
public class TraineeConsent {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "trainee_consent_id", nullable = false, updatable = false)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "trainee_id", nullable = false)
  private Trainee trainee;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "consent_type_id", nullable = false)
  private RefConsentType consentType;

  @Column(name = "purpose", length = 500, nullable = false)
  private String purpose;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "consent_status_id", nullable = false)
  private RefConsentStatus consentStatus;

  @Column(name = "policy_version", length = 32, nullable = false)
  private String policyVersion;

  @Column(name = "granted_at")
  private LocalDateTime grantedAt;

  @Column(name = "revoked_at")
  private LocalDateTime revokedAt;

  @Column(name = "expires_at")
  private LocalDateTime expiresAt;

  @Column(name = "captured_channel", length = 32)
  private String capturedChannel;

  @Column(name = "captured_by_user_id")
  private Long capturedByUserId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public TraineeConsent() {
  }

  public TraineeConsent(Trainee trainee, RefConsentType consentType, String purpose,
      RefConsentStatus consentStatus, String policyVersion) {
    this.trainee = trainee;
    this.consentType = consentType;
    this.purpose = purpose;
    this.consentStatus = consentStatus;
    this.policyVersion = policyVersion;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Trainee getTrainee() {
    return trainee;
  }

  public void setTrainee(Trainee trainee) {
    this.trainee = trainee;
  }

  public RefConsentType getConsentType() {
    return consentType;
  }

  public void setConsentType(RefConsentType consentType) {
    this.consentType = consentType;
  }

  public String getPurpose() {
    return purpose;
  }

  public void setPurpose(String purpose) {
    this.purpose = purpose;
  }

  public RefConsentStatus getConsentStatus() {
    return consentStatus;
  }

  public void setConsentStatus(RefConsentStatus consentStatus) {
    this.consentStatus = consentStatus;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TraineeConsent other)) {
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
    return "TraineeConsent{" +
        "id=" + id +
        ", purpose='" + purpose + '\'' +
        ", policyVersion='" + policyVersion + '\'' +
        ", grantedAt=" + grantedAt +
        ", revokedAt=" + revokedAt +
        '}';
  }
}
