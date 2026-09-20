package in.gov.sih.sih26135.entity.analytics;

import java.io.Serializable;
import java.util.Objects;

public class SkillGapSummaryId implements Serializable {

  private Long skillId;
  private Long skillGapSeverityId;

  public SkillGapSummaryId() {
  }

  public SkillGapSummaryId(Long skillId, Long skillGapSeverityId) {
    this.skillId = skillId;
    this.skillGapSeverityId = skillGapSeverityId;
  }

  public Long getSkillId() {
    return skillId;
  }

  public void setSkillId(Long skillId) {
    this.skillId = skillId;
  }

  public Long getSkillGapSeverityId() {
    return skillGapSeverityId;
  }

  public void setSkillGapSeverityId(Long skillGapSeverityId) {
    this.skillGapSeverityId = skillGapSeverityId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SkillGapSummaryId that)) {
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
