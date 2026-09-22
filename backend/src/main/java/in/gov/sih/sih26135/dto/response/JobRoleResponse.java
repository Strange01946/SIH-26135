package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public class JobRoleResponse {

  private Long id;
  private String jobRoleCode;
  private String jobRoleName;
  private String description;
  private Long sectorId;
  private String sectorCode;
  private String sectorName;
  private Long industryId;
  private String industryCode;
  private String industryName;
  private Long qualificationLevelId;
  private String qualificationLevelCode;
  private String qualificationLevelName;
  private String ncoCode;
  private Long lifecycleStatusId;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private LocalDateTime deletedAt;

  public JobRoleResponse() {
  }

  public JobRoleResponse(
      Long id,
      String jobRoleCode,
      String jobRoleName,
      String description,
      Long sectorId,
      String sectorCode,
      String sectorName,
      Long industryId,
      String industryCode,
      String industryName,
      Long qualificationLevelId,
      String qualificationLevelCode,
      String qualificationLevelName,
      String ncoCode,
      Long lifecycleStatusId,
      LocalDateTime createdAt,
      LocalDateTime updatedAt,
      LocalDateTime deletedAt) {
    this.id = id;
    this.jobRoleCode = jobRoleCode;
    this.jobRoleName = jobRoleName;
    this.description = description;
    this.sectorId = sectorId;
    this.sectorCode = sectorCode;
    this.sectorName = sectorName;
    this.industryId = industryId;
    this.industryCode = industryCode;
    this.industryName = industryName;
    this.qualificationLevelId = qualificationLevelId;
    this.qualificationLevelCode = qualificationLevelCode;
    this.qualificationLevelName = qualificationLevelName;
    this.ncoCode = ncoCode;
    this.lifecycleStatusId = lifecycleStatusId;
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

  public String getSectorCode() {
    return sectorCode;
  }

  public void setSectorCode(String sectorCode) {
    this.sectorCode = sectorCode;
  }

  public String getSectorName() {
    return sectorName;
  }

  public void setSectorName(String sectorName) {
    this.sectorName = sectorName;
  }

  public Long getIndustryId() {
    return industryId;
  }

  public void setIndustryId(Long industryId) {
    this.industryId = industryId;
  }

  public String getIndustryCode() {
    return industryCode;
  }

  public void setIndustryCode(String industryCode) {
    this.industryCode = industryCode;
  }

  public String getIndustryName() {
    return industryName;
  }

  public void setIndustryName(String industryName) {
    this.industryName = industryName;
  }

  public Long getQualificationLevelId() {
    return qualificationLevelId;
  }

  public void setQualificationLevelId(Long qualificationLevelId) {
    this.qualificationLevelId = qualificationLevelId;
  }

  public String getQualificationLevelCode() {
    return qualificationLevelCode;
  }

  public void setQualificationLevelCode(String qualificationLevelCode) {
    this.qualificationLevelCode = qualificationLevelCode;
  }

  public String getQualificationLevelName() {
    return qualificationLevelName;
  }

  public void setQualificationLevelName(String qualificationLevelName) {
    this.qualificationLevelName = qualificationLevelName;
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
