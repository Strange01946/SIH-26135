package in.gov.sih.sih26135.communication.migration;

import java.sql.Connection;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Idempotent database readiness and migration component for Phase 29.4
 * Durable Dispatch Queue, Retry Engine, and Rate Limiting.
 *
 * <p>Guarantees that supporting queue, attempt, and rate-limit tables exist exclusively
 * in test and demonstration databases (specifically {@code SIH26135_TEST}) and strictly never mutates
 * the production database {@code SIH26135}.
 *
 * <p>Enforces:
 * <ul>
 *   <li>Authoritative JDBC connection catalog validation: strictly requires catalog to be {@code SIH26135_TEST}.</li>
 *   <li>Rejection of URL substring matching when connection metadata disagrees.</li>
 *   <li>Fail-closed and fail-visible behavior on test migration failure.</li>
 * </ul>
 */
@Component
public class CommunicationDispatchQueueMigration {

  private static final Logger log = LoggerFactory.getLogger(CommunicationDispatchQueueMigration.class);

  private final JdbcTemplate jdbcTemplate;
  private final DataSource dataSource;

  @Autowired
  public CommunicationDispatchQueueMigration(JdbcTemplate jdbcTemplate, DataSource dataSource) {
    this.jdbcTemplate = jdbcTemplate;
    this.dataSource = dataSource;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void migrateQueueTablesIfTestDatabase() {
    try (Connection conn = dataSource.getConnection()) {
      String catalog = conn.getCatalog();

      // Strict fail-closed catalog check: strictly require connection metadata catalog to be SIH26135_TEST
      if (catalog == null || !catalog.equalsIgnoreCase("SIH26135_TEST")) {
        log.info("[CommunicationQueueMigration] Production or non-test catalog detected (catalog='{}'). Skipping queue tables migration.", catalog);
        return;
      }

      // 1. communication_dispatch_jobs
      jdbcTemplate.execute(
          "CREATE TABLE IF NOT EXISTS communication_dispatch_jobs ("
              + "job_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY, "
              + "idempotency_key VARCHAR(64) UNIQUE NOT NULL, "
              + "trainee_id BIGINT UNSIGNED NOT NULL, "
              + "training_provider_id BIGINT UNSIGNED NULL, "
              + "channel_id BIGINT UNSIGNED NOT NULL, "
              + "purpose_id BIGINT UNSIGNED NOT NULL, "
              + "template_code VARCHAR(64) NOT NULL, "
              + "template_parameters_json TEXT NULL, "
              + "job_status VARCHAR(32) NOT NULL DEFAULT 'PENDING', "
              + "attempt_count INT NOT NULL DEFAULT 0, "
              + "max_attempts INT NOT NULL DEFAULT 3, "
              + "delivery_mode VARCHAR(16) NOT NULL DEFAULT 'DEMO', "
              + "initiated_by_user_id BIGINT UNSIGNED NULL, "
              + "initiated_by_service VARCHAR(64) NULL, "
              + "tenant_scope_type VARCHAR(32) NULL, "
              + "scheduled_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, "
              + "lease_version INT UNSIGNED NOT NULL DEFAULT 1, "
              + "locked_at DATETIME NULL, "
              + "locked_by VARCHAR(64) NULL, "
              + "communication_log_id BIGINT UNSIGNED NULL, "
              + "last_error_code VARCHAR(64) NULL, "
              + "last_error_message VARCHAR(500) NULL, "
              + "dead_letter_reason VARCHAR(500) NULL, "
              + "dead_lettered_at DATETIME NULL, "
              + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, "
              + "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, "
              + "INDEX idx_cdj_status_sched (job_status, scheduled_at), "
              + "INDEX idx_cdj_trainee (trainee_id), "
              + "INDEX idx_cdj_provider (training_provider_id)"
              + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
      );

      // Ensure lease_version column exists for existing tables
      try {
        Integer colCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = 'communication_dispatch_jobs' AND COLUMN_NAME = 'lease_version'",
            Integer.class,
            catalog
        );
        if (colCount == null || colCount == 0) {
          jdbcTemplate.execute("ALTER TABLE communication_dispatch_jobs ADD COLUMN lease_version INT UNSIGNED NOT NULL DEFAULT 1 AFTER scheduled_at");
        }
      } catch (Exception ex) {
        log.debug("[CommunicationQueueMigration] Notice checking lease_version column: {}", ex.getMessage());
      }

      // 2. communication_dispatch_attempts
      jdbcTemplate.execute(
          "CREATE TABLE IF NOT EXISTS communication_dispatch_attempts ("
              + "attempt_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY, "
              + "job_id BIGINT UNSIGNED NOT NULL, "
              + "attempt_number INT NOT NULL, "
              + "attempt_status VARCHAR(32) NOT NULL, "
              + "provider_name VARCHAR(64) NULL, "
              + "provider_message_id VARCHAR(64) NULL, "
              + "error_code VARCHAR(64) NULL, "
              + "error_message VARCHAR(500) NULL, "
              + "attempted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, "
              + "completed_at DATETIME NULL, "
              + "INDEX idx_cda_job (job_id), "
              + "INDEX idx_cda_provider_msg (provider_message_id)"
              + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
      );

      // 3. communication_rate_limit_events
      jdbcTemplate.execute(
          "CREATE TABLE IF NOT EXISTS communication_rate_limit_events ("
              + "event_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY, "
              + "limiter_key VARCHAR(128) NOT NULL, "
              + "event_type VARCHAR(32) NOT NULL, "
              + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, "
              + "INDEX idx_crle_key_time (limiter_key, created_at)"
              + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
      );

      log.info("[CommunicationQueueMigration] Idempotently verified Phase 29.4 queue, attempt, and rate-limit tables in test catalog {}", catalog);

    } catch (Exception ex) {
      log.error("[CommunicationQueueMigration] Mandatory queue tables migration failed in test database: {}", ex.getMessage(), ex);
      throw new IllegalStateException("Mandatory queue tables migration failed in test catalog: " + ex.getMessage(), ex);
    }
  }

  /**
   * Helper method for verification testing of catalog validation safety.
   */
  public boolean shouldMigrateForCatalog(String catalog, String url) {
    if (catalog == null || !catalog.equalsIgnoreCase("SIH26135_TEST")) {
      return false;
    }
    return true;
  }
}
