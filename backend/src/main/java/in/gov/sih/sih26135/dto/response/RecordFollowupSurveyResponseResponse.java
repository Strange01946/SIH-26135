package in.gov.sih.sih26135.dto.response;

public record RecordFollowupSurveyResponseResponse(
    SurveyResponseResponse surveyResponse,
    FollowupTaskResponse updatedFollowupTask
) {}
