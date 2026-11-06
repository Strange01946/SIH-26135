package in.gov.sih.sih26135.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.gov.sih.sih26135.dto.response.CourseEmployabilityResponse;
import in.gov.sih.sih26135.dto.response.LeaderboardResponse;
import in.gov.sih.sih26135.dto.response.ProviderPerformanceResponse;
import in.gov.sih.sih26135.repository.analytics.InstitutionalAnalyticsRepository;
import in.gov.sih.sih26135.repository.analytics.projection.CourseEmployabilityProjection;
import in.gov.sih.sih26135.repository.analytics.projection.ProviderPerformanceProjection;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import in.gov.sih.sih26135.service.impl.InstitutionalAnalyticsServiceImpl;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link InstitutionalAnalyticsServiceImpl}.
 * Validates formal query Q04 and Q05 mapping, ranking logic, tie-breaking,
 * derived metric calculations (dropout rate), null-safety, and qualification grouping.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InstitutionalAnalyticsService Unit Tests")
class InstitutionalAnalyticsServiceTest {

  @Mock
  private InstitutionalAnalyticsRepository institutionalAnalyticsRepository;

  @Mock
  private WorkflowSecurityScopeValidator scopeValidator;

  private InstitutionalAnalyticsServiceImpl institutionalAnalyticsService;

  @BeforeEach
  void setUp() {
    institutionalAnalyticsService = new InstitutionalAnalyticsServiceImpl(institutionalAnalyticsRepository, scopeValidator);
  }

  @Test
  @DisplayName("Provider Leaderboard: maps projections, assigns 1-based ranks, and calculates metrics correctly")
  void testProviderLeaderboardMappingAndRanking() {
    ProviderPerformanceProjection p1 = new ProviderPerformanceProjection(
        1L, "TP-01", "Provider Alpha", 1000L, 950L, 900L, 850L, 800L, 750L,
        new BigDecimal("90.00"), new BigDecimal("85.00"), new BigDecimal("80.00"), new BigDecimal("75.00"), 200L
    );
    ProviderPerformanceProjection p2 = new ProviderPerformanceProjection(
        2L, "TP-02", "Provider Beta", 500L, 480L, 450L, 400L, 350L, 300L,
        new BigDecimal("90.00"), new BigDecimal("80.00"), new BigDecimal("70.00"), new BigDecimal("60.00"), 100L
    );

    when(institutionalAnalyticsRepository.getFormalQ04ProviderLeaderboard()).thenReturn(List.of(p1, p2));

    LeaderboardResponse<ProviderPerformanceResponse> response = institutionalAnalyticsService.getProviderLeaderboard();

    assertThat(response).isNotNull();
    assertThat(response.rankingMetric()).isEqualTo("employment_rate_pct DESC, enrollment_count DESC");
    assertThat(response.totalItems()).isEqualTo(2);
    assertThat(response.items()).hasSize(2);

    ProviderPerformanceResponse item1 = response.items().get(0);
    assertThat(item1.rank()).isEqualTo(1);
    assertThat(item1.providerId()).isEqualTo(1L);
    assertThat(item1.providerCode()).isEqualTo("TP-01");
    assertThat(item1.enrollmentCount()).isEqualTo(1000L);
    assertThat(item1.traineeCount()).isEqualTo(950L);
    assertThat(item1.completedCount()).isEqualTo(900L);
    assertThat(item1.certifiedCount()).isEqualTo(850L);
    assertThat(item1.joinedPlacementCount()).isEqualTo(800L);
    assertThat(item1.employedCount()).isEqualTo(750L);
    assertThat(item1.completionRatePct()).isEqualByComparingTo(new BigDecimal("90.00"));
    assertThat(item1.certificationRatePct()).isEqualByComparingTo(new BigDecimal("85.00"));
    assertThat(item1.placementRatePct()).isEqualByComparingTo(new BigDecimal("80.00"));
    assertThat(item1.employmentRatePct()).isEqualByComparingTo(new BigDecimal("75.00"));
    assertThat(item1.activeBatchCapacity()).isEqualTo(200L);

    ProviderPerformanceResponse item2 = response.items().get(1);
    assertThat(item2.rank()).isEqualTo(2);
    assertThat(item2.providerId()).isEqualTo(2L);
    assertThat(item2.employmentRatePct()).isEqualByComparingTo(new BigDecimal("60.00"));
    assertThat(item2.activeBatchCapacity()).isEqualTo(100L);

    verify(institutionalAnalyticsRepository).getFormalQ04ProviderLeaderboard();
  }

