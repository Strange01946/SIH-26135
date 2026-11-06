package in.gov.sih.sih26135.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.gov.sih.sih26135.dto.response.DistrictGeographicResponse;
import in.gov.sih.sih26135.dto.response.DistrictPerformanceResponse;
import in.gov.sih.sih26135.dto.response.GeographicOverviewResponse;
import in.gov.sih.sih26135.dto.response.LeaderboardResponse;
import in.gov.sih.sih26135.dto.response.StateGeographicResponse;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.repository.analytics.GeographicDashboardRepository;
import in.gov.sih.sih26135.repository.analytics.projection.DistrictPerformanceProjection;
import in.gov.sih.sih26135.repository.analytics.projection.StatePerformanceProjection;
import in.gov.sih.sih26135.repository.analytics.projection.TrainingCenterSummaryProjection;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import in.gov.sih.sih26135.service.impl.GeographicDashboardServiceImpl;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("GeographicDashboardService Unit Tests")
class GeographicDashboardServiceTest {

  @Mock
  private GeographicDashboardRepository geographicDashboardRepository;

  @Mock
  private WorkflowSecurityScopeValidator scopeValidator;

  private GeographicDashboardServiceImpl geographicDashboardService;

  @BeforeEach
  void setUp() {
    geographicDashboardService = new GeographicDashboardServiceImpl(geographicDashboardRepository, scopeValidator);
  }

  @Nested
  @DisplayName("National Geographic Overview Tests (GET /geography)")
  class NationalGeographyTests {

    @Test
    @DisplayName("Aggregates state and district projections into consistent national overview")
    void testGetNationalGeographicOverview() {
      StatePerformanceProjection state1 = new StatePerformanceProjection(
          1L, "MH", "Maharashtra", 2L, 1000L, 950L, 800L, 750L, 600L, 500L,
          new BigDecimal("80.00"), new BigDecimal("75.00"), new BigDecimal("60.00"), new BigDecimal("50.00"), 10L
      );
      StatePerformanceProjection state2 = new StatePerformanceProjection(
          2L, "KA", "Karnataka", 1L, 500L, 480L, 400L, 380L, 300L, 260L,
          new BigDecimal("80.00"), new BigDecimal("76.00"), new BigDecimal("60.00"), new BigDecimal("52.00"), 5L
      );

      DistrictPerformanceProjection dist1 = new DistrictPerformanceProjection(
          101L, "DIST-01", "Pune", 1L, "MH", "Maharashtra",
          600L, 580L, 500L, 480L, 400L, 350L,
          new BigDecimal("83.33"), new BigDecimal("80.00"), new BigDecimal("66.67"), new BigDecimal("58.33")
      );
      DistrictPerformanceProjection dist2 = new DistrictPerformanceProjection(
          102L, "DIST-02", "Nashik", 1L, "MH", "Maharashtra",
          400L, 370L, 300L, 270L, 200L, 150L,
          new BigDecimal("75.00"), new BigDecimal("67.50"), new BigDecimal("50.00"), new BigDecimal("37.50")
      );

      when(geographicDashboardRepository.getStateSummaries()).thenReturn(List.of(state1, state2));
      when(geographicDashboardRepository.getFormalQ06DistrictOutcomes()).thenReturn(List.of(dist1, dist2));
      when(geographicDashboardRepository.countDistrictsWithZeroEnrollments()).thenReturn(3L);
      when(geographicDashboardRepository.countDistrictsWithoutTrainingCenters()).thenReturn(2L);

      GeographicOverviewResponse response = geographicDashboardService.getNationalGeographicOverview();

      assertThat(response).isNotNull();
      assertThat(response.totalStates()).isEqualTo(2L);
      assertThat(response.totalDistricts()).isEqualTo(3L); // 2 + 1
      assertThat(response.totalEnrollments()).isEqualTo(1500L); // 1000 + 500
      assertThat(response.totalCompleted()).isEqualTo(1200L); // 800 + 400
      assertThat(response.totalCertified()).isEqualTo(1130L); // 750 + 380
      assertThat(response.totalJoinedPlacement()).isEqualTo(900L); // 600 + 300
      assertThat(response.totalEmployed()).isEqualTo(760L); // 500 + 260

      // National macro rates
      assertThat(response.overallCompletionRatePct()).isEqualByComparingTo(new BigDecimal("80.00"));
      assertThat(response.overallCertificationRatePct()).isEqualByComparingTo(new BigDecimal("75.33"));
      assertThat(response.overallPlacementRatePct()).isEqualByComparingTo(new BigDecimal("60.00"));
      assertThat(response.overallEmploymentRatePct()).isEqualByComparingTo(new BigDecimal("50.67"));

      // State summaries & rankings
      assertThat(response.stateSummaries()).hasSize(2);
      assertThat(response.stateSummaries().get(0).rank()).isEqualTo(1);
      assertThat(response.stateSummaries().get(1).rank()).isEqualTo(2);

      // District rankings (Q06)
      assertThat(response.districtRankings().items()).hasSize(2);
      assertThat(response.districtRankings().rankingMetric()).contains("employmentRatePct");
      assertThat(response.districtRankings().items().get(0).rank()).isEqualTo(1);
      assertThat(response.districtRankings().items().get(1).rank()).isEqualTo(2);

      // Disparities
      assertThat(response.regionalDisparity().districtsWithZeroEnrollment()).isEqualTo(3L);
      assertThat(response.regionalDisparity().districtsWithoutTrainingCenters()).isEqualTo(2L);
      assertThat(response.regionalDisparity().dataAvailabilityNotes()).containsKey("ruralUrbanClassification");
      assertThat(response.regionalDisparity().dataAvailabilityNotes()).containsKey("aspirationalDistrictIndicator");
      assertThat(response.regionalDisparity().dataAvailabilityNotes()).containsKey("populationDensity");
    }

