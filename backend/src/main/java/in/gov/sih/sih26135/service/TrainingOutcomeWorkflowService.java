package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.RecordAssessmentAndIssueCertificateRequest;
import in.gov.sih.sih26135.dto.response.RecordAssessmentAndIssueCertificateResponse;
import in.gov.sih.sih26135.dto.response.TraineeTrainingOutcomeSummaryResponse;

public interface TrainingOutcomeWorkflowService {

  RecordAssessmentAndIssueCertificateResponse recordAssessmentResultAndIssueCertificate(
      RecordAssessmentAndIssueCertificateRequest request);

  TraineeTrainingOutcomeSummaryResponse getTraineeTrainingOutcomeSummary(
      Long traineeId, Long enrollmentId);
}
