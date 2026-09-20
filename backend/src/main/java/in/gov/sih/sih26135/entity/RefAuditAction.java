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
@Table(name = "ref_audit_action", uniqueConstraints = {
    @UniqueConstraint(name = "uk_ref_audit_action_code", columnNames = {"action_code"})
})
public class RefAuditAction {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "audit_action_id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "action_code", length = 32, nullable = false, unique = true)
  private String actionCode;

  @Column(name = "action_name", length = 100, nullable = false)
  private String actionName;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "sort_order", nullable = false, columnDefinition = "SMALLINT UNSIGNED")
  private Integer sortOrder = 0;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public RefAuditAction() {
  }

  public RefAuditAction(String actionCode, String actionName, Integer sortOrder) {
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
    if (!(o instanceof RefAuditAction that)) {
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
    return "RefAuditAction{" +
        "id=" + id +
        ", actionCode='" + actionCode + '\'' +
        ", actionName='" + actionName + '\'' +
        ", sortOrder=" + sortOrder +
        '}';
  }
}
