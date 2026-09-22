package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public class JobRoleSkillResponse {

  private Long jobRoleId;
  private String jobRoleCode;
  private String jobRoleName;
  private Long skillId;
  private String skillCode;
  private String skillName;
  private Long requiredSkillLevelId;
  private String requiredSkillLevelCode;
  private String requiredSkillLevelName;
  private Integer requiredSkillLevelRank;
  private Long skillImportanceId;
  private String skillImportanceCode;
  private String skillImportanceName;
  private Integer skillImportanceWeight;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public JobRoleSkillResponse() {
  }

  public JobRoleSkillResponse(
      Long jobRoleId,
      String jobRoleCode,
      String jobRoleName,
      Long skillId,
      String skillCode,
      String skillName,
      Long requiredSkillLevelId,
      String requiredSkillLevelCode,
      String requiredSkillLevelName,
      Integer requiredSkillLevelRank,
      Long skillImportanceId,
      String skillImportanceCode,
      String skillImportanceName,
      Integer skillImportanceWeight,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.jobRoleId = jobRoleId;
    this.jobRoleCode = jobRoleCode;
    this.jobRoleName = jobRoleName;
    this.skillId = skillId;
    this.skillCode = skillCode;
    this.skillName = skillName;
    this.requiredSkillLevelId = requiredSkillLevelId;
    this.requiredSkillLevelCode = requiredSkillLevelCode;
    this.requiredSkillLevelName = requiredSkillLevelName;
    this.requiredSkillLevelRank = requiredSkillLevelRank;
    this.skillImportanceId = skillImportanceId;
    this.skillImportanceCode = skillImportanceCode;
    this.skillImportanceName = skillImportanceName;
    this.skillImportanceWeight = skillImportanceWeight;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public Long getJobRoleId() {
    return jobRoleId;
  }

  public void setJobRoleId(Long jobRoleId) {
    this.jobRoleId = jobRoleId;
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

  public Long getSkillId() {
    return skillId;
  }

  public void setSkillId(Long skillId) {
    this.skillId = skillId;
  }

  public String getSkillCode() {
    return skillCode;
  }

  public void setSkillCode(String skillCode) {
    this.skillCode = skillCode;
  }

  public String getSkillName() {
    return skillName;
  }

  public void setSkillName(String skillName) {
    this.skillName = skillName;
  }

  public Long getRequiredSkillLevelId() {
    return requiredSkillLevelId;
  }

  public void setRequiredSkillLevelId(Long requiredSkillLevelId) {
    this.requiredSkillLevelId = requiredSkillLevelId;
  }

  public String getRequiredSkillLevelCode() {
    return requiredSkillLevelCode;
  }

  public void setRequiredSkillLevelCode(String requiredSkillLevelCode) {
    this.requiredSkillLevelCode = requiredSkillLevelCode;
  }

  public String getRequiredSkillLevelName() {
    return requiredSkillLevelName;
  }

  public void setRequiredSkillLevelName(String requiredSkillLevelName) {
    this.requiredSkillLevelName = requiredSkillLevelName;
  }

  public Integer getRequiredSkillLevelRank() {
    return requiredSkillLevelRank;
  }

  public void setRequiredSkillLevelRank(Integer requiredSkillLevelRank) {
    this.requiredSkillLevelRank = requiredSkillLevelRank;
  }

  public Long getSkillImportanceId() {
    return skillImportanceId;
  }

  public void setSkillImportanceId(Long skillImportanceId) {
    this.skillImportanceId = skillImportanceId;
  }

  public String getSkillImportanceCode() {
    return skillImportanceCode;
  }

  public void setSkillImportanceCode(String skillImportanceCode) {
    this.skillImportanceCode = skillImportanceCode;
  }

  public String getSkillImportanceName() {
    return skillImportanceName;
  }

  public void setSkillImportanceName(String skillImportanceName) {
    this.skillImportanceName = skillImportanceName;
  }

  public Integer getSkillImportanceWeight() {
    return skillImportanceWeight;
  }

  public void setSkillImportanceWeight(Integer skillImportanceWeight) {
    this.skillImportanceWeight = skillImportanceWeight;
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
