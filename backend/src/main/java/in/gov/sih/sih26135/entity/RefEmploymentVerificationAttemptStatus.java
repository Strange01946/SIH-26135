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
@Table(name = "ref_employment_verification_attempt_status")
public class RefEmploymentVerificationAttemptStatus {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "employment_verification_attempt_status_id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "status_code", length = 32, nullable = false, unique = true)
  private String statusCode;

  @Column(name = "status_name", length = 100, nullable = false)
  private String statusName;

  @Column(name = "is_terminal_flag", nullable = false)
  private Boolean isTerminalFlag = false;

  @Column(name = "is_success_flag", nullable = false)
  private Boolean isSuccessFlag = false;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "sort_order", nullable = false, columnDefinition = "SMALLINT UNSIGNED")
  private Integer sortOrder = 0;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public RefEmploymentVerificationAttemptStatus() {
  }

  public RefEmploymentVerificationAttemptStatus(String statusCode, String statusName,
      Boolean isTerminalFlag, Boolean isSuccessFlag, Integer sortOrder) {
    this.statusCode = statusCode;
    this.statusName = statusName;
    this.isTerminalFlag = isTerminalFlag != null ? isTerminalFlag : false;
    this.isSuccessFlag = isSuccessFlag != null ? isSuccessFlag : false;
    this.sortOrder = sortOrder != null ? sortOrder : 0;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getStatusCode() {
    return statusCode;
  }

  public void setStatusCode(String statusCode) {
    this.statusCode = statusCode;
  }

  public String getStatusName() {
    return statusName;
  }

  public void setStatusName(String statusName) {
    this.statusName = statusName;
  }

  public Boolean getIsTerminalFlag() {
    return isTerminalFlag;
  }

  public void setIsTerminalFlag(Boolean terminalFlag) {
    isTerminalFlag = terminalFlag != null ? terminalFlag : false;
  }

  public Boolean getIsSuccessFlag() {
    return isSuccessFlag;
  }

  public void setIsSuccessFlag(Boolean successFlag) {
    isSuccessFlag = successFlag != null ? successFlag : false;
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
    if (!(o instanceof RefEmploymentVerificationAttemptStatus that)) {
      return false;
    }
    return Objects.equals(statusCode, that.statusCode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(statusCode);
  }

  @Override
  public String toString() {
    return "RefEmploymentVerificationAttemptStatus{" +
        "id=" + id +
        ", statusCode='" + statusCode + '\'' +
        ", statusName='" + statusName + '\'' +
        ", isTerminalFlag=" + isTerminalFlag +
        ", isSuccessFlag=" + isSuccessFlag +
        ", sortOrder=" + sortOrder +
        '}';
  }
}
