package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.NonSelectionReasonResponse;
import in.gov.sih.sih26135.entity.RefNonSelectionReason;
import org.springframework.stereotype.Component;

@Component
public class NonSelectionReasonMapper {

  public NonSelectionReasonResponse toResponse(RefNonSelectionReason entity) {
    if (entity == null) {
      return null;
    }
    return new NonSelectionReasonResponse(
        entity.getId(),
        entity.getReasonCode(),
        entity.getReasonName(),
        entity.getSortOrder(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
