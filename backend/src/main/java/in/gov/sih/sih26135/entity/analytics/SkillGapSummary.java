package in.gov.sih.sih26135.entity.analytics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Objects;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "vw_skill_gap_summary")
@IdClass(SkillGapSummaryId.class)
@Immutable
public class SkillGapSummary {

  @Id
  @Column(name = "skill_id", nullable = false)
  private Long skillId;

  @Column(name = "skill_code", length = 32, nullable = false)
  private String skillCode;

  @Id
  @Column(name = "skill_gap_severity_id", nullable = false)
  private Long skillGapSeverityId;

  @Column(name = "severity_code", length = 32, nullable = false)
  private String severityCode;

  @Column(name = "gap_count")
  private Long gapCount;

  @Column(name = "trainee_count")
  private Long traineeCount;

  @Column(name = "current_gap_count")
  private BigDecimal currentGapCount;

  @Column(name = "avg_gap_level_delta", precision = 14, scale = 4)
  private BigDecimal avgGapLevelDelta;

  public SkillGapSummary() {
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

  public Long getSkillGapSeverityId() {
    return skillGapSeverityId;
  }

  public void setSkillGapSeverityId(Long skillGapSeverityId) {
    this.skillGapSeverityId = skillGapSeverityId;
  }

  public String getSeverityCode() {
    return severityCode;
  }

  public void setSeverityCode(String severityCode) {
    this.severityCode = severityCode;
  }

  public Long getGapCount() {
    return gapCount;
  }

  public void setGapCount(Long gapCount) {
    this.gapCount = gapCount;
  }

  public Long getTraineeCount() {
    return traineeCount;
  }

  public void setTraineeCount(Long traineeCount) {
    this.traineeCount = traineeCount;
  }

  public BigDecimal getCurrentGapCount() {
    return currentGapCount;
  }

  public void setCurrentGapCount(BigDecimal currentGapCount) {
    this.currentGapCount = currentGapCount;
  }

  public BigDecimal getAvgGapLevelDelta() {
    return avgGapLevelDelta;
  }

  public void setAvgGapLevelDelta(BigDecimal avgGapLevelDelta) {
    this.avgGapLevelDelta = avgGapLevelDelta;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SkillGapSummary that)) {
      return false;
    }
    return Objects.equals(skillId, that.skillId)
        && Objects.equals(skillGapSeverityId, that.skillGapSeverityId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(skillId, skillGapSeverityId);
  }
}
