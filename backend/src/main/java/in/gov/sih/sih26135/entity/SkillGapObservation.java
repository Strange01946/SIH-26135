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
@Table(name = "skill_gap_observations", uniqueConstraints = {
    @UniqueConstraint(name = "uk_skill_gap_observations_number", columnNames = {
        "skill_gap_id", "observation_number"
    })
})
public class SkillGapObservation {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "skill_gap_observation_id", nullable = false, updatable = false)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "skill_gap_id", nullable = false)
  private SkillGap skillGap;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "observation_number", nullable = false, columnDefinition = "SMALLINT UNSIGNED")
  private Integer observationNumber = 1;

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
  @JoinColumn(name = "skill_gap_severity_id", nullable = false)
  private RefSkillGapSeverity skillGapSeverity;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "skill_gap_status_id", nullable = false)
  private RefSkillGapStatus skillGapStatus;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "skill_gap_source_id", nullable = false)
  private RefSkillGapSource skillGapSource;

  @Column(name = "observed_on", nullable = false)
  private LocalDate observedOn;

  @Column(name = "observed_by_user_id")
  private Long observedByUserId;

  @Column(name = "notes", length = 500)
  private String notes;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public SkillGapObservation() {
  }

  public SkillGapObservation(SkillGap skillGap, Integer observationNumber,
      SkillLevel requiredSkillLevel, RefSkillGapSeverity skillGapSeverity,
      RefSkillGapStatus skillGapStatus, RefSkillGapSource skillGapSource, LocalDate observedOn) {
    this.skillGap = skillGap;
    this.observationNumber = observationNumber;
    this.requiredSkillLevel = requiredSkillLevel;
    this.skillGapSeverity = skillGapSeverity;
    this.skillGapStatus = skillGapStatus;
    this.skillGapSource = skillGapSource;
    this.observedOn = observedOn;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public SkillGap getSkillGap() {
    return skillGap;
  }

  public void setSkillGap(SkillGap skillGap) {
    this.skillGap = skillGap;
  }

  public Integer getObservationNumber() {
    return observationNumber;
  }

  public void setObservationNumber(Integer observationNumber) {
    this.observationNumber = observationNumber;
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

  public LocalDate getObservedOn() {
    return observedOn;
  }

  public void setObservedOn(LocalDate observedOn) {
    this.observedOn = observedOn;
  }

  public Long getObservedByUserId() {
    return observedByUserId;
  }

  public void setObservedByUserId(Long observedByUserId) {
    this.observedByUserId = observedByUserId;
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
    if (!(o instanceof SkillGapObservation that)) {
      return false;
    }
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "SkillGapObservation{" +
        "id=" + id +
        ", observationNumber=" + observationNumber +
        ", gapLevelDelta=" + gapLevelDelta +
        ", observedOn=" + observedOn +
        '}';
  }
}
