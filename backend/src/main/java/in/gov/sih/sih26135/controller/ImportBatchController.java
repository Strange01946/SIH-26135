package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.request.CreateImportBatchRequest;
import in.gov.sih.sih26135.dto.request.UpdateImportBatchRequest;
import in.gov.sih.sih26135.dto.response.ImportBatchProgressResponse;
import in.gov.sih.sih26135.dto.response.ImportBatchResponse;
import in.gov.sih.sih26135.dto.response.ImportRecordResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.response.PagedResponse;
import in.gov.sih.sih26135.response.PaginationUtils;
import in.gov.sih.sih26135.service.ImportBatchProcessingWorkflow;
import in.gov.sih.sih26135.service.ImportBatchService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller for managing Import Batch domain resources and staged bulk ingestion workflows.
 *
 * <p>Base Routes: /api/v1/import-batches, /api/v1/import/batches
 * Consumes: CreateImportBatchRequest, UpdateImportBatchRequest, multipart/form-data uploads
 * Produces: ImportBatchResponse, ImportBatchProgressResponse, ImportRecordResponse enveloped in ApiResponse
 */
@Validated
@RestController
@RequestMapping({"/api/v1/import-batches", "/api/v1/import/batches"})
@PreAuthorize("hasAnyAuthority('system.manage', 'audit.read', 'trainee.read', 'trainee.write', 'placement.manage')")
public class ImportBatchController {

  private final ImportBatchService importBatchService;
  private final ImportBatchProcessingWorkflow importBatchProcessingWorkflow;

  @Autowired
  public ImportBatchController(
      ImportBatchService importBatchService,
      ImportBatchProcessingWorkflow importBatchProcessingWorkflow) {
    this.importBatchService = importBatchService;
    this.importBatchProcessingWorkflow = importBatchProcessingWorkflow;
  }

  public ImportBatchController(ImportBatchService importBatchService) {
    this(importBatchService, null);
  }

