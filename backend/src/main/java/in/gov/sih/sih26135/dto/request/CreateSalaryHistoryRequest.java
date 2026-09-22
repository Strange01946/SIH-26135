package in.gov.sih.sih26135.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class CreateSalaryHistoryRequest {

  private Long employmentId;
  private Long traineeId;
  private BigDecimal salaryAmount;
  private Long salaryFrequencyId;
  private String currencyCode;
  private LocalDate effectiveFrom;
  private LocalDate effectiveTo;
  private Integer observationMonthOffset;
  private Long employmentInfoSourceId;
  private Long recordVerificationStatusId;
  private LocalDateTime verifiedAt;
  private Long verifiedByUserId;

  public CreateSalaryHistoryRequest() {
  }

  public CreateSalaryHistoryRequest(
      Long employmentId,
      BigDecimal salaryAmount,
      Long salaryFrequencyId,
      LocalDate effectiveFrom,
      Long employmentInfoSourceId,
      Long recordVerificationStatusId) {
    this.employmentId = employmentId;
    this.salaryAmount = salaryAmount;
    this.salaryFrequencyId = salaryFrequencyId;
    this.effectiveFrom = effectiveFrom;
    this.employmentInfoSourceId = employmentInfoSourceId;
    this.recordVerificationStatusId = recordVerificationStatusId;
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

  public BigDecimal getSalaryAmount() {
    return salaryAmount;
  }

  public void setSalaryAmount(BigDecimal salaryAmount) {
    this.salaryAmount = salaryAmount;
  }

  public Long getSalaryFrequencyId() {
    return salaryFrequencyId;
  }

  public void setSalaryFrequencyId(Long salaryFrequencyId) {
    this.salaryFrequencyId = salaryFrequencyId;
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

  public Long getEmploymentInfoSourceId() {
    return employmentInfoSourceId;
  }

  public void setEmploymentInfoSourceId(Long employmentInfoSourceId) {
    this.employmentInfoSourceId = employmentInfoSourceId;
  }

  public Long getRecordVerificationStatusId() {
    return recordVerificationStatusId;
  }

  public void setRecordVerificationStatusId(Long recordVerificationStatusId) {
    this.recordVerificationStatusId = recordVerificationStatusId;
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
}
