package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.request.CreateImportBatchRequest;
import in.gov.sih.sih26135.dto.request.UpdateImportBatchRequest;
import in.gov.sih.sih26135.dto.response.ImportBatchResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.ImportBatchService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * REST controller for managing Import Batch domain resources.
 *
 * <p>Base Route: /api/v1/import-batches
 * Consumes: CreateImportBatchRequest, UpdateImportBatchRequest
 * Produces: ImportBatchResponse enveloped in ApiResponse
 */
@RestController
@RequestMapping("/api/v1/import-batches")
@PreAuthorize("hasAnyAuthority('system.manage', 'audit.read')")
public class ImportBatchController {

  private final ImportBatchService importBatchService;

  public ImportBatchController(ImportBatchService importBatchService) {
    this.importBatchService = importBatchService;
  }

  /**
   * Retrieves an import batch by primary key identifier.
   *
   * @param id primary key identifier of the import batch
   * @return 200 OK with ImportBatchResponse enveloped in ApiResponse
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<ImportBatchResponse>> getById(@PathVariable Long id) {
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
      @RequestParam("batchCode") String batchCode,
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
      @RequestParam("statusId") Long statusId,
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
      @RequestParam("initiatedByUserId") Long initiatedByUserId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "initiatedByUserId");
    List<ImportBatchResponse> batches = importBatchService.getBatchesByInitiatedByUserId(initiatedByUserId);
    return ResponseEntity.ok(ApiResponse.ok(batches));
  }

  /**
   * Creates a new import batch.
   *
   * @param request batch creation payload
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 201 Created with created ImportBatchResponse enveloped in ApiResponse
   */
  @PostMapping
  public ResponseEntity<ApiResponse<ImportBatchResponse>> createImportBatch(
      @RequestBody CreateImportBatchRequest request,
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
  public ResponseEntity<ApiResponse<ImportBatchResponse>> updateImportBatch(
      @PathVariable Long id,
      @RequestBody UpdateImportBatchRequest request) {
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
  public ResponseEntity<ApiResponse<Void>> deleteImportBatch(@PathVariable Long id) {
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
}
