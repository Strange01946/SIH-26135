package in.gov.sih.sih26135.dto.request;
import jakarta.validation.constraints.*;

public class UpdateImportRecordRequest {

  private Integer sourceRowNumber;
  private Long importRecordStatusId;
  @Size(max = 64, message = "entityType cannot exceed 64 characters")
  private String entityType;
  private Long entityId;
  @Size(max = 64, message = "errorCode cannot exceed 64 characters")
  private String errorCode;
  @Size(max = 500, message = "errorMessage cannot exceed 500 characters")
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
