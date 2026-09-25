package in.gov.sih.sih26135.controller.analytics;

import in.gov.sih.sih26135.dto.response.AttritionReasonSummaryResponse;
import in.gov.sih.sih26135.dto.response.CourseOutcomeSummaryResponse;
import in.gov.sih.sih26135.dto.response.DistrictOutcomeSummaryResponse;
import in.gov.sih.sih26135.dto.response.EmployerHiringSummaryResponse;
import in.gov.sih.sih26135.dto.response.FollowupOutcomeSummaryResponse;
import in.gov.sih.sih26135.dto.response.ProgramOutcomeSummaryResponse;
import in.gov.sih.sih26135.dto.response.ProviderOutcomeSummaryResponse;
import in.gov.sih.sih26135.dto.response.SkillGapSummaryResponse;
import in.gov.sih.sih26135.dto.response.TraineeOutcomeSummaryResponse;
import in.gov.sih.sih26135.dto.response.UnemploymentReasonSummaryResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.AttritionReasonSummaryService;
import in.gov.sih.sih26135.service.CourseOutcomeSummaryService;
import in.gov.sih.sih26135.service.DistrictOutcomeSummaryService;
import in.gov.sih.sih26135.service.EmployerHiringSummaryService;
import in.gov.sih.sih26135.service.FollowupOutcomeSummaryService;
import in.gov.sih.sih26135.service.ProgramOutcomeSummaryService;
import in.gov.sih.sih26135.service.ProviderOutcomeSummaryService;
import in.gov.sih.sih26135.service.SkillGapSummaryService;
import in.gov.sih.sih26135.service.TraineeOutcomeSummaryService;
import in.gov.sih.sih26135.service.UnemploymentReasonSummaryService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * REST controller for exposing analytical summary aggregates and leaderboards mapped to database views.
 *
 * <p>Base Route: /api/v1/analytics/summaries
 * Produces: Analytical summary aggregate responses enveloped in ApiResponse
 *
 * <p>Strict Read-Only Semantics:
 * All endpoints are strictly GET. Analytics views are immutable SQL aggregates.
 * No modifications, recalculations, or mutations are permitted.
 */
@RestController
@RequestMapping("/api/v1/analytics/summaries")
@PreAuthorize("hasAuthority('analytics.read')")
public class AnalyticsSummaryController {

  private final AttritionReasonSummaryService attritionReasonSummaryService;
  private final CourseOutcomeSummaryService courseOutcomeSummaryService;
  private final DistrictOutcomeSummaryService districtOutcomeSummaryService;
  private final EmployerHiringSummaryService employerHiringSummaryService;
  private final FollowupOutcomeSummaryService followupOutcomeSummaryService;
  private final ProgramOutcomeSummaryService programOutcomeSummaryService;
  private final ProviderOutcomeSummaryService providerOutcomeSummaryService;
  private final SkillGapSummaryService skillGapSummaryService;
  private final TraineeOutcomeSummaryService traineeOutcomeSummaryService;
  private final UnemploymentReasonSummaryService unemploymentReasonSummaryService;

  public AnalyticsSummaryController(
      AttritionReasonSummaryService attritionReasonSummaryService,
      CourseOutcomeSummaryService courseOutcomeSummaryService,
      DistrictOutcomeSummaryService districtOutcomeSummaryService,
      EmployerHiringSummaryService employerHiringSummaryService,
      FollowupOutcomeSummaryService followupOutcomeSummaryService,
      ProgramOutcomeSummaryService programOutcomeSummaryService,
      ProviderOutcomeSummaryService providerOutcomeSummaryService,
      SkillGapSummaryService skillGapSummaryService,
      TraineeOutcomeSummaryService traineeOutcomeSummaryService,
      UnemploymentReasonSummaryService unemploymentReasonSummaryService) {
    this.attritionReasonSummaryService = attritionReasonSummaryService;
    this.courseOutcomeSummaryService = courseOutcomeSummaryService;
    this.districtOutcomeSummaryService = districtOutcomeSummaryService;
    this.employerHiringSummaryService = employerHiringSummaryService;
    this.followupOutcomeSummaryService = followupOutcomeSummaryService;
    this.programOutcomeSummaryService = programOutcomeSummaryService;
    this.providerOutcomeSummaryService = providerOutcomeSummaryService;
    this.skillGapSummaryService = skillGapSummaryService;
    this.traineeOutcomeSummaryService = traineeOutcomeSummaryService;
    this.unemploymentReasonSummaryService = unemploymentReasonSummaryService;
  }

  // =========================================================================
  // 1. Attrition Reason Summaries (/attrition-reasons)
  // =========================================================================

