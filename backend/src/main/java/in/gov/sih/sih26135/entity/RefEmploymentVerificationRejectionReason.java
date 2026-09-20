package in.gov.sih.sih26135.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "ref_employment_verification_rejection_reason")
public class RefEmploymentVerificationRejectionReason {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "employment_verification_rejection_reason_id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "reason_code", length = 32, nullable = false, unique = true)
  private String reasonCode;

  @Column(name = "reason_name", length = 150, nullable = false)
  private String reasonName;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "sort_order", nullable = false, columnDefinition = "SMALLINT UNSIGNED")
  private Integer sortOrder = 0;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public RefEmploymentVerificationRejectionReason() {
  }

  public RefEmploymentVerificationRejectionReason(String reasonCode, String reasonName, Integer sortOrder) {
    this.reasonCode = reasonCode;
    this.reasonName = reasonName;
    this.sortOrder = sortOrder != null ? sortOrder : 0;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getReasonCode() {
    return reasonCode;
  }

  public void setReasonCode(String reasonCode) {
    this.reasonCode = reasonCode;
  }

  public String getReasonName() {
    return reasonName;
  }

  public void setReasonName(String reasonName) {
    this.reasonName = reasonName;
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
    if (!(o instanceof RefEmploymentVerificationRejectionReason that)) {
      return false;
    }
    return Objects.equals(reasonCode, that.reasonCode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(reasonCode);
  }

  @Override
  public String toString() {
    return "RefEmploymentVerificationRejectionReason{" +
        "id=" + id +
        ", reasonCode='" + reasonCode + '\'' +
        ", reasonName='" + reasonName + '\'' +
        ", sortOrder=" + sortOrder +
        '}';
  }
}
