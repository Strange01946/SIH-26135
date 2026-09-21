package in.gov.sih.sih26135.dto.request;

public class CreateUserRequest {

  private String username;
  private String email;
  private String passwordHash;
  private String passwordAlgo = "argon2id";
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
      String passwordHash,
      String passwordAlgo,
      Long userStatusId,
      Long organizationId,
      Long departmentId,
      Long stateId,
      Long districtId,
      Long trainingProviderId,
      Long employerId) {
    this.username = username;
    this.email = email;
    this.passwordHash = passwordHash;
    this.passwordAlgo = passwordAlgo != null ? passwordAlgo : "argon2id";
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

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public String getPasswordAlgo() {
    return passwordAlgo;
  }

  public void setPasswordAlgo(String passwordAlgo) {
    this.passwordAlgo = passwordAlgo;
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
}
