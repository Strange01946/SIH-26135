package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.request.CreateImportRecordRequest;
import in.gov.sih.sih26135.dto.request.UpdateImportRecordRequest;
import in.gov.sih.sih26135.dto.response.ImportRecordResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.response.PagedResponse;
import in.gov.sih.sih26135.response.PaginationUtils;
import in.gov.sih.sih26135.service.ImportRecordService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing Import Record domain resources.
 *
 * <p>Base Route: /api/v1/import-records
 * Consumes: CreateImportRecordRequest, UpdateImportRecordRequest
 * Produces: ImportRecordResponse and PagedResponse enveloped in ApiResponse
 */
@Validated
@RestController
@RequestMapping("/api/v1/import-records")
@PreAuthorize("hasAnyAuthority('system.manage', 'audit.read', 'trainee.read', 'trainee.write', 'placement.manage')")
public class ImportRecordController {

  private final ImportRecordService importRecordService;

  public ImportRecordController(ImportRecordService importRecordService) {
    this.importRecordService = importRecordService;
  }

  /**
   * Retrieves an import record by primary key identifier.
   *
   * @param id primary key identifier of the import record
   * @return 200 OK with ImportRecordResponse enveloped in ApiResponse
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<ImportRecordResponse>> getById(@PathVariable @Positive Long id) {
    ImportRecordResponse response = importRecordService.getRecordById(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves a single import record by import batch identifier and source row number.
   *
   * @param batchId import batch identifier
   * @param sourceRowNumber row number in the source file
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with ImportRecordResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "batchId", "sourceRowNumber", "!statusId", "!entityType", "!entityId"
  })
  public ResponseEntity<ApiResponse<ImportRecordResponse>> getByBatchIdAndSourceRowNumber(
      @RequestParam("batchId") @Positive Long batchId,
      @RequestParam("sourceRowNumber") @Positive Integer sourceRowNumber,
      HttpServletRequest httpRequest) {
    validateCompoundQueryParameters(httpRequest, Set.of("batchId", "sourceRowNumber"));
    ImportRecordResponse response = importRecordService.getRecordByBatchIdAndSourceRowNumber(batchId, sourceRowNumber);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves all import records for a specific import batch.
   *
   * @param batchId import batch identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of ImportRecordResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "batchId", "!sourceRowNumber", "!statusId", "!entityType", "!entityId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<ImportRecordResponse>>> getByBatchId(
      @RequestParam("batchId") @Positive Long batchId,
      @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
      @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(100) int size,
      @RequestParam(value = "sort", defaultValue = "sourceRowNumber") String sort,
      @RequestParam(value = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    validateCompoundQueryParameters(httpRequest, Set.of("batchId", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.IMPORT_RECORD_SORT_FIELDS,
        "sourceRowNumber", Sort.Direction.ASC, Sort.Direction.ASC);
    Page<ImportRecordResponse> records = importRecordService.getRecordsByBatchId(batchId, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(records)));
  }

  /**
   * Retrieves paginated import records matching a specific record status within caller scope.
   *
   * @param statusId import record status reference identifier
   * @param page zero-based page index
   * @param size page size (1..100)
   * @param sort sort field
   * @param direction sort direction (ASC or DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with PagedResponse of ImportRecordResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "statusId", "!batchId", "!sourceRowNumber", "!entityType", "!entityId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<ImportRecordResponse>>> getByStatusId(
      @RequestParam("statusId") @Positive Long statusId,
      @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
      @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(100) int size,
      @RequestParam(value = "sort", defaultValue = "sourceRowNumber") String sort,
      @RequestParam(value = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    validateCompoundQueryParameters(httpRequest, Set.of("statusId", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.IMPORT_RECORD_SORT_FIELDS,
        "sourceRowNumber", Sort.Direction.ASC, Sort.Direction.ASC);
    Page<ImportRecordResponse> records = importRecordService.getRecordsByStatusId(statusId, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(records)));
  }

  /**
   * Retrieves paginated import records linked to a specific entity within caller scope.
   *
   * @param entityType domain entity type name
   * @param entityId domain entity identifier
   * @param page zero-based page index
   * @param size page size (1..100)
   * @param sort sort field
   * @param direction sort direction (ASC or DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with PagedResponse of ImportRecordResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "entityType", "entityId", "!batchId", "!sourceRowNumber", "!statusId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<ImportRecordResponse>>> getByEntity(
      @RequestParam("entityType") String entityType,
      @RequestParam("entityId") @Positive Long entityId,
      @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
      @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(100) int size,
      @RequestParam(value = "sort", defaultValue = "sourceRowNumber") String sort,
      @RequestParam(value = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    validateCompoundQueryParameters(
        httpRequest, Set.of("entityType", "entityId", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.IMPORT_RECORD_SORT_FIELDS,
        "sourceRowNumber", Sort.Direction.ASC, Sort.Direction.ASC);
    Page<ImportRecordResponse> records = importRecordService.getRecordsByEntity(entityType, entityId, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(records)));
  }

  /**
   * Creates a new import record.
   *
   * @param request record creation payload
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 201 Created with created ImportRecordResponse enveloped in ApiResponse
   */
  @PostMapping
  @PreAuthorize("hasAuthority('system.manage')")
  public ResponseEntity<ApiResponse<ImportRecordResponse>> createImportRecord(
      @Valid @RequestBody CreateImportRecordRequest request,
      HttpServletRequest httpRequest) {
    ImportRecordResponse response = importRecordService.createImportRecord(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(
            "Import record created successfully",
            response,
            httpRequest.getRequestURI()));
  }

  /**
   * Updates an existing import record.
   *
   * @param id primary key identifier of the import record to update
   * @param request record update payload
   * @return 200 OK with updated ImportRecordResponse enveloped in ApiResponse
   */
  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('system.manage')")
  public ResponseEntity<ApiResponse<ImportRecordResponse>> updateImportRecord(
      @PathVariable @Positive Long id,
      @Valid @RequestBody UpdateImportRecordRequest request) {
    ImportRecordResponse response = importRecordService.updateImportRecord(id, request);
    return ResponseEntity.ok(ApiResponse.success(
        "Import record updated successfully",
        response));
  }

  /**
   * Deletes an import record.
   *
   * @param id primary key identifier of the import record to delete
   * @return 200 OK with success confirmation message
   */
  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('system.manage')")
  public ResponseEntity<ApiResponse<Void>> deleteImportRecord(@PathVariable @Positive Long id) {
    importRecordService.deleteImportRecord(id);
    return ResponseEntity.ok(ApiResponse.success("Import record deleted successfully"));
  }

  private void validateCompoundQueryParameters(HttpServletRequest request, Set<String> allowedParams) {
    for (String paramName : request.getParameterMap().keySet()) {
      if (!allowedParams.contains(paramName)) {
        throw new BadRequestException(
            "Unsupported query parameter: " + paramName,
            "UNSUPPORTED_PARAMETER"
        );
      }
    }
  }
}
