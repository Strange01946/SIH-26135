package in.gov.sih.sih26135.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class UpdateEmploymentRecordRequest {

  private Long employerBranchId;
  private Long jobRoleId;
  private Long engagementTypeId;
  private Long employmentSpellStatusId;
  private LocalDate startDate;
  private LocalDate endDate;
  private Boolean isCurrent;
  private BigDecimal startingSalary;
  private Long salaryFrequencyId;
  private String currencyCode;
  private Long workLocationId;
  private Long workStateId;
  private Long workDistrictId;
  private Long employmentInfoSourceId;
  private Long recordVerificationStatusId;
  private LocalDateTime verifiedAt;
  private Long verifiedByUserId;
  private Long employmentExitReasonId;
  private String exitRemarks;

  public UpdateEmploymentRecordRequest() {
  }

  public Long getEmployerBranchId() {
    return employerBranchId;
  }

  public void setEmployerBranchId(Long employerBranchId) {
    this.employerBranchId = employerBranchId;
  }

  public Long getJobRoleId() {
    return jobRoleId;
  }

  public void setJobRoleId(Long jobRoleId) {
    this.jobRoleId = jobRoleId;
  }

  public Long getEngagementTypeId() {
    return engagementTypeId;
  }

  public void setEngagementTypeId(Long engagementTypeId) {
    this.engagementTypeId = engagementTypeId;
  }

  public Long getEmploymentSpellStatusId() {
    return employmentSpellStatusId;
  }

  public void setEmploymentSpellStatusId(Long employmentSpellStatusId) {
    this.employmentSpellStatusId = employmentSpellStatusId;
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

  public void setIsCurrent(Boolean current) {
    isCurrent = current;
  }

  public BigDecimal getStartingSalary() {
    return startingSalary;
  }

  public void setStartingSalary(BigDecimal startingSalary) {
    this.startingSalary = startingSalary;
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

  public Long getWorkLocationId() {
    return workLocationId;
  }

  public void setWorkLocationId(Long workLocationId) {
    this.workLocationId = workLocationId;
  }

  public Long getWorkStateId() {
    return workStateId;
  }

  public void setWorkStateId(Long workStateId) {
    this.workStateId = workStateId;
  }

  public Long getWorkDistrictId() {
    return workDistrictId;
  }

  public void setWorkDistrictId(Long workDistrictId) {
    this.workDistrictId = workDistrictId;
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

  public Long getEmploymentExitReasonId() {
    return employmentExitReasonId;
  }

  public void setEmploymentExitReasonId(Long employmentExitReasonId) {
    this.employmentExitReasonId = employmentExitReasonId;
  }

  public String getExitRemarks() {
    return exitRemarks;
  }

  public void setExitRemarks(String exitRemarks) {
    this.exitRemarks = exitRemarks;
  }
}
