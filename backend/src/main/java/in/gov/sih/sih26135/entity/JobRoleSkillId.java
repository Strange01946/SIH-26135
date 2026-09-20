package in.gov.sih.sih26135.entity;

import java.io.Serializable;
import java.util.Objects;

public class JobRoleSkillId implements Serializable {

  private Long jobRoleId;
  private Long skillId;

  public JobRoleSkillId() {
  }

  public JobRoleSkillId(Long jobRoleId, Long skillId) {
    this.jobRoleId = jobRoleId;
    this.skillId = skillId;
  }

  public Long getJobRoleId() {
    return jobRoleId;
  }

  public void setJobRoleId(Long jobRoleId) {
    this.jobRoleId = jobRoleId;
  }

  public Long getSkillId() {
    return skillId;
  }

  public void setSkillId(Long skillId) {
    this.skillId = skillId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof JobRoleSkillId that)) {
      return false;
    }
    return Objects.equals(jobRoleId, that.jobRoleId) && Objects.equals(skillId, that.skillId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(jobRoleId, skillId);
  }
}
