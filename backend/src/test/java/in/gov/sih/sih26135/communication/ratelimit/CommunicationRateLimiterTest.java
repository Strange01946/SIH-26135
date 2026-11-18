package in.gov.sih.sih26135.communication.ratelimit;

import in.gov.sih.sih26135.communication.exception.CommunicationRateLimitException;
import in.gov.sih.sih26135.communication.model.CommunicationChannel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DisplayName("Phase 29.4 — Persistent Multi-Tenant Rate Limiter & Concurrency Safety Tests (SIH26135_TEST)")
class CommunicationRateLimiterTest {

  private static final Long TEST_TRAINEE_A = 98991L;
  private static final Long TEST_TRAINEE_B = 98992L;
  private static final Long TEST_PROVIDER_A = 9891L;
  private static final Long TEST_PROVIDER_B = 9892L;

  @Autowired
  private CommunicationRateLimiter rateLimiter;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @BeforeEach
  @AfterEach
  void cleanTestRateLimitEvents() {
    jdbcTemplate.update(
        "DELETE FROM communication_rate_limit_events WHERE limiter_key LIKE 'TRAINEE:%:9899%' "
            + "OR limiter_key LIKE 'PROVIDER:989%' OR limiter_key LIKE 'DEDUP:9899%'"
    );
  }

  @Test
  @DisplayName("1. SMS Rate Limit: Exactly 3 SMS allowed per 24 hours; 4th attempt is rejected with HTTP 429 and retryAfter")
  void testSmsRateLimit_3Per24Hours() {
    // Attempt 1, 2, 3 should pass
    rateLimiter.checkAndReserveQuota(TEST_TRAINEE_A, CommunicationChannel.SMS, null, "TPL-ENROLL-001", "p1");
    rateLimiter.checkAndReserveQuota(TEST_TRAINEE_A, CommunicationChannel.SMS, null, "TPL-ENROLL-001", "p2");
    rateLimiter.checkAndReserveQuota(TEST_TRAINEE_A, CommunicationChannel.SMS, null, "TPL-ENROLL-001", "p3");

    // 4th attempt must be rejected
    assertThatThrownBy(() ->
        rateLimiter.checkAndReserveQuota(TEST_TRAINEE_A, CommunicationChannel.SMS, null, "TPL-ENROLL-001", "p4")
    )
        .isInstanceOf(CommunicationRateLimitException.class)
        .extracting(e -> ((CommunicationRateLimitException) e).getErrorCode())
        .isEqualTo("RATE_LIMIT_EXCEEDED");
  }

  @Test
  @DisplayName("2. Email Rate Limit: Exactly 5 emails allowed per 24 hours; 6th attempt is rejected")
  void testEmailRateLimit_5Per24Hours() {
    for (int i = 1; i <= 5; i++) {
      rateLimiter.checkAndReserveQuota(TEST_TRAINEE_A, CommunicationChannel.EMAIL, null, "TPL-ALERT-005", "email_param_" + i);
    }

    assertThatThrownBy(() ->
        rateLimiter.checkAndReserveQuota(TEST_TRAINEE_A, CommunicationChannel.EMAIL, null, "TPL-ALERT-005", "email_param_6")
    )
        .isInstanceOf(CommunicationRateLimitException.class)
        .extracting(e -> ((CommunicationRateLimitException) e).getErrorCode())
        .isEqualTo("RATE_LIMIT_EXCEEDED");
  }

  @Test
  @DisplayName("3. Rapid Deduplication Window: Identical dispatch request within 5 seconds is rejected")
  void testDeduplicationWindow_rejectsIdenticalRequestsWithin5Seconds() {
    // First request succeeds
    rateLimiter.checkAndReserveQuota(TEST_TRAINEE_A, CommunicationChannel.SMS, null, "TPL-ENROLL-001", "identical_hash");

    // Immediate second identical request must be rejected as duplicate
    assertThatThrownBy(() ->
        rateLimiter.checkAndReserveQuota(TEST_TRAINEE_A, CommunicationChannel.SMS, null, "TPL-ENROLL-001", "identical_hash")
    )
        .isInstanceOf(CommunicationRateLimitException.class)
        .extracting(e -> ((CommunicationRateLimitException) e).getErrorCode())
        .isEqualTo("DUPLICATE_DISPATCH_REJECTED");
  }

