package in.gov.sih.sih26135.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "job_posting_skills")
@IdClass(JobPostingSkillId.class)
public class JobPostingSkill {

  @Id
  @Column(name = "job_posting_id", nullable = false)
  private Long jobPostingId;

  @Id
  @Column(name = "skill_id", nullable = false)
  private Long skillId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "job_posting_id", insertable = false, updatable = false)
  private JobPosting jobPosting;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "skill_id", insertable = false, updatable = false)
  private Skill skill;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "required_skill_level_id", nullable = false)
  private SkillLevel requiredSkillLevel;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "skill_importance_id", nullable = false)
  private RefSkillImportance skillImportance;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public JobPostingSkill() {
  }

  public JobPostingSkill(Long jobPostingId, Long skillId, SkillLevel requiredSkillLevel,
      RefSkillImportance skillImportance) {
    this.jobPostingId = jobPostingId;
    this.skillId = skillId;
    this.requiredSkillLevel = requiredSkillLevel;
    this.skillImportance = skillImportance;
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

  public JobPosting getJobPosting() {
    return jobPosting;
  }

  public void setJobPosting(JobPosting jobPosting) {
    this.jobPosting = jobPosting;
    if (jobPosting != null) {
      this.jobPostingId = jobPosting.getId();
    }
  }

  public Skill getSkill() {
    return skill;
  }

  public void setSkill(Skill skill) {
    this.skill = skill;
    if (skill != null) {
      this.skillId = skill.getId();
    }
  }

  public SkillLevel getRequiredSkillLevel() {
    return requiredSkillLevel;
  }

  public void setRequiredSkillLevel(SkillLevel requiredSkillLevel) {
    this.requiredSkillLevel = requiredSkillLevel;
  }

  public RefSkillImportance getSkillImportance() {
    return skillImportance;
  }

  public void setSkillImportance(RefSkillImportance skillImportance) {
    this.skillImportance = skillImportance;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof JobPostingSkill that)) {
      return false;
    }
    return Objects.equals(jobPostingId, that.jobPostingId) && Objects.equals(skillId, that.skillId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(jobPostingId, skillId);
  }

  @Override
  public String toString() {
    return "JobPostingSkill{" +
        "jobPostingId=" + jobPostingId +
        ", skillId=" + skillId +
        '}';
  }
}
