package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.request.CreateSchemeRequest;
import in.gov.sih.sih26135.dto.request.UpdateSchemeRequest;
import in.gov.sih.sih26135.dto.response.SchemeResponse;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.SchemeService;
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
 * REST controller for managing Scheme domain resources.
 *
 * <p>Base Route: /api/v1/schemes
 * Consumes: CreateSchemeRequest, UpdateSchemeRequest
 * Produces: SchemeResponse enveloped in ApiResponse
 */
@RestController
@RequestMapping("/api/v1/schemes")
@PreAuthorize("hasAuthority('program.manage')")
public class SchemeController {

  private final SchemeService schemeService;

  public SchemeController(SchemeService schemeService) {
    this.schemeService = schemeService;
  }

  /**
   * Retrieves a scheme by primary key identifier.
   *
   * @param id primary key identifier of the scheme
   * @return 200 OK with SchemeResponse enveloped in ApiResponse
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<SchemeResponse>> getById(@PathVariable Long id) {
    SchemeResponse response = schemeService.getById(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves schemes with optional filtering by scheme code or department ID.
   *
   * @param code optional scheme code filter
   * @param departmentId optional department identifier filter
   * @return 200 OK with list of schemes or single matched scheme enveloped in ApiResponse
   */
  @GetMapping
  public ResponseEntity<ApiResponse<?>> getSchemes(
      @RequestParam(value = "code", required = false) String code,
      @RequestParam(value = "departmentId", required = false) Long departmentId) {
    if (code != null && !code.isBlank()) {
      SchemeResponse response = schemeService.getByCode(code.trim());
      return ResponseEntity.ok(ApiResponse.ok(response));
    }
    if (departmentId != null) {
      List<SchemeResponse> schemes = schemeService.getSchemesByDepartmentId(departmentId);
      return ResponseEntity.ok(ApiResponse.ok(schemes));
    }
    List<SchemeResponse> schemes = schemeService.getAllSchemes();
    return ResponseEntity.ok(ApiResponse.ok(schemes));
  }

  /**
   * Creates a new scheme record.
   *
   * @param request scheme creation payload
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 201 Created with created SchemeResponse enveloped in ApiResponse
   */
  @PostMapping
  public ResponseEntity<ApiResponse<SchemeResponse>> createScheme(
      @RequestBody CreateSchemeRequest request,
      HttpServletRequest httpRequest) {
    SchemeResponse response = schemeService.createScheme(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Scheme created successfully", response, httpRequest.getRequestURI()));
  }

  /**
   * Updates an existing scheme record.
   *
   * @param id primary key identifier of the scheme to update
   * @param request scheme update payload
   * @return 200 OK with updated SchemeResponse enveloped in ApiResponse
   */
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<SchemeResponse>> updateScheme(
      @PathVariable Long id,
      @RequestBody UpdateSchemeRequest request) {
    SchemeResponse response = schemeService.updateScheme(id, request);
    return ResponseEntity.ok(ApiResponse.success("Scheme updated successfully", response));
  }

  /**
   * Soft-deletes a scheme record.
   *
   * @param id primary key identifier of the scheme to delete
   * @return 200 OK with success confirmation message
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteScheme(@PathVariable Long id) {
    schemeService.deleteScheme(id);
    return ResponseEntity.ok(ApiResponse.success("Scheme deleted successfully"));
  }
}
