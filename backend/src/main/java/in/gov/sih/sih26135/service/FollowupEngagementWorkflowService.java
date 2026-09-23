package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.RecordEngagementAttemptRequest;
import in.gov.sih.sih26135.dto.request.RecordFollowupSurveyResponseRequest;
import in.gov.sih.sih26135.dto.response.RecordEngagementAttemptResponse;
import in.gov.sih.sih26135.dto.response.RecordFollowupSurveyResponseResponse;
import in.gov.sih.sih26135.dto.response.TraineeEngagementHistoryResponse;

public interface FollowupEngagementWorkflowService {

  RecordFollowupSurveyResponseResponse recordFollowupSurveyResponse(
      RecordFollowupSurveyResponseRequest request);

  RecordEngagementAttemptResponse recordEngagementAttempt(
      RecordEngagementAttemptRequest request);

  TraineeEngagementHistoryResponse getTraineeEngagementHistory(
      Long traineeId);
}
