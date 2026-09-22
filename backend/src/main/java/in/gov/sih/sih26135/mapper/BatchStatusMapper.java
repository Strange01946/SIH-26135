package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.BatchStatusResponse;
import in.gov.sih.sih26135.entity.RefBatchStatus;
import org.springframework.stereotype.Component;

@Component
public class BatchStatusMapper {

  public BatchStatusResponse toResponse(RefBatchStatus entity) {
    if (entity == null) {
      return null;
    }

    return new BatchStatusResponse(
        entity.getId(),
        entity.getStatusCode(),
        entity.getStatusName(),
        entity.getSortOrder(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
