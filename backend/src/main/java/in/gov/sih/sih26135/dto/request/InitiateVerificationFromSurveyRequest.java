package in.gov.sih.sih26135.dto.request;

public class InitiateVerificationFromSurveyRequest {

  private Long surveyResponseId;
  private CreateEmploymentVerificationRequestRequest verificationRequestDetails;

  public InitiateVerificationFromSurveyRequest() {
  }

  public InitiateVerificationFromSurveyRequest(
      Long surveyResponseId,
      CreateEmploymentVerificationRequestRequest verificationRequestDetails) {
    this.surveyResponseId = surveyResponseId;
    this.verificationRequestDetails = verificationRequestDetails;
  }

  public Long getSurveyResponseId() {
    return surveyResponseId;
  }

  public void setSurveyResponseId(Long surveyResponseId) {
    this.surveyResponseId = surveyResponseId;
  }

  public CreateEmploymentVerificationRequestRequest getVerificationRequestDetails() {
    return verificationRequestDetails;
  }

  public void setVerificationRequestDetails(
      CreateEmploymentVerificationRequestRequest verificationRequestDetails) {
    this.verificationRequestDetails = verificationRequestDetails;
  }
}
