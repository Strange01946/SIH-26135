package in.gov.sih.sih26135.dto.request;

import java.time.LocalDateTime;

public class UpdateDataQualityIssueRequest {

  private Long dataQualityCategoryId;
  private Long dataQualitySeverityId;
  private Long dataQualityIssueStatusId;
  private Long dataQualityDetectionSourceId;
  private String issueSummary;
  private Long assignedUserId;
  private LocalDateTime resolvedAt;
  private Long resolvedByUserId;
  private String resolutionNotes;

  public UpdateDataQualityIssueRequest() {}

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

  public String getIssueSummary() {
    return issueSummary;
  }

  public void setIssueSummary(String issueSummary) {
    this.issueSummary = issueSummary;
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
}
