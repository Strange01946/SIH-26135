package in.gov.sih.sih26135.entity.analytics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "vw_salary_progression_fact")
@Immutable
public class SalaryProgressionFact {

  @Id
  @Column(name = "employment_id", nullable = false)
  private Long employmentId;

  @Column(name = "trainee_id", nullable = false)
  private Long traineeId;

  @Column(name = "enrollment_id")
  private Long enrollmentId;

  @Column(name = "course_id")
  private Long courseId;

  @Column(name = "provider_id")
  private Long providerId;

  @Column(name = "engagement_type_id", nullable = false)
  private Long engagementTypeId;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "currency_code", length = 3, columnDefinition = "CHAR(3)")
  private String currencyCode;

  @Column(name = "starting_salary", precision = 12, scale = 2)
  private BigDecimal startingSalary;

  @Column(name = "first_recorded_salary", precision = 12, scale = 2)
  private BigDecimal firstRecordedSalary;

  @Column(name = "first_salary_effective_from")
  private LocalDate firstSalaryEffectiveFrom;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "first_observation_month_offset",
          columnDefinition = "SMALLINT UNSIGNED")
  private Integer firstObservationMonthOffset;

  @Column(name = "latest_recorded_salary", precision = 12, scale = 2)
  private BigDecimal latestRecordedSalary;

  @Column(name = "latest_salary_effective_from")
  private LocalDate latestSalaryEffectiveFrom;

  @Column(name = "latest_salary_effective_to")
  private LocalDate latestSalaryEffectiveTo;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "latest_observation_month_offset",
          columnDefinition = "SMALLINT UNSIGNED")
  private Integer latestObservationMonthOffset;

  @Column(name = "latest_salary_frequency_code", length = 32)
  private String latestSalaryFrequencyCode;

  @Column(name = "salary_change_amount", precision = 14, scale = 2)
  private BigDecimal salaryChangeAmount;

  @Column(name = "salary_change_pct", precision = 7, scale = 2)
  private BigDecimal salaryChangePct;

  public SalaryProgressionFact() {
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

  public Long getCourseId() {
    return courseId;
  }

  public void setCourseId(Long courseId) {
    this.courseId = courseId;
  }

  public Long getProviderId() {
    return providerId;
  }

  public void setProviderId(Long providerId) {
    this.providerId = providerId;
  }

  public Long getEngagementTypeId() {
    return engagementTypeId;
  }

  public void setEngagementTypeId(Long engagementTypeId) {
    this.engagementTypeId = engagementTypeId;
  }

  public String getCurrencyCode() {
    return currencyCode;
  }

  public void setCurrencyCode(String currencyCode) {
    this.currencyCode = currencyCode;
  }

  public BigDecimal getStartingSalary() {
    return startingSalary;
  }

  public void setStartingSalary(BigDecimal startingSalary) {
    this.startingSalary = startingSalary;
  }

  public BigDecimal getFirstRecordedSalary() {
    return firstRecordedSalary;
  }

  public void setFirstRecordedSalary(BigDecimal firstRecordedSalary) {
    this.firstRecordedSalary = firstRecordedSalary;
  }

  public LocalDate getFirstSalaryEffectiveFrom() {
    return firstSalaryEffectiveFrom;
  }

  public void setFirstSalaryEffectiveFrom(LocalDate firstSalaryEffectiveFrom) {
    this.firstSalaryEffectiveFrom = firstSalaryEffectiveFrom;
  }

  public Integer getFirstObservationMonthOffset() {
    return firstObservationMonthOffset;
  }

  public void setFirstObservationMonthOffset(Integer firstObservationMonthOffset) {
    this.firstObservationMonthOffset = firstObservationMonthOffset;
  }

  public BigDecimal getLatestRecordedSalary() {
    return latestRecordedSalary;
  }

  public void setLatestRecordedSalary(BigDecimal latestRecordedSalary) {
    this.latestRecordedSalary = latestRecordedSalary;
  }

  public LocalDate getLatestSalaryEffectiveFrom() {
    return latestSalaryEffectiveFrom;
  }

  public void setLatestSalaryEffectiveFrom(LocalDate latestSalaryEffectiveFrom) {
    this.latestSalaryEffectiveFrom = latestSalaryEffectiveFrom;
  }

  public LocalDate getLatestSalaryEffectiveTo() {
    return latestSalaryEffectiveTo;
  }

  public void setLatestSalaryEffectiveTo(LocalDate latestSalaryEffectiveTo) {
    this.latestSalaryEffectiveTo = latestSalaryEffectiveTo;
  }

  public Integer getLatestObservationMonthOffset() {
    return latestObservationMonthOffset;
  }

  public void setLatestObservationMonthOffset(Integer latestObservationMonthOffset) {
    this.latestObservationMonthOffset = latestObservationMonthOffset;
  }

  public String getLatestSalaryFrequencyCode() {
    return latestSalaryFrequencyCode;
  }

  public void setLatestSalaryFrequencyCode(String latestSalaryFrequencyCode) {
    this.latestSalaryFrequencyCode = latestSalaryFrequencyCode;
  }

  public BigDecimal getSalaryChangeAmount() {
    return salaryChangeAmount;
  }

  public void setSalaryChangeAmount(BigDecimal salaryChangeAmount) {
    this.salaryChangeAmount = salaryChangeAmount;
  }

  public BigDecimal getSalaryChangePct() {
    return salaryChangePct;
  }

  public void setSalaryChangePct(BigDecimal salaryChangePct) {
    this.salaryChangePct = salaryChangePct;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SalaryProgressionFact that)) {
      return false;
    }
    return Objects.equals(employmentId, that.employmentId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(employmentId);
  }
}
