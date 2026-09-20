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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "ref_communication_purpose")
public class RefCommunicationPurpose {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "communication_purpose_id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "purpose_code", length = 32, nullable = false, unique = true)
  private String purposeCode;

  @Column(name = "purpose_name", length = 150, nullable = false)
  private String purposeName;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "required_consent_type_id")
  private RefConsentType requiredConsentType;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "sort_order", nullable = false, columnDefinition = "SMALLINT UNSIGNED")
  private Integer sortOrder = 0;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public RefCommunicationPurpose() {
  }

  public RefCommunicationPurpose(String purposeCode, String purposeName, Integer sortOrder) {
    this.purposeCode = purposeCode;
    this.purposeName = purposeName;
    this.sortOrder = sortOrder != null ? sortOrder : 0;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getPurposeCode() {
    return purposeCode;
  }

  public void setPurposeCode(String purposeCode) {
    this.purposeCode = purposeCode;
  }

  public String getPurposeName() {
    return purposeName;
  }

  public void setPurposeName(String purposeName) {
    this.purposeName = purposeName;
  }

  public RefConsentType getRequiredConsentType() {
    return requiredConsentType;
  }

  public void setRequiredConsentType(RefConsentType requiredConsentType) {
    this.requiredConsentType = requiredConsentType;
  }

  public Integer getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(Integer sortOrder) {
    this.sortOrder = sortOrder != null ? sortOrder : 0;
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
    if (!(o instanceof RefCommunicationPurpose that)) {
      return false;
    }
    return Objects.equals(purposeCode, that.purposeCode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(purposeCode);
  }

  @Override
  public String toString() {
    return "RefCommunicationPurpose{" +
        "id=" + id +
        ", purposeCode='" + purposeCode + '\'' +
        ", purposeName='" + purposeName + '\'' +
        ", sortOrder=" + sortOrder +
        '}';
  }
}
