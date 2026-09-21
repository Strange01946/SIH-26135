package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public class PermissionResponse {

  private Long id;
  private String permissionCode;
  private String permissionName;
  private String moduleCode;
  private String description;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public PermissionResponse() {
  }

  public PermissionResponse(
      Long id,
      String permissionCode,
      String permissionName,
      String moduleCode,
      String description,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.permissionCode = permissionCode;
    this.permissionName = permissionName;
    this.moduleCode = moduleCode;
    this.description = description;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
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

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
