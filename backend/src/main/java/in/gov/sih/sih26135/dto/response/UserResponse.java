package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public class UserResponse {

  private Long id;
  private String username;
  private String email;
  private Long userStatusId;
  private String userStatusCode;
  private LocalDateTime lastLoginAt;
  private Integer failedLoginCount;
  private LocalDateTime lockedUntil;
  private Boolean mustChangePassword;
  private Long organizationId;
  private Long departmentId;
  private Long stateId;
  private Long districtId;
  private Long trainingProviderId;
  private Long employerId;
  private Long createdByUserId;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private LocalDateTime deletedAt;

  public UserResponse() {
  }

  public UserResponse(
      Long id,
      String username,
      String email,
      Long userStatusId,
      String userStatusCode,
      LocalDateTime lastLoginAt,
      Integer failedLoginCount,
      LocalDateTime lockedUntil,
      Boolean mustChangePassword,
      Long organizationId,
      Long departmentId,
      Long stateId,
      Long districtId,
      Long trainingProviderId,
      Long employerId,
      Long createdByUserId,
      LocalDateTime createdAt,
      LocalDateTime updatedAt,
      LocalDateTime deletedAt) {
    this.id = id;
    this.username = username;
    this.email = email;
    this.userStatusId = userStatusId;
    this.userStatusCode = userStatusCode;
    this.lastLoginAt = lastLoginAt;
    this.failedLoginCount = failedLoginCount;
    this.lockedUntil = lockedUntil;
    this.mustChangePassword = mustChangePassword;
    this.organizationId = organizationId;
    this.departmentId = departmentId;
    this.stateId = stateId;
    this.districtId = districtId;
    this.trainingProviderId = trainingProviderId;
    this.employerId = employerId;
    this.createdByUserId = createdByUserId;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.deletedAt = deletedAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public Long getUserStatusId() {
    return userStatusId;
  }

  public void setUserStatusId(Long userStatusId) {
    this.userStatusId = userStatusId;
  }

  public String getUserStatusCode() {
    return userStatusCode;
  }

  public void setUserStatusCode(String userStatusCode) {
    this.userStatusCode = userStatusCode;
  }

  public LocalDateTime getLastLoginAt() {
    return lastLoginAt;
  }

  public void setLastLoginAt(LocalDateTime lastLoginAt) {
    this.lastLoginAt = lastLoginAt;
  }

  public Integer getFailedLoginCount() {
    return failedLoginCount;
  }

  public void setFailedLoginCount(Integer failedLoginCount) {
    this.failedLoginCount = failedLoginCount;
  }

  public LocalDateTime getLockedUntil() {
    return lockedUntil;
  }

  public void setLockedUntil(LocalDateTime lockedUntil) {
    this.lockedUntil = lockedUntil;
  }

  public Boolean getMustChangePassword() {
    return mustChangePassword;
  }

  public void setMustChangePassword(Boolean mustChangePassword) {
    this.mustChangePassword = mustChangePassword;
  }

  public Long getOrganizationId() {
    return organizationId;
  }

  public void setOrganizationId(Long organizationId) {
    this.organizationId = organizationId;
  }

  public Long getDepartmentId() {
    return departmentId;
  }

  public void setDepartmentId(Long departmentId) {
    this.departmentId = departmentId;
  }

  public Long getStateId() {
    return stateId;
  }

  public void setStateId(Long stateId) {
    this.stateId = stateId;
  }

  public Long getDistrictId() {
    return districtId;
  }

  public void setDistrictId(Long districtId) {
    this.districtId = districtId;
  }

  public Long getTrainingProviderId() {
    return trainingProviderId;
  }

  public void setTrainingProviderId(Long trainingProviderId) {
    this.trainingProviderId = trainingProviderId;
  }

  public Long getEmployerId() {
    return employerId;
  }

  public void setEmployerId(Long employerId) {
    this.employerId = employerId;
  }

  public Long getCreatedByUserId() {
    return createdByUserId;
  }

  public void setCreatedByUserId(Long createdByUserId) {
    this.createdByUserId = createdByUserId;
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

  public LocalDateTime getDeletedAt() {
    return deletedAt;
  }

  public void setDeletedAt(LocalDateTime deletedAt) {
    this.deletedAt = deletedAt;
  }
}