  @Test
  @DisplayName("4. Provider Throughput Quota: Maximum 100 requests per 60 seconds enforced across all trainees")
  void testProviderThroughputQuota_100RequestsPer60Seconds() {
    String providerLimiterKey = "PROVIDER:" + TEST_PROVIDER_A;

    // Seed 100 provider requests in MySQL within last 60 seconds
    for (int i = 0; i < 100; i++) {
      jdbcTemplate.update(
          "INSERT INTO communication_rate_limit_events (limiter_key, event_type, created_at) VALUES (?, 'PROVIDER_REQUEST', NOW())",
          providerLimiterKey
      );
    }

    // 101st request on behalf of this provider must be rejected
    assertThatThrownBy(() ->
        rateLimiter.checkAndReserveQuota(TEST_TRAINEE_A, CommunicationChannel.SMS, TEST_PROVIDER_A, "TPL-ENROLL-001", "param_101")
    )
        .isInstanceOf(CommunicationRateLimitException.class)
        .extracting(e -> ((CommunicationRateLimitException) e).getErrorCode())
        .isEqualTo("PROVIDER_RATE_LIMIT_EXCEEDED");
  }

  @Test
  @DisplayName("5. Multi-Tenant Recipient Isolation: Trainee A reaching quota does not block Trainee B")
  void testMultiTenantIsolation_traineeIndependence() {
    // Trainee A exhausts quota
    for (int i = 1; i <= 3; i++) {
      rateLimiter.checkAndReserveQuota(TEST_TRAINEE_A, CommunicationChannel.SMS, null, "TPL-ENROLL-001", "hash_a_" + i);
    }

    // Trainee A is blocked
    assertThatThrownBy(() ->
        rateLimiter.checkAndReserveQuota(TEST_TRAINEE_A, CommunicationChannel.SMS, null, "TPL-ENROLL-001", "hash_a_4")
    ).isInstanceOf(CommunicationRateLimitException.class);

    // Trainee B can still dispatch SMS without error
    rateLimiter.checkAndReserveQuota(TEST_TRAINEE_B, CommunicationChannel.SMS, null, "TPL-ENROLL-001", "hash_b_1");
  }

  @Test
  @DisplayName("6. Multi-Tenant Provider Isolation: Provider A reaching quota does not block Provider B")
  void testMultiTenantIsolation_providerIndependence() {
    String providerALimiterKey = "PROVIDER:" + TEST_PROVIDER_A;
    for (int i = 0; i < 100; i++) {
      jdbcTemplate.update(
          "INSERT INTO communication_rate_limit_events (limiter_key, event_type, created_at) VALUES (?, 'PROVIDER_REQUEST', NOW())",
          providerALimiterKey
      );
    }

    // Provider A is throttled
    assertThatThrownBy(() ->
        rateLimiter.checkAndReserveQuota(TEST_TRAINEE_A, CommunicationChannel.SMS, TEST_PROVIDER_A, "TPL-ENROLL-001", "hash_pA")
    ).isInstanceOf(CommunicationRateLimitException.class);

    // Provider B can dispatch requests without throttle
    rateLimiter.checkAndReserveQuota(TEST_TRAINEE_A, CommunicationChannel.SMS, TEST_PROVIDER_B, "TPL-ENROLL-001", "hash_pB");
  }

  @Test
  @DisplayName("7. Concurrency Safety: Parallel threads competing for quotas respect limits atomically without over-allocation")
  void testConcurrencySafety_parallelQuotaContention() throws InterruptedException {
    int threadCount = 6;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(threadCount);

    AtomicInteger successfulReservations = new AtomicInteger(0);
    AtomicInteger rejectedReservations = new AtomicInteger(0);

    for (int i = 0; i < threadCount; i++) {
      final int idx = i;
      executor.execute(() -> {
        try {
          startLatch.await();
          rateLimiter.checkAndReserveQuota(TEST_TRAINEE_A, CommunicationChannel.SMS, null, "TPL-ENROLL-001", "thread_hash_" + idx);
          successfulReservations.incrementAndGet();
        } catch (CommunicationRateLimitException ex) {
          rejectedReservations.incrementAndGet();
        } catch (Exception ex) {
          // Unexpected error
        } finally {
          doneLatch.countDown();
        }
      });
    }

    startLatch.countDown();
    doneLatch.await();
    executor.shutdown();

    // Exactly 3 should succeed (SMS max quota) and 3 should be rejected
    assertThat(successfulReservations.get()).isEqualTo(3);
    assertThat(rejectedReservations.get()).isEqualTo(3);
  }

