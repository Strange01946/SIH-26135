package in.gov.sih.sih26135.dto.request;

import jakarta.validation.constraints.Size;
import java.util.Objects;

/**
 * Request payload for transitioning an assigned data quality issue into active investigation.
 */
public class StartInvestigationRequest {

  @Size(max = 500, message = "remarks cannot exceed 500 characters")
  private String remarks;

  public StartInvestigationRequest() {}

  public StartInvestigationRequest(String remarks) {
    this.remarks = remarks;
  }

  public String getRemarks() {
    return remarks;
  }

  public void setRemarks(String remarks) {
    this.remarks = remarks;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    StartInvestigationRequest that = (StartInvestigationRequest) o;
    return Objects.equals(remarks, that.remarks);
  }

  @Override
  public int hashCode() {
    return Objects.hash(remarks);
  }

  @Override
  public String toString() {
    return "StartInvestigationRequest{remarks='" + remarks + "'}";
  }
}