  @Test
  @DisplayName("Course Leaderboard: maps projections, assigns ranks, computes dropout rate and preserves wage")
  void testCourseLeaderboardMappingAndRanking() {
    CourseEmployabilityProjection c1 = new CourseEmployabilityProjection(
        101L, "CRS-WEB", "Full Stack Web Development",
        1L, "NSQF-4", "NSQF Level 4", 4,
        200L, 190L, 160L, 150L, 140L, 130L,
        new BigDecimal("80.00"), new BigDecimal("75.00"), new BigDecimal("70.00"), new BigDecimal("65.00"),
        new BigDecimal("35000.00")
    );

    when(institutionalAnalyticsRepository.getFormalQ05CourseEmployability()).thenReturn(List.of(c1));

    LeaderboardResponse<CourseEmployabilityResponse> response = institutionalAnalyticsService.getCourseLeaderboard();

    assertThat(response).isNotNull();
    assertThat(response.rankingMetric()).isEqualTo("employment_rate_pct DESC, enrollment_count DESC");
    assertThat(response.totalItems()).isEqualTo(1);
    assertThat(response.items()).hasSize(1);

    CourseEmployabilityResponse item = response.items().get(0);
    assertThat(item.rank()).isEqualTo(1);
    assertThat(item.courseId()).isEqualTo(101L);
    assertThat(item.courseCode()).isEqualTo("CRS-WEB");
    assertThat(item.qualificationLevelId()).isEqualTo(1L);
    assertThat(item.qualificationLevelCode()).isEqualTo("NSQF-4");
    assertThat(item.qualificationLevelName()).isEqualTo("NSQF Level 4");
    assertThat(item.nsqfLevel()).isEqualTo(4);
    assertThat(item.enrollmentCount()).isEqualTo(200L);
    assertThat(item.traineeCount()).isEqualTo(190L);
    assertThat(item.completedCount()).isEqualTo(160L);
    assertThat(item.certifiedCount()).isEqualTo(150L);
    assertThat(item.joinedPlacementCount()).isEqualTo(140L);
    assertThat(item.employedCount()).isEqualTo(130L);
    assertThat(item.completionRatePct()).isEqualByComparingTo(new BigDecimal("80.00"));
    assertThat(item.certificationRatePct()).isEqualByComparingTo(new BigDecimal("75.00"));
    assertThat(item.placementRatePct()).isEqualByComparingTo(new BigDecimal("70.00"));
    assertThat(item.employmentRatePct()).isEqualByComparingTo(new BigDecimal("65.00"));
    // Dropout rate: (200 - 160) / 200 * 100 = 40 / 200 * 100 = 20.00%
    assertThat(item.dropoutRatePct()).isEqualByComparingTo(new BigDecimal("20.00"));
    assertThat(item.avgStartingWage()).isEqualByComparingTo(new BigDecimal("35000.00"));

    verify(institutionalAnalyticsRepository).getFormalQ05CourseEmployability();
  }

