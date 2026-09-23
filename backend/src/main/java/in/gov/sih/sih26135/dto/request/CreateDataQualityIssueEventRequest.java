package in.gov.sih.sih26135.dto.request;

import java.time.LocalDateTime;

public class CreateDataQualityIssueEventRequest {

  private Long dataQualityIssueId;
  private Long dataQualityIssueStatusId;
  private Long changedByUserId;
  private LocalDateTime changedAt;
  private String remarks;

  public CreateDataQualityIssueEventRequest() {}

  public CreateDataQualityIssueEventRequest(
      Long dataQualityIssueId,
      Long dataQualityIssueStatusId,
      Long changedByUserId,
      String remarks) {
    this.dataQualityIssueId = dataQualityIssueId;
    this.dataQualityIssueStatusId = dataQualityIssueStatusId;
    this.changedByUserId = changedByUserId;
    this.remarks = remarks;
  }

  public Long getDataQualityIssueId() {
    return dataQualityIssueId;
  }

  public void setDataQualityIssueId(Long dataQualityIssueId) {
    this.dataQualityIssueId = dataQualityIssueId;
  }

  public Long getDataQualityIssueStatusId() {
    return dataQualityIssueStatusId;
  }

  public void setDataQualityIssueStatusId(Long dataQualityIssueStatusId) {
    this.dataQualityIssueStatusId = dataQualityIssueStatusId;
  }

  public Long getChangedByUserId() {
    return changedByUserId;
  }

  public void setChangedByUserId(Long changedByUserId) {
    this.changedByUserId = changedByUserId;
  }

  public LocalDateTime getChangedAt() {
    return changedAt;
  }

  public void setChangedAt(LocalDateTime changedAt) {
    this.changedAt = changedAt;
  }

  public String getRemarks() {
    return remarks;
  }

  public void setRemarks(String remarks) {
    this.remarks = remarks;
  }
}
