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
@Table(name = "ref_skill_gap_action_type", uniqueConstraints = {
    @UniqueConstraint(name = "uk_ref_skill_gap_action_type_code", columnNames = {"action_code"})
})
public class RefSkillGapActionType {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "skill_gap_action_type_id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "action_code", length = 32, nullable = false, unique = true)
  private String actionCode;

  @Column(name = "action_name", length = 150, nullable = false)
  private String actionName;

  @Column(name = "requires_course_flag", nullable = false)
  private Boolean requiresCourseFlag = false;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "sort_order", nullable = false, columnDefinition = "SMALLINT UNSIGNED")
  private Integer sortOrder = 0;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public RefSkillGapActionType() {
  }

  public RefSkillGapActionType(String actionCode, String actionName, Integer sortOrder) {
    this.actionCode = actionCode;
    this.actionName = actionName;
    this.sortOrder = sortOrder;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getActionCode() {
    return actionCode;
  }

  public void setActionCode(String actionCode) {
    this.actionCode = actionCode;
  }

  public String getActionName() {
    return actionName;
  }

  public void setActionName(String actionName) {
    this.actionName = actionName;
  }

  public Boolean getRequiresCourseFlag() {
    return requiresCourseFlag;
  }

  public void setRequiresCourseFlag(Boolean requiresCourseFlag) {
    this.requiresCourseFlag = requiresCourseFlag;
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
    if (!(o instanceof RefSkillGapActionType that)) {
      return false;
    }
    return Objects.equals(actionCode, that.actionCode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(actionCode);
  }

  @Override
  public String toString() {
    return "RefSkillGapActionType{" +
        "id=" + id +
        ", actionCode='" + actionCode + '\'' +
        ", actionName='" + actionName + '\'' +
        ", requiresCourseFlag=" + requiresCourseFlag +
        ", sortOrder=" + sortOrder +
        '}';
  }
}
