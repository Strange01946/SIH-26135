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
@Table(name = "ref_assessment_outcome")
public class RefAssessmentOutcome {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "assessment_outcome_id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "outcome_code", length = 32, nullable = false, unique = true)
  private String outcomeCode;

  @Column(name = "outcome_name", length = 100, nullable = false)
  private String outcomeName;

  @Column(name = "is_pass_flag", nullable = false)
  private Boolean isPassFlag = false;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "sort_order", nullable = false, columnDefinition = "SMALLINT UNSIGNED")
  private Integer sortOrder = 0;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public RefAssessmentOutcome() {
  }

  public RefAssessmentOutcome(String outcomeCode, String outcomeName, Boolean isPassFlag, Integer sortOrder) {
    this.outcomeCode = outcomeCode;
    this.outcomeName = outcomeName;
    this.isPassFlag = isPassFlag != null ? isPassFlag : false;
    this.sortOrder = sortOrder != null ? sortOrder : 0;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getOutcomeCode() {
    return outcomeCode;
  }

  public void setOutcomeCode(String outcomeCode) {
    this.outcomeCode = outcomeCode;
  }

  public String getOutcomeName() {
    return outcomeName;
  }

  public void setOutcomeName(String outcomeName) {
    this.outcomeName = outcomeName;
  }

  public Boolean getIsPassFlag() {
    return isPassFlag;
  }

  public void setIsPassFlag(Boolean passFlag) {
    isPassFlag = passFlag != null ? passFlag : false;
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
    if (!(o instanceof RefAssessmentOutcome other)) {
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
    return "RefAssessmentOutcome{" +
        "id=" + id +
        ", outcomeCode='" + outcomeCode + '\'' +
        ", outcomeName='" + outcomeName + '\'' +
        ", isPassFlag=" + isPassFlag +
        ", sortOrder=" + sortOrder +
        '}';
  }
}
