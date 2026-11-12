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
import in.gov.sih.sih26135.service.ingestion.ImportContractSchema;
import in.gov.sih.sih26135.service.ingestion.ImportSecurityScopeValidator;
import in.gov.sih.sih26135.service.ingestion.ImportSecurityScopeValidator.CallerScope;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
  private final JdbcTemplate jdbcTemplate;

  @Autowired
  public ImportRecordServiceImpl(
      ImportRecordRepository importRecordRepository,
      ImportBatchRepository importBatchRepository,
      RefImportRecordStatusRepository refImportRecordStatusRepository,
      ImportRecordMapper mapper,
      ImportSecurityScopeValidator importSecurityScopeValidator,
      JdbcTemplate jdbcTemplate) {
    this.importRecordRepository = importRecordRepository;
    this.importBatchRepository = importBatchRepository;
    this.refImportRecordStatusRepository = refImportRecordStatusRepository;
    this.mapper = mapper;
    this.importSecurityScopeValidator = importSecurityScopeValidator;
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

    ImportBatch batch = importBatchRepository.findById(request.getImportBatchId())
        .orElseThrow(() -> new ResourceNotFoundException("ImportBatch", "id"));
    validator.validateCallerCanWriteBatch(callerScope, batch);

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

    LocalDateTime now = LocalDateTime.now();
    ImportRecord record = new ImportRecord(batch, request.getSourceRowNumber(), status);
    record.setEntityType(request.getEntityType() != null ? request.getEntityType().trim() : null);
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

    ImportRecord record = importRecordRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("ImportRecord", "id"));
    validator.validateCallerCanWriteBatch(callerScope, record.getImportBatch());

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

    return importRecordRepository.findByImportBatchId(batchId, pageable)
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
      return importRecordRepository.findByImportRecordStatusId(statusId, pageable)
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
      return importRecordRepository.findByEntityTypeAndEntityId(entityType.trim(), entityId, pageable)
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

    ImportRecord record = importRecordRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("ImportRecord", "id"));
    validator.validateCallerCanWriteBatch(callerScope, record.getImportBatch());

    importRecordRepository.delete(record);
    log.info("Deleted import record id={}", id);
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
