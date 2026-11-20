package in.gov.sih.sih26135.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;

@Schema(description = "Request payload for transitioning an accepted placement to active employment")
public class PlacementToEmploymentTransitionRequest {

  @Schema(description = "Accepted placement record ID", example = "401", requiredMode = Schema.RequiredMode.REQUIRED)
  private Long placementId;

  @Schema(description = "Employment creation specifications")
  @Valid
  private CreateEmploymentRecordRequest employmentRecordRequest;

  @Schema(description = "Initial salary compensation specifications")
  @Valid
  private CreateSalaryHistoryRequest initialSalaryRequest;

  @Schema(description = "Placement joining status lookup ID", example = "1")
  private Long placementJoiningStatusId;

  @Schema(description = "Updated placement status lookup ID", example = "2")
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
