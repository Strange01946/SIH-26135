package in.gov.sih.sih26135.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Profile response payload representing the currently authenticated user for the GET /me endpoint.
 */
@Schema(description = "Authenticated user profile and multi-tenant tenancy scope")
public class AuthenticatedUserResponse {

  @Schema(description = "User identifier", example = "1")
  private Long id;

  @Schema(description = "Username", example = "admin")
  private String username;

  @Schema(description = "Email address", example = "admin@example.org")
  private String email;

  @Schema(description = "List of assigned roles and authorities", example = "[\"ROLE_super_admin\"]")
  private List<String> authorities;

  @Schema(description = "Associated organization ID if scoped", example = "1")
  private Long organizationId;

  @Schema(description = "Associated department ID if scoped", example = "1")
  private Long departmentId;

  @Schema(description = "Associated state ID for state tenancy", example = "10")
  private Long stateId;

  @Schema(description = "Associated district ID for district tenancy", example = "101")
  private Long districtId;

  @Schema(description = "Associated training provider ID for TP tenancy", example = "201")
  private Long trainingProviderId;

  @Schema(description = "Associated employer ID for employer tenancy", example = "301")
  private Long employerId;

  public AuthenticatedUserResponse() {
  }

  public AuthenticatedUserResponse(
      Long id,
      String username,
      String email,
      List<String> authorities,
      Long organizationId,
      Long departmentId,
      Long stateId,
      Long districtId,
      Long trainingProviderId,
      Long employerId) {
    this.id = id;
    this.username = username;
    this.email = email;
    this.authorities = authorities;
    this.organizationId = organizationId;
    this.departmentId = departmentId;
    this.stateId = stateId;
    this.districtId = districtId;
    this.trainingProviderId = trainingProviderId;
    this.employerId = employerId;
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

  public List<String> getAuthorities() {
    return authorities;
  }

  public void setAuthorities(List<String> authorities) {
    this.authorities = authorities;
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
}
