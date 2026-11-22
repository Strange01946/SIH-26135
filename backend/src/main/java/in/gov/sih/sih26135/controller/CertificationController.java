package in.gov.sih.sih26135.controller;

import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import in.gov.sih.sih26135.dto.request.CreateCertificationRequest;
import in.gov.sih.sih26135.dto.request.UpdateCertificationRequest;
import in.gov.sih.sih26135.dto.response.CertificationResponse;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.AssessorWorkbenchService;
import in.gov.sih.sih26135.service.CertificationService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

import in.gov.sih.sih26135.entity.Certification;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.entity.User;
import in.gov.sih.sih26135.repository.CertificationRepository;
import in.gov.sih.sih26135.repository.TraineeRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Optional;

/**
 * REST controller for managing Certification credential domain resources.
 *
 * <p>Base Route: /api/v1/certifications
 * Consumes: CreateCertificationRequest, UpdateCertificationRequest
 * Produces: CertificationResponse enveloped in ApiResponse
 */
@Validated
@RestController
@RequestMapping({"/api/v1/certifications", "/api/v1/certificates"})
@PreAuthorize("hasAnyAuthority('assessment.manage', 'trainee.read', 'ROLE_trainee', 'ROLE_assessor')")
public class CertificationController {

  private final CertificationService certificationService;
  private final AssessorWorkbenchService assessorWorkbenchService;

  @org.springframework.beans.factory.annotation.Autowired(required = false)
  private in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator securityScopeValidator;

  @org.springframework.beans.factory.annotation.Autowired(required = false)
  private CertificationRepository certificationRepository;

  @org.springframework.beans.factory.annotation.Autowired(required = false)
  private UserRepository userRepository;

  @org.springframework.beans.factory.annotation.Autowired(required = false)
  private TraineeRepository traineeRepository;

  public CertificationController(
      CertificationService certificationService,
      @org.springframework.beans.factory.annotation.Autowired(required = false) AssessorWorkbenchService assessorWorkbenchService) {
    this.certificationService = certificationService;
    this.assessorWorkbenchService = assessorWorkbenchService;
  }

  private void validateCertificateAccess(Long certificationId) {
    if (certificationId == null || certificationRepository == null) return;
    Certification cert = certificationRepository.findById(certificationId)
        .orElseThrow(() -> new in.gov.sih.sih26135.exception.ResourceNotFoundException("Certification", "id=" + certificationId));
    validateCertificateAccess(cert);
  }

