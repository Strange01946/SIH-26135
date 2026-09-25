package in.gov.sih.sih26135.dto.auth;

import java.util.List;

/**
 * Profile response payload representing the currently authenticated user for the GET /me endpoint.
 */
public class AuthenticatedUserResponse {

  private Long id;
  private String username;
  private String email;
  private List<String> authorities;
  private Long organizationId;
  private Long departmentId;
  private Long stateId;
  private Long districtId;
  private Long trainingProviderId;
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
