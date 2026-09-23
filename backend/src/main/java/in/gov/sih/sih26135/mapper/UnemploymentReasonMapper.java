package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.UnemploymentReasonResponse;
import in.gov.sih.sih26135.entity.RefUnemploymentReason;
import org.springframework.stereotype.Component;

@Component
public class UnemploymentReasonMapper {

  public UnemploymentReasonResponse toResponse(RefUnemploymentReason entity) {
    if (entity == null) {
      return null;
    }
    return new UnemploymentReasonResponse(
        entity.getId(),
        entity.getReasonCode(),
        entity.getReasonName(),
        entity.getSortOrder(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
