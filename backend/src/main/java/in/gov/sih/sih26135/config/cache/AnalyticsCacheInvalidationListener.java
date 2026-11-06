package in.gov.sih.sih26135.config.cache;

import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Transaction-aware listener that invalidates relevant analytical caches only after
 * the originating workflow state transition has successfully committed to the database.
 *
 * <p>Guarantees:
 * <ul>
 *   <li>Committed workflow mutation -> affected cache regions cleared immediately.</li>
 *   <li>Rolled-back or failed transaction -> event discarded; no false invalidation occurs.</li>
 *   <li>Fallback execution -> supported in non-transactional test execution contexts.</li>
 * </ul>
 */
@Component
public class AnalyticsCacheInvalidationListener {

  private static final Logger log = LoggerFactory.getLogger(AnalyticsCacheInvalidationListener.class);

  private final CacheManager cacheManager;

  public AnalyticsCacheInvalidationListener(CacheManager cacheManager) {
    this.cacheManager = Objects.requireNonNull(cacheManager, "CacheManager is required");
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void onWorkflowInvalidation(AnalyticsWorkflowInvalidationEvent event) {
    if (event == null || event.getCacheNames().isEmpty()) {
      return;
    }

    log.debug("Processing cache invalidation from workflow '{}' for caches: {}",
        event.getSourceWorkflow(), event.getCacheNames());

    for (String cacheName : event.getCacheNames()) {
      Cache cache = cacheManager.getCache(cacheName);
      if (cache != null) {
        cache.clear();
        log.trace("Evicted cache '{}'", cacheName);
      }
    }
  }
}
