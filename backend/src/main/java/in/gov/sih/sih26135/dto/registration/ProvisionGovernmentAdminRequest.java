package in.gov.sih.sih26135.dto.registration;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ProvisionGovernmentAdminRequest {

  @NotBlank(message = "Username is required")
  @Size(min = 3, max = 80, message = "Username must be between 3 and 80 characters")
  private String username;

  @NotBlank(message = "Email is required")
  @Email(message = "Email must be a valid email address")
  private String email;

  @NotBlank(message = "Password is required")
  @Size(min = 8, max = 100, message = "Password must be at least 8 characters")
  private String password;

  private String fullName;

  @NotBlank(message = "Role code is required")
  @Pattern(
      regexp = "^(government_admin|government_officer|district_officer|analyst)$",
      message = "Role code must be one of: government_admin, government_officer, district_officer, analyst"
  )
  private String roleCode;

  private Long organizationId;

  private Long stateId;

  private Long districtId;

  public ProvisionGovernmentAdminRequest() {
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

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public String getRoleCode() {
    return roleCode;
  }

  public void setRoleCode(String roleCode) {
    this.roleCode = roleCode;
  }

  public Long getOrganizationId() {
    return organizationId;
  }

  public void setOrganizationId(Long organizationId) {
    this.organizationId = organizationId;
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
}
