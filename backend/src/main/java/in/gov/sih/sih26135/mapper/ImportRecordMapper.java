package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.ImportRecordResponse;
import in.gov.sih.sih26135.entity.ImportRecord;
import org.springframework.stereotype.Component;

@Component
public class ImportRecordMapper {

  public ImportRecordResponse toResponse(ImportRecord entity) {
    if (entity == null) {
      return null;
    }

    Long batchId = null;
    String batchCode = null;
    if (entity.getImportBatch() != null) {
      batchId = entity.getImportBatch().getId();
      batchCode = entity.getImportBatch().getBatchCode();
    }

    Long statusId = null;
    String statusCode = null;
    String statusName = null;
    if (entity.getImportRecordStatus() != null) {
      statusId = entity.getImportRecordStatus().getId();
      statusCode = entity.getImportRecordStatus().getStatusCode();
      statusName = entity.getImportRecordStatus().getStatusName();
    }

    return new ImportRecordResponse(
        entity.getId(),
        batchId,
        batchCode,
        entity.getSourceRowNumber(),
        entity.getEntityType(),
        entity.getEntityId(),
        statusId,
        statusCode,
        statusName,
        entity.getErrorCode(),
        entity.getErrorMessage(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
