package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.request.CreateEmployerRequest;
import in.gov.sih.sih26135.dto.request.UpdateEmployerRequest;
import in.gov.sih.sih26135.dto.response.EmployerResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.EmployerService;
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

/**
 * REST controller for managing Employer domain resources.
 *
 * <p>Base Route: /api/v1/employers
 * Consumes: CreateEmployerRequest, UpdateEmployerRequest
 * Produces: EmployerResponse enveloped in ApiResponse
 */
@RestController
@RequestMapping("/api/v1/employers")
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
   * @return 200 OK with EmployerResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"code", "!districtId", "!industryId", "!sectorId", "!includeDeleted"})
  public ResponseEntity<ApiResponse<EmployerResponse>> getByCode(
      @RequestParam("code") String code,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "code");
    EmployerResponse response = employerService.getEmployerByCode(code);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves all employers located in a specific district.
   *
   * @param districtId district identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of EmployerResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"districtId", "!code", "!industryId", "!sectorId", "!includeDeleted"})
  public ResponseEntity<ApiResponse<List<EmployerResponse>>> getByDistrict(
      @RequestParam("districtId") Long districtId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "districtId");
    List<EmployerResponse> employers = employerService.getEmployersByDistrict(districtId);
    return ResponseEntity.ok(ApiResponse.ok(employers));
  }

  /**
   * Retrieves all employers operating within a specific industry.
   *
   * @param industryId industry identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of EmployerResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"industryId", "!code", "!districtId", "!sectorId", "!includeDeleted"})
  public ResponseEntity<ApiResponse<List<EmployerResponse>>> getByIndustry(
      @RequestParam("industryId") Long industryId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "industryId");
    List<EmployerResponse> employers = employerService.getEmployersByIndustry(industryId);
    return ResponseEntity.ok(ApiResponse.ok(employers));
  }

  /**
   * Retrieves all employers associated with a specific sector.
   *
   * @param sectorId sector identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of EmployerResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"sectorId", "!code", "!districtId", "!industryId", "!includeDeleted"})
  public ResponseEntity<ApiResponse<List<EmployerResponse>>> getBySector(
      @RequestParam("sectorId") Long sectorId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "sectorId");
    List<EmployerResponse> employers = employerService.getEmployersBySector(sectorId);
    return ResponseEntity.ok(ApiResponse.ok(employers));
  }

  /**
   * Retrieves all employers with soft-delete inclusion specified.
   *
   * @param includeDeleted whether to include soft-deleted employers
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of EmployerResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"includeDeleted", "!code", "!districtId", "!industryId", "!sectorId"})
  public ResponseEntity<ApiResponse<List<EmployerResponse>>> getByIncludeDeleted(
      @RequestParam("includeDeleted") boolean includeDeleted,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "includeDeleted");
    List<EmployerResponse> employers = employerService.getAllEmployers(includeDeleted);
    return ResponseEntity.ok(ApiResponse.ok(employers));
  }

  /**
   * Retrieves all active employers.
   *
   * @param httpRequest HTTP servlet request to ensure unsupported query parameters are not silently accepted
   * @return 200 OK with list of active EmployerResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"!code", "!districtId", "!industryId", "!sectorId", "!includeDeleted"})
  public ResponseEntity<ApiResponse<List<EmployerResponse>>> getAllEmployers(
      HttpServletRequest httpRequest) {
    if (!httpRequest.getParameterMap().isEmpty()) {
      throw new BadRequestException("Unsupported query parameters: " + httpRequest.getParameterMap().keySet());
    }
    List<EmployerResponse> employers = employerService.getAllEmployers(false);
    return ResponseEntity.ok(ApiResponse.ok(employers));
  }

  private void validateOnlyQueryParameter(HttpServletRequest request, String allowedParam) {
    for (String paramName : request.getParameterMap().keySet()) {
      if (!paramName.equals(allowedParam)) {
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
      @RequestBody CreateEmployerRequest request,
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
      @RequestBody UpdateEmployerRequest request) {
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
