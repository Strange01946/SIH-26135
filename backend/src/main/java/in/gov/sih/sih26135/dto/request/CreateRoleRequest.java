package in.gov.sih.sih26135.dto.request;

public class CreateRoleRequest {

  private String roleCode;
  private String roleName;
  private String description;
  private Boolean isSystemRole = true;
  private Long lifecycleStatusId;

  public CreateRoleRequest() {
  }

  public CreateRoleRequest(
      String roleCode,
      String roleName,
      String description,
      Boolean isSystemRole,
      Long lifecycleStatusId) {
    this.roleCode = roleCode;
    this.roleName = roleName;
    this.description = description;
    this.isSystemRole = isSystemRole != null ? isSystemRole : true;
    this.lifecycleStatusId = lifecycleStatusId;
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
}
