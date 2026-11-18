package in.gov.sih.sih26135.communication.template;

import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.exception.BadRequestException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Phase 29.4 — Notification Templating, Parameter Allowlisting & Security Unit Tests")
class CommunicationTemplateServiceTest {

  private CommunicationTemplateRegistry registry;
  private CommunicationTemplateService service;
  private Trainee testTrainee;

  @BeforeEach
  void setUp() {
    registry = new CommunicationTemplateRegistry();
    service = new CommunicationTemplateServiceImpl(registry);

    testTrainee = new Trainee();
    testTrainee.setId(1005143L);
    testTrainee.setFirstName("Aditya");
    testTrainee.setLastName("Sharma");
    testTrainee.setPhone("+919876543210");
    testTrainee.setEmail("aditya.sharma@example.gov.in");
  }

  @Test
  @DisplayName("1. TPL-ENROLL-001: Enrolls trainee with all required parameters and validates output")
  void testTplEnroll001_success() {
    Map<String, String> params = new HashMap<>();
    params.put("courseName", "Solar PV Installation & Maintenance");
    params.put("batchCode", "BATCH-2026-SPV-01");
    params.put("startDate", "2026-12-01");
    params.put("trainingCenterName", "National Skill Training Institute, Noida");

    CommunicationTemplateRegistry.RenderedTemplate rendered = service.renderTemplate("TPL-ENROLL-001", params, testTrainee);

    assertThat(rendered.templateCode()).isEqualTo("TPL-ENROLL-001");
    assertThat(rendered.subject()).contains("Training Enrollment Confirmation", "BATCH-2026-SPV-01");
    assertThat(rendered.bodyText()).contains("Dear Aditya Sharma", "BATCH-2026-SPV-01", "2026-12-01", "National Skill Training Institute, Noida");
    assertThat(rendered.bodyHtml()).contains("&bull; Automated System Communication");
  }

  @Test
  @DisplayName("2. TPL-ASSESS-002: Renders assessment notice with trusted certificate verification URL")
  void testTplAssess002_success() {
    Map<String, String> params = new HashMap<>();
    params.put("courseName", "Electric Vehicle Technician");
    params.put("resultStatus", "PASSED");
    params.put("certificateNumber", "CERT-EV-2026-99");
    params.put("certificateUrl", "https://skillindia.gov.in/verify/cert/CERT-EV-2026-99");

    CommunicationTemplateRegistry.RenderedTemplate rendered = service.renderTemplate("TPL-ASSESS-002", params, testTrainee);

    assertThat(rendered.subject()).contains("Assessment Result & Certificate Issued");
    assertThat(rendered.bodyText()).contains("Aditya Sharma", "Electric Vehicle Technician", "PASSED", "CERT-EV-2026-99");
    assertThat(rendered.bodyText()).contains("https://skillindia.gov.in/verify/cert/CERT-EV-2026-99");
  }

  @Test
  @DisplayName("3. TPL-VERIF-003: Renders employment verification request with trusted verification portal link")
  void testTplVerif003_success() {
    Map<String, String> params = new HashMap<>();
    params.put("employerName", "Tata Power Solar Systems Ltd");
    params.put("verificationId", "REQ-2026-0042");
    params.put("verificationUrl", "https://sih.gov.in/employer/verify/REQ-2026-0042");

    CommunicationTemplateRegistry.RenderedTemplate rendered = service.renderTemplate("TPL-VERIF-003", params, testTrainee);

    assertThat(rendered.subject()).contains("Employment Verification Request");
    assertThat(rendered.bodyText()).contains("Aditya Sharma", "Tata Power Solar Systems Ltd", "REQ-2026-0042", "https://sih.gov.in/employer/verify/REQ-2026-0042");
  }

  @Test
  @DisplayName("4. TPL-SURVEY-004: Validates milestone allowed values (3, 6, 12 months) and trusted survey URL")
  void testTplSurvey004_success() {
    for (String validMilestone : new String[]{"3", "6", "12"}) {
      Map<String, String> params = new HashMap<>();
      params.put("milestoneMonths", validMilestone);
      params.put("surveyId", "SURV-2026-" + validMilestone);
      params.put("surveyUrl", "https://skillindia.gov.in/surveys/followup?id=SURV-2026-998");

      CommunicationTemplateRegistry.RenderedTemplate rendered = service.renderTemplate("TPL-SURVEY-004", params, testTrainee);
      assertThat(rendered.subject()).contains(validMilestone + "-Month Post-Placement Follow-up Survey");
      assertThat(rendered.bodyText()).contains(validMilestone + "-month employment milestone", "SURV-2026-" + validMilestone);
    }
  }

