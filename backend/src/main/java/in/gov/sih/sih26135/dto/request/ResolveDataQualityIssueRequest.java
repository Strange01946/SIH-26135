package in.gov.sih.sih26135.dto.request;

import java.time.LocalDateTime;

public class ResolveDataQualityIssueRequest {

  private Long resolvedByUserId;
  private String resolutionNotes;
  private LocalDateTime resolvedAt;
  private Boolean isDismissed = false;

  public ResolveDataQualityIssueRequest() {}

  public ResolveDataQualityIssueRequest(Long resolvedByUserId, String resolutionNotes, Boolean isDismissed) {
    this.resolvedByUserId = resolvedByUserId;
    this.resolutionNotes = resolutionNotes;
    this.isDismissed = isDismissed;
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

  public LocalDateTime getResolvedAt() {
    return resolvedAt;
  }

  public void setResolvedAt(LocalDateTime resolvedAt) {
    this.resolvedAt = resolvedAt;
  }

  public Boolean getIsDismissed() {
    return isDismissed;
  }

  public void setIsDismissed(Boolean isDismissed) {
    this.isDismissed = isDismissed;
  }
}
