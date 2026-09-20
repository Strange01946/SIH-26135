package in.gov.sih.sih26135.entity.analytics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Objects;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "vw_followup_outcome_summary")
@IdClass(FollowupOutcomeSummaryId.class)
@Immutable
public class FollowupOutcomeSummary {

  @Id
  @Column(name = "followup_type_code", length = 32, nullable = false)
  private String followupTypeCode;

  @Id
  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "followup_offset_months", nullable = false, columnDefinition = "SMALLINT UNSIGNED")
  private Integer followupOffsetMonths;

  @Column(name = "task_count")
  private Long taskCount;

  @Column(name = "trainee_count")
  private Long traineeCount;

  @Column(name = "success_count")
  private BigDecimal successCount;

  @Column(name = "no_response_count")
  private BigDecimal noResponseCount;

  @Column(name = "unreachable_count")
  private BigDecimal unreachableCount;

  @Column(name = "success_rate_pct", precision = 7, scale = 2)
  private BigDecimal successRatePct;

  public FollowupOutcomeSummary() {
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

  public Long getTaskCount() {
    return taskCount;
  }

  public void setTaskCount(Long taskCount) {
    this.taskCount = taskCount;
  }

  public Long getTraineeCount() {
    return traineeCount;
  }

  public void setTraineeCount(Long traineeCount) {
    this.traineeCount = traineeCount;
  }

  public BigDecimal getSuccessCount() {
    return successCount;
  }

  public void setSuccessCount(BigDecimal successCount) {
    this.successCount = successCount;
  }

  public BigDecimal getNoResponseCount() {
    return noResponseCount;
  }

  public void setNoResponseCount(BigDecimal noResponseCount) {
    this.noResponseCount = noResponseCount;
  }

  public BigDecimal getUnreachableCount() {
    return unreachableCount;
  }

  public void setUnreachableCount(BigDecimal unreachableCount) {
    this.unreachableCount = unreachableCount;
  }

  public BigDecimal getSuccessRatePct() {
    return successRatePct;
  }

  public void setSuccessRatePct(BigDecimal successRatePct) {
    this.successRatePct = successRatePct;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof FollowupOutcomeSummary that)) {
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
