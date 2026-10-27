package in.gov.sih.sih26135.dto.request;
import jakarta.validation.constraints.*;

public class CreateRoleRequest {

  @NotBlank(message = "roleCode is required")
  @Size(max = 64, message = "roleCode cannot exceed 64 characters")
  private String roleCode;
  @NotBlank(message = "roleName is required")
  @Size(max = 100, message = "roleName cannot exceed 100 characters")
  private String roleName;
  @Size(max = 500, message = "description cannot exceed 500 characters")
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
