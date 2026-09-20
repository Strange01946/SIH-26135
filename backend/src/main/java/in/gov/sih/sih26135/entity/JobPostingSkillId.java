package in.gov.sih.sih26135.entity;

import java.io.Serializable;
import java.util.Objects;

public class JobPostingSkillId implements Serializable {

  private Long jobPostingId;
  private Long skillId;

  public JobPostingSkillId() {
  }

  public JobPostingSkillId(Long jobPostingId, Long skillId) {
    this.jobPostingId = jobPostingId;
    this.skillId = skillId;
  }

  public Long getJobPostingId() {
    return jobPostingId;
  }

  public void setJobPostingId(Long jobPostingId) {
    this.jobPostingId = jobPostingId;
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
    if (!(o instanceof JobPostingSkillId that)) {
      return false;
    }
    return Objects.equals(jobPostingId, that.jobPostingId) && Objects.equals(skillId, that.skillId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(jobPostingId, skillId);
  }
}
