package in.gov.sih.sih26135.dto.request;

public class PlacementToEmploymentTransitionRequest {

  private Long placementId;
  private CreateEmploymentRecordRequest employmentRecordRequest;
  private CreateSalaryHistoryRequest initialSalaryRequest;
  private Long placementJoiningStatusId;
  private Long placementStatusId;

  public PlacementToEmploymentTransitionRequest() {
  }

  public PlacementToEmploymentTransitionRequest(
      Long placementId,
      CreateEmploymentRecordRequest employmentRecordRequest,
      CreateSalaryHistoryRequest initialSalaryRequest,
      Long placementJoiningStatusId,
      Long placementStatusId) {
    this.placementId = placementId;
    this.employmentRecordRequest = employmentRecordRequest;
    this.initialSalaryRequest = initialSalaryRequest;
    this.placementJoiningStatusId = placementJoiningStatusId;
    this.placementStatusId = placementStatusId;
  }

  public Long getPlacementId() {
    return placementId;
  }

  public void setPlacementId(Long placementId) {
    this.placementId = placementId;
  }

  public CreateEmploymentRecordRequest getEmploymentRecordRequest() {
    return employmentRecordRequest;
  }

  public void setEmploymentRecordRequest(CreateEmploymentRecordRequest employmentRecordRequest) {
    this.employmentRecordRequest = employmentRecordRequest;
  }

  public CreateSalaryHistoryRequest getInitialSalaryRequest() {
    return initialSalaryRequest;
  }

  public void setInitialSalaryRequest(CreateSalaryHistoryRequest initialSalaryRequest) {
    this.initialSalaryRequest = initialSalaryRequest;
  }

  public Long getPlacementJoiningStatusId() {
    return placementJoiningStatusId;
  }

  public void setPlacementJoiningStatusId(Long placementJoiningStatusId) {
    this.placementJoiningStatusId = placementJoiningStatusId;
  }

  public Long getPlacementStatusId() {
    return placementStatusId;
  }

  public void setPlacementStatusId(Long placementStatusId) {
    this.placementStatusId = placementStatusId;
  }
}
