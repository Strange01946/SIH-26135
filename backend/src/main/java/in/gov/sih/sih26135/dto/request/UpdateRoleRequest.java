package in.gov.sih.sih26135.dto.request;

public class UpdateRoleRequest {

  private String roleName;
  private String description;
  private Boolean isSystemRole;
  private Long lifecycleStatusId;

  public UpdateRoleRequest() {
  }

  public UpdateRoleRequest(String roleName, String description, Boolean isSystemRole, Long lifecycleStatusId) {
    this.roleName = roleName;
    this.description = description;
    this.isSystemRole = isSystemRole;
    this.lifecycleStatusId = lifecycleStatusId;
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
