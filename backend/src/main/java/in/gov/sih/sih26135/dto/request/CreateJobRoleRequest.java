package in.gov.sih.sih26135.dto.request;

public class CreateJobRoleRequest {

  private String jobRoleCode;
  private String jobRoleName;
  private String description;
  private Long sectorId;
  private Long industryId;
  private Long qualificationLevelId;
  private String ncoCode;
  private Long lifecycleStatusId;

  public CreateJobRoleRequest() {
  }

  public CreateJobRoleRequest(String jobRoleCode, String jobRoleName, Long sectorId, Long lifecycleStatusId) {
    this.jobRoleCode = jobRoleCode;
    this.jobRoleName = jobRoleName;
    this.sectorId = sectorId;
    this.lifecycleStatusId = lifecycleStatusId;
  }

  public CreateJobRoleRequest(
      String jobRoleCode,
      String jobRoleName,
      String description,
      Long sectorId,
      Long industryId,
      Long qualificationLevelId,
      String ncoCode,
      Long lifecycleStatusId) {
    this.jobRoleCode = jobRoleCode;
    this.jobRoleName = jobRoleName;
    this.description = description;
    this.sectorId = sectorId;
    this.industryId = industryId;
    this.qualificationLevelId = qualificationLevelId;
    this.ncoCode = ncoCode;
    this.lifecycleStatusId = lifecycleStatusId;
  }

  public String getJobRoleCode() {
    return jobRoleCode;
  }

  public void setJobRoleCode(String jobRoleCode) {
    this.jobRoleCode = jobRoleCode;
  }

  public String getJobRoleName() {
    return jobRoleName;
  }

  public void setJobRoleName(String jobRoleName) {
    this.jobRoleName = jobRoleName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Long getSectorId() {
    return sectorId;
  }

  public void setSectorId(Long sectorId) {
    this.sectorId = sectorId;
  }

  public Long getIndustryId() {
    return industryId;
  }

  public void setIndustryId(Long industryId) {
    this.industryId = industryId;
  }

  public Long getQualificationLevelId() {
    return qualificationLevelId;
  }

  public void setQualificationLevelId(Long qualificationLevelId) {
    this.qualificationLevelId = qualificationLevelId;
  }

  public String getNcoCode() {
    return ncoCode;
  }

  public void setNcoCode(String ncoCode) {
    this.ncoCode = ncoCode;
  }

  public Long getLifecycleStatusId() {
    return lifecycleStatusId;
  }

  public void setLifecycleStatusId(Long lifecycleStatusId) {
    this.lifecycleStatusId = lifecycleStatusId;
  }
}
