package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.RecordVerificationStatusResponse;
import in.gov.sih.sih26135.entity.RefRecordVerificationStatus;
import org.springframework.stereotype.Component;

@Component
public class RecordVerificationStatusMapper {

  public RecordVerificationStatusResponse toResponse(RefRecordVerificationStatus entity) {
    if (entity == null) {
      return null;
    }
    return new RecordVerificationStatusResponse(
        entity.getId(),
        entity.getStatusCode(),
        entity.getStatusName(),
        entity.getIsVerifiedFlag(),
        entity.getSortOrder(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
