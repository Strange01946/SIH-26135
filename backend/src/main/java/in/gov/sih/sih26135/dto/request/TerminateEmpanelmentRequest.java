package in.gov.sih.sih26135.dto.request;

import java.time.LocalDate;

public class TerminateEmpanelmentRequest {

  private LocalDate empanelledTo;
  private Long lifecycleStatusId;

  public TerminateEmpanelmentRequest() {
  }

  public TerminateEmpanelmentRequest(LocalDate empanelledTo, Long lifecycleStatusId) {
    this.empanelledTo = empanelledTo;
    this.lifecycleStatusId = lifecycleStatusId;
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
