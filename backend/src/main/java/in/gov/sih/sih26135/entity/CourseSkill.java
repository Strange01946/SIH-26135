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
@Table(name = "course_skills")
@IdClass(CourseSkillId.class)
public class CourseSkill {

  @Id
  @Column(name = "course_id", nullable = false)
  private Long courseId;

  @Id
  @Column(name = "skill_id", nullable = false)
  private Long skillId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "course_id", insertable = false, updatable = false)
  private Course course;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "skill_id", insertable = false, updatable = false)
  private Skill skill;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "taught_skill_level_id", nullable = false)
  private SkillLevel taughtSkillLevel;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "skill_importance_id", nullable = false)
  private RefSkillImportance skillImportance;

  @Column(name = "is_core_skill", nullable = false)
  private Boolean isCoreSkill = false;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public CourseSkill() {
  }

  public CourseSkill(Long courseId, Long skillId, SkillLevel taughtSkillLevel, RefSkillImportance skillImportance, Boolean isCoreSkill) {
    this.courseId = courseId;
    this.skillId = skillId;
    this.taughtSkillLevel = taughtSkillLevel;
    this.skillImportance = skillImportance;
    this.isCoreSkill = isCoreSkill != null ? isCoreSkill : false;
  }

  public CourseSkill(Course course, Skill skill, SkillLevel taughtSkillLevel, RefSkillImportance skillImportance, Boolean isCoreSkill) {
    this.course = course;
    this.courseId = course != null ? course.getId() : null;
    this.skill = skill;
    this.skillId = skill != null ? skill.getId() : null;
    this.taughtSkillLevel = taughtSkillLevel;
    this.skillImportance = skillImportance;
    this.isCoreSkill = isCoreSkill != null ? isCoreSkill : false;
  }

  public Long getCourseId() {
    return courseId;
  }

  public void setCourseId(Long courseId) {
    this.courseId = courseId;
  }

  public Long getSkillId() {
    return skillId;
  }

  public void setSkillId(Long skillId) {
    this.skillId = skillId;
  }

  public Course getCourse() {
    return course;
  }

  public void setCourse(Course course) {
    this.course = course;
    this.courseId = course != null ? course.getId() : null;
  }

  public Skill getSkill() {
    return skill;
  }

  public void setSkill(Skill skill) {
    this.skill = skill;
    this.skillId = skill != null ? skill.getId() : null;
  }

  public SkillLevel getTaughtSkillLevel() {
    return taughtSkillLevel;
  }

  public void setTaughtSkillLevel(SkillLevel taughtSkillLevel) {
    this.taughtSkillLevel = taughtSkillLevel;
  }

  public RefSkillImportance getSkillImportance() {
    return skillImportance;
  }

  public void setSkillImportance(RefSkillImportance skillImportance) {
    this.skillImportance = skillImportance;
  }

  public Boolean getIsCoreSkill() {
    return isCoreSkill;
  }

  public void setIsCoreSkill(Boolean coreSkill) {
    isCoreSkill = coreSkill != null ? coreSkill : false;
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
    if (!(o instanceof CourseSkill other)) {
      return false;
    }
    return Objects.equals(courseId, other.courseId) && Objects.equals(skillId, other.skillId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(courseId, skillId);
  }

  @Override
  public String toString() {
    return "CourseSkill{" +
        "courseId=" + courseId +
        ", skillId=" + skillId +
        ", isCoreSkill=" + isCoreSkill +
        '}';
  }
}
