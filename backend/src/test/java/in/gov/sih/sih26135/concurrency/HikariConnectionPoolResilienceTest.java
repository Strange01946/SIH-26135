package in.gov.sih.sih26135.concurrency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import in.gov.sih.sih26135.MysqlConnectionTest;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

/**
 * Phase 29.6 — HikariCP Connection Pool Resilience Stress Tests.
 *
 * <p>Validates connection pool behavior under constrained sizing (max-pool-size = 3):
 * <ul>
 *   <li>Scenario A: Moderate contention (6 concurrent workers competing for 3 connections; queueing and completion).</li>
 *   <li>Scenario B: Controlled exhaustion (holding all 3 connections; bounded 2000ms acquisition timeout verification).</li>
 *   <li>Scenario C: Application recovery (clean connection release, baseline restoration, and subsequent query success).</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.datasource.hikari.maximum-pool-size=3",
    "spring.datasource.hikari.minimum-idle=1",
    "spring.datasource.hikari.connection-timeout=2000"
})
@DisplayName("Phase 29.6 — HikariCP Connection Pool Resilience Stress Tests")
public class HikariConnectionPoolResilienceTest {

  private static final Logger log = LoggerFactory.getLogger(HikariConnectionPoolResilienceTest.class);

  @Autowired
  private DataSource dataSource;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void verifyTestCatalog() throws Exception {
    try (Connection conn = dataSource.getConnection()) {
      MysqlConnectionTest.assertDedicatedTestCatalog(conn.getCatalog());
    }
  }

  @Test
  @DisplayName("1. Verify HikariDataSource pool sizing configuration (maximum-pool-size = 3)")
  void testHikariConfiguration_maxPoolSizeIsThree() {
    assertThat(dataSource).isInstanceOf(HikariDataSource.class);
    HikariDataSource hikari = (HikariDataSource) dataSource;
    assertThat(hikari.getMaximumPoolSize()).isEqualTo(3);
    assertThat(hikari.getConnectionTimeout()).isEqualTo(2000L);
    log.info("[HikariResilience] Verified configuration: maxPoolSize={}, connectionTimeout={}ms",
        hikari.getMaximumPoolSize(), hikari.getConnectionTimeout());
  }

  @Test
  @DisplayName("2. Scenario A — Moderate Contention: 6 concurrent database queries against 3 connections")
  void testModerateContention_sixWorkersOnThreeConnections() throws Exception {
    int workerCount = 6;
    CyclicBarrier barrier = new CyclicBarrier(workerCount);
    ExecutorService executor = Executors.newFixedThreadPool(workerCount);
    List<Future<Integer>> futures = new ArrayList<>(workerCount);

    try {
      for (int i = 0; i < workerCount; i++) {
        final int workerId = i;
        Callable<Integer> task = () -> {
          barrier.await(10, TimeUnit.SECONDS);
          // Execute a query that consumes connection briefly
          return jdbcTemplate.queryForObject("SELECT ? + 1", Integer.class, workerId);
        };
        futures.add(executor.submit(task));
      }

      int completed = 0;
      for (int i = 0; i < workerCount; i++) {
        Integer result = futures.get(i).get(10, TimeUnit.SECONDS);
        assertThat(result).isEqualTo(i + 1);
        completed++;
      }

      assertThat(completed).isEqualTo(workerCount);

      HikariDataSource hikari = (HikariDataSource) dataSource;
      HikariPoolMXBean poolMx = hikari.getHikariPoolMXBean();
      if (poolMx != null) {
        log.info("[HikariResilience] Moderate contention complete: totalConnections={}, activeConnections={}, idleConnections={}, waitingThreads={}",
            poolMx.getTotalConnections(), poolMx.getActiveConnections(), poolMx.getIdleConnections(), poolMx.getThreadsAwaitingConnection());
        assertThat(poolMx.getTotalConnections()).isLessThanOrEqualTo(3);
      }
    } finally {
      executor.shutdownNow();
      assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }
  }

  @Test
  @DisplayName("3. Scenario B & C — Controlled Exhaustion and Application Recovery")
  void testControlledExhaustionAndRecovery() throws Exception {
    HikariDataSource hikari = (HikariDataSource) dataSource;
    int poolSize = hikari.getMaximumPoolSize();
    assertThat(poolSize).isEqualTo(3);

    CountDownLatch holdLatch = new CountDownLatch(1);
    CountDownLatch acquiredLatch = new CountDownLatch(poolSize);
    List<Connection> heldConnections = new ArrayList<>(poolSize);
    ExecutorService holdingExecutor = Executors.newFixedThreadPool(poolSize);

    try {
      // 1. Hold exactly 3 connections in worker threads
      for (int i = 0; i < poolSize; i++) {
        holdingExecutor.submit(() -> {
          try {
            Connection conn = dataSource.getConnection();
            synchronized (heldConnections) {
              heldConnections.add(conn);
            }
            acquiredLatch.countDown();
            // Hold connection until release signal
            holdLatch.await(30, TimeUnit.SECONDS);
          } catch (Exception e) {
            log.error("Error holding connection: {}", e.getMessage(), e);
          }
        });
      }

      // Wait until all 3 connections are acquired and held
      assertThat(acquiredLatch.await(10, TimeUnit.SECONDS)).as("All 3 connections acquired").isTrue();

      HikariPoolMXBean poolMx = hikari.getHikariPoolMXBean();
      if (poolMx != null) {
        log.info("[HikariResilience] Exhaustion state: activeConnections={}, totalConnections={}",
            poolMx.getActiveConnections(), poolMx.getTotalConnections());
        assertThat(poolMx.getActiveConnections()).isEqualTo(3);
      }

      // 2. Attempt to acquire a 4th connection on main thread -> must time out after 2000ms
      long startAcquisition = System.currentTimeMillis();
      assertThatThrownBy(() -> dataSource.getConnection())
          .isInstanceOf(SQLException.class)
          .satisfies(ex -> {
            SQLException sqlEx = (SQLException) ex;
            assertThat((Throwable) sqlEx).isInstanceOf(SQLTransientConnectionException.class);
            assertThat(sqlEx.getMessage()).contains("Connection is not available, request timed out after");
          });
      long elapsed = System.currentTimeMillis() - startAcquisition;
      assertThat(elapsed).as("Acquisition timeout was approximately 2000ms").isGreaterThanOrEqualTo(1900L);
      log.info("[HikariResilience] 4th connection timed out as expected after {}ms", elapsed);

      // 3. Release held connections
      holdLatch.countDown();
      holdingExecutor.shutdown();
      assertThat(holdingExecutor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

      synchronized (heldConnections) {
        for (Connection c : heldConnections) {
          try {
            if (c != null && !c.isClosed()) {
              c.close();
            }
          } catch (SQLException ignore) {
          }
        }
        heldConnections.clear();
      }

      // 4. Scenario C — Application Recovery: verify subsequent query succeeds cleanly
      Integer recoveryResult = jdbcTemplate.queryForObject("SELECT 100 + 200", Integer.class);
      assertThat(recoveryResult).isEqualTo(300);

      // Verify active connections return to baseline
      if (poolMx != null) {
        log.info("[HikariResilience] Recovery state: activeConnections={}, idleConnections={}, totalConnections={}",
            poolMx.getActiveConnections(), poolMx.getIdleConnections(), poolMx.getTotalConnections());
        assertThat(poolMx.getActiveConnections()).isLessThanOrEqualTo(1);
      }

      log.info("[HikariResilience] Scenario B & C complete: pool exhaustion, bounded timeout, and recovery fully verified");
    } finally {
      holdLatch.countDown();
      holdingExecutor.shutdownNow();
      synchronized (heldConnections) {
        for (Connection c : heldConnections) {
          try {
            if (c != null && !c.isClosed()) {
              c.close();
            }
          } catch (SQLException ignore) {
          }
        }
      }
    }
  }
}
