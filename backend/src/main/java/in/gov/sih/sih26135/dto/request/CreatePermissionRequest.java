package in.gov.sih.sih26135.dto.request;
import jakarta.validation.constraints.*;

public class CreatePermissionRequest {

  @NotBlank(message = "permissionCode is required")
  @Size(max = 100, message = "permissionCode cannot exceed 100 characters")
  private String permissionCode;
  @Size(max = 150, message = "permissionName cannot exceed 150 characters")
  private String permissionName;
  @NotBlank(message = "moduleCode is required")
  @Size(max = 64, message = "moduleCode cannot exceed 64 characters")
  private String moduleCode;
  @Size(max = 500, message = "description cannot exceed 500 characters")
  private String description;

  public CreatePermissionRequest() {
  }

  public CreatePermissionRequest(String permissionCode, String permissionName, String moduleCode, String description) {
    this.permissionCode = permissionCode;
    this.permissionName = permissionName;
    this.moduleCode = moduleCode;
    this.description = description;
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

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}
