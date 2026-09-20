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
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "data_quality_issues")
public class DataQualityIssue {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "data_quality_issue_id", nullable = false, updatable = false)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "data_quality_rule_id")
  private DataQualityRule dataQualityRule;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "data_quality_category_id", nullable = false)
  private RefDataQualityCategory dataQualityCategory;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "data_quality_severity_id", nullable = false)
  private RefDataQualitySeverity dataQualitySeverity;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "data_quality_issue_status_id", nullable = false)
  private RefDataQualityIssueStatus dataQualityIssueStatus;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "data_quality_detection_source_id", nullable = false)
  private RefDataQualityDetectionSource dataQualityDetectionSource;

  @Column(name = "entity_type", length = 64, nullable = false)
  private String entityType;

  @Column(name = "entity_id")
  private Long entityId;

  @Column(name = "issue_summary", length = 500, nullable = false)
  private String issueSummary;

  @Column(name = "detected_at", nullable = false)
  private LocalDateTime detectedAt;

  @Column(name = "assigned_user_id")
  private Long assignedUserId;

  @Column(name = "resolved_at")
  private LocalDateTime resolvedAt;

  @Column(name = "resolved_by_user_id")
  private Long resolvedByUserId;

  @Column(name = "resolution_notes", length = 500)
  private String resolutionNotes;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public DataQualityIssue() {
  }

  public DataQualityIssue(RefDataQualityCategory dataQualityCategory,
      RefDataQualitySeverity dataQualitySeverity,
      RefDataQualityIssueStatus dataQualityIssueStatus,
      RefDataQualityDetectionSource dataQualityDetectionSource, String entityType,
      String issueSummary, LocalDateTime detectedAt) {
    this.dataQualityCategory = dataQualityCategory;
    this.dataQualitySeverity = dataQualitySeverity;
    this.dataQualityIssueStatus = dataQualityIssueStatus;
    this.dataQualityDetectionSource = dataQualityDetectionSource;
    this.entityType = entityType;
    this.issueSummary = issueSummary;
    this.detectedAt = detectedAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public DataQualityRule getDataQualityRule() {
    return dataQualityRule;
  }

  public void setDataQualityRule(DataQualityRule dataQualityRule) {
    this.dataQualityRule = dataQualityRule;
  }

  public RefDataQualityCategory getDataQualityCategory() {
    return dataQualityCategory;
  }

  public void setDataQualityCategory(RefDataQualityCategory dataQualityCategory) {
    this.dataQualityCategory = dataQualityCategory;
  }

  public RefDataQualitySeverity getDataQualitySeverity() {
    return dataQualitySeverity;
  }

  public void setDataQualitySeverity(RefDataQualitySeverity dataQualitySeverity) {
    this.dataQualitySeverity = dataQualitySeverity;
  }

  public RefDataQualityIssueStatus getDataQualityIssueStatus() {
    return dataQualityIssueStatus;
  }

  public void setDataQualityIssueStatus(RefDataQualityIssueStatus dataQualityIssueStatus) {
    this.dataQualityIssueStatus = dataQualityIssueStatus;
  }

  public RefDataQualityDetectionSource getDataQualityDetectionSource() {
    return dataQualityDetectionSource;
  }

  public void setDataQualityDetectionSource(
      RefDataQualityDetectionSource dataQualityDetectionSource) {
    this.dataQualityDetectionSource = dataQualityDetectionSource;
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

  public String getIssueSummary() {
    return issueSummary;
  }

  public void setIssueSummary(String issueSummary) {
    this.issueSummary = issueSummary;
  }

  public LocalDateTime getDetectedAt() {
    return detectedAt;
  }

  public void setDetectedAt(LocalDateTime detectedAt) {
    this.detectedAt = detectedAt;
  }

  public Long getAssignedUserId() {
    return assignedUserId;
  }

  public void setAssignedUserId(Long assignedUserId) {
    this.assignedUserId = assignedUserId;
  }

  public LocalDateTime getResolvedAt() {
    return resolvedAt;
  }

  public void setResolvedAt(LocalDateTime resolvedAt) {
    this.resolvedAt = resolvedAt;
  }

  public Long getResolvedByUserId() {
    return resolvedByUserId;
  }

  public void setResolvedByUserId(Long resolvedByUserId) {
    this.resolvedByUserId = resolvedByUserId;
  }

  public String getResolutionNotes() {
    return resolutionNotes;
  }

  public void setResolutionNotes(String resolutionNotes) {
    this.resolutionNotes = resolutionNotes;
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
    if (!(o instanceof DataQualityIssue that)) {
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
    return "DataQualityIssue{" +
        "id=" + id +
        ", entityType='" + entityType + '\'' +
        ", entityId=" + entityId +
        ", detectedAt=" + detectedAt +
        '}';
  }
}
