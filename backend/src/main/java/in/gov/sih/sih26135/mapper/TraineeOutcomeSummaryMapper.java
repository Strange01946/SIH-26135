package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.TraineeOutcomeSummaryResponse;
import in.gov.sih.sih26135.entity.analytics.TraineeOutcomeSummary;
import org.springframework.stereotype.Component;

@Component
public class TraineeOutcomeSummaryMapper {

  public TraineeOutcomeSummaryResponse toResponse(TraineeOutcomeSummary entity) {
    if (entity == null) {
      return null;
    }
    return new TraineeOutcomeSummaryResponse(
        entity.getTraineeId(),
        entity.getStateId(),
        entity.getDistrictId(),
        entity.getCurrentEmploymentStatusId(),
        entity.getSnapshotEmploymentStatusCode(),
        entity.getSnapshotIsEmployedFlag(),
        entity.getEnrollmentCount(),
        entity.getCompletedEnrollmentCount(),
        entity.getCertifiedEnrollmentCount(),
        entity.getPlacedEnrollmentCount(),
        entity.getEmployedEnrollmentCount(),
        entity.getEmploymentSpellCount(),
        entity.getCurrentEmploymentCount(),
        entity.getCurrentWageEmploymentCount(),
        entity.getCurrentSelfEmploymentCount(),
        entity.getUnemploymentPeriodCount(),
        entity.getCurrentUnemploymentCount(),
        entity.getCurrentSkillGapCount(),
        entity.getCompletionRatePct()
    );
  }
}
