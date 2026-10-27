package in.gov.sih.sih26135.dto.request;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

import in.gov.sih.sih26135.validation.DateSequence;

@DateSequence(startField = "startDate", endField = "endDate")
public class CreateProgramRequest {

  @NotBlank(message = "programCode is required")
  @Size(max = 32, message = "programCode cannot exceed 32 characters")
  private String programCode;
  @NotBlank(message = "programName is required")
  @Size(max = 200, message = "programName cannot exceed 200 characters")
  private String programName;
  @Size(max = 65535, message = "description cannot exceed 65535 characters")
  private String description;
  private Long departmentId;
  private Long schemeId;
  private LocalDate startDate;
  private LocalDate endDate;
  @PositiveOrZero(message = "budget must be greater than or equal to 0")
  @Digits(integer = 16, fraction = 2, message = "budget numeric value out of bounds (<16 digits>.<2 digits> expected)")
  private BigDecimal budget;
  private String currencyCode = "INR";
  private Integer targetBeneficiaries;
  private Long lifecycleStatusId;

  public CreateProgramRequest() {
  }

  public CreateProgramRequest(
      String programCode,
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
    this.programCode = programCode;
    this.programName = programName;
    this.description = description;
    this.departmentId = departmentId;
    this.schemeId = schemeId;
    this.startDate = startDate;
    this.endDate = endDate;
    this.budget = budget;
    this.currencyCode = currencyCode != null ? currencyCode : "INR";
    this.targetBeneficiaries = targetBeneficiaries;
    this.lifecycleStatusId = lifecycleStatusId;
  }

  public String getProgramCode() {
    return programCode;
  }

  public void setProgramCode(String programCode) {
    this.programCode = programCode;
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
