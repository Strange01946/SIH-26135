package in.gov.sih.sih26135.entity.analytics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Objects;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "vw_employment_retention_fact")
@Immutable
public class EmploymentRetentionFact {

  @Id
  @Column(name = "employment_id", nullable = false)
  private Long employmentId;

  @Column(name = "trainee_id", nullable = false)
  private Long traineeId;

  @Column(name = "enrollment_id")
  private Long enrollmentId;

  @Column(name = "engagement_type_id", nullable = false)
  private Long engagementTypeId;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date")
  private LocalDate endDate;

  @Column(name = "is_current", nullable = false)
  private Boolean isCurrent;

  @Column(name = "retained_6m_flag")
  private Integer retained6mFlag;

  @Column(name = "retained_12m_flag")
  private Integer retained12mFlag;

  @Column(name = "retained_24m_flag")
  private Integer retained24mFlag;

  @Column(name = "retained_36m_flag")
  private Integer retained36mFlag;

  public EmploymentRetentionFact() {
  }

  public Long getEmploymentId() {
    return employmentId;
  }

  public void setEmploymentId(Long employmentId) {
    this.employmentId = employmentId;
  }

  public Long getTraineeId() {
    return traineeId;
  }

  public void setTraineeId(Long traineeId) {
    this.traineeId = traineeId;
  }

  public Long getEnrollmentId() {
    return enrollmentId;
  }

  public void setEnrollmentId(Long enrollmentId) {
    this.enrollmentId = enrollmentId;
  }

  public Long getEngagementTypeId() {
    return engagementTypeId;
  }

  public void setEngagementTypeId(Long engagementTypeId) {
    this.engagementTypeId = engagementTypeId;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public void setStartDate(LocalDate startDate) {
    this.startDate = startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public void setEndDate(LocalDate endDate) {
    this.endDate = endDate;
  }

  public Boolean getIsCurrent() {
    return isCurrent;
  }

  public void setIsCurrent(Boolean isCurrent) {
    this.isCurrent = isCurrent;
  }

  public Integer getRetained6mFlag() {
    return retained6mFlag;
  }

  public void setRetained6mFlag(Integer retained6mFlag) {
    this.retained6mFlag = retained6mFlag;
  }

  public Integer getRetained12mFlag() {
    return retained12mFlag;
  }

  public void setRetained12mFlag(Integer retained12mFlag) {
    this.retained12mFlag = retained12mFlag;
  }

  public Integer getRetained24mFlag() {
    return retained24mFlag;
  }

  public void setRetained24mFlag(Integer retained24mFlag) {
    this.retained24mFlag = retained24mFlag;
  }

  public Integer getRetained36mFlag() {
    return retained36mFlag;
  }

  public void setRetained36mFlag(Integer retained36mFlag) {
    this.retained36mFlag = retained36mFlag;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof EmploymentRetentionFact that)) {
      return false;
    }
    return Objects.equals(employmentId, that.employmentId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(employmentId);
  }
}
