package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.CourseOutcomeSummaryResponse;
import in.gov.sih.sih26135.entity.analytics.CourseOutcomeSummary;
import org.springframework.stereotype.Component;

@Component
public class CourseOutcomeSummaryMapper {

  public CourseOutcomeSummaryResponse toResponse(CourseOutcomeSummary entity) {
    if (entity == null) {
      return null;
    }
    return new CourseOutcomeSummaryResponse(
        entity.getCourseId(),
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
