package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.FollowupFactResponse;
import in.gov.sih.sih26135.entity.analytics.FollowupFact;
import org.springframework.stereotype.Component;

@Component
public class FollowupFactMapper {

  public FollowupFactResponse toResponse(FollowupFact entity) {
    if (entity == null) {
      return null;
    }
    return new FollowupFactResponse(
        entity.getFollowupTaskId(),
        entity.getFollowupCampaignId(),
        entity.getFollowupTypeId(),
        entity.getFollowupTypeCode(),
        entity.getFollowupOffsetMonths(),
        entity.getTraineeId(),
        entity.getEnrollmentId(),
        entity.getPlacementId(),
        entity.getEmploymentId(),
        entity.getSurveyId(),
        entity.getScheduledDate(),
        entity.getNextFollowupDate(),
        entity.getFollowupStatusId(),
        entity.getFollowupStatusCode(),
        entity.getIsOpenFlag(),
        entity.getFollowupOutcomeId(),
        entity.getFollowupOutcomeCode(),
        entity.getIsSuccessFlag(),
        entity.getIsUnreachableFlag(),
        entity.getIsNoResponseFlag(),
        entity.getNonResponseReasonId(),
        entity.getLastChannelId(),
        entity.getLastChannelCode(),
        entity.getTraineeStateId(),
        entity.getTraineeDistrictId()
    );
  }
}
