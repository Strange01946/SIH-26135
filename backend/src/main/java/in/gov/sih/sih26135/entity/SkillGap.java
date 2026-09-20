package in.gov.sih.sih26135.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "skill_gaps", uniqueConstraints = {
    @UniqueConstraint(name = "uk_skill_gaps_number", columnNames = {"skill_gap_number"}),
    @UniqueConstraint(name = "uk_skill_gaps_assessment_skill", columnNames = {
        "skill_gap_assessment_id", "skill_id"
    }),
    @UniqueConstraint(name = "uk_skill_gaps_current", columnNames = {
        "trainee_id", "skill_id", "current_gap_key"
    })
})
public class SkillGap {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "skill_gap_id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "skill_gap_number", length = 32, nullable = false, unique = true)
  private String skillGapNumber;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "skill_gap_assessment_id", nullable = false)
  private SkillGapAssessment skillGapAssessment;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "trainee_id", nullable = false)
  private Trainee trainee;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "skill_id", nullable = false)
  private Skill skill;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "observed_skill_level_id")
  private SkillLevel observedSkillLevel;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "required_skill_level_id", nullable = false)
  private SkillLevel requiredSkillLevel;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "target_skill_level_id")
  private SkillLevel targetSkillLevel;

  @JdbcTypeCode(SqlTypes.TINYINT)
  @Column(name = "gap_level_delta")
  private Integer gapLevelDelta;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "skill_importance_id")
  private RefSkillImportance skillImportance;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "skill_gap_severity_id", nullable = false)
  private RefSkillGapSeverity skillGapSeverity;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "skill_gap_status_id", nullable = false)
  private RefSkillGapStatus skillGapStatus;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "skill_gap_source_id", nullable = false)
  private RefSkillGapSource skillGapSource;

  @Column(name = "is_current", nullable = false)
  private Boolean isCurrent = true;

  @JdbcTypeCode(SqlTypes.TINYINT)
  @Column(name = "current_gap_key", insertable = false, updatable = false)
  private Integer currentGapKey;

  @Column(name = "identified_on", nullable = false)
  private LocalDate identifiedOn;

  @Column(name = "resolved_on")
  private LocalDate resolvedOn;

  @Column(name = "notes", length = 500)
  private String notes;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public SkillGap() {
  }

  public SkillGap(String skillGapNumber, SkillGapAssessment skillGapAssessment, Trainee trainee,
      Skill skill, SkillLevel requiredSkillLevel, RefSkillGapSeverity skillGapSeverity,
      RefSkillGapStatus skillGapStatus, RefSkillGapSource skillGapSource, LocalDate identifiedOn) {
    this.skillGapNumber = skillGapNumber;
    this.skillGapAssessment = skillGapAssessment;
    this.trainee = trainee;
    this.skill = skill;
    this.requiredSkillLevel = requiredSkillLevel;
    this.skillGapSeverity = skillGapSeverity;
    this.skillGapStatus = skillGapStatus;
    this.skillGapSource = skillGapSource;
    this.identifiedOn = identifiedOn;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getSkillGapNumber() {
    return skillGapNumber;
  }

  public void setSkillGapNumber(String skillGapNumber) {
    this.skillGapNumber = skillGapNumber;
  }

  public SkillGapAssessment getSkillGapAssessment() {
    return skillGapAssessment;
  }

  public void setSkillGapAssessment(SkillGapAssessment skillGapAssessment) {
    this.skillGapAssessment = skillGapAssessment;
  }

  public Trainee getTrainee() {
    return trainee;
  }

  public void setTrainee(Trainee trainee) {
    this.trainee = trainee;
  }

  public Skill getSkill() {
    return skill;
  }

  public void setSkill(Skill skill) {
    this.skill = skill;
  }

  public SkillLevel getObservedSkillLevel() {
    return observedSkillLevel;
  }

  public void setObservedSkillLevel(SkillLevel observedSkillLevel) {
    this.observedSkillLevel = observedSkillLevel;
  }

  public SkillLevel getRequiredSkillLevel() {
    return requiredSkillLevel;
  }

  public void setRequiredSkillLevel(SkillLevel requiredSkillLevel) {
    this.requiredSkillLevel = requiredSkillLevel;
  }

  public SkillLevel getTargetSkillLevel() {
    return targetSkillLevel;
  }

  public void setTargetSkillLevel(SkillLevel targetSkillLevel) {
    this.targetSkillLevel = targetSkillLevel;
  }

  public Integer getGapLevelDelta() {
    return gapLevelDelta;
  }

  public void setGapLevelDelta(Integer gapLevelDelta) {
    this.gapLevelDelta = gapLevelDelta;
  }

  public RefSkillImportance getSkillImportance() {
    return skillImportance;
  }

  public void setSkillImportance(RefSkillImportance skillImportance) {
    this.skillImportance = skillImportance;
  }

  public RefSkillGapSeverity getSkillGapSeverity() {
    return skillGapSeverity;
  }

  public void setSkillGapSeverity(RefSkillGapSeverity skillGapSeverity) {
    this.skillGapSeverity = skillGapSeverity;
  }

  public RefSkillGapStatus getSkillGapStatus() {
    return skillGapStatus;
  }

  public void setSkillGapStatus(RefSkillGapStatus skillGapStatus) {
    this.skillGapStatus = skillGapStatus;
  }

  public RefSkillGapSource getSkillGapSource() {
    return skillGapSource;
  }

  public void setSkillGapSource(RefSkillGapSource skillGapSource) {
    this.skillGapSource = skillGapSource;
  }

  public Boolean getIsCurrent() {
    return isCurrent;
  }

  public void setIsCurrent(Boolean isCurrent) {
    this.isCurrent = isCurrent;
  }

  public Integer getCurrentGapKey() {
    return currentGapKey;
  }

  public void setCurrentGapKey(Integer currentGapKey) {
    this.currentGapKey = currentGapKey;
  }

  public LocalDate getIdentifiedOn() {
    return identifiedOn;
  }

  public void setIdentifiedOn(LocalDate identifiedOn) {
    this.identifiedOn = identifiedOn;
  }

  public LocalDate getResolvedOn() {
    return resolvedOn;
  }

  public void setResolvedOn(LocalDate resolvedOn) {
    this.resolvedOn = resolvedOn;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
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
    if (!(o instanceof SkillGap that)) {
      return false;
    }
    return Objects.equals(skillGapNumber, that.skillGapNumber);
  }

  @Override
  public int hashCode() {
    return Objects.hash(skillGapNumber);
  }

  @Override
  public String toString() {
    return "SkillGap{" +
        "id=" + id +
        ", skillGapNumber='" + skillGapNumber + '\'' +
        ", gapLevelDelta=" + gapLevelDelta +
        ", isCurrent=" + isCurrent +
        ", identifiedOn=" + identifiedOn +
        ", resolvedOn=" + resolvedOn +
        '}';
  }
}
