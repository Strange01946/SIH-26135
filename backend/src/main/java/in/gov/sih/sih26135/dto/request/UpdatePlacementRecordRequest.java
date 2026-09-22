package in.gov.sih.sih26135.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class UpdatePlacementRecordRequest {

  private Long employerBranchId;
  private Long jobRoleId;
  private Long engagementTypeId;
  private Long placementSourceId;
  private Long placementStatusId;
  private Long joiningStatusId;
  private BigDecimal offeredSalary;
  private BigDecimal joiningSalary;
  private Long salaryFrequencyId;
  private String currencyCode;
  private LocalDate offerDate;
  private LocalDate expectedJoiningDate;
  private LocalDate actualJoiningDate;
  private Long nonSelectionReasonId;
  private String outcomeRemarks;
  private Long workStateId;
  private Long workDistrictId;
  private Long recordVerificationStatusId;
  private LocalDateTime verifiedAt;
  private Long verifiedByUserId;

  public UpdatePlacementRecordRequest() {
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

  public Long getPlacementSourceId() {
    return placementSourceId;
  }

  public void setPlacementSourceId(Long placementSourceId) {
    this.placementSourceId = placementSourceId;
  }

  public Long getPlacementStatusId() {
    return placementStatusId;
  }

  public void setPlacementStatusId(Long placementStatusId) {
    this.placementStatusId = placementStatusId;
  }

  public Long getJoiningStatusId() {
    return joiningStatusId;
  }

  public void setJoiningStatusId(Long joiningStatusId) {
    this.joiningStatusId = joiningStatusId;
  }

  public BigDecimal getOfferedSalary() {
    return offeredSalary;
  }

  public void setOfferedSalary(BigDecimal offeredSalary) {
    this.offeredSalary = offeredSalary;
  }

  public BigDecimal getJoiningSalary() {
    return joiningSalary;
  }

  public void setJoiningSalary(BigDecimal joiningSalary) {
    this.joiningSalary = joiningSalary;
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

  public LocalDate getOfferDate() {
    return offerDate;
  }

  public void setOfferDate(LocalDate offerDate) {
    this.offerDate = offerDate;
  }

  public LocalDate getExpectedJoiningDate() {
    return expectedJoiningDate;
  }

  public void setExpectedJoiningDate(LocalDate expectedJoiningDate) {
    this.expectedJoiningDate = expectedJoiningDate;
  }

  public LocalDate getActualJoiningDate() {
    return actualJoiningDate;
  }

  public void setActualJoiningDate(LocalDate actualJoiningDate) {
    this.actualJoiningDate = actualJoiningDate;
  }

  public Long getNonSelectionReasonId() {
    return nonSelectionReasonId;
  }

  public void setNonSelectionReasonId(Long nonSelectionReasonId) {
    this.nonSelectionReasonId = nonSelectionReasonId;
  }

  public String getOutcomeRemarks() {
    return outcomeRemarks;
  }

  public void setOutcomeRemarks(String outcomeRemarks) {
    this.outcomeRemarks = outcomeRemarks;
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
