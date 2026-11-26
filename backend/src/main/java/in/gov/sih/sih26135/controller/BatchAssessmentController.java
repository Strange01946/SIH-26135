package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.assessor.BatchAssessment.*;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.AssessmentDocuments;
import in.gov.sih.sih26135.service.BatchAssessmentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api/v1")
public class BatchAssessmentController {
  private static final String PROVIDER = "hasRole('training_provider') and hasAuthority('enrollment.manage')";
  private static final String ASSESSOR = "hasRole('assessor') and hasAuthority('assessment.manage')";
  private final BatchAssessmentService assessments;
  private final AssessmentDocuments documents;

  public BatchAssessmentController(BatchAssessmentService assessments, AssessmentDocuments documents) {
    this.assessments = assessments;
    this.documents = documents;
  }

  @GetMapping("/training-batches/{id}/assessment")
  @PreAuthorize(PROVIDER)
  public ApiResponse<Readiness> batchAssessmentReadiness(@PathVariable @Positive long id) {
    return ApiResponse.ok(assessments.readiness(id));
  }

  @PostMapping("/training-batches/{id}/assessment")
  @PreAuthorize(PROVIDER)
  public ApiResponse<Publication> publishBatchAssessment(@PathVariable @Positive long id,
      @Valid @RequestBody PublishBatchAssessmentRequest input) {
    return ApiResponse.ok(assessments.publish(id, input));
  }

  @GetMapping("/training-batches/{id}/assessment/roster")
  @PreAuthorize(PROVIDER)
  public ApiResponse<Review> providerAssessmentReview(@PathVariable @Positive long id) {
    return ApiResponse.ok(assessments.providerReview(id));
  }

  @GetMapping("/assessor/open-batch-assessments")
  @PreAuthorize(ASSESSOR)
  public ApiResponse<List<Publication>> listOpenBatchAssessments() {
    return ApiResponse.ok(assessments.openAssessments());
  }

  @PostMapping("/assessor/open-batch-assessments/{id}/claim")
  @PreAuthorize(ASSESSOR)
  public ApiResponse<Publication> claimBatchAssessment(@PathVariable @Positive long id,
      @Valid @RequestBody ClaimBatchAssessmentRequest input) {
    return ApiResponse.ok(assessments.claim(id, input));
  }

  @GetMapping("/assessor/assessments/{id}/published-batch")
  @PreAuthorize(ASSESSOR)
  public ApiResponse<Review> getPublishedBatch(@PathVariable @Positive long id) {
    return ApiResponse.ok(assessments.assessorReview(id));
  }

  @PostMapping("/assessor/assessments/{id}/complete")
  @PreAuthorize(ASSESSOR)
  public ApiResponse<Publication> completeBatchAssessment(@PathVariable @Positive long id) {
    return ApiResponse.ok(assessments.complete(id));
  }

  @GetMapping("/trainees/me/batch-assessments")
  @PreAuthorize("hasRole('trainee')")
  public ApiResponse<List<Appointment>> myBatchAssessmentAppointments() {
    return ApiResponse.ok(assessments.ownAppointments());
  }

  @GetMapping(value = "/assessment-results/{id}/document", produces = MediaType.APPLICATION_PDF_VALUE)
  @PreAuthorize("hasAnyRole('trainee','training_provider','assessor','super_admin','government_admin')")
  public ResponseEntity<byte[]> downloadAssessmentDocument(@PathVariable @Positive long id) {
    var document = documents.result(id);
    return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).header(HttpHeaders.CACHE_CONTROL, "private, no-store")
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(document.filename()).build().toString())
        .body(document.bytes());
  }
}
