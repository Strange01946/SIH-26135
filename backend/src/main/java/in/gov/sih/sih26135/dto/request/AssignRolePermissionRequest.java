package in.gov.sih.sih26135.dto.request;

public class AssignRolePermissionRequest {

  private Long permissionId;

  public AssignRolePermissionRequest() {
  }

  public AssignRolePermissionRequest(Long permissionId) {
    this.permissionId = permissionId;
  }

  public Long getPermissionId() {
    return permissionId;
  }

  public void setPermissionId(Long permissionId) {
    this.permissionId = permissionId;
  }
}
