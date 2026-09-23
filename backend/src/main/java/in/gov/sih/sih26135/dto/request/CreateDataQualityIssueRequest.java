package in.gov.sih.sih26135.dto.request;

import java.time.LocalDateTime;

public class CreateDataQualityIssueRequest {

  private Long dataQualityRuleId;
  private Long dataQualityCategoryId;
  private Long dataQualitySeverityId;
  private Long dataQualityIssueStatusId;
  private Long dataQualityDetectionSourceId;
  private String entityType;
  private Long entityId;
  private String issueSummary;
  private LocalDateTime detectedAt;
  private Long assignedUserId;

  public CreateDataQualityIssueRequest() {}

  public CreateDataQualityIssueRequest(
      Long dataQualityCategoryId,
      Long dataQualitySeverityId,
      Long dataQualityDetectionSourceId,
      String entityType,
      String issueSummary) {
    this.dataQualityCategoryId = dataQualityCategoryId;
    this.dataQualitySeverityId = dataQualitySeverityId;
    this.dataQualityDetectionSourceId = dataQualityDetectionSourceId;
    this.entityType = entityType;
    this.issueSummary = issueSummary;
  }

  public Long getDataQualityRuleId() {
    return dataQualityRuleId;
  }

  public void setDataQualityRuleId(Long dataQualityRuleId) {
    this.dataQualityRuleId = dataQualityRuleId;
  }

  public Long getDataQualityCategoryId() {
    return dataQualityCategoryId;
  }

  public void setDataQualityCategoryId(Long dataQualityCategoryId) {
    this.dataQualityCategoryId = dataQualityCategoryId;
  }

  public Long getDataQualitySeverityId() {
    return dataQualitySeverityId;
  }

  public void setDataQualitySeverityId(Long dataQualitySeverityId) {
    this.dataQualitySeverityId = dataQualitySeverityId;
  }

  public Long getDataQualityIssueStatusId() {
    return dataQualityIssueStatusId;
  }

  public void setDataQualityIssueStatusId(Long dataQualityIssueStatusId) {
    this.dataQualityIssueStatusId = dataQualityIssueStatusId;
  }

  public Long getDataQualityDetectionSourceId() {
    return dataQualityDetectionSourceId;
  }

  public void setDataQualityDetectionSourceId(Long dataQualityDetectionSourceId) {
    this.dataQualityDetectionSourceId = dataQualityDetectionSourceId;
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
}
