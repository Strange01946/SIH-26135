package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.InitiateVerificationFromSurveyRequest;
import in.gov.sih.sih26135.dto.request.RecordVerificationVerdictRequest;
import in.gov.sih.sih26135.dto.response.EmploymentVerificationRequestResponse;
import in.gov.sih.sih26135.dto.response.EmploymentVerificationSummaryResponse;
import in.gov.sih.sih26135.dto.response.RecordVerificationVerdictResponse;

public interface VerificationWorkflowService {

  EmploymentVerificationRequestResponse initiateVerificationFromSurvey(
      InitiateVerificationFromSurveyRequest request);

  RecordVerificationVerdictResponse recordVerificationVerdictAndSynchronize(
      RecordVerificationVerdictRequest request);

  EmploymentVerificationSummaryResponse getEmploymentVerificationSummary(
      Long employmentId);
}
