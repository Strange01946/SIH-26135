package in.gov.sih.sih26135.entity.analytics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Objects;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "vw_trainee_outcome_summary")
@Immutable
public class TraineeOutcomeSummary {

  @Id
  @Column(name = "trainee_id", nullable = false)
  private Long traineeId;

  @Column(name = "state_id", nullable = false)
  private Long stateId;

  @Column(name = "district_id", nullable = false)
  private Long districtId;

  @Column(name = "current_employment_status_id", nullable = false)
  private Long currentEmploymentStatusId;

  @Column(name = "snapshot_employment_status_code", length = 32, nullable = false)
  private String snapshotEmploymentStatusCode;

  @Column(name = "snapshot_is_employed_flag", nullable = false)
  private Boolean snapshotIsEmployedFlag;

  @Column(name = "enrollment_count")
  private Long enrollmentCount;

  @Column(name = "completed_enrollment_count")
  private BigDecimal completedEnrollmentCount;

  @Column(name = "certified_enrollment_count")
  private BigDecimal certifiedEnrollmentCount;

  @Column(name = "placed_enrollment_count")
  private BigDecimal placedEnrollmentCount;

  @Column(name = "employed_enrollment_count")
  private BigDecimal employedEnrollmentCount;

  @Column(name = "employment_spell_count")
  private Long employmentSpellCount;

  @Column(name = "current_employment_count")
  private BigDecimal currentEmploymentCount;

  @Column(name = "current_wage_employment_count")
  private BigDecimal currentWageEmploymentCount;

  @Column(name = "current_self_employment_count")
  private BigDecimal currentSelfEmploymentCount;

  @Column(name = "unemployment_period_count")
  private Long unemploymentPeriodCount;

  @Column(name = "current_unemployment_count")
  private BigDecimal currentUnemploymentCount;

  @Column(name = "current_skill_gap_count")
  private Long currentSkillGapCount;

  @Column(name = "completion_rate_pct", precision = 7, scale = 2)
  private BigDecimal completionRatePct;

  public TraineeOutcomeSummary() {
  }

  public Long getTraineeId() {
    return traineeId;
  }

  public void setTraineeId(Long traineeId) {
    this.traineeId = traineeId;
  }

  public Long getStateId() {
    return stateId;
  }

  public void setStateId(Long stateId) {
    this.stateId = stateId;
  }

  public Long getDistrictId() {
    return districtId;
  }

  public void setDistrictId(Long districtId) {
    this.districtId = districtId;
  }

  public Long getCurrentEmploymentStatusId() {
    return currentEmploymentStatusId;
  }

  public void setCurrentEmploymentStatusId(Long currentEmploymentStatusId) {
    this.currentEmploymentStatusId = currentEmploymentStatusId;
  }

  public String getSnapshotEmploymentStatusCode() {
    return snapshotEmploymentStatusCode;
  }

  public void setSnapshotEmploymentStatusCode(String snapshotEmploymentStatusCode) {
    this.snapshotEmploymentStatusCode = snapshotEmploymentStatusCode;
  }

  public Boolean getSnapshotIsEmployedFlag() {
    return snapshotIsEmployedFlag;
  }

  public void setSnapshotIsEmployedFlag(Boolean snapshotIsEmployedFlag) {
    this.snapshotIsEmployedFlag = snapshotIsEmployedFlag;
  }

  public Long getEnrollmentCount() {
    return enrollmentCount;
  }

  public void setEnrollmentCount(Long enrollmentCount) {
    this.enrollmentCount = enrollmentCount;
  }

  public BigDecimal getCompletedEnrollmentCount() {
    return completedEnrollmentCount;
  }

  public void setCompletedEnrollmentCount(BigDecimal completedEnrollmentCount) {
    this.completedEnrollmentCount = completedEnrollmentCount;
  }

  public BigDecimal getCertifiedEnrollmentCount() {
    return certifiedEnrollmentCount;
  }

  public void setCertifiedEnrollmentCount(BigDecimal certifiedEnrollmentCount) {
    this.certifiedEnrollmentCount = certifiedEnrollmentCount;
  }

  public BigDecimal getPlacedEnrollmentCount() {
    return placedEnrollmentCount;
  }

  public void setPlacedEnrollmentCount(BigDecimal placedEnrollmentCount) {
    this.placedEnrollmentCount = placedEnrollmentCount;
  }

  public BigDecimal getEmployedEnrollmentCount() {
    return employedEnrollmentCount;
  }

  public void setEmployedEnrollmentCount(BigDecimal employedEnrollmentCount) {
    this.employedEnrollmentCount = employedEnrollmentCount;
  }

  public Long getEmploymentSpellCount() {
    return employmentSpellCount;
  }

  public void setEmploymentSpellCount(Long employmentSpellCount) {
    this.employmentSpellCount = employmentSpellCount;
  }

  public BigDecimal getCurrentEmploymentCount() {
    return currentEmploymentCount;
  }

  public void setCurrentEmploymentCount(BigDecimal currentEmploymentCount) {
    this.currentEmploymentCount = currentEmploymentCount;
  }

  public BigDecimal getCurrentWageEmploymentCount() {
    return currentWageEmploymentCount;
  }

  public void setCurrentWageEmploymentCount(BigDecimal currentWageEmploymentCount) {
    this.currentWageEmploymentCount = currentWageEmploymentCount;
  }

  public BigDecimal getCurrentSelfEmploymentCount() {
    return currentSelfEmploymentCount;
  }

  public void setCurrentSelfEmploymentCount(BigDecimal currentSelfEmploymentCount) {
    this.currentSelfEmploymentCount = currentSelfEmploymentCount;
  }

  public Long getUnemploymentPeriodCount() {
    return unemploymentPeriodCount;
  }

  public void setUnemploymentPeriodCount(Long unemploymentPeriodCount) {
    this.unemploymentPeriodCount = unemploymentPeriodCount;
  }

  public BigDecimal getCurrentUnemploymentCount() {
    return currentUnemploymentCount;
  }

  public void setCurrentUnemploymentCount(BigDecimal currentUnemploymentCount) {
    this.currentUnemploymentCount = currentUnemploymentCount;
  }

  public Long getCurrentSkillGapCount() {
    return currentSkillGapCount;
  }

  public void setCurrentSkillGapCount(Long currentSkillGapCount) {
    this.currentSkillGapCount = currentSkillGapCount;
  }

  public BigDecimal getCompletionRatePct() {
    return completionRatePct;
  }

  public void setCompletionRatePct(BigDecimal completionRatePct) {
    this.completionRatePct = completionRatePct;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TraineeOutcomeSummary that)) {
      return false;
    }
    return Objects.equals(traineeId, that.traineeId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(traineeId);
  }
}
