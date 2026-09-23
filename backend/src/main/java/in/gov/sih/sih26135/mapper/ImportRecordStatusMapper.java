package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.ImportRecordStatusResponse;
import in.gov.sih.sih26135.entity.RefImportRecordStatus;
import org.springframework.stereotype.Component;

@Component
public class ImportRecordStatusMapper {

  public ImportRecordStatusResponse toResponse(RefImportRecordStatus entity) {
    if (entity == null) {
      return null;
    }
    return new ImportRecordStatusResponse(
        entity.getId(),
        entity.getStatusCode(),
        entity.getStatusName(),
        entity.getSortOrder(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
