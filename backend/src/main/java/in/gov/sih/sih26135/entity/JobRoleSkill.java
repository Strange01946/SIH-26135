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
@Table(name = "job_role_skills")
@IdClass(JobRoleSkillId.class)
public class JobRoleSkill {

  @Id
  @Column(name = "job_role_id", nullable = false)
  private Long jobRoleId;

  @Id
  @Column(name = "skill_id", nullable = false)
  private Long skillId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "job_role_id", insertable = false, updatable = false)
  private JobRole jobRole;

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

  public JobRoleSkill() {
  }

  public JobRoleSkill(Long jobRoleId, Long skillId, SkillLevel requiredSkillLevel, RefSkillImportance skillImportance) {
    this.jobRoleId = jobRoleId;
    this.skillId = skillId;
    this.requiredSkillLevel = requiredSkillLevel;
    this.skillImportance = skillImportance;
  }

  public JobRoleSkill(JobRole jobRole, Skill skill, SkillLevel requiredSkillLevel, RefSkillImportance skillImportance) {
    this.jobRole = jobRole;
    this.jobRoleId = jobRole != null ? jobRole.getId() : null;
    this.skill = skill;
    this.skillId = skill != null ? skill.getId() : null;
    this.requiredSkillLevel = requiredSkillLevel;
    this.skillImportance = skillImportance;
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

  public JobRole getJobRole() {
    return jobRole;
  }

  public void setJobRole(JobRole jobRole) {
    this.jobRole = jobRole;
    this.jobRoleId = jobRole != null ? jobRole.getId() : null;
  }

  public Skill getSkill() {
    return skill;
  }

  public void setSkill(Skill skill) {
    this.skill = skill;
    this.skillId = skill != null ? skill.getId() : null;
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
    if (!(o instanceof JobRoleSkill other)) {
      return false;
    }
    return Objects.equals(jobRoleId, other.jobRoleId) && Objects.equals(skillId, other.skillId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(jobRoleId, skillId);
  }

  @Override
  public String toString() {
    return "JobRoleSkill{" +
        "jobRoleId=" + jobRoleId +
        ", skillId=" + skillId +
        '}';
  }
}
