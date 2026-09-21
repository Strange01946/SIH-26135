package in.gov.sih.sih26135.dto.request;

import java.time.LocalDate;

public class AssignProgramTrainingProviderRequest {

  private Long programId;
  private Long providerId;
  private LocalDate empanelledFrom;
  private LocalDate empanelledTo;
  private Long lifecycleStatusId;

  public AssignProgramTrainingProviderRequest() {
  }

  public AssignProgramTrainingProviderRequest(
      Long programId,
      Long providerId,
      LocalDate empanelledFrom,
      LocalDate empanelledTo,
      Long lifecycleStatusId) {
    this.programId = programId;
    this.providerId = providerId;
    this.empanelledFrom = empanelledFrom;
    this.empanelledTo = empanelledTo;
    this.lifecycleStatusId = lifecycleStatusId;
  }

  public Long getProgramId() {
    return programId;
  }

  public void setProgramId(Long programId) {
    this.programId = programId;
  }

  public Long getProviderId() {
    return providerId;
  }

  public void setProviderId(Long providerId) {
    this.providerId = providerId;
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
}
