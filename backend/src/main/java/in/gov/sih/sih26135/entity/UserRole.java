package in.gov.sih.sih26135.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "user_roles")
@IdClass(UserRoleId.class)
public class UserRole {

  @Id
  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Id
  @Column(name = "role_id", nullable = false)
  private Long roleId;

  @Column(name = "assigned_at", nullable = false, updatable = false)
  private LocalDateTime assignedAt;

  @Column(name = "assigned_by_user_id")
  private Long assignedByUserId;

  public UserRole() {
  }

  public UserRole(Long userId, Long roleId) {
    this.userId = userId;
    this.roleId = roleId;
  }

  public UserRole(Long userId, Long roleId, Long assignedByUserId) {
    this.userId = userId;
    this.roleId = roleId;
    this.assignedByUserId = assignedByUserId;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public Long getRoleId() {
    return roleId;
  }

  public void setRoleId(Long roleId) {
    this.roleId = roleId;
  }

  public LocalDateTime getAssignedAt() {
    return assignedAt;
  }

  public void setAssignedAt(LocalDateTime assignedAt) {
    this.assignedAt = assignedAt;
  }

  public Long getAssignedByUserId() {
    return assignedByUserId;
  }

  public void setAssignedByUserId(Long assignedByUserId) {
    this.assignedByUserId = assignedByUserId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof UserRole other)) {
      return false;
    }
    return Objects.equals(userId, other.userId) && Objects.equals(roleId, other.roleId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, roleId);
  }

  @Override
  public String toString() {
    return "UserRole{" +
        "userId=" + userId +
        ", roleId=" + roleId +
        ", assignedAt=" + assignedAt +
        ", assignedByUserId=" + assignedByUserId +
        '}';
  }
}
