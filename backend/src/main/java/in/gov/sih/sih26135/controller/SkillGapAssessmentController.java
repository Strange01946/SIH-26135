package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.request.CreateSkillGapAssessmentRequest;
import in.gov.sih.sih26135.dto.request.UpdateSkillGapAssessmentRequest;
import in.gov.sih.sih26135.dto.response.SkillGapAssessmentResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.SkillGapAssessmentService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
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
 * REST controller for managing Skill Gap Assessment domain resources.
 *
 * <p>Base Route: /api/v1/skill-gap-assessments
 * Consumes: CreateSkillGapAssessmentRequest, UpdateSkillGapAssessmentRequest
 * Produces: SkillGapAssessmentResponse enveloped in ApiResponse
 */
@RestController
@RequestMapping("/api/v1/skill-gap-assessments")
@PreAuthorize("hasAuthority('assessment.manage')")
public class SkillGapAssessmentController {

  private final SkillGapAssessmentService skillGapAssessmentService;

  public SkillGapAssessmentController(SkillGapAssessmentService skillGapAssessmentService) {
    this.skillGapAssessmentService = skillGapAssessmentService;
  }

  /**
   * Retrieves a skill gap assessment by primary key identifier.
   *
   * @param id primary key identifier of the assessment
   * @return 200 OK with SkillGapAssessmentResponse enveloped in ApiResponse
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<SkillGapAssessmentResponse>> getById(@PathVariable Long id) {
    SkillGapAssessmentResponse response = skillGapAssessmentService.getAssessmentById(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves a single skill gap assessment by unique assessment number.
   *
   * @param assessmentNumber unique assessment number
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with SkillGapAssessmentResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "assessmentNumber", "!traineeId", "!assessedOn", "!enrollmentId", "!courseId",
      "!batchId", "!jobRoleId", "!jobPostingId", "!employmentId", "!placementId",
      "!traineeAssessmentId", "!assessmentResultId", "!certificationId", "!surveyResponseId",
      "!followupTaskId", "!employmentVerificationId", "!sourceId", "!statusId"
  })
  public ResponseEntity<ApiResponse<SkillGapAssessmentResponse>> getByAssessmentNumber(
      @RequestParam("assessmentNumber") String assessmentNumber,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "assessmentNumber");
    SkillGapAssessmentResponse response = skillGapAssessmentService.getAssessmentByNumber(assessmentNumber);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves skill gap assessments for a specific trainee on a specific assessment date.
   *
   * @param traineeId trainee identifier
   * @param assessedOn assessment date
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SkillGapAssessmentResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "traineeId", "assessedOn", "!assessmentNumber", "!enrollmentId", "!courseId",
      "!batchId", "!jobRoleId", "!jobPostingId", "!employmentId", "!placementId",
      "!traineeAssessmentId", "!assessmentResultId", "!certificationId", "!surveyResponseId",
      "!followupTaskId", "!employmentVerificationId", "!sourceId", "!statusId"
  })
  public ResponseEntity<ApiResponse<List<SkillGapAssessmentResponse>>> getByTraineeAndDate(
      @RequestParam("traineeId") Long traineeId,
      @RequestParam("assessedOn") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate assessedOn,
      HttpServletRequest httpRequest) {
    validateCompoundQueryParameters(httpRequest, Set.of("traineeId", "assessedOn"));
    List<SkillGapAssessmentResponse> assessments =
        skillGapAssessmentService.getAssessmentsByTraineeAndDate(traineeId, assessedOn);
    return ResponseEntity.ok(ApiResponse.ok(assessments));
  }

  /**
   * Retrieves all skill gap assessments for a specific trainee.
   *
   * @param traineeId trainee identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SkillGapAssessmentResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "traineeId", "!assessedOn", "!assessmentNumber", "!enrollmentId", "!courseId",
      "!batchId", "!jobRoleId", "!jobPostingId", "!employmentId", "!placementId",
      "!traineeAssessmentId", "!assessmentResultId", "!certificationId", "!surveyResponseId",
      "!followupTaskId", "!employmentVerificationId", "!sourceId", "!statusId"
  })
  public ResponseEntity<ApiResponse<List<SkillGapAssessmentResponse>>> getByTrainee(
      @RequestParam("traineeId") Long traineeId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "traineeId");
    List<SkillGapAssessmentResponse> assessments = skillGapAssessmentService.getAssessmentsByTrainee(traineeId);
    return ResponseEntity.ok(ApiResponse.ok(assessments));
  }

  /**
   * Retrieves all skill gap assessments performed on a specific date.
   *
   * @param assessedOn assessment date
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SkillGapAssessmentResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "assessedOn", "!traineeId", "!assessmentNumber", "!enrollmentId", "!courseId",
      "!batchId", "!jobRoleId", "!jobPostingId", "!employmentId", "!placementId",
      "!traineeAssessmentId", "!assessmentResultId", "!certificationId", "!surveyResponseId",
      "!followupTaskId", "!employmentVerificationId", "!sourceId", "!statusId"
  })
  public ResponseEntity<ApiResponse<List<SkillGapAssessmentResponse>>> getByDate(
      @RequestParam("assessedOn") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate assessedOn,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "assessedOn");
    List<SkillGapAssessmentResponse> assessments = skillGapAssessmentService.getAssessmentsByDate(assessedOn);
    return ResponseEntity.ok(ApiResponse.ok(assessments));
  }

  /**
   * Retrieves all skill gap assessments linked to a specific training enrollment.
   *
   * @param enrollmentId training enrollment identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SkillGapAssessmentResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "enrollmentId", "!assessmentNumber", "!traineeId", "!assessedOn", "!courseId",
      "!batchId", "!jobRoleId", "!jobPostingId", "!employmentId", "!placementId",
      "!traineeAssessmentId", "!assessmentResultId", "!certificationId", "!surveyResponseId",
      "!followupTaskId", "!employmentVerificationId", "!sourceId", "!statusId"
  })
  public ResponseEntity<ApiResponse<List<SkillGapAssessmentResponse>>> getByEnrollment(
      @RequestParam("enrollmentId") Long enrollmentId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "enrollmentId");
    List<SkillGapAssessmentResponse> assessments = skillGapAssessmentService.getAssessmentsByEnrollment(enrollmentId);
    return ResponseEntity.ok(ApiResponse.ok(assessments));
  }

  /**
   * Retrieves all skill gap assessments linked to a specific course.
   *
   * @param courseId course identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SkillGapAssessmentResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "courseId", "!assessmentNumber", "!traineeId", "!assessedOn", "!enrollmentId",
      "!batchId", "!jobRoleId", "!jobPostingId", "!employmentId", "!placementId",
      "!traineeAssessmentId", "!assessmentResultId", "!certificationId", "!surveyResponseId",
      "!followupTaskId", "!employmentVerificationId", "!sourceId", "!statusId"
  })
  public ResponseEntity<ApiResponse<List<SkillGapAssessmentResponse>>> getByCourse(
      @RequestParam("courseId") Long courseId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "courseId");
    List<SkillGapAssessmentResponse> assessments = skillGapAssessmentService.getAssessmentsByCourse(courseId);
    return ResponseEntity.ok(ApiResponse.ok(assessments));
  }

  /**
   * Retrieves all skill gap assessments linked to a specific training batch.
   *
   * @param batchId training batch identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SkillGapAssessmentResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "batchId", "!assessmentNumber", "!traineeId", "!assessedOn", "!enrollmentId",
      "!courseId", "!jobRoleId", "!jobPostingId", "!employmentId", "!placementId",
      "!traineeAssessmentId", "!assessmentResultId", "!certificationId", "!surveyResponseId",
      "!followupTaskId", "!employmentVerificationId", "!sourceId", "!statusId"
  })
  public ResponseEntity<ApiResponse<List<SkillGapAssessmentResponse>>> getByBatch(
      @RequestParam("batchId") Long batchId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "batchId");
    List<SkillGapAssessmentResponse> assessments = skillGapAssessmentService.getAssessmentsByBatch(batchId);
    return ResponseEntity.ok(ApiResponse.ok(assessments));
  }

  /**
   * Retrieves all skill gap assessments linked to a specific job role.
   *
   * @param jobRoleId job role identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SkillGapAssessmentResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "jobRoleId", "!assessmentNumber", "!traineeId", "!assessedOn", "!enrollmentId",
      "!courseId", "!batchId", "!jobPostingId", "!employmentId", "!placementId",
      "!traineeAssessmentId", "!assessmentResultId", "!certificationId", "!surveyResponseId",
      "!followupTaskId", "!employmentVerificationId", "!sourceId", "!statusId"
  })
  public ResponseEntity<ApiResponse<List<SkillGapAssessmentResponse>>> getByJobRole(
      @RequestParam("jobRoleId") Long jobRoleId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "jobRoleId");
    List<SkillGapAssessmentResponse> assessments = skillGapAssessmentService.getAssessmentsByJobRole(jobRoleId);
    return ResponseEntity.ok(ApiResponse.ok(assessments));
  }

  /**
   * Retrieves all skill gap assessments linked to a specific job posting.
   *
   * @param jobPostingId job posting identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SkillGapAssessmentResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "jobPostingId", "!assessmentNumber", "!traineeId", "!assessedOn", "!enrollmentId",
      "!courseId", "!batchId", "!jobRoleId", "!employmentId", "!placementId",
      "!traineeAssessmentId", "!assessmentResultId", "!certificationId", "!surveyResponseId",
      "!followupTaskId", "!employmentVerificationId", "!sourceId", "!statusId"
  })
  public ResponseEntity<ApiResponse<List<SkillGapAssessmentResponse>>> getByJobPosting(
      @RequestParam("jobPostingId") Long jobPostingId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "jobPostingId");
    List<SkillGapAssessmentResponse> assessments = skillGapAssessmentService.getAssessmentsByJobPosting(jobPostingId);
    return ResponseEntity.ok(ApiResponse.ok(assessments));
  }

  /**
   * Retrieves all skill gap assessments linked to a specific employment record.
   *
   * @param employmentId employment record identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SkillGapAssessmentResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "employmentId", "!assessmentNumber", "!traineeId", "!assessedOn", "!enrollmentId",
      "!courseId", "!batchId", "!jobRoleId", "!jobPostingId", "!placementId",
      "!traineeAssessmentId", "!assessmentResultId", "!certificationId", "!surveyResponseId",
      "!followupTaskId", "!employmentVerificationId", "!sourceId", "!statusId"
  })
  public ResponseEntity<ApiResponse<List<SkillGapAssessmentResponse>>> getByEmploymentRecord(
      @RequestParam("employmentId") Long employmentId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "employmentId");
    List<SkillGapAssessmentResponse> assessments =
        skillGapAssessmentService.getAssessmentsByEmploymentRecord(employmentId);
    return ResponseEntity.ok(ApiResponse.ok(assessments));
  }

  /**
   * Retrieves all skill gap assessments linked to a specific placement record.
   *
   * @param placementId placement record identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SkillGapAssessmentResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "placementId", "!assessmentNumber", "!traineeId", "!assessedOn", "!enrollmentId",
      "!courseId", "!batchId", "!jobRoleId", "!jobPostingId", "!employmentId",
      "!traineeAssessmentId", "!assessmentResultId", "!certificationId", "!surveyResponseId",
      "!followupTaskId", "!employmentVerificationId", "!sourceId", "!statusId"
  })
  public ResponseEntity<ApiResponse<List<SkillGapAssessmentResponse>>> getByPlacementRecord(
      @RequestParam("placementId") Long placementId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "placementId");
    List<SkillGapAssessmentResponse> assessments =
        skillGapAssessmentService.getAssessmentsByPlacementRecord(placementId);
    return ResponseEntity.ok(ApiResponse.ok(assessments));
  }

  /**
   * Retrieves all skill gap assessments linked to a specific trainee assessment.
   *
   * @param traineeAssessmentId trainee assessment identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SkillGapAssessmentResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "traineeAssessmentId", "!assessmentNumber", "!traineeId", "!assessedOn", "!enrollmentId",
      "!courseId", "!batchId", "!jobRoleId", "!jobPostingId", "!employmentId",
      "!placementId", "!assessmentResultId", "!certificationId", "!surveyResponseId",
      "!followupTaskId", "!employmentVerificationId", "!sourceId", "!statusId"
  })
  public ResponseEntity<ApiResponse<List<SkillGapAssessmentResponse>>> getByTraineeAssessment(
      @RequestParam("traineeAssessmentId") Long traineeAssessmentId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "traineeAssessmentId");
    List<SkillGapAssessmentResponse> assessments =
        skillGapAssessmentService.getAssessmentsByTraineeAssessment(traineeAssessmentId);
    return ResponseEntity.ok(ApiResponse.ok(assessments));
  }

  /**
   * Retrieves all skill gap assessments linked to a specific assessment result.
   *
   * @param assessmentResultId assessment result identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SkillGapAssessmentResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "assessmentResultId", "!assessmentNumber", "!traineeId", "!assessedOn", "!enrollmentId",
      "!courseId", "!batchId", "!jobRoleId", "!jobPostingId", "!employmentId",
      "!placementId", "!traineeAssessmentId", "!certificationId", "!surveyResponseId",
      "!followupTaskId", "!employmentVerificationId", "!sourceId", "!statusId"
  })
  public ResponseEntity<ApiResponse<List<SkillGapAssessmentResponse>>> getByAssessmentResult(
      @RequestParam("assessmentResultId") Long assessmentResultId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "assessmentResultId");
    List<SkillGapAssessmentResponse> assessments =
        skillGapAssessmentService.getAssessmentsByAssessmentResult(assessmentResultId);
    return ResponseEntity.ok(ApiResponse.ok(assessments));
  }

  /**
   * Retrieves all skill gap assessments linked to a specific certification.
   *
   * @param certificationId certification identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SkillGapAssessmentResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "certificationId", "!assessmentNumber", "!traineeId", "!assessedOn", "!enrollmentId",
      "!courseId", "!batchId", "!jobRoleId", "!jobPostingId", "!employmentId",
      "!placementId", "!traineeAssessmentId", "!assessmentResultId", "!surveyResponseId",
      "!followupTaskId", "!employmentVerificationId", "!sourceId", "!statusId"
  })
  public ResponseEntity<ApiResponse<List<SkillGapAssessmentResponse>>> getByCertification(
      @RequestParam("certificationId") Long certificationId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "certificationId");
    List<SkillGapAssessmentResponse> assessments =
        skillGapAssessmentService.getAssessmentsByCertification(certificationId);
    return ResponseEntity.ok(ApiResponse.ok(assessments));
  }

  /**
   * Retrieves all skill gap assessments linked to a specific survey response.
   *
   * @param surveyResponseId survey response identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SkillGapAssessmentResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "surveyResponseId", "!assessmentNumber", "!traineeId", "!assessedOn", "!enrollmentId",
      "!courseId", "!batchId", "!jobRoleId", "!jobPostingId", "!employmentId",
      "!placementId", "!traineeAssessmentId", "!assessmentResultId", "!certificationId",
      "!followupTaskId", "!employmentVerificationId", "!sourceId", "!statusId"
  })
  public ResponseEntity<ApiResponse<List<SkillGapAssessmentResponse>>> getBySurveyResponse(
      @RequestParam("surveyResponseId") Long surveyResponseId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "surveyResponseId");
    List<SkillGapAssessmentResponse> assessments =
        skillGapAssessmentService.getAssessmentsBySurveyResponse(surveyResponseId);
    return ResponseEntity.ok(ApiResponse.ok(assessments));
  }

  /**
   * Retrieves all skill gap assessments linked to a specific follow-up task.
   *
   * @param followupTaskId follow-up task identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SkillGapAssessmentResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "followupTaskId", "!assessmentNumber", "!traineeId", "!assessedOn", "!enrollmentId",
      "!courseId", "!batchId", "!jobRoleId", "!jobPostingId", "!employmentId",
      "!placementId", "!traineeAssessmentId", "!assessmentResultId", "!certificationId",
      "!surveyResponseId", "!employmentVerificationId", "!sourceId", "!statusId"
  })
  public ResponseEntity<ApiResponse<List<SkillGapAssessmentResponse>>> getByFollowupTask(
      @RequestParam("followupTaskId") Long followupTaskId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "followupTaskId");
    List<SkillGapAssessmentResponse> assessments =
        skillGapAssessmentService.getAssessmentsByFollowupTask(followupTaskId);
    return ResponseEntity.ok(ApiResponse.ok(assessments));
  }

  /**
   * Retrieves all skill gap assessments linked to a specific employment verification.
   *
   * @param employmentVerificationId employment verification identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SkillGapAssessmentResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "employmentVerificationId", "!assessmentNumber", "!traineeId", "!assessedOn", "!enrollmentId",
      "!courseId", "!batchId", "!jobRoleId", "!jobPostingId", "!employmentId",
      "!placementId", "!traineeAssessmentId", "!assessmentResultId", "!certificationId",
      "!surveyResponseId", "!followupTaskId", "!sourceId", "!statusId"
  })
  public ResponseEntity<ApiResponse<List<SkillGapAssessmentResponse>>> getByEmploymentVerification(
      @RequestParam("employmentVerificationId") Long employmentVerificationId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "employmentVerificationId");
    List<SkillGapAssessmentResponse> assessments =
        skillGapAssessmentService.getAssessmentsByEmploymentVerification(employmentVerificationId);
    return ResponseEntity.ok(ApiResponse.ok(assessments));
  }

  /**
   * Retrieves all skill gap assessments linked to a specific assessment source.
   *
   * @param sourceId skill gap source reference identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SkillGapAssessmentResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "sourceId", "!assessmentNumber", "!traineeId", "!assessedOn", "!enrollmentId",
      "!courseId", "!batchId", "!jobRoleId", "!jobPostingId", "!employmentId",
      "!placementId", "!traineeAssessmentId", "!assessmentResultId", "!certificationId",
      "!surveyResponseId", "!followupTaskId", "!employmentVerificationId", "!statusId"
  })
  public ResponseEntity<ApiResponse<List<SkillGapAssessmentResponse>>> getBySource(
      @RequestParam("sourceId") Long sourceId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "sourceId");
    List<SkillGapAssessmentResponse> assessments = skillGapAssessmentService.getAssessmentsBySource(sourceId);
    return ResponseEntity.ok(ApiResponse.ok(assessments));
  }

  /**
   * Retrieves all skill gap assessments with a specific assessment status.
   *
   * @param statusId skill gap assessment status reference identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SkillGapAssessmentResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "statusId", "!assessmentNumber", "!traineeId", "!assessedOn", "!enrollmentId",
      "!courseId", "!batchId", "!jobRoleId", "!jobPostingId", "!employmentId",
      "!placementId", "!traineeAssessmentId", "!assessmentResultId", "!certificationId",
      "!surveyResponseId", "!followupTaskId", "!employmentVerificationId", "!sourceId"
  })
  public ResponseEntity<ApiResponse<List<SkillGapAssessmentResponse>>> getByStatus(
      @RequestParam("statusId") Long statusId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "statusId");
    List<SkillGapAssessmentResponse> assessments = skillGapAssessmentService.getAssessmentsByStatus(statusId);
    return ResponseEntity.ok(ApiResponse.ok(assessments));
  }

  /**
   * Creates a new skill gap assessment.
   *
   * @param request assessment creation payload
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 201 Created with created SkillGapAssessmentResponse enveloped in ApiResponse
   */
  @PostMapping
  public ResponseEntity<ApiResponse<SkillGapAssessmentResponse>> createAssessment(
      @RequestBody CreateSkillGapAssessmentRequest request,
      HttpServletRequest httpRequest) {
    SkillGapAssessmentResponse response = skillGapAssessmentService.createAssessment(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(
            "Skill gap assessment created successfully",
            response,
            httpRequest.getRequestURI()));
  }

  /**
   * Updates an existing skill gap assessment.
   *
   * @param id primary key identifier of the assessment to update
   * @param request assessment update payload
   * @return 200 OK with updated SkillGapAssessmentResponse enveloped in ApiResponse
   */
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<SkillGapAssessmentResponse>> updateAssessment(
      @PathVariable Long id,
      @RequestBody UpdateSkillGapAssessmentRequest request) {
    SkillGapAssessmentResponse response = skillGapAssessmentService.updateAssessment(id, request);
    return ResponseEntity.ok(ApiResponse.success(
        "Skill gap assessment updated successfully",
        response));
  }

  /**
   * Deletes a skill gap assessment.
   *
   * @param id primary key identifier of the assessment to delete
   * @return 200 OK with success confirmation message
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteAssessment(@PathVariable Long id) {
    skillGapAssessmentService.deleteAssessment(id);
    return ResponseEntity.ok(ApiResponse.success("Skill gap assessment deleted successfully"));
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
