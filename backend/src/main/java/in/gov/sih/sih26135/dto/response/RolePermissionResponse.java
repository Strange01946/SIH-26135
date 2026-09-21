package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public class RolePermissionResponse {

  private Long roleId;
  private Long permissionId;
  private String permissionCode;
  private String permissionName;
  private String moduleCode;
  private LocalDateTime grantedAt;

  public RolePermissionResponse() {
  }

  public RolePermissionResponse(
      Long roleId,
      Long permissionId,
      String permissionCode,
      String permissionName,
      String moduleCode,
      LocalDateTime grantedAt) {
    this.roleId = roleId;
    this.permissionId = permissionId;
    this.permissionCode = permissionCode;
    this.permissionName = permissionName;
    this.moduleCode = moduleCode;
    this.grantedAt = grantedAt;
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

  public String getPermissionCode() {
    return permissionCode;
  }

  public void setPermissionCode(String permissionCode) {
    this.permissionCode = permissionCode;
  }

  public String getPermissionName() {
    return permissionName;
  }

  public void setPermissionName(String permissionName) {
    this.permissionName = permissionName;
  }

  public String getModuleCode() {
    return moduleCode;
  }

  public void setModuleCode(String moduleCode) {
    this.moduleCode = moduleCode;
  }

  public LocalDateTime getGrantedAt() {
    return grantedAt;
  }

  public void setGrantedAt(LocalDateTime grantedAt) {
    this.grantedAt = grantedAt;
  }
}
