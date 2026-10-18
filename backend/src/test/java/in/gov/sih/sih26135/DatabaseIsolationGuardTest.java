package in.gov.sih.sih26135;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pure unit tests verifying the Database Isolation Guard logic.
 * Ensures the guard reliably rejects the normal/production database catalog (SIH26135)
 * and accepts only the dedicated test database catalog (SIH26135_TEST).
 */
class DatabaseIsolationGuardTest {

  @Test
  @DisplayName("Rejects forbidden normal/production catalog SIH26135")
  void guardRejectsProductionCatalog() {
    assertThatThrownBy(() -> MysqlConnectionTest.assertDedicatedTestCatalog("SIH26135"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("targets forbidden normal/production catalog 'SIH26135'");
  }

  @Test
  @DisplayName("Rejects forbidden normal/production catalog case-insensitively (sih26135)")
  void guardRejectsProductionCatalogCaseInsensitive() {
    assertThatThrownBy(() -> MysqlConnectionTest.assertDedicatedTestCatalog("sih26135"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("targets forbidden normal/production catalog 'sih26135'");
  }

  @Test
  @DisplayName("Rejects null or blank catalog")
  void guardRejectsNullOrBlank() {
    assertThatThrownBy(() -> MysqlConnectionTest.assertDedicatedTestCatalog(null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("has no active catalog");

    assertThatThrownBy(() -> MysqlConnectionTest.assertDedicatedTestCatalog("   "))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("has no active catalog");
  }

  @Test
  @DisplayName("Rejects arbitrary third-party catalog")
  void guardRejectsOtherCatalog() {
    assertThatThrownBy(() -> MysqlConnectionTest.assertDedicatedTestCatalog("other_db"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("targets unrecognized catalog 'other_db'");
  }

  @Test
  @DisplayName("Accepts dedicated test catalog SIH26135_TEST and sih26135_test")
  void guardAcceptsDedicatedTestCatalog() {
    assertDoesNotThrow(() -> MysqlConnectionTest.assertDedicatedTestCatalog("SIH26135_TEST"));
    assertDoesNotThrow(() -> MysqlConnectionTest.assertDedicatedTestCatalog("sih26135_test"));
    assertDoesNotThrow(() -> MysqlConnectionTest.assertDedicatedTestCatalog("  SIH26135_TEST  "));
  }

  @Test
  @DisplayName("Rejects forbidden normal/production catalog with leading/trailing whitespace")
  void guardRejectsProductionCatalogWithWhitespace() {
    assertThatThrownBy(() -> MysqlConnectionTest.assertDedicatedTestCatalog("  SIH26135  "))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("targets forbidden normal/production catalog");
  }

  @Test
  @DisplayName("Rejects forbidden normal/production catalog with mixed case")
  void guardRejectsProductionCatalogMixedCase() {
    assertThatThrownBy(() -> MysqlConnectionTest.assertDedicatedTestCatalog("Sih26135"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("targets forbidden normal/production catalog");
  }

  @Test
  @DisplayName("Rejects catalogs with production prefix or suffix variations")
  void guardRejectsProductionPrefixOrSuffix() {
    assertThatThrownBy(() -> MysqlConnectionTest.assertDedicatedTestCatalog("SIH26135_PROD"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("targets unrecognized catalog 'SIH26135_PROD'");

    assertThatThrownBy(() -> MysqlConnectionTest.assertDedicatedTestCatalog("PROD_SIH26135"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("targets unrecognized catalog 'PROD_SIH26135'");
  }
}
