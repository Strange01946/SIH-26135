package in.gov.sih.sih26135.controller;

import jakarta.validation.Valid;
import in.gov.sih.sih26135.dto.request.CreateEmployerRequest;
import in.gov.sih.sih26135.dto.request.UpdateEmployerRequest;
import in.gov.sih.sih26135.dto.response.EmployerResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.response.PagedResponse;
import in.gov.sih.sih26135.response.PaginationUtils;
import in.gov.sih.sih26135.service.EmployerService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
 * REST controller for managing Employer domain resources.
 *
 * <p>Base Route: /api/v1/employers
 * Consumes: CreateEmployerRequest, UpdateEmployerRequest
 * Produces: EmployerResponse enveloped in ApiResponse
 */
@RestController
@RequestMapping("/api/v1/employers")
@PreAuthorize("hasAuthority('placement.manage')")
public class EmployerController {

  private final EmployerService employerService;

  public EmployerController(EmployerService employerService) {
    this.employerService = employerService;
  }

  /**
   * Retrieves an employer by primary key identifier.
   *
   * @param id primary key identifier of the employer
   * @return 200 OK with EmployerResponse enveloped in ApiResponse
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<EmployerResponse>> getById(@PathVariable Long id) {
    EmployerResponse response = employerService.getEmployerById(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves an employer by unique employer code.
   *
   * @param code unique employer code
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with EmployerResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"code", "!districtId", "!industryId", "!sectorId", "!includeDeleted"})
  public ResponseEntity<ApiResponse<EmployerResponse>> getByCode(
      @RequestParam("code") String code,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("code"));
    EmployerResponse response = employerService.getEmployerByCode(code);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves paginated employers located in a specific district.
   *
   * @param districtId district identifier
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: id)
   * @param direction sort direction (ASC or DESC, default: ASC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated EmployerResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"districtId", "!code", "!industryId", "!sectorId", "!includeDeleted"})
  public ResponseEntity<ApiResponse<PagedResponse<EmployerResponse>>> getByDistrict(
      @RequestParam("districtId") Long districtId,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size,
      @RequestParam(value = "sort", defaultValue = "id") String sort,
      @RequestParam(value = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("districtId", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.EMPLOYER_SORT_FIELDS, "id"
    );
    Page<EmployerResponse> employers = employerService.getEmployersByDistrict(districtId, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(employers)));
  }

  /**
   * Retrieves paginated employers operating within a specific industry.
   *
   * @param industryId industry identifier
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: id)
   * @param direction sort direction (ASC or DESC, default: ASC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated EmployerResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"industryId", "!code", "!districtId", "!sectorId", "!includeDeleted"})
  public ResponseEntity<ApiResponse<PagedResponse<EmployerResponse>>> getByIndustry(
      @RequestParam("industryId") Long industryId,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size,
      @RequestParam(value = "sort", defaultValue = "id") String sort,
      @RequestParam(value = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("industryId", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.EMPLOYER_SORT_FIELDS, "id"
    );
    Page<EmployerResponse> employers = employerService.getEmployersByIndustry(industryId, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(employers)));
  }

  /**
   * Retrieves paginated employers associated with a specific sector.
   *
   * @param sectorId sector identifier
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: id)
   * @param direction sort direction (ASC or DESC, default: ASC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated EmployerResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"sectorId", "!code", "!districtId", "!industryId", "!includeDeleted"})
  public ResponseEntity<ApiResponse<PagedResponse<EmployerResponse>>> getBySector(
      @RequestParam("sectorId") Long sectorId,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size,
      @RequestParam(value = "sort", defaultValue = "id") String sort,
      @RequestParam(value = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("sectorId", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.EMPLOYER_SORT_FIELDS, "id"
    );
    Page<EmployerResponse> employers = employerService.getEmployersBySector(sectorId, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(employers)));
  }

  /**
   * Retrieves paginated employers with soft-delete inclusion specified.
   *
   * @param includeDeleted whether to include soft-deleted employers
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: id)
   * @param direction sort direction (ASC or DESC, default: ASC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated EmployerResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"includeDeleted", "!code", "!districtId", "!industryId", "!sectorId"})
  public ResponseEntity<ApiResponse<PagedResponse<EmployerResponse>>> getByIncludeDeleted(
      @RequestParam("includeDeleted") boolean includeDeleted,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size,
      @RequestParam(value = "sort", defaultValue = "id") String sort,
      @RequestParam(value = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("includeDeleted", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.EMPLOYER_SORT_FIELDS, "id"
    );
    Page<EmployerResponse> employers = employerService.getAllEmployers(includeDeleted, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(employers)));
  }

  /**
   * Retrieves all active employers with pagination.
   *
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: id)
   * @param direction sort direction (ASC or DESC, default: ASC)
   * @param httpRequest HTTP servlet request to ensure unsupported query parameters are not silently accepted
   * @return 200 OK with paginated active EmployerResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"!code", "!districtId", "!industryId", "!sectorId", "!includeDeleted"})
  public ResponseEntity<ApiResponse<PagedResponse<EmployerResponse>>> getAllEmployers(
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size,
      @RequestParam(value = "sort", defaultValue = "id") String sort,
      @RequestParam(value = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.EMPLOYER_SORT_FIELDS, "id"
    );
    Page<EmployerResponse> employers = employerService.getAllEmployers(false, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(employers)));
  }

  private void validateAllowedQueryParameters(HttpServletRequest request, Set<String> allowedParams) {
    for (String paramName : request.getParameterMap().keySet()) {
      if (!allowedParams.contains(paramName)) {
        throw new BadRequestException("Unsupported query parameter: " + paramName, "UNSUPPORTED_PARAMETER");
      }
    }
  }

  /**
   * Creates a new employer record.
   *
   * @param request employer creation payload
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 201 Created with created EmployerResponse enveloped in ApiResponse
   */
  @PostMapping
  public ResponseEntity<ApiResponse<EmployerResponse>> createEmployer(
      @Valid @RequestBody CreateEmployerRequest request,
      HttpServletRequest httpRequest) {
    EmployerResponse response = employerService.createEmployer(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Employer created successfully", response, httpRequest.getRequestURI()));
  }

  /**
   * Updates an existing employer record.
   *
   * @param id primary key identifier of the employer to update
   * @param request employer update payload
   * @return 200 OK with updated EmployerResponse enveloped in ApiResponse
   */
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<EmployerResponse>> updateEmployer(
      @PathVariable Long id,
      @Valid @RequestBody UpdateEmployerRequest request) {
    EmployerResponse response = employerService.updateEmployer(id, request);
    return ResponseEntity.ok(ApiResponse.success("Employer updated successfully", response));
  }

  /**
   * Soft-deletes an employer record.
   *
   * @param id primary key identifier of the employer to delete
   * @return 200 OK with success confirmation message
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteEmployer(@PathVariable Long id) {
    employerService.deleteEmployer(id);
    return ResponseEntity.ok(ApiResponse.success("Employer deleted successfully"));
  }
}