  @Test
  @DisplayName("Dropout calculation: positive enrollment and positive completed count calculates dropout from counts")
  void testDropoutRatePositiveEnrollmentPositiveCompletedCount() {
    CourseEmployabilityProjection c = new CourseEmployabilityProjection(
        101L, "CRS-POS", "Positive Course",
        1L, "NSQF-4", "NSQF Level 4", 4,
        200L, 190L, 160L, 150L, 140L, 130L,
        new BigDecimal("80.00"), new BigDecimal("75.00"), new BigDecimal("70.00"), new BigDecimal("65.00"),
        new BigDecimal("35000.00")
    );

    when(institutionalAnalyticsRepository.getFormalQ05CourseEmployability()).thenReturn(List.of(c));

    LeaderboardResponse<CourseEmployabilityResponse> response = institutionalAnalyticsService.getCourseLeaderboard();

    CourseEmployabilityResponse item = response.items().get(0);
    assertThat(item.enrollmentCount()).isEqualTo(200L);
    assertThat(item.completedCount()).isEqualTo(160L);
    // (200 - 160) / 200 * 100 = 20.00%
    assertThat(item.dropoutRatePct()).isEqualByComparingTo(new BigDecimal("20.00"));
  }

  @Test
  @DisplayName("Dropout calculation: positive enrollment and genuinely zero completed count calculates 100.00% dropout")
  void testDropoutRatePositiveEnrollmentZeroCompletedCount() {
    CourseEmployabilityProjection c = new CourseEmployabilityProjection(
        102L, "CRS-ZERO-COMP", "Zero Completed Course",
        2L, "NSQF-5", "NSQF Level 5", 5,
        100L, 95L, 0L, 0L, 0L, 0L,
        new BigDecimal("0.00"), new BigDecimal("0.00"), new BigDecimal("0.00"), new BigDecimal("0.00"),
        null
    );

    when(institutionalAnalyticsRepository.getFormalQ05CourseEmployability()).thenReturn(List.of(c));

    LeaderboardResponse<CourseEmployabilityResponse> response = institutionalAnalyticsService.getCourseLeaderboard();

    CourseEmployabilityResponse item = response.items().get(0);
    assertThat(item.enrollmentCount()).isEqualTo(100L);
    assertThat(item.completedCount()).isEqualTo(0L);
    // (100 - 0) / 100 * 100 = 100.00%
    assertThat(item.dropoutRatePct()).isEqualByComparingTo(new BigDecimal("100.00"));
  }

  @Test
  @DisplayName("Dropout calculation: positive enrollment with null completed count falls back to 100.00 - completionRatePct")
  void testDropoutRatePositiveEnrollmentNullCompletedCountWithCompletionRate() {
    CourseEmployabilityProjection c = new CourseEmployabilityProjection(
        103L, "CRS-PARTIAL", "Course Partial Data",
        2L, "NSQF-5", "NSQF Level 5", 5,
        100L, 90L, null, null, null, null,
        new BigDecimal("85.50"), null, null, null, null
    );

    when(institutionalAnalyticsRepository.getFormalQ05CourseEmployability()).thenReturn(List.of(c));

    LeaderboardResponse<CourseEmployabilityResponse> response = institutionalAnalyticsService.getCourseLeaderboard();

    CourseEmployabilityResponse item = response.items().get(0);
    assertThat(item.enrollmentCount()).isEqualTo(100L);
    assertThat(item.completedCount()).isNull();
    // 100.00 - 85.50 = 14.50%
    assertThat(item.dropoutRatePct()).isEqualByComparingTo(new BigDecimal("14.50"));
  }

  @Test
  @DisplayName("Dropout calculation: positive enrollment with null completed count and null completion rate returns null")
  void testDropoutRatePositiveEnrollmentNullCompletedCountNullCompletionRate() {
    CourseEmployabilityProjection c = new CourseEmployabilityProjection(
        104L, "CRS-NULL-BOTH", "Null Completed and Null Rate",
        2L, "NSQF-5", "NSQF Level 5", 5,
        100L, 90L, null, null, null, null,
        null, null, null, null, null
    );

    when(institutionalAnalyticsRepository.getFormalQ05CourseEmployability()).thenReturn(List.of(c));

    LeaderboardResponse<CourseEmployabilityResponse> response = institutionalAnalyticsService.getCourseLeaderboard();

    CourseEmployabilityResponse item = response.items().get(0);
    assertThat(item.enrollmentCount()).isEqualTo(100L);
    assertThat(item.completedCount()).isNull();
    assertThat(item.dropoutRatePct()).isNull();
  }