  private void validateCertificateAccess(Certification cert) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
      throw new AccessDeniedException("Authentication required to access certificate");
    }

    boolean isAdmin = auth.getAuthorities().stream().anyMatch(a ->
        "ROLE_super_admin".equalsIgnoreCase(a.getAuthority()) ||
        "ROLE_admin".equalsIgnoreCase(a.getAuthority()) ||
        "ROLE_government_admin".equalsIgnoreCase(a.getAuthority()) ||
        "assessment.manage".equalsIgnoreCase(a.getAuthority())
    );
    if (isAdmin) {
      return;
    }

    String username = auth.getName();
    User caller = userRepository != null ? userRepository.findByUsername(username).orElse(null) : null;
    if (caller == null) {
      throw new AccessDeniedException("Authenticated user not found");
    }

    boolean isAssessor = auth.getAuthorities().stream().anyMatch(a ->
        "ROLE_assessor".equalsIgnoreCase(a.getAuthority())
    );
    if (isAssessor) {
      boolean isAssigned = false;
      if (cert.getAssessmentResult() != null
          && cert.getAssessmentResult().getTraineeAssessment() != null
          && cert.getAssessmentResult().getTraineeAssessment().getAssessment() != null) {
        Long evaluatorId = cert.getAssessmentResult().getTraineeAssessment().getAssessment().getEvaluatorUserId();
        if (evaluatorId != null && evaluatorId.equals(caller.getId())) {
          isAssigned = true;
        }
      }
      if (!isAssigned) {
        throw new AccessDeniedException("Access denied: You are not the assigned assessor for certificate #" + cert.getId());
      }
      return;
    }

    boolean isTrainee = auth.getAuthorities().stream().anyMatch(a ->
        "ROLE_trainee".equalsIgnoreCase(a.getAuthority()) || "trainee.read".equalsIgnoreCase(a.getAuthority())
    );
    if (isTrainee) {
      boolean isOwn = false;
      if (cert.getTrainee() != null && traineeRepository != null) {
        Optional<Trainee> callerTrainee = traineeRepository.findByUserId(caller.getId());
        if (callerTrainee.isPresent() && callerTrainee.get().getId().equals(cert.getTrainee().getId())) {
          isOwn = true;
        }
      }
      if (!isOwn) {
        throw new AccessDeniedException("Access denied: Trainee can only view their own certificate");
      }
      return;
    }

    throw new AccessDeniedException("Access denied: You do not have permission to access certificate #" + cert.getId());
  }

  /**
   * Retrieves a certification by primary key identifier.
   *
   * @param id primary key identifier of the certification
   * @return 200 OK with CertificationResponse enveloped in ApiResponse
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<CertificationResponse>> getById(@PathVariable @Positive Long id) {
    validateCertificateAccess(id);
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
      @RequestParam("certificateNumber") @NotBlank String certificateNumber) {
    if (certificationRepository != null) {
      Certification cert = certificationRepository.findByCertificateNumber(certificateNumber.trim())
          .orElseThrow(() -> new in.gov.sih.sih26135.exception.ResourceNotFoundException("Certification", "certificateNumber=" + certificateNumber));
      validateCertificateAccess(cert);
    }
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
      @RequestParam("traineeId") @Positive Long traineeId) {
    if (securityScopeValidator != null) {
      securityScopeValidator.validateTraineeScope(traineeId);
    }
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
      @RequestParam("enrollmentId") @Positive Long enrollmentId) {
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
      @RequestParam("courseId") @Positive Long courseId) {
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
      @RequestParam("programId") @Positive Long programId) {
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
      @RequestParam("assessmentResultId") @Positive Long assessmentResultId) {
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
      @RequestParam("statusId") @Positive Long statusId) {
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
      @RequestParam("verificationStatusId") @Positive Long verificationStatusId) {
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
  @PreAuthorize("hasAuthority('assessment.manage')")
  public ResponseEntity<ApiResponse<CertificationResponse>> createCertification(
      @Valid @RequestBody CreateCertificationRequest request,
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
  @PreAuthorize("hasAuthority('assessment.manage')")
  public ResponseEntity<ApiResponse<CertificationResponse>> updateCertification(
      @PathVariable @Positive Long id,
      @Valid @RequestBody UpdateCertificationRequest request) {
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
  @PreAuthorize("hasAuthority('assessment.manage')")
  public ResponseEntity<ApiResponse<Void>> deleteCertification(@PathVariable @Positive Long id) {
    certificationService.deleteCertification(id);
    return ResponseEntity.ok(ApiResponse.success("Certification deleted successfully"));
  }

  /**
   * Generates and streams authentic Certificate PDF.
   */
  @GetMapping(value = "/{id}/download", produces = MediaType.APPLICATION_PDF_VALUE)
  @PreAuthorize("hasAnyAuthority('assessment.manage', 'trainee.read', 'ROLE_trainee', 'ROLE_assessor', 'ROLE_super_admin', 'ROLE_government_admin')")
  public ResponseEntity<byte[]> downloadCertificatePdf(@PathVariable @Positive Long id) {
    validateCertificateAccess(id);
    if (assessorWorkbenchService == null) {
      throw new IllegalStateException("AssessorWorkbenchService is required for PDF certificate generation");
    }
    byte[] pdfBytes = assessorWorkbenchService.generateCertificatePdf(id);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_PDF);
    headers.setContentDisposition(ContentDisposition.attachment().filename("Certificate_" + id + ".pdf").build());
    headers.setContentLength(pdfBytes.length);
    return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
  }

  /**
   * Alias endpoint for issuing certificates at /api/v1/certificates/issue.
   */
  @PostMapping("/issue")
  @PreAuthorize("hasAuthority('assessment.manage')")
  public ResponseEntity<ApiResponse<CertificationResponse>> issueCertificate(
      @Valid @RequestBody CreateCertificationRequest request,
      HttpServletRequest httpRequest) {
    return createCertification(request, httpRequest);
  }
}
