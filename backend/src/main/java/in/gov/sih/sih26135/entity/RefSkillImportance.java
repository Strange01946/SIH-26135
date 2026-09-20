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
@Table(name = "ref_skill_importance")
public class RefSkillImportance {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "skill_importance_id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "importance_code", length = 32, nullable = false, unique = true)
  private String importanceCode;

  @Column(name = "importance_name", length = 100, nullable = false)
  private String importanceName;

  @JdbcTypeCode(SqlTypes.TINYINT)
  @Column(name = "importance_weight", nullable = false)
  private Integer importanceWeight;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "sort_order", nullable = false, columnDefinition = "SMALLINT UNSIGNED")
  private Integer sortOrder = 0;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public RefSkillImportance() {
  }

  public RefSkillImportance(String importanceCode, String importanceName, Integer importanceWeight, Integer sortOrder) {
    this.importanceCode = importanceCode;
    this.importanceName = importanceName;
    this.importanceWeight = importanceWeight;
    this.sortOrder = sortOrder;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getImportanceCode() {
    return importanceCode;
  }

  public void setImportanceCode(String importanceCode) {
    this.importanceCode = importanceCode;
  }

  public String getImportanceName() {
    return importanceName;
  }

  public void setImportanceName(String importanceName) {
    this.importanceName = importanceName;
  }

  public Integer getImportanceWeight() {
    return importanceWeight;
  }

  public void setImportanceWeight(Integer importanceWeight) {
    this.importanceWeight = importanceWeight;
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
    if (!(o instanceof RefSkillImportance other)) {
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
    return "RefSkillImportance{" +
        "id=" + id +
        ", importanceCode='" + importanceCode + '\'' +
        ", importanceName='" + importanceName + '\'' +
        ", importanceWeight=" + importanceWeight +
        ", sortOrder=" + sortOrder +
        '}';
  }
}
