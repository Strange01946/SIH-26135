package in.gov.sih.sih26135.config.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Cache configuration with Caffeine providing tiered TTL strategies and bounded capacities
 * for analytical dashboards and summary projections.
 *
 * <p>Tiered TTL Strategy:
 * <ul>
 *   <li><b>Tier 1 (15m, max 100):</b> National overview aggregates (KPI strip, pipeline funnel, scheme matrix, national geography).</li>
 *   <li><b>Tier 2 (10m, max 200):</b> State-level aggregates, institutional rankings, economic summaries, retention survival, skill gap metrics.</li>
 *   <li><b>Tier 3 (5m, max 200-1000):</b> District performance tables, verification integrity, follow-up/survey summaries, analytical views.</li>
 * </ul>
 *
 * <p>All Caffeine caches are explicitly configured with {@code recordStats()} to enable performance benchmarking
 * and validation of cache hit/miss behavior without external dependencies.
 */
@Configuration
@EnableCaching
public class AnalyticsCacheConfig {

  public static final String CACHE_NATIONAL_OVERVIEW = "analytics_national_overview";
  public static final String CACHE_STATE_AGGREGATES = "analytics_state_aggregates";
  public static final String CACHE_DISTRICT_AGGREGATES = "analytics_district_aggregates";
  public static final String CACHE_INSTITUTIONAL = "analytics_institutional";
  public static final String CACHE_ECONOMIC = "analytics_economic";
  public static final String CACHE_RETENTION = "analytics_retention";
  public static final String CACHE_SKILL_GAP = "analytics_skill_gap";
  public static final String CACHE_VERIFICATION = "analytics_verification";
  public static final String CACHE_SUMMARIES = "analytics_summaries";

  @Bean
  public CacheManager cacheManager() {
    SimpleCacheManager cacheManager = new SimpleCacheManager();
    List<CaffeineCache> caches = new ArrayList<>();

    // Tier 1: National Overview Aggregates (15 min TTL, max 100 entries)
    caches.add(buildCache(CACHE_NATIONAL_OVERVIEW, 15, TimeUnit.MINUTES, 100));

    // Tier 2: State-Level Aggregates and Medium-Lived Dashboard Metrics (10 min TTL, max 200 entries)
    caches.add(buildCache(CACHE_STATE_AGGREGATES, 10, TimeUnit.MINUTES, 200));
    caches.add(buildCache(CACHE_INSTITUTIONAL, 10, TimeUnit.MINUTES, 200));
    caches.add(buildCache(CACHE_ECONOMIC, 10, TimeUnit.MINUTES, 200));
    caches.add(buildCache(CACHE_RETENTION, 10, TimeUnit.MINUTES, 200));
    caches.add(buildCache(CACHE_SKILL_GAP, 10, TimeUnit.MINUTES, 200));

    // Tier 3: District Tables and Workflow-Sensitive Summaries (5 min TTL, bounded capacity)
    caches.add(buildCache(CACHE_DISTRICT_AGGREGATES, 5, TimeUnit.MINUTES, 1000));
    caches.add(buildCache(CACHE_VERIFICATION, 5, TimeUnit.MINUTES, 200));
    caches.add(buildCache(CACHE_SUMMARIES, 5, TimeUnit.MINUTES, 500));

    cacheManager.setCaches(caches);
    return cacheManager;
  }

  private CaffeineCache buildCache(String name, long ttl, TimeUnit unit, long maxSize) {
    return new CaffeineCache(
        name,
        Caffeine.newBuilder()
            .expireAfterWrite(ttl, unit)
            .maximumSize(maxSize)
            .recordStats()
            .build()
    );
  }
}
