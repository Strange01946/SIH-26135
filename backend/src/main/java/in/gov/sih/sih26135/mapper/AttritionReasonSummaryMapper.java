package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.AttritionReasonSummaryResponse;
import in.gov.sih.sih26135.entity.analytics.AttritionReasonSummary;
import org.springframework.stereotype.Component;

@Component
public class AttritionReasonSummaryMapper {

  public AttritionReasonSummaryResponse toResponse(AttritionReasonSummary entity) {
    if (entity == null) {
      return null;
    }
    return new AttritionReasonSummaryResponse(
        entity.getEmploymentExitReasonId(),
        entity.getExitReasonCode(),
        entity.getSeparationNatureId(),
        entity.getSeparationNatureCode(),
        entity.getIsVoluntaryFlag(),
        entity.getIsInvoluntaryFlag(),
        entity.getExitCount(),
        entity.getTraineeCount()
    );
  }
}
