package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.config.ImportPipelineProperties;
import in.gov.sih.sih26135.dto.response.ImportBatchProgressResponse;
import in.gov.sih.sih26135.dto.response.ImportBatchResponse;
import in.gov.sih.sih26135.dto.response.ImportRecordResponse;
import in.gov.sih.sih26135.entity.ImportBatch;
import in.gov.sih.sih26135.entity.ImportRecord;
import in.gov.sih.sih26135.entity.RefImportBatchStatus;
import in.gov.sih.sih26135.entity.RefImportRecordStatus;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ImportBatchTerminalException;
import in.gov.sih.sih26135.exception.ImportRowLimitExceededException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.ImportBatchMapper;
import in.gov.sih.sih26135.mapper.ImportRecordMapper;
import in.gov.sih.sih26135.repository.ImportBatchRepository;
import in.gov.sih.sih26135.repository.ImportRecordRepository;
import in.gov.sih.sih26135.repository.RefImportBatchStatusRepository;
import in.gov.sih.sih26135.repository.RefImportRecordStatusRepository;
import in.gov.sih.sih26135.service.ImportBatchProcessingWorkflow;
import in.gov.sih.sih26135.service.ingestion.ImportContractSchema;
import in.gov.sih.sih26135.service.ingestion.ImportErrorSanitizer;
import in.gov.sih.sih26135.service.ingestion.ImportFileStagingService;
import in.gov.sih.sih26135.service.ingestion.ImportFileStagingService.FileFormat;
import in.gov.sih.sih26135.service.ingestion.ImportFileStagingService.StagedFileResult;
import in.gov.sih.sih26135.service.ingestion.ImportSecurityScopeValidator;
import in.gov.sih.sih26135.service.ingestion.ImportSecurityScopeValidator.CallerScope;
import in.gov.sih.sih26135.service.ingestion.ParsedImportRow;
import in.gov.sih.sih26135.service.ingestion.PlacementImportRowProcessor;
import in.gov.sih.sih26135.service.ingestion.RowOutcomeResult;
import in.gov.sih.sih26135.service.ingestion.RowOutcomeResult.ChunkSeenKeys;
import in.gov.sih.sih26135.service.ingestion.RowOutcomeResult.OutcomeType;
import in.gov.sih.sih26135.service.ingestion.StreamingImportParser;
import in.gov.sih.sih26135.service.ingestion.TraineeImportRowProcessor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

/**
 * Memory-bounded, multi-tenant, chunk-isolated implementation of {@link ImportBatchProcessingWorkflow}.
 *
 * <p>Guarantees:
 * <ul>
 *   <li>Upload staging streams directly to server-controlled storage and rolls back cleanly on failure.</li>
 *   <li>Batch claim uses pessimistic DB row locks ({@code FOR UPDATE}) and atomic {@code PENDING -> RUNNING}
 *       transition so concurrent workers cannot execute the same batch simultaneously.</li>
 *   <li>Parsing and chunked persistence execute OUTSIDE any outer file-spanning transaction; each chunk
 *       commits independently in {@code REQUIRES_NEW}, keeping prior committed chunks intact if a later chunk fails.</li>
 *   <li>Chunk failures trigger clean per-row isolated {@code REQUIRES_NEW} fallback transactions without
 *       ever reusing rollback-only transactions.</li>
 *   <li>Final batch counters ({@code row_count}, {@code success_count}, {@code failure_count}) are
 *       reconciled directly from committed {@code import_records} rows, and {@code completed_at >= started_at}
 *       is strictly enforced.</li>
 * </ul>
 */
@Service
public class ImportBatchProcessingWorkflowImpl implements ImportBatchProcessingWorkflow {

  private static final Logger log = LoggerFactory.getLogger(ImportBatchProcessingWorkflowImpl.class);

  private static final Set<String> VALID_RECORD_STATUSES = Set.of("SUCCESS", "FAILED", "SKIPPED", "DUPLICATE");

