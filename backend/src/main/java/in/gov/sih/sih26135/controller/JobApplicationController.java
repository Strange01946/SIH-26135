package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.request.CreateJobApplicationRequest;
import in.gov.sih.sih26135.dto.request.UpdateJobApplicationRequest;
import in.gov.sih.sih26135.dto.response.JobApplicationResponse;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.JobApplicationService;
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
 * REST controller for managing Job Application domain resources.
 *
 * <p>Base Route: /api/v1/job-applications
 * Consumes: CreateJobApplicationRequest, UpdateJobApplicationRequest
 * Produces: JobApplicationResponse enveloped in ApiResponse
 */
@RestController
@RequestMapping("/api/v1/job-applications")
@PreAuthorize("hasAuthority('placement.manage')")
public class JobApplicationController {

  private final JobApplicationService jobApplicationService;

  public JobApplicationController(JobApplicationService jobApplicationService) {
    this.jobApplicationService = jobApplicationService;
  }

  /**
   * Retrieves a job application by primary key identifier.
   *
   * @param id primary key identifier of the job application
   * @return 200 OK with JobApplicationResponse enveloped in ApiResponse
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<JobApplicationResponse>> getById(@PathVariable Long id) {
    JobApplicationResponse response = jobApplicationService.getJobApplicationById(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves the specific job application submitted by a trainee for a job posting.
   *
   * @param traineeId trainee identifier
   * @param jobPostingId job posting identifier
   * @return 200 OK with JobApplicationResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"traineeId", "jobPostingId"})
  public ResponseEntity<ApiResponse<JobApplicationResponse>> getByTraineeAndPosting(
      @RequestParam("traineeId") Long traineeId,
      @RequestParam("jobPostingId") Long jobPostingId) {
    JobApplicationResponse response = jobApplicationService.getJobApplicationByTraineeAndPosting(
        traineeId, jobPostingId);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves all job applications submitted by a specific trainee.
   *
   * @param traineeId trainee identifier
   * @return 200 OK with list of JobApplicationResponse enveloped in ApiResponse
   */
  @GetMapping(params = "traineeId")
  public ResponseEntity<ApiResponse<List<JobApplicationResponse>>> getByTrainee(
      @RequestParam("traineeId") Long traineeId) {
    List<JobApplicationResponse> applications = jobApplicationService.getJobApplicationsByTrainee(traineeId);
    return ResponseEntity.ok(ApiResponse.ok(applications));
  }

  /**
   * Retrieves all job applications received for a specific job posting.
   *
   * @param jobPostingId job posting identifier
   * @return 200 OK with list of JobApplicationResponse enveloped in ApiResponse
   */
  @GetMapping(params = "jobPostingId")
  public ResponseEntity<ApiResponse<List<JobApplicationResponse>>> getByPosting(
      @RequestParam("jobPostingId") Long jobPostingId) {
    List<JobApplicationResponse> applications = jobApplicationService.getJobApplicationsByPosting(jobPostingId);
    return ResponseEntity.ok(ApiResponse.ok(applications));
  }

  /**
   * Retrieves all job applications in a specific application status.
   *
   * @param statusId application status identifier
   * @return 200 OK with list of JobApplicationResponse enveloped in ApiResponse
   */
  @GetMapping(params = "statusId")
  public ResponseEntity<ApiResponse<List<JobApplicationResponse>>> getByStatus(
      @RequestParam("statusId") Long statusId) {
    List<JobApplicationResponse> applications = jobApplicationService.getJobApplicationsByStatus(statusId);
    return ResponseEntity.ok(ApiResponse.ok(applications));
  }

  /**
   * Creates a new job application record.
   *
   * @param request job application creation payload
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 201 Created with created JobApplicationResponse enveloped in ApiResponse
   */
  @PostMapping
  public ResponseEntity<ApiResponse<JobApplicationResponse>> createJobApplication(
      @RequestBody CreateJobApplicationRequest request,
      HttpServletRequest httpRequest) {
    JobApplicationResponse response = jobApplicationService.createJobApplication(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Job application created successfully", response, httpRequest.getRequestURI()));
  }

  /**
   * Updates an existing job application record.
   *
   * @param id primary key identifier of the job application to update
   * @param request job application update payload
   * @return 200 OK with updated JobApplicationResponse enveloped in ApiResponse
   */
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<JobApplicationResponse>> updateJobApplication(
      @PathVariable Long id,
      @RequestBody UpdateJobApplicationRequest request) {
    JobApplicationResponse response = jobApplicationService.updateJobApplication(id, request);
    return ResponseEntity.ok(ApiResponse.success("Job application updated successfully", response));
  }

  /**
   * Deletes a job application record.
   *
   * @param id primary key identifier of the job application to delete
   * @return 200 OK with success confirmation message
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteJobApplication(@PathVariable Long id) {
    jobApplicationService.deleteJobApplication(id);
    return ResponseEntity.ok(ApiResponse.success("Job application deleted successfully"));
  }
}
