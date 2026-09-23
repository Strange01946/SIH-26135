package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.FollowupOutcomeSummaryResponse;
import in.gov.sih.sih26135.entity.analytics.FollowupOutcomeSummary;
import org.springframework.stereotype.Component;

@Component
public class FollowupOutcomeSummaryMapper {

  public FollowupOutcomeSummaryResponse toResponse(FollowupOutcomeSummary entity) {
    if (entity == null) {
      return null;
    }
    return new FollowupOutcomeSummaryResponse(
        entity.getFollowupTypeCode(),
        entity.getFollowupOffsetMonths(),
        entity.getTaskCount(),
        entity.getTraineeCount(),
        entity.getSuccessCount(),
        entity.getNoResponseCount(),
        entity.getUnreachableCount(),
        entity.getSuccessRatePct()
    );
  }
}
