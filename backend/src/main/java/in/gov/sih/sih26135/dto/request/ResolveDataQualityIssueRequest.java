package in.gov.sih.sih26135.dto.request;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Request payload for resolving or dismissing a reported data quality issue.
 */
public class ResolveDataQualityIssueRequest {

  @Positive(message = "resolvedByUserId must be positive")
  private Long resolvedByUserId;

  @Size(max = 500, message = "resolutionNotes cannot exceed 500 characters")
  private String resolutionNotes;

  private LocalDateTime resolvedAt;

  private Boolean isDismissed = false;

  public ResolveDataQualityIssueRequest() {}

  public ResolveDataQualityIssueRequest(Long resolvedByUserId, String resolutionNotes, Boolean isDismissed) {
    this.resolvedByUserId = resolvedByUserId;
    this.resolutionNotes = resolutionNotes;
    this.isDismissed = isDismissed != null ? isDismissed : false;
  }

  public ResolveDataQualityIssueRequest(Long resolvedByUserId, String resolutionNotes, LocalDateTime resolvedAt, Boolean isDismissed) {
    this.resolvedByUserId = resolvedByUserId;
    this.resolutionNotes = resolutionNotes;
    this.resolvedAt = resolvedAt;
    this.isDismissed = isDismissed != null ? isDismissed : false;
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
    this.isDismissed = isDismissed != null ? isDismissed : false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ResolveDataQualityIssueRequest that = (ResolveDataQualityIssueRequest) o;
    return Objects.equals(resolvedByUserId, that.resolvedByUserId) &&
        Objects.equals(resolutionNotes, that.resolutionNotes) &&
        Objects.equals(resolvedAt, that.resolvedAt) &&
        Objects.equals(isDismissed, that.isDismissed);
  }

  @Override
  public int hashCode() {
    return Objects.hash(resolvedByUserId, resolutionNotes, resolvedAt, isDismissed);
  }

  @Override
  public String toString() {
    return "ResolveDataQualityIssueRequest{" +
        "resolvedByUserId=" + resolvedByUserId +
        ", resolutionNotes='" + resolutionNotes + '\'' +
        ", resolvedAt=" + resolvedAt +
        ", isDismissed=" + isDismissed +
        '}';
  }
}
