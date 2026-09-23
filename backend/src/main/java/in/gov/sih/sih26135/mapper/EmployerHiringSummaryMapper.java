package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.EmployerHiringSummaryResponse;
import in.gov.sih.sih26135.entity.analytics.EmployerHiringSummary;
import org.springframework.stereotype.Component;

@Component
public class EmployerHiringSummaryMapper {

  public EmployerHiringSummaryResponse toResponse(EmployerHiringSummary entity) {
    if (entity == null) {
      return null;
    }
    return new EmployerHiringSummaryResponse(
        entity.getEmployerId(),
        entity.getPlacementCount(),
        entity.getTraineeCount(),
        entity.getJoinedCount(),
        entity.getJoinRatePct(),
        entity.getAvgJoiningSalary()
    );
  }
}
