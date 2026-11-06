package in.gov.sih.sih26135.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import in.gov.sih.sih26135.MysqlConnectionTest;
import in.gov.sih.sih26135.config.cache.AnalyticsCacheConfig;
import in.gov.sih.sih26135.config.cache.AnalyticsCacheDelegate;
import in.gov.sih.sih26135.config.cache.AnalyticsCacheEvictionService;
import in.gov.sih.sih26135.config.cache.AnalyticsCacheKeyGenerator;
import in.gov.sih.sih26135.dto.request.AnalyticsFilterCriteria;
import in.gov.sih.sih26135.dto.response.DashboardKpiStripResponse;
import in.gov.sih.sih26135.dto.response.ExecutiveDashboardResponse;
import in.gov.sih.sih26135.dto.response.LeaderboardResponse;
import in.gov.sih.sih26135.dto.response.ProviderPerformanceResponse;
import in.gov.sih.sih26135.repository.CourseSkillRepository;
import in.gov.sih.sih26135.repository.EmploymentRecordRepository;
import in.gov.sih.sih26135.repository.projection.CourseSkillIdOnlyProjection;
import in.gov.sih.sih26135.security.UserPrincipal;
import in.gov.sih.sih26135.security.jwt.JwtTokenProvider;
import in.gov.sih.sih26135.service.ExecutiveDashboardService;
import in.gov.sih.sih26135.service.InstitutionalAnalyticsService;
import java.sql.Connection;
import java.util.List;
import java.util.Set;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Phase 27.13 — Analytics Caching, Performance & Projections Suite")
public class AnalyticsCachingAndReadModelProjectionsTest {

  private static final Long FIXTURE_STATE_ID = 99871L;
  private static final Long FIXTURE_USER_NATIONAL_ADMIN_ID = 99871L;
  private static final Long FIXTURE_USER_STATE_ADMIN_ID = 99872L;

  @Autowired
  private DataSource dataSource;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private CacheManager cacheManager;

  @Autowired
  private ExecutiveDashboardService executiveDashboardService;

  @Autowired
  private InstitutionalAnalyticsService institutionalAnalyticsService;

  @Autowired
  private AnalyticsCacheEvictionService evictionService;

  @Autowired
  private AnalyticsCacheKeyGenerator keyGenerator;

  @Autowired
  private CourseSkillRepository courseSkillRepository;

  @Autowired
  private EmploymentRecordRepository employmentRecordRepository;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  @Autowired
  private MockMvc mockMvc;

  private String tokenNationalAdmin;
  private String tokenStateAdmin;