  @Test
  @DisplayName("5. TPL-SURVEY-004: Rejects invalid survey milestones (e.g. 5, 9, 24)")
  void testTplSurvey004_invalidMilestone() {
    Map<String, String> params = new HashMap<>();
    params.put("milestoneMonths", "9");
    params.put("surveyId", "SURV-2026-9");
    params.put("surveyUrl", "https://skillindia.gov.in/surveys/followup");

    assertThatThrownBy(() -> service.renderTemplate("TPL-SURVEY-004", params, testTrainee))
        .isInstanceOf(BadRequestException.class)
        .extracting(e -> ((BadRequestException) e).getErrorCode())
        .isEqualTo("INVALID_SURVEY_MILESTONE");
  }

  @Test
  @DisplayName("6. TPL-ALERT-005: Renders administrative notification with severity and reference ID")
  void testTplAlert005_success() {
    Map<String, String> params = new HashMap<>();
    params.put("alertType", "BIOMETRIC_SYNC_NOTICE");
    params.put("actionRequired", "Submit attendance within 24 hours");
    params.put("referenceId", "ATT-SYNC-2026-88");

    CommunicationTemplateRegistry.RenderedTemplate rendered = service.renderTemplate("TPL-ALERT-005", params, testTrainee);
    assertThat(rendered.subject()).contains("Critical Operational Alert - BIOMETRIC_SYNC_NOTICE");
    assertThat(rendered.bodyText()).contains("BIOMETRIC_SYNC_NOTICE", "Submit attendance within 24 hours", "ATT-SYNC-2026-88");
  }

  @Test
  @DisplayName("7. Allowlist Enforcement: Unknown parameters are strictly rejected")
  void testAllowlistEnforcement_rejectsUnknownParameter() {
    Map<String, String> params = new HashMap<>();
    params.put("courseName", "Data Science");
    params.put("batchCode", "B-01");
    params.put("startDate", "2026-12-01");
    params.put("trainingCenterName", "Main NSTI");
    params.put("unauthorizedField", "exploit_value");

    assertThatThrownBy(() -> service.renderTemplate("TPL-ENROLL-001", params, testTrainee))
        .isInstanceOf(BadRequestException.class)
        .extracting(e -> ((BadRequestException) e).getErrorCode())
        .isEqualTo("PARAMETER_ALLOWLIST_VIOLATION");
  }

  @Test
  @DisplayName("8. Mandatory Parameters: Missing required parameters fail closed")
  void testMissingRequiredParameter_fails() {
    Map<String, String> params = new HashMap<>();
    params.put("courseName", "Data Science");
    // Missing batchCode, startDate, trainingCenterName

    assertThatThrownBy(() -> service.renderTemplate("TPL-ENROLL-001", params, testTrainee))
        .isInstanceOf(BadRequestException.class)
        .extracting(e -> ((BadRequestException) e).getErrorCode())
        .isEqualTo("MISSING_TEMPLATE_PARAMETER");
  }

  @Test
  @DisplayName("9. Sensitive Pattern Rejection: 12-digit Aadhaar pattern is strictly rejected")
  void testSensitivePattern_rejectsAadhaar() {
    Map<String, String> params = new HashMap<>();
    params.put("courseName", "Data Science with Aadhaar 999988887777");
    params.put("batchCode", "B-01");
    params.put("startDate", "2026-12-01");
    params.put("trainingCenterName", "NSTI");

    assertThatThrownBy(() -> service.renderTemplate("TPL-ENROLL-001", params, testTrainee))
        .isInstanceOf(BadRequestException.class)
        .extracting(e -> ((BadRequestException) e).getErrorCode())
        .isEqualTo("SENSITIVE_PARAMETER_REJECTED");
  }

  @Test
  @DisplayName("10. Sensitive Pattern Rejection: Indian PAN format is strictly rejected")
  void testSensitivePattern_rejectsPan() {
    Map<String, String> params = new HashMap<>();
    params.put("courseName", "Accounts ABCDE1234F");
    params.put("batchCode", "B-01");
    params.put("startDate", "2026-12-01");
    params.put("trainingCenterName", "NSTI");

    assertThatThrownBy(() -> service.renderTemplate("TPL-ENROLL-001", params, testTrainee))
        .isInstanceOf(BadRequestException.class)
        .extracting(e -> ((BadRequestException) e).getErrorCode())
        .isEqualTo("SENSITIVE_PARAMETER_REJECTED");
  }

