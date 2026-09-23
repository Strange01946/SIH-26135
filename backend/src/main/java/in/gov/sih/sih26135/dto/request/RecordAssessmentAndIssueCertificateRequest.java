package in.gov.sih.sih26135.dto.request;

public class RecordAssessmentAndIssueCertificateRequest {

  private CreateAssessmentResultRequest assessmentResultRequest;
  private CreateCertificationRequest certificationRequest;
  private Long enrollmentId;
  private Long completionEnrollmentStatusId;

  public RecordAssessmentAndIssueCertificateRequest() {
  }

  public RecordAssessmentAndIssueCertificateRequest(
      CreateAssessmentResultRequest assessmentResultRequest,
      CreateCertificationRequest certificationRequest,
      Long enrollmentId,
      Long completionEnrollmentStatusId) {
    this.assessmentResultRequest = assessmentResultRequest;
    this.certificationRequest = certificationRequest;
    this.enrollmentId = enrollmentId;
    this.completionEnrollmentStatusId = completionEnrollmentStatusId;
  }

  public CreateAssessmentResultRequest getAssessmentResultRequest() {
    return assessmentResultRequest;
  }

  public void setAssessmentResultRequest(CreateAssessmentResultRequest assessmentResultRequest) {
    this.assessmentResultRequest = assessmentResultRequest;
  }

  public CreateCertificationRequest getCertificationRequest() {
    return certificationRequest;
  }

  public void setCertificationRequest(CreateCertificationRequest certificationRequest) {
    this.certificationRequest = certificationRequest;
  }

  public Long getEnrollmentId() {
    return enrollmentId;
  }

  public void setEnrollmentId(Long enrollmentId) {
    this.enrollmentId = enrollmentId;
  }

  public Long getCompletionEnrollmentStatusId() {
    return completionEnrollmentStatusId;
  }

  public void setCompletionEnrollmentStatusId(Long completionEnrollmentStatusId) {
    this.completionEnrollmentStatusId = completionEnrollmentStatusId;
  }
}