  private record RecordStatusIds(Long successId, Long failedId, Long skippedId, Long duplicateId) {}

  private record ClaimedBatchContext(
      Long batchId,
      String batchCode,
      String normalizedEntityType,
      LocalDateTime startedAt,
      RecordStatusIds statusIds
  ) {}

  private record ReconciledCounts(int rowCount, int successCount, int failureCount, int duplicateCount, int skippedCount) {}

  private final ImportPipelineProperties properties;
  private final ImportFileStagingService fileStagingService;
  private final ImportSecurityScopeValidator importSecurityScopeValidator;
  private final TraineeImportRowProcessor traineeRowProcessor;
  private final PlacementImportRowProcessor placementRowProcessor;
  private final ImportBatchRepository importBatchRepository;
  private final ImportRecordRepository importRecordRepository;
  private final RefImportBatchStatusRepository refImportBatchStatusRepository;
  private final RefImportRecordStatusRepository refImportRecordStatusRepository;
  private final ImportBatchMapper batchMapper;
  private final ImportRecordMapper recordMapper;
  private final JdbcTemplate jdbcTemplate;
  private final TransactionTemplate requiresNewTxTemplate;

  @PersistenceContext
  private EntityManager entityManager;

  public ImportBatchProcessingWorkflowImpl(
      ImportPipelineProperties properties,
      ImportFileStagingService fileStagingService,
      ImportSecurityScopeValidator importSecurityScopeValidator,
      TraineeImportRowProcessor traineeRowProcessor,
      PlacementImportRowProcessor placementRowProcessor,
      ImportBatchRepository importBatchRepository,
      ImportRecordRepository importRecordRepository,
      RefImportBatchStatusRepository refImportBatchStatusRepository,
      RefImportRecordStatusRepository refImportRecordStatusRepository,
      ImportBatchMapper batchMapper,
      ImportRecordMapper recordMapper,
      JdbcTemplate jdbcTemplate,
      PlatformTransactionManager transactionManager) {
    this.properties = Objects.requireNonNull(properties);
    this.fileStagingService = Objects.requireNonNull(fileStagingService);
    this.importSecurityScopeValidator = Objects.requireNonNull(importSecurityScopeValidator);
    this.traineeRowProcessor = Objects.requireNonNull(traineeRowProcessor);
    this.placementRowProcessor = Objects.requireNonNull(placementRowProcessor);
    this.importBatchRepository = Objects.requireNonNull(importBatchRepository);
    this.importRecordRepository = Objects.requireNonNull(importRecordRepository);
    this.refImportBatchStatusRepository = Objects.requireNonNull(refImportBatchStatusRepository);
    this.refImportRecordStatusRepository = Objects.requireNonNull(refImportRecordStatusRepository);
    this.batchMapper = Objects.requireNonNull(batchMapper);
    this.recordMapper = Objects.requireNonNull(recordMapper);
    this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate);
    this.requiresNewTxTemplate = new TransactionTemplate(Objects.requireNonNull(transactionManager));
    this.requiresNewTxTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  @Override
  public ImportBatchResponse stageUpload(MultipartFile file, String entityType, String sourceSystem) {
    String normalizedEntityType = importSecurityScopeValidator.normalizeImportEntityType(entityType);
    CallerScope callerScope = importSecurityScopeValidator.resolveAndValidateUploadOrProcessCaller(normalizedEntityType);

    String effectiveSourceSystem = sanitizeSourceSystem(sourceSystem);
    FileFormat format = fileStagingService.detectAndValidateFormat(file);
    String batchCode = fileStagingService.generateUniqueBatchCode();

    StagedFileResult staged = fileStagingService.stageFile(file, batchCode, format);
    try {
      ImportBatchResponse created = requiresNewTxTemplate.execute(status -> {
        RefImportBatchStatus pendingStatus = refImportBatchStatusRepository.findByStatusCode("PENDING")
            .orElseThrow(() -> new ResourceNotFoundException("RefImportBatchStatus", "statusCode=PENDING"));

        LocalDateTime now = LocalDateTime.now();
        ImportBatch batch = new ImportBatch(
            batchCode,
            effectiveSourceSystem,
            normalizedEntityType,
            pendingStatus
        );
        batch.setInitiatedByUserId(callerScope.caller().getId());
        batch.setRowCount(0);
        batch.setSuccessCount(0);
        batch.setFailureCount(0);
        batch.setCreatedAt(now);
        batch.setUpdatedAt(now);

        ImportBatch saved = importBatchRepository.saveAndFlush(batch);
        return batchMapper.toResponse(saved);
      });

      if (created == null || !fileStagingService.hasStagedFile(batchCode)) {
        fileStagingService.deleteStagedFileQuietly(staged.stagedPath());
        throw new IllegalStateException("Staged upload persistence verification failed");
      }

      log.info("Staged bulk import batch id={}, batchCode={}, entityType={}, format={}, bytes={}",
          created.id(), batchCode, normalizedEntityType, format, staged.byteCount());
      return created;
    } catch (RuntimeException ex) {
      fileStagingService.deleteStagedFileQuietly(staged.stagedPath());
      throw ex;
    }
  }

