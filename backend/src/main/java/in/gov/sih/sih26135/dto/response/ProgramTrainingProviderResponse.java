package in.gov.sih.sih26135.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ProgramTrainingProviderResponse {

  private Long id;
  private Long programId;
  private String programCode;
  private String programName;
  private Long providerId;
  private String providerCode;
  private String providerName;
  private LocalDate empanelledFrom;
  private LocalDate empanelledTo;
  private Long lifecycleStatusId;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public ProgramTrainingProviderResponse() {
  }

  public ProgramTrainingProviderResponse(
      Long id,
      Long programId,
      String programCode,
      String programName,
      Long providerId,
      String providerCode,
      String providerName,
      LocalDate empanelledFrom,
      LocalDate empanelledTo,
      Long lifecycleStatusId,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.programId = programId;
    this.programCode = programCode;
    this.programName = programName;
    this.providerId = providerId;
    this.providerCode = providerCode;
    this.providerName = providerName;
    this.empanelledFrom = empanelledFrom;
    this.empanelledTo = empanelledTo;
    this.lifecycleStatusId = lifecycleStatusId;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getProgramId() {
    return programId;
  }

  public void setProgramId(Long programId) {
    this.programId = programId;
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

  public Long getProviderId() {
    return providerId;
  }

  public void setProviderId(Long providerId) {
    this.providerId = providerId;
  }

  public String getProviderCode() {
    return providerCode;
  }

  public void setProviderCode(String providerCode) {
    this.providerCode = providerCode;
  }

  public String getProviderName() {
    return providerName;
  }

  public void setProviderName(String providerName) {
    this.providerName = providerName;
  }

  public LocalDate getEmpanelledFrom() {
    return empanelledFrom;
  }

  public void setEmpanelledFrom(LocalDate empanelledFrom) {
    this.empanelledFrom = empanelledFrom;
  }

  public LocalDate getEmpanelledTo() {
    return empanelledTo;
  }

  public void setEmpanelledTo(LocalDate empanelledTo) {
    this.empanelledTo = empanelledTo;
  }

  public Long getLifecycleStatusId() {
    return lifecycleStatusId;
  }

  public void setLifecycleStatusId(Long lifecycleStatusId) {
    this.lifecycleStatusId = lifecycleStatusId;
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
}
