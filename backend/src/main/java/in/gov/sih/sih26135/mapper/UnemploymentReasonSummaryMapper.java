package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.UnemploymentReasonSummaryResponse;
import in.gov.sih.sih26135.entity.analytics.UnemploymentReasonSummary;
import org.springframework.stereotype.Component;

@Component
public class UnemploymentReasonSummaryMapper {

  public UnemploymentReasonSummaryResponse toResponse(UnemploymentReasonSummary entity) {
    if (entity == null) {
      return null;
    }
    return new UnemploymentReasonSummaryResponse(
        entity.getUnemploymentReasonId(),
        entity.getUnemploymentReasonCode(),
        entity.getPeriodCount(),
        entity.getTraineeCount(),
        entity.getCurrentPeriodCount(),
        entity.getReemployedCount(),
        entity.getAvgDurationDays()
    );
  }
}
