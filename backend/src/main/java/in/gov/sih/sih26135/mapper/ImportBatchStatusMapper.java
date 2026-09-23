package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.ImportBatchStatusResponse;
import in.gov.sih.sih26135.entity.RefImportBatchStatus;
import org.springframework.stereotype.Component;

@Component
public class ImportBatchStatusMapper {

  public ImportBatchStatusResponse toResponse(RefImportBatchStatus entity) {
    if (entity == null) {
      return null;
    }
    return new ImportBatchStatusResponse(
        entity.getId(),
        entity.getStatusCode(),
        entity.getStatusName(),
        entity.getIsTerminalFlag(),
        entity.getSortOrder(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
