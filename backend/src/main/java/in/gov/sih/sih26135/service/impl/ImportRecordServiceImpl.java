package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.CreateImportRecordRequest;
import in.gov.sih.sih26135.dto.request.UpdateImportRecordRequest;
import in.gov.sih.sih26135.dto.response.ImportRecordResponse;
import in.gov.sih.sih26135.entity.ImportBatch;
import in.gov.sih.sih26135.entity.ImportRecord;
import in.gov.sih.sih26135.entity.RefImportRecordStatus;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.ImportRecordMapper;
import in.gov.sih.sih26135.repository.ImportBatchRepository;
import in.gov.sih.sih26135.repository.ImportRecordRepository;
import in.gov.sih.sih26135.repository.RefImportRecordStatusRepository;
import in.gov.sih.sih26135.service.ImportRecordService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ImportRecordServiceImpl implements ImportRecordService {

  private static final Logger log = LoggerFactory.getLogger(ImportRecordServiceImpl.class);

  private final ImportRecordRepository importRecordRepository;
  private final ImportBatchRepository importBatchRepository;
  private final RefImportRecordStatusRepository refImportRecordStatusRepository;
  private final ImportRecordMapper mapper;

  public ImportRecordServiceImpl(
      ImportRecordRepository importRecordRepository,
      ImportBatchRepository importBatchRepository,
      RefImportRecordStatusRepository refImportRecordStatusRepository,
      ImportRecordMapper mapper) {
    this.importRecordRepository = importRecordRepository;
    this.importBatchRepository = importBatchRepository;
    this.refImportRecordStatusRepository = refImportRecordStatusRepository;
    this.mapper = mapper;
  }

  @Override
  @Transactional
  public ImportRecordResponse createImportRecord(CreateImportRecordRequest request) {
    if (request == null) {
      throw new BadRequestException("Request body cannot be null", "REQUEST_BODY_NULL");
    }
    if (request.getImportBatchId() == null) {
      throw new BadRequestException("Import batch ID is required", "BATCH_ID_REQUIRED");
    }
    if (request.getSourceRowNumber() == null) {
      throw new BadRequestException("Source row number is required", "SOURCE_ROW_NUMBER_REQUIRED");
    }
    if (request.getSourceRowNumber() < 1) {
      throw new BadRequestException("Source row number must be greater than or equal to 1", "INVALID_SOURCE_ROW_NUMBER");
    }
    if (request.getImportRecordStatusId() == null) {
      throw new BadRequestException("Import record status ID is required", "STATUS_ID_REQUIRED");
    }

    ImportBatch batch = importBatchRepository.findById(request.getImportBatchId())
        .orElseThrow(() -> new ResourceNotFoundException("ImportBatch", "id"));

    if (importRecordRepository.existsByImportBatchIdAndSourceRowNumber(batch.getId(), request.getSourceRowNumber())) {
      throw new ConflictException("Row number " + request.getSourceRowNumber() + " already exists in import batch", "IMPORT_RECORD_ROW_EXISTS");
    }

    RefImportRecordStatus status = refImportRecordStatusRepository.findById(request.getImportRecordStatusId())
        .orElseThrow(() -> new ResourceNotFoundException("RefImportRecordStatus", "id"));

    if (request.getEntityType() != null && request.getEntityType().trim().length() > 64) {
      throw new BadRequestException("Entity type must not exceed 64 characters", "ENTITY_TYPE_TOO_LONG");
    }
    if (request.getErrorCode() != null && request.getErrorCode().trim().length() > 64) {
      throw new BadRequestException("Error code must not exceed 64 characters", "ERROR_CODE_TOO_LONG");
    }
    if (request.getErrorMessage() != null && request.getErrorMessage().trim().length() > 500) {
      throw new BadRequestException("Error message must not exceed 500 characters", "ERROR_MESSAGE_TOO_LONG");
    }

    ImportRecord record = new ImportRecord(batch, request.getSourceRowNumber(), status);
    record.setEntityType(request.getEntityType() != null ? request.getEntityType().trim() : null);
    record.setEntityId(request.getEntityId());
    record.setErrorCode(request.getErrorCode() != null ? request.getErrorCode().trim() : null);
    record.setErrorMessage(request.getErrorMessage() != null ? request.getErrorMessage().trim() : null);

    ImportRecord saved = importRecordRepository.save(record);
    log.info("Created import record id={}, batchId={}, rowNumber={}", saved.getId(), batch.getId(), saved.getSourceRowNumber());
    return mapper.toResponse(saved);
  }

  @Override
  @Transactional
  public ImportRecordResponse updateImportRecord(Long id, UpdateImportRecordRequest request) {
    if (id == null) {
      throw new BadRequestException("Record ID is required", "ID_REQUIRED");
    }
    if (request == null) {
      throw new BadRequestException("Request body cannot be null", "REQUEST_BODY_NULL");
    }

    ImportRecord record = importRecordRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("ImportRecord", "id"));

    if (request.getSourceRowNumber() != null) {
      if (request.getSourceRowNumber() < 1) {
        throw new BadRequestException("Source row number must be greater than or equal to 1", "INVALID_SOURCE_ROW_NUMBER");
      }
      if (!request.getSourceRowNumber().equals(record.getSourceRowNumber())) {
        Optional<ImportRecord> existingWithRow = importRecordRepository.findByImportBatchIdAndSourceRowNumber(
            record.getImportBatch().getId(), request.getSourceRowNumber());
        if (existingWithRow.isPresent() && !existingWithRow.get().getId().equals(record.getId())) {
          throw new ConflictException("Row number " + request.getSourceRowNumber() + " already exists in import batch", "IMPORT_RECORD_ROW_EXISTS");
        }
        record.setSourceRowNumber(request.getSourceRowNumber());
      }
    }

    if (request.getImportRecordStatusId() != null) {
      RefImportRecordStatus status = refImportRecordStatusRepository.findById(request.getImportRecordStatusId())
          .orElseThrow(() -> new ResourceNotFoundException("RefImportRecordStatus", "id"));
      record.setImportRecordStatus(status);
    }

    if (request.getEntityType() != null) {
      if (request.getEntityType().trim().length() > 64) {
        throw new BadRequestException("Entity type must not exceed 64 characters", "ENTITY_TYPE_TOO_LONG");
      }
      record.setEntityType(request.getEntityType().trim());
    }

    if (request.getEntityId() != null) {
      record.setEntityId(request.getEntityId());
    }

    if (request.getErrorCode() != null) {
      if (request.getErrorCode().trim().length() > 64) {
        throw new BadRequestException("Error code must not exceed 64 characters", "ERROR_CODE_TOO_LONG");
      }
      record.setErrorCode(request.getErrorCode().trim());
    }

    if (request.getErrorMessage() != null) {
      if (request.getErrorMessage().trim().length() > 500) {
        throw new BadRequestException("Error message must not exceed 500 characters", "ERROR_MESSAGE_TOO_LONG");
      }
      record.setErrorMessage(request.getErrorMessage().trim());
    }

    record.setUpdatedAt(LocalDateTime.now());
    ImportRecord saved = importRecordRepository.save(record);
    log.info("Updated import record id={}, batchId={}, rowNumber={}", saved.getId(), saved.getImportBatch().getId(), saved.getSourceRowNumber());
    return mapper.toResponse(saved);
  }

  @Override
  public ImportRecordResponse getRecordById(Long id) {
    if (id == null) {
      throw new BadRequestException("Record ID is required");
    }
    ImportRecord record = importRecordRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("ImportRecord", "id"));
    return mapper.toResponse(record);
  }

  @Override
  public ImportRecordResponse getRecordByBatchIdAndSourceRowNumber(Long batchId, Integer sourceRowNumber) {
    if (batchId == null) {
      throw new BadRequestException("Batch ID is required");
    }
    if (sourceRowNumber == null) {
      throw new BadRequestException("Source row number is required");
    }
    ImportRecord record = importRecordRepository.findByImportBatchIdAndSourceRowNumber(batchId, sourceRowNumber)
        .orElseThrow(() -> new ResourceNotFoundException("ImportRecord", "batchId/sourceRowNumber"));
    return mapper.toResponse(record);
  }

  @Override
  public List<ImportRecordResponse> getRecordsByBatchId(Long batchId) {
    if (batchId == null) {
      throw new BadRequestException("Batch ID is required");
    }
    return importRecordRepository.findByImportBatchId(batchId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<ImportRecordResponse> getRecordsByStatusId(Long statusId) {
    if (statusId == null) {
      throw new BadRequestException("Status ID is required");
    }
    return importRecordRepository.findByImportRecordStatusId(statusId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<ImportRecordResponse> getRecordsByEntity(String entityType, Long entityId) {
    if (entityType == null || entityType.isBlank()) {
      throw new BadRequestException("Entity type is required");
    }
    if (entityId == null) {
      throw new BadRequestException("Entity ID is required");
    }
    return importRecordRepository.findByEntityTypeAndEntityId(entityType.trim(), entityId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public void deleteImportRecord(Long id) {
    if (id == null) {
      throw new BadRequestException("Record ID is required", "ID_REQUIRED");
    }
    ImportRecord record = importRecordRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("ImportRecord", "id"));
    importRecordRepository.delete(record);
    log.info("Deleted import record id={}", id);
  }
}