  @GetMapping(value = "/attrition-reasons", params = {
      "!exitReasonId", "!employmentExitReasonId", "!separationNatureId",
      "!isVoluntaryFlag", "!isInvoluntaryFlag"
  })
  public ResponseEntity<ApiResponse<List<AttritionReasonSummaryResponse>>> getAllAttritionReasonSummaries(
      HttpServletRequest httpRequest) {
    validateNoQueryParameters(httpRequest);
    return ResponseEntity.ok(ApiResponse.ok(attritionReasonSummaryService.getAllAttritionReasonSummaries()));
  }

  @GetMapping("/attrition-reasons/leaderboard")
  public ResponseEntity<ApiResponse<List<AttritionReasonSummaryResponse>>> getAttritionReasonLeaderboard() {
    return ResponseEntity.ok(ApiResponse.ok(
        attritionReasonSummaryService.getAllAttritionReasonSummariesOrderByExitCountDesc()));
  }

  @GetMapping("/attrition-reasons/{exitReasonId}/{separationNatureId}")
  public ResponseEntity<ApiResponse<AttritionReasonSummaryResponse>> getAttritionReasonSummaryById(
      @PathVariable Long exitReasonId,
      @PathVariable Long separationNatureId) {
    return ResponseEntity.ok(ApiResponse.ok(
        attritionReasonSummaryService.getAttritionReasonSummaryById(exitReasonId, separationNatureId)));
  }

