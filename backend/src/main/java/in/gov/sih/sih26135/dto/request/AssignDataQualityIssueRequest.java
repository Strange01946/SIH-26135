package in.gov.sih.sih26135.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.Objects;

/**
 * Request payload for assigning a data quality issue to an operational reviewer.
 */
public class AssignDataQualityIssueRequest {

  @NotNull(message = "assignedUserId is required")
  @Positive(message = "assignedUserId must be positive")
  private Long assignedUserId;

  @Size(max = 500, message = "remarks cannot exceed 500 characters")
  private String remarks;

  public AssignDataQualityIssueRequest() {}

  public AssignDataQualityIssueRequest(Long assignedUserId) {
    this.assignedUserId = assignedUserId;
  }

  public AssignDataQualityIssueRequest(Long assignedUserId, String remarks) {
    this.assignedUserId = assignedUserId;
    this.remarks = remarks;
  }

  public Long getAssignedUserId() {
    return assignedUserId;
  }

  public void setAssignedUserId(Long assignedUserId) {
    this.assignedUserId = assignedUserId;
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
    AssignDataQualityIssueRequest that = (AssignDataQualityIssueRequest) o;
    return Objects.equals(assignedUserId, that.assignedUserId) &&
        Objects.equals(remarks, that.remarks);
  }

  @Override
  public int hashCode() {
    return Objects.hash(assignedUserId, remarks);
  }

  @Override
  public String toString() {
    return "AssignDataQualityIssueRequest{" +
        "assignedUserId=" + assignedUserId +
        ", remarks='" + remarks + '\'' +
        '}';
  }
}
