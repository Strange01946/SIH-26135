package in.gov.sih.sih26135.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "import_records", uniqueConstraints = {
    @UniqueConstraint(name = "uk_import_records_batch_row", columnNames = {
        "import_batch_id", "source_row_number"
    })
})
public class ImportRecord {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "import_record_id", nullable = false, updatable = false)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "import_batch_id", nullable = false)
  private ImportBatch importBatch;

  @Column(name = "source_row_number", nullable = false)
  private Integer sourceRowNumber;

  @Column(name = "entity_type", length = 64)
  private String entityType;

  @Column(name = "entity_id")
  private Long entityId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "import_record_status_id", nullable = false)
  private RefImportRecordStatus importRecordStatus;

  @Column(name = "error_code", length = 64)
  private String errorCode;

  @Column(name = "error_message", length = 500)
  private String errorMessage;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public ImportRecord() {
  }

  public ImportRecord(ImportBatch importBatch, Integer sourceRowNumber,
      RefImportRecordStatus importRecordStatus) {
    this.importBatch = importBatch;
    this.sourceRowNumber = sourceRowNumber;
    this.importRecordStatus = importRecordStatus;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public ImportBatch getImportBatch() {
    return importBatch;
  }

  public void setImportBatch(ImportBatch importBatch) {
    this.importBatch = importBatch;
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

  public RefImportRecordStatus getImportRecordStatus() {
    return importRecordStatus;
  }

  public void setImportRecordStatus(RefImportRecordStatus importRecordStatus) {
    this.importRecordStatus = importRecordStatus;
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

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ImportRecord that)) {
      return false;
    }
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "ImportRecord{" +
        "id=" + id +
        ", sourceRowNumber=" + sourceRowNumber +
        ", entityType='" + entityType + '\'' +
        ", entityId=" + entityId +
        '}';
  }
}
