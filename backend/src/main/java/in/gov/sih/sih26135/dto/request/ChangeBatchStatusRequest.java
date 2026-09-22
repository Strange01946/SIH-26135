package in.gov.sih.sih26135.dto.request;

public class ChangeBatchStatusRequest {

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