  @Override
  public ImportBatchProgressResponse processBatch(Long batchId) {
    if (batchId == null || batchId <= 0) {
      throw new BadRequestException("Import batch ID must be a positive integer", "INVALID_BATCH_ID");
    }

    // 1. Atomic claim transaction (PENDING -> RUNNING under DB FOR UPDATE lock)
    ClaimedBatchContext claimed = claimBatchForProcessing(batchId);
    CallerScope callerScope = importSecurityScopeValidator.resolveAndValidateUploadOrProcessCaller(
        claimed.normalizedEntityType());

    boolean fatalFailure = false;
    RuntimeException deferredFailure = null;
    int chunkSize = properties.getChunkSize();
    int maxRows = properties.getMaxRowsPerBatch();

    // 2. Streaming parse + bounded chunk transactions (NO outer transaction spanning entire file)
    try (StreamingImportParser parser = fileStagingService.openParser(
        claimed.batchCode(), claimed.normalizedEntityType())) {

      List<ParsedImportRow> chunk = new ArrayList<>(chunkSize);
      ParsedImportRow row;
      while ((row = parser.nextRow()) != null) {
        if (row.sourceRowNumber() > maxRows) {
          if (!chunk.isEmpty()) {
            processChunkWithIsolation(claimed, callerScope, chunk);
            chunk.clear();
          }
          fatalFailure = true;
          deferredFailure = new ImportRowLimitExceededException(row.sourceRowNumber(), maxRows);
          break;
        }
        chunk.add(row);
        if (chunk.size() >= chunkSize) {
          processChunkWithIsolation(claimed, callerScope, chunk);
          chunk.clear();
        }
      }

      if (!fatalFailure && !chunk.isEmpty()) {
        processChunkWithIsolation(claimed, callerScope, chunk);
        chunk.clear();
      }
    } catch (StreamingImportParser.ImportStreamParseException parseEx) {
      fatalFailure = true;
      log.warn("Fatal stream parse failure for batchId={}, code={}: {}",
          batchId, parseEx.getErrorCode(), parseEx.getMessage());
      deferredFailure = new BadRequestException(
          ImportErrorSanitizer.sanitizeErrorMessage(parseEx.getMessage(), "Malformed import file"),
          ImportErrorSanitizer.sanitizeErrorCode(parseEx.getErrorCode(), "MALFORMED_IMPORT_FILE")
      );
    } catch (RuntimeException ex) {
      fatalFailure = true;
      log.error("Unexpected failure processing batchId={}: {}", batchId, ex.getMessage(), ex);
      deferredFailure = ex;
    } catch (Exception ioEx) {
      fatalFailure = true;
      log.error("Failed closing import stream for batchId={}: {}", batchId, ioEx.getMessage());
      deferredFailure = new BadRequestException("Import file stream processing failed", "IMPORT_STREAM_ERROR");
    }

    // 3. Reconcile counters from committed import_records & finalize batch in isolated tx
    boolean finalFatal = fatalFailure;
    ImportBatchProgressResponse finalized;
    try {
      finalized = finalizeBatch(claimed.batchId(), finalFatal, claimed.startedAt());
    } catch (RuntimeException finalizeEx) {
      markBatchFailedSafely(claimed.batchId(), claimed.startedAt());
      throw finalizeEx;
    }

    if (!finalFatal && finalized.rowCount() == 0) {
      throw new BadRequestException("Import file contains no data records", "NO_DATA_ROWS");
    }

    if (deferredFailure != null) {
      throw deferredFailure;
    }
    return finalized;
  }

