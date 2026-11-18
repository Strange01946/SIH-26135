package in.gov.sih.sih26135.communication.migration;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Phase 29.4 — Database Migration Catalog Safety & Fail-Closed Guard Tests")
class CommunicationDispatchQueueMigrationTest {

  @Test
  @DisplayName("1. Catalog Validation: Rejects production catalog SIH26135 even if URL contains test substring")
  void testCatalogValidation_rejectsProductionCatalogEvenWithTestUrl() {
    JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    DataSource dataSource = mock(DataSource.class);
    CommunicationDispatchQueueMigration migration = new CommunicationDispatchQueueMigration(jdbcTemplate, dataSource);

    // Metadata catalog says SIH26135 (production), but URL contains substring SIH26135_TEST
    boolean result = migration.shouldMigrateForCatalog("SIH26135", "jdbc:mysql://localhost:3306/SIH26135_TEST?useUnicode=true");
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("2. Catalog Validation: Rejects null or blank catalog")
  void testCatalogValidation_rejectsNullOrBlankCatalog() {
    JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    DataSource dataSource = mock(DataSource.class);
    CommunicationDispatchQueueMigration migration = new CommunicationDispatchQueueMigration(jdbcTemplate, dataSource);

    assertThat(migration.shouldMigrateForCatalog(null, "jdbc:mysql://localhost:3306/SIH26135_TEST")).isFalse();
    assertThat(migration.shouldMigrateForCatalog("", "jdbc:mysql://localhost:3306/SIH26135_TEST")).isFalse();
    assertThat(migration.shouldMigrateForCatalog("other_db", "jdbc:mysql://localhost:3306/other_db")).isFalse();
  }

  @Test
  @DisplayName("3. Catalog Validation: Strictly accepts SIH26135_TEST")
  void testCatalogValidation_acceptsSih26135Test() {
    JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    DataSource dataSource = mock(DataSource.class);
    CommunicationDispatchQueueMigration migration = new CommunicationDispatchQueueMigration(jdbcTemplate, dataSource);

    assertThat(migration.shouldMigrateForCatalog("SIH26135_TEST", "jdbc:mysql://localhost:3306/SIH26135_TEST")).isTrue();
    assertThat(migration.shouldMigrateForCatalog("sih26135_test", null)).isTrue();
  }

  @Test
  @DisplayName("4. Fail-Closed Guard: Production database connection skips DDL execution completely")
  void testProductionDatabase_skipsDdlExecution() throws SQLException {
    JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    DataSource dataSource = mock(DataSource.class);
    Connection connection = mock(Connection.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.getCatalog()).thenReturn("SIH26135"); // Production catalog

    CommunicationDispatchQueueMigration migration = new CommunicationDispatchQueueMigration(jdbcTemplate, dataSource);
    migration.migrateQueueTablesIfTestDatabase();

    // Verify zero DDL queries executed on production database
    verify(jdbcTemplate, never()).execute(org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  @DisplayName("5. Fail-Visible Guard: Test catalog migration failure throws IllegalStateException visibly")
  void testTestDatabase_migrationFailureThrowsVisibleIllegalStateException() throws SQLException {
    JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    DataSource dataSource = mock(DataSource.class);
    Connection connection = mock(Connection.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.getCatalog()).thenReturn("SIH26135_TEST");
    // Simulate DDL execution failure in test catalog
    org.mockito.Mockito.doThrow(new org.springframework.dao.DataIntegrityViolationException("Simulated DDL failure"))
        .when(jdbcTemplate).execute(org.mockito.ArgumentMatchers.anyString());

    CommunicationDispatchQueueMigration migration = new CommunicationDispatchQueueMigration(jdbcTemplate, dataSource);

    assertThatThrownBy(migration::migrateQueueTablesIfTestDatabase)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Mandatory queue tables migration failed in test catalog");
  }
}
