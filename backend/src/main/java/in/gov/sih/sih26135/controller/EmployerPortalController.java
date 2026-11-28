package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.employer.EmployerPortal;
import in.gov.sih.sih26135.dto.response.PlacementToEmploymentTransitionResponse;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.response.PagedResponse;
import in.gov.sih.sih26135.service.employer.EmployerPortalService;
import in.gov.sih.sih26135.service.employer.VerificationEvidenceFiles;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Validated
@RequestMapping("/api/v1/employer")
@PreAuthorize("hasAuthority('ROLE_employer')")
@SecurityRequirement(name = "bearerAuth")
public class EmployerPortalController {
  private final EmployerPortalService employer;
  private final VerificationEvidenceFiles files;

  public EmployerPortalController(EmployerPortalService employer, VerificationEvidenceFiles files) {
    this.employer = employer;
    this.files = files;
  }

  @GetMapping("/reference-data")
  public ApiResponse<EmployerPortal.References> employerReferences() {
    return ApiResponse.ok(employer.references());
  }

  @GetMapping("/skills")
  @PreAuthorize("hasAuthority('ROLE_employer') and hasAuthority('placement.manage')")
  public ApiResponse<List<EmployerPortal.Reference>> employerSkills(@RequestParam(defaultValue = "") @Size(max = 100) String search) {
    return ApiResponse.ok(employer.searchSkills(search));
  }

  @GetMapping("/job-postings")
  @PreAuthorize("hasAuthority('ROLE_employer') and hasAuthority('placement.manage')")
  public ApiResponse<PagedResponse<EmployerPortal.Vacancy>> employerVacancies(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size,
      @RequestParam(required = false) @Size(max = 120) String search) {
    return ApiResponse.ok(employer.vacancies(page, size, search));
  }

  @PostMapping("/job-postings")
  @PreAuthorize("hasAuthority('ROLE_employer') and hasAuthority('placement.manage')")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<EmployerPortal.Vacancy> publishEmployerVacancy(@Valid @RequestBody EmployerPortal.VacancyRequest request) {
    return ApiResponse.ok(employer.publish(request));
  }

  @PostMapping("/job-postings/{postingId}/hires")
  @PreAuthorize("hasAuthority('ROLE_employer') and hasAuthority('placement.manage') and hasAuthority('employment.manage')")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<PlacementToEmploymentTransitionResponse> recordEmployerPostedJobHire(
      @PathVariable @Positive long postingId, @Valid @RequestBody EmployerPortal.PostedJobHire request) {
    return ApiResponse.ok(employer.recordPostedJobHire(postingId, request));
  }

  @GetMapping("/placements")
  @PreAuthorize("hasAuthority('ROLE_employer') and hasAuthority('placement.manage')")
  public ApiResponse<PagedResponse<EmployerPortal.Placement>> employerPlacements(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
    return ApiResponse.ok(employer.placements(page, size));
  }

  @PostMapping("/placements/{placementId}/confirm")
  @PreAuthorize("hasAuthority('ROLE_employer') and hasAuthority('placement.manage') and hasAuthority('employment.manage')")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<PlacementToEmploymentTransitionResponse> confirmEmployerPlacement(
      @PathVariable @Positive long placementId, @Valid @RequestBody EmployerPortal.ConfirmPlacementRequest request) {
    return ApiResponse.ok(employer.confirm(placementId, request));
  }

  @GetMapping("/verifications")
  @PreAuthorize("hasAuthority('ROLE_employer') and hasAuthority('verification.manage')")
  public ApiResponse<PagedResponse<EmployerPortal.Verification>> employerVerificationHistory(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
    return ApiResponse.ok(employer.verifications(false, page, size));
  }

  @GetMapping("/verifications/pending")
  @PreAuthorize("hasAuthority('ROLE_employer') and hasAuthority('verification.manage')")
  public ApiResponse<PagedResponse<EmployerPortal.Verification>> pendingEmployerVerifications(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
    return ApiResponse.ok(employer.verifications(true, page, size));
  }

  @PostMapping(value = "/verifications/{requestId}/verdict", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAuthority('ROLE_employer') and hasAuthority('verification.manage')")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<EmployerPortal.Verification> submitEmployerVerdict(
      @PathVariable @Positive long requestId, @Valid @RequestPart("verdict") EmployerPortal.VerdictRequest verdict,
      @RequestPart(value = "evidence", required = false) MultipartFile evidence) {
    return ApiResponse.ok(employer.submitVerdict(requestId, verdict, evidence));
  }

  @GetMapping("/verifications/{requestId}/evidence/{evidenceId}")
  @PreAuthorize("hasAuthority('ROLE_employer') and hasAuthority('verification.manage')")
  public ResponseEntity<Resource> downloadEmployerEvidence(
      @PathVariable @Positive long requestId, @PathVariable @Positive long evidenceId) {
    String reference = employer.evidenceReference(requestId, evidenceId);
    Resource content = files.read(reference);
    return ResponseEntity.ok().contentType(files.mediaType(reference))
        .cacheControl(CacheControl.noStore())
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(reference).build().toString())
        .body(content);
  }
}
