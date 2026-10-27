package in.gov.sih.sih26135.dto.request;
import jakarta.validation.constraints.*;

public class ChangeBatchStatusRequest {

  @NotNull(message = "Batch status ID is required")
  private Long batchStatusId;

  public ChangeBatchStatusRequest() {
  }

  public ChangeBatchStatusRequest(Long batchStatusId) {
    this.batchStatusId = batchStatusId;
  }

  public Long getBatchStatusId() {
    return batchStatusId;
  }

  public void setBatchStatusId(Long batchStatusId) {
    this.batchStatusId = batchStatusId;
  }
}
