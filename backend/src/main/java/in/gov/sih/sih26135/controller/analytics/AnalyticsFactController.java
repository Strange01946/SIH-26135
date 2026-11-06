package in.gov.sih.sih26135.controller.analytics;

import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotBlank;
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
import in.gov.sih.sih26135.response.PagedResponse;
import in.gov.sih.sih26135.response.PaginationUtils;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
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
import java.util.Objects;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

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
@Validated
@RestController
@RequestMapping("/api/v1/analytics/facts")
@PreAuthorize("hasAuthority('analytics.read')")
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
  private final WorkflowSecurityScopeValidator scopeValidator;

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
      UnemploymentFactService unemploymentFactService,
      WorkflowSecurityScopeValidator scopeValidator) {
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
    this.scopeValidator = Objects.requireNonNull(scopeValidator, "scopeValidator must not be null");
  }

  // =========================================================================
  // 1. Employment Exit Facts (/employment-exits)
  // =========================================================================

  @GetMapping(value = "/employment-exits", params = {
      "!traineeId", "!employmentId", "!employmentExitReasonId", "!separationNatureId",
      "!isVoluntaryFlag", "!isInvoluntaryFlag"
  })
  public ResponseEntity<ApiResponse<PagedResponse<EmploymentExitFactResponse>>> getAllEmploymentExits(
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validatePaginationParametersOnly(httpRequest);
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.EMPLOYMENT_EXIT_FACT_SORT_FIELDS,
        "employmentExitEventId", "employmentExitEventId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(employmentExitFactService.getAllEmploymentExits(pageable)));
  }

  @GetMapping("/employment-exits/{id}")
  public ResponseEntity<ApiResponse<EmploymentExitFactResponse>> getEmploymentExitById(
      @PathVariable @Positive Long id) {
    scopeValidator.validateNationwideScope();
    return ResponseEntity.ok(ApiResponse.ok(employmentExitFactService.getEmploymentExitById(id)));
  }

  @GetMapping(value = "/employment-exits", params = {
      "traineeId", "!employmentId", "!employmentExitReasonId", "!separationNatureId",
      "!isVoluntaryFlag", "!isInvoluntaryFlag"
  })
  public ResponseEntity<ApiResponse<PagedResponse<EmploymentExitFactResponse>>> getEmploymentExitsByTraineeId(
      @RequestParam("traineeId") @Positive Long traineeId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "traineeId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.EMPLOYMENT_EXIT_FACT_SORT_FIELDS,
        "employmentExitEventId", "employmentExitEventId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(employmentExitFactService.getEmploymentExitsByTraineeId(traineeId, pageable)));
  }

  @GetMapping(value = "/employment-exits", params = {
      "employmentId", "!traineeId", "!employmentExitReasonId", "!separationNatureId",
      "!isVoluntaryFlag", "!isInvoluntaryFlag"
  })
  public ResponseEntity<ApiResponse<PagedResponse<EmploymentExitFactResponse>>> getEmploymentExitsByEmploymentId(
      @RequestParam("employmentId") @Positive Long employmentId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "employmentId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.EMPLOYMENT_EXIT_FACT_SORT_FIELDS,
        "employmentExitEventId", "employmentExitEventId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(employmentExitFactService.getEmploymentExitsByEmploymentId(employmentId, pageable)));
  }

  @GetMapping(value = "/employment-exits", params = {
      "employmentExitReasonId", "!traineeId", "!employmentId", "!separationNatureId",
      "!isVoluntaryFlag", "!isInvoluntaryFlag"
  })
  public ResponseEntity<ApiResponse<PagedResponse<EmploymentExitFactResponse>>> getEmploymentExitsByExitReasonId(
      @RequestParam("employmentExitReasonId") @Positive Long employmentExitReasonId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "employmentExitReasonId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.EMPLOYMENT_EXIT_FACT_SORT_FIELDS,
        "employmentExitEventId", "employmentExitEventId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(employmentExitFactService.getEmploymentExitsByEmploymentExitReasonId(employmentExitReasonId, pageable)));
  }

  @GetMapping(value = "/employment-exits", params = {
      "separationNatureId", "!traineeId", "!employmentId", "!employmentExitReasonId",
      "!isVoluntaryFlag", "!isInvoluntaryFlag"
  })
  public ResponseEntity<ApiResponse<PagedResponse<EmploymentExitFactResponse>>> getEmploymentExitsBySeparationNatureId(
      @RequestParam("separationNatureId") @Positive Long separationNatureId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "separationNatureId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.EMPLOYMENT_EXIT_FACT_SORT_FIELDS,
        "employmentExitEventId", "employmentExitEventId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(employmentExitFactService.getEmploymentExitsBySeparationNatureId(separationNatureId, pageable)));
  }

  @GetMapping(value = "/employment-exits", params = {
      "isVoluntaryFlag", "!traineeId", "!employmentId", "!employmentExitReasonId",
      "!separationNatureId", "!isInvoluntaryFlag"
  })
  public ResponseEntity<ApiResponse<PagedResponse<EmploymentExitFactResponse>>> getEmploymentExitsByIsVoluntaryFlag(
      @RequestParam("isVoluntaryFlag") Boolean isVoluntaryFlag,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "isVoluntaryFlag");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.EMPLOYMENT_EXIT_FACT_SORT_FIELDS,
        "employmentExitEventId", "employmentExitEventId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(employmentExitFactService.getEmploymentExitsByIsVoluntaryFlag(isVoluntaryFlag, pageable)));
  }

  @GetMapping(value = "/employment-exits", params = {
      "isInvoluntaryFlag", "!traineeId", "!employmentId", "!employmentExitReasonId",
      "!separationNatureId", "!isVoluntaryFlag"
  })
  public ResponseEntity<ApiResponse<PagedResponse<EmploymentExitFactResponse>>> getEmploymentExitsByIsInvoluntaryFlag(
      @RequestParam("isInvoluntaryFlag") Boolean isInvoluntaryFlag,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "isInvoluntaryFlag");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.EMPLOYMENT_EXIT_FACT_SORT_FIELDS,
        "employmentExitEventId", "employmentExitEventId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(employmentExitFactService.getEmploymentExitsByIsInvoluntaryFlag(isInvoluntaryFlag, pageable)));
  }

  // =========================================================================
  // 2. Employment Facts (/employments)
  // =========================================================================

  @GetMapping(value = "/employments", params = {
      "!employmentNumber", "!traineeId", "!employerId", "!enrollmentId",
      "!placementId", "!jobRoleId", "!isCurrent", "!engagementTypeId", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<EmploymentFactResponse>>> getAllEmployments(
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validatePaginationParametersOnly(httpRequest);
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.EMPLOYMENT_FACT_SORT_FIELDS,
        "employmentId", "employmentId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(employmentFactService.getAllEmployments(pageable)));
  }

  @GetMapping("/employments/{id}")
  public ResponseEntity<ApiResponse<EmploymentFactResponse>> getEmploymentById(
      @PathVariable @Positive Long id) {
    scopeValidator.validateNationwideScope();
    return ResponseEntity.ok(ApiResponse.ok(employmentFactService.getEmploymentById(id)));
  }

  @GetMapping(value = "/employments", params = {
      "employmentNumber", "!traineeId", "!employerId", "!enrollmentId",
      "!placementId", "!jobRoleId", "!isCurrent", "!engagementTypeId", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<EmploymentFactResponse>> getEmploymentByEmploymentNumber(
      @RequestParam("employmentNumber") @NotBlank String employmentNumber,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateOnlyQueryParameter(httpRequest, "employmentNumber");
    return ResponseEntity.ok(ApiResponse.ok(employmentFactService.getEmploymentByEmploymentNumber(employmentNumber)));
  }

  @GetMapping(value = "/employments", params = {
      "traineeId", "!employmentNumber", "!employerId", "!enrollmentId",
      "!placementId", "!jobRoleId", "!isCurrent", "!engagementTypeId", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<EmploymentFactResponse>>> getEmploymentsByTraineeId(
      @RequestParam("traineeId") @Positive Long traineeId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "traineeId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.EMPLOYMENT_FACT_SORT_FIELDS,
        "employmentId", "employmentId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(employmentFactService.getEmploymentsByTraineeId(traineeId, pageable)));
  }

  @GetMapping(value = "/employments", params = {
      "employerId", "!employmentNumber", "!traineeId", "!enrollmentId",
      "!placementId", "!jobRoleId", "!isCurrent", "!engagementTypeId", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<EmploymentFactResponse>>> getEmploymentsByEmployerId(
      @RequestParam("employerId") @Positive Long employerId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "employerId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.EMPLOYMENT_FACT_SORT_FIELDS,
        "employmentId", "employmentId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(employmentFactService.getEmploymentsByEmployerId(employerId, pageable)));
  }

  @GetMapping(value = "/employments", params = {
      "enrollmentId", "!employmentNumber", "!traineeId", "!employerId",
      "!placementId", "!jobRoleId", "!isCurrent", "!engagementTypeId", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<EmploymentFactResponse>>> getEmploymentsByEnrollmentId(
      @RequestParam("enrollmentId") @Positive Long enrollmentId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "enrollmentId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.EMPLOYMENT_FACT_SORT_FIELDS,
        "employmentId", "employmentId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(employmentFactService.getEmploymentsByEnrollmentId(enrollmentId, pageable)));
  }

  @GetMapping(value = "/employments", params = {
      "placementId", "!employmentNumber", "!traineeId", "!employerId",
      "!enrollmentId", "!jobRoleId", "!isCurrent", "!engagementTypeId", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<EmploymentFactResponse>>> getEmploymentsByPlacementId(
      @RequestParam("placementId") @Positive Long placementId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "placementId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.EMPLOYMENT_FACT_SORT_FIELDS,
        "employmentId", "employmentId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(employmentFactService.getEmploymentsByPlacementId(placementId, pageable)));
  }

  @GetMapping(value = "/employments", params = {
      "jobRoleId", "!employmentNumber", "!traineeId", "!employerId",
      "!enrollmentId", "!placementId", "!isCurrent", "!engagementTypeId", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<EmploymentFactResponse>>> getEmploymentsByJobRoleId(
      @RequestParam("jobRoleId") @Positive Long jobRoleId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "jobRoleId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.EMPLOYMENT_FACT_SORT_FIELDS,
        "employmentId", "employmentId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(employmentFactService.getEmploymentsByJobRoleId(jobRoleId, pageable)));
  }

  @GetMapping(value = "/employments", params = {
      "isCurrent", "!employmentNumber", "!traineeId", "!employerId",
      "!enrollmentId", "!placementId", "!jobRoleId", "!engagementTypeId", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<EmploymentFactResponse>>> getCurrentEmployments(
      @RequestParam("isCurrent") boolean isCurrent,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "isCurrent");
    if (!isCurrent) {
      throw new BadRequestException("Only isCurrent=true is supported for current employments filter", "INVALID_FILTER_PARAMETER");
    }
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.EMPLOYMENT_FACT_SORT_FIELDS,
        "employmentId", "employmentId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(employmentFactService.getCurrentEmployments(pageable)));
  }

  @GetMapping(value = "/employments", params = {
      "engagementTypeId", "!employmentNumber", "!traineeId", "!employerId",
      "!enrollmentId", "!placementId", "!jobRoleId", "!isCurrent", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<EmploymentFactResponse>>> getEmploymentsByEngagementTypeId(
      @RequestParam("engagementTypeId") @Positive Long engagementTypeId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "engagementTypeId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.EMPLOYMENT_FACT_SORT_FIELDS,
        "employmentId", "employmentId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(employmentFactService.getEmploymentsByEngagementTypeId(engagementTypeId, pageable)));
  }

  @GetMapping(value = "/employments", params = {
      "traineeDistrictId", "!employmentNumber", "!traineeId", "!employerId",
      "!enrollmentId", "!placementId", "!jobRoleId", "!isCurrent", "!engagementTypeId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<EmploymentFactResponse>>> getEmploymentsByTraineeDistrictId(
      @RequestParam("traineeDistrictId") @Positive Long traineeDistrictId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "traineeDistrictId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.EMPLOYMENT_FACT_SORT_FIELDS,
        "employmentId", "employmentId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(employmentFactService.getEmploymentsByTraineeDistrictId(traineeDistrictId, pageable)));
  }

  // =========================================================================
  // 3. Employment Retention Facts (/employment-retentions)
  // =========================================================================

  @GetMapping(value = "/employment-retentions", params = {
      "!traineeId", "!enrollmentId", "!engagementTypeId", "!isCurrent"
  })
  public ResponseEntity<ApiResponse<PagedResponse<EmploymentRetentionFactResponse>>> getAllEmploymentRetentions(
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validatePaginationParametersOnly(httpRequest);
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.EMPLOYMENT_RETENTION_FACT_SORT_FIELDS,
        "employmentId", "employmentId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(employmentRetentionFactService.getAllEmploymentRetentions(pageable)));
  }

  @GetMapping("/employment-retentions/{id}")
  public ResponseEntity<ApiResponse<EmploymentRetentionFactResponse>> getEmploymentRetentionById(
      @PathVariable @Positive Long id) {
    scopeValidator.validateNationwideScope();
    return ResponseEntity.ok(ApiResponse.ok(employmentRetentionFactService.getEmploymentRetentionById(id)));
  }

  @GetMapping(value = "/employment-retentions", params = {
      "traineeId", "!enrollmentId", "!engagementTypeId", "!isCurrent"
  })
  public ResponseEntity<ApiResponse<PagedResponse<EmploymentRetentionFactResponse>>> getEmploymentRetentionsByTraineeId(
      @RequestParam("traineeId") @Positive Long traineeId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "traineeId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.EMPLOYMENT_RETENTION_FACT_SORT_FIELDS,
        "employmentId", "employmentId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(employmentRetentionFactService.getEmploymentRetentionsByTraineeId(traineeId, pageable)));
  }

  @GetMapping(value = "/employment-retentions", params = {
      "enrollmentId", "!traineeId", "!engagementTypeId", "!isCurrent"
  })
  public ResponseEntity<ApiResponse<PagedResponse<EmploymentRetentionFactResponse>>> getEmploymentRetentionsByEnrollmentId(
      @RequestParam("enrollmentId") @Positive Long enrollmentId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "enrollmentId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.EMPLOYMENT_RETENTION_FACT_SORT_FIELDS,
        "employmentId", "employmentId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(employmentRetentionFactService.getEmploymentRetentionsByEnrollmentId(enrollmentId, pageable)));
  }

  @GetMapping(value = "/employment-retentions", params = {
      "engagementTypeId", "!traineeId", "!enrollmentId", "!isCurrent"
  })
  public ResponseEntity<ApiResponse<PagedResponse<EmploymentRetentionFactResponse>>> getEmploymentRetentionsByEngagementTypeId(
      @RequestParam("engagementTypeId") @Positive Long engagementTypeId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "engagementTypeId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.EMPLOYMENT_RETENTION_FACT_SORT_FIELDS,
        "employmentId", "employmentId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(employmentRetentionFactService.getEmploymentRetentionsByEngagementTypeId(engagementTypeId, pageable)));
  }

  @GetMapping(value = "/employment-retentions", params = {
      "isCurrent", "!traineeId", "!enrollmentId", "!engagementTypeId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<EmploymentRetentionFactResponse>>> getCurrentEmploymentRetentions(
      @RequestParam("isCurrent") boolean isCurrent,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "isCurrent");
    if (!isCurrent) {
      throw new BadRequestException("Only isCurrent=true is supported for current employment retentions filter", "INVALID_FILTER_PARAMETER");
    }
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.EMPLOYMENT_RETENTION_FACT_SORT_FIELDS,
        "employmentId", "employmentId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(employmentRetentionFactService.getCurrentEmploymentRetentions(pageable)));
  }

  // =========================================================================
  // 4. Employment Verification Facts (/employment-verifications)
  // =========================================================================

  @GetMapping(value = "/employment-verifications", params = {
      "!verificationNumber", "!traineeId", "!employmentId", "!placementId",
      "!employerId", "!statusId", "!methodId", "!isCurrent", "!isVerifiedFlag"
  })
  public ResponseEntity<ApiResponse<PagedResponse<EmploymentVerificationFactResponse>>> getAllEmploymentVerifications(
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validatePaginationParametersOnly(httpRequest);
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.EMPLOYMENT_VERIFICATION_FACT_SORT_FIELDS,
        "employmentVerificationId", "employmentVerificationId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(employmentVerificationFactService.getAllEmploymentVerifications(pageable)));
  }

  @GetMapping("/employment-verifications/{id}")
  public ResponseEntity<ApiResponse<EmploymentVerificationFactResponse>> getEmploymentVerificationById(
      @PathVariable @Positive Long id) {
    scopeValidator.validateNationwideScope();
    return ResponseEntity.ok(ApiResponse.ok(employmentVerificationFactService.getEmploymentVerificationById(id)));
  }

  @GetMapping(value = "/employment-verifications", params = {
      "verificationNumber", "!traineeId", "!employmentId", "!placementId",
      "!employerId", "!statusId", "!methodId", "!isCurrent", "!isVerifiedFlag"
  })
  public ResponseEntity<ApiResponse<EmploymentVerificationFactResponse>> getEmploymentVerificationByNumber(
      @RequestParam("verificationNumber") @NotBlank String verificationNumber,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateOnlyQueryParameter(httpRequest, "verificationNumber");
    return ResponseEntity.ok(ApiResponse.ok(
        employmentVerificationFactService.getEmploymentVerificationByNumber(verificationNumber)));
  }

  @GetMapping(value = "/employment-verifications", params = {
      "traineeId", "!verificationNumber", "!employmentId", "!placementId",
      "!employerId", "!statusId", "!methodId", "!isCurrent", "!isVerifiedFlag"
  })
  public ResponseEntity<ApiResponse<PagedResponse<EmploymentVerificationFactResponse>>> getEmploymentVerificationsByTraineeId(
      @RequestParam("traineeId") @Positive Long traineeId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "traineeId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.EMPLOYMENT_VERIFICATION_FACT_SORT_FIELDS,
        "employmentVerificationId", "employmentVerificationId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(employmentVerificationFactService.getEmploymentVerificationsByTraineeId(traineeId, pageable)));
  }

  @GetMapping(value = "/employment-verifications", params = {
      "employmentId", "!verificationNumber", "!traineeId", "!placementId",
      "!employerId", "!statusId", "!methodId", "!isCurrent", "!isVerifiedFlag"
  })
  public ResponseEntity<ApiResponse<PagedResponse<EmploymentVerificationFactResponse>>> getEmploymentVerificationsByEmploymentId(
      @RequestParam("employmentId") @Positive Long employmentId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "employmentId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.EMPLOYMENT_VERIFICATION_FACT_SORT_FIELDS,
        "employmentVerificationId", "employmentVerificationId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(employmentVerificationFactService.getEmploymentVerificationsByEmploymentId(employmentId, pageable)));
  }

  @GetMapping(value = "/employment-verifications", params = {
      "placementId", "!verificationNumber", "!traineeId", "!employmentId",
      "!employerId", "!statusId", "!methodId", "!isCurrent", "!isVerifiedFlag"
  })
  public ResponseEntity<ApiResponse<PagedResponse<EmploymentVerificationFactResponse>>> getEmploymentVerificationsByPlacementId(
      @RequestParam("placementId") @Positive Long placementId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "placementId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.EMPLOYMENT_VERIFICATION_FACT_SORT_FIELDS,
        "employmentVerificationId", "employmentVerificationId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(employmentVerificationFactService.getEmploymentVerificationsByPlacementId(placementId, pageable)));
  }

  @GetMapping(value = "/employment-verifications", params = {
      "employerId", "!verificationNumber", "!traineeId", "!employmentId",
      "!placementId", "!statusId", "!methodId", "!isCurrent", "!isVerifiedFlag"
  })
  public ResponseEntity<ApiResponse<PagedResponse<EmploymentVerificationFactResponse>>> getEmploymentVerificationsByEmployerId(
      @RequestParam("employerId") @Positive Long employerId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "employerId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.EMPLOYMENT_VERIFICATION_FACT_SORT_FIELDS,
        "employmentVerificationId", "employmentVerificationId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(employmentVerificationFactService.getEmploymentVerificationsByEmployerId(employerId, pageable)));
  }

  @GetMapping(value = "/employment-verifications", params = {
      "statusId", "!verificationNumber", "!traineeId", "!employmentId",
      "!placementId", "!employerId", "!methodId", "!isCurrent", "!isVerifiedFlag"
  })
  public ResponseEntity<ApiResponse<PagedResponse<EmploymentVerificationFactResponse>>> getEmploymentVerificationsByStatusId(
      @RequestParam("statusId") @Positive Long statusId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "statusId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.EMPLOYMENT_VERIFICATION_FACT_SORT_FIELDS,
        "employmentVerificationId", "employmentVerificationId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(employmentVerificationFactService.getEmploymentVerificationsByStatusId(statusId, pageable)));
  }

  @GetMapping(value = "/employment-verifications", params = {
      "methodId", "!verificationNumber", "!traineeId", "!employmentId",
      "!placementId", "!employerId", "!statusId", "!isCurrent", "!isVerifiedFlag"
  })
  public ResponseEntity<ApiResponse<PagedResponse<EmploymentVerificationFactResponse>>> getEmploymentVerificationsByMethodId(
      @RequestParam("methodId") @Positive Long methodId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "methodId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.EMPLOYMENT_VERIFICATION_FACT_SORT_FIELDS,
        "employmentVerificationId", "employmentVerificationId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(employmentVerificationFactService.getEmploymentVerificationsByMethodId(methodId, pageable)));
  }

  @GetMapping(value = "/employment-verifications", params = {
      "isCurrent", "!verificationNumber", "!traineeId", "!employmentId",
      "!placementId", "!employerId", "!statusId", "!methodId", "!isVerifiedFlag"
  })
  public ResponseEntity<ApiResponse<PagedResponse<EmploymentVerificationFactResponse>>> getCurrentEmploymentVerifications(
      @RequestParam("isCurrent") boolean isCurrent,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "isCurrent");
    if (!isCurrent) {
      throw new BadRequestException("Only isCurrent=true is supported for current employment verifications filter", "INVALID_FILTER_PARAMETER");
    }
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.EMPLOYMENT_VERIFICATION_FACT_SORT_FIELDS,
        "employmentVerificationId", "employmentVerificationId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(employmentVerificationFactService.getCurrentEmploymentVerifications(pageable)));
  }

  @GetMapping(value = "/employment-verifications", params = {
      "isVerifiedFlag", "!verificationNumber", "!traineeId", "!employmentId",
      "!placementId", "!employerId", "!statusId", "!methodId", "!isCurrent"
  })
  public ResponseEntity<ApiResponse<PagedResponse<EmploymentVerificationFactResponse>>> getEmploymentVerificationsByIsVerifiedFlag(
      @RequestParam("isVerifiedFlag") Boolean isVerifiedFlag,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "isVerifiedFlag");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.EMPLOYMENT_VERIFICATION_FACT_SORT_FIELDS,
        "employmentVerificationId", "employmentVerificationId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(employmentVerificationFactService.getEmploymentVerificationsByIsVerifiedFlag(isVerifiedFlag, pageable)));
  }

  // =========================================================================
  // 5. Enrollment Outcome Facts (/enrollment-outcomes)
  // =========================================================================

  @GetMapping(value = "/enrollment-outcomes", params = {
      "!enrollmentNumber", "!traineeId", "!programId", "!courseId",
      "!providerId", "!batchId", "!traineeDistrictId", "!traineeStateId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<EnrollmentOutcomeFactResponse>>> getAllEnrollmentOutcomes(
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validatePaginationParametersOnly(httpRequest);
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.ENROLLMENT_OUTCOME_FACT_SORT_FIELDS,
        "enrollmentId", "enrollmentId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(enrollmentOutcomeFactService.getAllEnrollmentOutcomes(pageable)));
  }

  @GetMapping("/enrollment-outcomes/{id}")
  public ResponseEntity<ApiResponse<EnrollmentOutcomeFactResponse>> getEnrollmentOutcomeById(
      @PathVariable @Positive Long id) {
    scopeValidator.validateNationwideScope();
    return ResponseEntity.ok(ApiResponse.ok(enrollmentOutcomeFactService.getEnrollmentOutcomeById(id)));
  }

  @GetMapping(value = "/enrollment-outcomes", params = {
      "enrollmentNumber", "!traineeId", "!programId", "!courseId",
      "!providerId", "!batchId", "!traineeDistrictId", "!traineeStateId"
  })
  public ResponseEntity<ApiResponse<EnrollmentOutcomeFactResponse>> getEnrollmentOutcomeByEnrollmentNumber(
      @RequestParam("enrollmentNumber") @NotBlank String enrollmentNumber,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateOnlyQueryParameter(httpRequest, "enrollmentNumber");
    return ResponseEntity.ok(ApiResponse.ok(
        enrollmentOutcomeFactService.getEnrollmentOutcomeByEnrollmentNumber(enrollmentNumber)));
  }

  @GetMapping(value = "/enrollment-outcomes", params = {
      "traineeId", "!enrollmentNumber", "!programId", "!courseId",
      "!providerId", "!batchId", "!traineeDistrictId", "!traineeStateId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<EnrollmentOutcomeFactResponse>>> getEnrollmentOutcomesByTraineeId(
      @RequestParam("traineeId") @Positive Long traineeId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "traineeId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.ENROLLMENT_OUTCOME_FACT_SORT_FIELDS,
        "enrollmentId", "enrollmentId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(enrollmentOutcomeFactService.getEnrollmentOutcomesByTraineeId(traineeId, pageable)));
  }

  @GetMapping(value = "/enrollment-outcomes", params = {
      "programId", "!enrollmentNumber", "!traineeId", "!courseId",
      "!providerId", "!batchId", "!traineeDistrictId", "!traineeStateId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<EnrollmentOutcomeFactResponse>>> getEnrollmentOutcomesByProgramId(
      @RequestParam("programId") @Positive Long programId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "programId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.ENROLLMENT_OUTCOME_FACT_SORT_FIELDS,
        "enrollmentId", "enrollmentId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(enrollmentOutcomeFactService.getEnrollmentOutcomesByProgramId(programId, pageable)));
  }

  @GetMapping(value = "/enrollment-outcomes", params = {
      "courseId", "!enrollmentNumber", "!traineeId", "!programId",
      "!providerId", "!batchId", "!traineeDistrictId", "!traineeStateId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<EnrollmentOutcomeFactResponse>>> getEnrollmentOutcomesByCourseId(
      @RequestParam("courseId") @Positive Long courseId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "courseId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.ENROLLMENT_OUTCOME_FACT_SORT_FIELDS,
        "enrollmentId", "enrollmentId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(enrollmentOutcomeFactService.getEnrollmentOutcomesByCourseId(courseId, pageable)));
  }

  @GetMapping(value = "/enrollment-outcomes", params = {
      "providerId", "!enrollmentNumber", "!traineeId", "!programId",
      "!courseId", "!batchId", "!traineeDistrictId", "!traineeStateId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<EnrollmentOutcomeFactResponse>>> getEnrollmentOutcomesByProviderId(
      @RequestParam("providerId") @Positive Long providerId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "providerId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.ENROLLMENT_OUTCOME_FACT_SORT_FIELDS,
        "enrollmentId", "enrollmentId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(enrollmentOutcomeFactService.getEnrollmentOutcomesByProviderId(providerId, pageable)));
  }

  @GetMapping(value = "/enrollment-outcomes", params = {
      "batchId", "!enrollmentNumber", "!traineeId", "!programId",
      "!courseId", "!providerId", "!traineeDistrictId", "!traineeStateId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<EnrollmentOutcomeFactResponse>>> getEnrollmentOutcomesByBatchId(
      @RequestParam("batchId") @Positive Long batchId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "batchId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.ENROLLMENT_OUTCOME_FACT_SORT_FIELDS,
        "enrollmentId", "enrollmentId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(enrollmentOutcomeFactService.getEnrollmentOutcomesByBatchId(batchId, pageable)));
  }

  @GetMapping(value = "/enrollment-outcomes", params = {
      "traineeDistrictId", "!enrollmentNumber", "!traineeId", "!programId",
      "!courseId", "!providerId", "!batchId", "!traineeStateId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<EnrollmentOutcomeFactResponse>>> getEnrollmentOutcomesByTraineeDistrictId(
      @RequestParam("traineeDistrictId") @Positive Long traineeDistrictId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "traineeDistrictId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.ENROLLMENT_OUTCOME_FACT_SORT_FIELDS,
        "enrollmentId", "enrollmentId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(enrollmentOutcomeFactService.getEnrollmentOutcomesByTraineeDistrictId(traineeDistrictId, pageable)));
  }

  @GetMapping(value = "/enrollment-outcomes", params = {
      "traineeStateId", "!enrollmentNumber", "!traineeId", "!programId",
      "!courseId", "!providerId", "!batchId", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<EnrollmentOutcomeFactResponse>>> getEnrollmentOutcomesByTraineeStateId(
      @RequestParam("traineeStateId") @Positive Long traineeStateId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "traineeStateId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.ENROLLMENT_OUTCOME_FACT_SORT_FIELDS,
        "enrollmentId", "enrollmentId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(enrollmentOutcomeFactService.getEnrollmentOutcomesByTraineeStateId(traineeStateId, pageable)));
  }

  // =========================================================================
  // 6. Followup Facts (/followups)
  // =========================================================================

  @GetMapping(value = "/followups", params = {
      "!traineeId", "!campaignId", "!typeId", "!typeCode",
      "!statusId", "!outcomeId", "!isOpenFlag"
  })
  public ResponseEntity<ApiResponse<PagedResponse<FollowupFactResponse>>> getAllFollowupFacts(
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validatePaginationParametersOnly(httpRequest);
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.FOLLOWUP_FACT_SORT_FIELDS,
        "followupTaskId", "followupTaskId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(followupFactService.getAllFollowupFacts(pageable)));
  }

  @GetMapping("/followups/{id}")
  public ResponseEntity<ApiResponse<FollowupFactResponse>> getFollowupFactById(
      @PathVariable @Positive Long id) {
    scopeValidator.validateNationwideScope();
    return ResponseEntity.ok(ApiResponse.ok(followupFactService.getFollowupFactById(id)));
  }

  @GetMapping(value = "/followups", params = {
      "traineeId", "!campaignId", "!typeId", "!typeCode",
      "!statusId", "!outcomeId", "!isOpenFlag"
  })
  public ResponseEntity<ApiResponse<PagedResponse<FollowupFactResponse>>> getFollowupFactsByTraineeId(
      @RequestParam("traineeId") @Positive Long traineeId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "traineeId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.FOLLOWUP_FACT_SORT_FIELDS,
        "followupTaskId", "followupTaskId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(followupFactService.getFollowupFactsByTraineeId(traineeId, pageable)));
  }

  @GetMapping(value = "/followups", params = {
      "campaignId", "!traineeId", "!typeId", "!typeCode",
      "!statusId", "!outcomeId", "!isOpenFlag"
  })
  public ResponseEntity<ApiResponse<PagedResponse<FollowupFactResponse>>> getFollowupFactsByCampaignId(
      @RequestParam("campaignId") @Positive Long campaignId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "campaignId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.FOLLOWUP_FACT_SORT_FIELDS,
        "followupTaskId", "followupTaskId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(followupFactService.getFollowupFactsByCampaignId(campaignId, pageable)));
  }

  @GetMapping(value = "/followups", params = {
      "typeId", "!traineeId", "!campaignId", "!typeCode",
      "!statusId", "!outcomeId", "!isOpenFlag"
  })
  public ResponseEntity<ApiResponse<PagedResponse<FollowupFactResponse>>> getFollowupFactsByTypeId(
      @RequestParam("typeId") @Positive Long typeId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "typeId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.FOLLOWUP_FACT_SORT_FIELDS,
        "followupTaskId", "followupTaskId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(followupFactService.getFollowupFactsByTypeId(typeId, pageable)));
  }

  @GetMapping(value = "/followups", params = {
      "typeCode", "!traineeId", "!campaignId", "!typeId",
      "!statusId", "!outcomeId", "!isOpenFlag"
  })
  public ResponseEntity<ApiResponse<PagedResponse<FollowupFactResponse>>> getFollowupFactsByTypeCode(
      @RequestParam("typeCode") @NotBlank String typeCode,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "typeCode");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.FOLLOWUP_FACT_SORT_FIELDS,
        "followupTaskId", "followupTaskId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(followupFactService.getFollowupFactsByTypeCode(typeCode, pageable)));
  }

  @GetMapping(value = "/followups", params = {
      "statusId", "!traineeId", "!campaignId", "!typeId",
      "!typeCode", "!outcomeId", "!isOpenFlag"
  })
  public ResponseEntity<ApiResponse<PagedResponse<FollowupFactResponse>>> getFollowupFactsByStatusId(
      @RequestParam("statusId") @Positive Long statusId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "statusId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.FOLLOWUP_FACT_SORT_FIELDS,
        "followupTaskId", "followupTaskId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(followupFactService.getFollowupFactsByStatusId(statusId, pageable)));
  }

  @GetMapping(value = "/followups", params = {
      "outcomeId", "!traineeId", "!campaignId", "!typeId",
      "!typeCode", "!statusId", "!isOpenFlag"
  })
  public ResponseEntity<ApiResponse<PagedResponse<FollowupFactResponse>>> getFollowupFactsByOutcomeId(
      @RequestParam("outcomeId") @Positive Long outcomeId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "outcomeId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.FOLLOWUP_FACT_SORT_FIELDS,
        "followupTaskId", "followupTaskId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(followupFactService.getFollowupFactsByOutcomeId(outcomeId, pageable)));
  }

  @GetMapping(value = "/followups", params = {
      "isOpenFlag", "!traineeId", "!campaignId", "!typeId",
      "!typeCode", "!statusId", "!outcomeId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<FollowupFactResponse>>> getFollowupFactsByIsOpenFlag(
      @RequestParam("isOpenFlag") Boolean isOpenFlag,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "isOpenFlag");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.FOLLOWUP_FACT_SORT_FIELDS,
        "followupTaskId", "followupTaskId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(followupFactService.getFollowupFactsByIsOpenFlag(isOpenFlag, pageable)));
  }

  // =========================================================================
  // 7. Placement Facts (/placements)
  // =========================================================================

  @GetMapping(value = "/placements", params = {
      "!placementNumber", "!traineeId", "!enrollmentId", "!employerId",
      "!courseId", "!programId", "!providerId", "!jobRoleId", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<PlacementFactResponse>>> getAllPlacements(
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validatePaginationParametersOnly(httpRequest);
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.PLACEMENT_FACT_SORT_FIELDS,
        "placementId", "placementId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(placementFactService.getAllPlacements(pageable)));
  }

  @GetMapping("/placements/{id}")
  public ResponseEntity<ApiResponse<PlacementFactResponse>> getPlacementById(
      @PathVariable @Positive Long id) {
    scopeValidator.validateNationwideScope();
    return ResponseEntity.ok(ApiResponse.ok(placementFactService.getPlacementById(id)));
  }

  @GetMapping(value = "/placements", params = {
      "placementNumber", "!traineeId", "!enrollmentId", "!employerId",
      "!courseId", "!programId", "!providerId", "!jobRoleId", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<PlacementFactResponse>> getPlacementByPlacementNumber(
      @RequestParam("placementNumber") @NotBlank String placementNumber,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateOnlyQueryParameter(httpRequest, "placementNumber");
    return ResponseEntity.ok(ApiResponse.ok(placementFactService.getPlacementByPlacementNumber(placementNumber)));
  }

  @GetMapping(value = "/placements", params = {
      "traineeId", "!placementNumber", "!enrollmentId", "!employerId",
      "!courseId", "!programId", "!providerId", "!jobRoleId", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<PlacementFactResponse>>> getPlacementsByTraineeId(
      @RequestParam("traineeId") @Positive Long traineeId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "traineeId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.PLACEMENT_FACT_SORT_FIELDS,
        "placementId", "placementId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(placementFactService.getPlacementsByTraineeId(traineeId, pageable)));
  }

  @GetMapping(value = "/placements", params = {
      "enrollmentId", "!placementNumber", "!traineeId", "!employerId",
      "!courseId", "!programId", "!providerId", "!jobRoleId", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<PlacementFactResponse>>> getPlacementsByEnrollmentId(
      @RequestParam("enrollmentId") @Positive Long enrollmentId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "enrollmentId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.PLACEMENT_FACT_SORT_FIELDS,
        "placementId", "placementId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(placementFactService.getPlacementsByEnrollmentId(enrollmentId, pageable)));
  }

  @GetMapping(value = "/placements", params = {
      "employerId", "!placementNumber", "!traineeId", "!enrollmentId",
      "!courseId", "!programId", "!providerId", "!jobRoleId", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<PlacementFactResponse>>> getPlacementsByEmployerId(
      @RequestParam("employerId") @Positive Long employerId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "employerId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.PLACEMENT_FACT_SORT_FIELDS,
        "placementId", "placementId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(placementFactService.getPlacementsByEmployerId(employerId, pageable)));
  }

  @GetMapping(value = "/placements", params = {
      "courseId", "!placementNumber", "!traineeId", "!enrollmentId",
      "!employerId", "!programId", "!providerId", "!jobRoleId", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<PlacementFactResponse>>> getPlacementsByCourseId(
      @RequestParam("courseId") @Positive Long courseId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "courseId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.PLACEMENT_FACT_SORT_FIELDS,
        "placementId", "placementId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(placementFactService.getPlacementsByCourseId(courseId, pageable)));
  }

  @GetMapping(value = "/placements", params = {
      "programId", "!placementNumber", "!traineeId", "!enrollmentId",
      "!employerId", "!courseId", "!providerId", "!jobRoleId", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<PlacementFactResponse>>> getPlacementsByProgramId(
      @RequestParam("programId") @Positive Long programId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "programId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.PLACEMENT_FACT_SORT_FIELDS,
        "placementId", "placementId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(placementFactService.getPlacementsByProgramId(programId, pageable)));
  }

  @GetMapping(value = "/placements", params = {
      "providerId", "!placementNumber", "!traineeId", "!enrollmentId",
      "!employerId", "!courseId", "!programId", "!jobRoleId", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<PlacementFactResponse>>> getPlacementsByProviderId(
      @RequestParam("providerId") @Positive Long providerId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "providerId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.PLACEMENT_FACT_SORT_FIELDS,
        "placementId", "placementId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(placementFactService.getPlacementsByProviderId(providerId, pageable)));
  }

  @GetMapping(value = "/placements", params = {
      "jobRoleId", "!placementNumber", "!traineeId", "!enrollmentId",
      "!employerId", "!courseId", "!programId", "!providerId", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<PlacementFactResponse>>> getPlacementsByJobRoleId(
      @RequestParam("jobRoleId") @Positive Long jobRoleId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "jobRoleId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.PLACEMENT_FACT_SORT_FIELDS,
        "placementId", "placementId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(placementFactService.getPlacementsByJobRoleId(jobRoleId, pageable)));
  }

  @GetMapping(value = "/placements", params = {
      "traineeDistrictId", "!placementNumber", "!traineeId", "!enrollmentId",
      "!employerId", "!courseId", "!programId", "!providerId", "!jobRoleId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<PlacementFactResponse>>> getPlacementsByTraineeDistrictId(
      @RequestParam("traineeDistrictId") @Positive Long traineeDistrictId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "traineeDistrictId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.PLACEMENT_FACT_SORT_FIELDS,
        "placementId", "placementId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(placementFactService.getPlacementsByTraineeDistrictId(traineeDistrictId, pageable)));
  }

  // =========================================================================
  // 8. Salary Progression Facts (/salary-progressions)
  // =========================================================================

  @GetMapping(value = "/salary-progressions", params = {
      "!traineeId", "!enrollmentId", "!courseId", "!providerId", "!engagementTypeId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<SalaryProgressionFactResponse>>> getAllSalaryProgressions(
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validatePaginationParametersOnly(httpRequest);
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.SALARY_PROGRESSION_FACT_SORT_FIELDS,
        "employmentId", "employmentId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(salaryProgressionFactService.getAllSalaryProgressions(pageable)));
  }

  @GetMapping("/salary-progressions/{id}")
  public ResponseEntity<ApiResponse<SalaryProgressionFactResponse>> getSalaryProgressionById(
      @PathVariable @Positive Long id) {
    scopeValidator.validateNationwideScope();
    return ResponseEntity.ok(ApiResponse.ok(salaryProgressionFactService.getSalaryProgressionById(id)));
  }

  @GetMapping(value = "/salary-progressions", params = {
      "traineeId", "!enrollmentId", "!courseId", "!providerId", "!engagementTypeId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<SalaryProgressionFactResponse>>> getSalaryProgressionsByTraineeId(
      @RequestParam("traineeId") @Positive Long traineeId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "traineeId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.SALARY_PROGRESSION_FACT_SORT_FIELDS,
        "employmentId", "employmentId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(salaryProgressionFactService.getSalaryProgressionsByTraineeId(traineeId, pageable)));
  }

  @GetMapping(value = "/salary-progressions", params = {
      "enrollmentId", "!traineeId", "!courseId", "!providerId", "!engagementTypeId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<SalaryProgressionFactResponse>>> getSalaryProgressionsByEnrollmentId(
      @RequestParam("enrollmentId") @Positive Long enrollmentId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "enrollmentId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.SALARY_PROGRESSION_FACT_SORT_FIELDS,
        "employmentId", "employmentId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(salaryProgressionFactService.getSalaryProgressionsByEnrollmentId(enrollmentId, pageable)));
  }

  @GetMapping(value = "/salary-progressions", params = {
      "courseId", "!traineeId", "!enrollmentId", "!providerId", "!engagementTypeId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<SalaryProgressionFactResponse>>> getSalaryProgressionsByCourseId(
      @RequestParam("courseId") @Positive Long courseId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "courseId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.SALARY_PROGRESSION_FACT_SORT_FIELDS,
        "employmentId", "employmentId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(salaryProgressionFactService.getSalaryProgressionsByCourseId(courseId, pageable)));
  }

  @GetMapping(value = "/salary-progressions", params = {
      "providerId", "!traineeId", "!enrollmentId", "!courseId", "!engagementTypeId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<SalaryProgressionFactResponse>>> getSalaryProgressionsByProviderId(
      @RequestParam("providerId") @Positive Long providerId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "providerId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.SALARY_PROGRESSION_FACT_SORT_FIELDS,
        "employmentId", "employmentId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(salaryProgressionFactService.getSalaryProgressionsByProviderId(providerId, pageable)));
  }

  @GetMapping(value = "/salary-progressions", params = {
      "engagementTypeId", "!traineeId", "!enrollmentId", "!courseId", "!providerId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<SalaryProgressionFactResponse>>> getSalaryProgressionsByEngagementTypeId(
      @RequestParam("engagementTypeId") @Positive Long engagementTypeId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "engagementTypeId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.SALARY_PROGRESSION_FACT_SORT_FIELDS,
        "employmentId", "employmentId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(salaryProgressionFactService.getSalaryProgressionsByEngagementTypeId(engagementTypeId, pageable)));
  }

  // =========================================================================
  // 9. Skill Gap Facts (/skill-gaps)
  // =========================================================================

  @GetMapping(value = "/skill-gaps", params = {
      "!skillGapNumber", "!traineeId", "!skillId", "!courseId",
      "!jobRoleId", "!severityId", "!statusId", "!isCurrent", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<SkillGapFactResponse>>> getAllSkillGaps(
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validatePaginationParametersOnly(httpRequest);
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.SKILL_GAP_FACT_SORT_FIELDS,
        "skillGapId", "skillGapId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(skillGapFactService.getAllSkillGaps(pageable)));
  }

  @GetMapping("/skill-gaps/{id}")
  public ResponseEntity<ApiResponse<SkillGapFactResponse>> getSkillGapById(
      @PathVariable @Positive Long id) {
    scopeValidator.validateNationwideScope();
    return ResponseEntity.ok(ApiResponse.ok(skillGapFactService.getSkillGapById(id)));
  }

  @GetMapping(value = "/skill-gaps", params = {
      "skillGapNumber", "!traineeId", "!skillId", "!courseId",
      "!jobRoleId", "!severityId", "!statusId", "!isCurrent", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<SkillGapFactResponse>> getSkillGapByNumber(
      @RequestParam("skillGapNumber") @NotBlank String skillGapNumber,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateOnlyQueryParameter(httpRequest, "skillGapNumber");
    return ResponseEntity.ok(ApiResponse.ok(skillGapFactService.getSkillGapByNumber(skillGapNumber)));
  }

  @GetMapping(value = "/skill-gaps", params = {
      "traineeId", "!skillGapNumber", "!skillId", "!courseId",
      "!jobRoleId", "!severityId", "!statusId", "!isCurrent", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<SkillGapFactResponse>>> getSkillGapsByTraineeId(
      @RequestParam("traineeId") @Positive Long traineeId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "traineeId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.SKILL_GAP_FACT_SORT_FIELDS,
        "skillGapId", "skillGapId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(skillGapFactService.getSkillGapsByTraineeId(traineeId, pageable)));
  }

  @GetMapping(value = "/skill-gaps", params = {
      "skillId", "!skillGapNumber", "!traineeId", "!courseId",
      "!jobRoleId", "!severityId", "!statusId", "!isCurrent", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<SkillGapFactResponse>>> getSkillGapsBySkillId(
      @RequestParam("skillId") @Positive Long skillId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "skillId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.SKILL_GAP_FACT_SORT_FIELDS,
        "skillGapId", "skillGapId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(skillGapFactService.getSkillGapsBySkillId(skillId, pageable)));
  }

  @GetMapping(value = "/skill-gaps", params = {
      "courseId", "!skillGapNumber", "!traineeId", "!skillId",
      "!jobRoleId", "!severityId", "!statusId", "!isCurrent", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<SkillGapFactResponse>>> getSkillGapsByCourseId(
      @RequestParam("courseId") @Positive Long courseId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "courseId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.SKILL_GAP_FACT_SORT_FIELDS,
        "skillGapId", "skillGapId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(skillGapFactService.getSkillGapsByCourseId(courseId, pageable)));
  }

  @GetMapping(value = "/skill-gaps", params = {
      "jobRoleId", "!skillGapNumber", "!traineeId", "!skillId",
      "!courseId", "!severityId", "!statusId", "!isCurrent", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<SkillGapFactResponse>>> getSkillGapsByJobRoleId(
      @RequestParam("jobRoleId") @Positive Long jobRoleId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "jobRoleId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.SKILL_GAP_FACT_SORT_FIELDS,
        "skillGapId", "skillGapId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(skillGapFactService.getSkillGapsByJobRoleId(jobRoleId, pageable)));
  }

  @GetMapping(value = "/skill-gaps", params = {
      "severityId", "!skillGapNumber", "!traineeId", "!skillId",
      "!courseId", "!jobRoleId", "!statusId", "!isCurrent", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<SkillGapFactResponse>>> getSkillGapsBySeverityId(
      @RequestParam("severityId") @Positive Long severityId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "severityId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.SKILL_GAP_FACT_SORT_FIELDS,
        "skillGapId", "skillGapId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(skillGapFactService.getSkillGapsBySeverityId(severityId, pageable)));
  }

  @GetMapping(value = "/skill-gaps", params = {
      "statusId", "!skillGapNumber", "!traineeId", "!skillId",
      "!courseId", "!jobRoleId", "!severityId", "!isCurrent", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<SkillGapFactResponse>>> getSkillGapsByStatusId(
      @RequestParam("statusId") @Positive Long statusId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "statusId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.SKILL_GAP_FACT_SORT_FIELDS,
        "skillGapId", "skillGapId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(skillGapFactService.getSkillGapsByStatusId(statusId, pageable)));
  }

  @GetMapping(value = "/skill-gaps", params = {
      "isCurrent", "!skillGapNumber", "!traineeId", "!skillId",
      "!courseId", "!jobRoleId", "!severityId", "!statusId", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<SkillGapFactResponse>>> getCurrentSkillGaps(
      @RequestParam("isCurrent") boolean isCurrent,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "isCurrent");
    if (!isCurrent) {
      throw new BadRequestException("Only isCurrent=true is supported for current skill gaps filter", "INVALID_FILTER_PARAMETER");
    }
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.SKILL_GAP_FACT_SORT_FIELDS,
        "skillGapId", "skillGapId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(skillGapFactService.getCurrentSkillGaps(pageable)));
  }

  @GetMapping(value = "/skill-gaps", params = {
      "traineeDistrictId", "!skillGapNumber", "!traineeId", "!skillId",
      "!courseId", "!jobRoleId", "!severityId", "!statusId", "!isCurrent"
  })
  public ResponseEntity<ApiResponse<PagedResponse<SkillGapFactResponse>>> getSkillGapsByTraineeDistrictId(
      @RequestParam("traineeDistrictId") @Positive Long traineeDistrictId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "traineeDistrictId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.SKILL_GAP_FACT_SORT_FIELDS,
        "skillGapId", "skillGapId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(skillGapFactService.getSkillGapsByTraineeDistrictId(traineeDistrictId, pageable)));
  }

  // =========================================================================
  // 10. Survey Response Facts (/survey-responses)
  // =========================================================================

  @GetMapping(value = "/survey-responses", params = {
      "!traineeId", "!surveyId", "!surveyPurposeId", "!surveyPurposeCode",
      "!programId", "!courseId", "!batchId", "!enrollmentId", "!followupTaskId", "!isSubmittedFlag"
  })
  public ResponseEntity<ApiResponse<PagedResponse<SurveyResponseFactResponse>>> getAllSurveyResponses(
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validatePaginationParametersOnly(httpRequest);
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.SURVEY_RESPONSE_FACT_SORT_FIELDS,
        "surveyResponseId", "surveyResponseId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(surveyResponseFactService.getAllSurveyResponses(pageable)));
  }

  @GetMapping("/survey-responses/{id}")
  public ResponseEntity<ApiResponse<SurveyResponseFactResponse>> getSurveyResponseById(
      @PathVariable @Positive Long id) {
    scopeValidator.validateNationwideScope();
    return ResponseEntity.ok(ApiResponse.ok(surveyResponseFactService.getSurveyResponseById(id)));
  }

  @GetMapping(value = "/survey-responses", params = {
      "traineeId", "!surveyId", "!surveyPurposeId", "!surveyPurposeCode",
      "!programId", "!courseId", "!batchId", "!enrollmentId", "!followupTaskId", "!isSubmittedFlag"
  })
  public ResponseEntity<ApiResponse<PagedResponse<SurveyResponseFactResponse>>> getSurveyResponsesByTraineeId(
      @RequestParam("traineeId") @Positive Long traineeId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "traineeId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.SURVEY_RESPONSE_FACT_SORT_FIELDS,
        "surveyResponseId", "surveyResponseId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(surveyResponseFactService.getSurveyResponsesByTraineeId(traineeId, pageable)));
  }

  @GetMapping(value = "/survey-responses", params = {
      "surveyId", "!traineeId", "!surveyPurposeId", "!surveyPurposeCode",
      "!programId", "!courseId", "!batchId", "!enrollmentId", "!followupTaskId", "!isSubmittedFlag"
  })
  public ResponseEntity<ApiResponse<PagedResponse<SurveyResponseFactResponse>>> getSurveyResponsesBySurveyId(
      @RequestParam("surveyId") @Positive Long surveyId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "surveyId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.SURVEY_RESPONSE_FACT_SORT_FIELDS,
        "surveyResponseId", "surveyResponseId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(surveyResponseFactService.getSurveyResponsesBySurveyId(surveyId, pageable)));
  }

  @GetMapping(value = "/survey-responses", params = {
      "surveyPurposeId", "!traineeId", "!surveyId", "!surveyPurposeCode",
      "!programId", "!courseId", "!batchId", "!enrollmentId", "!followupTaskId", "!isSubmittedFlag"
  })
  public ResponseEntity<ApiResponse<PagedResponse<SurveyResponseFactResponse>>> getSurveyResponsesBySurveyPurposeId(
      @RequestParam("surveyPurposeId") @Positive Long surveyPurposeId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "surveyPurposeId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.SURVEY_RESPONSE_FACT_SORT_FIELDS,
        "surveyResponseId", "surveyResponseId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(surveyResponseFactService.getSurveyResponsesBySurveyPurposeId(surveyPurposeId, pageable)));
  }

  @GetMapping(value = "/survey-responses", params = {
      "surveyPurposeCode", "!traineeId", "!surveyId", "!surveyPurposeId",
      "!programId", "!courseId", "!batchId", "!enrollmentId", "!followupTaskId", "!isSubmittedFlag"
  })
  public ResponseEntity<ApiResponse<PagedResponse<SurveyResponseFactResponse>>> getSurveyResponsesBySurveyPurposeCode(
      @RequestParam("surveyPurposeCode") @NotBlank String surveyPurposeCode,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "surveyPurposeCode");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.SURVEY_RESPONSE_FACT_SORT_FIELDS,
        "surveyResponseId", "surveyResponseId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(surveyResponseFactService.getSurveyResponsesBySurveyPurposeCode(surveyPurposeCode, pageable)));
  }

  @GetMapping(value = "/survey-responses", params = {
      "programId", "!traineeId", "!surveyId", "!surveyPurposeId",
      "!surveyPurposeCode", "!courseId", "!batchId", "!enrollmentId", "!followupTaskId", "!isSubmittedFlag"
  })
  public ResponseEntity<ApiResponse<PagedResponse<SurveyResponseFactResponse>>> getSurveyResponsesByProgramId(
      @RequestParam("programId") @Positive Long programId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "programId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.SURVEY_RESPONSE_FACT_SORT_FIELDS,
        "surveyResponseId", "surveyResponseId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(surveyResponseFactService.getSurveyResponsesByProgramId(programId, pageable)));
  }

  @GetMapping(value = "/survey-responses", params = {
      "courseId", "!traineeId", "!surveyId", "!surveyPurposeId",
      "!surveyPurposeCode", "!programId", "!batchId", "!enrollmentId", "!followupTaskId", "!isSubmittedFlag"
  })
  public ResponseEntity<ApiResponse<PagedResponse<SurveyResponseFactResponse>>> getSurveyResponsesByCourseId(
      @RequestParam("courseId") @Positive Long courseId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "courseId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.SURVEY_RESPONSE_FACT_SORT_FIELDS,
        "surveyResponseId", "surveyResponseId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(surveyResponseFactService.getSurveyResponsesByCourseId(courseId, pageable)));
  }

  @GetMapping(value = "/survey-responses", params = {
      "batchId", "!traineeId", "!surveyId", "!surveyPurposeId",
      "!surveyPurposeCode", "!programId", "!courseId", "!enrollmentId", "!followupTaskId", "!isSubmittedFlag"
  })
  public ResponseEntity<ApiResponse<PagedResponse<SurveyResponseFactResponse>>> getSurveyResponsesByBatchId(
      @RequestParam("batchId") @Positive Long batchId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "batchId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.SURVEY_RESPONSE_FACT_SORT_FIELDS,
        "surveyResponseId", "surveyResponseId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(surveyResponseFactService.getSurveyResponsesByBatchId(batchId, pageable)));
  }

  @GetMapping(value = "/survey-responses", params = {
      "enrollmentId", "!traineeId", "!surveyId", "!surveyPurposeId",
      "!surveyPurposeCode", "!programId", "!courseId", "!batchId", "!followupTaskId", "!isSubmittedFlag"
  })
  public ResponseEntity<ApiResponse<PagedResponse<SurveyResponseFactResponse>>> getSurveyResponsesByEnrollmentId(
      @RequestParam("enrollmentId") @Positive Long enrollmentId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "enrollmentId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.SURVEY_RESPONSE_FACT_SORT_FIELDS,
        "surveyResponseId", "surveyResponseId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(surveyResponseFactService.getSurveyResponsesByEnrollmentId(enrollmentId, pageable)));
  }

  @GetMapping(value = "/survey-responses", params = {
      "followupTaskId", "!traineeId", "!surveyId", "!surveyPurposeId",
      "!surveyPurposeCode", "!programId", "!courseId", "!batchId", "!enrollmentId", "!isSubmittedFlag"
  })
  public ResponseEntity<ApiResponse<PagedResponse<SurveyResponseFactResponse>>> getSurveyResponsesByFollowupTaskId(
      @RequestParam("followupTaskId") @Positive Long followupTaskId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "followupTaskId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.SURVEY_RESPONSE_FACT_SORT_FIELDS,
        "surveyResponseId", "surveyResponseId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(surveyResponseFactService.getSurveyResponsesByFollowupTaskId(followupTaskId, pageable)));
  }

  @GetMapping(value = "/survey-responses", params = {
      "isSubmittedFlag", "!traineeId", "!surveyId", "!surveyPurposeId",
      "!surveyPurposeCode", "!programId", "!courseId", "!batchId", "!enrollmentId", "!followupTaskId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<SurveyResponseFactResponse>>> getSurveyResponsesByIsSubmittedFlag(
      @RequestParam("isSubmittedFlag") Boolean isSubmittedFlag,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "isSubmittedFlag");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.SURVEY_RESPONSE_FACT_SORT_FIELDS,
        "surveyResponseId", "surveyResponseId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(surveyResponseFactService.getSurveyResponsesByIsSubmittedFlag(isSubmittedFlag, pageable)));
  }

  // =========================================================================
  // 11. Unemployment Facts (/unemployments)
  // =========================================================================

  @GetMapping(value = "/unemployments", params = {
      "!traineeId", "!unemploymentReasonId", "!labourStatusId", "!isCurrent", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<UnemploymentFactResponse>>> getAllUnemploymentFacts(
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validatePaginationParametersOnly(httpRequest);
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.UNEMPLOYMENT_FACT_SORT_FIELDS,
        "unemploymentEventId", "unemploymentEventId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(unemploymentFactService.getAllUnemploymentFacts(pageable)));
  }

  @GetMapping("/unemployments/{id}")
  public ResponseEntity<ApiResponse<UnemploymentFactResponse>> getUnemploymentFactById(
      @PathVariable @Positive Long id) {
    scopeValidator.validateNationwideScope();
    return ResponseEntity.ok(ApiResponse.ok(unemploymentFactService.getUnemploymentFactById(id)));
  }

  @GetMapping(value = "/unemployments", params = {
      "traineeId", "!unemploymentReasonId", "!labourStatusId", "!isCurrent", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<UnemploymentFactResponse>>> getUnemploymentFactsByTraineeId(
      @RequestParam("traineeId") @Positive Long traineeId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "traineeId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.UNEMPLOYMENT_FACT_SORT_FIELDS,
        "unemploymentEventId", "unemploymentEventId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(unemploymentFactService.getUnemploymentFactsByTraineeId(traineeId, pageable)));
  }

  @GetMapping(value = "/unemployments", params = {
      "unemploymentReasonId", "!traineeId", "!labourStatusId", "!isCurrent", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<UnemploymentFactResponse>>> getUnemploymentFactsByUnemploymentReasonId(
      @RequestParam("unemploymentReasonId") @Positive Long unemploymentReasonId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "unemploymentReasonId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.UNEMPLOYMENT_FACT_SORT_FIELDS,
        "unemploymentEventId", "unemploymentEventId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(unemploymentFactService.getUnemploymentFactsByUnemploymentReasonId(unemploymentReasonId, pageable)));
  }

  @GetMapping(value = "/unemployments", params = {
      "labourStatusId", "!traineeId", "!unemploymentReasonId", "!isCurrent", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<UnemploymentFactResponse>>> getUnemploymentFactsByLabourStatusId(
      @RequestParam("labourStatusId") @Positive Long labourStatusId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "labourStatusId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.UNEMPLOYMENT_FACT_SORT_FIELDS,
        "unemploymentEventId", "unemploymentEventId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(unemploymentFactService.getUnemploymentFactsByLabourStatusId(labourStatusId, pageable)));
  }

  @GetMapping(value = "/unemployments", params = {
      "isCurrent", "!traineeId", "!unemploymentReasonId", "!labourStatusId", "!traineeDistrictId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<UnemploymentFactResponse>>> getCurrentUnemploymentFacts(
      @RequestParam("isCurrent") boolean isCurrent,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "isCurrent");
    if (!isCurrent) {
      throw new BadRequestException("Only isCurrent=true is supported for current unemployments filter", "INVALID_FILTER_PARAMETER");
    }
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.UNEMPLOYMENT_FACT_SORT_FIELDS,
        "unemploymentEventId", "unemploymentEventId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(unemploymentFactService.getCurrentUnemploymentFacts(pageable)));
  }

  @GetMapping(value = "/unemployments", params = {
      "traineeDistrictId", "!traineeId", "!unemploymentReasonId", "!labourStatusId", "!isCurrent"
  })
  public ResponseEntity<ApiResponse<PagedResponse<UnemploymentFactResponse>>> getUnemploymentFactsByTraineeDistrictId(
      @RequestParam("traineeDistrictId") @Positive Long traineeDistrictId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "direction", defaultValue = "ASC") String direction,
      HttpServletRequest httpRequest) {
    scopeValidator.validateNationwideScope();
    validateFilteredPaginationParameters(httpRequest, "traineeDistrictId");
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction,
        PaginationUtils.UNEMPLOYMENT_FACT_SORT_FIELDS,
        "unemploymentEventId", "unemploymentEventId",
        Sort.Direction.ASC, Sort.Direction.ASC
    );
    return ResponseEntity.ok(ApiResponse.ok(unemploymentFactService.getUnemploymentFactsByTraineeDistrictId(traineeDistrictId, pageable)));
  }

  // =========================================================================
  // Validation Helpers
  // =========================================================================

  private static final Set<String> ALLOWED_PAGINATION_PARAMS = Set.of("page", "size", "sort", "direction");

  private void validateFilteredPaginationParameters(HttpServletRequest httpRequest, String allowedFilterParam) {
    for (String paramName : httpRequest.getParameterMap().keySet()) {
      if (!paramName.equals(allowedFilterParam) && !ALLOWED_PAGINATION_PARAMS.contains(paramName)) {
        throw new BadRequestException(
            "Unsupported query parameter: " + paramName,
            "UNSUPPORTED_PARAMETER"
        );
      }
    }
  }

  private void validatePaginationParametersOnly(HttpServletRequest httpRequest) {
    for (String paramName : httpRequest.getParameterMap().keySet()) {
      if (!ALLOWED_PAGINATION_PARAMS.contains(paramName)) {
        throw new BadRequestException(
            "Unsupported query parameter: " + paramName,
            "UNSUPPORTED_PARAMETER"
        );
      }
    }
  }

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
