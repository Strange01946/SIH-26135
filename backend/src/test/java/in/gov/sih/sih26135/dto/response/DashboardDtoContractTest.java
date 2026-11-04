package in.gov.sih.sih26135.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Dashboard Response DTO Contract and Serialization Tests")
class DashboardDtoContractTest {

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
  }

  @Test
  @DisplayName("DashboardKpiStripResponse: instantiation, getters, and JSON roundtrip")
  void testDashboardKpiStripResponseContract() throws Exception {
    DashboardKpiStripResponse dto = new DashboardKpiStripResponse(
        1000L,
        1200L,
        950L,
        800L,
        750L,
        600L,
        550L,
        520L,
        500L,
        480L,
        30L,
        10L,
        new BigDecimal("66.67"),
        new BigDecimal("62.50"),
        new BigDecimal("45.83"),
        new BigDecimal("41.67")
    );

    assertThat(dto.traineeCount()).isEqualTo(1000L);
    assertThat(dto.enrollmentCount()).isEqualTo(1200L);
    assertThat(dto.enrolledTraineeCount()).isEqualTo(950L);
    assertThat(dto.completedEnrollmentCount()).isEqualTo(800L);
    assertThat(dto.certifiedEnrollmentCount()).isEqualTo(750L);
    assertThat(dto.placementCount()).isEqualTo(600L);
    assertThat(dto.joinedPlacementCount()).isEqualTo(550L);
    assertThat(dto.employmentSpellCount()).isEqualTo(520L);
    assertThat(dto.employedTraineeCount()).isEqualTo(500L);
    assertThat(dto.currentEmploymentSpellCount()).isEqualTo(480L);
    assertThat(dto.currentSelfEmploymentCount()).isEqualTo(30L);
    assertThat(dto.currentApprenticeshipCount()).isEqualTo(10L);
    assertThat(dto.completionRatePct()).isEqualByComparingTo(new BigDecimal("66.67"));
    assertThat(dto.certificationRatePct()).isEqualByComparingTo(new BigDecimal("62.50"));
    assertThat(dto.placementRatePct()).isEqualByComparingTo(new BigDecimal("45.83"));
    assertThat(dto.employmentRatePct()).isEqualByComparingTo(new BigDecimal("41.67"));

    String json = objectMapper.writeValueAsString(dto);
    assertThat(json).contains("\"traineeCount\":1000");
    assertThat(json).contains("\"completionRatePct\":66.67");

    DashboardKpiStripResponse deserialized = objectMapper.readValue(json, DashboardKpiStripResponse.class);
    assertThat(deserialized).isEqualTo(dto);
  }

  @Test
  @DisplayName("PipelineFunnelResponse: instantiation, getters, and JSON roundtrip")
  void testPipelineFunnelResponseContract() throws Exception {
    PipelineFunnelResponse dto = new PipelineFunnelResponse(
        5000L,
        4200L,
        3800L,
        3000L,
        2800L,
        new BigDecimal("84.00"),
        new BigDecimal("76.00"),
        new BigDecimal("60.00"),
        new BigDecimal("56.00")
    );

    assertThat(dto.enrollmentCount()).isEqualTo(5000L);
    assertThat(dto.completedCount()).isEqualTo(4200L);
    assertThat(dto.certifiedCount()).isEqualTo(3800L);
    assertThat(dto.joinedPlacementCount()).isEqualTo(3000L);
    assertThat(dto.employedCount()).isEqualTo(2800L);
    assertThat(dto.completionRatePct()).isEqualByComparingTo(new BigDecimal("84.00"));
    assertThat(dto.certificationRatePct()).isEqualByComparingTo(new BigDecimal("76.00"));
    assertThat(dto.placementRatePct()).isEqualByComparingTo(new BigDecimal("60.00"));
    assertThat(dto.employmentRatePct()).isEqualByComparingTo(new BigDecimal("56.00"));

    String json = objectMapper.writeValueAsString(dto);
    PipelineFunnelResponse deserialized = objectMapper.readValue(json, PipelineFunnelResponse.class);
    assertThat(deserialized).isEqualTo(dto);
  }

  @Test
  @DisplayName("LeaderboardResponse<T>: generic typing, immutability, and JSON roundtrip")
  void testLeaderboardResponseContract() throws Exception {
    record TestItem(Long id, String name, BigDecimal score) {}

    List<TestItem> originalList = new ArrayList<>();
    originalList.add(new TestItem(1L, "Alpha", new BigDecimal("98.50")));
    originalList.add(new TestItem(2L, "Beta", new BigDecimal("87.20")));

    LeaderboardResponse<TestItem> leaderboard = LeaderboardResponse.of("employmentRatePct", originalList);

    assertThat(leaderboard.rankingMetric()).isEqualTo("employmentRatePct");
    assertThat(leaderboard.totalItems()).isEqualTo(2L);
    assertThat(leaderboard.items()).hasSize(2);

    // Verify immutability: modifying original does not mutate leaderboard items
    originalList.add(new TestItem(3L, "Gamma", new BigDecimal("75.00")));
    assertThat(leaderboard.items()).hasSize(2);
    assertThat(leaderboard.totalItems()).isEqualTo(2L);

    // Verify null handling
    LeaderboardResponse<String> empty = LeaderboardResponse.of("count", null);
    assertThat(empty.items()).isEmpty();
    assertThat(empty.totalItems()).isEqualTo(0L);

    // Verify JSON roundtrip
    String json = objectMapper.writeValueAsString(leaderboard);
    LeaderboardResponse<TestItem> deserialized = objectMapper.readValue(
        json,
        new TypeReference<LeaderboardResponse<TestItem>>() {}
    );
    assertThat(deserialized.rankingMetric()).isEqualTo(leaderboard.rankingMetric());
    assertThat(deserialized.totalItems()).isEqualTo(leaderboard.totalItems());
    assertThat(deserialized.items()).isEqualTo(leaderboard.items());
  }

  @Test
  @DisplayName("TimeSeriesDataPointResponse: factories, date range, and JSON roundtrip")
  void testTimeSeriesDataPointResponseContract() throws Exception {
    // Count factory
    TimeSeriesDataPointResponse countPoint = TimeSeriesDataPointResponse.ofCount("2026-Q1", "enrollments", 350L);
    assertThat(countPoint.period()).isEqualTo("2026-Q1");
    assertThat(countPoint.metricName()).isEqualTo("enrollments");
    assertThat(countPoint.countValue()).isEqualTo(350L);
    assertThat(countPoint.decimalValue()).isNull();
    assertThat(countPoint.startDate()).isNull();
    assertThat(countPoint.endDate()).isNull();

    // Decimal factory
    TimeSeriesDataPointResponse decimalPoint = TimeSeriesDataPointResponse.ofDecimal("2026-Q1", "placementRatePct", new BigDecimal("72.50"));
    assertThat(decimalPoint.period()).isEqualTo("2026-Q1");
    assertThat(decimalPoint.decimalValue()).isEqualByComparingTo(new BigDecimal("72.50"));
    assertThat(decimalPoint.countValue()).isNull();

    // Full constructor with dates and dimension
    LocalDate start = LocalDate.of(2026, 1, 1);
    LocalDate end = LocalDate.of(2026, 3, 31);
    TimeSeriesDataPointResponse fullPoint = new TimeSeriesDataPointResponse(
        "2026-Q1",
        start,
        end,
        "salaries",
        null,
        new BigDecimal("35000.00"),
        "IT_SECTOR"
    );
    assertThat(fullPoint.dimension()).isEqualTo("IT_SECTOR");
    assertThat(fullPoint.startDate()).isEqualTo(start);
    assertThat(fullPoint.endDate()).isEqualTo(end);

    String json = objectMapper.writeValueAsString(fullPoint);
    TimeSeriesDataPointResponse deserialized = objectMapper.readValue(json, TimeSeriesDataPointResponse.class);
    assertThat(deserialized).isEqualTo(fullPoint);
  }

  @Test
  @DisplayName("RetentionCohortResponse: instantiation, null milestones, and JSON roundtrip")
  void testRetentionCohortResponseContract() throws Exception {
    RetentionCohortResponse dto = new RetentionCohortResponse(
        1000L,
        1000L,
        850L,
        new BigDecimal("85.00"),
        900L,
        720L,
        new BigDecimal("80.00"),
        500L,
        350L,
        new BigDecimal("70.00"),
        0L,
        0L,
        null // 36-month unobservable / unreached milestone
    );

    assertThat(dto.employmentSpellCount()).isEqualTo(1000L);
    assertThat(dto.observable6mCount()).isEqualTo(1000L);
    assertThat(dto.retained6mCount()).isEqualTo(850L);
    assertThat(dto.retention6mPct()).isEqualByComparingTo(new BigDecimal("85.00"));
    assertThat(dto.observable12mCount()).isEqualTo(900L);
    assertThat(dto.retained12mCount()).isEqualTo(720L);
    assertThat(dto.retention12mPct()).isEqualByComparingTo(new BigDecimal("80.00"));
    assertThat(dto.observable24mCount()).isEqualTo(500L);
    assertThat(dto.retained24mCount()).isEqualTo(350L);
    assertThat(dto.retention24mPct()).isEqualByComparingTo(new BigDecimal("70.00"));
    assertThat(dto.observable36mCount()).isEqualTo(0L);
    assertThat(dto.retained36mCount()).isEqualTo(0L);
    assertThat(dto.retention36mPct()).isNull();

    String json = objectMapper.writeValueAsString(dto);
    RetentionCohortResponse deserialized = objectMapper.readValue(json, RetentionCohortResponse.class);
    assertThat(deserialized).isEqualTo(dto);
  }

  @Test
  @DisplayName("DistributionBreakdownResponse: factories, dimension, auxiliary metric, and JSON roundtrip")
  void testDistributionBreakdownResponseContract() throws Exception {
    DistributionBreakdownResponse simple = DistributionBreakdownResponse.of(
        "WAGE",
        "Wage Employment",
        450L,
        new BigDecimal("75.00")
    );
    assertThat(simple.categoryCode()).isEqualTo("WAGE");
    assertThat(simple.categoryName()).isEqualTo("Wage Employment");
    assertThat(simple.count()).isEqualTo(450L);
    assertThat(simple.percentage()).isEqualByComparingTo(new BigDecimal("75.00"));
    assertThat(simple.dimension()).isNull();
    assertThat(simple.auxiliaryMetric()).isNull();

    DistributionBreakdownResponse dimensional = DistributionBreakdownResponse.of(
        "HIGH",
        "High Severity Gap",
        120L,
        new BigDecimal("20.00"),
        "TECHNICAL_SKILLS"
    );
    assertThat(dimensional.dimension()).isEqualTo("TECHNICAL_SKILLS");

    DistributionBreakdownResponse full = DistributionBreakdownResponse.of(
        "SECTOR_IT",
        "Information Technology",
        300L,
        new BigDecimal("50.00"),
        "INDUSTRY_VERTICAL",
        new BigDecimal("42500.00")
    );
    assertThat(full.auxiliaryMetric()).isEqualByComparingTo(new BigDecimal("42500.00"));

    String json = objectMapper.writeValueAsString(full);
    DistributionBreakdownResponse deserialized = objectMapper.readValue(json, DistributionBreakdownResponse.class);
    assertThat(deserialized).isEqualTo(full);
  }

  @Test
  @DisplayName("ProviderPerformanceResponse: instantiation, getters, and JSON roundtrip")
  void testProviderPerformanceResponseContract() throws Exception {
    ProviderPerformanceResponse dto = new ProviderPerformanceResponse(
        10L,
        "TP-001",
        "Apex Training Institute",
        1500L,
        1400L,
        1350L,
        1300L,
        1100L,
        1050L,
        new BigDecimal("90.00"),
        new BigDecimal("86.67"),
        new BigDecimal("73.33"),
        new BigDecimal("70.00"),
        250L,
        1
    );

    assertThat(dto.providerId()).isEqualTo(10L);
    assertThat(dto.providerCode()).isEqualTo("TP-001");
    assertThat(dto.providerName()).isEqualTo("Apex Training Institute");
    assertThat(dto.enrollmentCount()).isEqualTo(1500L);
    assertThat(dto.traineeCount()).isEqualTo(1400L);
    assertThat(dto.completedCount()).isEqualTo(1350L);
    assertThat(dto.certifiedCount()).isEqualTo(1300L);
    assertThat(dto.joinedPlacementCount()).isEqualTo(1100L);
    assertThat(dto.employedCount()).isEqualTo(1050L);
    assertThat(dto.completionRatePct()).isEqualByComparingTo(new BigDecimal("90.00"));
    assertThat(dto.certificationRatePct()).isEqualByComparingTo(new BigDecimal("86.67"));
    assertThat(dto.placementRatePct()).isEqualByComparingTo(new BigDecimal("73.33"));
    assertThat(dto.employmentRatePct()).isEqualByComparingTo(new BigDecimal("70.00"));
    assertThat(dto.activeBatchCapacity()).isEqualTo(250L);
    assertThat(dto.rank()).isEqualTo(1);

    String json = objectMapper.writeValueAsString(dto);
    assertThat(json).contains("\"providerCode\":\"TP-001\"");
    assertThat(json).contains("\"rank\":1");

    ProviderPerformanceResponse deserialized = objectMapper.readValue(json, ProviderPerformanceResponse.class);
    assertThat(deserialized).isEqualTo(dto);
  }

  @Test
  @DisplayName("CourseEmployabilityResponse: instantiation, getters, and JSON roundtrip")
  void testCourseEmployabilityResponseContract() throws Exception {
    CourseEmployabilityResponse dto = new CourseEmployabilityResponse(
        50L,
        "CRS-DATA-ENG",
        "Data Engineering Specialist",
        3L,
        "NSQF-5",
        "NSQF Level 5",
        5,
        800L,
        750L,
        720L,
        700L,
        600L,
        580L,
        new BigDecimal("90.00"),
        new BigDecimal("87.50"),
        new BigDecimal("75.00"),
        new BigDecimal("72.50"),
        new BigDecimal("10.00"),
        new BigDecimal("45000.00"),
        1
    );

    assertThat(dto.courseId()).isEqualTo(50L);
    assertThat(dto.courseCode()).isEqualTo("CRS-DATA-ENG");
    assertThat(dto.courseName()).isEqualTo("Data Engineering Specialist");
    assertThat(dto.qualificationLevelId()).isEqualTo(3L);
    assertThat(dto.qualificationLevelCode()).isEqualTo("NSQF-5");
    assertThat(dto.qualificationLevelName()).isEqualTo("NSQF Level 5");
    assertThat(dto.nsqfLevel()).isEqualTo(5);
    assertThat(dto.enrollmentCount()).isEqualTo(800L);
    assertThat(dto.traineeCount()).isEqualTo(750L);
    assertThat(dto.completedCount()).isEqualTo(720L);
    assertThat(dto.certifiedCount()).isEqualTo(700L);
    assertThat(dto.joinedPlacementCount()).isEqualTo(600L);
    assertThat(dto.employedCount()).isEqualTo(580L);
    assertThat(dto.completionRatePct()).isEqualByComparingTo(new BigDecimal("90.00"));
    assertThat(dto.certificationRatePct()).isEqualByComparingTo(new BigDecimal("87.50"));
    assertThat(dto.placementRatePct()).isEqualByComparingTo(new BigDecimal("75.00"));
    assertThat(dto.employmentRatePct()).isEqualByComparingTo(new BigDecimal("72.50"));
    assertThat(dto.dropoutRatePct()).isEqualByComparingTo(new BigDecimal("10.00"));
    assertThat(dto.avgStartingWage()).isEqualByComparingTo(new BigDecimal("45000.00"));
    assertThat(dto.rank()).isEqualTo(1);

    String json = objectMapper.writeValueAsString(dto);
    assertThat(json).contains("\"courseCode\":\"CRS-DATA-ENG\"");
    assertThat(json).contains("\"dropoutRatePct\":10.00");
    assertThat(json).contains("\"avgStartingWage\":45000.00");

    CourseEmployabilityResponse deserialized = objectMapper.readValue(json, CourseEmployabilityResponse.class);
    assertThat(deserialized).isEqualTo(dto);
  }

  @Test
  @DisplayName("EmployerHiringPerformanceResponse record contract and JSON serialization")
  void testEmployerHiringPerformanceResponseContract() throws Exception {
    EmployerHiringPerformanceResponse dto = new EmployerHiringPerformanceResponse(
        10L,
        "EMP-TATA",
        "Tata Consultancy Services",
        500L,
        450L,
        400L,
        new BigDecimal("80.00"),
        new BigDecimal("35000.00"),
        1
    );

    assertThat(dto.employerId()).isEqualTo(10L);
    assertThat(dto.employerCode()).isEqualTo("EMP-TATA");
    assertThat(dto.employerName()).isEqualTo("Tata Consultancy Services");
    assertThat(dto.placementCount()).isEqualTo(500L);
    assertThat(dto.traineeCount()).isEqualTo(450L);
    assertThat(dto.joinedCount()).isEqualTo(400L);
    assertThat(dto.joinRatePct()).isEqualByComparingTo(new BigDecimal("80.00"));
    assertThat(dto.avgJoiningSalary()).isEqualByComparingTo(new BigDecimal("35000.00"));
    assertThat(dto.rank()).isEqualTo(1);

    String json = objectMapper.writeValueAsString(dto);
    assertThat(json).contains("\"employerCode\":\"EMP-TATA\"");
    assertThat(json).contains("\"joinRatePct\":80.00");
    assertThat(json).contains("\"avgJoiningSalary\":35000.00");

    EmployerHiringPerformanceResponse deserialized =
        objectMapper.readValue(json, EmployerHiringPerformanceResponse.class);
    assertThat(deserialized).isEqualTo(dto);
  }

  @Test
  @DisplayName("WageProgressionResponse record contract and JSON serialization")
  void testWageProgressionResponseContract() throws Exception {
    WageProgressionResponse dto = new WageProgressionResponse(
        1000L,
        800L,
        750L,
        new BigDecimal("18000.00"),
        new BigDecimal("24000.00"),
        new BigDecimal("6000.00"),
        new BigDecimal("33.33")
    );

    assertThat(dto.employmentWithSalaryRowCount()).isEqualTo(1000L);
    assertThat(dto.firstSalaryCount()).isEqualTo(800L);
    assertThat(dto.latestSalaryCount()).isEqualTo(750L);
    assertThat(dto.avgFirstRecordedSalary()).isEqualByComparingTo(new BigDecimal("18000.00"));
    assertThat(dto.avgLatestRecordedSalary()).isEqualByComparingTo(new BigDecimal("24000.00"));
    assertThat(dto.avgSalaryChangeAmount()).isEqualByComparingTo(new BigDecimal("6000.00"));
    assertThat(dto.avgSalaryChangePct()).isEqualByComparingTo(new BigDecimal("33.33"));

    String json = objectMapper.writeValueAsString(dto);
    assertThat(json).contains("\"avgSalaryChangeAmount\":6000.00");
    assertThat(json).contains("\"avgSalaryChangePct\":33.33");

    WageProgressionResponse deserialized =
        objectMapper.readValue(json, WageProgressionResponse.class);
    assertThat(deserialized).isEqualTo(dto);
  }

  @Test
  @DisplayName("SalaryDistributionResponse record contract and JSON serialization")
  void testSalaryDistributionResponseContract() throws Exception {
    SalaryDistributionResponse dto = new SalaryDistributionResponse(
        "FROM_15K_TO_25K",
        "₹15,000–₹25,000",
        450L,
        new BigDecimal("45.00")
    );

    assertThat(dto.bracketCode()).isEqualTo("FROM_15K_TO_25K");
    assertThat(dto.bracketName()).isEqualTo("₹15,000–₹25,000");
    assertThat(dto.count()).isEqualTo(450L);
    assertThat(dto.percentage()).isEqualByComparingTo(new BigDecimal("45.00"));

    String json = objectMapper.writeValueAsString(dto);
    assertThat(json).contains("\"bracketCode\":\"FROM_15K_TO_25K\"");
    assertThat(json).contains("\"count\":450");
    assertThat(json).contains("\"percentage\":45.00");

    SalaryDistributionResponse deserialized =
        objectMapper.readValue(json, SalaryDistributionResponse.class);
    assertThat(deserialized).isEqualTo(dto);
  }

  @Test
  @DisplayName("EmploymentMixResponse record contract and JSON serialization")
  void testEmploymentMixResponseContract() throws Exception {
    EmploymentMixResponse dto = new EmploymentMixResponse(
        1000L,
        900L,
        700L,
        600L,
        200L,
        150L,
        50L,
        450L,
        150L,
        new BigDecimal("180.50"),
        new BigDecimal("120.00"),
        new BigDecimal("60.00"),
        new BigDecimal("20.00"),
        new BigDecimal("15.00"),
        new BigDecimal("5.00")
    );

    assertThat(dto.employmentSpellCount()).isEqualTo(1000L);
    assertThat(dto.traineeCount()).isEqualTo(900L);
    assertThat(dto.wageEmploymentSharePct()).isEqualByComparingTo(new BigDecimal("60.00"));
    assertThat(dto.selfEmploymentSharePct()).isEqualByComparingTo(new BigDecimal("20.00"));
    assertThat(dto.apprenticeshipSharePct()).isEqualByComparingTo(new BigDecimal("15.00"));
    assertThat(dto.internshipSharePct()).isEqualByComparingTo(new BigDecimal("5.00"));

    String json = objectMapper.writeValueAsString(dto);
    assertThat(json).contains("\"employmentSpellCount\":1000");
    assertThat(json).contains("\"wageEmploymentSharePct\":60.00");

    EmploymentMixResponse deserialized =
        objectMapper.readValue(json, EmploymentMixResponse.class);
    assertThat(deserialized).isEqualTo(dto);
  }

  @Test
  @DisplayName("EmploymentCategoryBreakdownResponse record contract and JSON serialization")
  void testEmploymentCategoryBreakdownResponseContract() throws Exception {
    EmploymentCategoryBreakdownResponse dto = new EmploymentCategoryBreakdownResponse(
        "WAGE_EMPLOYMENT",
        "Wage employment",
        true,
        false,
        false,
        false,
        600L,
        550L,
        450L,
        new BigDecimal("60.00"),
        new BigDecimal("210.00")
    );

    assertThat(dto.engagementTypeCode()).isEqualTo("WAGE_EMPLOYMENT");
    assertThat(dto.engagementTypeName()).isEqualTo("Wage employment");
    assertThat(dto.isWageEmployment()).isTrue();
    assertThat(dto.isSelfEmployment()).isFalse();
    assertThat(dto.spellCount()).isEqualTo(600L);
    assertThat(dto.sharePct()).isEqualByComparingTo(new BigDecimal("60.00"));
    assertThat(dto.avgDurationDays()).isEqualByComparingTo(new BigDecimal("210.00"));

    String json = objectMapper.writeValueAsString(dto);
    assertThat(json).contains("\"engagementTypeCode\":\"WAGE_EMPLOYMENT\"");
    assertThat(json).contains("\"sharePct\":60.00");

    EmploymentCategoryBreakdownResponse deserialized =
        objectMapper.readValue(json, EmploymentCategoryBreakdownResponse.class);
    assertThat(deserialized).isEqualTo(dto);
  }

  @Test
  @DisplayName("CourseWageProgressionResponse record contract and JSON serialization")
  void testCourseWageProgressionResponseContract() throws Exception {
    CourseWageProgressionResponse dto = new CourseWageProgressionResponse(
        101L,
        "CRS-JAVA",
        "Full Stack Java",
        200L,
        new BigDecimal("20000.00"),
        new BigDecimal("28000.00"),
        new BigDecimal("40.00")
    );

    assertThat(dto.courseId()).isEqualTo(101L);
    assertThat(dto.courseCode()).isEqualTo("CRS-JAVA");
    assertThat(dto.courseName()).isEqualTo("Full Stack Java");
    assertThat(dto.spellCount()).isEqualTo(200L);
    assertThat(dto.avgFirstRecordedSalary()).isEqualByComparingTo(new BigDecimal("20000.00"));
    assertThat(dto.avgLatestRecordedSalary()).isEqualByComparingTo(new BigDecimal("28000.00"));
    assertThat(dto.avgSalaryChangePct()).isEqualByComparingTo(new BigDecimal("40.00"));

    String json = objectMapper.writeValueAsString(dto);
    assertThat(json).contains("\"courseCode\":\"CRS-JAVA\"");
    assertThat(json).contains("\"avgSalaryChangePct\":40.00");

    CourseWageProgressionResponse deserialized =
        objectMapper.readValue(json, CourseWageProgressionResponse.class);
    assertThat(deserialized).isEqualTo(dto);
  }

  @Test
  @DisplayName("EconomicOutcomeSummaryResponse record contract and JSON serialization")
  void testEconomicOutcomeSummaryResponseContract() throws Exception {
    WageProgressionResponse wage = new WageProgressionResponse(
        100L, 80L, 75L,
        new BigDecimal("15000.00"), new BigDecimal("20000.00"),
        new BigDecimal("5000.00"), new BigDecimal("33.33")
    );
    SalaryDistributionResponse dist = new SalaryDistributionResponse(
        "BELOW_15K", "Below ₹15,000", 20L, new BigDecimal("25.00")
    );
    EmploymentMixResponse mix = new EmploymentMixResponse(
        100L, 90L, 70L, 60L, 20L, 15L, 5L, 45L, 15L,
        new BigDecimal("150.00"), new BigDecimal("100.00"),
        new BigDecimal("60.00"), new BigDecimal("20.00"),
        new BigDecimal("15.00"), new BigDecimal("5.00")
    );
    EmploymentCategoryBreakdownResponse cat = new EmploymentCategoryBreakdownResponse(
        "WAGE_EMPLOYMENT", "Wage employment", true, false, false, false,
        60L, 55L, 45L, new BigDecimal("60.00"), new BigDecimal("150.00")
    );
    CourseWageProgressionResponse course = new CourseWageProgressionResponse(
        10L, "CRS-10", "Course 10", 30L,
        new BigDecimal("15000.00"), new BigDecimal("21000.00"), new BigDecimal("40.00")
    );

    EconomicOutcomeSummaryResponse summary = new EconomicOutcomeSummaryResponse(
        wage,
        List.of(dist),
        mix,
        List.of(cat),
        List.of(course)
    );

    assertThat(summary.wageProgression()).isEqualTo(wage);
    assertThat(summary.salaryDistribution()).hasSize(1);
    assertThat(summary.employmentMix()).isEqualTo(mix);
    assertThat(summary.categoryBreakdown()).hasSize(1);
    assertThat(summary.courseWageProgression()).hasSize(1);

    String json = objectMapper.writeValueAsString(summary);
    assertThat(json).contains("\"wageProgression\"");
    assertThat(json).contains("\"salaryDistribution\"");
    assertThat(json).contains("\"employmentMix\"");
    assertThat(json).contains("\"categoryBreakdown\"");
    assertThat(json).contains("\"courseWageProgression\"");

    EconomicOutcomeSummaryResponse deserialized =
        objectMapper.readValue(json, EconomicOutcomeSummaryResponse.class);
    assertThat(deserialized).isEqualTo(summary);
  }
}
