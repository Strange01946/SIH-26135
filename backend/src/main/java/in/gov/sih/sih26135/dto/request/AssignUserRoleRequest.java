package in.gov.sih.sih26135.dto.request;
import jakarta.validation.constraints.*;

public class AssignUserRoleRequest {

  @NotNull(message = "Role ID is required")
  private Long roleId;

  public AssignUserRoleRequest() {
  }

  public AssignUserRoleRequest(Long roleId) {
    this.roleId = roleId;
  }

  public Long getRoleId() {
    return roleId;
  }

  public void setRoleId(Long roleId) {
    this.roleId = roleId;
  }
}
