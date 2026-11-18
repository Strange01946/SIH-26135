package in.gov.sih.sih26135.chaos;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.datasource.DelegatingDataSource;

/**
 * Test-only fault-injecting DataSource proxy for Phase 29.7 chaos engineering.
 * Allows deterministic simulation of database connection failure at the external JDBC boundary
 * and mid-transaction statement/commit boundary without disrupting or shutting down the shared MySQL instance.
 */
public class TestDatabaseFaultDataSource extends DelegatingDataSource {

  private final AtomicBoolean connectionFailureEnabled = new AtomicBoolean(false);
  private final AtomicBoolean midStreamFailureEnabled = new AtomicBoolean(false);
  private final AtomicInteger statementCount = new AtomicInteger(0);
  private final AtomicInteger failAfterStatements = new AtomicInteger(1);
  private final AtomicBoolean failOnCommit = new AtomicBoolean(false);

  public TestDatabaseFaultDataSource(DataSource targetDataSource) {
    super(targetDataSource);
  }

  public void setConnectionFailureEnabled(boolean enabled) {
    this.connectionFailureEnabled.set(enabled);
  }

  public boolean isConnectionFailureEnabled() {
    return this.connectionFailureEnabled.get();
  }

  public void setMidStreamFailure(int failAfterStatements) {
    this.failAfterStatements.set(failAfterStatements);
    this.statementCount.set(0);
    this.midStreamFailureEnabled.set(true);
  }

  public void setFailOnCommit(boolean enabled) {
    this.failOnCommit.set(enabled);
  }

  public void disableAllFaults() {
    this.connectionFailureEnabled.set(false);
    this.midStreamFailureEnabled.set(false);
    this.failOnCommit.set(false);
    this.statementCount.set(0);
  }

  public int getStatementCount() {
    return this.statementCount.get();
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
    Connection actualConn = super.getConnection();
    if (midStreamFailureEnabled.get() || failOnCommit.get()) {
      return wrapConnection(actualConn);
    }
    return actualConn;
  }

  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    if (connectionFailureEnabled.get()) {
      throw new CannotGetJdbcConnectionException(
          "Could not get JDBC Connection; simulated transient database failure",
          new SQLException("Connection refused: simulated MySQL connection failure")
      );
    }
    Connection actualConn = super.getConnection(username, password);
    if (midStreamFailureEnabled.get() || failOnCommit.get()) {
      return wrapConnection(actualConn);
    }
    return actualConn;
  }

  private Connection wrapConnection(Connection actualConnection) {
    return (Connection) Proxy.newProxyInstance(
        TestDatabaseFaultDataSource.class.getClassLoader(),
        new Class<?>[]{Connection.class},
        new InvocationHandler() {
          @Override
          public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();
            if ("commit".equals(methodName) && failOnCommit.get()) {
              throw new SQLException("Connection lost during commit acknowledgment: commit status ambiguous");
            }
            try {
              Object result = method.invoke(actualConnection, args);
              if (midStreamFailureEnabled.get()) {
                if ("prepareStatement".equals(methodName) || "createStatement".equals(methodName)) {
                  return wrapStatement((Statement) result);
                }
              }
              return result;
            } catch (java.lang.reflect.InvocationTargetException ite) {
              throw ite.getCause();
            }
          }
        }
    );
  }

  private Statement wrapStatement(Statement actualStatement) {
    Class<?>[] interfaces = actualStatement instanceof PreparedStatement
        ? new Class<?>[]{PreparedStatement.class}
        : new Class<?>[]{Statement.class};

    return (Statement) Proxy.newProxyInstance(
        TestDatabaseFaultDataSource.class.getClassLoader(),
        interfaces,
        new InvocationHandler() {
          @Override
          public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();
            if (methodName.startsWith("execute") && midStreamFailureEnabled.get()) {
              int currentCount = statementCount.incrementAndGet();
              if (currentCount >= failAfterStatements.get()) {
                throw new SQLException("Communications link failure during statement execution: simulated mid-stream JDBC network drop");
              }
            }
            try {
              return method.invoke(actualStatement, args);
            } catch (java.lang.reflect.InvocationTargetException ite) {
              throw ite.getCause();
            }
          }
        }
    );
  }
}
