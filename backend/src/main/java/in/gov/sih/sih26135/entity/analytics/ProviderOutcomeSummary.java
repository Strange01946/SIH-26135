package in.gov.sih.sih26135.entity.analytics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Objects;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "vw_provider_outcome_summary")
@Immutable
public class ProviderOutcomeSummary {

  @Id
  @Column(name = "provider_id", nullable = false)
  private Long providerId;

  @Column(name = "enrollment_count")
  private Long enrollmentCount;

  @Column(name = "trainee_count")
  private Long traineeCount;

  @Column(name = "completed_count")
  private BigDecimal completedCount;

  @Column(name = "certified_count")
  private BigDecimal certifiedCount;

  @Column(name = "joined_placement_count")
  private BigDecimal joinedPlacementCount;

  @Column(name = "employed_count")
  private BigDecimal employedCount;

  @Column(name = "completion_rate_pct", precision = 7, scale = 2)
  private BigDecimal completionRatePct;

  @Column(name = "certification_rate_pct", precision = 7, scale = 2)
  private BigDecimal certificationRatePct;

  @Column(name = "placement_rate_pct", precision = 7, scale = 2)
  private BigDecimal placementRatePct;

  @Column(name = "employment_rate_pct", precision = 7, scale = 2)
  private BigDecimal employmentRatePct;

  public ProviderOutcomeSummary() {
  }

  public Long getProviderId() {
    return providerId;
  }

  public void setProviderId(Long providerId) {
    this.providerId = providerId;
  }

  public Long getEnrollmentCount() {
    return enrollmentCount;
  }

  public void setEnrollmentCount(Long enrollmentCount) {
    this.enrollmentCount = enrollmentCount;
  }

  public Long getTraineeCount() {
    return traineeCount;
  }

  public void setTraineeCount(Long traineeCount) {
    this.traineeCount = traineeCount;
  }

  public BigDecimal getCompletedCount() {
    return completedCount;
  }

  public void setCompletedCount(BigDecimal completedCount) {
    this.completedCount = completedCount;
  }

  public BigDecimal getCertifiedCount() {
    return certifiedCount;
  }

  public void setCertifiedCount(BigDecimal certifiedCount) {
    this.certifiedCount = certifiedCount;
  }

  public BigDecimal getJoinedPlacementCount() {
    return joinedPlacementCount;
  }

  public void setJoinedPlacementCount(BigDecimal joinedPlacementCount) {
    this.joinedPlacementCount = joinedPlacementCount;
  }

  public BigDecimal getEmployedCount() {
    return employedCount;
  }

  public void setEmployedCount(BigDecimal employedCount) {
    this.employedCount = employedCount;
  }

  public BigDecimal getCompletionRatePct() {
    return completionRatePct;
  }

  public void setCompletionRatePct(BigDecimal completionRatePct) {
    this.completionRatePct = completionRatePct;
  }

  public BigDecimal getCertificationRatePct() {
    return certificationRatePct;
  }

  public void setCertificationRatePct(BigDecimal certificationRatePct) {
    this.certificationRatePct = certificationRatePct;
  }

  public BigDecimal getPlacementRatePct() {
    return placementRatePct;
  }

  public void setPlacementRatePct(BigDecimal placementRatePct) {
    this.placementRatePct = placementRatePct;
  }

  public BigDecimal getEmploymentRatePct() {
    return employmentRatePct;
  }

  public void setEmploymentRatePct(BigDecimal employmentRatePct) {
    this.employmentRatePct = employmentRatePct;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ProviderOutcomeSummary that)) {
      return false;
    }
    return Objects.equals(providerId, that.providerId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(providerId);
  }
}
