package in.gov.sih.sih26135.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.gov.sih.sih26135.MysqlConnectionTest;
import in.gov.sih.sih26135.dto.response.SkillGapDashboardResponse;
import in.gov.sih.sih26135.dto.response.SkillGapSeverityBreakdownResponse;
import in.gov.sih.sih26135.dto.response.TopSkillGapResponse;
import in.gov.sih.sih26135.repository.analytics.SkillGapDashboardRepository;
import in.gov.sih.sih26135.repository.analytics.projection.SkillGapSeverityMixProjection;
import in.gov.sih.sih26135.repository.analytics.projection.SkillGapSummaryProjection;
import in.gov.sih.sih26135.repository.analytics.projection.TopSkillGapProjection;
import in.gov.sih.sih26135.security.jwt.JwtTokenProvider;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Live database integration tests for Skill Gap, Industry Demand Alignment,
 * and Remediation Analytics Dashboards.
 *
 * <p>Verifies exact formal queries Q13 and Q14 execution, mathematical reconciliation,
 * and REST endpoint behavior against dedicated test database catalog {@code SIH26135_TEST}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("SkillGapDashboard Live Database Integration Tests (SIH26135_TEST)")
class SkillGapDashboardIntegrationTest {

  @Autowired
  private DataSource dataSource;

  @Autowired
  private SkillGapDashboardService skillGapDashboardService;

  @Autowired
  private SkillGapDashboardRepository skillGapDashboardRepository;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  private String tokenAnalyticsRead;

  @BeforeEach
  void enforceDatabaseIsolationGuardAndSetupTokens() throws Exception {
    try (Connection connection = dataSource.getConnection()) {
      MysqlConnectionTest.assertDedicatedTestCatalog(connection.getCatalog());
      assertThat(connection.getCatalog()).isEqualToIgnoringCase(MysqlConnectionTest.EXPECTED_TEST_CATALOG);
    }

    tokenAnalyticsRead = "Bearer " + jwtTokenProvider.generateAccessToken(
        1L,
        "executive",
        List.of("analytics.read")
    );
  }

  @Test
  @DisplayName("Repository: execute formal Q13 top skill gaps query against live SIH26135_TEST")
  void testLiveFormalQ13Execution() {
    List<TopSkillGapProjection> topGaps = skillGapDashboardRepository.getFormalQ13TopSkillGaps();

    assertThat(topGaps).isNotNull();
    assertThat(topGaps.size()).isLessThanOrEqualTo(20);

    for (int i = 0; i < topGaps.size(); i++) {
      TopSkillGapProjection p = topGaps.get(i);
      assertThat(p.skillId()).isNotNull().isPositive();
      assertThat(p.skillCode()).isNotBlank();
      assertThat(p.severityCode()).isNotBlank();

      if (p.gapCount() != null) {
        assertThat(p.gapCount()).isGreaterThanOrEqualTo(0L);
      }
      if (p.traineeCount() != null) {
        assertThat(p.traineeCount()).isGreaterThanOrEqualTo(0L);
      }
      if (p.currentGapCount() != null) {
        assertThat(p.currentGapCount()).isGreaterThanOrEqualTo(0L);
      }

      // Check ordering invariants
      if (i > 0) {
        TopSkillGapProjection prev = topGaps.get(i - 1);
        long currCurrent = p.currentGapCount() != null ? p.currentGapCount() : 0L;
        long prevCurrent = prev.currentGapCount() != null ? prev.currentGapCount() : 0L;
        assertThat(prevCurrent)
            .as("Q13 must sort descending by current_gap_count")
            .isGreaterThanOrEqualTo(currCurrent);
      }
    }
  }

  @Test
  @DisplayName("Repository: execute formal Q14 severity mix and uptake query against live SIH26135_TEST")
  void testLiveFormalQ14Execution() {
    List<SkillGapSeverityMixProjection> severities =
        skillGapDashboardRepository.getFormalQ14SeverityAndUptakeMix();

    assertThat(severities).isNotNull();

    for (SkillGapSeverityMixProjection s : severities) {
      assertThat(s.severityCode()).isNotBlank();
      if (s.currentGapCount() != null) {
        assertThat(s.currentGapCount()).isGreaterThanOrEqualTo(0L);
      }
      if (s.traineeCount() != null) {
        assertThat(s.traineeCount()).isGreaterThanOrEqualTo(0L);
      }
      if (s.recommendationCount() != null) {
        assertThat(s.recommendationCount()).isGreaterThanOrEqualTo(0L);
      }
      if (s.acceptedRecommendationCount() != null) {
        assertThat(s.acceptedRecommendationCount()).isGreaterThanOrEqualTo(0L);
      }

      // Uptake invariant: accepted recommendations cannot exceed assigned recommendations
      if (s.acceptedRecommendationCount() != null && s.recommendationCount() != null) {
        assertThat(s.acceptedRecommendationCount())
            .as("Accepted recommendations cannot exceed assigned recommendations")
            .isLessThanOrEqualTo(s.recommendationCount());
      }
    }
  }

