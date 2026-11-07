package in.gov.sih.sih26135.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.sih.sih26135.MysqlConnectionTest;
import in.gov.sih.sih26135.dto.response.AttritionReasonBreakdownResponse;
import in.gov.sih.sih26135.dto.response.CourseEmployabilityResponse;
import in.gov.sih.sih26135.dto.response.DashboardKpiStripResponse;
import in.gov.sih.sih26135.dto.response.DistrictPerformanceResponse;
import in.gov.sih.sih26135.dto.response.EmployerHiringPerformanceResponse;
import in.gov.sih.sih26135.dto.response.EmploymentCategoryBreakdownResponse;
import in.gov.sih.sih26135.dto.response.EmploymentExitFactResponse;
import in.gov.sih.sih26135.dto.response.EmploymentFactResponse;
import in.gov.sih.sih26135.dto.response.EmploymentRetentionFactResponse;
import in.gov.sih.sih26135.dto.response.EmploymentVerificationFactResponse;
import in.gov.sih.sih26135.dto.response.EnrollmentOutcomeFactResponse;
import in.gov.sih.sih26135.dto.response.FollowupFactResponse;
import in.gov.sih.sih26135.dto.response.GeographicOverviewResponse;
import in.gov.sih.sih26135.dto.response.PipelineFunnelResponse;
import in.gov.sih.sih26135.dto.response.PlacementFactResponse;
import in.gov.sih.sih26135.dto.response.ProviderPerformanceResponse;
import in.gov.sih.sih26135.dto.response.RetentionCohortResponse;
import in.gov.sih.sih26135.dto.response.RetentionMilestoneResponse;
import in.gov.sih.sih26135.dto.response.SalaryDistributionResponse;
import in.gov.sih.sih26135.dto.response.SalaryProgressionFactResponse;
import in.gov.sih.sih26135.dto.response.SeparationAnalysisResponse;
import in.gov.sih.sih26135.dto.response.SkillGapFactResponse;
import in.gov.sih.sih26135.dto.response.StatePerformanceResponse;
import in.gov.sih.sih26135.dto.response.SurveyResponseFactResponse;
import in.gov.sih.sih26135.dto.response.UnemploymentFactResponse;
import in.gov.sih.sih26135.dto.response.WageProgressionResponse;
import in.gov.sih.sih26135.security.UserPrincipal;
import in.gov.sih.sih26135.security.jwt.JwtTokenProvider;
import in.gov.sih.sih26135.service.privacy.AnalyticsPrivacySuppressionService;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Production-grade Test Suite for Phase 27.14:
 * Statistical Disclosure Control, Data Privacy & k-Anonymity Guardrails.
 *
 * <p>Enforces:
 * <ul>
 *   <li>Boundary conditions (k = 5): 0 preserved, 1..4 suppressed, 5 released, 6+ released.</li>
 *   <li>Distinct person denominators (trainee_id, not raw rows/events).</li>
 *   <li>Independent k=5 cell-level suppression for sensitive outcomes (completed, employed) even when parent meets k >= 5.</li>
 *   <li>Secondary (complementary) suppression preventing deductive algebraic reconstruction from sibling totals.</li>
 *   <li>Sensitive monetary measures (avgStartingWage, avgJoiningSalary, wage delta) protected when distinct salary contributors < 5.</li>
 *   <li>Zero direct-PII leakage across analytical dashboard and fact endpoints.</li>
 *   <li>Cold miss and warm Caffeine cache privacy preservation.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Phase 27.14 — Statistical Disclosure Control, Data Privacy & k-Anonymity Tests")
public class AnalyticsStatisticalDisclosureControlTest {

  @Autowired
  private AnalyticsPrivacySuppressionService suppressionService;

  @Autowired
  private GeographicDashboardService geographicDashboardService;

  @Autowired
  private DataSource dataSource;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  private String adminToken;

  private static final Pattern PII_PATTERN = Pattern.compile(
      "(?<!\\d)[1-9]\\d{3}\\s?\\d{4}\\s?\\d{4}(?!\\d)" // Aadhaar (12-digit)
      + "|(?<!\\w)[A-Z]{5}[0-9]{4}[A-Z](?!\\w)"          // PAN card
      + "|(?<!\\d)[6-9]\\d{9}(?!\\d)"                   // Indian 10-digit mobile number
      + "|[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}" // Email address
  );

