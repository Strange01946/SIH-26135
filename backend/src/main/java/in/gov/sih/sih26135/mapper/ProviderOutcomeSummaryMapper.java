package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.ProviderOutcomeSummaryResponse;
import in.gov.sih.sih26135.entity.analytics.ProviderOutcomeSummary;
import org.springframework.stereotype.Component;

@Component
public class ProviderOutcomeSummaryMapper {

  public ProviderOutcomeSummaryResponse toResponse(ProviderOutcomeSummary entity) {
    if (entity == null) {
      return null;
    }
    return new ProviderOutcomeSummaryResponse(
        entity.getProviderId(),
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
