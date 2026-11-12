package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.CreateImportBatchRequest;
import in.gov.sih.sih26135.dto.request.UpdateImportBatchRequest;
import in.gov.sih.sih26135.dto.response.ImportBatchResponse;
import in.gov.sih.sih26135.entity.ImportBatch;
import in.gov.sih.sih26135.entity.RefImportBatchStatus;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ImportBatchTerminalException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.ImportBatchMapper;
import in.gov.sih.sih26135.repository.ImportBatchRepository;
import in.gov.sih.sih26135.repository.ImportRecordRepository;
import in.gov.sih.sih26135.repository.RefImportBatchStatusRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import in.gov.sih.sih26135.service.ImportBatchService;
import in.gov.sih.sih26135.service.ingestion.ImportContractSchema;
import in.gov.sih.sih26135.service.ingestion.ImportFileStagingService;
import in.gov.sih.sih26135.service.ingestion.ImportSecurityScopeValidator;
import in.gov.sih.sih26135.service.ingestion.ImportSecurityScopeValidator.CallerScope;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
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
  private final ImportSecurityScopeValidator importSecurityScopeValidator;
  private final ImportFileStagingService importFileStagingService;
  private final JdbcTemplate jdbcTemplate;

  @PersistenceContext
  private EntityManager entityManager;

  @Autowired
  public ImportBatchServiceImpl(
      ImportBatchRepository importBatchRepository,
      RefImportBatchStatusRepository refImportBatchStatusRepository,
      UserRepository userRepository,
      ImportRecordRepository importRecordRepository,
      ImportBatchMapper mapper,
      ImportSecurityScopeValidator importSecurityScopeValidator,
      ImportFileStagingService importFileStagingService,
      JdbcTemplate jdbcTemplate) {
    this.importBatchRepository = importBatchRepository;
    this.refImportBatchStatusRepository = refImportBatchStatusRepository;
    this.userRepository = userRepository;
    this.importRecordRepository = importRecordRepository;
    this.mapper = mapper;
    this.importSecurityScopeValidator = importSecurityScopeValidator;
    this.importFileStagingService = importFileStagingService;
    this.jdbcTemplate = jdbcTemplate;
  }

  public ImportBatchServiceImpl(
      ImportBatchRepository importBatchRepository,
      RefImportBatchStatusRepository refImportBatchStatusRepository,
      UserRepository userRepository,
      ImportRecordRepository importRecordRepository,
      ImportBatchMapper mapper) {
    this(
        importBatchRepository,
        refImportBatchStatusRepository,
        userRepository,
        importRecordRepository,
        mapper,
        null,
        null,
        null
    );
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

    ImportSecurityScopeValidator validator = requireScopeValidator();
    String normalizedEntity = validator.normalizeEntityTypeLenient(request.getEntityType());
    CallerScope callerScope = validator.resolveAndValidateUploadOrProcessCaller(normalizedEntity);
    if (!callerScope.authorities().contains("system.manage")) {
      throw new AccessDeniedException("Caller lacks system.manage authority for legacy batch creation");
    }

    if (importBatchRepository.existsByBatchCode(batchCode)) {
      throw new ConflictException("Batch code already exists: " + batchCode, "BATCH_CODE_EXISTS");
    }

    RefImportBatchStatus status;
    if (request.getImportBatchStatusId() != null) {
      status = refImportBatchStatusRepository.findById(request.getImportBatchStatusId())
          .orElseThrow(() -> new ResourceNotFoundException("RefImportBatchStatus", "id"));
      if (!"PENDING".equalsIgnoreCase(status.getStatusCode())) {
        throw new BadRequestException(
            "Import batches must start in PENDING status; non-PENDING status cannot be set on creation",
            "IMPORT_LIFECYCLE_BYPASS_PROHIBITED");
      }
    } else {
      status = refImportBatchStatusRepository.findByStatusCode("PENDING")
          .orElseThrow(() -> new ResourceNotFoundException("RefImportBatchStatus", "statusCode"));
    }

    if (request.getRowCount() != null) {
      if (request.getRowCount() < 0) {
        throw new BadRequestException("Row count must be non-negative", "INVALID_ROW_COUNT");
      }
      if (request.getRowCount() > 0) {
        throw new BadRequestException(
            "Client-supplied rowCount > 0 is not permitted on batch creation",
            "IMPORT_COUNTER_MANIPULATION_PROHIBITED");
      }
    }
    if (request.getStartedAt() != null) {
      throw new BadRequestException(
          "Client-supplied startedAt is not permitted on batch creation",
          "IMPORT_LIFECYCLE_BYPASS_PROHIBITED");
    }

    Long initiatedByUserId = request.getInitiatedByUserId();
    if (!callerScope.nationwideAdmin()) {
      if (initiatedByUserId != null && !initiatedByUserId.equals(callerScope.caller().getId())) {
        throw new AccessDeniedException("Scoped caller cannot initiate an import batch on behalf of another user");
      }
      initiatedByUserId = callerScope.caller().getId();
    } else if (initiatedByUserId != null && !userRepository.existsById(initiatedByUserId)) {
      throw new ResourceNotFoundException("User", "id");
    }

    LocalDateTime now = LocalDateTime.now();
    ImportBatch batch = new ImportBatch(
        batchCode,
        request.getSourceSystem().trim(),
        request.getEntityType().trim(),
        status
    );
    batch.setInitiatedByUserId(initiatedByUserId);
    batch.setRowCount(0);
    batch.setSuccessCount(0);
    batch.setFailureCount(0);
    batch.setStartedAt(null);
    batch.setCompletedAt(null);
    batch.setCreatedAt(now);
    batch.setUpdatedAt(now);

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

    ImportSecurityScopeValidator validator = requireScopeValidator();
    CallerScope callerScope = validator.resolveAndValidateGenericWriteCaller();
    if (!callerScope.authorities().contains("system.manage")) {
      throw new AccessDeniedException("Caller lacks system.manage authority for legacy batch updates");
    }

    ImportBatch batch = lockAndLoadBatchForMutation(id);
    validator.validateCallerCanWriteBatch(callerScope, batch);

    RefImportBatchStatus currentStatus = batch.getImportBatchStatus();
    String currentCode = currentStatus != null && currentStatus.getStatusCode() != null
        ? currentStatus.getStatusCode().toUpperCase()
        : "PENDING";

    if ("RUNNING".equalsIgnoreCase(currentCode)) {
      throw new ConflictException(
          "Cannot modify a RUNNING import batch outside processing workflow",
          "IMPORT_BATCH_RUNNING_PROTECTED");
    }
    if (currentStatus != null && Boolean.TRUE.equals(currentStatus.getIsTerminalFlag())) {
      throw new ImportBatchTerminalException(id, currentCode);
    }
    if (isWorkflowManagedBatch(batch)) {
      throw new ConflictException(
          "Staged or workflow-owned import batches cannot be modified via generic update endpoint",
          "WORKFLOW_BATCH_PROTECTED");
    }

    if (request.getRowCount() != null || request.getSuccessCount() != null || request.getFailureCount() != null) {
      throw new BadRequestException(
          "Batch counters cannot be modified directly outside processing workflow",
          "IMPORT_COUNTER_MANIPULATION_PROHIBITED");
    }

    RefImportBatchStatus targetStatus = currentStatus;
    if (request.getImportBatchStatusId() != null) {
      targetStatus = refImportBatchStatusRepository.findById(request.getImportBatchStatusId())
          .orElseThrow(() -> new ResourceNotFoundException("RefImportBatchStatus", "id"));
      String targetCode = targetStatus.getStatusCode() != null ? targetStatus.getStatusCode().toUpperCase() : "";
      if (!"PENDING".equals(targetCode) && !"CANCELLED".equals(targetCode)) {
        throw new BadRequestException(
            "Generic batch update may only keep PENDING or transition an empty unstaged batch to CANCELLED",
            "IMPORT_LIFECYCLE_BYPASS_PROHIBITED");
      }
      batch.setImportBatchStatus(targetStatus);
    }

    String effectiveTargetCode = targetStatus != null && targetStatus.getStatusCode() != null
        ? targetStatus.getStatusCode().toUpperCase()
        : "PENDING";
    if (request.getStartedAt() != null
        || (request.getCompletedAt() != null && !"CANCELLED".equals(effectiveTargetCode))) {
      throw new BadRequestException(
          "Execution timestamps cannot be modified directly outside processing workflow",
          "IMPORT_LIFECYCLE_BYPASS_PROHIBITED");
    }

    if ("CANCELLED".equals(effectiveTargetCode)) {
      LocalDateTime cancelledAt = request.getCompletedAt() != null ? request.getCompletedAt() : LocalDateTime.now();
      if (batch.getStartedAt() == null) {
        batch.setStartedAt(cancelledAt);
      }
      if (cancelledAt.isBefore(batch.getStartedAt())) {
        throw new BadRequestException("Completed at date cannot be before started at date", "COMPLETED_DATE_BEFORE_STARTED");
      }
      batch.setCompletedAt(cancelledAt);
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
    ImportSecurityScopeValidator validator = requireScopeValidator();
    CallerScope callerScope = validator.resolveAndValidateReadCaller(null);
    ImportBatch batch = importBatchRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("ImportBatch", "id"));
    validator.validateCallerCanAccessBatch(callerScope, batch);
    return mapper.toResponse(batch);
  }

  @Override
  public ImportBatchResponse getBatchByCode(String batchCode) {
    if (batchCode == null || batchCode.isBlank()) {
      throw new BadRequestException("Batch code is required");
    }
    ImportSecurityScopeValidator validator = requireScopeValidator();
    CallerScope callerScope = validator.resolveAndValidateReadCaller(null);
    ImportBatch batch = importBatchRepository.findByBatchCode(batchCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("ImportBatch", "batchCode"));
    validator.validateCallerCanAccessBatch(callerScope, batch);
    return mapper.toResponse(batch);
  }

  @Override
  public List<ImportBatchResponse> getBatchesByStatusId(Long statusId) {
    if (statusId == null) {
      throw new BadRequestException("Status ID is required");
    }
    ImportSecurityScopeValidator validator = requireScopeValidator();
    CallerScope callerScope = validator.resolveAndValidateReadCaller(null);
    return importBatchRepository.findByImportBatchStatusId(statusId).stream()
        .filter(batch -> validator.canCallerViewBatchSilently(callerScope, batch))
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<ImportBatchResponse> getBatchesByEntityType(String entityType) {
    if (entityType == null || entityType.isBlank()) {
      throw new BadRequestException("Entity type is required");
    }
    ImportSecurityScopeValidator validator = requireScopeValidator();
    CallerScope callerScope = validator.resolveAndValidateReadCaller(entityType);
    if (callerScope.isEmployerScoped()
        && ImportContractSchema.ENTITY_TRAINEE.equals(validator.normalizeEntityTypeLenient(entityType))) {
      throw new AccessDeniedException("Employers are not authorized to access trainee import batches");
    }
    return importBatchRepository.findByEntityType(entityType.trim()).stream()
        .filter(batch -> validator.canCallerViewBatchSilently(callerScope, batch))
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<ImportBatchResponse> getBatchesBySourceSystem(String sourceSystem) {
    if (sourceSystem == null || sourceSystem.isBlank()) {
      throw new BadRequestException("Source system is required");
    }
    ImportSecurityScopeValidator validator = requireScopeValidator();
    CallerScope callerScope = validator.resolveAndValidateReadCaller(null);
    return importBatchRepository.findBySourceSystem(sourceSystem.trim()).stream()
        .filter(batch -> validator.canCallerViewBatchSilently(callerScope, batch))
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<ImportBatchResponse> getBatchesByInitiatedByUserId(Long initiatedByUserId) {
    if (initiatedByUserId == null) {
      throw new BadRequestException("Initiated-by user ID is required");
    }
    ImportSecurityScopeValidator validator = requireScopeValidator();
    CallerScope callerScope = validator.resolveAndValidateReadCaller(null);
    return importBatchRepository.findByInitiatedByUserId(initiatedByUserId).stream()
        .filter(batch -> validator.canCallerViewBatchSilently(callerScope, batch))
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public void deleteImportBatch(Long id) {
    if (id == null) {
      throw new BadRequestException("Batch ID is required", "ID_REQUIRED");
    }
    ImportSecurityScopeValidator validator = requireScopeValidator();
    CallerScope callerScope = validator.resolveAndValidateGenericWriteCaller();
    if (!callerScope.authorities().contains("system.manage")) {
      throw new AccessDeniedException("Caller lacks system.manage authority for legacy batch deletion");
    }

    ImportBatch batch = lockAndLoadBatchForMutation(id);
    validator.validateCallerCanWriteBatch(callerScope, batch);

    RefImportBatchStatus status = batch.getImportBatchStatus();
    String statusCode = status != null && status.getStatusCode() != null
        ? status.getStatusCode().toUpperCase()
        : "PENDING";

    if ("RUNNING".equalsIgnoreCase(statusCode)) {
      throw new ConflictException(
          "Cannot delete an actively running import batch",
          "IMPORT_BATCH_RUNNING_PROTECTED");
    }
    if ("COMPLETED".equalsIgnoreCase(statusCode) || "FAILED".equalsIgnoreCase(statusCode)) {
      throw new ImportBatchTerminalException(id, statusCode);
    }
    if (importFileStagingService != null && importFileStagingService.hasStagedFile(batch.getBatchCode())) {
      throw new ConflictException(
          "Cannot delete a staged workflow import batch via generic CRUD endpoint",
          "WORKFLOW_BATCH_PROTECTED");
    }
    if (importRecordRepository.existsByImportBatchId(id)) {
      throw new ConflictException("Cannot delete import batch with existing records", "BATCH_HAS_RECORDS");
    }

    importBatchRepository.delete(batch);
    log.info("Deleted import batch id={}", id);
  }

  private ImportBatch lockAndLoadBatchForMutation(Long id) {
    if (jdbcTemplate != null) {
      try {
        jdbcTemplate.queryForObject(
            "SELECT import_batch_id FROM import_batches WHERE import_batch_id = ? FOR UPDATE",
            Long.class, id);
      } catch (EmptyResultDataAccessException e) {
        throw new ResourceNotFoundException("ImportBatch", "id");
      }
    }
    ImportBatch batch = importBatchRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("ImportBatch", "id"));
    if (entityManager != null) {
      entityManager.refresh(batch);
    }
    return batch;
  }

  private boolean isWorkflowManagedBatch(ImportBatch batch) {
    if (batch == null) {
      return false;
    }
    if (importFileStagingService != null && importFileStagingService.hasStagedFile(batch.getBatchCode())) {
      return true;
    }
    return batch.getId() != null && importRecordRepository.existsByImportBatchId(batch.getId());
  }

  private ImportSecurityScopeValidator requireScopeValidator() {
    if (importSecurityScopeValidator == null) {
      throw new IllegalStateException("ImportSecurityScopeValidator is not configured");
    }
    return importSecurityScopeValidator;
  }
}
