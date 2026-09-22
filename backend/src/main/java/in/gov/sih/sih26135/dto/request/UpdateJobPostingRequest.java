package in.gov.sih.sih26135.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

public class UpdateJobPostingRequest {

  private String postingTitle;
  private String description;
  private Long employerBranchId;
  private Long jobRoleId;
  private Long engagementTypeId;
  private Long qualificationLevelId;
  private Integer vacancies;
  private BigDecimal minSalary;
  private BigDecimal maxSalary;
  private Long salaryFrequencyId;
  private String currencyCode;
  private Long stateId;
  private Long districtId;
  private Long locationId;
  private LocalDate postedDate;
  private LocalDate closingDate;
  private Long jobPostingStatusId;

  public UpdateJobPostingRequest() {
  }

  public String getPostingTitle() {
    return postingTitle;
  }

  public void setPostingTitle(String postingTitle) {
    this.postingTitle = postingTitle;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
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

  public Long getQualificationLevelId() {
    return qualificationLevelId;
  }

  public void setQualificationLevelId(Long qualificationLevelId) {
    this.qualificationLevelId = qualificationLevelId;
  }

  public Integer getVacancies() {
    return vacancies;
  }

  public void setVacancies(Integer vacancies) {
    this.vacancies = vacancies;
  }

  public BigDecimal getMinSalary() {
    return minSalary;
  }

  public void setMinSalary(BigDecimal minSalary) {
    this.minSalary = minSalary;
  }

  public BigDecimal getMaxSalary() {
    return maxSalary;
  }

  public void setMaxSalary(BigDecimal maxSalary) {
    this.maxSalary = maxSalary;
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

  public Long getLocationId() {
    return locationId;
  }

  public void setLocationId(Long locationId) {
    this.locationId = locationId;
  }

  public LocalDate getPostedDate() {
    return postedDate;
  }

  public void setPostedDate(LocalDate postedDate) {
    this.postedDate = postedDate;
  }

  public LocalDate getClosingDate() {
    return closingDate;
  }

  public void setClosingDate(LocalDate closingDate) {
    this.closingDate = closingDate;
  }

  public Long getJobPostingStatusId() {
    return jobPostingStatusId;
  }

  public void setJobPostingStatusId(Long jobPostingStatusId) {
    this.jobPostingStatusId = jobPostingStatusId;
  }
}
