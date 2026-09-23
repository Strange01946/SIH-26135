package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.DistrictOutcomeSummaryResponse;
import in.gov.sih.sih26135.entity.analytics.DistrictOutcomeSummary;
import org.springframework.stereotype.Component;

@Component
public class DistrictOutcomeSummaryMapper {

  public DistrictOutcomeSummaryResponse toResponse(DistrictOutcomeSummary entity) {
    if (entity == null) {
      return null;
    }
    return new DistrictOutcomeSummaryResponse(
        entity.getDistrictId(),
        entity.getEnrollmentCount(),
        entity.getTraineeCount(),
        entity.getCompletedCount(),
        entity.getCertifiedCount(),
        entity.getJoinedPlacementCount(),
        entity.getEmployedCount(),
        entity.getCompletionRatePct(),
        entity.getCertificationRatePct(),
        entity.getPlacementRatePct(),
        entity.getEmploymentRatePct()
    );
  }
}
