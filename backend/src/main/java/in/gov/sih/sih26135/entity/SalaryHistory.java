package in.gov.sih.sih26135.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "salary_history", uniqueConstraints = {
    @UniqueConstraint(name = "uk_salary_history_employment_from", columnNames = {"employment_id", "effective_from"}),
    @UniqueConstraint(name = "uk_salary_history_employment_milestone", columnNames = {"employment_id", "observation_month_offset"})
})
public class SalaryHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "salary_history_id", nullable = false, updatable = false)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "employment_id", nullable = false)
  private EmploymentRecord employmentRecord;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "trainee_id", nullable = false)
  private Trainee trainee;

  @Column(name = "salary_amount", precision = 12, scale = 2, nullable = false)
  private BigDecimal salaryAmount;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "salary_frequency_id", nullable = false)
  private RefSalaryFrequency salaryFrequency;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "currency_code", length = 3, nullable = false, columnDefinition = "CHAR(3)")
  private String currencyCode = "INR";

  @Column(name = "effective_from", nullable = false)
  private LocalDate effectiveFrom;

  @Column(name = "effective_to")
  private LocalDate effectiveTo;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "observation_month_offset", columnDefinition = "SMALLINT UNSIGNED")
  private Integer observationMonthOffset;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "employment_info_source_id", nullable = false)
  private RefEmploymentInfoSource employmentInfoSource;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "record_verification_status_id", nullable = false)
  private RefRecordVerificationStatus recordVerificationStatus;

  @Column(name = "verified_at")
  private LocalDateTime verifiedAt;

  @Column(name = "verified_by_user_id")
  private Long verifiedByUserId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public SalaryHistory() {
  }

  public SalaryHistory(EmploymentRecord employmentRecord, Trainee trainee, BigDecimal salaryAmount,
      RefSalaryFrequency salaryFrequency, LocalDate effectiveFrom,
      RefEmploymentInfoSource employmentInfoSource, RefRecordVerificationStatus recordVerificationStatus) {
    this.employmentRecord = employmentRecord;
    this.trainee = trainee;
    this.salaryAmount = salaryAmount;
    this.salaryFrequency = salaryFrequency;
    this.effectiveFrom = effectiveFrom;
    this.employmentInfoSource = employmentInfoSource;
    this.recordVerificationStatus = recordVerificationStatus;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public EmploymentRecord getEmploymentRecord() {
    return employmentRecord;
  }

  public void setEmploymentRecord(EmploymentRecord employmentRecord) {
    this.employmentRecord = employmentRecord;
  }

  public Trainee getTrainee() {
    return trainee;
  }

  public void setTrainee(Trainee trainee) {
    this.trainee = trainee;
  }

  public BigDecimal getSalaryAmount() {
    return salaryAmount;
  }

  public void setSalaryAmount(BigDecimal salaryAmount) {
    this.salaryAmount = salaryAmount;
  }

  public RefSalaryFrequency getSalaryFrequency() {
    return salaryFrequency;
  }

  public void setSalaryFrequency(RefSalaryFrequency salaryFrequency) {
    this.salaryFrequency = salaryFrequency;
  }

  public String getCurrencyCode() {
    return currencyCode;
  }

  public void setCurrencyCode(String currencyCode) {
    this.currencyCode = currencyCode;
  }

  public LocalDate getEffectiveFrom() {
    return effectiveFrom;
  }

  public void setEffectiveFrom(LocalDate effectiveFrom) {
    this.effectiveFrom = effectiveFrom;
  }

  public LocalDate getEffectiveTo() {
    return effectiveTo;
  }

  public void setEffectiveTo(LocalDate effectiveTo) {
    this.effectiveTo = effectiveTo;
  }

  public Integer getObservationMonthOffset() {
    return observationMonthOffset;
  }

  public void setObservationMonthOffset(Integer observationMonthOffset) {
    this.observationMonthOffset = observationMonthOffset;
  }

  public RefEmploymentInfoSource getEmploymentInfoSource() {
    return employmentInfoSource;
  }

  public void setEmploymentInfoSource(RefEmploymentInfoSource employmentInfoSource) {
    this.employmentInfoSource = employmentInfoSource;
  }

  public RefRecordVerificationStatus getRecordVerificationStatus() {
    return recordVerificationStatus;
  }

  public void setRecordVerificationStatus(RefRecordVerificationStatus recordVerificationStatus) {
    this.recordVerificationStatus = recordVerificationStatus;
  }

  public LocalDateTime getVerifiedAt() {
    return verifiedAt;
  }

  public void setVerifiedAt(LocalDateTime verifiedAt) {
    this.verifiedAt = verifiedAt;
  }

  public Long getVerifiedByUserId() {
    return verifiedByUserId;
  }

  public void setVerifiedByUserId(Long verifiedByUserId) {
    this.verifiedByUserId = verifiedByUserId;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SalaryHistory that)) {
      return false;
    }
    return Objects.equals(employmentRecord != null ? employmentRecord.getId() : null, that.employmentRecord != null ? that.employmentRecord.getId() : null)
        && Objects.equals(effectiveFrom, that.effectiveFrom);
  }

  @Override
  public int hashCode() {
    return Objects.hash(employmentRecord != null ? employmentRecord.getId() : null, effectiveFrom);
  }

  @Override
  public String toString() {
    return "SalaryHistory{" +
        "id=" + id +
        ", salaryAmount=" + salaryAmount +
        ", currencyCode='" + currencyCode + '\'' +
        ", effectiveFrom=" + effectiveFrom +
        ", effectiveTo=" + effectiveTo +
        ", observationMonthOffset=" + observationMonthOffset +
        '}';
  }
}
