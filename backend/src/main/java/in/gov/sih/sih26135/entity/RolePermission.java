package in.gov.sih.sih26135.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "role_permissions")
@IdClass(RolePermissionId.class)
public class RolePermission {

  @Id
  @Column(name = "role_id", nullable = false)
  private Long roleId;

  @Id
  @Column(name = "permission_id", nullable = false)
  private Long permissionId;

  @Column(name = "granted_at", nullable = false, updatable = false)
  private LocalDateTime grantedAt;

  public RolePermission() {
  }

  public RolePermission(Long roleId, Long permissionId) {
    this.roleId = roleId;
    this.permissionId = permissionId;
  }

  public Long getRoleId() {
    return roleId;
  }

  public void setRoleId(Long roleId) {
    this.roleId = roleId;
  }

  public Long getPermissionId() {
    return permissionId;
  }

  public void setPermissionId(Long permissionId) {
    this.permissionId = permissionId;
  }

  public LocalDateTime getGrantedAt() {
    return grantedAt;
  }

  public void setGrantedAt(LocalDateTime grantedAt) {
    this.grantedAt = grantedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof RolePermission other)) {
      return false;
    }
    return Objects.equals(roleId, other.roleId) && Objects.equals(permissionId, other.permissionId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(roleId, permissionId);
  }

  @Override
  public String toString() {
    return "RolePermission{" +
        "roleId=" + roleId +
        ", permissionId=" + permissionId +
        ", grantedAt=" + grantedAt +
        '}';
  }
}
