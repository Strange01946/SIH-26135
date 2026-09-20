package in.gov.sih.sih26135.entity.analytics;

import java.io.Serializable;
import java.util.Objects;

public class FollowupOutcomeSummaryId implements Serializable {

  private String followupTypeCode;
  private Integer followupOffsetMonths;

  public FollowupOutcomeSummaryId() {
  }

  public FollowupOutcomeSummaryId(String followupTypeCode, Integer followupOffsetMonths) {
    this.followupTypeCode = followupTypeCode;
    this.followupOffsetMonths = followupOffsetMonths;
  }

  public String getFollowupTypeCode() {
    return followupTypeCode;
  }

  public void setFollowupTypeCode(String followupTypeCode) {
    this.followupTypeCode = followupTypeCode;
  }

  public Integer getFollowupOffsetMonths() {
    return followupOffsetMonths;
  }

  public void setFollowupOffsetMonths(Integer followupOffsetMonths) {
    this.followupOffsetMonths = followupOffsetMonths;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof FollowupOutcomeSummaryId that)) {
      return false;
    }
    return Objects.equals(followupTypeCode, that.followupTypeCode)
        && Objects.equals(followupOffsetMonths, that.followupOffsetMonths);
  }

  @Override
  public int hashCode() {
    return Objects.hash(followupTypeCode, followupOffsetMonths);
  }
}
