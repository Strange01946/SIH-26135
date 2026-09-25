package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.request.CreateImportRecordRequest;
import in.gov.sih.sih26135.dto.request.UpdateImportRecordRequest;
import in.gov.sih.sih26135.dto.response.ImportRecordResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.ImportRecordService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;
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
 * REST controller for managing Import Record domain resources.
 *
 * <p>Base Route: /api/v1/import-records
 * Consumes: CreateImportRecordRequest, UpdateImportRecordRequest
 * Produces: ImportRecordResponse enveloped in ApiResponse
 */
@RestController
@RequestMapping("/api/v1/import-records")
@PreAuthorize("hasAnyAuthority('system.manage', 'audit.read')")
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
  public ResponseEntity<ApiResponse<ImportRecordResponse>> getById(@PathVariable Long id) {
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
      @RequestParam("batchId") Long batchId,
      @RequestParam("sourceRowNumber") Integer sourceRowNumber,
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
  public ResponseEntity<ApiResponse<List<ImportRecordResponse>>> getByBatchId(
      @RequestParam("batchId") Long batchId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "batchId");
    List<ImportRecordResponse> records = importRecordService.getRecordsByBatchId(batchId);
    return ResponseEntity.ok(ApiResponse.ok(records));
  }

  /**
   * Retrieves all import records with a specific record status.
   *
   * @param statusId import record status reference identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of ImportRecordResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "statusId", "!batchId", "!sourceRowNumber", "!entityType", "!entityId"
  })
  public ResponseEntity<ApiResponse<List<ImportRecordResponse>>> getByStatusId(
      @RequestParam("statusId") Long statusId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "statusId");
    List<ImportRecordResponse> records = importRecordService.getRecordsByStatusId(statusId);
    return ResponseEntity.ok(ApiResponse.ok(records));
  }

  /**
   * Retrieves all import records linked to a specific entity type and entity identifier.
   *
   * @param entityType target entity type name
   * @param entityId target entity primary key identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of ImportRecordResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "entityType", "entityId", "!batchId", "!sourceRowNumber", "!statusId"
  })
  public ResponseEntity<ApiResponse<List<ImportRecordResponse>>> getByEntity(
      @RequestParam("entityType") String entityType,
      @RequestParam("entityId") Long entityId,
      HttpServletRequest httpRequest) {
    validateCompoundQueryParameters(httpRequest, Set.of("entityType", "entityId"));
    List<ImportRecordResponse> records = importRecordService.getRecordsByEntity(entityType, entityId);
    return ResponseEntity.ok(ApiResponse.ok(records));
  }

  /**
   * Creates a new import record.
   *
   * @param request record creation payload
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 201 Created with created ImportRecordResponse enveloped in ApiResponse
   */
  @PostMapping
  public ResponseEntity<ApiResponse<ImportRecordResponse>> createImportRecord(
      @RequestBody CreateImportRecordRequest request,
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
  public ResponseEntity<ApiResponse<ImportRecordResponse>> updateImportRecord(
      @PathVariable Long id,
      @RequestBody UpdateImportRecordRequest request) {
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
  public ResponseEntity<ApiResponse<Void>> deleteImportRecord(@PathVariable Long id) {
    importRecordService.deleteImportRecord(id);
    return ResponseEntity.ok(ApiResponse.success("Import record deleted successfully"));
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
