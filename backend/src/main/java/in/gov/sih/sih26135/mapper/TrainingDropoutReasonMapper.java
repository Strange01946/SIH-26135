package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.TrainingDropoutReasonResponse;
import in.gov.sih.sih26135.entity.RefTrainingDropoutReason;
import org.springframework.stereotype.Component;

@Component
public class TrainingDropoutReasonMapper {

  public TrainingDropoutReasonResponse toResponse(RefTrainingDropoutReason entity) {
    if (entity == null) {
      return null;
    }

    return new TrainingDropoutReasonResponse(
        entity.getId(),
        entity.getReasonCode(),
        entity.getReasonName(),
        entity.getSortOrder(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
