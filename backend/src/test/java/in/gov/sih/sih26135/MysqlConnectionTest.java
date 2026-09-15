package in.gov.sih.sih26135;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MysqlConnectionTest {

  @Autowired
  private DataSource dataSource;

  @Test
  void connectionSelectOne() throws Exception {
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT 1")) {
      assertThat(resultSet.next()).isTrue();
      assertThat(resultSet.getInt(1)).isEqualTo(1);
      assertThat(connection.getCatalog()).isEqualToIgnoringCase("SIH26135");
    }
  }

  @Test
  void schemaObjectCountsRemainFinalized() throws Exception {
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {
      assertThat(count(statement, "BASE TABLE")).isEqualTo(144);
      assertThat(count(statement, "VIEW")).isEqualTo(21);
    }
  }

  private static int count(Statement statement, String tableType) throws Exception {
    String sql =
        "SELECT COUNT(*) FROM information_schema.TABLES "
            + "WHERE TABLE_SCHEMA = 'SIH26135' AND TABLE_TYPE = '"
            + tableType
            + "'";
    try (ResultSet resultSet = statement.executeQuery(sql)) {
      assertThat(resultSet.next()).isTrue();
      return resultSet.getInt(1);
    }
  }
}
