package in.gov.sih.sih26135;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class Sih26135ApplicationTests {

  @Autowired
  private DataSource dataSource;

  @Test
  void contextLoads() throws Exception {
    assertThat(dataSource)
        .as("DataSource bean must be present in application context")
        .isNotNull();

    try (Connection connection = dataSource.getConnection()) {
      assertThat(connection)
          .as("Valid database connection must be established")
          .isNotNull();

      String catalog = connection.getCatalog();
      // Fail-closed guard: strictly rejects SIH26135, null, blank, or any non-SIH26135_TEST catalog
      MysqlConnectionTest.assertDedicatedTestCatalog(catalog);
      assertThat(catalog).isEqualToIgnoringCase(MysqlConnectionTest.EXPECTED_TEST_CATALOG);
    }
  }
}
