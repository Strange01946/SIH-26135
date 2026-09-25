package in.gov.sih.sih26135.dto.request;

/**
 * Request payload for creating a new User.
 *
 * <p>Accepts raw password from client which is strictly validated and hashed server-side
 * before database persistence. Raw password is never stored, exposed, or logged.
 */
public class CreateUserRequest {

  private String username;
  private String email;
  private String password;
  private Long userStatusId;
  private Long organizationId;
  private Long departmentId;
  private Long stateId;
  private Long districtId;
  private Long trainingProviderId;
  private Long employerId;

  public CreateUserRequest() {
  }

  public CreateUserRequest(
      String username,
      String email,
      String password,
      Long userStatusId,
      Long organizationId,
      Long departmentId,
      Long stateId,
      Long districtId,
      Long trainingProviderId,
      Long employerId) {
    this.username = username;
    this.email = email;
    this.password = password;
    this.userStatusId = userStatusId;
    this.organizationId = organizationId;
    this.departmentId = departmentId;
    this.stateId = stateId;
    this.districtId = districtId;
    this.trainingProviderId = trainingProviderId;
    this.employerId = employerId;
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

  public Long getUserStatusId() {
    return userStatusId;
  }

  public void setUserStatusId(Long userStatusId) {
    this.userStatusId = userStatusId;
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

  @Override
  public String toString() {
    return "CreateUserRequest{" +
        "username='" + username + '\'' +
        ", email='" + email + '\'' +
        ", userStatusId=" + userStatusId +
        ", organizationId=" + organizationId +
        ", departmentId=" + departmentId +
        ", stateId=" + stateId +
        ", districtId=" + districtId +
        ", trainingProviderId=" + trainingProviderId +
        ", employerId=" + employerId +
        '}';
  }
}
