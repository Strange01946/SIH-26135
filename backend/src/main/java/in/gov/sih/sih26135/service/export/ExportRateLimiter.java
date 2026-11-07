package in.gov.sih.sih26135.service.export;

import in.gov.sih.sih26135.config.AnalyticsExportProperties;
import in.gov.sih.sih26135.exception.RateLimitExceededException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * In-memory sliding-window rate limiter and bounded concurrency manager for analytical export operations.
 *
 * <p>Enforces:
 * <ul>
 *   <li>Per-user sliding window limits: max 5 requests per minute, max 30 requests per hour (configurable).</li>
 *   <li>Per-user active concurrency limit: max 2 simultaneous export streams per user (configurable).</li>
 *   <li>System-wide active concurrency limit: max 10 simultaneous export streams globally (configurable).</li>
 *   <li>Atomic concurrency check and permit acquisition across users and system-wide.</li>
 *   <li>Deterministic permit release on stream completion, error, client cancellation, or preflight failure.</li>
 *   <li>Guard against releasing unacquired permits.</li>
 * </ul>
 *
 * <p><b>Multi-instance limitation:</b> This in-memory limiter operates per application JVM instance.
 */
@Component
public class ExportRateLimiter {

  private static final Logger log = LoggerFactory.getLogger(ExportRateLimiter.class);

  private static final long ONE_MINUTE_MS = 60_000L;
  private static final long ONE_HOUR_MS = 3_600_000L;

  private final AnalyticsExportProperties properties;

  private final Map<Long, UserRateState> userStates = new ConcurrentHashMap<>();
  private final Map<Long, AtomicInteger> activeExportsPerUser = new ConcurrentHashMap<>();
  private final AtomicInteger globalActiveExports = new AtomicInteger(0);

  public ExportRateLimiter(AnalyticsExportProperties properties) {
    this.properties = properties;
  }

  private static final class UserRateState {
    final Deque<Long> minuteTimestamps = new ArrayDeque<>();
    final Deque<Long> hourTimestamps = new ArrayDeque<>();
  }

  /**
   * Preflight rate-limit and concurrency check before beginning an analytical export stream.
   * Atomically acquires a concurrency permit if all limits are satisfied.
   *
   * @param userId authenticated user ID
   * @throws RateLimitExceededException if rate or concurrency limit is exceeded (HTTP 429)
   */
  public synchronized void checkAndAcquire(Long userId) {
    long effectiveUserId = userId != null ? userId : 0L;
    long now = System.currentTimeMillis();

    // 1. Sliding window rate limit check
    UserRateState state = userStates.computeIfAbsent(effectiveUserId, k -> new UserRateState());
    
    // Evict timestamps older than 1 minute
    while (!state.minuteTimestamps.isEmpty() && now - state.minuteTimestamps.peekFirst() >= ONE_MINUTE_MS) {
      state.minuteTimestamps.pollFirst();
    }
    if (state.minuteTimestamps.size() >= properties.getRateLimitPerMinute()) {
      long oldest = state.minuteTimestamps.peekFirst();
      long retryAfter = Math.max(1, (oldest + ONE_MINUTE_MS - now + 999) / 1000);
      log.warn("Export rate limit (minute) exceeded for user {}: count={}, limit={}, retryAfter={}s",
          effectiveUserId, state.minuteTimestamps.size(), properties.getRateLimitPerMinute(), retryAfter);
      throw new RateLimitExceededException("Export rate limit exceeded. Please retry later.", retryAfter);
    }

    // Evict timestamps older than 1 hour
    while (!state.hourTimestamps.isEmpty() && now - state.hourTimestamps.peekFirst() >= ONE_HOUR_MS) {
      state.hourTimestamps.pollFirst();
    }
    if (state.hourTimestamps.size() >= properties.getRateLimitPerHour()) {
      long oldest = state.hourTimestamps.peekFirst();
      long retryAfter = Math.max(1, (oldest + ONE_HOUR_MS - now + 999) / 1000);
      log.warn("Export rate limit (hour) exceeded for user {}: count={}, limit={}, retryAfter={}s",
          effectiveUserId, state.hourTimestamps.size(), properties.getRateLimitPerHour(), retryAfter);
      throw new RateLimitExceededException("Export hourly rate limit exceeded. Please retry later.", retryAfter);
    }

    // 2. Concurrency checks (Atomic across all users)
    AtomicInteger userActive = activeExportsPerUser.computeIfAbsent(effectiveUserId, k -> new AtomicInteger(0));
    if (userActive.get() >= properties.getConcurrencyLimitPerUser()) {
      log.warn("User concurrent export limit exceeded for user {}: active={}, limit={}",
          effectiveUserId, userActive.get(), properties.getConcurrencyLimitPerUser());
      throw new RateLimitExceededException(
          "Concurrent export limit exceeded for user. Please wait for active export to complete.",
          "EXPORT_CONCURRENCY_LIMIT_EXCEEDED",
          5
      );
    }

    if (globalActiveExports.get() >= properties.getConcurrencyLimitGlobal()) {
      log.warn("Global concurrent export limit exceeded: active={}, limit={}",
          globalActiveExports.get(), properties.getConcurrencyLimitGlobal());
      throw new RateLimitExceededException(
          "System-wide concurrent export limit reached. Please retry shortly.",
          "EXPORT_CONCURRENCY_LIMIT_EXCEEDED",
          5
      );
    }

    // 3. Atomically acquire permits and register request timestamps
    userActive.incrementAndGet();
    globalActiveExports.incrementAndGet();
    state.minuteTimestamps.addLast(now);
    state.hourTimestamps.addLast(now);
    log.debug("Acquired export concurrency permit for user {}: userActive={}, globalActive={}",
        effectiveUserId, userActive.get(), globalActiveExports.get());
  }

  /**
   * Releases active concurrency permit held by the specified user.
   * Guaranteed to be called in a finally block on stream completion, failure, disconnect, or preflight error.
   *
   * <p>Safeguard: If the user does not hold an active permit, this is a no-op and does not decrement global count.
   *
   * @param userId authenticated user ID
   */
  public synchronized void release(Long userId) {
    long effectiveUserId = userId != null ? userId : 0L;
    AtomicInteger userActive = activeExportsPerUser.get(effectiveUserId);
    if (userActive != null && userActive.get() > 0) {
      userActive.decrementAndGet();
      globalActiveExports.updateAndGet(curr -> Math.max(0, curr - 1));
      log.debug("Released export concurrency permit for user {}: userActive={}, globalActive={}",
          effectiveUserId, userActive.get(), globalActiveExports.get());
    } else {
      log.warn("Attempted to release export permit for user {} but no active permit found", effectiveUserId);
    }
  }

  /**
   * Resets all internal rate-limiting and concurrency counters.
   * Intended strictly for automated test suites.
   */
  public synchronized void reset() {
    userStates.clear();
    activeExportsPerUser.clear();
    globalActiveExports.set(0);
  }

  public synchronized int getActiveExportsForUser(Long userId) {
    long effectiveUserId = userId != null ? userId : 0L;
    AtomicInteger userActive = activeExportsPerUser.get(effectiveUserId);
    return userActive != null ? userActive.get() : 0;
  }

  public synchronized int getGlobalActiveExports() {
    return globalActiveExports.get();
  }
}
