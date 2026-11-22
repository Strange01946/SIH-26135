package in.gov.sih.sih26135.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "onboarding_applications")
public class OnboardingApplication {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "application_id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "application_number", length = 64, nullable = false, unique = true)
  private String applicationNumber;

  @Column(name = "applicant_type", length = 32, nullable = false)
  private String applicantType;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "entity_type", length = 32, nullable = false)
  private String entityType;

  @Column(name = "entity_id", nullable = false)
  private Long entityId;

  @Column(name = "organization_name", length = 200, nullable = false)
  private String organizationName;

  @Column(name = "contact_person", length = 150, nullable = false)
  private String contactPerson;

  @Column(name = "contact_email", length = 255, nullable = false)
  private String contactEmail;

  @Column(name = "contact_phone", length = 15)
  private String contactPhone;

  @Column(name = "status", length = 32, nullable = false)
  private String status = "PENDING";

  @Column(name = "rejection_reason", length = 500)
  private String rejectionReason;

  @Column(name = "reviewed_by_user_id")
  private Long reviewedByUserId;

  @Column(name = "reviewed_at")
  private LocalDateTime reviewedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public OnboardingApplication() {
  }

  @PrePersist
  public void prePersist() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
    if (updatedAt == null) {
      updatedAt = LocalDateTime.now();
    }
    if (status == null) {
      status = "PENDING";
    }
  }

  @PreUpdate
  public void preUpdate() {
    updatedAt = LocalDateTime.now();
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getApplicationNumber() {
    return applicationNumber;
  }

  public void setApplicationNumber(String applicationNumber) {
    this.applicationNumber = applicationNumber;
  }

  public String getApplicantType() {
    return applicantType;
  }

  public void setApplicantType(String applicantType) {
    this.applicantType = applicantType;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public String getEntityType() {
    return entityType;
  }

  public void setEntityType(String entityType) {
    this.entityType = entityType;
  }

  public Long getEntityId() {
    return entityId;
  }

  public void setEntityId(Long entityId) {
    this.entityId = entityId;
  }

  public String getOrganizationName() {
    return organizationName;
  }

  public void setOrganizationName(String organizationName) {
    this.organizationName = organizationName;
  }

  public String getContactPerson() {
    return contactPerson;
  }

  public void setContactPerson(String contactPerson) {
    this.contactPerson = contactPerson;
  }

  public String getContactEmail() {
    return contactEmail;
  }

  public void setContactEmail(String contactEmail) {
    this.contactEmail = contactEmail;
  }

  public String getContactPhone() {
    return contactPhone;
  }

  public void setContactPhone(String contactPhone) {
    this.contactPhone = contactPhone;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getRejectionReason() {
    return rejectionReason;
  }

  public void setRejectionReason(String rejectionReason) {
    this.rejectionReason = rejectionReason;
  }

  public Long getReviewedByUserId() {
    return reviewedByUserId;
  }

  public void setReviewedByUserId(Long reviewedByUserId) {
    this.reviewedByUserId = reviewedByUserId;
  }

  public LocalDateTime getReviewedAt() {
    return reviewedAt;
  }

  public void setReviewedAt(LocalDateTime reviewedAt) {
    this.reviewedAt = reviewedAt;
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
