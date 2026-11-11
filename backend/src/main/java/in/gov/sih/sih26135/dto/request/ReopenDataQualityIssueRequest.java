package in.gov.sih.sih26135.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Objects;

/**
 * Request payload for reopening a previously resolved data quality issue with mandatory justification reason.
 */
public class ReopenDataQualityIssueRequest {

  @NotBlank(message = "reopeningReason cannot be blank")
  @Size(max = 500, message = "reopeningReason cannot exceed 500 characters")
  private String reopeningReason;

  public ReopenDataQualityIssueRequest() {}

  public ReopenDataQualityIssueRequest(String reopeningReason) {
    this.reopeningReason = reopeningReason;
  }

  public String getReopeningReason() {
    return reopeningReason;
  }

  public void setReopeningReason(String reopeningReason) {
    this.reopeningReason = reopeningReason;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ReopenDataQualityIssueRequest that = (ReopenDataQualityIssueRequest) o;
    return Objects.equals(reopeningReason, that.reopeningReason);
  }

  @Override
  public int hashCode() {
    return Objects.hash(reopeningReason);
  }

  @Override
  public String toString() {
    return "ReopenDataQualityIssueRequest{reopeningReason='" + reopeningReason + "'}";
  }
}
