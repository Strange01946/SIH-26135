package in.gov.sih.sih26135.chaos;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.datasource.DelegatingDataSource;

/**
 * Test-only fault-injecting DataSource proxy for Phase 29.7 chaos engineering.
 * Allows deterministic simulation of database connection failure at the external JDBC boundary
 * without disrupting or shutting down the shared MySQL instance.
 */
public class TestDatabaseFaultDataSource extends DelegatingDataSource {

  private final AtomicBoolean connectionFailureEnabled = new AtomicBoolean(false);

  public TestDatabaseFaultDataSource(DataSource targetDataSource) {
    super(targetDataSource);
  }

  public void setConnectionFailureEnabled(boolean enabled) {
    this.connectionFailureEnabled.set(enabled);
  }

  public boolean isConnectionFailureEnabled() {
    return this.connectionFailureEnabled.get();
  }

  public DataSource getActualTargetDataSource() {
    return super.getTargetDataSource();
  }

  @Override
  public Connection getConnection() throws SQLException {
    if (connectionFailureEnabled.get()) {
      throw new CannotGetJdbcConnectionException(
          "Could not get JDBC Connection; simulated transient database failure",
          new SQLException("Connection refused: simulated MySQL connection failure")
      );
    }
    return super.getConnection();
  }

  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    if (connectionFailureEnabled.get()) {
      throw new CannotGetJdbcConnectionException(
          "Could not get JDBC Connection; simulated transient database failure",
          new SQLException("Connection refused: simulated MySQL connection failure")
      );
    }
    return super.getConnection(username, password);
  }
}
