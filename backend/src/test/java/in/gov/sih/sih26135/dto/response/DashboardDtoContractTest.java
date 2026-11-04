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
}
