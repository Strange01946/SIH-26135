package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.ImportBatchResponse;
import in.gov.sih.sih26135.entity.ImportBatch;
import org.springframework.stereotype.Component;

@Component
public class ImportBatchMapper {

  public ImportBatchResponse toResponse(ImportBatch entity) {
    if (entity == null) {
      return null;
    }

    Long statusId = null;
    String statusCode = null;
    String statusName = null;
    Boolean isTerminalFlag = null;
    if (entity.getImportBatchStatus() != null) {
      statusId = entity.getImportBatchStatus().getId();
      statusCode = entity.getImportBatchStatus().getStatusCode();
      statusName = entity.getImportBatchStatus().getStatusName();
      isTerminalFlag = entity.getImportBatchStatus().getIsTerminalFlag();
    }

    return new ImportBatchResponse(
        entity.getId(),
        entity.getBatchCode(),
        entity.getSourceSystem(),
        entity.getEntityType(),
        statusId,
        statusCode,
        statusName,
        isTerminalFlag,
        entity.getInitiatedByUserId(),
        entity.getRowCount(),
        entity.getSuccessCount(),
        entity.getFailureCount(),
        entity.getStartedAt(),
        entity.getCompletedAt(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
