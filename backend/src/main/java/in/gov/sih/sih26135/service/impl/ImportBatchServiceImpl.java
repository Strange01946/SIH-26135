package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.CreateImportBatchRequest;
import in.gov.sih.sih26135.dto.request.UpdateImportBatchRequest;
import in.gov.sih.sih26135.dto.response.ImportBatchResponse;
import in.gov.sih.sih26135.entity.ImportBatch;
import in.gov.sih.sih26135.entity.RefImportBatchStatus;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.ImportBatchMapper;
import in.gov.sih.sih26135.repository.ImportBatchRepository;
import in.gov.sih.sih26135.repository.ImportRecordRepository;
import in.gov.sih.sih26135.repository.RefImportBatchStatusRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import in.gov.sih.sih26135.service.ImportBatchService;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ImportBatchServiceImpl implements ImportBatchService {

  private static final Logger log = LoggerFactory.getLogger(ImportBatchServiceImpl.class);

  private final ImportBatchRepository importBatchRepository;
  private final RefImportBatchStatusRepository refImportBatchStatusRepository;
  private final UserRepository userRepository;
  private final ImportRecordRepository importRecordRepository;
  private final ImportBatchMapper mapper;

  public ImportBatchServiceImpl(
      ImportBatchRepository importBatchRepository,
      RefImportBatchStatusRepository refImportBatchStatusRepository,
      UserRepository userRepository,
      ImportRecordRepository importRecordRepository,
      ImportBatchMapper mapper) {
    this.importBatchRepository = importBatchRepository;
    this.refImportBatchStatusRepository = refImportBatchStatusRepository;
    this.userRepository = userRepository;
    this.importRecordRepository = importRecordRepository;
    this.mapper = mapper;
  }

  @Override
  @Transactional
  public ImportBatchResponse createImportBatch(CreateImportBatchRequest request) {
    if (request == null) {
      throw new BadRequestException("Request body cannot be null", "REQUEST_BODY_NULL");
    }
    if (request.getBatchCode() == null || request.getBatchCode().isBlank()) {
      throw new BadRequestException("Batch code is required", "BATCH_CODE_REQUIRED");
    }
    String batchCode = request.getBatchCode().trim();
    if (batchCode.length() > 32) {
      throw new BadRequestException("Batch code must not exceed 32 characters", "BATCH_CODE_TOO_LONG");
    }
    if (importBatchRepository.existsByBatchCode(batchCode)) {
      throw new ConflictException("Batch code already exists: " + batchCode, "BATCH_CODE_EXISTS");
    }

    if (request.getSourceSystem() == null || request.getSourceSystem().isBlank()) {
      throw new BadRequestException("Source system is required", "SOURCE_SYSTEM_REQUIRED");
    }
    if (request.getSourceSystem().trim().length() > 64) {
      throw new BadRequestException("Source system must not exceed 64 characters", "SOURCE_SYSTEM_TOO_LONG");
    }

    if (request.getEntityType() == null || request.getEntityType().isBlank()) {
      throw new BadRequestException("Entity type is required", "ENTITY_TYPE_REQUIRED");
    }
    if (request.getEntityType().trim().length() > 64) {
      throw new BadRequestException("Entity type must not exceed 64 characters", "ENTITY_TYPE_TOO_LONG");
    }

    RefImportBatchStatus status;
    if (request.getImportBatchStatusId() != null) {
      status = refImportBatchStatusRepository.findById(request.getImportBatchStatusId())
          .orElseThrow(() -> new ResourceNotFoundException("RefImportBatchStatus", "id"));
    } else {
      status = refImportBatchStatusRepository.findByStatusCode("PENDING")
          .orElseThrow(() -> new ResourceNotFoundException("RefImportBatchStatus", "statusCode"));
    }

    if (request.getInitiatedByUserId() != null && !userRepository.existsById(request.getInitiatedByUserId())) {
      throw new ResourceNotFoundException("User", "id");
    }

    if (request.getRowCount() != null && request.getRowCount() < 0) {
      throw new BadRequestException("Row count must be non-negative", "INVALID_ROW_COUNT");
    }

    ImportBatch batch = new ImportBatch(
        batchCode,
        request.getSourceSystem().trim(),
        request.getEntityType().trim(),
        status
    );
    batch.setInitiatedByUserId(request.getInitiatedByUserId());
    batch.setRowCount(request.getRowCount() != null ? request.getRowCount() : 0);
    batch.setSuccessCount(0);
    batch.setFailureCount(0);
    batch.setStartedAt(request.getStartedAt());

    ImportBatch saved = importBatchRepository.save(batch);
    log.info("Created import batch id={}, batchCode={}", saved.getId(), saved.getBatchCode());
    return mapper.toResponse(saved);
  }

  @Override
  @Transactional
  public ImportBatchResponse updateImportBatch(Long id, UpdateImportBatchRequest request) {
    if (id == null) {
      throw new BadRequestException("Batch ID is required", "ID_REQUIRED");
    }
    if (request == null) {
      throw new BadRequestException("Request body cannot be null", "REQUEST_BODY_NULL");
    }

    ImportBatch batch = importBatchRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("ImportBatch", "id"));

    boolean currentIsTerminal = Boolean.TRUE.equals(batch.getImportBatchStatus().getIsTerminalFlag());
    if (currentIsTerminal) {
      throw new ConflictException("Terminal import batch cannot be updated", "BATCH_IS_TERMINAL");
    }

    RefImportBatchStatus targetStatus = batch.getImportBatchStatus();
    if (request.getImportBatchStatusId() != null) {
      targetStatus = refImportBatchStatusRepository.findById(request.getImportBatchStatusId())
          .orElseThrow(() -> new ResourceNotFoundException("RefImportBatchStatus", "id"));
      batch.setImportBatchStatus(targetStatus);
    }

    boolean targetIsTerminal = Boolean.TRUE.equals(targetStatus.getIsTerminalFlag());
    if (targetIsTerminal) {
      LocalDateTime completedAt = request.getCompletedAt() != null ? request.getCompletedAt() : batch.getCompletedAt();
      if (completedAt == null) {
        completedAt = LocalDateTime.now();
      }
      batch.setCompletedAt(completedAt);

      if (request.getStartedAt() != null) {
        batch.setStartedAt(request.getStartedAt());
      } else if (batch.getStartedAt() == null) {
        batch.setStartedAt(completedAt);
      }
    } else {
      if (request.getStartedAt() != null) {
        batch.setStartedAt(request.getStartedAt());
      }
      if (request.getCompletedAt() != null) {
        batch.setCompletedAt(request.getCompletedAt());
      }
    }

    if (batch.getStartedAt() != null && batch.getCompletedAt() != null
        && batch.getCompletedAt().isBefore(batch.getStartedAt())) {
      throw new BadRequestException("Completed at date cannot be before started at date", "COMPLETED_DATE_BEFORE_STARTED");
    }

    if (request.getRowCount() != null) {
      if (request.getRowCount() < 0) {
        throw new BadRequestException("Row count must be non-negative", "INVALID_ROW_COUNT");
      }
      batch.setRowCount(request.getRowCount());
    }
    if (request.getSuccessCount() != null) {
      if (request.getSuccessCount() < 0) {
        throw new BadRequestException("Success count must be non-negative", "INVALID_SUCCESS_COUNT");
      }
      batch.setSuccessCount(request.getSuccessCount());
    }
    if (request.getFailureCount() != null) {
      if (request.getFailureCount() < 0) {
        throw new BadRequestException("Failure count must be non-negative", "INVALID_FAILURE_COUNT");
      }
      batch.setFailureCount(request.getFailureCount());
    }

    Integer rowCount = batch.getRowCount();
    Integer successCount = batch.getSuccessCount();
    Integer failureCount = batch.getFailureCount();
    if (rowCount != null && rowCount > 0) {
      int totalProcessed = (successCount != null ? successCount : 0) + (failureCount != null ? failureCount : 0);
      if (totalProcessed > rowCount) {
        throw new BadRequestException("Sum of success and failure counts cannot exceed total row count", "COUNTS_EXCEED_ROW_COUNT");
      }
    }

    batch.setUpdatedAt(LocalDateTime.now());
    ImportBatch saved = importBatchRepository.save(batch);
    log.info("Updated import batch id={}, status={}", saved.getId(), saved.getImportBatchStatus().getStatusCode());
    return mapper.toResponse(saved);
  }

  @Override
  public ImportBatchResponse getBatchById(Long id) {
    if (id == null) {
      throw new BadRequestException("Batch ID is required");
    }
    ImportBatch batch = importBatchRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("ImportBatch", "id"));
    return mapper.toResponse(batch);
  }

  @Override
  public ImportBatchResponse getBatchByCode(String batchCode) {
    if (batchCode == null || batchCode.isBlank()) {
      throw new BadRequestException("Batch code is required");
    }
    ImportBatch batch = importBatchRepository.findByBatchCode(batchCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("ImportBatch", "batchCode"));
    return mapper.toResponse(batch);
  }

  @Override
  public List<ImportBatchResponse> getBatchesByStatusId(Long statusId) {
    if (statusId == null) {
      throw new BadRequestException("Status ID is required");
    }
    return importBatchRepository.findByImportBatchStatusId(statusId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<ImportBatchResponse> getBatchesByEntityType(String entityType) {
    if (entityType == null || entityType.isBlank()) {
      throw new BadRequestException("Entity type is required");
    }
    return importBatchRepository.findByEntityType(entityType.trim()).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<ImportBatchResponse> getBatchesBySourceSystem(String sourceSystem) {
    if (sourceSystem == null || sourceSystem.isBlank()) {
      throw new BadRequestException("Source system is required");
    }
    return importBatchRepository.findBySourceSystem(sourceSystem.trim()).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<ImportBatchResponse> getBatchesByInitiatedByUserId(Long initiatedByUserId) {
    if (initiatedByUserId == null) {
      throw new BadRequestException("Initiated-by user ID is required");
    }
    return importBatchRepository.findByInitiatedByUserId(initiatedByUserId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public void deleteImportBatch(Long id) {
    if (id == null) {
      throw new BadRequestException("Batch ID is required", "ID_REQUIRED");
    }
    ImportBatch batch = importBatchRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("ImportBatch", "id"));

    if ("RUNNING".equalsIgnoreCase(batch.getImportBatchStatus().getStatusCode())) {
      throw new BadRequestException("Cannot delete a running import batch", "BATCH_IS_RUNNING");
    }

    if (!importRecordRepository.findByImportBatchId(id).isEmpty()) {
      throw new ConflictException("Cannot delete import batch with existing records", "BATCH_HAS_RECORDS");
    }

    importBatchRepository.delete(batch);
    log.info("Deleted import batch id={}", id);
  }
}
