package in.gov.sih.sih26135;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MysqlConnectionTest {

  public static final String EXPECTED_TEST_CATALOG = "SIH26135_TEST";
  public static final String FORBIDDEN_PRODUCTION_CATALOG = "SIH26135";

  @Autowired
  private DataSource dataSource;

  /**
   * Automated guard for database integration tests that rejects the normal SIH26135 database
   * and accepts only the dedicated SIH26135_TEST catalog.
   */
  @BeforeEach
  void enforceDatabaseIsolationGuard() throws Exception {
    try (Connection connection = dataSource.getConnection()) {
      assertDedicatedTestCatalog(connection.getCatalog());
    }
  }

  public static void assertDedicatedTestCatalog(String currentCatalog) {
    if (currentCatalog == null || currentCatalog.isBlank()) {
      throw new IllegalStateException(
          "FATAL: Database connection has no active catalog. Dedicated test catalog '"
              + EXPECTED_TEST_CATALOG
              + "' is required.");
    }

    if (FORBIDDEN_PRODUCTION_CATALOG.equalsIgnoreCase(currentCatalog.trim())) {
      throw new IllegalStateException(
          "FATAL: Database isolation violation! Connection targets forbidden normal/production catalog '"
              + currentCatalog
              + "'. Database integration tests must only target dedicated test database '"
              + EXPECTED_TEST_CATALOG
              + "'.");
    }

    if (!EXPECTED_TEST_CATALOG.equalsIgnoreCase(currentCatalog.trim())) {
      throw new IllegalStateException(
          "FATAL: Database isolation violation! Expected dedicated test catalog '"
              + EXPECTED_TEST_CATALOG
              + "', but connection targets unrecognized catalog '"
              + currentCatalog
              + "'.");
    }
  }

  @Test
  void connectionSelectOne() throws Exception {
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT 1")) {
      assertThat(resultSet.next()).isTrue();
      assertThat(resultSet.getInt(1)).isEqualTo(1);
      assertThat(connection.getCatalog()).isEqualToIgnoringCase(EXPECTED_TEST_CATALOG);
    }
  }

  @Test
  void schemaObjectCountsRemainFinalized() throws Exception {
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {
      assertThat(count(statement, "BASE TABLE", EXPECTED_TEST_CATALOG)).isEqualTo(144);
      assertThat(count(statement, "VIEW", EXPECTED_TEST_CATALOG)).isEqualTo(21);
    }
  }

  private static int count(Statement statement, String tableType, String catalog) throws Exception {
    String sql =
        "SELECT COUNT(*) FROM information_schema.TABLES "
            + "WHERE TABLE_SCHEMA = '"
            + catalog
            + "' AND TABLE_TYPE = '"
            + tableType
            + "'";
    try (ResultSet resultSet = statement.executeQuery(sql)) {
      assertThat(resultSet.next()).isTrue();
      return resultSet.getInt(1);
    }
  }

}
