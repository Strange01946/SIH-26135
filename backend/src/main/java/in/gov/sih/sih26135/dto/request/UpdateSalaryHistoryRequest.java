package in.gov.sih.sih26135.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class UpdateSalaryHistoryRequest {

  private BigDecimal salaryAmount;
  private Long salaryFrequencyId;
  private String currencyCode;
  private LocalDate effectiveTo;
  private boolean effectiveToSpecified;
  private Integer observationMonthOffset;
  private boolean observationMonthOffsetSpecified;
  private Long employmentInfoSourceId;
  private Long recordVerificationStatusId;
  private LocalDateTime verifiedAt;
  private Long verifiedByUserId;

  public UpdateSalaryHistoryRequest() {
  }

  public UpdateSalaryHistoryRequest(
      BigDecimal salaryAmount,
      Long salaryFrequencyId,
      String currencyCode,
      LocalDate effectiveTo,
      Integer observationMonthOffset,
      Long employmentInfoSourceId,
      Long recordVerificationStatusId,
      LocalDateTime verifiedAt,
      Long verifiedByUserId) {
    this.salaryAmount = salaryAmount;
    this.salaryFrequencyId = salaryFrequencyId;
    this.currencyCode = currencyCode;
    this.effectiveTo = effectiveTo;
    this.effectiveToSpecified = true;
    this.observationMonthOffset = observationMonthOffset;
    this.observationMonthOffsetSpecified = true;
    this.employmentInfoSourceId = employmentInfoSourceId;
    this.recordVerificationStatusId = recordVerificationStatusId;
    this.verifiedAt = verifiedAt;
    this.verifiedByUserId = verifiedByUserId;
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

  public LocalDate getEffectiveTo() {
    return effectiveTo;
  }

  public void setEffectiveTo(LocalDate effectiveTo) {
    this.effectiveTo = effectiveTo;
    this.effectiveToSpecified = true;
  }

  public boolean isEffectiveToSpecified() {
    return effectiveToSpecified;
  }

  public void setEffectiveToSpecified(boolean effectiveToSpecified) {
    this.effectiveToSpecified = effectiveToSpecified;
  }

  public Integer getObservationMonthOffset() {
    return observationMonthOffset;
  }

  public void setObservationMonthOffset(Integer observationMonthOffset) {
    this.observationMonthOffset = observationMonthOffset;
    this.observationMonthOffsetSpecified = true;
  }

  public boolean isObservationMonthOffsetSpecified() {
    return observationMonthOffsetSpecified;
  }

  public void setObservationMonthOffsetSpecified(boolean observationMonthOffsetSpecified) {
    this.observationMonthOffsetSpecified = observationMonthOffsetSpecified;
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
