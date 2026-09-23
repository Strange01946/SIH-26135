package in.gov.sih.sih26135.dto.request;

import java.time.LocalDateTime;

public class UpdateImportBatchRequest {

  private Long importBatchStatusId;
  private Integer rowCount;
  private Integer successCount;
  private Integer failureCount;
  private LocalDateTime startedAt;
  private LocalDateTime completedAt;

  public UpdateImportBatchRequest() {}

  public Long getImportBatchStatusId() {
    return importBatchStatusId;
  }

  public void setImportBatchStatusId(Long importBatchStatusId) {
    this.importBatchStatusId = importBatchStatusId;
  }

  public Integer getRowCount() {
    return rowCount;
  }

  public void setRowCount(Integer rowCount) {
    this.rowCount = rowCount;
  }

  public Integer getSuccessCount() {
    return successCount;
  }

  public void setSuccessCount(Integer successCount) {
    this.successCount = successCount;
  }

  public Integer getFailureCount() {
    return failureCount;
  }

  public void setFailureCount(Integer failureCount) {
    this.failureCount = failureCount;
  }

  public LocalDateTime getStartedAt() {
    return startedAt;
  }

  public void setStartedAt(LocalDateTime startedAt) {
    this.startedAt = startedAt;
  }

  public LocalDateTime getCompletedAt() {
    return completedAt;
  }

  public void setCompletedAt(LocalDateTime completedAt) {
    this.completedAt = completedAt;
  }
}
