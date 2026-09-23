package in.gov.sih.sih26135.dto.request;

public class CreateImportRecordRequest {

  private Long importBatchId;
  private Integer sourceRowNumber;
  private String entityType;
  private Long entityId;
  private Long importRecordStatusId;
  private String errorCode;
  private String errorMessage;

  public CreateImportRecordRequest() {}

  public CreateImportRecordRequest(
      Long importBatchId,
      Integer sourceRowNumber,
      Long importRecordStatusId) {
    this.importBatchId = importBatchId;
    this.sourceRowNumber = sourceRowNumber;
    this.importRecordStatusId = importRecordStatusId;
  }

  public Long getImportBatchId() {
    return importBatchId;
  }

  public void setImportBatchId(Long importBatchId) {
    this.importBatchId = importBatchId;
  }

  public Integer getSourceRowNumber() {
    return sourceRowNumber;
  }

  public void setSourceRowNumber(Integer sourceRowNumber) {
    this.sourceRowNumber = sourceRowNumber;
  }

  public String getEntityType() {
    return entityType;
  }

  public void setEntityType(String entityType) {
    this.entityType = entityType;
  }

  public Long getEntityId() {
    return entityId;
  }

  public void setEntityId(Long entityId) {
    this.entityId = entityId;
  }

  public Long getImportRecordStatusId() {
    return importRecordStatusId;
  }

  public void setImportRecordStatusId(Long importRecordStatusId) {
    this.importRecordStatusId = importRecordStatusId;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public void setErrorCode(String errorCode) {
    this.errorCode = errorCode;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }
}