  @Test
  @DisplayName("Repository: execute formal Q14 overall summary query against live SIH26135_TEST")
  void testLiveFormalQ14OverallSummaryExecution() {
    SkillGapSummaryProjection summary = skillGapDashboardRepository.getFormalQ14OverallSummary();

    assertThat(summary).isNotNull();
    if (summary.totalCurrentGaps() != null) {
      assertThat(summary.totalCurrentGaps()).isGreaterThanOrEqualTo(0L);
    }
    if (summary.totalAffectedTrainees() != null) {
      assertThat(summary.totalAffectedTrainees()).isGreaterThanOrEqualTo(0L);
    }
    if (summary.totalRecommendations() != null) {
      assertThat(summary.totalRecommendations()).isGreaterThanOrEqualTo(0L);
    }
    if (summary.totalAcceptedRecommendations() != null) {
      assertThat(summary.totalAcceptedRecommendations()).isGreaterThanOrEqualTo(0L);
    }

    if (summary.totalAcceptedRecommendations() != null && summary.totalRecommendations() != null) {
      assertThat(summary.totalAcceptedRecommendations())
          .isLessThanOrEqualTo(summary.totalRecommendations());
    }
  }

  @Test
  @DisplayName("Service: live retrieval and mathematical reconciliation")
  void testLiveServiceExecutionAndReconciliation() {
    SkillGapDashboardResponse response = skillGapDashboardService.getSkillGapDashboard();

    assertThat(response).isNotNull();
    assertThat(response.totalCurrentGaps()).isNotNull().isGreaterThanOrEqualTo(0L);
    assertThat(response.totalAffectedTrainees()).isNotNull().isGreaterThanOrEqualTo(0L);
    assertThat(response.totalRecommendations()).isNotNull().isGreaterThanOrEqualTo(0L);
    assertThat(response.totalAcceptedRecommendations()).isNotNull().isGreaterThanOrEqualTo(0L);

    if (response.totalRecommendations() > 0) {
      assertThat(response.overallAcceptanceRatePct()).isNotNull();
      assertThat(response.overallAcceptanceRatePct())
          .isBetween(BigDecimal.ZERO, new BigDecimal("100.00"));
    } else {
      assertThat(response.overallAcceptanceRatePct()).isNull();
    }

    List<SkillGapSeverityBreakdownResponse> severities = response.severityBreakdown();
    assertThat(severities).isNotNull();

    // Verify share sum does not exceed 100.05% due to rounding
    if (response.totalCurrentGaps() > 0 && !severities.isEmpty()) {
      BigDecimal totalShare = BigDecimal.ZERO;
      for (SkillGapSeverityBreakdownResponse s : severities) {
        if (s.sharePct() != null) {
          totalShare = totalShare.add(s.sharePct());
        }
      }
      assertThat(totalShare).isLessThanOrEqualTo(new BigDecimal("100.05"));
    }
  }

  @Test
  @DisplayName("REST Endpoint: GET /api/v1/dashboards/skill-gaps returns 200 OK with live data")
  void testLiveRestEndpointHttpExecution() throws Exception {
    mockMvc.perform(get("/api/v1/dashboards/skill-gaps")
            .header(HttpHeaders.AUTHORIZATION, tokenAnalyticsRead)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("Skill gap analytics retrieved successfully"))
        .andExpect(jsonPath("$.path").value("/api/v1/dashboards/skill-gaps"))
        .andExpect(jsonPath("$.data").exists())
        .andExpect(jsonPath("$.data.totalCurrentGaps").isNumber())
        .andExpect(jsonPath("$.data.topSkillGaps").isArray())
        .andExpect(jsonPath("$.data.severityBreakdown").isArray());
  }
}
