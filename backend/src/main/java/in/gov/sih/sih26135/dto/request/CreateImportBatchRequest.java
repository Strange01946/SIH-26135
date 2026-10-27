package in.gov.sih.sih26135.dto.request;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public class CreateImportBatchRequest {

  @NotBlank(message = "batchCode is required")
  @Size(max = 32, message = "batchCode cannot exceed 32 characters")
  private String batchCode;
  @Size(max = 64, message = "sourceSystem cannot exceed 64 characters")
  private String sourceSystem;
  @Size(max = 64, message = "entityType cannot exceed 64 characters")
  private String entityType;
  private Long importBatchStatusId;
  private Long initiatedByUserId;
  @PositiveOrZero(message = "rowCount must be greater than or equal to 0")
  private Integer rowCount;
  private LocalDateTime startedAt;

  public CreateImportBatchRequest() {}

  public CreateImportBatchRequest(
      String batchCode,
      String sourceSystem,
      String entityType,
      Long importBatchStatusId) {
    this.batchCode = batchCode;
    this.sourceSystem = sourceSystem;
    this.entityType = entityType;
    this.importBatchStatusId = importBatchStatusId;
  }

  public String getBatchCode() {
    return batchCode;
  }

  public void setBatchCode(String batchCode) {
    this.batchCode = batchCode;
  }

  public String getSourceSystem() {
    return sourceSystem;
  }

  public void setSourceSystem(String sourceSystem) {
    this.sourceSystem = sourceSystem;
  }

  public String getEntityType() {
    return entityType;
  }

  public void setEntityType(String entityType) {
    this.entityType = entityType;
  }

  public Long getImportBatchStatusId() {
    return importBatchStatusId;
  }

  public void setImportBatchStatusId(Long importBatchStatusId) {
    this.importBatchStatusId = importBatchStatusId;
  }

  public Long getInitiatedByUserId() {
    return initiatedByUserId;
  }

  public void setInitiatedByUserId(Long initiatedByUserId) {
    this.initiatedByUserId = initiatedByUserId;
  }

  public Integer getRowCount() {
    return rowCount;
  }

  public void setRowCount(Integer rowCount) {
    this.rowCount = rowCount;
  }

  public LocalDateTime getStartedAt() {
    return startedAt;
  }

  public void setStartedAt(LocalDateTime startedAt) {
    this.startedAt = startedAt;
  }
}
