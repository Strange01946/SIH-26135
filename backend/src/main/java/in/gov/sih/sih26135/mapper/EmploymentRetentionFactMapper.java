package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.EmploymentRetentionFactResponse;
import in.gov.sih.sih26135.entity.analytics.EmploymentRetentionFact;
import org.springframework.stereotype.Component;

@Component
public class EmploymentRetentionFactMapper {

  public EmploymentRetentionFactResponse toResponse(EmploymentRetentionFact entity) {
    if (entity == null) {
      return null;
    }
    return new EmploymentRetentionFactResponse(
        entity.getEmploymentId(),
        entity.getTraineeId(),
        entity.getEnrollmentId(),
        entity.getEngagementTypeId(),
        entity.getStartDate(),
        entity.getEndDate(),
        entity.getIsCurrent(),
        entity.getRetained6mFlag(),
        entity.getRetained12mFlag(),
        entity.getRetained24mFlag(),
        entity.getRetained36mFlag()
    );
  }
}
