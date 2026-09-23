package in.gov.sih.sih26135.dto.response;

public record RecordEngagementAttemptResponse(
    CommunicationLogResponse communicationLog,
    FollowupTaskResponse updatedFollowupTask
) {}
