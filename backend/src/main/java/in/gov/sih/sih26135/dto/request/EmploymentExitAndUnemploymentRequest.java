package in.gov.sih.sih26135.dto.request;

public class EmploymentExitAndUnemploymentRequest {

  private CreateEmploymentExitEventRequest exitEventRequest;
  private CreateTraineeUnemploymentEventRequest unemploymentEventRequest;

  public EmploymentExitAndUnemploymentRequest() {
  }

  public EmploymentExitAndUnemploymentRequest(
      CreateEmploymentExitEventRequest exitEventRequest,
      CreateTraineeUnemploymentEventRequest unemploymentEventRequest) {
    this.exitEventRequest = exitEventRequest;
    this.unemploymentEventRequest = unemploymentEventRequest;
  }

  public CreateEmploymentExitEventRequest getExitEventRequest() {
    return exitEventRequest;
  }

  public void setExitEventRequest(CreateEmploymentExitEventRequest exitEventRequest) {
    this.exitEventRequest = exitEventRequest;
  }

  public CreateTraineeUnemploymentEventRequest getUnemploymentEventRequest() {
    return unemploymentEventRequest;
  }

  public void setUnemploymentEventRequest(CreateTraineeUnemploymentEventRequest unemploymentEventRequest) {
    this.unemploymentEventRequest = unemploymentEventRequest;
  }
}