  @BeforeEach
  void setUpSecurity() throws Exception {
    try (Connection connection = dataSource.getConnection()) {
      MysqlConnectionTest.assertDedicatedTestCatalog(connection.getCatalog());
      assertThat(connection.getCatalog()).isEqualToIgnoringCase(MysqlConnectionTest.EXPECTED_TEST_CATALOG);
    }

    adminToken = "Bearer " + jwtTokenProvider.generateAccessToken(
        1L,
        "admin",
        List.of("ROLE_super_admin", "analytics.read")
    );

    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(
            new UserPrincipal(1L, "admin"),
            null,
            List.of(new SimpleGrantedAuthority("ROLE_super_admin"), new SimpleGrantedAuthority("analytics.read"))
        )
    );
  }

  @AfterEach
  void tearDownSecurity() {
    SecurityContextHolder.clearContext();
  }

  // =========================================================================
  // 1. Boundary Condition Tests (k = 5)
  // =========================================================================

  @Nested
  @DisplayName("1. Boundary Condition Tests (k = 5)")
  class BoundaryConditionTests {

    @Test
    @DisplayName("Safe Zero Preserved: count=0 and rate=0.00% are not suppressed")
    void testSafeZeroPreserved() {
      assertThat(suppressionService.isSparse(0L)).isFalse();
      assertThat(suppressionService.suppressCount(0L)).isEqualTo(0L);
      assertThat(suppressionService.suppressRate(BigDecimal.ZERO, 0L)).isEqualTo(BigDecimal.ZERO);

      DistrictPerformanceResponse input = new DistrictPerformanceResponse(
          101L, "D-01", "Zero Outcomes District", 1L, "ST", "State",
          10L, 10L, 0L, 0L, 0L, 0L,
          BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 1
      );

      DistrictPerformanceResponse result = suppressionService.applyPrivacyGuardrail(input);

      // Safe zero counts and zero rates are preserved because 0 indicates non-occurrence without singling out
      assertThat(result.traineeCount()).isEqualTo(10L);
      assertThat(result.completedCount()).isEqualTo(0L);
      assertThat(result.employedCount()).isEqualTo(0L);
      assertThat(result.completionRatePct()).isEqualByComparingTo(BigDecimal.ZERO);
      assertThat(result.employmentRatePct()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Boundary 1 to 4: Sparse parent counts suppressed to null")
    void testBoundaryOneToFourSuppressed() {
      for (long sparseCount = 1; sparseCount < 5; sparseCount++) {
        assertThat(suppressionService.isSparse(sparseCount))
            .as("Count %d must be recognized as sparse (< 5)", sparseCount)
            .isTrue();

        assertThat(suppressionService.suppressCount(sparseCount))
            .as("Count %d must be suppressed to null", sparseCount)
            .isNull();

        assertThat(suppressionService.suppressRate(new BigDecimal("50.00"), sparseCount))
            .as("Rate with denominator %d must be suppressed to null", sparseCount)
            .isNull();

        DistrictPerformanceResponse input = new DistrictPerformanceResponse(
            101L, "D-01", "Sparse District", 1L, "ST", "State",
            sparseCount, sparseCount, sparseCount, sparseCount, sparseCount, sparseCount,
            new BigDecimal("100.00"), new BigDecimal("100.00"), new BigDecimal("100.00"), new BigDecimal("100.00"), 1
        );

        DistrictPerformanceResponse result = suppressionService.applyPrivacyGuardrail(input);

        assertThat(result.traineeCount()).isNull();
        assertThat(result.enrollmentCount()).isNull();
        assertThat(result.completedCount()).isNull();
        assertThat(result.certifiedCount()).isNull();
        assertThat(result.joinedPlacementCount()).isNull();
        assertThat(result.employedCount()).isNull();
        assertThat(result.completionRatePct()).isNull();
        assertThat(result.certificationRatePct()).isNull();
        assertThat(result.placementRatePct()).isNull();
        assertThat(result.employmentRatePct()).isNull();
        // Identifiers and ranking preserved
        assertThat(result.districtId()).isEqualTo(101L);
        assertThat(result.rank()).isEqualTo(1);
      }
    }

    @Test
    @DisplayName("Boundary 5: Minimum threshold met, parent released but sensitive sub-outcomes < 5 suppressed")
    void testBoundaryFiveReleased() {
      // Cohort of 5 with 4 completed and 3 employed trainees
      DistrictPerformanceResponse input = new DistrictPerformanceResponse(
          101L, "D-01", "Threshold District", 1L, "ST", "State",
          5L, 5L, 4L, 4L, 3L, 3L,
          new BigDecimal("80.00"), new BigDecimal("80.00"), new BigDecimal("60.00"), new BigDecimal("60.00"), 1
      );

      DistrictPerformanceResponse result = suppressionService.applyPrivacyGuardrail(input);

      // Parent counts meeting k=5 are released
      assertThat(result.traineeCount()).isEqualTo(5L);
      assertThat(result.enrollmentCount()).isEqualTo(5L);

      // Sensitive sub-outcomes < 5 MUST BE INDEPENDENTLY SUPPRESSED to null!
      assertThat(result.completedCount()).isNull();
      assertThat(result.completionRatePct()).isNull();
      assertThat(result.certifiedCount()).isNull();
      assertThat(result.certificationRatePct()).isNull();
      assertThat(result.joinedPlacementCount()).isNull();
      assertThat(result.placementRatePct()).isNull();
      assertThat(result.employedCount()).isNull();
      assertThat(result.employmentRatePct()).isNull();
    }

    @Test
    @DisplayName("Cohort of 5 with 3 employed trainees does not disclose sensitive 3-person outcome or its rate")
    void testCohortOfFiveWithThreeEmployedSuppressesSensitiveOutcome() {
      DistrictPerformanceResponse input = new DistrictPerformanceResponse(
          101L, "D-01", "District 5", 1L, "ST", "State",
          5L, 5L, 5L, 5L, 3L, 3L,
          new BigDecimal("100.00"), new BigDecimal("100.00"), new BigDecimal("60.00"), new BigDecimal("60.00"), 1
      );

      DistrictPerformanceResponse result = suppressionService.applyPrivacyGuardrail(input);

      // Trainee count (5) and completed count (5) are released
      assertThat(result.traineeCount()).isEqualTo(5L);
      assertThat(result.completedCount()).isEqualTo(5L);
      assertThat(result.completionRatePct()).isEqualByComparingTo(new BigDecimal("100.00"));

      // Employed count (3) and its rate (60.00%) MUST NOT be disclosed
      assertThat(result.employedCount()).isNull();
      assertThat(result.employmentRatePct()).isNull();
    }

    @Test
    @DisplayName("Boundary 6+: Normal population released without suppression")
    void testBoundarySixPlusReleased() {
      DistrictPerformanceResponse input = new DistrictPerformanceResponse(
          101L, "D-01", "Large District", 1L, "ST", "State",
          500L, 480L, 400L, 380L, 300L, 260L,
          new BigDecimal("80.00"), new BigDecimal("76.00"), new BigDecimal("60.00"), new BigDecimal("52.00"), 1
      );

      DistrictPerformanceResponse result = suppressionService.applyPrivacyGuardrail(input);

      assertThat(result.traineeCount()).isEqualTo(480L);
      assertThat(result.enrollmentCount()).isEqualTo(500L);
      assertThat(result.completedCount()).isEqualTo(400L);
      assertThat(result.employmentRatePct()).isEqualByComparingTo(new BigDecimal("52.00"));
    }
  }

  // =========================================================================
  // 2. Distinct Individuals vs Raw Fact/Row Counts
  // =========================================================================

  @Nested
  @DisplayName("2. Distinct Individuals vs Raw Row Counts")
  class DistinctDenominatorTests {

    @Test
    @DisplayName("Course with 20 raw enrollments but only 3 distinct trainees is suppressed")
    void testCourseFewDistinctTraineesSuppressed() {
      CourseEmployabilityResponse input = new CourseEmployabilityResponse(
          201L, "CRS-SPARSE", "Sparse Course", 1L, "NSQF-4", "Level 4", 4,
          20L, 3L, 3L, 3L, 2L, 2L, // 20 enrollments, but only 3 distinct trainees!
          new BigDecimal("100.00"), new BigDecimal("100.00"), new BigDecimal("66.67"), new BigDecimal("66.67"),
          BigDecimal.ZERO, new BigDecimal("25000.00"), 1
      );

      CourseEmployabilityResponse result = suppressionService.applyPrivacyGuardrail(input);

      assertThat(result.traineeCount()).isNull();
      assertThat(result.enrollmentCount()).isNull();
      assertThat(result.completionRatePct()).isNull();
      assertThat(result.avgStartingWage()).isNull();
    }

    @Test
    @DisplayName("Provider with 10 raw events but only 2 distinct trainees is suppressed")
    void testProviderFewDistinctTraineesSuppressed() {
      ProviderPerformanceResponse input = new ProviderPerformanceResponse(
          301L, "TP-SPARSE", "Sparse Provider",
          10L, 2L, 2L, 2L, 1L, 1L,
          new BigDecimal("100.00"), new BigDecimal("100.00"), new BigDecimal("50.00"), new BigDecimal("50.00"),
          50L, 1
      );

      ProviderPerformanceResponse result = suppressionService.applyPrivacyGuardrail(input);

      assertThat(result.traineeCount()).isNull();
      assertThat(result.enrollmentCount()).isNull();
      assertThat(result.employmentRatePct()).isNull();
    }

    @Test
    @DisplayName("Pipeline Funnel: 1 trainee with 5 repeated enrollments does not satisfy k=5 and is suppressed")
    void testRepeatedEnrollmentsFromOneTraineeDoesNotSatisfyK() {
      PipelineFunnelResponse input = new PipelineFunnelResponse(
          5L, 5L, 5L, 4L, 4L,
          new BigDecimal("100.00"), new BigDecimal("100.00"), new BigDecimal("80.00"), new BigDecimal("80.00")
      );

      // Trainee count is 1 (repeated enrollments by 1 trainee)
      PipelineFunnelResponse result = suppressionService.applyPrivacyGuardrail(input, 1L);

      // Since distinct trainee count is 1 (< 5), the entire funnel must be suppressed
      assertThat(result.enrollmentCount()).isNull();
      assertThat(result.completedCount()).isNull();
      assertThat(result.certifiedCount()).isNull();
      assertThat(result.joinedPlacementCount()).isNull();
      assertThat(result.employedCount()).isNull();
      assertThat(result.completionRatePct()).isNull();
      assertThat(result.employmentRatePct()).isNull();
    }
  }

  // =========================================================================
  // 3. Secondary (Complementary) Suppression & Reconstruction Defense
  // =========================================================================

  @Nested
  @DisplayName("3. Secondary Suppression & Anti-Reconstruction Tests")
  class SecondarySuppressionTests {

    @Test
    @DisplayName("Salary Distribution: When exactly 1 cell is sparse, smallest sibling is also suppressed")
    void testSalaryDistributionSecondarySuppression() {
      List<SalaryDistributionResponse> brackets = List.of(
          new SalaryDistributionResponse("BELOW_15K", "Below ₹15,000", 3L, new BigDecimal("3.00")), // Sparse!
          new SalaryDistributionResponse("FROM_15K_TO_25K", "₹15,000–₹25,000", 50L, new BigDecimal("50.00")),
          new SalaryDistributionResponse("FROM_25K_TO_50K", "₹25,000–₹50,000", 30L, new BigDecimal("30.00")),
          new SalaryDistributionResponse("ABOVE_50K", "Above ₹50,000", 17L, new BigDecimal("17.00")) // Smallest sibling!
      );

      List<SalaryDistributionResponse> protectedList =
          suppressionService.applyPrivacyGuardrailToSalaryDistribution(brackets);

      // Primary cell (3) suppressed
      assertThat(protectedList.get(0).count()).isNull();
      assertThat(protectedList.get(0).percentage()).isNull();

      // Sibling cell (17) MUST also be suppressed by secondary suppression
      assertThat(protectedList.get(3).count()).isNull();
      assertThat(protectedList.get(3).percentage()).isNull();

      // Other larger cells (50, 30) remain released
      assertThat(protectedList.get(1).count()).isEqualTo(50L);
      assertThat(protectedList.get(2).count()).isEqualTo(30L);

      // Total suppressed count is 2 (reconstruction impossible)
      long nullCount = protectedList.stream().filter(b -> b.count() == null).count();
      assertThat(nullCount).isEqualTo(2L);
    }

    @Test
    @DisplayName("Salary Distribution: When 2 cells are already primary suppressed, no extra suppression needed")
    void testSalaryDistributionTwoPrimarySuppressed() {
      List<SalaryDistributionResponse> brackets = List.of(
          new SalaryDistributionResponse("BELOW_15K", "Below ₹15,000", 2L, new BigDecimal("2.00")), // Sparse!
          new SalaryDistributionResponse("FROM_15K_TO_25K", "₹15,000–₹25,000", 3L, new BigDecimal("3.00")), // Sparse!
          new SalaryDistributionResponse("FROM_25K_TO_50K", "₹25,000–₹50,000", 50L, new BigDecimal("50.00")),
          new SalaryDistributionResponse("ABOVE_50K", "Above ₹50,000", 45L, new BigDecimal("45.00"))
      );

      List<SalaryDistributionResponse> protectedList =
          suppressionService.applyPrivacyGuardrailToSalaryDistribution(brackets);

      assertThat(protectedList.get(0).count()).isNull();
      assertThat(protectedList.get(1).count()).isNull();
      assertThat(protectedList.get(2).count()).isEqualTo(50L);
      assertThat(protectedList.get(3).count()).isEqualTo(45L);
    }

    @Test
    @DisplayName("Separation Analysis: When voluntary exit is sparse, secondary suppression applies")
    void testSeparationAnalysisSecondarySuppression() {
      SeparationAnalysisResponse input = new SeparationAnalysisResponse(
          100L,
          3L, // Voluntary sparse!
          80L, // Involuntary
          17L, // Other (smallest sibling >= 5)
          new BigDecimal("3.00"),
          new BigDecimal("80.00"),
          new BigDecimal("17.00"),
          new BigDecimal("0.04"),
          null
      );

      SeparationAnalysisResponse result = suppressionService.applyPrivacyGuardrail(input);

      // Voluntary count suppressed
      assertThat(result.voluntaryExitCount()).isNull();
      assertThat(result.voluntaryExitSharePct()).isNull();
      assertThat(result.voluntaryToInvoluntaryRatio()).isNull();

      // Sibling 'other' count also suppressed via secondary suppression
      assertThat(result.otherExitCount()).isNull();
      assertThat(result.otherExitSharePct()).isNull();

      // Involuntary (80) released
      assertThat(result.involuntaryExitCount()).isEqualTo(80L);
    }

    @Test
    @DisplayName("Geographic States: National total and geographic siblings cannot reconstruct a suppressed 1-4 cell")
    void testGeographicStatesSecondarySuppressionPreventsReconstruction() {
      List<StatePerformanceResponse> states = List.of(
          new StatePerformanceResponse(1L, "ST-A", "State A", 5L, 3L, 3L, null, null, null, null, null, null, null, null, 10L, 1), // Sparse: 3 trainees!
          new StatePerformanceResponse(2L, "ST-B", "State B", 10L, 10L, 10L, 8L, 8L, 6L, 6L, new BigDecimal("80.00"), new BigDecimal("80.00"), new BigDecimal("60.00"), new BigDecimal("60.00"), 20L, 2), // Smallest sibling >= 5: 10 trainees!
          new StatePerformanceResponse(3L, "ST-C", "State C", 15L, 20L, 20L, 18L, 18L, 15L, 15L, new BigDecimal("90.00"), new BigDecimal("90.00"), new BigDecimal("75.00"), new BigDecimal("75.00"), 30L, 3) // 20 trainees
      );

      List<StatePerformanceResponse> protectedStates = suppressionService.applyPrivacyGuardrailsToStates(states);

      // Primary sparse state A (3) is suppressed
      assertThat(protectedStates.get(0).traineeCount()).isNull();
      assertThat(protectedStates.get(0).enrollmentCount()).isNull();

      // Sibling state B (10) MUST be secondary-suppressed so that National Total (33) cannot reconstruct State A (33 - 10 - 20 = 3)
      assertThat(protectedStates.get(1).traineeCount()).isNull();
      assertThat(protectedStates.get(1).enrollmentCount()).isNull();

      // Larger state C (20) remains released
      assertThat(protectedStates.get(2).traineeCount()).isEqualTo(20L);

      // Total suppressed count is 2 (reconstruction impossible: State A + State B = 33 - 20 = 13, cannot solve for State A)
      long nullTraineeCount = protectedStates.stream().filter(s -> s.traineeCount() == null).count();
      assertThat(nullTraineeCount).isEqualTo(2L);
    }

    @Test
    @DisplayName("Geographic Overview: Single state suppression withholds revealing totals")
    void testGeographicOverviewSingleStateWithholdsTotals() {
      List<StatePerformanceResponse> singleStateList = List.of(
          new StatePerformanceResponse(1L, "ST-A", "Single Sparse State", 5L, 3L, 3L, null, null, null, null, null, null, null, null, 10L, 1)
      );
      GeographicOverviewResponse input = new GeographicOverviewResponse(
          1L, 5L, 3L, 2L, 2L, 1L, 1L,
          new BigDecimal("66.67"), new BigDecimal("66.67"), new BigDecimal("33.33"), new BigDecimal("33.33"),
          singleStateList, null, null
      );

      GeographicOverviewResponse result = suppressionService.applyPrivacyGuardrail(input);

      // Since the only state is sparse (3), published national totals MUST be withheld to prevent reconstruction
      assertThat(result.totalEnrollments()).isNull();
      assertThat(result.totalCompleted()).isNull();
      assertThat(result.totalCertified()).isNull();
      assertThat(result.totalJoinedPlacement()).isNull();
      assertThat(result.totalEmployed()).isNull();
    }
  }

  // =========================================================================
  // 4. Sensitive Salary & Measure Protection
  // =========================================================================

  @Nested
  @DisplayName("4. Sensitive Salary & Monetary Measure Protection")
  class SensitiveSalaryProtectionTests {

    @Test
    @DisplayName("Course Employability: avgStartingWage suppressed when employed trainees < 5 even if enrollment >= 5")
    void testCourseAvgStartingWageProtection() {
      // 50 trainees enrolled, but only 2 employed trainees contributing starting wage
      CourseEmployabilityResponse input = new CourseEmployabilityResponse(
          101L, "CRS-IT", "IT Specialist", 1L, "NSQF-5", "Level 5", 5,
          60L, 50L, 40L, 35L, 2L, 2L, // employedCount = 2 (< 5)!
          new BigDecimal("80.00"), new BigDecimal("70.00"), new BigDecimal("4.00"), new BigDecimal("4.00"),
          new BigDecimal("20.00"), new BigDecimal("35000.00"), 1
      );

      CourseEmployabilityResponse result = suppressionService.applyPrivacyGuardrail(input);

      // Macro counts and completion rates are released because traineeCount (50) >= 5
      assertThat(result.traineeCount()).isEqualTo(50L);
      assertThat(result.enrollmentCount()).isEqualTo(60L);
      assertThat(result.completionRatePct()).isNotNull();

      // But avgStartingWage MUST be suppressed to null because salary contributors (2) < 5!
      assertThat(result.avgStartingWage()).isNull();
    }

    @Test
    @DisplayName("Course Employability: 20 employed trainees with only 2 valid salary contributors does not expose average salary")
    void testTwentyEmployedWithTwoSalaryContributorsSuppressesSalary() {
      CourseEmployabilityResponse input = new CourseEmployabilityResponse(
          101L, "CRS-IT", "IT Specialist", 1L, "NSQF-5", "Level 5", 5,
          30L, 25L, 22L, 21L, 20L, 20L, // 20 employed trainees (>= 5)!
          new BigDecimal("88.00"), new BigDecimal("84.00"), new BigDecimal("80.00"), new BigDecimal("80.00"),
          BigDecimal.ZERO, new BigDecimal("35000.00"), 1
      );

      // Only 2 valid salary contributors!
      CourseEmployabilityResponse result = suppressionService.applyPrivacyGuardrail(input, 2L);

      // Outcome counts are released (all >= 5)
      assertThat(result.traineeCount()).isEqualTo(25L);
      assertThat(result.employedCount()).isEqualTo(20L);
      assertThat(result.employmentRatePct()).isEqualByComparingTo(new BigDecimal("80.00"));

      // But avgStartingWage MUST be suppressed to null because distinct salary contributors (2) < 5!
      assertThat(result.avgStartingWage()).isNull();
    }

    @Test
    @DisplayName("Employer Leaderboard: 20 joined trainees with only 2 valid salary contributors does not expose average joining salary")
    void testEmployerTwentyJoinedWithTwoSalaryContributorsSuppressesSalary() {
      EmployerHiringPerformanceResponse input = new EmployerHiringPerformanceResponse(
          501L, "EMP-CORP", "MegaCorp",
          25L, 22L, 20L, // joinedCount = 20 (>= 5)!
          new BigDecimal("80.00"), new BigDecimal("45000.00"), 1
      );

      EmployerHiringPerformanceResponse result = suppressionService.applyPrivacyGuardrail(input, 2L);

      assertThat(result.traineeCount()).isEqualTo(22L);
      assertThat(result.joinedCount()).isEqualTo(20L);
      assertThat(result.avgJoiningSalary()).isNull();
    }

    @Test
    @DisplayName("Employer Leaderboard: avgJoiningSalary suppressed when joinedCount < 5 even if traineeCount >= 5")
    void testEmployerAvgJoiningSalaryProtection() {
      EmployerHiringPerformanceResponse input = new EmployerHiringPerformanceResponse(
          501L, "EMP-CORP", "MegaCorp",
          50L, 40L, 3L, // joinedCount = 3 (< 5)!
          new BigDecimal("7.50"), new BigDecimal("45000.00"), 1
      );

      EmployerHiringPerformanceResponse result = suppressionService.applyPrivacyGuardrail(input);

      assertThat(result.traineeCount()).isEqualTo(40L);
      assertThat(result.placementCount()).isEqualTo(50L);
      assertThat(result.avgJoiningSalary()).isNull();
    }

    @Test
    @DisplayName("Wage Progression: averages and delta percentages suppressed when salary counts < 5")
    void testWageProgressionProtection() {
      WageProgressionResponse input = new WageProgressionResponse(
          100L, 3L, 2L, // firstSalaryCount = 3, latestSalaryCount = 2 (< 5)
          new BigDecimal("20000.00"), new BigDecimal("28000.00"),
          new BigDecimal("8000.00"), new BigDecimal("40.00")
      );

      WageProgressionResponse result = suppressionService.applyPrivacyGuardrail(input);

      assertThat(result.firstSalaryCount()).isNull();
      assertThat(result.latestSalaryCount()).isNull();
      assertThat(result.avgFirstRecordedSalary()).isNull();
      assertThat(result.avgLatestRecordedSalary()).isNull();
      assertThat(result.avgSalaryChangeAmount()).isNull();
      assertThat(result.avgSalaryChangePct()).isNull();
    }
  }

  // =========================================================================
  // 5. Zero PII Exposure Audit (Fact & Dashboard Routes)
  // =========================================================================

  @Nested
  @DisplayName("5. Zero Direct-PII Exposure Audit")
  class ZeroPiiExposureTests {

    @Test
    @DisplayName("Structural PII Audit: all 11 Fact Response Record classes contain zero PII fields")
    void testAllFactResponseRecordsContainZeroPiiFields() {
      Class<?>[] factClasses = new Class<?>[] {
          EnrollmentOutcomeFactResponse.class,
          PlacementFactResponse.class,
          EmploymentFactResponse.class,
          EmploymentRetentionFactResponse.class,
          SalaryProgressionFactResponse.class,
          EmploymentVerificationFactResponse.class,
          SkillGapFactResponse.class,
          UnemploymentFactResponse.class,
          EmploymentExitFactResponse.class,
          FollowupFactResponse.class,
          SurveyResponseFactResponse.class
      };

      for (Class<?> clazz : factClasses) {
        for (var field : clazz.getDeclaredFields()) {
          String name = field.getName().toLowerCase();
          assertThat(name).doesNotContain("aadhaar");
          assertThat(name).doesNotContain("traineename");
          assertThat(name).doesNotContain("pannumber");
          assertThat(name).doesNotContain("bankaccount");
          assertThat(name).doesNotContain("phonenumber");
          assertThat(name).doesNotContain("password");
        }
      }
    }

    @Test
    @DisplayName("Audit /api/v1/dashboards/geography: produces 200 OK with zero direct PII")
    void testGeographyDashboardNoPii() throws Exception {
      MvcResult result = mockMvc.perform(get("/api/v1/dashboards/geography")
              .header(HttpHeaders.AUTHORIZATION, adminToken)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andReturn();

      String content = result.getResponse().getContentAsString();
      assertNoPiiLeakage(content);
    }

    @Test
    @DisplayName("Audit /api/v1/dashboards/providers: produces 200 OK with zero direct PII")
    void testProviderLeaderboardNoPii() throws Exception {
      MvcResult result = mockMvc.perform(get("/api/v1/dashboards/providers")
              .header(HttpHeaders.AUTHORIZATION, adminToken)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andReturn();

      String content = result.getResponse().getContentAsString();
      assertNoPiiLeakage(content);
    }

    @Test
    @DisplayName("Audit /api/v1/dashboards/courses: produces 200 OK with zero direct PII")
    void testCourseLeaderboardNoPii() throws Exception {
      MvcResult result = mockMvc.perform(get("/api/v1/dashboards/courses")
              .header(HttpHeaders.AUTHORIZATION, adminToken)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andReturn();

      String content = result.getResponse().getContentAsString();
      assertNoPiiLeakage(content);
    }

    @Test
    @DisplayName("Audit /api/v1/dashboards/economic-outcomes: produces 200 OK with zero direct PII")
    void testEconomicDashboardNoPii() throws Exception {
      MvcResult result = mockMvc.perform(get("/api/v1/dashboards/economic-outcomes")
              .header(HttpHeaders.AUTHORIZATION, adminToken)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andReturn();

      String content = result.getResponse().getContentAsString();
      assertNoPiiLeakage(content);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/api/v1/analytics/facts/enrollment-outcomes",
        "/api/v1/analytics/facts/employments",
        "/api/v1/analytics/facts/placements",
        "/api/v1/analytics/facts/salary-progressions",
        "/api/v1/analytics/facts/employment-verifications",
        "/api/v1/analytics/facts/followups",
        "/api/v1/analytics/facts/survey-responses",
        "/api/v1/analytics/facts/skill-gaps",
        "/api/v1/analytics/facts/unemployments",
        "/api/v1/analytics/facts/employment-retentions",
        "/api/v1/analytics/facts/employment-exits"
    })
    @DisplayName("Serialized HTTP Fact Endpoints: bounded 200 OK with zero direct PII")
    void testSerializedFactEndpointsZeroPii(String endpoint) throws Exception {
      MvcResult result = mockMvc.perform(get(endpoint + "?page=0&size=5")
              .header(HttpHeaders.AUTHORIZATION, adminToken)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andReturn();

      String content = result.getResponse().getContentAsString();
      assertNoPiiLeakage(content);

      // Verify structure uses surrogate IDs only
      JsonNode root = objectMapper.readTree(content);
      JsonNode items = root.path("data").path("content");
      if (items.isArray() && items.size() > 0) {
        JsonNode first = items.get(0);
        assertThat(first.has("traineeName")).isFalse();
        assertThat(first.has("aadhaarNumber")).isFalse();
        assertThat(first.has("panNumber")).isFalse();
        assertThat(first.has("phoneNumber")).isFalse();
        assertThat(first.has("email")).isFalse();
      }
    }

    private void assertNoPiiLeakage(String jsonContent) {
      assertThat(jsonContent).doesNotContain("aadhaar");
      assertThat(jsonContent).doesNotContain("traineeName");
      assertThat(jsonContent).doesNotContain("panNumber");
      // Strip metadata envelope fields (correlationId random UUIDs and ISO timestamps) to prevent false-positive PII matches
      String payload = jsonContent
          .replaceAll("\"correlationId\"\\s*:\\s*\"[^\"]*\"", "")
          .replaceAll("\"timestamp\"\\s*:\\s*\"[^\"]*\"", "");
      var matcher = PII_PATTERN.matcher(payload);
      if (matcher.find()) {
        org.junit.jupiter.api.Assertions.fail("Response JSON contains sensitive pattern match: '" + matcher.group() + "' in " + jsonContent);
      }
    }
  }

  // =========================================================================
  // 6. Caffeine Cache Privacy Preservation
  // =========================================================================

  @Nested
  @DisplayName("6. Cold and Warm Cache Privacy Preservation")
  class CachePrivacyTests {

    @Test
    @DisplayName("Cold miss computes and applies privacy guardrails; warm hit returns privacy-safe cached DTO")
    void testCachePreservesSuppression() {
      // 1. Cold execution
      var firstCall = geographicDashboardService.getNationalGeographicOverview();
      assertThat(firstCall).isNotNull();

      // 2. Warm cached execution
      var secondCall = geographicDashboardService.getNationalGeographicOverview();
      assertThat(secondCall).isNotNull();

      // State summaries must remain privacy-guarded on warm cache hits
      for (StatePerformanceResponse state : secondCall.stateSummaries()) {
        if (state.traineeCount() != null && state.traineeCount() > 0 && state.traineeCount() < 5) {
          assertThat(state.completionRatePct()).isNull();
          assertThat(state.enrollmentCount()).isNull();
        }
      }
    }
  }
}
