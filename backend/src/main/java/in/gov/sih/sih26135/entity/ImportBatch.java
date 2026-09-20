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
@Table(name = "import_batches", uniqueConstraints = {
    @UniqueConstraint(name = "uk_import_batches_code", columnNames = {"batch_code"})
})
public class ImportBatch {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "import_batch_id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "batch_code", length = 32, nullable = false, unique = true)
  private String batchCode;

  @Column(name = "source_system", length = 64, nullable = false)
  private String sourceSystem;

  @Column(name = "entity_type", length = 64, nullable = false)
  private String entityType;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "import_batch_status_id", nullable = false)
  private RefImportBatchStatus importBatchStatus;

  @Column(name = "initiated_by_user_id")
  private Long initiatedByUserId;

  @Column(name = "row_count")
  private Integer rowCount;

  @Column(name = "success_count")
  private Integer successCount;

  @Column(name = "failure_count")
  private Integer failureCount;

  @Column(name = "started_at")
  private LocalDateTime startedAt;

  @Column(name = "completed_at")
  private LocalDateTime completedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public ImportBatch() {
  }

  public ImportBatch(String batchCode, String sourceSystem, String entityType,
      RefImportBatchStatus importBatchStatus) {
    this.batchCode = batchCode;
    this.sourceSystem = sourceSystem;
    this.entityType = entityType;
    this.importBatchStatus = importBatchStatus;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
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

  public RefImportBatchStatus getImportBatchStatus() {
    return importBatchStatus;
  }

  public void setImportBatchStatus(RefImportBatchStatus importBatchStatus) {
    this.importBatchStatus = importBatchStatus;
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
    if (!(o instanceof ImportBatch that)) {
      return false;
    }
    return Objects.equals(batchCode, that.batchCode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(batchCode);
  }

  @Override
  public String toString() {
    return "ImportBatch{" +
        "id=" + id +
        ", batchCode='" + batchCode + '\'' +
        ", sourceSystem='" + sourceSystem + '\'' +
        ", entityType='" + entityType + '\'' +
        '}';
  }
}
