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
 * Idempotent database readiness and migration component for Phase 29.3 Demo Mode.
 *
 * <p>Guarantees that status ID 9 ('SIMULATED') exists exclusively in test and demonstration
 * databases (e.g. {@code SIH26135_TEST}) and strictly never mutates the production database {@code SIH26135}.
 */
@Component
public class CommunicationStatusDemoMigration {

  private static final Logger log = LoggerFactory.getLogger(CommunicationStatusDemoMigration.class);

  private final JdbcTemplate jdbcTemplate;
  private final DataSource dataSource;

  @Autowired
  public CommunicationStatusDemoMigration(JdbcTemplate jdbcTemplate, DataSource dataSource) {
    this.jdbcTemplate = jdbcTemplate;
    this.dataSource = dataSource;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void migrateStatusIfTestDatabase() {
    try (Connection conn = dataSource.getConnection()) {
      String catalog = conn.getCatalog();
      String url = conn.getMetaData().getURL();

      boolean isTestCatalog = (catalog != null && catalog.equalsIgnoreCase("SIH26135_TEST"))
          || (url != null && url.contains("SIH26135_TEST"));

      if (!isTestCatalog) {
        log.info("[CommunicationStatusMigration] Production or non-test catalog detected ({}). Skipping SIMULATED status migration.", catalog);
        return;
      }

      Integer count = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM ref_communication_status WHERE status_code = 'SIMULATED' OR communication_status_id = 9",
          Integer.class
      );

      if (count == null || count == 0) {
        jdbcTemplate.update(
            "INSERT INTO ref_communication_status (communication_status_id, status_code, status_name, is_success_flag, is_failure_flag, sort_order, created_at, updated_at) "
                + "VALUES (9, 'SIMULATED', 'Simulated (Demo Mode)', 0, 0, 9, NOW(), NOW())"
        );
        log.info("[CommunicationStatusMigration] Idempotently inserted status ID 9 (SIMULATED) into test catalog {}", catalog);
      } else {
        log.info("[CommunicationStatusMigration] Status SIMULATED already present in test catalog {}. No action required.", catalog);
      }
    } catch (Exception ex) {
      log.warn("[CommunicationStatusMigration] Non-fatal notice during SIMULATED status verification: {}", ex.getMessage());
    }
  }
}
