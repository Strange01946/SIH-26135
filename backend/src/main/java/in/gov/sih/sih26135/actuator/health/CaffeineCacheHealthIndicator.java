package in.gov.sih.sih26135.actuator.health;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import jakarta.annotation.PostConstruct;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.stereotype.Component;

/**
 * Health and diagnostics indicator exposing non-sensitive statistics for analytical Caffeine caches.
 *
 * <p>Principles:
 * <ul>
 *   <li>Exposes hit count, miss count, hit ratio, evictions, and estimated cache size.</li>
 *   <li>Binds native Caffeine caches to Micrometer via {@link CaffeineCacheMetrics}.</li>
 *   <li>Zero per-request key scans; strictly uses O(1) atomic counters from {@link CacheStats}.</li>
 *   <li>Zero exposure of cache keys, tenant identifiers, or cached business payloads.</li>
 *   <li>A low hit ratio or empty cache is treated as normal operational state, never taking the service DOWN.</li>
 * </ul>
 */
@Component("caffeineCache")
public class CaffeineCacheHealthIndicator implements HealthIndicator {

  private final CacheManager cacheManager;
  private final MeterRegistry meterRegistry;

  @Autowired
  public CaffeineCacheHealthIndicator(CacheManager cacheManager, MeterRegistry meterRegistry) {
    this.cacheManager = Objects.requireNonNull(cacheManager, "CacheManager is required");
    this.meterRegistry = Objects.requireNonNull(meterRegistry, "MeterRegistry is required");
  }

  @PostConstruct
  public void registerCacheMetrics() {
    Collection<String> cacheNames = cacheManager.getCacheNames();
    for (String name : cacheNames) {
      org.springframework.cache.Cache springCache = cacheManager.getCache(name);
      if (springCache instanceof CaffeineCache caffeineCache) {
        Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();
        CaffeineCacheMetrics.monitor(meterRegistry, nativeCache, name);
      }
    }
  }

  @Override
  public Health health() {
    Collection<String> cacheNames = cacheManager.getCacheNames();
    Map<String, Object> details = new LinkedHashMap<>();

    long totalHits = 0;
    long totalMisses = 0;
    long totalEvictions = 0;
    long totalEstimatedSize = 0;

    Map<String, Object> cacheBreakdown = new LinkedHashMap<>();

    for (String name : cacheNames) {
      org.springframework.cache.Cache springCache = cacheManager.getCache(name);
      if (springCache instanceof CaffeineCache caffeineCache) {
        Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();
        CacheStats stats = nativeCache.stats();
        long size = nativeCache.estimatedSize();

        totalHits += stats.hitCount();
        totalMisses += stats.missCount();
        totalEvictions += stats.evictionCount();
        totalEstimatedSize += size;

        Map<String, Object> cStat = new LinkedHashMap<>();
        cStat.put("estimatedSize", size);
        cStat.put("hitCount", stats.hitCount());
        cStat.put("missCount", stats.missCount());
        cStat.put("hitRate", Math.round(stats.hitRate() * 10000.0) / 100.0);
        cStat.put("evictionCount", stats.evictionCount());
        cacheBreakdown.put(name, cStat);
      }
    }

    long totalRequests = totalHits + totalMisses;
    double aggregateHitRate = totalRequests > 0
        ? Math.round(((double) totalHits / totalRequests) * 10000.0) / 100.0
        : 0.0;

    details.put("configuredCachesCount", cacheNames.size());
    details.put("totalEstimatedEntries", totalEstimatedSize);
    details.put("totalHits", totalHits);
    details.put("totalMisses", totalMisses);
    details.put("aggregateHitRatePct", aggregateHitRate);
    details.put("totalEvictions", totalEvictions);
    details.put("caches", cacheBreakdown);

    return Health.up()
        .withDetails(details)
        .build();
  }
}