    @Test
    @DisplayName("Handles empty geographic data gracefully without NullPointerException")
    void testGetNationalGeographicOverviewEmpty() {
      when(geographicDashboardRepository.getStateSummaries()).thenReturn(List.of());
      when(geographicDashboardRepository.getFormalQ06DistrictOutcomes()).thenReturn(List.of());
      when(geographicDashboardRepository.countDistrictsWithZeroEnrollments()).thenReturn(0L);
      when(geographicDashboardRepository.countDistrictsWithoutTrainingCenters()).thenReturn(0L);

      GeographicOverviewResponse response = geographicDashboardService.getNationalGeographicOverview();

      assertThat(response).isNotNull();
      assertThat(response.totalStates()).isEqualTo(0L);
      assertThat(response.totalDistricts()).isEqualTo(0L);
      assertThat(response.totalEnrollments()).isEqualTo(0L);
      assertThat(response.overallCompletionRatePct()).isNull();
      assertThat(response.overallEmploymentRatePct()).isNull();
      assertThat(response.stateSummaries()).isEmpty();
      assertThat(response.districtRankings().items()).isEmpty();
    }
  }

  @Nested
  @DisplayName("State Geographic Analytics Tests (GET /states/{stateId})")
  class StateGeographyTests {

    @Test
    @DisplayName("Retrieves state-specific outcome and ordered districts")
    void testGetStateGeographicAnalyticsSuccess() {
      Long stateId = 1L;
      StatePerformanceProjection stateProj = new StatePerformanceProjection(
          stateId, "MH", "Maharashtra", 2L, 1000L, 950L, 800L, 750L, 600L, 500L,
          new BigDecimal("80.00"), new BigDecimal("75.00"), new BigDecimal("60.00"), new BigDecimal("50.00"), 8L
      );

      DistrictPerformanceProjection d1 = new DistrictPerformanceProjection(
          101L, "DIST-01", "Pune", stateId, "MH", "Maharashtra",
          600L, 580L, 500L, 480L, 400L, 350L,
          new BigDecimal("83.33"), new BigDecimal("80.00"), new BigDecimal("66.67"), new BigDecimal("58.33")
      );
      DistrictPerformanceProjection d2 = new DistrictPerformanceProjection(
          102L, "DIST-02", "Nashik", stateId, "MH", "Maharashtra",
          400L, 370L, 300L, 270L, 200L, 150L,
          new BigDecimal("75.00"), new BigDecimal("67.50"), new BigDecimal("50.00"), new BigDecimal("37.50")
      );

      when(geographicDashboardRepository.existsStateById(stateId)).thenReturn(true);
      when(geographicDashboardRepository.getStateSummaryById(stateId)).thenReturn(stateProj);
      when(geographicDashboardRepository.getDistrictsByStateId(stateId)).thenReturn(List.of(d1, d2));

      StateGeographicResponse response = geographicDashboardService.getStateGeographicAnalytics(stateId);

      assertThat(response).isNotNull();
      assertThat(response.stateId()).isEqualTo(stateId);
      assertThat(response.stateCode()).isEqualTo("MH");
      assertThat(response.stateName()).isEqualTo("Maharashtra");
      assertThat(response.totalTrainingCenters()).isEqualTo(8L);
      assertThat(response.districts()).hasSize(2);
      // Within-state sorting by employment rate descending: Pune (58.33%) rank 1, Nashik (37.50%) rank 2
      assertThat(response.districts().get(0).districtName()).isEqualTo("Pune");
      assertThat(response.districts().get(0).rank()).isEqualTo(1);
      assertThat(response.districts().get(1).districtName()).isEqualTo("Nashik");
      assertThat(response.districts().get(1).rank()).isEqualTo(2);
    }