  @Test
  @DisplayName("Dropout calculation: zero enrollment safely returns null dropout rate")
  void testDropoutRateZeroEnrollment() {
    CourseEmployabilityProjection c = new CourseEmployabilityProjection(
        105L, "CRS-ZERO-ENR", "Zero Enrollment Course",
        2L, "NSQF-5", "NSQF Level 5", 5,
        0L, 0L, 0L, 0L, 0L, 0L,
        null, null, null, null, null
    );

    when(institutionalAnalyticsRepository.getFormalQ05CourseEmployability()).thenReturn(List.of(c));

    LeaderboardResponse<CourseEmployabilityResponse> response = institutionalAnalyticsService.getCourseLeaderboard();

    assertThat(response.items()).hasSize(1);
    CourseEmployabilityResponse item = response.items().get(0);
    assertThat(item.enrollmentCount()).isEqualTo(0L);
    assertThat(item.dropoutRatePct()).isNull();
    assertThat(item.avgStartingWage()).isNull();
  }

  @Test
  @DisplayName("Dropout calculation: null enrollment safely returns null dropout rate")
  void testDropoutRateNullEnrollment() {
    CourseEmployabilityProjection c = new CourseEmployabilityProjection(
        106L, "CRS-NULL-ENR", "Null Enrollment Course",
        2L, "NSQF-5", "NSQF Level 5", 5,
        null, 0L, null, null, null, null,
        new BigDecimal("80.00"), null, null, null, null
    );

    when(institutionalAnalyticsRepository.getFormalQ05CourseEmployability()).thenReturn(List.of(c));

    LeaderboardResponse<CourseEmployabilityResponse> response = institutionalAnalyticsService.getCourseLeaderboard();

    assertThat(response.items()).hasSize(1);
    CourseEmployabilityResponse item = response.items().get(0);
    assertThat(item.enrollmentCount()).isEqualTo(0L);
    assertThat(item.dropoutRatePct()).isNull();
  }

  @Test
  @DisplayName("Course Leaderboard: missing wage data safely remains null without converting to zero")
  void testMissingWageDataReturnsNull() {
    CourseEmployabilityProjection c = new CourseEmployabilityProjection(
        104L, "CRS-NOWAGE", "No Wage Course",
        null, null, null, null,
        50L, 50L, 40L, 35L, 30L, 25L,
        new BigDecimal("80.00"), new BigDecimal("70.00"), new BigDecimal("60.00"), new BigDecimal("50.00"),
        null
    );

    when(institutionalAnalyticsRepository.getFormalQ05CourseEmployability()).thenReturn(List.of(c));

    LeaderboardResponse<CourseEmployabilityResponse> response = institutionalAnalyticsService.getCourseLeaderboard();

    assertThat(response.items().get(0).avgStartingWage()).isNull();
  }

  @Test
  @DisplayName("Tie-Breaking Order: preserves repository ordering for identical employment rates")
  void testTieBreakingOrder() {
    ProviderPerformanceProjection p1 = new ProviderPerformanceProjection(
        1L, "TP-A", "Provider A", 2000L, 1900L, 1800L, 1700L, 1600L, 1500L,
        new BigDecimal("90.00"), new BigDecimal("85.00"), new BigDecimal("80.00"), new BigDecimal("75.00"), 500L
    );
    ProviderPerformanceProjection p2 = new ProviderPerformanceProjection(
        2L, "TP-B", "Provider B", 1000L, 950L, 900L, 850L, 800L, 750L,
        new BigDecimal("90.00"), new BigDecimal("85.00"), new BigDecimal("80.00"), new BigDecimal("75.00"), 200L
    );

    when(institutionalAnalyticsRepository.getFormalQ04ProviderLeaderboard()).thenReturn(List.of(p1, p2));

    LeaderboardResponse<ProviderPerformanceResponse> response = institutionalAnalyticsService.getProviderLeaderboard();

    assertThat(response.items()).hasSize(2);
    assertThat(response.items().get(0).providerCode()).isEqualTo("TP-A");
    assertThat(response.items().get(0).rank()).isEqualTo(1);
    assertThat(response.items().get(1).providerCode()).isEqualTo("TP-B");
    assertThat(response.items().get(1).rank()).isEqualTo(2);
  }

