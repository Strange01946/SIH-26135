package in.gov.sih.sih26135.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Request payload for resolving or dismissing a reported data quality issue.
 * Attribution fields (resolvedByUserId, resolvedAt) are restricted from client JSON input
 * (marked READ_ONLY) and must be set server-side by the authenticated resolution workflow.
 */
public class ResolveDataQualityIssueRequest {

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  @Positive(message = "resolvedByUserId must be positive")
  private Long resolvedByUserId;

  @NotBlank(message = "resolutionNotes cannot be blank")
  @Size(max = 500, message = "resolutionNotes cannot exceed 500 characters")
  private String resolutionNotes;

  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private LocalDateTime resolvedAt;

  private Boolean isDismissed = false;

  public ResolveDataQualityIssueRequest() {}

  public ResolveDataQualityIssueRequest(String resolutionNotes) {
    this.resolutionNotes = resolutionNotes;
    this.isDismissed = false;
  }

  public ResolveDataQualityIssueRequest(String resolutionNotes, Boolean isDismissed) {
    this.resolutionNotes = resolutionNotes;
    this.isDismissed = isDismissed != null ? isDismissed : false;
  }

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

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
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

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
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
