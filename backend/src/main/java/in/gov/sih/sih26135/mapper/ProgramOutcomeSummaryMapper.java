package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.ProgramOutcomeSummaryResponse;
import in.gov.sih.sih26135.entity.analytics.ProgramOutcomeSummary;
import org.springframework.stereotype.Component;

@Component
public class ProgramOutcomeSummaryMapper {

  public ProgramOutcomeSummaryResponse toResponse(ProgramOutcomeSummary entity) {
    if (entity == null) {
      return null;
    }
    return new ProgramOutcomeSummaryResponse(
        entity.getProgramId(),
        entity.getSchemeId(),
        entity.getEnrollmentCount(),
        entity.getTraineeCount(),
        entity.getCompletedCount(),
        entity.getCertifiedCount(),
        entity.getJoinedPlacementCount(),
        entity.getEmployedCount(),
        entity.getSelfEmploymentSpellCount(),
        entity.getApprenticeshipSpellCount(),
        entity.getCompletionRatePct(),
        entity.getCertificationRatePct(),
        entity.getPlacementRatePct(),
        entity.getEmploymentRatePct()
    );
  }
}
