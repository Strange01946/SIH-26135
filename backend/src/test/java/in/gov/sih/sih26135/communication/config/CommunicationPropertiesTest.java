package in.gov.sih.sih26135.communication.config;

import in.gov.sih.sih26135.communication.config.CommunicationProperties.ProviderMode;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Phase 29.4 — CommunicationProperties Synthetic Signature Guard Tests (Fail-Closed)")
class CommunicationPropertiesTest {

  private CommunicationProperties properties;
  private DataSource mockDataSource;
  private Connection mockConnection;
  private MockEnvironment mockEnvironment;

  @BeforeEach
  void setUp() throws SQLException {
    properties = new CommunicationProperties();
    mockDataSource = mock(DataSource.class);
    mockConnection = mock(Connection.class);
    mockEnvironment = new MockEnvironment();

    when(mockDataSource.getConnection()).thenReturn(mockConnection);
    when(mockConnection.getCatalog()).thenReturn("SIH26135_TEST");

    properties.setDataSource(mockDataSource);
    properties.setEnvironment(mockEnvironment);
    properties.setMode(ProviderMode.DEMO);
    properties.setAllowTestWebhookSignatures(true);
  }

  @Test
  @DisplayName("1. Synthetic signatures accepted when configured in test catalog with DEMO mode")
  void testSyntheticSignatures_acceptedWhenConfiguredInTestCatalog() {
    assertThat(properties.isSyntheticTestSignatureAllowed()).isTrue();
  }

  @Test
  @DisplayName("2. Synthetic signatures rejected when allowTestWebhookSignatures is false")
  void testSyntheticSignatures_rejectedWhenDisabled() {
    properties.setAllowTestWebhookSignatures(false);
    assertThat(properties.isSyntheticTestSignatureAllowed()).isFalse();
  }

  @Test
  @DisplayName("3. Synthetic signatures rejected in LIVE mode even if allowTestWebhookSignatures is true")
  void testSyntheticSignatures_rejectedInLiveMode() {
    properties.setMode(ProviderMode.LIVE);
    assertThat(properties.isSyntheticTestSignatureAllowed()).isFalse();
  }

  @Test
  @DisplayName("4. Fail closed when DataSource is null")
  void testSyntheticSignatures_rejectedWhenDataSourceNull() {
    properties.setDataSource(null);
    assertThat(properties.isSyntheticTestSignatureAllowed()).isFalse();
  }

  @Test
  @DisplayName("5. Fail closed when Connection is null")
  void testSyntheticSignatures_rejectedWhenConnectionNull() throws SQLException {
    when(mockDataSource.getConnection()).thenReturn(null);
    assertThat(properties.isSyntheticTestSignatureAllowed()).isFalse();
  }

  @Test
  @DisplayName("6. Fail closed when DataSource.getConnection() throws SQLException")
  void testSyntheticSignatures_rejectedWhenGetConnectionThrowsSQLException() throws SQLException {
    when(mockDataSource.getConnection()).thenThrow(new SQLException("Connection refused"));
    assertThat(properties.isSyntheticTestSignatureAllowed()).isFalse();
  }

  @Test
  @DisplayName("7. Fail closed when Connection.getCatalog() throws SQLException")
  void testSyntheticSignatures_rejectedWhenGetCatalogThrowsSQLException() throws SQLException {
    when(mockConnection.getCatalog()).thenThrow(new SQLException("Catalog lookup failed"));
    assertThat(properties.isSyntheticTestSignatureAllowed()).isFalse();
  }

  @Test
  @DisplayName("8. Fail closed when active catalog is null or blank")
  void testSyntheticSignatures_rejectedWhenCatalogNullOrBlank() throws SQLException {
    when(mockConnection.getCatalog()).thenReturn(null);
    assertThat(properties.isSyntheticTestSignatureAllowed()).isFalse();

    when(mockConnection.getCatalog()).thenReturn("   ");
    assertThat(properties.isSyntheticTestSignatureAllowed()).isFalse();
  }

  @Test
  @DisplayName("9. Fail closed when active catalog is production SIH26135")
  void testSyntheticSignatures_rejectedWhenCatalogIsProduction() throws SQLException {
    when(mockConnection.getCatalog()).thenReturn("SIH26135");
    assertThat(properties.isSyntheticTestSignatureAllowed()).isFalse();
  }

  @Test
  @DisplayName("10. Fail closed when active catalog is any unrecognized catalog (e.g. dev_db)")
  void testSyntheticSignatures_rejectedWhenCatalogIsUnrecognized() throws SQLException {
    when(mockConnection.getCatalog()).thenReturn("dev_db");
    assertThat(properties.isSyntheticTestSignatureAllowed()).isFalse();
  }

  @Test
  @DisplayName("11. Fail closed when production profile ('prod' or 'production') is active")
  void testSyntheticSignatures_rejectedWhenProdProfileActive() {
    mockEnvironment.setActiveProfiles("prod");
    assertThat(properties.isSyntheticTestSignatureAllowed()).isFalse();

    mockEnvironment.setActiveProfiles("production");
    assertThat(properties.isSyntheticTestSignatureAllowed()).isFalse();
  }

  @Test
  @DisplayName("12. Fail closed when APP_ENVIRONMENT is prod or production")
  void testSyntheticSignatures_rejectedWhenAppEnvIsProd() {
    mockEnvironment.setProperty("APP_ENVIRONMENT", "prod");
    assertThat(properties.isSyntheticTestSignatureAllowed()).isFalse();

    mockEnvironment.setProperty("APP_ENVIRONMENT", "production");
    assertThat(properties.isSyntheticTestSignatureAllowed()).isFalse();
  }

  @Test
  @DisplayName("13. Fail closed when datasource URL points to production database")
  void testSyntheticSignatures_rejectedWhenDatasourceUrlPointsToProduction() {
    mockEnvironment.setProperty("spring.datasource.url", "jdbc:mysql://localhost:3306/SIH26135?useSSL=false");
    assertThat(properties.isSyntheticTestSignatureAllowed()).isFalse();
  }

  @Test
  @DisplayName("14. Fail closed when DB_NAME property is production")
  void testSyntheticSignatures_rejectedWhenDbNamePropertyIsProduction() {
    mockEnvironment.setProperty("DB_NAME", "SIH26135");
    assertThat(properties.isSyntheticTestSignatureAllowed()).isFalse();
  }

  @Test
  @DisplayName("15. URL Catalog extraction utility correctly parses various JDBC URL formats")
  void testExtractCatalogFromJdbcUrl() {
    assertThat(CommunicationProperties.extractCatalogFromJdbcUrl("jdbc:mysql://localhost:3306/SIH26135_TEST?useSSL=false"))
        .isEqualTo("SIH26135_TEST");
    assertThat(CommunicationProperties.extractCatalogFromJdbcUrl("jdbc:mysql://db.host.internal:3306/SIH26135/"))
        .isEqualTo("SIH26135");
    assertThat(CommunicationProperties.extractCatalogFromJdbcUrl("jdbc:mysql://localhost:3306/mydb"))
        .isEqualTo("mydb");
    assertThat(CommunicationProperties.extractCatalogFromJdbcUrl(null))
        .isEqualTo("");
    assertThat(CommunicationProperties.extractCatalogFromJdbcUrl("   "))
        .isEqualTo("");
  }
}