  @Test
  @DisplayName("Empty Results: returns empty collections safely without NPE")
  void testEmptyResults() {
    when(institutionalAnalyticsRepository.getFormalQ04ProviderLeaderboard()).thenReturn(Collections.emptyList());
    when(institutionalAnalyticsRepository.getFormalQ05CourseEmployability()).thenReturn(Collections.emptyList());

    LeaderboardResponse<ProviderPerformanceResponse> providers = institutionalAnalyticsService.getProviderLeaderboard();
    assertThat(providers.totalItems()).isEqualTo(0);
    assertThat(providers.items()).isEmpty();

    LeaderboardResponse<CourseEmployabilityResponse> courses = institutionalAnalyticsService.getCourseLeaderboard();
    assertThat(courses.totalItems()).isEqualTo(0);
    assertThat(courses.items()).isEmpty();

    Map<String, List<CourseEmployabilityResponse>> grouped =
        institutionalAnalyticsService.getCourseLeaderboardByQualificationLevel();
    assertThat(grouped).isEmpty();
  }

  @Test
  @DisplayName("Qualification Level Grouping: correctly groups courses by qualification level name")
  void testQualificationLevelGrouping() {
    CourseEmployabilityProjection c1 = new CourseEmployabilityProjection(
        1L, "C-1", "Course 1", 1L, "NSQF-4", "NSQF Level 4", 4,
        100L, 90L, 80L, 70L, 60L, 50L,
        new BigDecimal("80.00"), new BigDecimal("70.00"), new BigDecimal("60.00"), new BigDecimal("50.00"),
        new BigDecimal("30000.00")
    );
    CourseEmployabilityProjection c2 = new CourseEmployabilityProjection(
        2L, "C-2", "Course 2", 2L, "NSQF-5", "NSQF Level 5", 5,
        150L, 140L, 130L, 120L, 110L, 100L,
        new BigDecimal("86.67"), new BigDecimal("80.00"), new BigDecimal("73.33"), new BigDecimal("66.67"),
        new BigDecimal("45000.00")
    );
    CourseEmployabilityProjection c3 = new CourseEmployabilityProjection(
        3L, "C-3", "Course 3", 1L, "NSQF-4", "NSQF Level 4", 4,
        80L, 75L, 70L, 65L, 60L, 55L,
        new BigDecimal("87.50"), new BigDecimal("81.25"), new BigDecimal("75.00"), new BigDecimal("68.75"),
        new BigDecimal("32000.00")
    );

    when(institutionalAnalyticsRepository.getFormalQ05CourseEmployability()).thenReturn(List.of(c1, c2, c3));

    Map<String, List<CourseEmployabilityResponse>> grouped =
        institutionalAnalyticsService.getCourseLeaderboardByQualificationLevel();

    assertThat(grouped).hasSize(2);
    assertThat(grouped.keySet()).containsExactly("NSQF Level 4", "NSQF Level 5");
    assertThat(grouped.get("NSQF Level 4")).hasSize(2);
    assertThat(grouped.get("NSQF Level 5")).hasSize(1);
    assertThat(grouped.get("NSQF Level 4").get(0).courseCode()).isEqualTo("C-1");
    assertThat(grouped.get("NSQF Level 4").get(1).courseCode()).isEqualTo("C-3");
  }
}
