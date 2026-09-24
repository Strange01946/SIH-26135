package in.gov.sih.sih26135.controller.analytics;

import in.gov.sih.sih26135.dto.response.EmploymentExitFactResponse;
import in.gov.sih.sih26135.dto.response.EmploymentFactResponse;
import in.gov.sih.sih26135.dto.response.EmploymentRetentionFactResponse;
import in.gov.sih.sih26135.dto.response.EmploymentVerificationFactResponse;
import in.gov.sih.sih26135.dto.response.EnrollmentOutcomeFactResponse;
import in.gov.sih.sih26135.dto.response.FollowupFactResponse;
import in.gov.sih.sih26135.dto.response.PlacementFactResponse;
import in.gov.sih.sih26135.dto.response.SalaryProgressionFactResponse;
import in.gov.sih.sih26135.dto.response.SkillGapFactResponse;
import in.gov.sih.sih26135.dto.response.SurveyResponseFactResponse;
import in.gov.sih.sih26135.dto.response.UnemploymentFactResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.EmploymentExitFactService;
import in.gov.sih.sih26135.service.EmploymentFactService;
import in.gov.sih.sih26135.service.EmploymentRetentionFactService;
import in.gov.sih.sih26135.service.EmploymentVerificationFactService;
import in.gov.sih.sih26135.service.EnrollmentOutcomeFactService;
import in.gov.sih.sih26135.service.FollowupFactService;
import in.gov.sih.sih26135.service.PlacementFactService;
import in.gov.sih.sih26135.service.SalaryProgressionFactService;
import in.gov.sih.sih26135.service.SkillGapFactService;
import in.gov.sih.sih26135.service.SurveyResponseFactService;
import in.gov.sih.sih26135.service.UnemploymentFactService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for exposing analytical fact projections mapped to database analytical views.
 *
 * <p>Base Route: /api/v1/analytics/facts
 * Produces: Analytical fact projection responses enveloped in ApiResponse
 *
 * <p>Strict Read-Only Semantics:
 * All endpoints are strictly GET. Analytics views are immutable SQL projections.
 * No modifications, recalculations, or mutations are permitted.
 */
@RestController
@RequestMapping("/api/v1/analytics/facts")
public class AnalyticsFactController {

  private final EmploymentExitFactService employmentExitFactService;
  private final EmploymentFactService employmentFactService;
  private final EmploymentRetentionFactService employmentRetentionFactService;
  private final EmploymentVerificationFactService employmentVerificationFactService;
  private final EnrollmentOutcomeFactService enrollmentOutcomeFactService;
  private final FollowupFactService followupFactService;
  private final PlacementFactService placementFactService;
  private final SalaryProgressionFactService salaryProgressionFactService;
  private final SkillGapFactService skillGapFactService;
  private final SurveyResponseFactService surveyResponseFactService;
  private final UnemploymentFactService unemploymentFactService;

  public AnalyticsFactController(
      EmploymentExitFactService employmentExitFactService,
      EmploymentFactService employmentFactService,
      EmploymentRetentionFactService employmentRetentionFactService,
      EmploymentVerificationFactService employmentVerificationFactService,
      EnrollmentOutcomeFactService enrollmentOutcomeFactService,
      FollowupFactService followupFactService,
      PlacementFactService placementFactService,
      SalaryProgressionFactService salaryProgressionFactService,
      SkillGapFactService skillGapFactService,
      SurveyResponseFactService surveyResponseFactService,
      UnemploymentFactService unemploymentFactService) {
    this.employmentExitFactService = employmentExitFactService;
    this.employmentFactService = employmentFactService;
    this.employmentRetentionFactService = employmentRetentionFactService;
    this.employmentVerificationFactService = employmentVerificationFactService;
    this.enrollmentOutcomeFactService = enrollmentOutcomeFactService;
    this.followupFactService = followupFactService;
    this.placementFactService = placementFactService;
    this.salaryProgressionFactService = salaryProgressionFactService;
    this.skillGapFactService = skillGapFactService;
    this.surveyResponseFactService = surveyResponseFactService;
    this.unemploymentFactService = unemploymentFactService;
  }

  // =========================================================================
  // 1. Employment Exit Facts (/employment-exits)
  // =========================================================================

  @GetMapping(value = "/employment-exits", params = {
      "!traineeId", "!employmentId", "!employmentExitReasonId", "!separationNatureId",
      "!isVoluntaryFlag", "!isInvoluntaryFlag"
  })
  public ResponseEntity<ApiResponse<List<EmploymentExitFactResponse>>> getAllEmploymentExits(
      HttpServletRequest httpRequest) {
    validateNoQueryParameters(httpRequest);
    return ResponseEntity.ok(ApiResponse.ok(employmentExitFactService.getAllEmploymentExits()));
  }