  @Test
  @DisplayName("11. Sensitive Pattern Rejection: Bearer token is strictly rejected")
  void testSensitivePattern_rejectsBearerToken() {
    Map<String, String> params = new HashMap<>();
    params.put("alertType", "Security Notice");
    params.put("actionRequired", "Here is your token: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9");
    params.put("referenceId", "SEC-001");

    assertThatThrownBy(() -> service.renderTemplate("TPL-ALERT-005", params, testTrainee))
        .isInstanceOf(BadRequestException.class)
        .extracting(e -> ((BadRequestException) e).getErrorCode())
        .isEqualTo("SENSITIVE_PARAMETER_REJECTED");
  }

  @Test
  @DisplayName("12. Trusted URL Validation: Non-HTTPS scheme is rejected")
  void testTrustedUrl_rejectsHttpScheme() {
    Map<String, String> params = new HashMap<>();
    params.put("courseName", "Electric Vehicle");
    params.put("resultStatus", "PASSED");
    params.put("certificateNumber", "CERT-001");
    params.put("certificateUrl", "http://skillindia.gov.in/verify");

    assertThatThrownBy(() -> service.renderTemplate("TPL-ASSESS-002", params, testTrainee))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("must use HTTPS")
        .extracting(e -> ((BadRequestException) e).getErrorCode())
        .isEqualTo("HTTPS_REQUIRED");
  }

  @Test
  @DisplayName("13. Trusted URL Validation: Untrusted external domain is rejected")
  void testTrustedUrl_rejectsUntrustedExternalDomain() {
    Map<String, String> params = new HashMap<>();
    params.put("employerName", "Tata Power");
    params.put("verificationId", "REQ-001");
    params.put("verificationUrl", "https://phishing-site-attacker.com/verify");

    assertThatThrownBy(() -> service.renderTemplate("TPL-VERIF-003", params, testTrainee))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("not on the trusted application domain allowlist")
        .extracting(e -> ((BadRequestException) e).getErrorCode())
        .isEqualTo("UNTRUSTED_URL_HOST");
  }

  @Test
  @DisplayName("14. HTML Body Sanitization: Injected HTML tags are safely entity-escaped")
  void testHtmlSanitization_escapesTags() {
    Map<String, String> params = new HashMap<>();
    params.put("courseName", "Web Development <script>alert('xss')</script>");
    params.put("batchCode", "B-01");
    params.put("startDate", "2026-12-01");
    params.put("trainingCenterName", "NSTI <b>Bold Center</b>");

    CommunicationTemplateRegistry.RenderedTemplate rendered = service.renderTemplate("TPL-ENROLL-001", params, testTrainee);

    assertThat(rendered.bodyHtml()).doesNotContain("<script>");
    assertThat(rendered.bodyHtml()).contains("&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt;");
    assertThat(rendered.bodyHtml()).contains("&lt;b&gt;Bold Center&lt;/b&gt;");
  }

  @Test
  @DisplayName("15. Backward Compatibility: Preserves Phase 29.3 template codes without regressions")
  void testBackwardCompatibility_phase29_3_templates() {
    // OPERATIONAL_ALERT
    Map<String, String> opParams = Map.of("referenceId", "REF-OP-9988");
    CommunicationTemplateRegistry.RenderedTemplate opRendered = service.renderTemplate("OPERATIONAL_ALERT", opParams, testTrainee);
    assertThat(opRendered.bodyText()).contains("REF-OP-9988");

    // LIVE_SMOKE_TEST
    Map<String, String> smokeParams = Map.of(
        "providerName", "TwilioSmsProvider",
        "channel", "SMS",
        "timestamp", "2026-11-18T10:00:00"
    );
    CommunicationTemplateRegistry.RenderedTemplate smokeRendered = service.renderTemplate("LIVE_SMOKE_TEST", smokeParams, testTrainee);
    assertThat(smokeRendered.bodyText()).contains("TwilioSmsProvider", "SMS");

    // TRAINEE_ENROLLMENT_CONFIRMATION
    Map<String, String> enrollParams = Map.of(
        "batchCode", "BATCH-PHASE29-3",
        "courseName", "Solar Installer",
        "startDate", "2026-11-20"
    );
    CommunicationTemplateRegistry.RenderedTemplate enrollRendered = service.renderTemplate("TRAINEE_ENROLLMENT_CONFIRMATION", enrollParams, testTrainee);
    assertThat(enrollRendered.bodyText()).contains("BATCH-PHASE29-3", "Solar Installer", "Aditya Sharma");
  }
}
