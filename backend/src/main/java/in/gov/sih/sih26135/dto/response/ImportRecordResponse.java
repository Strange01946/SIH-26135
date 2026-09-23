package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public record ImportRecordResponse(
    Long id,
    Long importBatchId,
    String importBatchCode,
    Integer sourceRowNumber,
    String entityType,
    Long entityId,
    Long importRecordStatusId,
    String importRecordStatusCode,
    String importRecordStatusName,
    String errorCode,
    String errorMessage,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