  @GetMapping("/employment-exits/{id}")
  public ResponseEntity<ApiResponse<EmploymentExitFactResponse>> getEmploymentExitById(
      @PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.ok(employmentExitFactService.getEmploymentExitById(id)));
  }

  @GetMapping(value = "/employment-exits", params = {
      "traineeId", "!employmentId", "!employmentExitReasonId", "!separationNatureId",
      "!isVoluntaryFlag", "!isInvoluntaryFlag"
  })
  public ResponseEntity<ApiResponse<List<EmploymentExitFactResponse>>> getEmploymentExitsByTraineeId(
      @RequestParam("traineeId") Long traineeId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "traineeId");
    return ResponseEntity.ok(ApiResponse.ok(employmentExitFactService.getEmploymentExitsByTraineeId(traineeId)));
  }

  @GetMapping(value = "/employment-exits", params = {
      "employmentId", "!traineeId", "!employmentExitReasonId", "!separationNatureId",
      "!isVoluntaryFlag", "!isInvoluntaryFlag"
  })
  public ResponseEntity<ApiResponse<List<EmploymentExitFactResponse>>> getEmploymentExitsByEmploymentId(
      @RequestParam("employmentId") Long employmentId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "employmentId");
    return ResponseEntity.ok(ApiResponse.ok(employmentExitFactService.getEmploymentExitsByEmploymentId(employmentId)));
  }

  @GetMapping(value = "/employment-exits", params = {
      "employmentExitReasonId", "!traineeId", "!employmentId", "!separationNatureId",
      "!isVoluntaryFlag", "!isInvoluntaryFlag"
  })
  public ResponseEntity<ApiResponse<List<EmploymentExitFactResponse>>> getEmploymentExitsByExitReasonId(
      @RequestParam("employmentExitReasonId") Long employmentExitReasonId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "employmentExitReasonId");
    return ResponseEntity.ok(ApiResponse.ok(
        employmentExitFactService.getEmploymentExitsByEmploymentExitReasonId(employmentExitReasonId)));
  }

  @GetMapping(value = "/employment-exits", params = {
      "separationNatureId", "!traineeId", "!employmentId", "!employmentExitReasonId",
      "!isVoluntaryFlag", "!isInvoluntaryFlag"
  })
  public ResponseEntity<ApiResponse<List<EmploymentExitFactResponse>>> getEmploymentExitsBySeparationNatureId(
      @RequestParam("separationNatureId") Long separationNatureId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "separationNatureId");
    return ResponseEntity.ok(ApiResponse.ok(
        employmentExitFactService.getEmploymentExitsBySeparationNatureId(separationNatureId)));
  }

  @GetMapping(value = "/employment-exits", params = {
      "isVoluntaryFlag", "!traineeId", "!employmentId", "!employmentExitReasonId",
      "!separationNatureId", "!isInvoluntaryFlag"
  })
  public ResponseEntity<ApiResponse<List<EmploymentExitFactResponse>>> getEmploymentExitsByIsVoluntaryFlag(
      @RequestParam("isVoluntaryFlag") Boolean isVoluntaryFlag,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "isVoluntaryFlag");
    return ResponseEntity.ok(ApiResponse.ok(
        employmentExitFactService.getEmploymentExitsByIsVoluntaryFlag(isVoluntaryFlag)));
  }

  @GetMapping(value = "/employment-exits", params = {
      "isInvoluntaryFlag", "!traineeId", "!employmentId", "!employmentExitReasonId",
      "!separationNatureId", "!isVoluntaryFlag"
  })
  public ResponseEntity<ApiResponse<List<EmploymentExitFactResponse>>> getEmploymentExitsByIsInvoluntaryFlag(
      @RequestParam("isInvoluntaryFlag") Boolean isInvoluntaryFlag,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "isInvoluntaryFlag");
    return ResponseEntity.ok(ApiResponse.ok(
        employmentExitFactService.getEmploymentExitsByIsInvoluntaryFlag(isInvoluntaryFlag)));
  }

  // =========================================================================
  // 2. Employment Facts (/employments)
  // =========================================================================

  @GetMapping(value = "/employments", params = {
      "!employmentNumber", "!traineeId", "!employerId", "!enrollmentId",
      "!placementId", "!jobRoleId", "!isCurrent", "!engagementTypeId", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<List<EmploymentFactResponse>>> getAllEmployments(
      HttpServletRequest httpRequest) {
    validateNoQueryParameters(httpRequest);
    return ResponseEntity.ok(ApiResponse.ok(employmentFactService.getAllEmployments()));
  }

  @GetMapping("/employments/{id}")
  public ResponseEntity<ApiResponse<EmploymentFactResponse>> getEmploymentById(
      @PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.ok(employmentFactService.getEmploymentById(id)));
  }

  @GetMapping(value = "/employments", params = {
      "employmentNumber", "!traineeId", "!employerId", "!enrollmentId",
      "!placementId", "!jobRoleId", "!isCurrent", "!engagementTypeId", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<EmploymentFactResponse>> getEmploymentByEmploymentNumber(
      @RequestParam("employmentNumber") String employmentNumber,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "employmentNumber");
    return ResponseEntity.ok(ApiResponse.ok(employmentFactService.getEmploymentByEmploymentNumber(employmentNumber)));
  }

  @GetMapping(value = "/employments", params = {
      "traineeId", "!employmentNumber", "!employerId", "!enrollmentId",
      "!placementId", "!jobRoleId", "!isCurrent", "!engagementTypeId", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<List<EmploymentFactResponse>>> getEmploymentsByTraineeId(
      @RequestParam("traineeId") Long traineeId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "traineeId");
    return ResponseEntity.ok(ApiResponse.ok(employmentFactService.getEmploymentsByTraineeId(traineeId)));
  }

  @GetMapping(value = "/employments", params = {
      "employerId", "!employmentNumber", "!traineeId", "!enrollmentId",
      "!placementId", "!jobRoleId", "!isCurrent", "!engagementTypeId", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<List<EmploymentFactResponse>>> getEmploymentsByEmployerId(
      @RequestParam("employerId") Long employerId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "employerId");
    return ResponseEntity.ok(ApiResponse.ok(employmentFactService.getEmploymentsByEmployerId(employerId)));
  }

  @GetMapping(value = "/employments", params = {
      "enrollmentId", "!employmentNumber", "!traineeId", "!employerId",
      "!placementId", "!jobRoleId", "!isCurrent", "!engagementTypeId", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<List<EmploymentFactResponse>>> getEmploymentsByEnrollmentId(
      @RequestParam("enrollmentId") Long enrollmentId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "enrollmentId");
    return ResponseEntity.ok(ApiResponse.ok(employmentFactService.getEmploymentsByEnrollmentId(enrollmentId)));
  }

  @GetMapping(value = "/employments", params = {
      "placementId", "!employmentNumber", "!traineeId", "!employerId",
      "!enrollmentId", "!jobRoleId", "!isCurrent", "!engagementTypeId", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<List<EmploymentFactResponse>>> getEmploymentsByPlacementId(
      @RequestParam("placementId") Long placementId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "placementId");
    return ResponseEntity.ok(ApiResponse.ok(employmentFactService.getEmploymentsByPlacementId(placementId)));
  }

  @GetMapping(value = "/employments", params = {
      "jobRoleId", "!employmentNumber", "!traineeId", "!employerId",
      "!enrollmentId", "!placementId", "!isCurrent", "!engagementTypeId", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<List<EmploymentFactResponse>>> getEmploymentsByJobRoleId(
      @RequestParam("jobRoleId") Long jobRoleId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "jobRoleId");
    return ResponseEntity.ok(ApiResponse.ok(employmentFactService.getEmploymentsByJobRoleId(jobRoleId)));
  }

  @GetMapping(value = "/employments", params = {
      "isCurrent", "!employmentNumber", "!traineeId", "!employerId",
      "!enrollmentId", "!placementId", "!jobRoleId", "!engagementTypeId", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<List<EmploymentFactResponse>>> getCurrentEmployments(
      @RequestParam("isCurrent") boolean isCurrent,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "isCurrent");
    if (!isCurrent) {
      throw new BadRequestException("Only isCurrent=true is supported for current employments filter", "INVALID_FILTER_PARAMETER");
    }
    return ResponseEntity.ok(ApiResponse.ok(employmentFactService.getCurrentEmployments()));
  }

  @GetMapping(value = "/employments", params = {
      "engagementTypeId", "!employmentNumber", "!traineeId", "!employerId",
      "!enrollmentId", "!placementId", "!jobRoleId", "!isCurrent", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<List<EmploymentFactResponse>>> getEmploymentsByEngagementTypeId(
      @RequestParam("engagementTypeId") Long engagementTypeId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "engagementTypeId");
    return ResponseEntity.ok(ApiResponse.ok(employmentFactService.getEmploymentsByEngagementTypeId(engagementTypeId)));
  }

  @GetMapping(value = "/employments", params = {
      "traineeDistrictId", "!employmentNumber", "!traineeId", "!employerId",
      "!enrollmentId", "!placementId", "!jobRoleId", "!isCurrent", "!engagementTypeId"
  })
  public ResponseEntity<ApiResponse<List<EmploymentFactResponse>>> getEmploymentsByTraineeDistrictId(
      @RequestParam("traineeDistrictId") Long traineeDistrictId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "traineeDistrictId");
    return ResponseEntity.ok(ApiResponse.ok(employmentFactService.getEmploymentsByTraineeDistrictId(traineeDistrictId)));
  }

  // =========================================================================
  // 3. Employment Retention Facts (/employment-retentions)
  // =========================================================================

  @GetMapping(value = "/employment-retentions", params = {
      "!traineeId", "!enrollmentId", "!engagementTypeId", "!isCurrent"
  })
  public ResponseEntity<ApiResponse<List<EmploymentRetentionFactResponse>>> getAllEmploymentRetentions(
      HttpServletRequest httpRequest) {
    validateNoQueryParameters(httpRequest);
    return ResponseEntity.ok(ApiResponse.ok(employmentRetentionFactService.getAllEmploymentRetentions()));
  }

  @GetMapping("/employment-retentions/{id}")
  public ResponseEntity<ApiResponse<EmploymentRetentionFactResponse>> getEmploymentRetentionById(
      @PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.ok(employmentRetentionFactService.getEmploymentRetentionById(id)));
  }

  @GetMapping(value = "/employment-retentions", params = {
      "traineeId", "!enrollmentId", "!engagementTypeId", "!isCurrent"
  })
  public ResponseEntity<ApiResponse<List<EmploymentRetentionFactResponse>>> getEmploymentRetentionsByTraineeId(
      @RequestParam("traineeId") Long traineeId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "traineeId");
    return ResponseEntity.ok(ApiResponse.ok(employmentRetentionFactService.getEmploymentRetentionsByTraineeId(traineeId)));
  }

  @GetMapping(value = "/employment-retentions", params = {
      "enrollmentId", "!traineeId", "!engagementTypeId", "!isCurrent"
  })
  public ResponseEntity<ApiResponse<List<EmploymentRetentionFactResponse>>> getEmploymentRetentionsByEnrollmentId(
      @RequestParam("enrollmentId") Long enrollmentId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "enrollmentId");
    return ResponseEntity.ok(ApiResponse.ok(employmentRetentionFactService.getEmploymentRetentionsByEnrollmentId(enrollmentId)));
  }

  @GetMapping(value = "/employment-retentions", params = {
      "engagementTypeId", "!traineeId", "!enrollmentId", "!isCurrent"
  })
  public ResponseEntity<ApiResponse<List<EmploymentRetentionFactResponse>>> getEmploymentRetentionsByEngagementTypeId(
      @RequestParam("engagementTypeId") Long engagementTypeId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "engagementTypeId");
    return ResponseEntity.ok(ApiResponse.ok(employmentRetentionFactService.getEmploymentRetentionsByEngagementTypeId(engagementTypeId)));
  }

  @GetMapping(value = "/employment-retentions", params = {
      "isCurrent", "!traineeId", "!enrollmentId", "!engagementTypeId"
  })
  public ResponseEntity<ApiResponse<List<EmploymentRetentionFactResponse>>> getCurrentEmploymentRetentions(
      @RequestParam("isCurrent") boolean isCurrent,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "isCurrent");
    if (!isCurrent) {
      throw new BadRequestException("Only isCurrent=true is supported for current employment retentions filter", "INVALID_FILTER_PARAMETER");
    }
    return ResponseEntity.ok(ApiResponse.ok(employmentRetentionFactService.getCurrentEmploymentRetentions()));
  }

  // =========================================================================
  // 4. Employment Verification Facts (/employment-verifications)
  // =========================================================================

  @GetMapping(value = "/employment-verifications", params = {
      "!verificationNumber", "!traineeId", "!employmentId", "!placementId",
      "!employerId", "!statusId", "!methodId", "!isCurrent", "!isVerifiedFlag"
  })
  public ResponseEntity<ApiResponse<List<EmploymentVerificationFactResponse>>> getAllEmploymentVerifications(
      HttpServletRequest httpRequest) {
    validateNoQueryParameters(httpRequest);
    return ResponseEntity.ok(ApiResponse.ok(employmentVerificationFactService.getAllEmploymentVerifications()));
  }

  @GetMapping("/employment-verifications/{id}")
  public ResponseEntity<ApiResponse<EmploymentVerificationFactResponse>> getEmploymentVerificationById(
      @PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.ok(employmentVerificationFactService.getEmploymentVerificationById(id)));
  }

  @GetMapping(value = "/employment-verifications", params = {
      "verificationNumber", "!traineeId", "!employmentId", "!placementId",
      "!employerId", "!statusId", "!methodId", "!isCurrent", "!isVerifiedFlag"
  })
  public ResponseEntity<ApiResponse<EmploymentVerificationFactResponse>> getEmploymentVerificationByNumber(
      @RequestParam("verificationNumber") String verificationNumber,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "verificationNumber");
    return ResponseEntity.ok(ApiResponse.ok(
        employmentVerificationFactService.getEmploymentVerificationByNumber(verificationNumber)));
  }

  @GetMapping(value = "/employment-verifications", params = {
      "traineeId", "!verificationNumber", "!employmentId", "!placementId",
      "!employerId", "!statusId", "!methodId", "!isCurrent", "!isVerifiedFlag"
  })
  public ResponseEntity<ApiResponse<List<EmploymentVerificationFactResponse>>> getEmploymentVerificationsByTraineeId(
      @RequestParam("traineeId") Long traineeId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "traineeId");
    return ResponseEntity.ok(ApiResponse.ok(
        employmentVerificationFactService.getEmploymentVerificationsByTraineeId(traineeId)));
  }

  @GetMapping(value = "/employment-verifications", params = {
      "employmentId", "!verificationNumber", "!traineeId", "!placementId",
      "!employerId", "!statusId", "!methodId", "!isCurrent", "!isVerifiedFlag"
  })
  public ResponseEntity<ApiResponse<List<EmploymentVerificationFactResponse>>> getEmploymentVerificationsByEmploymentId(
      @RequestParam("employmentId") Long employmentId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "employmentId");
    return ResponseEntity.ok(ApiResponse.ok(
        employmentVerificationFactService.getEmploymentVerificationsByEmploymentId(employmentId)));
  }

  @GetMapping(value = "/employment-verifications", params = {
      "placementId", "!verificationNumber", "!traineeId", "!employmentId",
      "!employerId", "!statusId", "!methodId", "!isCurrent", "!isVerifiedFlag"
  })
  public ResponseEntity<ApiResponse<List<EmploymentVerificationFactResponse>>> getEmploymentVerificationsByPlacementId(
      @RequestParam("placementId") Long placementId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "placementId");
    return ResponseEntity.ok(ApiResponse.ok(
        employmentVerificationFactService.getEmploymentVerificationsByPlacementId(placementId)));
  }

  @GetMapping(value = "/employment-verifications", params = {
      "employerId", "!verificationNumber", "!traineeId", "!employmentId",
      "!placementId", "!statusId", "!methodId", "!isCurrent", "!isVerifiedFlag"
  })
  public ResponseEntity<ApiResponse<List<EmploymentVerificationFactResponse>>> getEmploymentVerificationsByEmployerId(
      @RequestParam("employerId") Long employerId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "employerId");
    return ResponseEntity.ok(ApiResponse.ok(
        employmentVerificationFactService.getEmploymentVerificationsByEmployerId(employerId)));
  }

  @GetMapping(value = "/employment-verifications", params = {
      "statusId", "!verificationNumber", "!traineeId", "!employmentId",
      "!placementId", "!employerId", "!methodId", "!isCurrent", "!isVerifiedFlag"
  })
  public ResponseEntity<ApiResponse<List<EmploymentVerificationFactResponse>>> getEmploymentVerificationsByStatusId(
      @RequestParam("statusId") Long statusId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "statusId");
    return ResponseEntity.ok(ApiResponse.ok(
        employmentVerificationFactService.getEmploymentVerificationsByStatusId(statusId)));
  }

  @GetMapping(value = "/employment-verifications", params = {
      "methodId", "!verificationNumber", "!traineeId", "!employmentId",
      "!placementId", "!employerId", "!statusId", "!isCurrent", "!isVerifiedFlag"
  })
  public ResponseEntity<ApiResponse<List<EmploymentVerificationFactResponse>>> getEmploymentVerificationsByMethodId(
      @RequestParam("methodId") Long methodId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "methodId");
    return ResponseEntity.ok(ApiResponse.ok(
        employmentVerificationFactService.getEmploymentVerificationsByMethodId(methodId)));
  }

  @GetMapping(value = "/employment-verifications", params = {
      "isCurrent", "!verificationNumber", "!traineeId", "!employmentId",
      "!placementId", "!employerId", "!statusId", "!methodId", "!isVerifiedFlag"
  })
  public ResponseEntity<ApiResponse<List<EmploymentVerificationFactResponse>>> getCurrentEmploymentVerifications(
      @RequestParam("isCurrent") boolean isCurrent,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "isCurrent");
    if (!isCurrent) {
      throw new BadRequestException("Only isCurrent=true is supported for current employment verifications filter", "INVALID_FILTER_PARAMETER");
    }
    return ResponseEntity.ok(ApiResponse.ok(employmentVerificationFactService.getCurrentEmploymentVerifications()));
  }

  @GetMapping(value = "/employment-verifications", params = {
      "isVerifiedFlag", "!verificationNumber", "!traineeId", "!employmentId",
      "!placementId", "!employerId", "!statusId", "!methodId", "!isCurrent"
  })
  public ResponseEntity<ApiResponse<List<EmploymentVerificationFactResponse>>> getEmploymentVerificationsByIsVerifiedFlag(
      @RequestParam("isVerifiedFlag") Boolean isVerifiedFlag,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "isVerifiedFlag");
    return ResponseEntity.ok(ApiResponse.ok(
        employmentVerificationFactService.getEmploymentVerificationsByIsVerifiedFlag(isVerifiedFlag)));
  }

  // =========================================================================
  // 5. Enrollment Outcome Facts (/enrollment-outcomes)
  // =========================================================================

  @GetMapping(value = "/enrollment-outcomes", params = {
      "!enrollmentNumber", "!traineeId", "!programId", "!courseId",
      "!providerId", "!batchId", "!traineeDistrictId", "!traineeStateId"
  })
  public ResponseEntity<ApiResponse<List<EnrollmentOutcomeFactResponse>>> getAllEnrollmentOutcomes(
      HttpServletRequest httpRequest) {
    validateNoQueryParameters(httpRequest);
    return ResponseEntity.ok(ApiResponse.ok(enrollmentOutcomeFactService.getAllEnrollmentOutcomes()));
  }

  @GetMapping("/enrollment-outcomes/{id}")
  public ResponseEntity<ApiResponse<EnrollmentOutcomeFactResponse>> getEnrollmentOutcomeById(
      @PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.ok(enrollmentOutcomeFactService.getEnrollmentOutcomeById(id)));
  }

  @GetMapping(value = "/enrollment-outcomes", params = {
      "enrollmentNumber", "!traineeId", "!programId", "!courseId",
      "!providerId", "!batchId", "!traineeDistrictId", "!traineeStateId"
  })
  public ResponseEntity<ApiResponse<EnrollmentOutcomeFactResponse>> getEnrollmentOutcomeByEnrollmentNumber(
      @RequestParam("enrollmentNumber") String enrollmentNumber,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "enrollmentNumber");
    return ResponseEntity.ok(ApiResponse.ok(
        enrollmentOutcomeFactService.getEnrollmentOutcomeByEnrollmentNumber(enrollmentNumber)));
  }

  @GetMapping(value = "/enrollment-outcomes", params = {
      "traineeId", "!enrollmentNumber", "!programId", "!courseId",
      "!providerId", "!batchId", "!traineeDistrictId", "!traineeStateId"
  })
  public ResponseEntity<ApiResponse<List<EnrollmentOutcomeFactResponse>>> getEnrollmentOutcomesByTraineeId(
      @RequestParam("traineeId") Long traineeId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "traineeId");
    return ResponseEntity.ok(ApiResponse.ok(
        enrollmentOutcomeFactService.getEnrollmentOutcomesByTraineeId(traineeId)));
  }

  @GetMapping(value = "/enrollment-outcomes", params = {
      "programId", "!enrollmentNumber", "!traineeId", "!courseId",
      "!providerId", "!batchId", "!traineeDistrictId", "!traineeStateId"
  })
  public ResponseEntity<ApiResponse<List<EnrollmentOutcomeFactResponse>>> getEnrollmentOutcomesByProgramId(
      @RequestParam("programId") Long programId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "programId");
    return ResponseEntity.ok(ApiResponse.ok(
        enrollmentOutcomeFactService.getEnrollmentOutcomesByProgramId(programId)));
  }

  @GetMapping(value = "/enrollment-outcomes", params = {
      "courseId", "!enrollmentNumber", "!traineeId", "!programId",
      "!providerId", "!batchId", "!traineeDistrictId", "!traineeStateId"
  })
  public ResponseEntity<ApiResponse<List<EnrollmentOutcomeFactResponse>>> getEnrollmentOutcomesByCourseId(
      @RequestParam("courseId") Long courseId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "courseId");
    return ResponseEntity.ok(ApiResponse.ok(
        enrollmentOutcomeFactService.getEnrollmentOutcomesByCourseId(courseId)));
  }

  @GetMapping(value = "/enrollment-outcomes", params = {
      "providerId", "!enrollmentNumber", "!traineeId", "!programId",
      "!courseId", "!batchId", "!traineeDistrictId", "!traineeStateId"
  })
  public ResponseEntity<ApiResponse<List<EnrollmentOutcomeFactResponse>>> getEnrollmentOutcomesByProviderId(
      @RequestParam("providerId") Long providerId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "providerId");
    return ResponseEntity.ok(ApiResponse.ok(
        enrollmentOutcomeFactService.getEnrollmentOutcomesByProviderId(providerId)));
  }

  @GetMapping(value = "/enrollment-outcomes", params = {
      "batchId", "!enrollmentNumber", "!traineeId", "!programId",
      "!courseId", "!providerId", "!traineeDistrictId", "!traineeStateId"
  })
  public ResponseEntity<ApiResponse<List<EnrollmentOutcomeFactResponse>>> getEnrollmentOutcomesByBatchId(
      @RequestParam("batchId") Long batchId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "batchId");
    return ResponseEntity.ok(ApiResponse.ok(
        enrollmentOutcomeFactService.getEnrollmentOutcomesByBatchId(batchId)));
  }

  @GetMapping(value = "/enrollment-outcomes", params = {
      "traineeDistrictId", "!enrollmentNumber", "!traineeId", "!programId",
      "!courseId", "!providerId", "!batchId", "!traineeStateId"
  })
  public ResponseEntity<ApiResponse<List<EnrollmentOutcomeFactResponse>>> getEnrollmentOutcomesByTraineeDistrictId(
      @RequestParam("traineeDistrictId") Long traineeDistrictId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "traineeDistrictId");
    return ResponseEntity.ok(ApiResponse.ok(
        enrollmentOutcomeFactService.getEnrollmentOutcomesByTraineeDistrictId(traineeDistrictId)));
  }

  @GetMapping(value = "/enrollment-outcomes", params = {
      "traineeStateId", "!enrollmentNumber", "!traineeId", "!programId",
      "!courseId", "!providerId", "!batchId", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<List<EnrollmentOutcomeFactResponse>>> getEnrollmentOutcomesByTraineeStateId(
      @RequestParam("traineeStateId") Long traineeStateId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "traineeStateId");
    return ResponseEntity.ok(ApiResponse.ok(
        enrollmentOutcomeFactService.getEnrollmentOutcomesByTraineeStateId(traineeStateId)));
  }

  // =========================================================================
  // 6. Followup Facts (/followups)
  // =========================================================================

  @GetMapping(value = "/followups", params = {
      "!traineeId", "!campaignId", "!typeId", "!typeCode",
      "!statusId", "!outcomeId", "!isOpenFlag"
  })
  public ResponseEntity<ApiResponse<List<FollowupFactResponse>>> getAllFollowupFacts(
      HttpServletRequest httpRequest) {
    validateNoQueryParameters(httpRequest);
    return ResponseEntity.ok(ApiResponse.ok(followupFactService.getAllFollowupFacts()));
  }

  @GetMapping("/followups/{id}")
  public ResponseEntity<ApiResponse<FollowupFactResponse>> getFollowupFactById(
      @PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.ok(followupFactService.getFollowupFactById(id)));
  }

  @GetMapping(value = "/followups", params = {
      "traineeId", "!campaignId", "!typeId", "!typeCode",
      "!statusId", "!outcomeId", "!isOpenFlag"
  })
  public ResponseEntity<ApiResponse<List<FollowupFactResponse>>> getFollowupFactsByTraineeId(
      @RequestParam("traineeId") Long traineeId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "traineeId");
    return ResponseEntity.ok(ApiResponse.ok(followupFactService.getFollowupFactsByTraineeId(traineeId)));
  }

  @GetMapping(value = "/followups", params = {
      "campaignId", "!traineeId", "!typeId", "!typeCode",
      "!statusId", "!outcomeId", "!isOpenFlag"
  })
  public ResponseEntity<ApiResponse<List<FollowupFactResponse>>> getFollowupFactsByCampaignId(
      @RequestParam("campaignId") Long campaignId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "campaignId");
    return ResponseEntity.ok(ApiResponse.ok(followupFactService.getFollowupFactsByCampaignId(campaignId)));
  }

  @GetMapping(value = "/followups", params = {
      "typeId", "!traineeId", "!campaignId", "!typeCode",
      "!statusId", "!outcomeId", "!isOpenFlag"
  })
  public ResponseEntity<ApiResponse<List<FollowupFactResponse>>> getFollowupFactsByTypeId(
      @RequestParam("typeId") Long typeId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "typeId");
    return ResponseEntity.ok(ApiResponse.ok(followupFactService.getFollowupFactsByTypeId(typeId)));
  }

  @GetMapping(value = "/followups", params = {
      "typeCode", "!traineeId", "!campaignId", "!typeId",
      "!statusId", "!outcomeId", "!isOpenFlag"
  })
  public ResponseEntity<ApiResponse<List<FollowupFactResponse>>> getFollowupFactsByTypeCode(
      @RequestParam("typeCode") String typeCode,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "typeCode");
    return ResponseEntity.ok(ApiResponse.ok(followupFactService.getFollowupFactsByTypeCode(typeCode)));
  }

  @GetMapping(value = "/followups", params = {
      "statusId", "!traineeId", "!campaignId", "!typeId",
      "!typeCode", "!outcomeId", "!isOpenFlag"
  })
  public ResponseEntity<ApiResponse<List<FollowupFactResponse>>> getFollowupFactsByStatusId(
      @RequestParam("statusId") Long statusId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "statusId");
    return ResponseEntity.ok(ApiResponse.ok(followupFactService.getFollowupFactsByStatusId(statusId)));
  }

  @GetMapping(value = "/followups", params = {
      "outcomeId", "!traineeId", "!campaignId", "!typeId",
      "!typeCode", "!statusId", "!isOpenFlag"
  })
  public ResponseEntity<ApiResponse<List<FollowupFactResponse>>> getFollowupFactsByOutcomeId(
      @RequestParam("outcomeId") Long outcomeId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "outcomeId");
    return ResponseEntity.ok(ApiResponse.ok(followupFactService.getFollowupFactsByOutcomeId(outcomeId)));
  }

  @GetMapping(value = "/followups", params = {
      "isOpenFlag", "!traineeId", "!campaignId", "!typeId",
      "!typeCode", "!statusId", "!outcomeId"
  })
  public ResponseEntity<ApiResponse<List<FollowupFactResponse>>> getFollowupFactsByIsOpenFlag(
      @RequestParam("isOpenFlag") Boolean isOpenFlag,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "isOpenFlag");
    return ResponseEntity.ok(ApiResponse.ok(followupFactService.getFollowupFactsByIsOpenFlag(isOpenFlag)));
  }

  // =========================================================================
  // 7. Placement Facts (/placements)
  // =========================================================================

  @GetMapping(value = "/placements", params = {
      "!placementNumber", "!traineeId", "!enrollmentId", "!employerId",
      "!courseId", "!programId", "!providerId", "!jobRoleId", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<List<PlacementFactResponse>>> getAllPlacements(
      HttpServletRequest httpRequest) {
    validateNoQueryParameters(httpRequest);
    return ResponseEntity.ok(ApiResponse.ok(placementFactService.getAllPlacements()));
  }

  @GetMapping("/placements/{id}")
  public ResponseEntity<ApiResponse<PlacementFactResponse>> getPlacementById(
      @PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.ok(placementFactService.getPlacementById(id)));
  }

  @GetMapping(value = "/placements", params = {
      "placementNumber", "!traineeId", "!enrollmentId", "!employerId",
      "!courseId", "!programId", "!providerId", "!jobRoleId", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<PlacementFactResponse>> getPlacementByPlacementNumber(
      @RequestParam("placementNumber") String placementNumber,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "placementNumber");
    return ResponseEntity.ok(ApiResponse.ok(placementFactService.getPlacementByPlacementNumber(placementNumber)));
  }

  @GetMapping(value = "/placements", params = {
      "traineeId", "!placementNumber", "!enrollmentId", "!employerId",
      "!courseId", "!programId", "!providerId", "!jobRoleId", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<List<PlacementFactResponse>>> getPlacementsByTraineeId(
      @RequestParam("traineeId") Long traineeId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "traineeId");
    return ResponseEntity.ok(ApiResponse.ok(placementFactService.getPlacementsByTraineeId(traineeId)));
  }

  @GetMapping(value = "/placements", params = {
      "enrollmentId", "!placementNumber", "!traineeId", "!employerId",
      "!courseId", "!programId", "!providerId", "!jobRoleId", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<List<PlacementFactResponse>>> getPlacementsByEnrollmentId(
      @RequestParam("enrollmentId") Long enrollmentId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "enrollmentId");
    return ResponseEntity.ok(ApiResponse.ok(placementFactService.getPlacementsByEnrollmentId(enrollmentId)));
  }

  @GetMapping(value = "/placements", params = {
      "employerId", "!placementNumber", "!traineeId", "!enrollmentId",
      "!courseId", "!programId", "!providerId", "!jobRoleId", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<List<PlacementFactResponse>>> getPlacementsByEmployerId(
      @RequestParam("employerId") Long employerId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "employerId");
    return ResponseEntity.ok(ApiResponse.ok(placementFactService.getPlacementsByEmployerId(employerId)));
  }

  @GetMapping(value = "/placements", params = {
      "courseId", "!placementNumber", "!traineeId", "!enrollmentId",
      "!employerId", "!programId", "!providerId", "!jobRoleId", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<List<PlacementFactResponse>>> getPlacementsByCourseId(
      @RequestParam("courseId") Long courseId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "courseId");
    return ResponseEntity.ok(ApiResponse.ok(placementFactService.getPlacementsByCourseId(courseId)));
  }

  @GetMapping(value = "/placements", params = {
      "programId", "!placementNumber", "!traineeId", "!enrollmentId",
      "!employerId", "!courseId", "!providerId", "!jobRoleId", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<List<PlacementFactResponse>>> getPlacementsByProgramId(
      @RequestParam("programId") Long programId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "programId");
    return ResponseEntity.ok(ApiResponse.ok(placementFactService.getPlacementsByProgramId(programId)));
  }

  @GetMapping(value = "/placements", params = {
      "providerId", "!placementNumber", "!traineeId", "!enrollmentId",
      "!employerId", "!courseId", "!programId", "!jobRoleId", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<List<PlacementFactResponse>>> getPlacementsByProviderId(
      @RequestParam("providerId") Long providerId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "providerId");
    return ResponseEntity.ok(ApiResponse.ok(placementFactService.getPlacementsByProviderId(providerId)));
  }

  @GetMapping(value = "/placements", params = {
      "jobRoleId", "!placementNumber", "!traineeId", "!enrollmentId",
      "!employerId", "!courseId", "!programId", "!providerId", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<List<PlacementFactResponse>>> getPlacementsByJobRoleId(
      @RequestParam("jobRoleId") Long jobRoleId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "jobRoleId");
    return ResponseEntity.ok(ApiResponse.ok(placementFactService.getPlacementsByJobRoleId(jobRoleId)));
  }

  @GetMapping(value = "/placements", params = {
      "traineeDistrictId", "!placementNumber", "!traineeId", "!enrollmentId",
      "!employerId", "!courseId", "!programId", "!providerId", "!jobRoleId"
  })
  public ResponseEntity<ApiResponse<List<PlacementFactResponse>>> getPlacementsByTraineeDistrictId(
      @RequestParam("traineeDistrictId") Long traineeDistrictId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "traineeDistrictId");
    return ResponseEntity.ok(ApiResponse.ok(placementFactService.getPlacementsByTraineeDistrictId(traineeDistrictId)));
  }

  // =========================================================================
  // 8. Salary Progression Facts (/salary-progressions)
  // =========================================================================

  @GetMapping(value = "/salary-progressions", params = {
      "!traineeId", "!enrollmentId", "!courseId", "!providerId", "!engagementTypeId"
  })
  public ResponseEntity<ApiResponse<List<SalaryProgressionFactResponse>>> getAllSalaryProgressions(
      HttpServletRequest httpRequest) {
    validateNoQueryParameters(httpRequest);
    return ResponseEntity.ok(ApiResponse.ok(salaryProgressionFactService.getAllSalaryProgressions()));
  }

  @GetMapping("/salary-progressions/{id}")
  public ResponseEntity<ApiResponse<SalaryProgressionFactResponse>> getSalaryProgressionById(
      @PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.ok(salaryProgressionFactService.getSalaryProgressionById(id)));
  }

  @GetMapping(value = "/salary-progressions", params = {
      "traineeId", "!enrollmentId", "!courseId", "!providerId", "!engagementTypeId"
  })
  public ResponseEntity<ApiResponse<List<SalaryProgressionFactResponse>>> getSalaryProgressionsByTraineeId(
      @RequestParam("traineeId") Long traineeId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "traineeId");
    return ResponseEntity.ok(ApiResponse.ok(salaryProgressionFactService.getSalaryProgressionsByTraineeId(traineeId)));
  }

  @GetMapping(value = "/salary-progressions", params = {
      "enrollmentId", "!traineeId", "!courseId", "!providerId", "!engagementTypeId"
  })
  public ResponseEntity<ApiResponse<List<SalaryProgressionFactResponse>>> getSalaryProgressionsByEnrollmentId(
      @RequestParam("enrollmentId") Long enrollmentId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "enrollmentId");
    return ResponseEntity.ok(ApiResponse.ok(salaryProgressionFactService.getSalaryProgressionsByEnrollmentId(enrollmentId)));
  }

  @GetMapping(value = "/salary-progressions", params = {
      "courseId", "!traineeId", "!enrollmentId", "!providerId", "!engagementTypeId"
  })
  public ResponseEntity<ApiResponse<List<SalaryProgressionFactResponse>>> getSalaryProgressionsByCourseId(
      @RequestParam("courseId") Long courseId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "courseId");
    return ResponseEntity.ok(ApiResponse.ok(salaryProgressionFactService.getSalaryProgressionsByCourseId(courseId)));
  }

  @GetMapping(value = "/salary-progressions", params = {
      "providerId", "!traineeId", "!enrollmentId", "!courseId", "!engagementTypeId"
  })
  public ResponseEntity<ApiResponse<List<SalaryProgressionFactResponse>>> getSalaryProgressionsByProviderId(
      @RequestParam("providerId") Long providerId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "providerId");
    return ResponseEntity.ok(ApiResponse.ok(salaryProgressionFactService.getSalaryProgressionsByProviderId(providerId)));
  }

  @GetMapping(value = "/salary-progressions", params = {
      "engagementTypeId", "!traineeId", "!enrollmentId", "!courseId", "!providerId"
  })
  public ResponseEntity<ApiResponse<List<SalaryProgressionFactResponse>>> getSalaryProgressionsByEngagementTypeId(
      @RequestParam("engagementTypeId") Long engagementTypeId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "engagementTypeId");
    return ResponseEntity.ok(ApiResponse.ok(salaryProgressionFactService.getSalaryProgressionsByEngagementTypeId(engagementTypeId)));
  }

  // =========================================================================
  // 9. Skill Gap Facts (/skill-gaps)
  // =========================================================================

  @GetMapping(value = "/skill-gaps", params = {
      "!skillGapNumber", "!traineeId", "!skillId", "!courseId",
      "!jobRoleId", "!severityId", "!statusId", "!isCurrent", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<List<SkillGapFactResponse>>> getAllSkillGaps(
      HttpServletRequest httpRequest) {
    validateNoQueryParameters(httpRequest);
    return ResponseEntity.ok(ApiResponse.ok(skillGapFactService.getAllSkillGaps()));
  }

  @GetMapping("/skill-gaps/{id}")
  public ResponseEntity<ApiResponse<SkillGapFactResponse>> getSkillGapById(
      @PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.ok(skillGapFactService.getSkillGapById(id)));
  }

  @GetMapping(value = "/skill-gaps", params = {
      "skillGapNumber", "!traineeId", "!skillId", "!courseId",
      "!jobRoleId", "!severityId", "!statusId", "!isCurrent", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<SkillGapFactResponse>> getSkillGapByNumber(
      @RequestParam("skillGapNumber") String skillGapNumber,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "skillGapNumber");
    return ResponseEntity.ok(ApiResponse.ok(skillGapFactService.getSkillGapByNumber(skillGapNumber)));
  }

  @GetMapping(value = "/skill-gaps", params = {
      "traineeId", "!skillGapNumber", "!skillId", "!courseId",
      "!jobRoleId", "!severityId", "!statusId", "!isCurrent", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<List<SkillGapFactResponse>>> getSkillGapsByTraineeId(
      @RequestParam("traineeId") Long traineeId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "traineeId");
    return ResponseEntity.ok(ApiResponse.ok(skillGapFactService.getSkillGapsByTraineeId(traineeId)));
  }

  @GetMapping(value = "/skill-gaps", params = {
      "skillId", "!skillGapNumber", "!traineeId", "!courseId",
      "!jobRoleId", "!severityId", "!statusId", "!isCurrent", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<List<SkillGapFactResponse>>> getSkillGapsBySkillId(
      @RequestParam("skillId") Long skillId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "skillId");
    return ResponseEntity.ok(ApiResponse.ok(skillGapFactService.getSkillGapsBySkillId(skillId)));
  }

  @GetMapping(value = "/skill-gaps", params = {
      "courseId", "!skillGapNumber", "!traineeId", "!skillId",
      "!jobRoleId", "!severityId", "!statusId", "!isCurrent", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<List<SkillGapFactResponse>>> getSkillGapsByCourseId(
      @RequestParam("courseId") Long courseId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "courseId");
    return ResponseEntity.ok(ApiResponse.ok(skillGapFactService.getSkillGapsByCourseId(courseId)));
  }

  @GetMapping(value = "/skill-gaps", params = {
      "jobRoleId", "!skillGapNumber", "!traineeId", "!skillId",
      "!courseId", "!severityId", "!statusId", "!isCurrent", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<List<SkillGapFactResponse>>> getSkillGapsByJobRoleId(
      @RequestParam("jobRoleId") Long jobRoleId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "jobRoleId");
    return ResponseEntity.ok(ApiResponse.ok(skillGapFactService.getSkillGapsByJobRoleId(jobRoleId)));
  }

  @GetMapping(value = "/skill-gaps", params = {
      "severityId", "!skillGapNumber", "!traineeId", "!skillId",
      "!courseId", "!jobRoleId", "!statusId", "!isCurrent", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<List<SkillGapFactResponse>>> getSkillGapsBySeverityId(
      @RequestParam("severityId") Long severityId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "severityId");
    return ResponseEntity.ok(ApiResponse.ok(skillGapFactService.getSkillGapsBySeverityId(severityId)));
  }

  @GetMapping(value = "/skill-gaps", params = {
      "statusId", "!skillGapNumber", "!traineeId", "!skillId",
      "!courseId", "!jobRoleId", "!severityId", "!isCurrent", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<List<SkillGapFactResponse>>> getSkillGapsByStatusId(
      @RequestParam("statusId") Long statusId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "statusId");
    return ResponseEntity.ok(ApiResponse.ok(skillGapFactService.getSkillGapsByStatusId(statusId)));
  }

  @GetMapping(value = "/skill-gaps", params = {
      "isCurrent", "!skillGapNumber", "!traineeId", "!skillId",
      "!courseId", "!jobRoleId", "!severityId", "!statusId", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<List<SkillGapFactResponse>>> getCurrentSkillGaps(
      @RequestParam("isCurrent") boolean isCurrent,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "isCurrent");
    if (!isCurrent) {
      throw new BadRequestException("Only isCurrent=true is supported for current skill gaps filter", "INVALID_FILTER_PARAMETER");
    }
    return ResponseEntity.ok(ApiResponse.ok(skillGapFactService.getCurrentSkillGaps()));
  }

  @GetMapping(value = "/skill-gaps", params = {
      "traineeDistrictId", "!skillGapNumber", "!traineeId", "!skillId",
      "!courseId", "!jobRoleId", "!severityId", "!statusId", "!isCurrent"
  })
  public ResponseEntity<ApiResponse<List<SkillGapFactResponse>>> getSkillGapsByTraineeDistrictId(
      @RequestParam("traineeDistrictId") Long traineeDistrictId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "traineeDistrictId");
    return ResponseEntity.ok(ApiResponse.ok(skillGapFactService.getSkillGapsByTraineeDistrictId(traineeDistrictId)));
  }

  // =========================================================================
  // 10. Survey Response Facts (/survey-responses)
  // =========================================================================

  @GetMapping(value = "/survey-responses", params = {
      "!traineeId", "!surveyId", "!surveyPurposeId", "!surveyPurposeCode",
      "!programId", "!courseId", "!batchId", "!enrollmentId", "!followupTaskId", "!isSubmittedFlag"
  })
  public ResponseEntity<ApiResponse<List<SurveyResponseFactResponse>>> getAllSurveyResponses(
      HttpServletRequest httpRequest) {
    validateNoQueryParameters(httpRequest);
    return ResponseEntity.ok(ApiResponse.ok(surveyResponseFactService.getAllSurveyResponses()));
  }

  @GetMapping("/survey-responses/{id}")
  public ResponseEntity<ApiResponse<SurveyResponseFactResponse>> getSurveyResponseById(
      @PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.ok(surveyResponseFactService.getSurveyResponseById(id)));
  }

  @GetMapping(value = "/survey-responses", params = {
      "traineeId", "!surveyId", "!surveyPurposeId", "!surveyPurposeCode",
      "!programId", "!courseId", "!batchId", "!enrollmentId", "!followupTaskId", "!isSubmittedFlag"
  })
  public ResponseEntity<ApiResponse<List<SurveyResponseFactResponse>>> getSurveyResponsesByTraineeId(
      @RequestParam("traineeId") Long traineeId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "traineeId");
    return ResponseEntity.ok(ApiResponse.ok(surveyResponseFactService.getSurveyResponsesByTraineeId(traineeId)));
  }

  @GetMapping(value = "/survey-responses", params = {
      "surveyId", "!traineeId", "!surveyPurposeId", "!surveyPurposeCode",
      "!programId", "!courseId", "!batchId", "!enrollmentId", "!followupTaskId", "!isSubmittedFlag"
  })
  public ResponseEntity<ApiResponse<List<SurveyResponseFactResponse>>> getSurveyResponsesBySurveyId(
      @RequestParam("surveyId") Long surveyId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "surveyId");
    return ResponseEntity.ok(ApiResponse.ok(surveyResponseFactService.getSurveyResponsesBySurveyId(surveyId)));
  }

  @GetMapping(value = "/survey-responses", params = {
      "surveyPurposeId", "!traineeId", "!surveyId", "!surveyPurposeCode",
      "!programId", "!courseId", "!batchId", "!enrollmentId", "!followupTaskId", "!isSubmittedFlag"
  })
  public ResponseEntity<ApiResponse<List<SurveyResponseFactResponse>>> getSurveyResponsesBySurveyPurposeId(
      @RequestParam("surveyPurposeId") Long surveyPurposeId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "surveyPurposeId");
    return ResponseEntity.ok(ApiResponse.ok(
        surveyResponseFactService.getSurveyResponsesBySurveyPurposeId(surveyPurposeId)));
  }

  @GetMapping(value = "/survey-responses", params = {
      "surveyPurposeCode", "!traineeId", "!surveyId", "!surveyPurposeId",
      "!programId", "!courseId", "!batchId", "!enrollmentId", "!followupTaskId", "!isSubmittedFlag"
  })
  public ResponseEntity<ApiResponse<List<SurveyResponseFactResponse>>> getSurveyResponsesBySurveyPurposeCode(
      @RequestParam("surveyPurposeCode") String surveyPurposeCode,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "surveyPurposeCode");
    return ResponseEntity.ok(ApiResponse.ok(
        surveyResponseFactService.getSurveyResponsesBySurveyPurposeCode(surveyPurposeCode)));
  }

  @GetMapping(value = "/survey-responses", params = {
      "programId", "!traineeId", "!surveyId", "!surveyPurposeId",
      "!surveyPurposeCode", "!courseId", "!batchId", "!enrollmentId", "!followupTaskId", "!isSubmittedFlag"
  })
  public ResponseEntity<ApiResponse<List<SurveyResponseFactResponse>>> getSurveyResponsesByProgramId(
      @RequestParam("programId") Long programId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "programId");
    return ResponseEntity.ok(ApiResponse.ok(
        surveyResponseFactService.getSurveyResponsesByProgramId(programId)));
  }

  @GetMapping(value = "/survey-responses", params = {
      "courseId", "!traineeId", "!surveyId", "!surveyPurposeId",
      "!surveyPurposeCode", "!programId", "!batchId", "!enrollmentId", "!followupTaskId", "!isSubmittedFlag"
  })
  public ResponseEntity<ApiResponse<List<SurveyResponseFactResponse>>> getSurveyResponsesByCourseId(
      @RequestParam("courseId") Long courseId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "courseId");
    return ResponseEntity.ok(ApiResponse.ok(
        surveyResponseFactService.getSurveyResponsesByCourseId(courseId)));
  }

  @GetMapping(value = "/survey-responses", params = {
      "batchId", "!traineeId", "!surveyId", "!surveyPurposeId",
      "!surveyPurposeCode", "!programId", "!courseId", "!enrollmentId", "!followupTaskId", "!isSubmittedFlag"
  })
  public ResponseEntity<ApiResponse<List<SurveyResponseFactResponse>>> getSurveyResponsesByBatchId(
      @RequestParam("batchId") Long batchId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "batchId");
    return ResponseEntity.ok(ApiResponse.ok(
        surveyResponseFactService.getSurveyResponsesByBatchId(batchId)));
  }

  @GetMapping(value = "/survey-responses", params = {
      "enrollmentId", "!traineeId", "!surveyId", "!surveyPurposeId",
      "!surveyPurposeCode", "!programId", "!courseId", "!batchId", "!followupTaskId", "!isSubmittedFlag"
  })
  public ResponseEntity<ApiResponse<List<SurveyResponseFactResponse>>> getSurveyResponsesByEnrollmentId(
      @RequestParam("enrollmentId") Long enrollmentId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "enrollmentId");
    return ResponseEntity.ok(ApiResponse.ok(
        surveyResponseFactService.getSurveyResponsesByEnrollmentId(enrollmentId)));
  }

  @GetMapping(value = "/survey-responses", params = {
      "followupTaskId", "!traineeId", "!surveyId", "!surveyPurposeId",
      "!surveyPurposeCode", "!programId", "!courseId", "!batchId", "!enrollmentId", "!isSubmittedFlag"
  })
  public ResponseEntity<ApiResponse<List<SurveyResponseFactResponse>>> getSurveyResponsesByFollowupTaskId(
      @RequestParam("followupTaskId") Long followupTaskId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "followupTaskId");
    return ResponseEntity.ok(ApiResponse.ok(
        surveyResponseFactService.getSurveyResponsesByFollowupTaskId(followupTaskId)));
  }

  @GetMapping(value = "/survey-responses", params = {
      "isSubmittedFlag", "!traineeId", "!surveyId", "!surveyPurposeId",
      "!surveyPurposeCode", "!programId", "!courseId", "!batchId", "!enrollmentId", "!followupTaskId"
  })
  public ResponseEntity<ApiResponse<List<SurveyResponseFactResponse>>> getSurveyResponsesByIsSubmittedFlag(
      @RequestParam("isSubmittedFlag") Boolean isSubmittedFlag,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "isSubmittedFlag");
    return ResponseEntity.ok(ApiResponse.ok(
        surveyResponseFactService.getSurveyResponsesByIsSubmittedFlag(isSubmittedFlag)));
  }

  // =========================================================================
  // 11. Unemployment Facts (/unemployments)
  // =========================================================================

  @GetMapping(value = "/unemployments", params = {
      "!traineeId", "!unemploymentReasonId", "!labourStatusId", "!isCurrent", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<List<UnemploymentFactResponse>>> getAllUnemploymentFacts(
      HttpServletRequest httpRequest) {
    validateNoQueryParameters(httpRequest);
    return ResponseEntity.ok(ApiResponse.ok(unemploymentFactService.getAllUnemploymentFacts()));
  }

  @GetMapping("/unemployments/{id}")
  public ResponseEntity<ApiResponse<UnemploymentFactResponse>> getUnemploymentFactById(
      @PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.ok(unemploymentFactService.getUnemploymentFactById(id)));
  }

  @GetMapping(value = "/unemployments", params = {
      "traineeId", "!unemploymentReasonId", "!labourStatusId", "!isCurrent", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<List<UnemploymentFactResponse>>> getUnemploymentFactsByTraineeId(
      @RequestParam("traineeId") Long traineeId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "traineeId");
    return ResponseEntity.ok(ApiResponse.ok(unemploymentFactService.getUnemploymentFactsByTraineeId(traineeId)));
  }

  @GetMapping(value = "/unemployments", params = {
      "unemploymentReasonId", "!traineeId", "!labourStatusId", "!isCurrent", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<List<UnemploymentFactResponse>>> getUnemploymentFactsByUnemploymentReasonId(
      @RequestParam("unemploymentReasonId") Long unemploymentReasonId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "unemploymentReasonId");
    return ResponseEntity.ok(ApiResponse.ok(
        unemploymentFactService.getUnemploymentFactsByUnemploymentReasonId(unemploymentReasonId)));
  }

  @GetMapping(value = "/unemployments", params = {
      "labourStatusId", "!traineeId", "!unemploymentReasonId", "!isCurrent", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<List<UnemploymentFactResponse>>> getUnemploymentFactsByLabourStatusId(
      @RequestParam("labourStatusId") Long labourStatusId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "labourStatusId");
    return ResponseEntity.ok(ApiResponse.ok(
        unemploymentFactService.getUnemploymentFactsByLabourStatusId(labourStatusId)));
  }

  @GetMapping(value = "/unemployments", params = {
      "isCurrent", "!traineeId", "!unemploymentReasonId", "!labourStatusId", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<List<UnemploymentFactResponse>>> getCurrentUnemploymentFacts(
      @RequestParam("isCurrent") boolean isCurrent,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "isCurrent");
    if (!isCurrent) {
      throw new BadRequestException("Only isCurrent=true is supported for current unemployments filter", "INVALID_FILTER_PARAMETER");
    }
    return ResponseEntity.ok(ApiResponse.ok(unemploymentFactService.getCurrentUnemploymentFacts()));
  }

  @GetMapping(value = "/unemployments", params = {
      "traineeDistrictId", "!traineeId", "!unemploymentReasonId", "!labourStatusId", "!isCurrent"
  })
  public ResponseEntity<ApiResponse<List<UnemploymentFactResponse>>> getUnemploymentFactsByTraineeDistrictId(
      @RequestParam("traineeDistrictId") Long traineeDistrictId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "traineeDistrictId");
    return ResponseEntity.ok(ApiResponse.ok(
        unemploymentFactService.getUnemploymentFactsByTraineeDistrictId(traineeDistrictId)));
  }

  // =========================================================================
  // Validation Helpers
  // =========================================================================

  private void validateNoQueryParameters(HttpServletRequest request) {
    if (!request.getParameterMap().isEmpty()) {
      throw new BadRequestException(
          "Unsupported query parameters: " + request.getParameterMap().keySet(),
          "UNSUPPORTED_PARAMETER"
      );
    }
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
}