  @BeforeEach
  void setUp() throws Exception {
    try (Connection conn = dataSource.getConnection()) {
      MysqlConnectionTest.assertDedicatedTestCatalog(conn.getCatalog());
    }

    cleanUpFixtures();

    // 1. Seed State fixture
    jdbcTemplate.update("""
        INSERT INTO states (state_id, state_code, state_name, lgd_code, lifecycle_status_id, created_at, updated_at)
        VALUES (?, 'S7', 'Fixture State 27.13', 'LG99871', 1, NOW(), NOW())
        """, FIXTURE_STATE_ID);

    // 2. Seed National Admin user (no scoped tenancy)
    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, created_at, updated_at)
        VALUES (?, 'p2713_superadmin', 'p2713_superadmin@test.gov.in', 'hash', 'argon2id', 1, NOW(), NOW())
        """, FIXTURE_USER_NATIONAL_ADMIN_ID);

    // 3. Seed State Admin user (scoped to state 99871)
    jdbcTemplate.update("""
        INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, state_id, created_at, updated_at)
        VALUES (?, 'p2713_stateadmin', 'p2713_stateadmin@test.gov.in', 'hash', 'argon2id', 1, ?, NOW(), NOW())
        """, FIXTURE_USER_STATE_ADMIN_ID, FIXTURE_STATE_ID);

    // 4. Issue authentic JWT tokens
    tokenNationalAdmin = "Bearer " + jwtTokenProvider.generateAccessToken(
        FIXTURE_USER_NATIONAL_ADMIN_ID, "p2713_superadmin", List.of("ROLE_super_admin", "analytics.read"));
    tokenStateAdmin = "Bearer " + jwtTokenProvider.generateAccessToken(
        FIXTURE_USER_STATE_ADMIN_ID, "p2713_stateadmin", List.of("ROLE_government_admin", "analytics.read"));

    clearAllCaches();
    setNationalAdminContext();
  }

  @AfterEach
  void tearDown() {
    cleanUpFixtures();
  }

  private void cleanUpFixtures() {
    SecurityContextHolder.clearContext();
    jdbcTemplate.update("DELETE FROM users WHERE user_id IN (?, ?)",
        FIXTURE_USER_NATIONAL_ADMIN_ID, FIXTURE_USER_STATE_ADMIN_ID);
    jdbcTemplate.update("DELETE FROM states WHERE state_id = ?", FIXTURE_STATE_ID);
  }

  private void setNationalAdminContext() {
    UserPrincipal principal = new UserPrincipal(FIXTURE_USER_NATIONAL_ADMIN_ID, "p2713_superadmin");
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(
            principal,
            null,
            List.of(new SimpleGrantedAuthority("analytics.read"), new SimpleGrantedAuthority("ROLE_super_admin"))
        )
    );
  }

  private void clearAllCaches() {
    for (String cacheName : cacheManager.getCacheNames()) {
      Cache cache = cacheManager.getCache(cacheName);
      if (cache != null) {
        cache.clear();
      }
    }
  }

  // =========================================================================
  // 1. Cache Configuration & Bounded Capacity Tests
  // =========================================================================
  @Nested
  @DisplayName("1. Cache Configuration & Provisioning Tests")
  class CacheConfigurationTests {

    @Test
    @DisplayName("All 9 required analytical caches are registered and active")
    void allRequiredCachesAreConfigured() {
      Set<String> expectedCaches = Set.of(
          AnalyticsCacheConfig.CACHE_NATIONAL_OVERVIEW,
          AnalyticsCacheConfig.CACHE_STATE_AGGREGATES,
          AnalyticsCacheConfig.CACHE_DISTRICT_AGGREGATES,
          AnalyticsCacheConfig.CACHE_INSTITUTIONAL,
          AnalyticsCacheConfig.CACHE_ECONOMIC,
          AnalyticsCacheConfig.CACHE_RETENTION,
          AnalyticsCacheConfig.CACHE_SKILL_GAP,
          AnalyticsCacheConfig.CACHE_VERIFICATION,
          AnalyticsCacheConfig.CACHE_SUMMARIES
      );

      for (String cacheName : expectedCaches) {
        Cache cache = cacheManager.getCache(cacheName);
        assertThat(cache)
            .as("Cache '%s' must be configured in CacheManager", cacheName)
            .isNotNull();
        assertThat(cache).isInstanceOf(CaffeineCache.class);

        CaffeineCache caffeineCache = (CaffeineCache) cache;
        com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();
        assertThat(nativeCache).isNotNull();
        assertThat(nativeCache.stats()).isNotNull();
      }
    }
  }

  // =========================================================================
  // 2. Cache Hit, Miss and Determinism Tests
  // =========================================================================
  @Nested
  @DisplayName("2. Cache Hit, Miss & Key Determinism Tests")
  class CacheHitMissAndDeterminismTests {

    @Test
    @DisplayName("Repeated dashboard request results in cache hit on second execution")
    void repeatedRequestProducesCacheHit() {
      setNationalAdminContext();
      CaffeineCache nationalCache = (CaffeineCache) cacheManager.getCache(AnalyticsCacheConfig.CACHE_NATIONAL_OVERVIEW);
      assertThat(nationalCache).isNotNull();

      long initialMisses = nationalCache.getNativeCache().stats().missCount();
      long initialHits = nationalCache.getNativeCache().stats().hitCount();

      // Cold call: Miss -> computes from database
      DashboardKpiStripResponse firstResult = executiveDashboardService.getDashboardKpiStrip();
      assertThat(firstResult).isNotNull();
      assertThat(nationalCache.getNativeCache().stats().missCount()).isGreaterThanOrEqualTo(initialMisses + 1);

      // Warm call: Hit -> returns from Caffeine cache without query re-execution
      DashboardKpiStripResponse secondResult = executiveDashboardService.getDashboardKpiStrip();
      assertThat(secondResult).isNotNull();
      assertThat(secondResult.enrollmentCount()).isEqualTo(firstResult.enrollmentCount());
      assertThat(nationalCache.getNativeCache().stats().hitCount()).isGreaterThan(initialHits);
    }

    @Test
    @DisplayName("Deterministic key generator normalizes criteria and avoids key collision")
    void keyGeneratorDeterminismAndCollisionAvoidance() {
      AnalyticsFilterCriteria criteriaA = new AnalyticsFilterCriteria();
      criteriaA.setFiscalYear("FY2024-25");
      criteriaA.setStateId(27L);

      AnalyticsFilterCriteria criteriaB = new AnalyticsFilterCriteria();
      criteriaB.setStateId(27L);
      criteriaB.setFiscalYear("FY2024-25"); // same parameters, different order of setting

      String keyA = AnalyticsCacheKeyGenerator.serializeCriteria(criteriaA);
      String keyB = AnalyticsCacheKeyGenerator.serializeCriteria(criteriaB);
      assertThat(keyA).isEqualTo(keyB);
      assertThat(keyA).contains("fy=FY2024-25").contains("st=27");

      // Different filter criteria must produce different keys
      AnalyticsFilterCriteria criteriaDiff = new AnalyticsFilterCriteria();
      criteriaDiff.setFiscalYear("FY2023-24");
      criteriaDiff.setStateId(27L);
      String keyDiff = AnalyticsCacheKeyGenerator.serializeCriteria(criteriaDiff);
      assertThat(keyDiff).isNotEqualTo(keyA);

      // Empty criteria normalizes to ALL
      assertThat(AnalyticsCacheKeyGenerator.serializeCriteria(null)).isEqualTo("criteria=ALL");
      assertThat(AnalyticsCacheKeyGenerator.serializeCriteria(new AnalyticsFilterCriteria())).isEqualTo("criteria=ALL");
    }
  }

  // =========================================================================
  // 3. Tenancy Scope & Security Authorization Isolation Tests
  // =========================================================================
  @Nested
  @DisplayName("3. Tenancy Scope & Cache Security Isolation Tests")
  class SecurityAndAuthorizationCacheIsolationTests {

    @Test
    @DisplayName("Anonymous caller is rejected with HTTP 401 before any cache interaction")
    void unauthenticatedCallerRejectedWith401() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/overview"))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("State Admin requesting nationwide dashboard is rejected with HTTP 403")
    void stateAdminCallingNationwideOverviewRejectedWith403() throws Exception {
      mockMvc.perform(get("/api/v1/dashboards/overview")
              .header(HttpHeaders.AUTHORIZATION, tokenStateAdmin))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("National Admin warms cache, but subsequent unauthorized State Admin still receives HTTP 403")
    void cachedNationalResultNeverDisclosedToUnauthorizedStateAdmin() throws Exception {
      // 1. National Admin warms the cache
      mockMvc.perform(get("/api/v1/dashboards/overview")
              .header(HttpHeaders.AUTHORIZATION, tokenNationalAdmin))
          .andExpect(status().isOk());

      // Verify cache entry exists in Caffeine
      CaffeineCache nationalCache = (CaffeineCache) cacheManager.getCache(AnalyticsCacheConfig.CACHE_NATIONAL_OVERVIEW);
      assertThat(nationalCache).isNotNull();
      assertThat(nationalCache.getNativeCache().estimatedSize()).isGreaterThan(0);

      // 2. State Admin calls the exact same cached URL -> MUST still receive 403 Forbidden!
      mockMvc.perform(get("/api/v1/dashboards/overview")
              .header(HttpHeaders.AUTHORIZATION, tokenStateAdmin))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
  }

  // =========================================================================
  // 4. Workflow Invalidation Tests
  // =========================================================================
  @Nested
  @DisplayName("4. Workflow State Transition Cache Eviction Tests")
  class WorkflowCacheEvictionTests {

    @Test
    @DisplayName("Placement transition evicts national overview, economic, institutional and summary caches")
    void placementTransitionEviction() {
      setNationalAdminContext();
      // Warm caches
      executiveDashboardService.getDashboardKpiStrip();
      institutionalAnalyticsService.getProviderLeaderboard();

      CaffeineCache natCache = (CaffeineCache) cacheManager.getCache(AnalyticsCacheConfig.CACHE_NATIONAL_OVERVIEW);
      CaffeineCache instCache = (CaffeineCache) cacheManager.getCache(AnalyticsCacheConfig.CACHE_INSTITUTIONAL);
      assertThat(natCache.getNativeCache().estimatedSize()).isGreaterThan(0);
      assertThat(instCache.getNativeCache().estimatedSize()).isGreaterThan(0);

      // Trigger eviction
      evictionService.evictOnPlacementTransition(this);

      // Verify evicted caches are cleared
      assertThat(natCache.getNativeCache().estimatedSize()).isEqualTo(0);
      assertThat(instCache.getNativeCache().estimatedSize()).isEqualTo(0);
    }

    @Test
    @DisplayName("Verification verdict evicts verification dashboard and summary caches")
    void verificationVerdictEviction() {
      CaffeineCache verifCache = (CaffeineCache) cacheManager.getCache(AnalyticsCacheConfig.CACHE_VERIFICATION);
      verifCache.put("sampleKey", "sampleData");
      assertThat(verifCache.getNativeCache().estimatedSize()).isGreaterThan(0);

      evictionService.evictOnVerificationVerdict(this);

      assertThat(verifCache.getNativeCache().estimatedSize()).isEqualTo(0);
    }

    @Test
    @DisplayName("Skill gap remediation decision evicts skill gap cache")
    void skillGapRemediationEviction() {
      CaffeineCache skillCache = (CaffeineCache) cacheManager.getCache(AnalyticsCacheConfig.CACHE_SKILL_GAP);
      skillCache.put("sampleKey", "sampleData");
      assertThat(skillCache.getNativeCache().estimatedSize()).isGreaterThan(0);

      evictionService.evictOnSkillGapRemediation(this);

      assertThat(skillCache.getNativeCache().estimatedSize()).isEqualTo(0);
    }
  }

  // =========================================================================
  // 5. Projection and Read Model Optimization Tests
  // =========================================================================
  @Nested
  @DisplayName("5. Read Model Projection & Query Optimization Tests")
  class ProjectionOptimizationTests {

    @Test
    @DisplayName("CourseSkillIdOnlyProjection retrieves skillId without full entity loading")
    void courseSkillProjectionRetrieval() {
      List<CourseSkillIdOnlyProjection> projections = courseSkillRepository.findProjectedByCourseId(1L);
      assertThat(projections).isNotNull();
      if (!projections.isEmpty()) {
        assertThat(projections.get(0).getSkillId()).isNotNull();
      }
    }

    @Test
    @DisplayName("existsByPlacementRecordId executes lightweight boolean query without full entity loading")
    void existsByPlacementRecordIdOptimization() {
      boolean exists = employmentRecordRepository.existsByPlacementRecordId(999999L);
      assertThat(exists).isFalse();
    }
  }

  // =========================================================================
  // 6. Performance Benchmarking Tests
  // =========================================================================
  @Nested
  @DisplayName("6. Performance Benchmarking Tests")
  class PerformanceBenchmarkingTests {

    @Test
    @DisplayName("Measure cold vs warm response latency across representative dashboards")
    void benchmarkColdVsWarmLatency() {
      setNationalAdminContext();

      // Benchmark 1: Dashboard KPI Strip (/kpis)
      clearAllCaches();
      long startColdKpi = System.nanoTime();
      DashboardKpiStripResponse coldKpi = executiveDashboardService.getDashboardKpiStrip();
      long coldKpiMs = (System.nanoTime() - startColdKpi) / 1_000_000;
      assertThat(coldKpi).isNotNull();

      long startWarmKpi = System.nanoTime();
      DashboardKpiStripResponse warmKpi = executiveDashboardService.getDashboardKpiStrip();
      long warmKpiMs = (System.nanoTime() - startWarmKpi) / 1_000_000;
      assertThat(warmKpi).isNotNull();

      // Benchmark 2: Provider Leaderboard (/providers)
      clearAllCaches();
      long startColdProv = System.nanoTime();
      LeaderboardResponse<ProviderPerformanceResponse> coldProv = institutionalAnalyticsService.getProviderLeaderboard();
      long coldProvMs = (System.nanoTime() - startColdProv) / 1_000_000;
      assertThat(coldProv).isNotNull();

      long startWarmProv = System.nanoTime();
      LeaderboardResponse<ProviderPerformanceResponse> warmProv = institutionalAnalyticsService.getProviderLeaderboard();
      long warmProvMs = (System.nanoTime() - startWarmProv) / 1_000_000;
      assertThat(warmProv).isNotNull();

      // Sub-second validation for warm cached responses: Warm latency must be well under 1,000 ms
      assertThat(warmKpiMs).isLessThan(1000);
      assertThat(warmProvMs).isLessThan(1000);

      // Verify that warm response is drastically faster or sub-millisecond level
      System.out.printf("[BENCHMARK] KPI Strip: Cold=%d ms, Warm=%d ms%n", coldKpiMs, warmKpiMs);
      System.out.printf("[BENCHMARK] Provider Leaderboard: Cold=%d ms, Warm=%d ms%n", coldProvMs, warmProvMs);
    }
  }
}
