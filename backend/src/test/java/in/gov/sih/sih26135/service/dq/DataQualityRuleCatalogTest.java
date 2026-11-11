package in.gov.sih.sih26135.service.dq;

import in.gov.sih.sih26135.MysqlConnectionTest;
import in.gov.sih.sih26135.dto.request.DataQualityScanRequest;
import in.gov.sih.sih26135.entity.DataQualityRule;
import in.gov.sih.sih26135.exception.DataQualityRuleNotFoundException;
import in.gov.sih.sih26135.repository.DataQualityRuleRepository;
import in.gov.sih.sih26135.service.DataQualityRuleCatalogService;
import in.gov.sih.sih26135.service.impl.DataQualityRuleCatalogServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional(readOnly = true)
@DisplayName("Phase 28.3 - Data Quality Rule Catalog & Idempotent Provisioning Tests")
class DataQualityRuleCatalogTest {

  @Autowired
  private DataSource dataSource;

  @Autowired
  private DataQualityRuleCatalogService catalogService;

  @Autowired
  private DataQualityRuleRepository ruleRepository;

  @BeforeEach
  void setUp() throws Exception {
    try (Connection conn = dataSource.getConnection()) {
      String catalog = conn.getCatalog();
      MysqlConnectionTest.assertDedicatedTestCatalog(catalog);
      assertThat(catalog).isEqualToIgnoringCase(MysqlConnectionTest.EXPECTED_TEST_CATALOG);
    }
    catalogService.ensureDefaultRulesProvisioned();
  }

  @Test
  @DisplayName("1. All five core domain rules can be resolved by code with correct attributes")
  void testCoreRulesResolution() {
    // DQ-TRAINEE-001
    DataQualityRule r1 = catalogService.resolveRuleByCode("DQ-TRAINEE-001");
    assertThat(r1).isNotNull();
    assertThat(r1.getTargetEntityType()).isEqualTo("Trainee");
    assertThat(r1.getDataQualityCategory().getCategoryCode()).isEqualTo("MISSING");
    assertThat(r1.getDataQualitySeverity().getSeverityCode()).isEqualTo("MEDIUM");
    assertThat(r1.getLifecycleStatusId()).isEqualTo(DataQualityRuleCatalogServiceImpl.LIFECYCLE_STATUS_ACTIVE);

    // DQ-PLACEMENT-002
    DataQualityRule r2 = catalogService.resolveRuleByCode("DQ-PLACEMENT-002");
    assertThat(r2).isNotNull();
    assertThat(r2.getTargetEntityType()).isEqualTo("PlacementRecord");
    assertThat(r2.getDataQualityCategory().getCategoryCode()).isEqualTo("INCONSISTENT");
    assertThat(r2.getDataQualitySeverity().getSeverityCode()).isEqualTo("HIGH");

    // DQ-EMPLOYMENT-003
    DataQualityRule r3 = catalogService.resolveRuleByCode("DQ-EMPLOYMENT-003");
    assertThat(r3).isNotNull();
    assertThat(r3.getTargetEntityType()).isEqualTo("EmploymentRecord");
    assertThat(r3.getDataQualityCategory().getCategoryCode()).isEqualTo("INVALID");
    assertThat(r3.getDataQualitySeverity().getSeverityCode()).isEqualTo("HIGH");

    // DQ-ATTENDANCE-004
    DataQualityRule r4 = catalogService.resolveRuleByCode("DQ-ATTENDANCE-004");
    assertThat(r4).isNotNull();
    assertThat(r4.getTargetEntityType()).isEqualTo("AttendanceRecord");
    assertThat(r4.getDataQualityCategory().getCategoryCode()).isEqualTo("INCONSISTENT");
    assertThat(r4.getDataQualitySeverity().getSeverityCode()).isEqualTo("MEDIUM");

    // DQ-DUPLICATE-005
    DataQualityRule r5 = catalogService.resolveRuleByCode("DQ-DUPLICATE-005");
    assertThat(r5).isNotNull();
    assertThat(r5.getTargetEntityType()).isEqualTo("Trainee");
    assertThat(r5.getDataQualityCategory().getCategoryCode()).isEqualTo("DUPLICATE");
    assertThat(r5.getDataQualitySeverity().getSeverityCode()).isEqualTo("HIGH");
  }

  @Test
  @DisplayName("2. Repeated rule provisioning is strictly idempotent")
  void testIdempotentProvisioning() {
    long initialCount = ruleRepository.count();
    assertThat(initialCount).isGreaterThanOrEqualTo(5L);

    // Invoke provisioning multiple times
    catalogService.ensureDefaultRulesProvisioned();
    catalogService.ensureDefaultRulesProvisioned();

    long subsequentCount = ruleRepository.count();
    assertThat(subsequentCount).isEqualTo(initialCount);
  }

  @Test
  @DisplayName("3. Provisioning preserves manual administrative configuration and does not reset disabled rules")
  void testPreserveAdministrativeConfiguration() {
    DataQualityRule r1 = catalogService.resolveRuleByCode("DQ-TRAINEE-001");
    // Manually disable rule
    r1.setLifecycleStatusId(DataQualityRuleCatalogServiceImpl.LIFECYCLE_STATUS_INACTIVE);
    ruleRepository.save(r1);

    // Run provisioning again
    catalogService.ensureDefaultRulesProvisioned();

    // Verify it remains disabled
    DataQualityRule reloaded = catalogService.resolveRuleByCode("DQ-TRAINEE-001");
    assertThat(reloaded.getLifecycleStatusId()).isEqualTo(DataQualityRuleCatalogServiceImpl.LIFECYCLE_STATUS_INACTIVE);

    // Re-enable for other tests
    reloaded.setLifecycleStatusId(DataQualityRuleCatalogServiceImpl.LIFECYCLE_STATUS_ACTIVE);
    ruleRepository.save(reloaded);
  }

  @Test
  @DisplayName("4. Unknown rule code throws DataQualityRuleNotFoundException")
  void testUnknownRuleCodeThrowsException() {
    assertThatThrownBy(() -> catalogService.resolveRuleByCode("DQ-NONEXISTENT-999"))
        .isInstanceOf(DataQualityRuleNotFoundException.class)
        .hasMessageContaining("DQ-NONEXISTENT-999");
  }

  @Test
  @DisplayName("5. Candidate rules resolution for scan filters correctly")
  void testResolveRulesForScanFiltering() {
    // By explicit rule code
    DataQualityScanRequest codeReq = new DataQualityScanRequest();
    codeReq.setRuleCode("DQ-PLACEMENT-002");
    List<DataQualityRule> codeRules = catalogService.resolveRulesForScan(codeReq);
    assertThat(codeRules).hasSize(1);
    assertThat(codeRules.get(0).getRuleCode()).isEqualTo("DQ-PLACEMENT-002");

    // By target entity type
    DataQualityScanRequest entityReq = new DataQualityScanRequest();
    entityReq.setTargetEntityType("Trainee");
    List<DataQualityRule> entityRules = catalogService.resolveRulesForScan(entityReq);
    assertThat(entityRules).extracting(DataQualityRule::getRuleCode)
        .contains("DQ-TRAINEE-001", "DQ-DUPLICATE-005")
        .doesNotContain("DQ-PLACEMENT-002", "DQ-EMPLOYMENT-003", "DQ-ATTENDANCE-004");
  }
}
