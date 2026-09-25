package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.request.CreateCertificationRequest;
import in.gov.sih.sih26135.dto.request.UpdateCertificationRequest;
import in.gov.sih.sih26135.dto.response.CertificationResponse;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.CertificationService;
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
 * REST controller for managing Certification credential domain resources.
 *
 * <p>Base Route: /api/v1/certifications
 * Consumes: CreateCertificationRequest, UpdateCertificationRequest
 * Produces: CertificationResponse enveloped in ApiResponse
 */
@RestController
@RequestMapping("/api/v1/certifications")
@PreAuthorize("hasAuthority('assessment.manage')")
public class CertificationController {

  private final CertificationService certificationService;

  public CertificationController(CertificationService certificationService) {
    this.certificationService = certificationService;
  }

  /**
   * Retrieves a certification by primary key identifier.
   *
   * @param id primary key identifier of the certification
   * @return 200 OK with CertificationResponse enveloped in ApiResponse
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<CertificationResponse>> getById(@PathVariable Long id) {
    CertificationResponse response = certificationService.getById(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves a single certification by unique certificate number.
   *
   * @param certificateNumber unique certificate number
   * @return 200 OK with CertificationResponse enveloped in ApiResponse
   */
  @GetMapping(params = "certificateNumber")
  public ResponseEntity<ApiResponse<CertificationResponse>> getByCertificateNumber(
      @RequestParam("certificateNumber") String certificateNumber) {
    CertificationResponse response = certificationService.getByCertificateNumber(certificateNumber);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves all certifications issued to a specific trainee.
   *
   * @param traineeId trainee identifier
   * @return 200 OK with list of CertificationResponse enveloped in ApiResponse
   */
  @GetMapping(params = "traineeId")
  public ResponseEntity<ApiResponse<List<CertificationResponse>>> getByTraineeId(
      @RequestParam("traineeId") Long traineeId) {
    List<CertificationResponse> certifications = certificationService.getByTraineeId(traineeId);
    return ResponseEntity.ok(ApiResponse.ok(certifications));
  }

  /**
   * Retrieves all certifications issued for a specific training enrollment.
   *
   * @param enrollmentId training enrollment identifier
   * @return 200 OK with list of CertificationResponse enveloped in ApiResponse
   */
  @GetMapping(params = "enrollmentId")
  public ResponseEntity<ApiResponse<List<CertificationResponse>>> getByEnrollmentId(
      @RequestParam("enrollmentId") Long enrollmentId) {
    List<CertificationResponse> certifications = certificationService.getByEnrollmentId(enrollmentId);
    return ResponseEntity.ok(ApiResponse.ok(certifications));
  }

  /**
   * Retrieves all certifications issued for a specific course.
   *
   * @param courseId course identifier
   * @return 200 OK with list of CertificationResponse enveloped in ApiResponse
   */
  @GetMapping(params = "courseId")
  public ResponseEntity<ApiResponse<List<CertificationResponse>>> getByCourseId(
      @RequestParam("courseId") Long courseId) {
    List<CertificationResponse> certifications = certificationService.getByCourseId(courseId);
    return ResponseEntity.ok(ApiResponse.ok(certifications));
  }

  /**
   * Retrieves all certifications issued under a specific program.
   *
   * @param programId program identifier
   * @return 200 OK with list of CertificationResponse enveloped in ApiResponse
   */
  @GetMapping(params = "programId")
  public ResponseEntity<ApiResponse<List<CertificationResponse>>> getByProgramId(
      @RequestParam("programId") Long programId) {
    List<CertificationResponse> certifications = certificationService.getByProgramId(programId);
    return ResponseEntity.ok(ApiResponse.ok(certifications));
  }

  /**
   * Retrieves all certifications linked to a specific assessment result.
   *
   * @param assessmentResultId assessment result identifier
   * @return 200 OK with list of CertificationResponse enveloped in ApiResponse
   */
  @GetMapping(params = "assessmentResultId")
  public ResponseEntity<ApiResponse<List<CertificationResponse>>> getByAssessmentResultId(
      @RequestParam("assessmentResultId") Long assessmentResultId) {
    List<CertificationResponse> certifications = certificationService.getByAssessmentResultId(assessmentResultId);
    return ResponseEntity.ok(ApiResponse.ok(certifications));
  }

  /**
   * Retrieves all certifications with a specific certificate lifecycle status.
   *
   * @param statusId certificate status identifier
   * @return 200 OK with list of CertificationResponse enveloped in ApiResponse
   */
  @GetMapping(params = "statusId")
  public ResponseEntity<ApiResponse<List<CertificationResponse>>> getByCertificateStatusId(
      @RequestParam("statusId") Long statusId) {
    List<CertificationResponse> certifications = certificationService.getByCertificateStatusId(statusId);
    return ResponseEntity.ok(ApiResponse.ok(certifications));
  }

  /**
   * Retrieves all certifications with a specific verification status.
   *
   * @param verificationStatusId certificate verification status identifier
   * @return 200 OK with list of CertificationResponse enveloped in ApiResponse
   */
  @GetMapping(params = "verificationStatusId")
  public ResponseEntity<ApiResponse<List<CertificationResponse>>> getByCertificateVerificationStatusId(
      @RequestParam("verificationStatusId") Long verificationStatusId) {
    List<CertificationResponse> certifications = certificationService.getByCertificateVerificationStatusId(verificationStatusId);
    return ResponseEntity.ok(ApiResponse.ok(certifications));
  }

  /**
   * Retrieves all certifications issued on a specific date.
   *
   * @param issueDate issue date (ISO-8601 YYYY-MM-DD)
   * @return 200 OK with list of CertificationResponse enveloped in ApiResponse
   */
  @GetMapping(params = "issueDate")
  public ResponseEntity<ApiResponse<List<CertificationResponse>>> getByIssueDate(
      @RequestParam("issueDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate issueDate) {
    List<CertificationResponse> certifications = certificationService.getByIssueDate(issueDate);
    return ResponseEntity.ok(ApiResponse.ok(certifications));
  }

  /**
   * Retrieves all certifications.
   *
   * @return 200 OK with list of all CertificationResponse enveloped in ApiResponse
   */
  @GetMapping
  public ResponseEntity<ApiResponse<List<CertificationResponse>>> getAllCertifications() {
    List<CertificationResponse> certifications = certificationService.getAllCertifications();
    return ResponseEntity.ok(ApiResponse.ok(certifications));
  }

  /**
   * Creates a new certification credential record.
   *
   * @param request certification creation payload
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 201 Created with created CertificationResponse enveloped in ApiResponse
   */
  @PostMapping
  public ResponseEntity<ApiResponse<CertificationResponse>> createCertification(
      @RequestBody CreateCertificationRequest request,
      HttpServletRequest httpRequest) {
    CertificationResponse response = certificationService.createCertification(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Certification created successfully", response, httpRequest.getRequestURI()));
  }

  /**
   * Updates an existing certification credential record.
   *
   * @param id primary key identifier of the certification to update
   * @param request certification update payload
   * @return 200 OK with updated CertificationResponse enveloped in ApiResponse
   */
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<CertificationResponse>> updateCertification(
      @PathVariable Long id,
      @RequestBody UpdateCertificationRequest request) {
    CertificationResponse response = certificationService.updateCertification(id, request);
    return ResponseEntity.ok(ApiResponse.success("Certification updated successfully", response));
  }

  /**
   * Soft-deletes a certification credential record.
   *
   * @param id primary key identifier of the certification to delete
   * @return 200 OK with success confirmation message
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteCertification(@PathVariable Long id) {
    certificationService.deleteCertification(id);
    return ResponseEntity.ok(ApiResponse.success("Certification deleted successfully"));
  }
}
