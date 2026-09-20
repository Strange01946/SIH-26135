package in.gov.sih.sih26135.entity.analytics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Objects;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "vw_unemployment_reason_summary")
@Immutable
public class UnemploymentReasonSummary {

  @Id
  @Column(name = "unemployment_reason_id", nullable = false)
  private Long unemploymentReasonId;

  @Column(name = "unemployment_reason_code", length = 32, nullable = false)
  private String unemploymentReasonCode;

  @Column(name = "period_count")
  private Long periodCount;

  @Column(name = "trainee_count")
  private Long traineeCount;

  @Column(name = "current_period_count")
  private BigDecimal currentPeriodCount;

  @Column(name = "reemployed_count")
  private BigDecimal reemployedCount;

  @Column(name = "avg_duration_days", precision = 14, scale = 4)
  private BigDecimal avgDurationDays;

  public UnemploymentReasonSummary() {
  }

  public Long getUnemploymentReasonId() {
    return unemploymentReasonId;
  }

  public void setUnemploymentReasonId(Long unemploymentReasonId) {
    this.unemploymentReasonId = unemploymentReasonId;
  }

  public String getUnemploymentReasonCode() {
    return unemploymentReasonCode;
  }

  public void setUnemploymentReasonCode(String unemploymentReasonCode) {
    this.unemploymentReasonCode = unemploymentReasonCode;
  }

  public Long getPeriodCount() {
    return periodCount;
  }

  public void setPeriodCount(Long periodCount) {
    this.periodCount = periodCount;
  }

  public Long getTraineeCount() {
    return traineeCount;
  }

  public void setTraineeCount(Long traineeCount) {
    this.traineeCount = traineeCount;
  }

  public BigDecimal getCurrentPeriodCount() {
    return currentPeriodCount;
  }

  public void setCurrentPeriodCount(BigDecimal currentPeriodCount) {
    this.currentPeriodCount = currentPeriodCount;
  }

  public BigDecimal getReemployedCount() {
    return reemployedCount;
  }

  public void setReemployedCount(BigDecimal reemployedCount) {
    this.reemployedCount = reemployedCount;
  }

  public BigDecimal getAvgDurationDays() {
    return avgDurationDays;
  }

  public void setAvgDurationDays(BigDecimal avgDurationDays) {
    this.avgDurationDays = avgDurationDays;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof UnemploymentReasonSummary that)) {
      return false;
    }
    return Objects.equals(unemploymentReasonId, that.unemploymentReasonId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(unemploymentReasonId);
  }
}
