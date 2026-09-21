package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public class UserRoleResponse {

  private Long userId;
  private Long roleId;
  private String roleCode;
  private String roleName;
  private LocalDateTime assignedAt;
  private Long assignedByUserId;

  public UserRoleResponse() {
  }

  public UserRoleResponse(
      Long userId,
      Long roleId,
      String roleCode,
      String roleName,
      LocalDateTime assignedAt,
      Long assignedByUserId) {
    this.userId = userId;
    this.roleId = roleId;
    this.roleCode = roleCode;
    this.roleName = roleName;
    this.assignedAt = assignedAt;
    this.assignedByUserId = assignedByUserId;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public Long getRoleId() {
    return roleId;
  }

  public void setRoleId(Long roleId) {
    this.roleId = roleId;
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

  public LocalDateTime getAssignedAt() {
    return assignedAt;
  }

  public void setAssignedAt(LocalDateTime assignedAt) {
    this.assignedAt = assignedAt;
  }

  public Long getAssignedByUserId() {
    return assignedByUserId;
  }

  public void setAssignedByUserId(Long assignedByUserId) {
    this.assignedByUserId = assignedByUserId;
  }
}
