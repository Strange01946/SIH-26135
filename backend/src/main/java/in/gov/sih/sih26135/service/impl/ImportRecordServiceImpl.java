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
import in.gov.sih.sih26135.response.PaginationUtils;
import in.gov.sih.sih26135.service.ImportRecordService;
import in.gov.sih.sih26135.service.ingestion.ImportContractSchema;
import in.gov.sih.sih26135.service.ingestion.ImportFileStagingService;
import in.gov.sih.sih26135.service.ingestion.ImportSecurityScopeValidator;
import in.gov.sih.sih26135.service.ingestion.ImportSecurityScopeValidator.CallerScope;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
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
  private final ImportSecurityScopeValidator importSecurityScopeValidator;
  private final ImportFileStagingService importFileStagingService;
  private final JdbcTemplate jdbcTemplate;

  @PersistenceContext
  private EntityManager entityManager;

  @Autowired
  public ImportRecordServiceImpl(
      ImportRecordRepository importRecordRepository,
      ImportBatchRepository importBatchRepository,
      RefImportRecordStatusRepository refImportRecordStatusRepository,
      ImportRecordMapper mapper,
      ImportSecurityScopeValidator importSecurityScopeValidator,
      ImportFileStagingService importFileStagingService,
      JdbcTemplate jdbcTemplate) {
    this.importRecordRepository = importRecordRepository;
    this.importBatchRepository = importBatchRepository;
    this.refImportRecordStatusRepository = refImportRecordStatusRepository;
    this.mapper = mapper;
    this.importSecurityScopeValidator = importSecurityScopeValidator;
    this.importFileStagingService = importFileStagingService;
    this.jdbcTemplate = jdbcTemplate;
  }

  public ImportRecordServiceImpl(
      ImportRecordRepository importRecordRepository,
      ImportBatchRepository importBatchRepository,
      RefImportRecordStatusRepository refImportRecordStatusRepository,
      ImportRecordMapper mapper) {
    this(
        importRecordRepository,
        importBatchRepository,
        refImportRecordStatusRepository,
        mapper,
        null,
        null,
        null
    );
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

    ImportSecurityScopeValidator validator = requireScopeValidator();
    CallerScope callerScope = validator.resolveAndValidateGenericWriteCaller();
    if (!callerScope.authorities().contains("system.manage")) {
      throw new AccessDeniedException("Caller lacks system.manage authority for legacy import record creation");
    }

    ImportBatch batch = lockAndLoadBatchForMutation(request.getImportBatchId());
    validator.validateCallerCanWriteBatch(callerScope, batch);

    if (isWorkflowManagedBatch(batch)) {
      throw new ConflictException(
          "Cannot manually create import records in a staged, running, or finalized workflow batch",
          "WORKFLOW_RECORD_PROTECTED");
    }

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

    String statusCode = status.getStatusCode() != null ? status.getStatusCode().toUpperCase() : "";
    String normalizedEntityType = request.getEntityType() != null ? request.getEntityType().trim() : null;
    if ("SUCCESS".equals(statusCode)) {
      normalizedEntityType = verifyCommittedEntityExistsAndInScope(
          validator, callerScope, batch, normalizedEntityType, request.getEntityId());
    } else if (request.getEntityId() != null) {
      throw new BadRequestException(
          "Non-SUCCESS import records cannot reference a committed entity identifier",
          "INVALID_RECORD_OUTCOME");
    }

    LocalDateTime now = LocalDateTime.now();
    ImportRecord record = new ImportRecord(batch, request.getSourceRowNumber(), status);
    record.setEntityType(normalizedEntityType);
    record.setEntityId(request.getEntityId());
    record.setErrorCode(request.getErrorCode() != null ? request.getErrorCode().trim() : null);
    record.setErrorMessage(request.getErrorMessage() != null ? request.getErrorMessage().trim() : null);
    record.setCreatedAt(now);
    record.setUpdatedAt(now);

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

    ImportSecurityScopeValidator validator = requireScopeValidator();
    CallerScope callerScope = validator.resolveAndValidateGenericWriteCaller();
    if (!callerScope.authorities().contains("system.manage")) {
      throw new AccessDeniedException("Caller lacks system.manage authority for legacy import record modification");
    }

    ImportRecord record = importRecordRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("ImportRecord", "id"));
    ImportBatch batch = lockAndLoadBatchForMutation(record.getImportBatch().getId());
    if (entityManager != null) {
      entityManager.refresh(record);
    }
    validator.validateCallerCanWriteBatch(callerScope, batch);

    if (isWorkflowManagedBatch(batch)) {
      throw new ConflictException(
          "Workflow-generated import records are immutable and cannot be modified",
          "WORKFLOW_RECORD_IMMUTABLE");
    }
    String existingStatus = record.getImportRecordStatus() != null
        && record.getImportRecordStatus().getStatusCode() != null
        ? record.getImportRecordStatus().getStatusCode().toUpperCase()
        : "";
    if ("SUCCESS".equals(existingStatus)) {
      throw new ConflictException(
          "Committed SUCCESS import records cannot be modified via generic CRUD endpoint",
          "WORKFLOW_RECORD_IMMUTABLE");
    }

    if (request.getSourceRowNumber() != null) {
      if (request.getSourceRowNumber() < 1) {
        throw new BadRequestException("Source row number must be greater than or equal to 1", "INVALID_SOURCE_ROW_NUMBER");
      }
      if (!request.getSourceRowNumber().equals(record.getSourceRowNumber())) {
        Optional<ImportRecord> existingWithRow = importRecordRepository.findByImportBatchIdAndSourceRowNumber(
            batch.getId(), request.getSourceRowNumber());
        if (existingWithRow.isPresent() && !existingWithRow.get().getId().equals(record.getId())) {
          throw new ConflictException("Row number " + request.getSourceRowNumber() + " already exists in import batch", "IMPORT_RECORD_ROW_EXISTS");
        }
        record.setSourceRowNumber(request.getSourceRowNumber());
      }
    }

    RefImportRecordStatus targetStatus = record.getImportRecordStatus();
    if (request.getImportRecordStatusId() != null) {
      targetStatus = refImportRecordStatusRepository.findById(request.getImportRecordStatusId())
          .orElseThrow(() -> new ResourceNotFoundException("RefImportRecordStatus", "id"));
      record.setImportRecordStatus(targetStatus);
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

    String targetStatusCode = targetStatus != null && targetStatus.getStatusCode() != null
        ? targetStatus.getStatusCode().toUpperCase()
        : "";
    if ("SUCCESS".equals(targetStatusCode)) {
      String verifiedType = verifyCommittedEntityExistsAndInScope(
          validator, callerScope, batch, record.getEntityType(), record.getEntityId());
      record.setEntityType(verifiedType);
    } else if (record.getEntityId() != null) {
      throw new BadRequestException(
          "Non-SUCCESS import records cannot reference a committed entity identifier",
          "INVALID_RECORD_OUTCOME");
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
    log.info("Updated import record id={}, batchId={}, rowNumber={}", saved.getId(), batch.getId(), saved.getSourceRowNumber());
    return mapper.toResponse(saved);
  }

  @Override
  public ImportRecordResponse getRecordById(Long id) {
    if (id == null) {
      throw new BadRequestException("Record ID is required");
    }
    ImportSecurityScopeValidator validator = requireScopeValidator();
    CallerScope callerScope = validator.resolveAndValidateReadCaller(null);
    ImportRecord record = importRecordRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("ImportRecord", "id"));
    validator.validateCallerCanAccessBatch(callerScope, record.getImportBatch());
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
    ImportSecurityScopeValidator validator = requireScopeValidator();
    CallerScope callerScope = validator.resolveAndValidateReadCaller(null);
    ImportBatch batch = importBatchRepository.findById(batchId)
        .orElseThrow(() -> new ResourceNotFoundException("ImportBatch", "id"));
    validator.validateCallerCanAccessBatch(callerScope, batch);

    ImportRecord record = importRecordRepository.findByImportBatchIdAndSourceRowNumber(batchId, sourceRowNumber)
        .orElseThrow(() -> new ResourceNotFoundException("ImportRecord", "batchId/sourceRowNumber"));
    return mapper.toResponse(record);
  }

  @Override
  public List<ImportRecordResponse> getRecordsByBatchId(Long batchId) {
    if (batchId == null) {
      throw new BadRequestException("Batch ID is required");
    }
    ImportSecurityScopeValidator validator = requireScopeValidator();
    CallerScope callerScope = validator.resolveAndValidateReadCaller(null);
    ImportBatch batch = importBatchRepository.findById(batchId)
        .orElseThrow(() -> new ResourceNotFoundException("ImportBatch", "id"));
    validator.validateCallerCanAccessBatch(callerScope, batch);

    return importRecordRepository.findByImportBatchId(batchId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public Page<ImportRecordResponse> getRecordsByBatchId(Long batchId, Pageable pageable) {
    if (batchId == null) {
      throw new BadRequestException("Batch ID is required");
    }
    ImportSecurityScopeValidator validator = requireScopeValidator();
    CallerScope callerScope = validator.resolveAndValidateReadCaller(null);
    ImportBatch batch = importBatchRepository.findById(batchId)
        .orElseThrow(() -> new ResourceNotFoundException("ImportBatch", "id"));
    validator.validateCallerCanAccessBatch(callerScope, batch);

    return PaginationUtils.executeSafeJpaPage(
            pageable, p -> importRecordRepository.findByImportBatchId(batchId, p))
        .map(mapper::toResponse);
  }

  @Override
  public List<ImportRecordResponse> getRecordsByStatusId(Long statusId) {
    if (statusId == null) {
      throw new BadRequestException("Status ID is required");
    }
    ImportSecurityScopeValidator validator = requireScopeValidator();
    CallerScope callerScope = validator.resolveAndValidateReadCaller(null);
    if (callerScope.nationwideAdmin()) {
      return importRecordRepository.findByImportRecordStatusId(statusId).stream()
          .map(mapper::toResponse)
          .toList();
    }
    return importRecordRepository.findByImportRecordStatusId(statusId).stream()
        .filter(record -> validator.canCallerViewBatchSilently(callerScope, record.getImportBatch()))
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public Page<ImportRecordResponse> getRecordsByStatusId(Long statusId, Pageable pageable) {
    if (statusId == null) {
      throw new BadRequestException("Status ID is required");
    }
    ImportSecurityScopeValidator validator = requireScopeValidator();
    CallerScope callerScope = validator.resolveAndValidateReadCaller(null);
    if (callerScope.nationwideAdmin()) {
      return PaginationUtils.executeSafeJpaPage(
              pageable, p -> importRecordRepository.findByImportRecordStatusId(statusId, p))
          .map(mapper::toResponse);
    }
    List<Object> params = new ArrayList<>();
    params.add(statusId);
    return queryScopedRecordsPage("ir.import_record_status_id = ?", params, callerScope, validator, pageable);
  }

  @Override
  public List<ImportRecordResponse> getRecordsByEntity(String entityType, Long entityId) {
    if (entityType == null || entityType.isBlank()) {
      throw new BadRequestException("Entity type is required");
    }
    if (entityId == null) {
      throw new BadRequestException("Entity ID is required");
    }
    ImportSecurityScopeValidator validator = requireScopeValidator();
    CallerScope callerScope = validator.resolveAndValidateReadCaller(entityType);
    if (callerScope.isEmployerScoped()
        && ImportContractSchema.ENTITY_TRAINEE.equals(validator.normalizeEntityTypeLenient(entityType))) {
      throw new AccessDeniedException("Employers are not authorized to access trainee import records");
    }
    if (callerScope.nationwideAdmin()) {
      return importRecordRepository.findByEntityTypeAndEntityId(entityType.trim(), entityId).stream()
          .map(mapper::toResponse)
          .toList();
    }
    return importRecordRepository.findByEntityTypeAndEntityId(entityType.trim(), entityId).stream()
        .filter(record -> validator.canCallerViewBatchSilently(callerScope, record.getImportBatch()))
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public Page<ImportRecordResponse> getRecordsByEntity(String entityType, Long entityId, Pageable pageable) {
    if (entityType == null || entityType.isBlank()) {
      throw new BadRequestException("Entity type is required");
    }
    if (entityId == null) {
      throw new BadRequestException("Entity ID is required");
    }
    ImportSecurityScopeValidator validator = requireScopeValidator();
    CallerScope callerScope = validator.resolveAndValidateReadCaller(entityType);
    if (callerScope.isEmployerScoped()
        && ImportContractSchema.ENTITY_TRAINEE.equals(validator.normalizeEntityTypeLenient(entityType))) {
      throw new AccessDeniedException("Employers are not authorized to access trainee import records");
    }
    if (callerScope.nationwideAdmin()) {
      return PaginationUtils.executeSafeJpaPage(
              pageable, p -> importRecordRepository.findByEntityTypeAndEntityId(entityType.trim(), entityId, p))
          .map(mapper::toResponse);
    }
    List<Object> params = new ArrayList<>();
    params.add(entityType.trim());
    params.add(entityId);
    return queryScopedRecordsPage("ir.entity_type = ? AND ir.entity_id = ?", params, callerScope, validator, pageable);
  }

  @Override
  @Transactional
  public void deleteImportRecord(Long id) {
    if (id == null) {
      throw new BadRequestException("Record ID is required", "ID_REQUIRED");
    }
    ImportSecurityScopeValidator validator = requireScopeValidator();
    CallerScope callerScope = validator.resolveAndValidateGenericWriteCaller();
    if (!callerScope.authorities().contains("system.manage")) {
      throw new AccessDeniedException("Caller lacks system.manage authority for legacy import record deletion");
    }

    ImportRecord record = importRecordRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("ImportRecord", "id"));
    ImportBatch batch = lockAndLoadBatchForMutation(record.getImportBatch().getId());
    if (entityManager != null) {
      entityManager.refresh(record);
    }
    validator.validateCallerCanWriteBatch(callerScope, batch);

    if (isWorkflowManagedBatch(batch)) {
      throw new ConflictException(
          "Workflow-generated import records cannot be deleted",
          "WORKFLOW_RECORD_IMMUTABLE");
    }
    String existingStatus = record.getImportRecordStatus() != null
        && record.getImportRecordStatus().getStatusCode() != null
        ? record.getImportRecordStatus().getStatusCode().toUpperCase()
        : "";
    if ("SUCCESS".equals(existingStatus)) {
      throw new ConflictException(
          "Committed SUCCESS import records cannot be deleted via generic CRUD endpoint",
          "WORKFLOW_RECORD_IMMUTABLE");
    }

    importRecordRepository.delete(record);
    log.info("Deleted import record id={}", id);
  }

  private ImportBatch lockAndLoadBatchForMutation(Long batchId) {
    if (jdbcTemplate != null) {
      try {
        jdbcTemplate.queryForObject(
            "SELECT import_batch_id FROM import_batches WHERE import_batch_id = ? FOR UPDATE",
            Long.class, batchId);
      } catch (EmptyResultDataAccessException e) {
        throw new ResourceNotFoundException("ImportBatch", "id");
      }
    }
    ImportBatch batch = importBatchRepository.findById(batchId)
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
    String code = batch.getImportBatchStatus() != null && batch.getImportBatchStatus().getStatusCode() != null
        ? batch.getImportBatchStatus().getStatusCode().toUpperCase()
        : "";
    return "RUNNING".equals(code) || "COMPLETED".equals(code) || "FAILED".equals(code);
  }

  private String verifyCommittedEntityExistsAndInScope(
      ImportSecurityScopeValidator validator,
      CallerScope callerScope,
      ImportBatch batch,
      String rawEntityType,
      Long entityId) {
    if (rawEntityType == null || rawEntityType.isBlank() || entityId == null || entityId <= 0) {
      throw new BadRequestException(
          "SUCCESS import records require a valid entityType and positive entityId",
          "UNVERIFIABLE_SUCCESS_ENTITY");
    }
    if (jdbcTemplate == null) {
      throw new IllegalStateException("JdbcTemplate is required to verify committed entities");
    }
    String normalizedType = validator.normalizeEntityTypeLenient(rawEntityType);
    String batchType = validator.normalizeEntityTypeLenient(batch.getEntityType());
    if (!normalizedType.equals(batchType)) {
      throw new BadRequestException(
          "Record entityType does not match parent batch entityType",
          "UNVERIFIABLE_SUCCESS_ENTITY");
    }

    if (ImportContractSchema.ENTITY_TRAINEE.equals(normalizedType)) {
      if (callerScope.isEmployerScoped()) {
        throw new AccessDeniedException("Employers cannot register Trainee SUCCESS import records");
      }
      List<Map<String, Object>> rows = jdbcTemplate.queryForList(
          "SELECT t.state_id, t.district_id, u.training_provider_id AS user_provider_id " +
              "FROM trainees t " +
              "LEFT JOIN users u ON t.user_id = u.user_id AND u.deleted_at IS NULL " +
              "WHERE t.trainee_id = ? AND t.deleted_at IS NULL",
          entityId);
      if (rows.isEmpty()) {
        throw new BadRequestException(
            "Referenced Trainee entity does not exist or has been deleted",
            "ENTITY_NOT_FOUND_FOR_SUCCESS_RECORD");
      }
      if (!callerScope.nationwideAdmin()) {
        Map<String, Object> row = rows.get(0);
        Long stateId = row.get("state_id") != null ? ((Number) row.get("state_id")).longValue() : null;
        Long districtId = row.get("district_id") != null ? ((Number) row.get("district_id")).longValue() : null;
        Long userProviderId = row.get("user_provider_id") != null
            ? ((Number) row.get("user_provider_id")).longValue() : null;
        if (callerScope.isStateOrDistrictScoped()) {
          if (!Objects.equals(stateId, callerScope.stateId())
              || (callerScope.districtId() != null && !Objects.equals(districtId, callerScope.districtId()))) {
            throw new AccessDeniedException("Referenced Trainee is outside caller geographic scope");
          }
        } else if (callerScope.isProviderScoped()) {
          Long callerProviderId = callerScope.trainingProviderId();
          Integer matchingEnrollments = jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM training_enrollments te " +
                  "JOIN trainees t ON te.trainee_id = t.trainee_id " +
                  "WHERE te.trainee_id = ? AND te.provider_id = ? AND te.deleted_at IS NULL AND t.deleted_at IS NULL",
              Integer.class, entityId, callerProviderId);
          boolean hasMatchingEnrollment = matchingEnrollments != null && matchingEnrollments > 0;
          boolean hasMatchingUserProvider = Objects.equals(userProviderId, callerProviderId);
          boolean hasConflictingUserProvider = userProviderId != null && !hasMatchingUserProvider;
          if ((!hasMatchingEnrollment && !hasMatchingUserProvider) || hasConflictingUserProvider) {
            throw new AccessDeniedException(
                "Referenced Trainee does not have verifiable ownership under caller training provider");
          }
        } else {
          throw new AccessDeniedException("Caller lacks authorized tenant scope for Trainee SUCCESS import record");
        }
      }
      return ImportContractSchema.ENTITY_TRAINEE;
    }

    if (ImportContractSchema.ENTITY_PLACEMENT_RECORD.equals(normalizedType)) {
      List<Map<String, Object>> rows = jdbcTemplate.queryForList(
          "SELECT pr.provider_id, pr.employer_id, pr.work_state_id, pr.work_district_id, " +
              "COALESCE(tc.state_id, t.state_id) AS center_state_id, " +
              "COALESCE(tc.district_id, t.district_id) AS center_district_id " +
              "FROM placement_records pr " +
              "JOIN training_enrollments te ON pr.enrollment_id = te.enrollment_id " +
              "LEFT JOIN training_centers tc ON te.center_id = tc.center_id " +
              "JOIN trainees t ON pr.trainee_id = t.trainee_id " +
              "WHERE pr.placement_id = ? AND pr.deleted_at IS NULL",
          entityId);
      if (rows.isEmpty()) {
        throw new BadRequestException(
            "Referenced PlacementRecord entity does not exist or has been deleted",
            "ENTITY_NOT_FOUND_FOR_SUCCESS_RECORD");
      }
      if (!callerScope.nationwideAdmin()) {
        Map<String, Object> r = rows.get(0);
        Long providerId = r.get("provider_id") != null ? ((Number) r.get("provider_id")).longValue() : null;
        Long employerId = r.get("employer_id") != null ? ((Number) r.get("employer_id")).longValue() : null;
        Long workStateId = r.get("work_state_id") != null ? ((Number) r.get("work_state_id")).longValue() : null;
        Long workDistrictId = r.get("work_district_id") != null ? ((Number) r.get("work_district_id")).longValue() : null;
        Long centerStateId = r.get("center_state_id") != null ? ((Number) r.get("center_state_id")).longValue() : null;
        Long centerDistrictId = r.get("center_district_id") != null ? ((Number) r.get("center_district_id")).longValue() : null;
        if (callerScope.isEmployerScoped() && !Objects.equals(employerId, callerScope.employerId())) {
          throw new AccessDeniedException("Referenced PlacementRecord belongs to another employer");
        }
        if (callerScope.isProviderScoped() && !Objects.equals(providerId, callerScope.trainingProviderId())) {
          throw new AccessDeniedException("Referenced PlacementRecord belongs to another training provider");
        }
        if (callerScope.isStateOrDistrictScoped()) {
          if (!Objects.equals(centerStateId, callerScope.stateId())
              || (workStateId != null && !Objects.equals(workStateId, callerScope.stateId()))) {
            throw new AccessDeniedException("Referenced PlacementRecord is outside caller state scope");
          }
          if (callerScope.districtId() != null
              && (!Objects.equals(centerDistrictId, callerScope.districtId())
              || (workDistrictId != null && !Objects.equals(workDistrictId, callerScope.districtId())))) {
            throw new AccessDeniedException("Referenced PlacementRecord is outside caller district scope");
          }
        }
      }
      return ImportContractSchema.ENTITY_PLACEMENT_RECORD;
    }

    throw new BadRequestException(
        "SUCCESS import records require a verifiable Trainee or PlacementRecord entity",
        "UNVERIFIABLE_SUCCESS_ENTITY");
  }

  private Page<ImportRecordResponse> queryScopedRecordsPage(
      String basePredicate,
      List<Object> baseParams,
      CallerScope callerScope,
      ImportSecurityScopeValidator validator,
      Pageable pageable) {
    if (jdbcTemplate == null) {
      throw new IllegalStateException("JdbcTemplate is not configured");
    }
    StringBuilder fromWhere = new StringBuilder(
        " FROM import_records ir " +
        "JOIN import_batches ib ON ir.import_batch_id = ib.import_batch_id " +
        "LEFT JOIN users u ON ib.initiated_by_user_id = u.user_id " +
        "LEFT JOIN training_providers tp ON u.training_provider_id = tp.provider_id AND tp.deleted_at IS NULL " +
        "LEFT JOIN employers emp ON u.employer_id = emp.employer_id AND emp.deleted_at IS NULL " +
        "WHERE " + basePredicate);
    List<Object> params = new ArrayList<>(baseParams);
    validator.appendBatchTenantWhereClause(callerScope, fromWhere, params);

    Long totalObj = jdbcTemplate.queryForObject(
        "SELECT COUNT(*)" + fromWhere,
        Long.class,
        params.toArray());
    long total = totalObj != null ? totalObj : 0L;
    if (total == 0L || pageable.getOffset() >= total) {
      return new PageImpl<>(List.of(), pageable, total);
    }

    String orderBy = buildOrderByClause(pageable.getSort());
    List<Object> pageParams = new ArrayList<>(params);
    pageParams.add(pageable.getPageSize());
    pageParams.add(pageable.getOffset());

    List<Long> pageIds = jdbcTemplate.queryForList(
        "SELECT ir.import_record_id" + fromWhere + orderBy + " LIMIT ? OFFSET ?",
        Long.class,
        pageParams.toArray());
    if (pageIds.isEmpty()) {
      return new PageImpl<>(List.of(), pageable, total);
    }

    Map<Long, ImportRecordResponse> mappedById = new HashMap<>();
    for (ImportRecord rec : importRecordRepository.findAllById(pageIds)) {
      mappedById.put(rec.getId(), mapper.toResponse(rec));
    }

    List<ImportRecordResponse> ordered = new ArrayList<>(pageIds.size());
    for (Long recordId : pageIds) {
      ImportRecordResponse resp = mappedById.get(recordId);
      if (resp != null) {
        ordered.add(resp);
      }
    }
    return new PageImpl<>(ordered, pageable, total);
  }

  private String buildOrderByClause(Sort sort) {
    if (sort == null || sort.isUnsorted()) {
      return " ORDER BY ir.source_row_number ASC, ir.import_record_id ASC";
    }
    List<String> clauses = new ArrayList<>();
    for (Sort.Order order : sort) {
      String col = switch (order.getProperty()) {
        case "id" -> "ir.import_record_id";
        case "sourceRowNumber" -> "ir.source_row_number";
        case "entityType" -> "ir.entity_type";
        case "entityId" -> "ir.entity_id";
        case "errorCode" -> "ir.error_code";
        case "createdAt" -> "ir.created_at";
        case "updatedAt" -> "ir.updated_at";
        default -> "ir.import_record_id";
      };
      String dir = order.isDescending() ? "DESC" : "ASC";
      clauses.add(col + " " + dir);
    }
    if (clauses.isEmpty()) {
      return " ORDER BY ir.source_row_number ASC, ir.import_record_id ASC";
    }
    return " ORDER BY " + String.join(", ", clauses);
  }

  private ImportSecurityScopeValidator requireScopeValidator() {
    if (importSecurityScopeValidator == null) {
      throw new IllegalStateException("ImportSecurityScopeValidator is not configured");
    }
    return importSecurityScopeValidator;
  }
}