  @Test
  @DisplayName("8. Atomic Provider Burst Concurrency: Multiple distinct trainees under same provider respect 100 req/60s limit without over-allocation")
  void testConcurrencySafety_multiTraineeProviderQuotaContention() throws InterruptedException {
    String providerLimiterKey = "PROVIDER:" + TEST_PROVIDER_A;
    // Seed exactly 98 requests in rolling window
    for (int i = 0; i < 98; i++) {
      jdbcTemplate.update(
          "INSERT INTO communication_rate_limit_events (limiter_key, event_type, created_at) VALUES (?, 'PROVIDER_REQUEST', NOW())",
          providerLimiterKey
      );
    }

    // 6 distinct trainees under provider A attempt to dispatch concurrently
    int threadCount = 6;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(threadCount);

    AtomicInteger successfulReservations = new AtomicInteger(0);
    AtomicInteger throttledReservations = new AtomicInteger(0);

    for (int i = 0; i < threadCount; i++) {
      final long traineeId = 98990L + i;
      final int idx = i;
      executor.execute(() -> {
        try {
          startLatch.await();
          rateLimiter.checkAndReserveQuota(traineeId, CommunicationChannel.SMS, TEST_PROVIDER_A, "TPL-ENROLL-001", "multi_tr_hash_" + idx);
          successfulReservations.incrementAndGet();
        } catch (CommunicationRateLimitException ex) {
          if ("PROVIDER_RATE_LIMIT_EXCEEDED".equals(ex.getErrorCode())) {
            throttledReservations.incrementAndGet();
          }
        } catch (Exception ex) {
          // Unexpected error
        } finally {
          doneLatch.countDown();
        }
      });
    }

    startLatch.countDown();
    doneLatch.await();
    executor.shutdown();

    // With 98 already present and cap=100, exactly 2 must succeed and 4 must be throttled
    assertThat(successfulReservations.get()).isEqualTo(2);
    assertThat(throttledReservations.get()).isEqualTo(4);

    // Verify total provider count in MySQL is exactly 100 (zero over-allocation)
    Integer finalProviderCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM communication_rate_limit_events WHERE limiter_key = ? AND created_at >= NOW() - INTERVAL 60 SECOND",
        Integer.class,
        providerLimiterKey
    );
    assertThat(finalProviderCount).isEqualTo(100);
  }

  @Test
  @DisplayName("9. Retry Quota Policy: Automatic retries enforce provider burst capacity without penalizing trainee 24h quota")
  void testRetryQuotaPolicy_retriesEnforceProviderCapacityWithoutTraineePenalty() {
    String traineeKey = "TRAINEE:SMS:" + TEST_TRAINEE_A;
    String providerKey = "PROVIDER:" + TEST_PROVIDER_A;

    // Execute 3 normal dispatches (exhausting trainee's 3 SMS/24h quota)
    for (int i = 1; i <= 3; i++) {
      rateLimiter.checkAndReserveQuota(TEST_TRAINEE_A, CommunicationChannel.SMS, TEST_PROVIDER_A, "TPL-ENROLL-001", "param_" + i, false);
    }

    // Trainee normal dispatch is now blocked
    assertThatThrownBy(() ->
        rateLimiter.checkAndReserveQuota(TEST_TRAINEE_A, CommunicationChannel.SMS, TEST_PROVIDER_A, "TPL-ENROLL-001", "param_4", false)
    ).isInstanceOf(CommunicationRateLimitException.class);

    // Automatic technical retry (isRetry = true) succeeds because it does not re-penalize trainee 24h quota
    rateLimiter.checkAndReserveQuota(TEST_TRAINEE_A, CommunicationChannel.SMS, TEST_PROVIDER_A, "TPL-ENROLL-001", "param_retry", true);

    // Trainee events still equal exactly 3 in MySQL (no double-charging)
    Integer traineeCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM communication_rate_limit_events WHERE limiter_key = ? AND event_type = 'DISPATCH_ATTEMPT'",
        Integer.class,
        traineeKey
    );
    assertThat(traineeCount).isEqualTo(3);

    // Provider events equal 4 (each physical attempt reserves provider capacity)
    Integer providerCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM communication_rate_limit_events WHERE limiter_key = ? AND event_type = 'PROVIDER_REQUEST'",
        Integer.class,
        providerKey
    );
    assertThat(providerCount).isEqualTo(4);
  }
}
