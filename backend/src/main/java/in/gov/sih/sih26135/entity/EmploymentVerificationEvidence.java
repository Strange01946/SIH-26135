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
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "employment_verification_evidence", uniqueConstraints = {
    @UniqueConstraint(name = "uk_emp_verif_evidence_reference", columnNames = {
        "employment_verification_id", "document_reference_code"
    })
})
public class EmploymentVerificationEvidence {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "employment_verification_evidence_id", nullable = false, updatable = false)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "employment_verification_id", nullable = false)
  private EmploymentVerification employmentVerification;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "employment_verification_attempt_id")
  private EmploymentVerificationAttempt employmentVerificationAttempt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "employment_verification_evidence_type_id", nullable = false)
  private RefEmploymentVerificationEvidenceType employmentVerificationEvidenceType;

  @Column(name = "document_reference_code", length = 64, nullable = false)
  private String documentReferenceCode;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "content_sha256", length = 64, columnDefinition = "CHAR(64)")
  private String contentSha256;

  @Column(name = "original_filename", length = 200)
  private String originalFilename;

  @Column(name = "captured_at", nullable = false)
  private LocalDateTime capturedAt;

  @Column(name = "uploaded_by_user_id")
  private Long uploadedByUserId;

  @Column(name = "notes", length = 500)
  private String notes;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public EmploymentVerificationEvidence() {
  }

  public EmploymentVerificationEvidence(EmploymentVerification employmentVerification,
      RefEmploymentVerificationEvidenceType employmentVerificationEvidenceType,
      String documentReferenceCode, LocalDateTime capturedAt) {
    this.employmentVerification = employmentVerification;
    this.employmentVerificationEvidenceType = employmentVerificationEvidenceType;
    this.documentReferenceCode = documentReferenceCode;
    this.capturedAt = capturedAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public EmploymentVerification getEmploymentVerification() {
    return employmentVerification;
  }

  public void setEmploymentVerification(EmploymentVerification employmentVerification) {
    this.employmentVerification = employmentVerification;
  }

  public EmploymentVerificationAttempt getEmploymentVerificationAttempt() {
    return employmentVerificationAttempt;
  }

  public void setEmploymentVerificationAttempt(
      EmploymentVerificationAttempt employmentVerificationAttempt) {
    this.employmentVerificationAttempt = employmentVerificationAttempt;
  }

  public RefEmploymentVerificationEvidenceType getEmploymentVerificationEvidenceType() {
    return employmentVerificationEvidenceType;
  }

  public void setEmploymentVerificationEvidenceType(
      RefEmploymentVerificationEvidenceType employmentVerificationEvidenceType) {
    this.employmentVerificationEvidenceType = employmentVerificationEvidenceType;
  }

  public String getDocumentReferenceCode() {
    return documentReferenceCode;
  }

  public void setDocumentReferenceCode(String documentReferenceCode) {
    this.documentReferenceCode = documentReferenceCode;
  }

  public String getContentSha256() {
    return contentSha256;
  }

  public void setContentSha256(String contentSha256) {
    this.contentSha256 = contentSha256;
  }

  public String getOriginalFilename() {
    return originalFilename;
  }

  public void setOriginalFilename(String originalFilename) {
    this.originalFilename = originalFilename;
  }

  public LocalDateTime getCapturedAt() {
    return capturedAt;
  }

  public void setCapturedAt(LocalDateTime capturedAt) {
    this.capturedAt = capturedAt;
  }

  public Long getUploadedByUserId() {
    return uploadedByUserId;
  }

  public void setUploadedByUserId(Long uploadedByUserId) {
    this.uploadedByUserId = uploadedByUserId;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
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
    if (!(o instanceof EmploymentVerificationEvidence that)) {
      return false;
    }
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "EmploymentVerificationEvidence{" +
        "id=" + id +
        ", documentReferenceCode='" + documentReferenceCode + '\'' +
        ", originalFilename='" + originalFilename + '\'' +
        ", capturedAt=" + capturedAt +
        '}';
  }
}