  @Override
  @Transactional(readOnly = true)
  public ImportBatchProgressResponse getBatchProgress(Long batchId) {
    if (batchId == null || batchId <= 0) {
      throw new BadRequestException("Import batch ID must be a positive integer", "INVALID_BATCH_ID");
    }
    CallerScope callerScope = importSecurityScopeValidator.resolveAndValidateReadCaller(null);
    ImportBatch batch = importBatchRepository.findById(batchId)
        .orElseThrow(() -> new ResourceNotFoundException("ImportBatch", "id"));
    importSecurityScopeValidator.validateCallerCanAccessBatch(callerScope, batch);

    ReconciledCounts counts = queryCommittedCounts(batchId);
    return buildProgressResponse(batch, counts);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<ImportRecordResponse> getBatchRecords(Long batchId, String statusCode, Pageable pageable) {
    if (batchId == null || batchId <= 0) {
      throw new BadRequestException("Import batch ID must be a positive integer", "INVALID_BATCH_ID");
    }
    CallerScope callerScope = importSecurityScopeValidator.resolveAndValidateReadCaller(null);
    ImportBatch batch = importBatchRepository.findById(batchId)
        .orElseThrow(() -> new ResourceNotFoundException("ImportBatch", "id"));
    importSecurityScopeValidator.validateCallerCanAccessBatch(callerScope, batch);

    if (statusCode != null && !statusCode.isBlank()) {
      String normalizedStatus = statusCode.trim().toUpperCase(Locale.ROOT);
      if (!VALID_RECORD_STATUSES.contains(normalizedStatus)) {
        throw new BadRequestException("Unsupported import record status filter: " + normalizedStatus, "INVALID_STATUS");
      }
      return importRecordRepository.findByImportBatchIdAndImportRecordStatusStatusCodeIn(
              batchId, List.of(normalizedStatus), pageable)
          .map(recordMapper::toResponse);
    }
    return importRecordRepository.findByImportBatchId(batchId, pageable)
        .map(recordMapper::toResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<ImportRecordResponse> getQuarantinedRecords(Long batchId, boolean includeDuplicates, Pageable pageable) {
    if (batchId == null || batchId <= 0) {
      throw new BadRequestException("Import batch ID must be a positive integer", "INVALID_BATCH_ID");
    }
    CallerScope callerScope = importSecurityScopeValidator.resolveAndValidateReadCaller(null);
    ImportBatch batch = importBatchRepository.findById(batchId)
        .orElseThrow(() -> new ResourceNotFoundException("ImportBatch", "id"));
    importSecurityScopeValidator.validateCallerCanAccessBatch(callerScope, batch);

    List<String> targetStatuses = includeDuplicates
        ? List.of("FAILED", "DUPLICATE")
        : List.of("FAILED");
    return importRecordRepository.findByImportBatchIdAndImportRecordStatusStatusCodeIn(
            batchId, targetStatuses, pageable)
        .map(recordMapper::toResponse);
  }

  private ClaimedBatchContext claimBatchForProcessing(Long batchId) {
    return requiresNewTxTemplate.execute(status -> {
      try {
        jdbcTemplate.queryForObject(
            "SELECT import_batch_id FROM import_batches WHERE import_batch_id = ? FOR UPDATE",
            Long.class, batchId);
      } catch (EmptyResultDataAccessException e) {
        throw new ResourceNotFoundException("ImportBatch", "id");
      }

      ImportBatch batch = importBatchRepository.findById(batchId)
          .orElseThrow(() -> new ResourceNotFoundException("ImportBatch", "id"));
      entityManager.refresh(batch);

      String normalizedEntityType = importSecurityScopeValidator.normalizeImportEntityType(batch.getEntityType());
      CallerScope callerScope = importSecurityScopeValidator.resolveAndValidateUploadOrProcessCaller(normalizedEntityType);
      importSecurityScopeValidator.validateCallerCanAccessBatch(callerScope, batch);

      RefImportBatchStatus currentStatus = batch.getImportBatchStatus();
      String statusCode = currentStatus != null && currentStatus.getStatusCode() != null
          ? currentStatus.getStatusCode().toUpperCase(Locale.ROOT)
          : "PENDING";

      if (currentStatus != null && Boolean.TRUE.equals(currentStatus.getIsTerminalFlag())) {
        throw new ImportBatchTerminalException(batchId, statusCode);
      }
      if ("RUNNING".equals(statusCode)) {
        throw new ConflictException(
            "Import batch is already being processed",
            "IMPORT_BATCH_ALREADY_RUNNING");
      }
      if (!"PENDING".equals(statusCode)) {
        throw new ConflictException(
            "Import batch cannot be processed from status: " + statusCode,
            "IMPORT_BATCH_INVALID_STATE");
      }

      RefImportBatchStatus runningStatus = refImportBatchStatusRepository.findByStatusCode("RUNNING")
          .orElseThrow(() -> new ResourceNotFoundException("RefImportBatchStatus", "statusCode=RUNNING"));
      RecordStatusIds recordStatusIds = loadRecordStatusIds();

      LocalDateTime startedAt = LocalDateTime.now();
      batch.setImportBatchStatus(runningStatus);
      batch.setStartedAt(startedAt);
      batch.setCompletedAt(null);
      batch.setRowCount(0);
      batch.setSuccessCount(0);
      batch.setFailureCount(0);
      batch.setUpdatedAt(startedAt);
      importBatchRepository.saveAndFlush(batch);

      return new ClaimedBatchContext(batchId, batch.getBatchCode(), normalizedEntityType, startedAt, recordStatusIds);
    });
  }

  private void processChunkWithIsolation(
      ClaimedBatchContext claimed,
      CallerScope callerScope,
      List<ParsedImportRow> chunk) {
    if (chunk == null || chunk.isEmpty()) {
      return;
    }

    try {
      executeChunkTransaction(claimed, callerScope, chunk);
    } catch (RuntimeException chunkEx) {
      // Whole-chunk transaction rolled back cleanly; fall back to single-row isolated transactions.
      log.warn("Chunk write failed for batchId={} ({} rows), isolating rows: {}",
          claimed.batchId(), chunk.size(), chunkEx.getClass().getSimpleName());
      isolateChunkRowByRow(claimed, callerScope, chunk);
    }
  }

  private void executeChunkTransaction(
      ClaimedBatchContext claimed,
      CallerScope callerScope,
      List<ParsedImportRow> chunk) {
    requiresNewTxTemplate.executeWithoutResult(status -> {
      Set<Integer> alreadyRecorded = findRecordedRowNumbers(claimed.batchId(), chunk);
      ChunkSeenKeys seenKeys = new ChunkSeenKeys();

      for (ParsedImportRow row : chunk) {
        if (alreadyRecorded.contains(row.sourceRowNumber())) {
          continue;
        }
        RowOutcomeResult outcome = processSingleRow(
            row, claimed.normalizedEntityType(), callerScope, seenKeys);
        persistImportRecord(claimed.batchId(), row.sourceRowNumber(), outcome, claimed.statusIds());
      }
      entityManager.flush();
      entityManager.clear();
    });
  }

  private void isolateChunkRowByRow(
      ClaimedBatchContext claimed,
      CallerScope callerScope,
      List<ParsedImportRow> chunk) {
    for (ParsedImportRow row : chunk) {
      boolean completed = false;
      for (int attempt = 0; attempt < 2 && !completed; attempt++) {
        try {
          ChunkSeenKeys singleRowKeys = new ChunkSeenKeys();
          requiresNewTxTemplate.executeWithoutResult(status -> {
            if (isRowAlreadyRecorded(claimed.batchId(), row.sourceRowNumber())) {
              return;
            }
            RowOutcomeResult outcome = processSingleRow(
                row, claimed.normalizedEntityType(), callerScope, singleRowKeys);
            persistImportRecord(claimed.batchId(), row.sourceRowNumber(), outcome, claimed.statusIds());
            entityManager.flush();
            entityManager.clear();
          });
          completed = true;
        } catch (TransientDataAccessException transientEx) {
          if (attempt == 1) {
            recordQuarantinedFailureAfterRollback(claimed, row.sourceRowNumber(), transientEx);
            completed = true;
          }
        } catch (RuntimeException rowEx) {
          recordQuarantinedFailureAfterRollback(claimed, row.sourceRowNumber(), rowEx);
          completed = true;
        }
      }
    }
  }

  private void recordQuarantinedFailureAfterRollback(
      ClaimedBatchContext claimed,
      int sourceRowNumber,
      RuntimeException rowEx) {
    ImportErrorSanitizer.DbFailureClassification classified =
        ImportErrorSanitizer.classifyDatabaseFailure(rowEx);
    RowOutcomeResult quarantined = classified.duplicate()
        ? RowOutcomeResult.duplicate(claimed.normalizedEntityType(), classified.errorCode(), classified.errorMessage())
        : RowOutcomeResult.failed(claimed.normalizedEntityType(), classified.errorCode(), classified.errorMessage());

    requiresNewTxTemplate.executeWithoutResult(status -> {
      if (isRowAlreadyRecorded(claimed.batchId(), sourceRowNumber)) {
        return;
      }
      persistImportRecord(claimed.batchId(), sourceRowNumber, quarantined, claimed.statusIds());
      entityManager.flush();
      entityManager.clear();
    });
  }

  private RowOutcomeResult processSingleRow(
      ParsedImportRow row,
      String entityType,
      CallerScope callerScope,
      ChunkSeenKeys seenKeys) {
    if (row.rowKind() == ParsedImportRow.RowKind.SKIPPED) {
      return RowOutcomeResult.skipped(entityType, row.errorCode(), row.errorMessage());
    }
    if (row.rowKind() == ParsedImportRow.RowKind.MALFORMED) {
      return RowOutcomeResult.failed(entityType, row.errorCode(), row.errorMessage());
    }

    if (ImportContractSchema.ENTITY_TRAINEE.equals(entityType)) {
      return traineeRowProcessor.validateAndPersist(row, callerScope, seenKeys);
    }
    if (ImportContractSchema.ENTITY_PLACEMENT_RECORD.equals(entityType)) {
      return placementRowProcessor.validateAndPersist(row, callerScope, seenKeys);
    }
    return RowOutcomeResult.failed(entityType, "UNSUPPORTED_IMPORT_ENTITY_TYPE",
        "Unsupported import entity type");
  }

  private void persistImportRecord(
      Long batchId,
      int sourceRowNumber,
      RowOutcomeResult outcome,
      RecordStatusIds statusIds) {
    Long targetStatusId = switch (outcome.outcomeType()) {
      case SUCCESS -> statusIds.successId();
      case FAILED -> statusIds.failedId();
      case SKIPPED -> statusIds.skippedId();
      case DUPLICATE -> statusIds.duplicateId();
    };

    ImportBatch batchRef = entityManager.getReference(ImportBatch.class, batchId);
    RefImportRecordStatus statusRef = entityManager.getReference(RefImportRecordStatus.class, targetStatusId);

    LocalDateTime now = LocalDateTime.now();
    ImportRecord rec = new ImportRecord(batchRef, sourceRowNumber, statusRef);
    rec.setEntityType(outcome.entityType());
    rec.setEntityId(outcome.outcomeType() == OutcomeType.SUCCESS ? outcome.entityId() : null);
    rec.setErrorCode(outcome.outcomeType() == OutcomeType.SUCCESS ? null : outcome.errorCode());
    rec.setErrorMessage(outcome.outcomeType() == OutcomeType.SUCCESS ? null : outcome.errorMessage());
    rec.setCreatedAt(now);
    rec.setUpdatedAt(now);
    importRecordRepository.save(rec);
  }

  private ImportBatchProgressResponse finalizeBatch(Long batchId, boolean fatalFailure, LocalDateTime startedAt) {
    return requiresNewTxTemplate.execute(status -> {
      jdbcTemplate.queryForObject(
          "SELECT import_batch_id FROM import_batches WHERE import_batch_id = ? FOR UPDATE",
          Long.class, batchId);

      ImportBatch batch = importBatchRepository.findById(batchId)
          .orElseThrow(() -> new ResourceNotFoundException("ImportBatch", "id"));
      entityManager.refresh(batch);

      ReconciledCounts counts = queryCommittedCounts(batchId);
      boolean effectiveFatal = fatalFailure || counts.rowCount() == 0;

      String targetStatusCode = effectiveFatal ? "FAILED" : "COMPLETED";
      RefImportBatchStatus terminalStatus = refImportBatchStatusRepository.findByStatusCode(targetStatusCode)
          .orElseThrow(() -> new ResourceNotFoundException("RefImportBatchStatus", "statusCode=" + targetStatusCode));

      LocalDateTime effectiveStartedAt = batch.getStartedAt() != null ? batch.getStartedAt() : startedAt;
      if (effectiveStartedAt == null) {
        effectiveStartedAt = LocalDateTime.now();
        batch.setStartedAt(effectiveStartedAt);
      }

      LocalDateTime completedAt = LocalDateTime.now();
      if (completedAt.isBefore(effectiveStartedAt)) {
        completedAt = effectiveStartedAt;
      }

      batch.setImportBatchStatus(terminalStatus);
      batch.setRowCount(counts.rowCount());
      batch.setSuccessCount(counts.successCount());
      batch.setFailureCount(counts.failureCount());
      batch.setCompletedAt(completedAt);
      batch.setUpdatedAt(completedAt);

      ImportBatch saved = importBatchRepository.saveAndFlush(batch);
      log.info("Finalized import batch id={}, status={}, rowCount={}, success={}, failed={}, duplicate={}, skipped={}",
          batchId, targetStatusCode, counts.rowCount(), counts.successCount(),
          counts.failureCount(), counts.duplicateCount(), counts.skippedCount());
      return buildProgressResponse(saved, counts);
    });
  }

  private void markBatchFailedSafely(Long batchId, LocalDateTime startedAt) {
    try {
      finalizeBatch(batchId, true, startedAt);
    } catch (Exception ignored) {
      log.warn("Fallback mark-failed finalization could not run for batchId={}", batchId);
    }
  }

  private ReconciledCounts queryCommittedCounts(Long batchId) {
    List<Map<String, Object>> rows = jdbcTemplate.queryForList(
        "SELECT rirs.status_code AS status_code, COUNT(*) AS cnt " +
            "FROM import_records ir " +
            "JOIN ref_import_record_status rirs ON ir.import_record_status_id = rirs.import_record_status_id " +
            "WHERE ir.import_batch_id = ? " +
            "GROUP BY rirs.status_code",
        batchId);

    Map<String, Integer> map = new HashMap<>();
    for (Map<String, Object> r : rows) {
      String code = r.get("status_code") != null ? r.get("status_code").toString().toUpperCase(Locale.ROOT) : "";
      int cnt = r.get("cnt") != null ? ((Number) r.get("cnt")).intValue() : 0;
      map.put(code, cnt);
    }

    int success = map.getOrDefault("SUCCESS", 0);
    int failed = map.getOrDefault("FAILED", 0);
    int duplicate = map.getOrDefault("DUPLICATE", 0);
    int skipped = map.getOrDefault("SKIPPED", 0);
    int total = success + failed + duplicate + skipped;
    return new ReconciledCounts(total, success, failed, duplicate, skipped);
  }

  private ImportBatchProgressResponse buildProgressResponse(ImportBatch batch, ReconciledCounts counts) {
    String statusCode = batch.getImportBatchStatus() != null ? batch.getImportBatchStatus().getStatusCode() : null;
    Boolean terminal = batch.getImportBatchStatus() != null ? batch.getImportBatchStatus().getIsTerminalFlag() : Boolean.FALSE;
    Long durationMs = (batch.getStartedAt() != null && batch.getCompletedAt() != null)
        ? Math.max(0L, Duration.between(batch.getStartedAt(), batch.getCompletedAt()).toMillis())
        : null;

    return new ImportBatchProgressResponse(
        batch.getId(),
        batch.getBatchCode(),
        batch.getSourceSystem(),
        batch.getEntityType(),
        statusCode,
        terminal,
        batch.getInitiatedByUserId(),
        counts.rowCount(),
        counts.successCount(),
        counts.failureCount(),
        counts.duplicateCount(),
        counts.skippedCount(),
        batch.getStartedAt(),
        batch.getCompletedAt(),
        durationMs
    );
  }

  private RecordStatusIds loadRecordStatusIds() {
    Long successId = refImportRecordStatusRepository.findByStatusCode("SUCCESS")
        .orElseThrow(() -> new ResourceNotFoundException("RefImportRecordStatus", "statusCode=SUCCESS"))
        .getId();
    Long failedId = refImportRecordStatusRepository.findByStatusCode("FAILED")
        .orElseThrow(() -> new ResourceNotFoundException("RefImportRecordStatus", "statusCode=FAILED"))
        .getId();
    Long skippedId = refImportRecordStatusRepository.findByStatusCode("SKIPPED")
        .orElseThrow(() -> new ResourceNotFoundException("RefImportRecordStatus", "statusCode=SKIPPED"))
        .getId();
    Long duplicateId = refImportRecordStatusRepository.findByStatusCode("DUPLICATE")
        .orElseThrow(() -> new ResourceNotFoundException("RefImportRecordStatus", "statusCode=DUPLICATE"))
        .getId();
    return new RecordStatusIds(successId, failedId, skippedId, duplicateId);
  }

  private Set<Integer> findRecordedRowNumbers(Long batchId, List<ParsedImportRow> chunk) {
    if (chunk.isEmpty()) {
      return Set.of();
    }
    String placeholders = chunk.stream().map(r -> "?").collect(Collectors.joining(", "));
    List<Object> params = new ArrayList<>(chunk.size() + 1);
    params.add(batchId);
    for (ParsedImportRow row : chunk) {
      params.add(row.sourceRowNumber());
    }
    List<Integer> existing = jdbcTemplate.queryForList(
        "SELECT source_row_number FROM import_records WHERE import_batch_id = ? AND source_row_number IN (" + placeholders + ")",
        Integer.class, params.toArray());
    return new HashSet<>(existing);
  }

  private boolean isRowAlreadyRecorded(Long batchId, int sourceRowNumber) {
    Integer count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM import_records WHERE import_batch_id = ? AND source_row_number = ?",
        Integer.class, batchId, sourceRowNumber);
    return count != null && count > 0;
  }

  private String sanitizeSourceSystem(String sourceSystem) {
    if (sourceSystem == null || sourceSystem.isBlank()) {
      return "PORTAL_UPLOAD";
    }
    String trimmed = sourceSystem.trim();
    if (trimmed.length() > 64) {
      throw new BadRequestException("sourceSystem cannot exceed 64 characters", "SOURCE_SYSTEM_TOO_LONG");
    }
    return trimmed;
  }
}
