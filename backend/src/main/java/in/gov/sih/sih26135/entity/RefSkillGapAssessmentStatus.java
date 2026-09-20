package in.gov.sih.sih26135.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "ref_skill_gap_assessment_status", uniqueConstraints = {
    @UniqueConstraint(name = "uk_ref_skill_gap_assessment_status_code", columnNames = {"status_code"})
})
public class RefSkillGapAssessmentStatus {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "skill_gap_assessment_status_id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "status_code", length = 32, nullable = false, unique = true)
  private String statusCode;

  @Column(name = "status_name", length = 100, nullable = false)
  private String statusName;

  @Column(name = "is_completed_flag", nullable = false)
  private Boolean isCompletedFlag = false;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "sort_order", nullable = false, columnDefinition = "SMALLINT UNSIGNED")
  private Integer sortOrder = 0;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public RefSkillGapAssessmentStatus() {
  }

  public RefSkillGapAssessmentStatus(String statusCode, String statusName, Integer sortOrder) {
    this.statusCode = statusCode;
    this.statusName = statusName;
    this.sortOrder = sortOrder;
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

  public Boolean getIsCompletedFlag() {
    return isCompletedFlag;
  }

  public void setIsCompletedFlag(Boolean isCompletedFlag) {
    this.isCompletedFlag = isCompletedFlag;
  }

  public Integer getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(Integer sortOrder) {
    this.sortOrder = sortOrder;
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
    if (!(o instanceof RefSkillGapAssessmentStatus that)) {
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
    return "RefSkillGapAssessmentStatus{" +
        "id=" + id +
        ", statusCode='" + statusCode + '\'' +
        ", statusName='" + statusName + '\'' +
        ", isCompletedFlag=" + isCompletedFlag +
        ", sortOrder=" + sortOrder +
        '}';
  }
}