  @GetMapping(value = "/attrition-reasons", params = {
      "exitReasonId", "!separationNatureId", "!isVoluntaryFlag",
      "!isInvoluntaryFlag", "!employmentExitReasonId"
  })
  public ResponseEntity<ApiResponse<List<AttritionReasonSummaryResponse>>> getAttritionReasonSummariesByExitReasonId(
      @RequestParam("exitReasonId") Long exitReasonId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "exitReasonId");
    return ResponseEntity.ok(ApiResponse.ok(
        attritionReasonSummaryService.getAttritionReasonSummariesByExitReasonId(exitReasonId)));
  }

  @GetMapping(value = "/attrition-reasons", params = {
      "employmentExitReasonId", "!separationNatureId", "!isVoluntaryFlag",
      "!isInvoluntaryFlag", "!exitReasonId"
  })
  public ResponseEntity<ApiResponse<List<AttritionReasonSummaryResponse>>> getAttritionReasonSummariesByEmploymentExitReasonId(
      @RequestParam("employmentExitReasonId") Long employmentExitReasonId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "employmentExitReasonId");
    return ResponseEntity.ok(ApiResponse.ok(
        attritionReasonSummaryService.getAttritionReasonSummariesByExitReasonId(employmentExitReasonId)));
  }

  @GetMapping(value = "/attrition-reasons", params = {
      "separationNatureId", "!exitReasonId", "!employmentExitReasonId",
      "!isVoluntaryFlag", "!isInvoluntaryFlag"
  })
  public ResponseEntity<ApiResponse<List<AttritionReasonSummaryResponse>>> getAttritionReasonSummariesBySeparationNatureId(
      @RequestParam("separationNatureId") Long separationNatureId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "separationNatureId");
    return ResponseEntity.ok(ApiResponse.ok(
        attritionReasonSummaryService.getAttritionReasonSummariesBySeparationNatureId(separationNatureId)));
  }

  @GetMapping(value = "/attrition-reasons", params = {
      "isVoluntaryFlag", "!exitReasonId", "!employmentExitReasonId",
      "!separationNatureId", "!isInvoluntaryFlag"
  })
  public ResponseEntity<ApiResponse<List<AttritionReasonSummaryResponse>>> getAttritionReasonSummariesByIsVoluntaryFlag(
      @RequestParam("isVoluntaryFlag") Boolean isVoluntaryFlag,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "isVoluntaryFlag");
    return ResponseEntity.ok(ApiResponse.ok(
        attritionReasonSummaryService.getAttritionReasonSummariesByIsVoluntaryFlag(isVoluntaryFlag)));
  }

  @GetMapping(value = "/attrition-reasons", params = {
      "isInvoluntaryFlag", "!exitReasonId", "!employmentExitReasonId",
      "!separationNatureId", "!isVoluntaryFlag"
  })
  public ResponseEntity<ApiResponse<List<AttritionReasonSummaryResponse>>> getAttritionReasonSummariesByIsInvoluntaryFlag(
      @RequestParam("isInvoluntaryFlag") Boolean isInvoluntaryFlag,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "isInvoluntaryFlag");
    return ResponseEntity.ok(ApiResponse.ok(
        attritionReasonSummaryService.getAttritionReasonSummariesByIsInvoluntaryFlag(isInvoluntaryFlag)));
  }

  @GetMapping(value = "/attrition-reasons", params = {
      "exitReasonId", "separationNatureId", "!isVoluntaryFlag",
      "!isInvoluntaryFlag", "!employmentExitReasonId"
  })
  public ResponseEntity<ApiResponse<AttritionReasonSummaryResponse>> getAttritionReasonSummaryByQueryParams(
      @RequestParam("exitReasonId") Long exitReasonId,
      @RequestParam("separationNatureId") Long separationNatureId,
      HttpServletRequest httpRequest) {
    validateCompoundQueryParameters(httpRequest, Set.of("exitReasonId", "separationNatureId"));
    return ResponseEntity.ok(ApiResponse.ok(
        attritionReasonSummaryService.getAttritionReasonSummaryById(exitReasonId, separationNatureId)));
  }

  @GetMapping(value = "/attrition-reasons", params = {
      "employmentExitReasonId", "separationNatureId", "!isVoluntaryFlag",
      "!isInvoluntaryFlag", "!exitReasonId"
  })
  public ResponseEntity<ApiResponse<AttritionReasonSummaryResponse>> getAttritionReasonSummaryByEmploymentExitReasonQueryParams(
      @RequestParam("employmentExitReasonId") Long employmentExitReasonId,
      @RequestParam("separationNatureId") Long separationNatureId,
      HttpServletRequest httpRequest) {
    validateCompoundQueryParameters(httpRequest, Set.of("employmentExitReasonId", "separationNatureId"));
    return ResponseEntity.ok(ApiResponse.ok(
        attritionReasonSummaryService.getAttritionReasonSummaryById(employmentExitReasonId, separationNatureId)));
  }

  // =========================================================================
  // 2. Course Outcomes (/course-outcomes)
  // =========================================================================

  @GetMapping(value = "/course-outcomes", params = "!courseId")
  public ResponseEntity<ApiResponse<List<CourseOutcomeSummaryResponse>>> getAllCourseOutcomes(
      HttpServletRequest httpRequest) {
    validateNoQueryParameters(httpRequest);
    return ResponseEntity.ok(ApiResponse.ok(courseOutcomeSummaryService.getAllCourseOutcomes()));
  }

  @GetMapping("/course-outcomes/leaderboard")
  public ResponseEntity<ApiResponse<List<CourseOutcomeSummaryResponse>>> getCourseOutcomeLeaderboard() {
    return ResponseEntity.ok(ApiResponse.ok(
        courseOutcomeSummaryService.getAllCourseOutcomesOrderByEnrollmentCountDesc()));
  }

  @GetMapping("/course-outcomes/{courseId}")
  public ResponseEntity<ApiResponse<CourseOutcomeSummaryResponse>> getCourseOutcomeById(
      @PathVariable Long courseId) {
    return ResponseEntity.ok(ApiResponse.ok(courseOutcomeSummaryService.getCourseOutcomeByCourseId(courseId)));
  }

  @GetMapping(value = "/course-outcomes", params = "courseId")
  public ResponseEntity<ApiResponse<CourseOutcomeSummaryResponse>> getCourseOutcomeByCourseId(
      @RequestParam("courseId") Long courseId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "courseId");
    return ResponseEntity.ok(ApiResponse.ok(courseOutcomeSummaryService.getCourseOutcomeByCourseId(courseId)));
  }

  // =========================================================================
  // 3. District Outcomes (/district-outcomes)
  // =========================================================================

  @GetMapping(value = "/district-outcomes", params = "!districtId")
  public ResponseEntity<ApiResponse<List<DistrictOutcomeSummaryResponse>>> getAllDistrictOutcomes(
      HttpServletRequest httpRequest) {
    validateNoQueryParameters(httpRequest);
    return ResponseEntity.ok(ApiResponse.ok(districtOutcomeSummaryService.getAllDistrictOutcomes()));
  }

  @GetMapping("/district-outcomes/leaderboard")
  public ResponseEntity<ApiResponse<List<DistrictOutcomeSummaryResponse>>> getDistrictOutcomeLeaderboard() {
    return ResponseEntity.ok(ApiResponse.ok(
        districtOutcomeSummaryService.getAllDistrictOutcomesOrderByEnrollmentCountDesc()));
  }

  @GetMapping("/district-outcomes/{districtId}")
  public ResponseEntity<ApiResponse<DistrictOutcomeSummaryResponse>> getDistrictOutcomeById(
      @PathVariable Long districtId) {
    return ResponseEntity.ok(ApiResponse.ok(districtOutcomeSummaryService.getDistrictOutcomeByDistrictId(districtId)));
  }

  @GetMapping(value = "/district-outcomes", params = "districtId")
  public ResponseEntity<ApiResponse<DistrictOutcomeSummaryResponse>> getDistrictOutcomeByDistrictId(
      @RequestParam("districtId") Long districtId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "districtId");
    return ResponseEntity.ok(ApiResponse.ok(districtOutcomeSummaryService.getDistrictOutcomeByDistrictId(districtId)));
  }

  // =========================================================================
  // 4. Employer Hiring Summaries (/employer-hirings)
  // =========================================================================

  @GetMapping(value = "/employer-hirings", params = "!employerId")
  public ResponseEntity<ApiResponse<List<EmployerHiringSummaryResponse>>> getAllEmployerHiringSummaries(
      HttpServletRequest httpRequest) {
    validateNoQueryParameters(httpRequest);
    return ResponseEntity.ok(ApiResponse.ok(employerHiringSummaryService.getAllEmployerHiringSummaries()));
  }

  @GetMapping("/employer-hirings/leaderboard")
  public ResponseEntity<ApiResponse<List<EmployerHiringSummaryResponse>>> getEmployerHiringLeaderboard() {
    return ResponseEntity.ok(ApiResponse.ok(
        employerHiringSummaryService.getAllEmployerHiringSummariesOrderByJoinedCountDesc()));
  }

  @GetMapping("/employer-hirings/{employerId}")
  public ResponseEntity<ApiResponse<EmployerHiringSummaryResponse>> getEmployerHiringSummaryById(
      @PathVariable Long employerId) {
    return ResponseEntity.ok(ApiResponse.ok(
        employerHiringSummaryService.getEmployerHiringSummaryByEmployerId(employerId)));
  }

  @GetMapping(value = "/employer-hirings", params = "employerId")
  public ResponseEntity<ApiResponse<EmployerHiringSummaryResponse>> getEmployerHiringSummaryByEmployerId(
      @RequestParam("employerId") Long employerId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "employerId");
    return ResponseEntity.ok(ApiResponse.ok(
        employerHiringSummaryService.getEmployerHiringSummaryByEmployerId(employerId)));
  }

  // =========================================================================
  // 5. Followup Outcomes (/followup-outcomes)
  // =========================================================================

  @GetMapping(value = "/followup-outcomes", params = {
      "!typeCode", "!followupTypeCode", "!offsetMonths", "!followupOffsetMonths"
  })
  public ResponseEntity<ApiResponse<List<FollowupOutcomeSummaryResponse>>> getAllFollowupOutcomeSummaries(
      HttpServletRequest httpRequest) {
    validateNoQueryParameters(httpRequest);
    return ResponseEntity.ok(ApiResponse.ok(followupOutcomeSummaryService.getAllFollowupOutcomeSummaries()));
  }

  @GetMapping("/followup-outcomes/leaderboard")
  public ResponseEntity<ApiResponse<List<FollowupOutcomeSummaryResponse>>> getFollowupOutcomeLeaderboard() {
    return ResponseEntity.ok(ApiResponse.ok(
        followupOutcomeSummaryService.getAllFollowupOutcomeSummariesOrderByOffsetMonthsAsc()));
  }

  @GetMapping("/followup-outcomes/{typeCode}/{offsetMonths}")
  public ResponseEntity<ApiResponse<FollowupOutcomeSummaryResponse>> getFollowupOutcomeSummaryById(
      @PathVariable String typeCode,
      @PathVariable Integer offsetMonths) {
    return ResponseEntity.ok(ApiResponse.ok(
        followupOutcomeSummaryService.getFollowupOutcomeSummaryById(typeCode, offsetMonths)));
  }

  @GetMapping(value = "/followup-outcomes", params = {
      "typeCode", "!offsetMonths", "!followupOffsetMonths", "!followupTypeCode"
  })
  public ResponseEntity<ApiResponse<List<FollowupOutcomeSummaryResponse>>> getFollowupOutcomeSummariesByTypeCode(
      @RequestParam("typeCode") String typeCode,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "typeCode");
    return ResponseEntity.ok(ApiResponse.ok(
        followupOutcomeSummaryService.getFollowupOutcomeSummariesByTypeCode(typeCode)));
  }

  @GetMapping(value = "/followup-outcomes", params = {
      "followupTypeCode", "!offsetMonths", "!followupOffsetMonths", "!typeCode"
  })
  public ResponseEntity<ApiResponse<List<FollowupOutcomeSummaryResponse>>> getFollowupOutcomeSummariesByFollowupTypeCode(
      @RequestParam("followupTypeCode") String followupTypeCode,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "followupTypeCode");
    return ResponseEntity.ok(ApiResponse.ok(
        followupOutcomeSummaryService.getFollowupOutcomeSummariesByTypeCode(followupTypeCode)));
  }

  @GetMapping(value = "/followup-outcomes", params = {
      "offsetMonths", "!typeCode", "!followupTypeCode", "!followupOffsetMonths"
  })
  public ResponseEntity<ApiResponse<List<FollowupOutcomeSummaryResponse>>> getFollowupOutcomeSummariesByOffsetMonths(
      @RequestParam("offsetMonths") Integer offsetMonths,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "offsetMonths");
    return ResponseEntity.ok(ApiResponse.ok(
        followupOutcomeSummaryService.getFollowupOutcomeSummariesByOffsetMonths(offsetMonths)));
  }

  @GetMapping(value = "/followup-outcomes", params = {
      "followupOffsetMonths", "!typeCode", "!followupTypeCode", "!offsetMonths"
  })
  public ResponseEntity<ApiResponse<List<FollowupOutcomeSummaryResponse>>> getFollowupOutcomeSummariesByFollowupOffsetMonths(
      @RequestParam("followupOffsetMonths") Integer followupOffsetMonths,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "followupOffsetMonths");
    return ResponseEntity.ok(ApiResponse.ok(
        followupOutcomeSummaryService.getFollowupOutcomeSummariesByOffsetMonths(followupOffsetMonths)));
  }

  @GetMapping(value = "/followup-outcomes", params = {
      "typeCode", "offsetMonths", "!followupTypeCode", "!followupOffsetMonths"
  })
  public ResponseEntity<ApiResponse<FollowupOutcomeSummaryResponse>> getFollowupOutcomeSummaryByQueryParams(
      @RequestParam("typeCode") String typeCode,
      @RequestParam("offsetMonths") Integer offsetMonths,
      HttpServletRequest httpRequest) {
    validateCompoundQueryParameters(httpRequest, Set.of("typeCode", "offsetMonths"));
    return ResponseEntity.ok(ApiResponse.ok(
        followupOutcomeSummaryService.getFollowupOutcomeSummaryById(typeCode, offsetMonths)));
  }

  @GetMapping(value = "/followup-outcomes", params = {
      "followupTypeCode", "followupOffsetMonths", "!typeCode", "!offsetMonths"
  })
  public ResponseEntity<ApiResponse<FollowupOutcomeSummaryResponse>> getFollowupOutcomeSummaryByFollowupQueryParams(
      @RequestParam("followupTypeCode") String followupTypeCode,
      @RequestParam("followupOffsetMonths") Integer followupOffsetMonths,
      HttpServletRequest httpRequest) {
    validateCompoundQueryParameters(httpRequest, Set.of("followupTypeCode", "followupOffsetMonths"));
    return ResponseEntity.ok(ApiResponse.ok(
        followupOutcomeSummaryService.getFollowupOutcomeSummaryById(followupTypeCode, followupOffsetMonths)));
  }

  // =========================================================================
  // 6. Program Outcomes (/program-outcomes)
  // =========================================================================

  @GetMapping(value = "/program-outcomes", params = {"!programId", "!schemeId"})
  public ResponseEntity<ApiResponse<List<ProgramOutcomeSummaryResponse>>> getAllProgramOutcomes(
      HttpServletRequest httpRequest) {
    validateNoQueryParameters(httpRequest);
    return ResponseEntity.ok(ApiResponse.ok(programOutcomeSummaryService.getAllProgramOutcomes()));
  }

  @GetMapping("/program-outcomes/leaderboard")
  public ResponseEntity<ApiResponse<List<ProgramOutcomeSummaryResponse>>> getProgramOutcomeLeaderboard() {
    return ResponseEntity.ok(ApiResponse.ok(
        programOutcomeSummaryService.getAllProgramOutcomesOrderByEnrollmentCountDesc()));
  }

  @GetMapping("/program-outcomes/{programId}")
  public ResponseEntity<ApiResponse<ProgramOutcomeSummaryResponse>> getProgramOutcomeById(
      @PathVariable Long programId) {
    return ResponseEntity.ok(ApiResponse.ok(programOutcomeSummaryService.getProgramOutcomeByProgramId(programId)));
  }

  @GetMapping(value = "/program-outcomes", params = {"programId", "!schemeId"})
  public ResponseEntity<ApiResponse<ProgramOutcomeSummaryResponse>> getProgramOutcomeByProgramId(
      @RequestParam("programId") Long programId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "programId");
    return ResponseEntity.ok(ApiResponse.ok(programOutcomeSummaryService.getProgramOutcomeByProgramId(programId)));
  }

  @GetMapping(value = "/program-outcomes", params = {"schemeId", "!programId"})
  public ResponseEntity<ApiResponse<List<ProgramOutcomeSummaryResponse>>> getProgramOutcomesBySchemeId(
      @RequestParam("schemeId") Long schemeId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "schemeId");
    return ResponseEntity.ok(ApiResponse.ok(programOutcomeSummaryService.getProgramOutcomesBySchemeId(schemeId)));
  }

  // =========================================================================
  // 7. Provider Outcomes (/provider-outcomes)
  // =========================================================================

  @GetMapping(value = "/provider-outcomes", params = "!providerId")
  public ResponseEntity<ApiResponse<List<ProviderOutcomeSummaryResponse>>> getAllProviderOutcomes(
      HttpServletRequest httpRequest) {
    validateNoQueryParameters(httpRequest);
    return ResponseEntity.ok(ApiResponse.ok(providerOutcomeSummaryService.getAllProviderOutcomes()));
  }

  @GetMapping("/provider-outcomes/leaderboard")
  public ResponseEntity<ApiResponse<List<ProviderOutcomeSummaryResponse>>> getProviderOutcomeLeaderboard() {
    return ResponseEntity.ok(ApiResponse.ok(
        providerOutcomeSummaryService.getAllProviderOutcomesOrderByEnrollmentCountDesc()));
  }

  @GetMapping("/provider-outcomes/{providerId}")
  public ResponseEntity<ApiResponse<ProviderOutcomeSummaryResponse>> getProviderOutcomeById(
      @PathVariable Long providerId) {
    return ResponseEntity.ok(ApiResponse.ok(providerOutcomeSummaryService.getProviderOutcomeByProviderId(providerId)));
  }

  @GetMapping(value = "/provider-outcomes", params = "providerId")
  public ResponseEntity<ApiResponse<ProviderOutcomeSummaryResponse>> getProviderOutcomeByProviderId(
      @RequestParam("providerId") Long providerId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "providerId");
    return ResponseEntity.ok(ApiResponse.ok(providerOutcomeSummaryService.getProviderOutcomeByProviderId(providerId)));
  }

  // =========================================================================
  // 8. Skill Gap Summaries (/skill-gaps)
  // =========================================================================

  @GetMapping(value = "/skill-gaps", params = {
      "!skillId", "!severityId", "!skillGapSeverityId", "!severityCode"
  })
  public ResponseEntity<ApiResponse<List<SkillGapSummaryResponse>>> getAllSkillGapSummaries(
      HttpServletRequest httpRequest) {
    validateNoQueryParameters(httpRequest);
    return ResponseEntity.ok(ApiResponse.ok(skillGapSummaryService.getAllSkillGapSummaries()));
  }

  @GetMapping("/skill-gaps/leaderboard")
  public ResponseEntity<ApiResponse<List<SkillGapSummaryResponse>>> getSkillGapLeaderboard() {
    return ResponseEntity.ok(ApiResponse.ok(
        skillGapSummaryService.getAllSkillGapSummariesOrderByGapCountDesc()));
  }

  @GetMapping("/skill-gaps/{skillId}/{severityId}")
  public ResponseEntity<ApiResponse<SkillGapSummaryResponse>> getSkillGapSummaryById(
      @PathVariable Long skillId,
      @PathVariable Long severityId) {
    return ResponseEntity.ok(ApiResponse.ok(skillGapSummaryService.getSkillGapSummaryById(skillId, severityId)));
  }

  @GetMapping(value = "/skill-gaps", params = {
      "skillId", "!severityId", "!skillGapSeverityId", "!severityCode"
  })
  public ResponseEntity<ApiResponse<List<SkillGapSummaryResponse>>> getSkillGapSummariesBySkillId(
      @RequestParam("skillId") Long skillId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "skillId");
    return ResponseEntity.ok(ApiResponse.ok(skillGapSummaryService.getSkillGapSummariesBySkillId(skillId)));
  }

  @GetMapping(value = "/skill-gaps", params = {
      "severityId", "!skillId", "!skillGapSeverityId", "!severityCode"
  })
  public ResponseEntity<ApiResponse<List<SkillGapSummaryResponse>>> getSkillGapSummariesBySeverityId(
      @RequestParam("severityId") Long severityId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "severityId");
    return ResponseEntity.ok(ApiResponse.ok(skillGapSummaryService.getSkillGapSummariesBySeverityId(severityId)));
  }

  @GetMapping(value = "/skill-gaps", params = {
      "skillGapSeverityId", "!skillId", "!severityId", "!severityCode"
  })
  public ResponseEntity<ApiResponse<List<SkillGapSummaryResponse>>> getSkillGapSummariesBySkillGapSeverityId(
      @RequestParam("skillGapSeverityId") Long skillGapSeverityId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "skillGapSeverityId");
    return ResponseEntity.ok(ApiResponse.ok(skillGapSummaryService.getSkillGapSummariesBySeverityId(skillGapSeverityId)));
  }

  @GetMapping(value = "/skill-gaps", params = {
      "severityCode", "!skillId", "!severityId", "!skillGapSeverityId"
  })
  public ResponseEntity<ApiResponse<List<SkillGapSummaryResponse>>> getSkillGapSummariesBySeverityCode(
      @RequestParam("severityCode") String severityCode,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "severityCode");
    return ResponseEntity.ok(ApiResponse.ok(skillGapSummaryService.getSkillGapSummariesBySeverityCode(severityCode)));
  }

  @GetMapping(value = "/skill-gaps", params = {
      "skillId", "severityId", "!severityCode", "!skillGapSeverityId"
  })
  public ResponseEntity<ApiResponse<SkillGapSummaryResponse>> getSkillGapSummaryByQueryParams(
      @RequestParam("skillId") Long skillId,
      @RequestParam("severityId") Long severityId,
      HttpServletRequest httpRequest) {
    validateCompoundQueryParameters(httpRequest, Set.of("skillId", "severityId"));
    return ResponseEntity.ok(ApiResponse.ok(skillGapSummaryService.getSkillGapSummaryById(skillId, severityId)));
  }

  @GetMapping(value = "/skill-gaps", params = {
      "skillId", "skillGapSeverityId", "!severityCode", "!severityId"
  })
  public ResponseEntity<ApiResponse<SkillGapSummaryResponse>> getSkillGapSummaryBySkillGapSeverityQueryParams(
      @RequestParam("skillId") Long skillId,
      @RequestParam("skillGapSeverityId") Long skillGapSeverityId,
      HttpServletRequest httpRequest) {
    validateCompoundQueryParameters(httpRequest, Set.of("skillId", "skillGapSeverityId"));
    return ResponseEntity.ok(ApiResponse.ok(skillGapSummaryService.getSkillGapSummaryById(skillId, skillGapSeverityId)));
  }

  // =========================================================================
  // 9. Trainee Outcomes (/trainee-outcomes)
  // =========================================================================

  @GetMapping(value = "/trainee-outcomes", params = {
      "!traineeId", "!stateId", "!districtId", "!currentEmploymentStatusId", "!snapshotIsEmployedFlag"
  })
  public ResponseEntity<ApiResponse<List<TraineeOutcomeSummaryResponse>>> getAllTraineeOutcomes(
      HttpServletRequest httpRequest) {
    validateNoQueryParameters(httpRequest);
    return ResponseEntity.ok(ApiResponse.ok(traineeOutcomeSummaryService.getAllTraineeOutcomes()));
  }

  @GetMapping("/trainee-outcomes/{traineeId}")
  public ResponseEntity<ApiResponse<TraineeOutcomeSummaryResponse>> getTraineeOutcomeById(
      @PathVariable Long traineeId) {
    return ResponseEntity.ok(ApiResponse.ok(traineeOutcomeSummaryService.getTraineeOutcomeByTraineeId(traineeId)));
  }

  @GetMapping(value = "/trainee-outcomes", params = {
      "traineeId", "!stateId", "!districtId", "!currentEmploymentStatusId", "!snapshotIsEmployedFlag"
  })
  public ResponseEntity<ApiResponse<TraineeOutcomeSummaryResponse>> getTraineeOutcomeByTraineeId(
      @RequestParam("traineeId") Long traineeId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "traineeId");
    return ResponseEntity.ok(ApiResponse.ok(traineeOutcomeSummaryService.getTraineeOutcomeByTraineeId(traineeId)));
  }

  @GetMapping(value = "/trainee-outcomes", params = {
      "stateId", "!traineeId", "!districtId", "!currentEmploymentStatusId", "!snapshotIsEmployedFlag"
  })
  public ResponseEntity<ApiResponse<List<TraineeOutcomeSummaryResponse>>> getTraineeOutcomesByStateId(
      @RequestParam("stateId") Long stateId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "stateId");
    return ResponseEntity.ok(ApiResponse.ok(traineeOutcomeSummaryService.getTraineeOutcomesByStateId(stateId)));
  }

  @GetMapping(value = "/trainee-outcomes", params = {
      "districtId", "!traineeId", "!stateId", "!currentEmploymentStatusId", "!snapshotIsEmployedFlag"
  })
  public ResponseEntity<ApiResponse<List<TraineeOutcomeSummaryResponse>>> getTraineeOutcomesByDistrictId(
      @RequestParam("districtId") Long districtId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "districtId");
    return ResponseEntity.ok(ApiResponse.ok(traineeOutcomeSummaryService.getTraineeOutcomesByDistrictId(districtId)));
  }

  @GetMapping(value = "/trainee-outcomes", params = {
      "currentEmploymentStatusId", "!traineeId", "!stateId", "!districtId", "!snapshotIsEmployedFlag"
  })
  public ResponseEntity<ApiResponse<List<TraineeOutcomeSummaryResponse>>> getTraineeOutcomesByCurrentEmploymentStatusId(
      @RequestParam("currentEmploymentStatusId") Long currentEmploymentStatusId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "currentEmploymentStatusId");
    return ResponseEntity.ok(ApiResponse.ok(
        traineeOutcomeSummaryService.getTraineeOutcomesByCurrentEmploymentStatusId(currentEmploymentStatusId)));
  }

  @GetMapping(value = "/trainee-outcomes", params = {
      "snapshotIsEmployedFlag", "!traineeId", "!stateId", "!districtId", "!currentEmploymentStatusId"
  })
  public ResponseEntity<ApiResponse<List<TraineeOutcomeSummaryResponse>>> getTraineeOutcomesBySnapshotIsEmployedFlag(
      @RequestParam("snapshotIsEmployedFlag") Boolean snapshotIsEmployedFlag,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "snapshotIsEmployedFlag");
    return ResponseEntity.ok(ApiResponse.ok(
        traineeOutcomeSummaryService.getTraineeOutcomesBySnapshotIsEmployedFlag(snapshotIsEmployedFlag)));
  }

  // =========================================================================
  // 10. Unemployment Reason Summaries (/unemployment-reasons)
  // =========================================================================

  @GetMapping(value = "/unemployment-reasons", params = {
      "!unemploymentReasonId", "!code", "!unemploymentReasonCode"
  })
  public ResponseEntity<ApiResponse<List<UnemploymentReasonSummaryResponse>>> getAllUnemploymentReasonSummaries(
      HttpServletRequest httpRequest) {
    validateNoQueryParameters(httpRequest);
    return ResponseEntity.ok(ApiResponse.ok(unemploymentReasonSummaryService.getAllUnemploymentReasonSummaries()));
  }

  @GetMapping("/unemployment-reasons/leaderboard")
  public ResponseEntity<ApiResponse<List<UnemploymentReasonSummaryResponse>>> getUnemploymentReasonLeaderboard() {
    return ResponseEntity.ok(ApiResponse.ok(
        unemploymentReasonSummaryService.getAllUnemploymentReasonSummariesOrderByPeriodCountDesc()));
  }

  @GetMapping("/unemployment-reasons/{id}")
  public ResponseEntity<ApiResponse<UnemploymentReasonSummaryResponse>> getUnemploymentReasonSummaryById(
      @PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.ok(unemploymentReasonSummaryService.getUnemploymentReasonSummaryById(id)));
  }

  @GetMapping(value = "/unemployment-reasons", params = {
      "unemploymentReasonId", "!code", "!unemploymentReasonCode"
  })
  public ResponseEntity<ApiResponse<UnemploymentReasonSummaryResponse>> getUnemploymentReasonSummaryByReasonId(
      @RequestParam("unemploymentReasonId") Long unemploymentReasonId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "unemploymentReasonId");
    return ResponseEntity.ok(ApiResponse.ok(
        unemploymentReasonSummaryService.getUnemploymentReasonSummaryById(unemploymentReasonId)));
  }

  @GetMapping(value = "/unemployment-reasons", params = {
      "code", "!unemploymentReasonId", "!unemploymentReasonCode"
  })
  public ResponseEntity<ApiResponse<UnemploymentReasonSummaryResponse>> getUnemploymentReasonSummaryByCode(
      @RequestParam("code") String code,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "code");
    return ResponseEntity.ok(ApiResponse.ok(
        unemploymentReasonSummaryService.getUnemploymentReasonSummaryByCode(code)));
  }

  @GetMapping(value = "/unemployment-reasons", params = {
      "unemploymentReasonCode", "!unemploymentReasonId", "!code"
  })
  public ResponseEntity<ApiResponse<UnemploymentReasonSummaryResponse>> getUnemploymentReasonSummaryByReasonCode(
      @RequestParam("unemploymentReasonCode") String unemploymentReasonCode,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "unemploymentReasonCode");
    return ResponseEntity.ok(ApiResponse.ok(
        unemploymentReasonSummaryService.getUnemploymentReasonSummaryByCode(unemploymentReasonCode)));
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
