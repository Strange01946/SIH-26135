package in.gov.sih.sih26135.dto.request;

public class UnemploymentToEmploymentTransitionRequest {

  private Long unemploymentEventId;
  private CreateEmploymentRecordRequest newEmploymentRequest;
  private CreateSalaryHistoryRequest initialSalaryRequest;

  public UnemploymentToEmploymentTransitionRequest() {
  }

  public UnemploymentToEmploymentTransitionRequest(
      Long unemploymentEventId,
      CreateEmploymentRecordRequest newEmploymentRequest,
      CreateSalaryHistoryRequest initialSalaryRequest) {
    this.unemploymentEventId = unemploymentEventId;
    this.newEmploymentRequest = newEmploymentRequest;
    this.initialSalaryRequest = initialSalaryRequest;
  }

  public Long getUnemploymentEventId() {
    return unemploymentEventId;
  }

  public void setUnemploymentEventId(Long unemploymentEventId) {
    this.unemploymentEventId = unemploymentEventId;
  }

  public CreateEmploymentRecordRequest getNewEmploymentRequest() {
    return newEmploymentRequest;
  }

  public void setNewEmploymentRequest(CreateEmploymentRecordRequest newEmploymentRequest) {
    this.newEmploymentRequest = newEmploymentRequest;
  }

  public CreateSalaryHistoryRequest getInitialSalaryRequest() {
    return initialSalaryRequest;
  }

  public void setInitialSalaryRequest(CreateSalaryHistoryRequest initialSalaryRequest) {
    this.initialSalaryRequest = initialSalaryRequest;
  }
}
