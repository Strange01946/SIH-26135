package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.request.CreateJobPostingRequest;
import in.gov.sih.sih26135.dto.request.UpdateJobPostingRequest;
import in.gov.sih.sih26135.dto.response.JobPostingResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.JobPostingService;
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
 * REST controller for managing Job Posting domain resources.
 *
 * <p>Base Route: /api/v1/job-postings
 * Consumes: CreateJobPostingRequest, UpdateJobPostingRequest
 * Produces: JobPostingResponse enveloped in ApiResponse
 */
@RestController
@RequestMapping("/api/v1/job-postings")
public class JobPostingController {

  private final JobPostingService jobPostingService;

  public JobPostingController(JobPostingService jobPostingService) {
    this.jobPostingService = jobPostingService;
  }

  /**
   * Retrieves a job posting by primary key identifier.
   *
   * @param id primary key identifier of the job posting
   * @return 200 OK with JobPostingResponse enveloped in ApiResponse
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<JobPostingResponse>> getById(@PathVariable Long id) {
    JobPostingResponse response = jobPostingService.getJobPostingById(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves a job posting by unique posting code.
   *
   * @param code unique posting code
   * @return 200 OK with JobPostingResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"code", "!employerId", "!jobRoleId", "!districtId", "!includeDeleted"})
  public ResponseEntity<ApiResponse<JobPostingResponse>> getByCode(
      @RequestParam("code") String code,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "code");
    JobPostingResponse response = jobPostingService.getJobPostingByCode(code);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves all job postings published by a specific employer.
   *
   * @param employerId employer identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of JobPostingResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"employerId", "!code", "!jobRoleId", "!districtId", "!includeDeleted"})
  public ResponseEntity<ApiResponse<List<JobPostingResponse>>> getByEmployer(
      @RequestParam("employerId") Long employerId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "employerId");
    List<JobPostingResponse> postings = jobPostingService.getJobPostingsByEmployer(employerId);
    return ResponseEntity.ok(ApiResponse.ok(postings));
  }

  /**
   * Retrieves all job postings for a specific job role.
   *
   * @param jobRoleId job role identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of JobPostingResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"jobRoleId", "!code", "!employerId", "!districtId", "!includeDeleted"})
  public ResponseEntity<ApiResponse<List<JobPostingResponse>>> getByJobRole(
      @RequestParam("jobRoleId") Long jobRoleId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "jobRoleId");
    List<JobPostingResponse> postings = jobPostingService.getJobPostingsByJobRole(jobRoleId);
    return ResponseEntity.ok(ApiResponse.ok(postings));
  }

  /**
   * Retrieves all job postings located in a specific district.
   *
   * @param districtId district identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of JobPostingResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"districtId", "!code", "!employerId", "!jobRoleId", "!includeDeleted"})
  public ResponseEntity<ApiResponse<List<JobPostingResponse>>> getByDistrict(
      @RequestParam("districtId") Long districtId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "districtId");
    List<JobPostingResponse> postings = jobPostingService.getJobPostingsByDistrict(districtId);
    return ResponseEntity.ok(ApiResponse.ok(postings));
  }

  /**
   * Retrieves all job postings with soft-delete inclusion specified.
   *
   * @param includeDeleted whether to include soft-deleted job postings
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of JobPostingResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"includeDeleted", "!code", "!employerId", "!jobRoleId", "!districtId"})
  public ResponseEntity<ApiResponse<List<JobPostingResponse>>> getByIncludeDeleted(
      @RequestParam("includeDeleted") boolean includeDeleted,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "includeDeleted");
    List<JobPostingResponse> postings = jobPostingService.getAllJobPostings(includeDeleted);
    return ResponseEntity.ok(ApiResponse.ok(postings));
  }

  /**
   * Retrieves all active job postings.
   *
   * @param httpRequest HTTP servlet request to ensure unsupported query parameters are not silently accepted
   * @return 200 OK with list of active JobPostingResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"!code", "!employerId", "!jobRoleId", "!districtId", "!includeDeleted"})
  public ResponseEntity<ApiResponse<List<JobPostingResponse>>> getAllJobPostings(
      HttpServletRequest httpRequest) {
    if (!httpRequest.getParameterMap().isEmpty()) {
      throw new BadRequestException("Unsupported query parameters: " + httpRequest.getParameterMap().keySet());
    }
    List<JobPostingResponse> postings = jobPostingService.getAllJobPostings(false);
    return ResponseEntity.ok(ApiResponse.ok(postings));
  }

  private void validateOnlyQueryParameter(HttpServletRequest request, String allowedParam) {
    for (String paramName : request.getParameterMap().keySet()) {
      if (!paramName.equals(allowedParam)) {
        throw new BadRequestException("Unsupported query parameter: " + paramName, "UNSUPPORTED_PARAMETER");
      }
    }
  }

  /**
   * Creates a new job posting record.
   *
   * @param request job posting creation payload
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 201 Created with created JobPostingResponse enveloped in ApiResponse
   */
  @PostMapping
  public ResponseEntity<ApiResponse<JobPostingResponse>> createJobPosting(
      @RequestBody CreateJobPostingRequest request,
      HttpServletRequest httpRequest) {
    JobPostingResponse response = jobPostingService.createJobPosting(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Job posting created successfully", response, httpRequest.getRequestURI()));
  }

  /**
   * Updates an existing job posting record.
   *
   * @param id primary key identifier of the job posting to update
   * @param request job posting update payload
   * @return 200 OK with updated JobPostingResponse enveloped in ApiResponse
   */
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<JobPostingResponse>> updateJobPosting(
      @PathVariable Long id,
      @RequestBody UpdateJobPostingRequest request) {
    JobPostingResponse response = jobPostingService.updateJobPosting(id, request);
    return ResponseEntity.ok(ApiResponse.success("Job posting updated successfully", response));
  }

  /**
   * Soft-deletes a job posting record.
   *
   * @param id primary key identifier of the job posting to delete
   * @return 200 OK with success confirmation message
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteJobPosting(@PathVariable Long id) {
    jobPostingService.deleteJobPosting(id);
    return ResponseEntity.ok(ApiResponse.success("Job posting deleted successfully"));
  }
}
