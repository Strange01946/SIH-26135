package in.gov.sih.sih26135.entity.analytics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Objects;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "vw_program_outcome_summary")
@Immutable
public class ProgramOutcomeSummary {

  @Id
  @Column(name = "program_id", nullable = false)
  private Long programId;

  @Column(name = "scheme_id", nullable = false)
  private Long schemeId;

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

  @Column(name = "self_employment_spell_count")
  private BigDecimal selfEmploymentSpellCount;

  @Column(name = "apprenticeship_spell_count")
  private BigDecimal apprenticeshipSpellCount;

  @Column(name = "completion_rate_pct", precision = 7, scale = 2)
  private BigDecimal completionRatePct;

  @Column(name = "certification_rate_pct", precision = 7, scale = 2)
  private BigDecimal certificationRatePct;

  @Column(name = "placement_rate_pct", precision = 7, scale = 2)
  private BigDecimal placementRatePct;

  @Column(name = "employment_rate_pct", precision = 7, scale = 2)
  private BigDecimal employmentRatePct;

  public ProgramOutcomeSummary() {
  }

  public Long getProgramId() {
    return programId;
  }

  public void setProgramId(Long programId) {
    this.programId = programId;
  }

  public Long getSchemeId() {
    return schemeId;
  }

  public void setSchemeId(Long schemeId) {
    this.schemeId = schemeId;
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

  public BigDecimal getSelfEmploymentSpellCount() {
    return selfEmploymentSpellCount;
  }

  public void setSelfEmploymentSpellCount(BigDecimal selfEmploymentSpellCount) {
    this.selfEmploymentSpellCount = selfEmploymentSpellCount;
  }

  public BigDecimal getApprenticeshipSpellCount() {
    return apprenticeshipSpellCount;
  }

  public void setApprenticeshipSpellCount(BigDecimal apprenticeshipSpellCount) {
    this.apprenticeshipSpellCount = apprenticeshipSpellCount;
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
    if (!(o instanceof ProgramOutcomeSummary that)) {
      return false;
    }
    return Objects.equals(programId, that.programId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(programId);
  }
}