    @Test
    @DisplayName("Throws ResourceNotFoundException when stateId does not exist")
    void testGetStateGeographicAnalyticsNotFound() {
      Long stateId = 9999L;
      when(geographicDashboardRepository.existsStateById(stateId)).thenReturn(false);

      assertThatThrownBy(() -> geographicDashboardService.getStateGeographicAnalytics(stateId))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessageContaining("State not found with ID: 9999");
    }
  }

  @Nested
  @DisplayName("District Geographic Analytics Tests (GET /districts/{districtId})")
  class DistrictGeographyTests {

    @Test
    @DisplayName("Retrieves district-specific performance, national and state ranks, and training centers")
    void testGetDistrictGeographicAnalyticsSuccess() {
      Long districtId = 101L;
      Long stateId = 1L;

      DistrictPerformanceProjection distProj = new DistrictPerformanceProjection(
          districtId, "DIST-01", "Pune", stateId, "MH", "Maharashtra",
          600L, 580L, 500L, 480L, 400L, 350L,
          new BigDecimal("83.33"), new BigDecimal("80.00"), new BigDecimal("66.67"), new BigDecimal("58.33")
      );

      DistrictPerformanceProjection distOther = new DistrictPerformanceProjection(
          102L, "DIST-02", "Nashik", stateId, "MH", "Maharashtra",
          400L, 370L, 300L, 270L, 200L, 150L,
          new BigDecimal("75.00"), new BigDecimal("67.50"), new BigDecimal("50.00"), new BigDecimal("37.50")
      );

      TrainingCenterSummaryProjection tc1 = new TrainingCenterSummaryProjection(
          1L, "TC-01", "Pune Center A", 100, "411001"
      );

      when(geographicDashboardRepository.existsDistrictById(districtId)).thenReturn(true);
      when(geographicDashboardRepository.getDistrictById(districtId)).thenReturn(distProj);
      when(geographicDashboardRepository.getTrainingCentersByDistrictId(districtId)).thenReturn(List.of(tc1));
      when(geographicDashboardRepository.getFormalQ06DistrictOutcomes()).thenReturn(List.of(distOther, distProj)); // distProj is 2nd in Q06 ASC
      when(geographicDashboardRepository.getDistrictsByStateId(stateId)).thenReturn(List.of(distProj, distOther)); // distProj is 1st in state DESC

      DistrictGeographicResponse response = geographicDashboardService.getDistrictGeographicAnalytics(districtId);

      assertThat(response).isNotNull();
      assertThat(response.districtId()).isEqualTo(districtId);
      assertThat(response.districtCode()).isEqualTo("DIST-01");
      assertThat(response.districtName()).isEqualTo("Pune");
      assertThat(response.stateId()).isEqualTo(stateId);
      assertThat(response.trainingCenterCount()).isEqualTo(1L);
      assertThat(response.trainingCenters()).hasSize(1);
      assertThat(response.trainingCenters().get(0).centerCode()).isEqualTo("TC-01");
      assertThat(response.nationalRank()).isEqualTo(2);
      assertThat(response.stateRank()).isEqualTo(1);
      assertThat(response.regionalIndicators()).containsEntry("hasTrainingCenter", true);
      assertThat(response.regionalIndicators()).containsEntry("hasEnrollment", true);
    }

    @Test
    @DisplayName("Throws ResourceNotFoundException when districtId does not exist")
    void testGetDistrictGeographicAnalyticsNotFound() {
      Long districtId = 9999L;
      when(geographicDashboardRepository.existsDistrictById(districtId)).thenReturn(false);

      assertThatThrownBy(() -> geographicDashboardService.getDistrictGeographicAnalytics(districtId))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessageContaining("District not found with ID: 9999");
    }
  }

  @Nested
  @DisplayName("District Rankings Leaderboard Tests")
  class DistrictRankingsTests {

    @Test
    @DisplayName("Retrieves formal Q06 district outcome rankings wrapped in LeaderboardResponse")
    void testGetDistrictRankings() {
      DistrictPerformanceProjection dist1 = new DistrictPerformanceProjection(
          101L, "DIST-01", "Pune", 1L, "MH", "Maharashtra",
          600L, 580L, 500L, 480L, 400L, 350L,
          new BigDecimal("83.33"), new BigDecimal("80.00"), new BigDecimal("66.67"), new BigDecimal("58.33")
      );

      when(geographicDashboardRepository.getFormalQ06DistrictOutcomes()).thenReturn(List.of(dist1));

      LeaderboardResponse<DistrictPerformanceResponse> leaderboard =
          geographicDashboardService.getDistrictRankings();

      assertThat(leaderboard).isNotNull();
      assertThat(leaderboard.totalItems()).isEqualTo(1L);
      assertThat(leaderboard.rankingMetric()).contains("employmentRatePct");
      assertThat(leaderboard.items().get(0).districtName()).isEqualTo("Pune");
      assertThat(leaderboard.items().get(0).rank()).isEqualTo(1);

      verify(geographicDashboardRepository).getFormalQ06DistrictOutcomes();
    }
  }
}
