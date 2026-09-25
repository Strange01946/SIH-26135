package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.request.CreateSalaryHistoryRequest;
import in.gov.sih.sih26135.dto.request.UpdateSalaryHistoryRequest;
import in.gov.sih.sih26135.dto.response.SalaryHistoryResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.SalaryHistoryService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
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
 * REST controller for managing Salary History domain resources.
 *
 * <p>Base Route: /api/v1/salary-history
 * Consumes: CreateSalaryHistoryRequest, UpdateSalaryHistoryRequest
 * Produces: SalaryHistoryResponse enveloped in ApiResponse
 */
@RestController
@RequestMapping("/api/v1/salary-history")
@PreAuthorize("hasAuthority('employment.manage')")
public class SalaryHistoryController {

  private final SalaryHistoryService salaryHistoryService;

  public SalaryHistoryController(SalaryHistoryService salaryHistoryService) {
    this.salaryHistoryService = salaryHistoryService;
  }

  /**
   * Retrieves a salary history record by primary key identifier.
   *
   * @param id primary key identifier of the salary history record
   * @return 200 OK with SalaryHistoryResponse enveloped in ApiResponse
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<SalaryHistoryResponse>> getById(@PathVariable Long id) {
    SalaryHistoryResponse response = salaryHistoryService.getSalaryHistoryById(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves all salary history records linked to a specific employment record.
   *
   * @param employmentRecordId employment record identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SalaryHistoryResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"employmentRecordId", "!traineeId", "!effectiveFrom", "!observationMonthOffset"})
  public ResponseEntity<ApiResponse<List<SalaryHistoryResponse>>> getByEmploymentRecord(
      @RequestParam("employmentRecordId") Long employmentRecordId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "employmentRecordId");
    List<SalaryHistoryResponse> records = salaryHistoryService.getSalaryHistoryByEmploymentRecord(employmentRecordId);
    return ResponseEntity.ok(ApiResponse.ok(records));
  }

  /**
   * Retrieves all salary history records for a specific trainee.
   *
   * @param traineeId trainee identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SalaryHistoryResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"traineeId", "!employmentRecordId", "!effectiveFrom", "!observationMonthOffset"})
  public ResponseEntity<ApiResponse<List<SalaryHistoryResponse>>> getByTrainee(
      @RequestParam("traineeId") Long traineeId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "traineeId");
    List<SalaryHistoryResponse> records = salaryHistoryService.getSalaryHistoryByTrainee(traineeId);
    return ResponseEntity.ok(ApiResponse.ok(records));
  }

  /**
   * Retrieves all salary history records with a specific effective from date.
   *
   * @param effectiveFrom effective from date (ISO-8601 YYYY-MM-DD)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SalaryHistoryResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"effectiveFrom", "!employmentRecordId", "!traineeId", "!observationMonthOffset"})
  public ResponseEntity<ApiResponse<List<SalaryHistoryResponse>>> getByEffectiveFrom(
      @RequestParam("effectiveFrom") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveFrom,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "effectiveFrom");
    List<SalaryHistoryResponse> records = salaryHistoryService.getSalaryHistoryByEffectiveFrom(effectiveFrom);
    return ResponseEntity.ok(ApiResponse.ok(records));
  }

  /**
   * Retrieves all salary history records associated with an observation milestone month offset.
   *
   * @param observationMonthOffset observation milestone month offset (e.g. 0, 6, 12, 24, 36)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SalaryHistoryResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"observationMonthOffset", "!employmentRecordId", "!traineeId", "!effectiveFrom"})
  public ResponseEntity<ApiResponse<List<SalaryHistoryResponse>>> getByObservationMilestone(
      @RequestParam("observationMonthOffset") Integer observationMonthOffset,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "observationMonthOffset");
    List<SalaryHistoryResponse> records = salaryHistoryService.getSalaryHistoryByObservationMilestone(observationMonthOffset);
    return ResponseEntity.ok(ApiResponse.ok(records));
  }

  /**
   * Creates a new salary history record.
   *
   * @param request salary history creation payload
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 201 Created with created SalaryHistoryResponse enveloped in ApiResponse
   */
  @PostMapping
  public ResponseEntity<ApiResponse<SalaryHistoryResponse>> createSalaryHistory(
      @RequestBody CreateSalaryHistoryRequest request,
      HttpServletRequest httpRequest) {
    SalaryHistoryResponse response = salaryHistoryService.createSalaryHistory(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Salary history created successfully", response, httpRequest.getRequestURI()));
  }

  /**
   * Updates an existing salary history record.
   *
   * @param id primary key identifier of the salary history record to update
   * @param request salary history update payload
   * @return 200 OK with updated SalaryHistoryResponse enveloped in ApiResponse
   */
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<SalaryHistoryResponse>> updateSalaryHistory(
      @PathVariable Long id,
      @RequestBody UpdateSalaryHistoryRequest request) {
    SalaryHistoryResponse response = salaryHistoryService.updateSalaryHistory(id, request);
    return ResponseEntity.ok(ApiResponse.success("Salary history updated successfully", response));
  }

  /**
   * Deletes a salary history record.
   *
   * @param id primary key identifier of the salary history record to delete
   * @return 200 OK with success confirmation message
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteSalaryHistory(@PathVariable Long id) {
    salaryHistoryService.deleteSalaryHistory(id);
    return ResponseEntity.ok(ApiResponse.success("Salary history deleted successfully"));
  }

  private void validateOnlyQueryParameter(HttpServletRequest request, String allowedParam) {
    for (String paramName : request.getParameterMap().keySet()) {
      if (!paramName.equals(allowedParam)) {
        throw new BadRequestException("Unsupported query parameter: " + paramName, "UNSUPPORTED_PARAMETER");
      }
    }
  }
}
