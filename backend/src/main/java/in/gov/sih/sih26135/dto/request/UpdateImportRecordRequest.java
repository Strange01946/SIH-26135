package in.gov.sih.sih26135.dto.request;

public class UpdateImportRecordRequest {

  private Integer sourceRowNumber;
  private Long importRecordStatusId;
  private String entityType;
  private Long entityId;
  private String errorCode;
  private String errorMessage;

  public UpdateImportRecordRequest() {}

  public Integer getSourceRowNumber() {
    return sourceRowNumber;
  }

  public void setSourceRowNumber(Integer sourceRowNumber) {
    this.sourceRowNumber = sourceRowNumber;
  }

  public Long getImportRecordStatusId() {
    return importRecordStatusId;
  }

  public void setImportRecordStatusId(Long importRecordStatusId) {
    this.importRecordStatusId = importRecordStatusId;
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
