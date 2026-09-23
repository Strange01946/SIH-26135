package in.gov.sih.sih26135.dto.request;

import java.util.ArrayList;
import java.util.List;

public class RecordVerificationVerdictRequest {

  private CreateEmploymentVerificationRequest verificationRequest;
  private List<CreateEmploymentVerificationEvidenceRequest> evidenceRequests = new ArrayList<>();
  private Long requestStatusId;
  private Long employmentRecordStatusId;

  public RecordVerificationVerdictRequest() {
  }

  public RecordVerificationVerdictRequest(
      CreateEmploymentVerificationRequest verificationRequest,
      List<CreateEmploymentVerificationEvidenceRequest> evidenceRequests,
      Long requestStatusId,
      Long employmentRecordStatusId) {
    this.verificationRequest = verificationRequest;
    if (evidenceRequests != null) {
      this.evidenceRequests = evidenceRequests;
    }
    this.requestStatusId = requestStatusId;
    this.employmentRecordStatusId = employmentRecordStatusId;
  }

  public CreateEmploymentVerificationRequest getVerificationRequest() {
    return verificationRequest;
  }

  public void setVerificationRequest(CreateEmploymentVerificationRequest verificationRequest) {
    this.verificationRequest = verificationRequest;
  }

  public List<CreateEmploymentVerificationEvidenceRequest> getEvidenceRequests() {
    return evidenceRequests;
  }

  public void setEvidenceRequests(List<CreateEmploymentVerificationEvidenceRequest> evidenceRequests) {
    this.evidenceRequests = evidenceRequests != null ? evidenceRequests : new ArrayList<>();
  }

  public Long getRequestStatusId() {
    return requestStatusId;
  }

  public void setRequestStatusId(Long requestStatusId) {
    this.requestStatusId = requestStatusId;
  }

  public Long getEmploymentRecordStatusId() {
    return employmentRecordStatusId;
  }

  public void setEmploymentRecordStatusId(Long employmentRecordStatusId) {
    this.employmentRecordStatusId = employmentRecordStatusId;
  }
}
