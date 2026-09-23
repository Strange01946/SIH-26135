package in.gov.sih.sih26135.dto.response;

import java.time.LocalDate;

public record FollowupFactResponse(
    Long followupTaskId,
    Long followupCampaignId,
    Long followupTypeId,
    String followupTypeCode,
    Integer followupOffsetMonths,
    Long traineeId,
    Long enrollmentId,
    Long placementId,
    Long employmentId,
    Long surveyId,
    LocalDate scheduledDate,
    LocalDate nextFollowupDate,
    Long followupStatusId,
    String followupStatusCode,
    Boolean isOpenFlag,
    Long followupOutcomeId,
    String followupOutcomeCode,
    Boolean isSuccessFlag,
    Boolean isUnreachableFlag,
    Boolean isNoResponseFlag,
    Long nonResponseReasonId,
    Long lastChannelId,
    String lastChannelCode,
    Long traineeStateId,
    Long traineeDistrictId
) {}
