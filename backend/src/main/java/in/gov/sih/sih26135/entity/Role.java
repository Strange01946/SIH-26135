package in.gov.sih.sih26135.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "roles")
public class Role {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "role_id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "role_code", length = 64, nullable = false, unique = true)
  private String roleCode;

  @Column(name = "role_name", length = 100, nullable = false)
  private String roleName;

  @Column(name = "description", length = 500)
  private String description;

  @Column(name = "is_system_role", nullable = false)
  private Boolean isSystemRole = true;

  @Column(name = "lifecycle_status_id", nullable = false)
  private Long lifecycleStatusId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  public Role() {
  }

  public Role(String roleCode, String roleName, String description, Boolean isSystemRole, Long lifecycleStatusId) {
    this.roleCode = roleCode;
    this.roleName = roleName;
    this.description = description;
    this.isSystemRole = isSystemRole;
    this.lifecycleStatusId = lifecycleStatusId;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getRoleCode() {
    return roleCode;
  }

  public void setRoleCode(String roleCode) {
    this.roleCode = roleCode;
  }

  public String getRoleName() {
    return roleName;
  }

  public void setRoleName(String roleName) {
    this.roleName = roleName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Boolean getIsSystemRole() {
    return isSystemRole;
  }

  public void setIsSystemRole(Boolean isSystemRole) {
    this.isSystemRole = isSystemRole;
  }

  public Long getLifecycleStatusId() {
    return lifecycleStatusId;
  }

  public void setLifecycleStatusId(Long lifecycleStatusId) {
    this.lifecycleStatusId = lifecycleStatusId;
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

  public LocalDateTime getDeletedAt() {
    return deletedAt;
  }

  public void setDeletedAt(LocalDateTime deletedAt) {
    this.deletedAt = deletedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Role other)) {
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
    return "Role{" +
        "id=" + id +
        ", roleCode='" + roleCode + '\'' +
        ", roleName='" + roleName + '\'' +
        ", isSystemRole=" + isSystemRole +
        ", lifecycleStatusId=" + lifecycleStatusId +
        '}';
  }
}
