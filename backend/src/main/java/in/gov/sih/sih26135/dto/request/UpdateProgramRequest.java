package in.gov.sih.sih26135.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

public class UpdateProgramRequest {

  private String programName;
  private String description;
  private Long departmentId;
  private Long schemeId;
  private LocalDate startDate;
  private LocalDate endDate;
  private BigDecimal budget;
  private String currencyCode;
  private Integer targetBeneficiaries;
  private Long lifecycleStatusId;

  public UpdateProgramRequest() {
  }

  public UpdateProgramRequest(
      String programName,
      String description,
      Long departmentId,
      Long schemeId,
      LocalDate startDate,
      LocalDate endDate,
      BigDecimal budget,
      String currencyCode,
      Integer targetBeneficiaries,
      Long lifecycleStatusId) {
    this.programName = programName;
    this.description = description;
    this.departmentId = departmentId;
    this.schemeId = schemeId;
    this.startDate = startDate;
    this.endDate = endDate;
    this.budget = budget;
    this.currencyCode = currencyCode;
    this.targetBeneficiaries = targetBeneficiaries;
    this.lifecycleStatusId = lifecycleStatusId;
  }

  public String getProgramName() {
    return programName;
  }

  public void setProgramName(String programName) {
    this.programName = programName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Long getDepartmentId() {
    return departmentId;
  }

  public void setDepartmentId(Long departmentId) {
    this.departmentId = departmentId;
  }

  public Long getSchemeId() {
    return schemeId;
  }

  public void setSchemeId(Long schemeId) {
    this.schemeId = schemeId;
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

  public BigDecimal getBudget() {
    return budget;
  }

  public void setBudget(BigDecimal budget) {
    this.budget = budget;
  }

  public String getCurrencyCode() {
    return currencyCode;
  }

  public void setCurrencyCode(String currencyCode) {
    this.currencyCode = currencyCode;
  }

  public Integer getTargetBeneficiaries() {
    return targetBeneficiaries;
  }

  public void setTargetBeneficiaries(Integer targetBeneficiaries) {
    this.targetBeneficiaries = targetBeneficiaries;
  }

  public Long getLifecycleStatusId() {
    return lifecycleStatusId;
  }

  public void setLifecycleStatusId(Long lifecycleStatusId) {
    this.lifecycleStatusId = lifecycleStatusId;
  }
}
