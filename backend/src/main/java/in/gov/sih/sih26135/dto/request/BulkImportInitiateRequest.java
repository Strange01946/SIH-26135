package in.gov.sih.sih26135.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Objects;

/**
 * Request payload for initiating a bulk data ingestion batch.
 */
public class BulkImportInitiateRequest {

  @NotBlank(message = "sourceSystem is required")
  @Size(max = 64, message = "sourceSystem cannot exceed 64 characters")
  private String sourceSystem;

  @NotBlank(message = "entityType is required")
  @Size(max = 64, message = "entityType cannot exceed 64 characters")
  private String entityType;

  private Boolean dryRun = false;

  @Size(max = 500, message = "notes cannot exceed 500 characters")
  private String notes;

  public BulkImportInitiateRequest() {}

  public BulkImportInitiateRequest(String sourceSystem, String entityType) {
    this.sourceSystem = sourceSystem;
    this.entityType = entityType;
    this.dryRun = false;
  }

  public BulkImportInitiateRequest(String sourceSystem, String entityType, Boolean dryRun, String notes) {
    this.sourceSystem = sourceSystem;
    this.entityType = entityType;
    this.dryRun = dryRun != null ? dryRun : false;
    this.notes = notes;
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

  public Boolean getDryRun() {
    return dryRun;
  }

  public void setDryRun(Boolean dryRun) {
    this.dryRun = dryRun != null ? dryRun : false;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    BulkImportInitiateRequest that = (BulkImportInitiateRequest) o;
    return Objects.equals(sourceSystem, that.sourceSystem) &&
        Objects.equals(entityType, that.entityType) &&
        Objects.equals(dryRun, that.dryRun) &&
        Objects.equals(notes, that.notes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sourceSystem, entityType, dryRun, notes);
  }

  @Override
  public String toString() {
    return "BulkImportInitiateRequest{" +
        "sourceSystem='" + sourceSystem + '\'' +
        ", entityType='" + entityType + '\'' +
        ", dryRun=" + dryRun +
        ", notes='" + notes + '\'' +
        '}';
  }
}
