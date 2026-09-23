package in.gov.sih.sih26135.dto.response;

import java.util.List;

public record TraineeEngagementHistoryResponse(
    Long traineeId,
    List<FollowupTaskResponse> followupTasks,
    List<SurveyResponseResponse> surveyResponses
) {}
