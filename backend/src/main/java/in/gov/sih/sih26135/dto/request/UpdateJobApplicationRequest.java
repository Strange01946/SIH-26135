package in.gov.sih.sih26135.dto.request;

public class UpdateJobApplicationRequest {

  private Long applicationStatusId;
  private Long nonSelectionReasonId;
  private String remarks;

  public UpdateJobApplicationRequest() {
  }

  public UpdateJobApplicationRequest(Long applicationStatusId) {
    this.applicationStatusId = applicationStatusId;
  }

  public Long getApplicationStatusId() {
    return applicationStatusId;
  }

  public void setApplicationStatusId(Long applicationStatusId) {
    this.applicationStatusId = applicationStatusId;
  }

  public Long getNonSelectionReasonId() {
    return nonSelectionReasonId;
  }

  public void setNonSelectionReasonId(Long nonSelectionReasonId) {
    this.nonSelectionReasonId = nonSelectionReasonId;
  }

  public String getRemarks() {
    return remarks;
  }

  public void setRemarks(String remarks) {
    this.remarks = remarks;
  }
}
