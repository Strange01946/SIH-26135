package in.gov.sih.sih26135.dto.request;

public class CreatePermissionRequest {

  private String permissionCode;
  private String permissionName;
  private String moduleCode;
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
