package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.AssessmentOutcomeResponse;
import in.gov.sih.sih26135.entity.RefAssessmentOutcome;
import org.springframework.stereotype.Component;

@Component
public class AssessmentOutcomeMapper {

  public AssessmentOutcomeResponse toResponse(RefAssessmentOutcome entity) {
    if (entity == null) {
      return null;
    }

    return new AssessmentOutcomeResponse(
        entity.getId(),
        entity.getOutcomeCode(),
        entity.getOutcomeName(),
        entity.getIsPassFlag(),
        entity.getSortOrder(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
