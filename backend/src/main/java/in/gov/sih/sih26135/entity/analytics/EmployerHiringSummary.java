package in.gov.sih.sih26135.entity.analytics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Objects;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "vw_employer_hiring_summary")
@Immutable
public class EmployerHiringSummary {

  @Id
  @Column(name = "employer_id", nullable = false)
  private Long employerId;

  @Column(name = "placement_count")
  private Long placementCount;

  @Column(name = "trainee_count")
  private Long traineeCount;

  @Column(name = "joined_count")
  private BigDecimal joinedCount;

  @Column(name = "join_rate_pct", precision = 7, scale = 2)
  private BigDecimal joinRatePct;

  @Column(name = "avg_joining_salary", precision = 14, scale = 4)
  private BigDecimal avgJoiningSalary;

  public EmployerHiringSummary() {
  }

  public Long getEmployerId() {
    return employerId;
  }

  public void setEmployerId(Long employerId) {
    this.employerId = employerId;
  }

  public Long getPlacementCount() {
    return placementCount;
  }

  public void setPlacementCount(Long placementCount) {
    this.placementCount = placementCount;
  }

  public Long getTraineeCount() {
    return traineeCount;
  }

  public void setTraineeCount(Long traineeCount) {
    this.traineeCount = traineeCount;
  }

  public BigDecimal getJoinedCount() {
    return joinedCount;
  }

  public void setJoinedCount(BigDecimal joinedCount) {
    this.joinedCount = joinedCount;
  }

  public BigDecimal getJoinRatePct() {
    return joinRatePct;
  }

  public void setJoinRatePct(BigDecimal joinRatePct) {
    this.joinRatePct = joinRatePct;
  }

  public BigDecimal getAvgJoiningSalary() {
    return avgJoiningSalary;
  }

  public void setAvgJoiningSalary(BigDecimal avgJoiningSalary) {
    this.avgJoiningSalary = avgJoiningSalary;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof EmployerHiringSummary that)) {
      return false;
    }
    return Objects.equals(employerId, that.employerId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(employerId);
  }
}