  /**
   * Retrieves an import batch by primary key identifier.
   *
   * @param id primary key identifier of the import batch
   * @return 200 OK with ImportBatchResponse enveloped in ApiResponse
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<ImportBatchResponse>> getById(@PathVariable @Positive Long id) {
    ImportBatchResponse response = importBatchService.getBatchById(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves a single import batch by unique batch code.
   *
   * @param batchCode unique import batch code
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with ImportBatchResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "batchCode", "!statusId", "!entityType", "!sourceSystem", "!initiatedByUserId"
  })
  public ResponseEntity<ApiResponse<ImportBatchResponse>> getByBatchCode(
      @RequestParam("batchCode") @NotBlank String batchCode,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "batchCode");
    ImportBatchResponse response = importBatchService.getBatchByCode(batchCode);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves all import batches with a specific batch status.
   *
   * @param statusId import batch status reference identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of ImportBatchResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "statusId", "!batchCode", "!entityType", "!sourceSystem", "!initiatedByUserId"
  })
  public ResponseEntity<ApiResponse<List<ImportBatchResponse>>> getByStatusId(
      @RequestParam("statusId") @Positive Long statusId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "statusId");
    List<ImportBatchResponse> batches = importBatchService.getBatchesByStatusId(statusId);
    return ResponseEntity.ok(ApiResponse.ok(batches));
  }

  /**
   * Retrieves all import batches targeting a specific entity type.
   *
   * @param entityType target entity type name
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of ImportBatchResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "entityType", "!batchCode", "!statusId", "!sourceSystem", "!initiatedByUserId"
  })
  public ResponseEntity<ApiResponse<List<ImportBatchResponse>>> getByEntityType(
      @RequestParam("entityType") String entityType,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "entityType");
    List<ImportBatchResponse> batches = importBatchService.getBatchesByEntityType(entityType);
    return ResponseEntity.ok(ApiResponse.ok(batches));
  }

  /**
   * Retrieves all import batches originating from a specific source system.
   *
   * @param sourceSystem source system name
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of ImportBatchResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "sourceSystem", "!batchCode", "!statusId", "!entityType", "!initiatedByUserId"
  })
  public ResponseEntity<ApiResponse<List<ImportBatchResponse>>> getBySourceSystem(
      @RequestParam("sourceSystem") String sourceSystem,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "sourceSystem");
    List<ImportBatchResponse> batches = importBatchService.getBatchesBySourceSystem(sourceSystem);
    return ResponseEntity.ok(ApiResponse.ok(batches));
  }

  /**
   * Retrieves all import batches initiated by a specific user.
   *
   * @param initiatedByUserId initiating user identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of ImportBatchResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "initiatedByUserId", "!batchCode", "!statusId", "!entityType", "!sourceSystem"
  })
  public ResponseEntity<ApiResponse<List<ImportBatchResponse>>> getByInitiatedByUserId(
      @RequestParam("initiatedByUserId") @Positive Long initiatedByUserId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "initiatedByUserId");
    List<ImportBatchResponse> batches = importBatchService.getBatchesByInitiatedByUserId(initiatedByUserId);
    return ResponseEntity.ok(ApiResponse.ok(batches));
  }

  /**
   * Stages a multipart CSV, JSON, or NDJSON file for bulk import and registers a PENDING batch.
   *
   * @param file multipart import file
   * @param entityType target entity type (Trainee or PlacementRecord)
   * @param sourceSystem optional source system identifier
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 201 Created with staged ImportBatchResponse enveloped in ApiResponse
   */
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAnyAuthority('system.manage', 'trainee.write', 'placement.manage')")
  public ResponseEntity<ApiResponse<ImportBatchResponse>> uploadBatch(
      @RequestPart("file") MultipartFile file,
      @RequestParam(value = "entityType", required = false) String entityType,
      @RequestParam(value = "sourceSystem", required = false) String sourceSystem,
      HttpServletRequest httpRequest) {
    ImportBatchResponse response = requireWorkflow().stageUpload(file, entityType, sourceSystem);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(
            "Import batch staged successfully",
            response,
            httpRequest.getRequestURI()));
  }

  /**
   * Executes chunked validation, persistence, row quarantine, and finalization for a staged batch.
   *
   * @param id import batch identifier
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 200 OK with reconciled ImportBatchProgressResponse enveloped in ApiResponse
   */
  @PostMapping("/{id}/process")
  @PreAuthorize("hasAnyAuthority('system.manage', 'trainee.write', 'placement.manage')")
  public ResponseEntity<ApiResponse<ImportBatchProgressResponse>> processBatch(
      @PathVariable @Positive Long id,
      HttpServletRequest httpRequest) {
    ImportBatchProgressResponse response = requireWorkflow().processBatch(id);
    return ResponseEntity.ok(ApiResponse.success(
        "Import batch processed",
        response,
        httpRequest.getRequestURI()));
  }

  /**
   * Retrieves reconciled batch status and row outcome progress counters.
   *
   * @param id import batch identifier
   * @return 200 OK with ImportBatchProgressResponse enveloped in ApiResponse
   */
  @GetMapping("/{id}/progress")
  public ResponseEntity<ApiResponse<ImportBatchProgressResponse>> getBatchProgress(
      @PathVariable @Positive Long id) {
    ImportBatchProgressResponse response = requireWorkflow().getBatchProgress(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves paginated import records for a batch, optionally filtered by outcome status code.
   *
   * @param id import batch identifier
   * @param status optional outcome status filter (SUCCESS, FAILED, SKIPPED, DUPLICATE)
   * @param page zero-based page index
   * @param size page size (1..100)
   * @param sort sort field
   * @param direction sort direction (ASC or DESC)
   * @param httpRequest HTTP servlet request for query parameter allowlist verification
   * @return 200 OK with PagedResponse of ImportRecordResponse enveloped in ApiResponse
   */
  @GetMapping("/{id}/records")
  public ResponseEntity<ApiResponse<PagedResponse<ImportRecordResponse>>> getBatchRecords(
      @PathVariable @Positive Long id,
      @RequestParam(value = "status", required = false) String status,
      @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
      @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(100) int size,
      @RequestParam(value = "sort", defaultValue = "sourceRowNumber") String sort,
      @RequestParam(value = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("status", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.IMPORT_RECORD_SORT_FIELDS,
        "sourceRowNumber", Sort.Direction.ASC, Sort.Direction.ASC);
    Page<ImportRecordResponse> records = requireWorkflow().getBatchRecords(id, status, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(records)));
  }

  /**
   * Retrieves paginated quarantined rows (FAILED, plus optionally DUPLICATE) for an import batch.
   *
   * @param id import batch identifier
   * @param includeDuplicates whether DUPLICATE rows should be included alongside FAILED rows
   * @param page zero-based page index
   * @param size page size (1..100)
   * @param sort sort field
   * @param direction sort direction (ASC or DESC)
   * @param httpRequest HTTP servlet request for query parameter allowlist verification
   * @return 200 OK with PagedResponse of quarantined ImportRecordResponse enveloped in ApiResponse
   */
  @GetMapping("/{id}/quarantine")
  public ResponseEntity<ApiResponse<PagedResponse<ImportRecordResponse>>> getQuarantinedRecords(
      @PathVariable @Positive Long id,
      @RequestParam(value = "includeDuplicates", defaultValue = "true") boolean includeDuplicates,
      @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
      @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(100) int size,
      @RequestParam(value = "sort", defaultValue = "sourceRowNumber") String sort,
      @RequestParam(value = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(
        httpRequest, Set.of("includeDuplicates", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.IMPORT_RECORD_SORT_FIELDS,
        "sourceRowNumber", Sort.Direction.ASC, Sort.Direction.ASC);
    Page<ImportRecordResponse> records = requireWorkflow().getQuarantinedRecords(id, includeDuplicates, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(records)));
  }

  /**
   * Creates a new import batch.
   *
   * @param request batch creation payload
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 201 Created with created ImportBatchResponse enveloped in ApiResponse
   */
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasAuthority('system.manage')")
  public ResponseEntity<ApiResponse<ImportBatchResponse>> createImportBatch(
      @Valid @RequestBody CreateImportBatchRequest request,
      HttpServletRequest httpRequest) {
    ImportBatchResponse response = importBatchService.createImportBatch(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(
            "Import batch created successfully",
            response,
            httpRequest.getRequestURI()));
  }

  /**
   * Updates an existing import batch.
   *
   * @param id primary key identifier of the import batch to update
   * @param request batch update payload
   * @return 200 OK with updated ImportBatchResponse enveloped in ApiResponse
   */
  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('system.manage')")
  public ResponseEntity<ApiResponse<ImportBatchResponse>> updateImportBatch(
      @PathVariable @Positive Long id,
      @Valid @RequestBody UpdateImportBatchRequest request) {
    ImportBatchResponse response = importBatchService.updateImportBatch(id, request);
    return ResponseEntity.ok(ApiResponse.success(
        "Import batch updated successfully",
        response));
  }

  /**
   * Deletes an import batch.
   *
   * @param id primary key identifier of the import batch to delete
   * @return 200 OK with success confirmation message
   */
  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('system.manage')")
  public ResponseEntity<ApiResponse<Void>> deleteImportBatch(@PathVariable @Positive Long id) {
    importBatchService.deleteImportBatch(id);
    return ResponseEntity.ok(ApiResponse.success("Import batch deleted successfully"));
  }

  private void validateOnlyQueryParameter(HttpServletRequest request, String allowedParam) {
    for (String paramName : request.getParameterMap().keySet()) {
      if (!paramName.equals(allowedParam)) {
        throw new BadRequestException(
            "Unsupported query parameter: " + paramName,
            "UNSUPPORTED_PARAMETER"
        );
      }
    }
  }

  private void validateAllowedQueryParameters(HttpServletRequest request, Set<String> allowedParams) {
    for (String paramName : request.getParameterMap().keySet()) {
      if (!allowedParams.contains(paramName)) {
        throw new BadRequestException(
            "Unsupported query parameter: " + paramName,
            "UNSUPPORTED_PARAMETER"
        );
      }
    }
  }

  private ImportBatchProcessingWorkflow requireWorkflow() {
    if (importBatchProcessingWorkflow == null) {
      throw new IllegalStateException("ImportBatchProcessingWorkflow is not configured");
    }
    return importBatchProcessingWorkflow;
  }
}
