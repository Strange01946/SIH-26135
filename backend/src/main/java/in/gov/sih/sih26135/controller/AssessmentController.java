package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.request.CreateAssessmentRequest;
import in.gov.sih.sih26135.dto.request.UpdateAssessmentRequest;
import in.gov.sih.sih26135.dto.response.AssessmentResponse;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.AssessmentService;
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

/**
 * REST controller for managing Assessment event domain resources.
 *
 * <p>Base Route: /api/v1/assessments
 * Consumes: CreateAssessmentRequest, UpdateAssessmentRequest
 * Produces: AssessmentResponse enveloped in ApiResponse
 */
@RestController
@RequestMapping("/api/v1/assessments")
public class AssessmentController {

  private final AssessmentService assessmentService;

  public AssessmentController(AssessmentService assessmentService) {
    this.assessmentService = assessmentService;
  }

  /**
   * Retrieves an assessment event by primary key identifier.
   *
   * @param id primary key identifier of the assessment
   * @return 200 OK with AssessmentResponse enveloped in ApiResponse
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<AssessmentResponse>> getById(@PathVariable Long id) {
    AssessmentResponse response = assessmentService.getById(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves an assessment event by unique assessment code.
   *
   * @param code unique assessment code
   * @return 200 OK with AssessmentResponse enveloped in ApiResponse
   */
  @GetMapping(params = "code")
  public ResponseEntity<ApiResponse<AssessmentResponse>> getByCode(
      @RequestParam("code") String code) {
    AssessmentResponse response = assessmentService.getByCode(code);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves all assessments scheduled for a specific course.
   *
   * @param courseId course identifier
   * @return 200 OK with list of AssessmentResponse enveloped in ApiResponse
   */
  @GetMapping(params = "courseId")
  public ResponseEntity<ApiResponse<List<AssessmentResponse>>> getByCourseId(
      @RequestParam("courseId") Long courseId) {
    List<AssessmentResponse> assessments = assessmentService.getByCourseId(courseId);
    return ResponseEntity.ok(ApiResponse.ok(assessments));
  }

  /**
   * Retrieves all assessments scheduled for a specific training batch.
   *
   * @param batchId training batch identifier
   * @return 200 OK with list of AssessmentResponse enveloped in ApiResponse
   */
  @GetMapping(params = "batchId")
  public ResponseEntity<ApiResponse<List<AssessmentResponse>>> getByBatchId(
      @RequestParam("batchId") Long batchId) {
    List<AssessmentResponse> assessments = assessmentService.getByBatchId(batchId);
    return ResponseEntity.ok(ApiResponse.ok(assessments));
  }

  /**
   * Retrieves all assessments under a specific program.
   *
   * @param programId program identifier
   * @return 200 OK with list of AssessmentResponse enveloped in ApiResponse
   */
  @GetMapping(params = "programId")
  public ResponseEntity<ApiResponse<List<AssessmentResponse>>> getByProgramId(
      @RequestParam("programId") Long programId) {
    List<AssessmentResponse> assessments = assessmentService.getByProgramId(programId);
    return ResponseEntity.ok(ApiResponse.ok(assessments));
  }

  /**
   * Retrieves all assessments of a specific assessment type.
   *
   * @param assessmentTypeId assessment type identifier
   * @return 200 OK with list of AssessmentResponse enveloped in ApiResponse
   */
  @GetMapping(params = "assessmentTypeId")
  public ResponseEntity<ApiResponse<List<AssessmentResponse>>> getByAssessmentTypeId(
      @RequestParam("assessmentTypeId") Long assessmentTypeId) {
    List<AssessmentResponse> assessments = assessmentService.getByAssessmentTypeId(assessmentTypeId);
    return ResponseEntity.ok(ApiResponse.ok(assessments));
  }

  /**
   * Retrieves all assessments scheduled on a specific assessment date.
   *
   * @param assessmentDate assessment date (ISO-8601 YYYY-MM-DD)
   * @return 200 OK with list of AssessmentResponse enveloped in ApiResponse
   */
  @GetMapping(params = "assessmentDate")
  public ResponseEntity<ApiResponse<List<AssessmentResponse>>> getByAssessmentDate(
      @RequestParam("assessmentDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate assessmentDate) {
    List<AssessmentResponse> assessments = assessmentService.getByAssessmentDate(assessmentDate);
    return ResponseEntity.ok(ApiResponse.ok(assessments));
  }

  /**
   * Retrieves all assessments in a specific lifecycle status.
   *
   * @param lifecycleStatusId lifecycle status identifier
   * @return 200 OK with list of AssessmentResponse enveloped in ApiResponse
   */
  @GetMapping(params = "lifecycleStatusId")
  public ResponseEntity<ApiResponse<List<AssessmentResponse>>> getByLifecycleStatusId(
      @RequestParam("lifecycleStatusId") Long lifecycleStatusId) {
    List<AssessmentResponse> assessments = assessmentService.getByLifecycleStatusId(lifecycleStatusId);
    return ResponseEntity.ok(ApiResponse.ok(assessments));
  }

  /**
   * Retrieves all assessment events.
   *
   * @return 200 OK with list of all AssessmentResponse enveloped in ApiResponse
   */
  @GetMapping
  public ResponseEntity<ApiResponse<List<AssessmentResponse>>> getAllAssessments() {
    List<AssessmentResponse> assessments = assessmentService.getAllAssessments();
    return ResponseEntity.ok(ApiResponse.ok(assessments));
  }

  /**
   * Creates a new assessment event.
   *
   * @param request assessment creation payload
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 201 Created with created AssessmentResponse enveloped in ApiResponse
   */
  @PostMapping
  public ResponseEntity<ApiResponse<AssessmentResponse>> createAssessment(
      @RequestBody CreateAssessmentRequest request,
      HttpServletRequest httpRequest) {
    AssessmentResponse response = assessmentService.createAssessment(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Assessment created successfully", response, httpRequest.getRequestURI()));
  }

  /**
   * Updates an existing assessment event.
   *
   * @param id primary key identifier of the assessment to update
   * @param request assessment update payload
   * @return 200 OK with updated AssessmentResponse enveloped in ApiResponse
   */
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<AssessmentResponse>> updateAssessment(
      @PathVariable Long id,
      @RequestBody UpdateAssessmentRequest request) {
    AssessmentResponse response = assessmentService.updateAssessment(id, request);
    return ResponseEntity.ok(ApiResponse.success("Assessment updated successfully", response));
  }

  /**
   * Soft-deletes an assessment event.
   *
   * @param id primary key identifier of the assessment to delete
   * @return 200 OK with success confirmation message
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteAssessment(@PathVariable Long id) {
    assessmentService.deleteAssessment(id);
    return ResponseEntity.ok(ApiResponse.success("Assessment deleted successfully"));
  }
}
