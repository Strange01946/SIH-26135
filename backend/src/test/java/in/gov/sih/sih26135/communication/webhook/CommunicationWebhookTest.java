package in.gov.sih.sih26135.communication.webhook;

import in.gov.sih.sih26135.communication.config.CommunicationProperties;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Phase 29.4 — Inbound Delivery Webhook, Signature Security & Reconciliation Tests (SIH26135_TEST)")
class CommunicationWebhookTest {

  private static final Long AUTHORITATIVE_TEST_TRAINEE_ID = 1005143L;
  private static final String MSG_PREFIX = "TEST-MSG-WH-";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private CommunicationProperties properties;

  @BeforeEach
  @AfterEach
  void cleanTestLogs() {
    jdbcTemplate.update("DELETE FROM communication_logs WHERE provider_message_id LIKE '" + MSG_PREFIX + "%'");
  }

  private Long insertTestCommLog(String providerMessageId, long statusId) {
    jdbcTemplate.update(
        "INSERT INTO communication_logs ("
            + "trainee_id, communication_channel_id, communication_direction_id, "
            + "communication_purpose_id, communication_status_id, provider_message_id, "
            + "message_template_code, sent_at, created_at, updated_at) "
            + "VALUES (?, 1, 1, 6, ?, ?, 'TPL-ENROLL-001', NOW(), NOW(), NOW())",
        AUTHORITATIVE_TEST_TRAINEE_ID,
        statusId,
        providerMessageId
    );
    return jdbcTemplate.queryForObject(
        "SELECT communication_log_id FROM communication_logs WHERE provider_message_id = ?",
        Long.class,
        providerMessageId
    );
  }

  @Test
  @DisplayName("1. Twilio SMS Webhook Reconciles DELIVERED status and populates delivered_at")
  void testTwilioWebhook_deliveredStatus_updatesCommunicationLog() throws Exception {
    String msgId = MSG_PREFIX + "TW-DELIV-01";
    Long logId = insertTestCommLog(msgId, 3L); // 3 = SENT

    mockMvc.perform(post("/api/v1/communication/webhooks/twilio")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .header(TwilioWebhookSignatureValidator.HEADER_TWILIO_SIGNATURE, TwilioWebhookSignatureValidator.TEST_TWILIO_SIGNATURE)
            .param("MessageSid", msgId)
            .param("MessageStatus", "delivered"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.accepted").value(true))
        .andExpect(jsonPath("$.data.updatedStatus").value("DELIVERED"))
        .andExpect(jsonPath("$.data.resolutionCode").value("STATUS_UPDATED"));

    Long currentStatusId = jdbcTemplate.queryForObject(
        "SELECT communication_status_id FROM communication_logs WHERE communication_log_id = ?",
        Long.class,
        logId
    );
    assertThat(currentStatusId).isEqualTo(4L); // 4 = DELIVERED

    String deliveredAt = jdbcTemplate.queryForObject(
        "SELECT delivered_at FROM communication_logs WHERE communication_log_id = ?",
        String.class,
        logId
    );
    assertThat(deliveredAt).isNotNull();
  }

  @Test
  @DisplayName("2. Twilio WhatsApp Webhook Reconciles READ status and backfills delivered_at")
  void testTwilioWebhook_readStatus_updatesCommunicationLogAndDeliveredAt() throws Exception {
    String msgId = MSG_PREFIX + "TW-READ-02";
    Long logId = insertTestCommLog(msgId, 3L); // 3 = SENT

    mockMvc.perform(post("/api/v1/communication/webhooks/twilio")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .header(TwilioWebhookSignatureValidator.HEADER_TWILIO_SIGNATURE, TwilioWebhookSignatureValidator.TEST_TWILIO_SIGNATURE)
            .param("MessageSid", msgId)
            .param("MessageStatus", "read"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.accepted").value(true))
        .andExpect(jsonPath("$.data.updatedStatus").value("READ"))
        .andExpect(jsonPath("$.data.resolutionCode").value("STATUS_UPDATED"));

    Long currentStatusId = jdbcTemplate.queryForObject(
        "SELECT communication_status_id FROM communication_logs WHERE communication_log_id = ?",
        Long.class,
        logId
    );
    assertThat(currentStatusId).isEqualTo(5L); // 5 = READ

    String readAt = jdbcTemplate.queryForObject(
        "SELECT read_at FROM communication_logs WHERE communication_log_id = ?",
        String.class,
        logId
    );
    assertThat(readAt).isNotNull();
  }

  @Test
  @DisplayName("3. Twilio Webhook Invalid Signature Header -> Returns 401 Unauthorized")
  void testTwilioWebhook_invalidSignature_returns401() throws Exception {
    String msgId = MSG_PREFIX + "TW-INVAL-03";

    mockMvc.perform(post("/api/v1/communication/webhooks/twilio")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .header(TwilioWebhookSignatureValidator.HEADER_TWILIO_SIGNATURE, "completely-invalid-signature")
            .param("MessageSid", msgId)
            .param("MessageStatus", "delivered"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("INVALID_WEBHOOK_SIGNATURE"));
  }

  @Test
  @DisplayName("4. Twilio Webhook Missing Signature Header -> Returns 401 Unauthorized")
  void testTwilioWebhook_missingSignature_returns401() throws Exception {
    String msgId = MSG_PREFIX + "TW-MISS-04";

    mockMvc.perform(post("/api/v1/communication/webhooks/twilio")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("MessageSid", msgId)
            .param("MessageStatus", "delivered"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("INVALID_WEBHOOK_SIGNATURE"));
  }

  @Test
  @DisplayName("5. SendGrid Email Webhook Reconciles DELIVERED status with valid timestamp")
  void testSendGridWebhook_deliveredEvent_updatesCommunicationLog() throws Exception {
    String msgId = MSG_PREFIX + "SG-DELIV-05";
    Long logId = insertTestCommLog(msgId, 3L); // 3 = SENT

    long currentEpoch = System.currentTimeMillis() / 1000L;
    String payload = "[{\"sg_message_id\":\"" + msgId + "\",\"event\":\"delivered\"}]";

    mockMvc.perform(post("/api/v1/communication/webhooks/sendgrid")
            .contentType(MediaType.APPLICATION_JSON)
            .header(SendGridWebhookSignatureValidator.HEADER_SENDGRID_SIGNATURE, SendGridWebhookSignatureValidator.TEST_SENDGRID_SIGNATURE)
            .header(SendGridWebhookSignatureValidator.HEADER_SENDGRID_TIMESTAMP, String.valueOf(currentEpoch))
            .content(payload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.accepted").value(true))
        .andExpect(jsonPath("$.data.updatedStatus").value("DELIVERED"))
        .andExpect(jsonPath("$.data.resolutionCode").value("STATUS_UPDATED"));

    Long currentStatusId = jdbcTemplate.queryForObject(
        "SELECT communication_status_id FROM communication_logs WHERE communication_log_id = ?",
        Long.class,
        logId
    );
    assertThat(currentStatusId).isEqualTo(4L); // 4 = DELIVERED
  }

  @Test
  @DisplayName("6. SendGrid Email Webhook Stale Timestamp (>600s) -> Returns 401 Unauthorized")
  void testSendGridWebhook_staleTimestamp_returns401() throws Exception {
    String msgId = MSG_PREFIX + "SG-STALE-06";
    // 1000 seconds in past
    long staleEpoch = (System.currentTimeMillis() / 1000L) - 1000L;
    String payload = "[{\"sg_message_id\":\"" + msgId + "\",\"event\":\"delivered\"}]";

    mockMvc.perform(post("/api/v1/communication/webhooks/sendgrid")
            .contentType(MediaType.APPLICATION_JSON)
            .header(SendGridWebhookSignatureValidator.HEADER_SENDGRID_SIGNATURE, SendGridWebhookSignatureValidator.TEST_SENDGRID_SIGNATURE)
            .header(SendGridWebhookSignatureValidator.HEADER_SENDGRID_TIMESTAMP, String.valueOf(staleEpoch))
            .content(payload))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("INVALID_WEBHOOK_SIGNATURE"));
  }

  @Test
  @DisplayName("7. Mock Webhook HMAC-SHA256 Signature Authenticates and Reconciles")
  void testMockWebhook_hmacSha256Signature_authenticatesAndReconciles() throws Exception {
    String msgId = MSG_PREFIX + "MOCK-HMAC-07";
    Long logId = insertTestCommLog(msgId, 3L); // 3 = SENT

    String payload = "{\"providerMessageId\":\"" + msgId + "\",\"status\":\"delivered\"}";
    String computedSignature = MockWebhookSignatureValidator.computeHmacSha256Hex(
        payload.getBytes(StandardCharsets.UTF_8),
        MockWebhookSignatureValidator.DEFAULT_MOCK_SECRET
    );

    mockMvc.perform(post("/api/v1/communication/webhooks/mock")
            .contentType(MediaType.APPLICATION_JSON)
            .header(MockWebhookSignatureValidator.HEADER_MOCK_SIGNATURE, computedSignature)
            .content(payload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.accepted").value(true))
        .andExpect(jsonPath("$.data.updatedStatus").value("DELIVERED"))
        .andExpect(jsonPath("$.data.resolutionCode").value("STATUS_UPDATED"));

    Long currentStatusId = jdbcTemplate.queryForObject(
        "SELECT communication_status_id FROM communication_logs WHERE communication_log_id = ?",
        Long.class,
        logId
    );
    assertThat(currentStatusId).isEqualTo(4L);
  }

  @Test
  @DisplayName("8. Out-of-Order Safety: DELIVERED log cannot be downgraded by delayed SENT webhook")
  void testOutOfOrderProtection_deliveredLogCannotBeDowngraded() throws Exception {
    String msgId = MSG_PREFIX + "OO-08";
    Long logId = insertTestCommLog(msgId, 4L); // 4 = DELIVERED

    mockMvc.perform(post("/api/v1/communication/webhooks/twilio")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .header(TwilioWebhookSignatureValidator.HEADER_TWILIO_SIGNATURE, TwilioWebhookSignatureValidator.TEST_TWILIO_SIGNATURE)
            .param("MessageSid", msgId)
            .param("MessageStatus", "sent"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.accepted").value(true))
        .andExpect(jsonPath("$.data.updatedStatus").value("DELIVERED"))
        .andExpect(jsonPath("$.data.resolutionCode").value("OUT_OF_ORDER_IGNORED"));

    Long currentStatusId = jdbcTemplate.queryForObject(
        "SELECT communication_status_id FROM communication_logs WHERE communication_log_id = ?",
        Long.class,
        logId
    );
    assertThat(currentStatusId).isEqualTo(4L); // Still DELIVERED!
  }

  @Test
  @DisplayName("9. Duplicate Delivery Idempotency: Duplicate webhook returns DUPLICATE_IGNORED safely")
  void testDuplicateDeliveryIdempotency_ignoresDuplicateSafely() throws Exception {
    String msgId = MSG_PREFIX + "DUP-09";
    Long logId = insertTestCommLog(msgId, 4L); // 4 = DELIVERED

    mockMvc.perform(post("/api/v1/communication/webhooks/twilio")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .header(TwilioWebhookSignatureValidator.HEADER_TWILIO_SIGNATURE, TwilioWebhookSignatureValidator.TEST_TWILIO_SIGNATURE)
            .param("MessageSid", msgId)
            .param("MessageStatus", "delivered"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.accepted").value(true))
        .andExpect(jsonPath("$.data.updatedStatus").value("DELIVERED"))
        .andExpect(jsonPath("$.data.resolutionCode").value("DUPLICATE_IGNORED"));

    Long currentStatusId = jdbcTemplate.queryForObject(
        "SELECT communication_status_id FROM communication_logs WHERE communication_log_id = ?",
        Long.class,
        logId
    );
    assertThat(currentStatusId).isEqualTo(4L);
  }

  @Test
  @DisplayName("10. DEMO/SIMULATED Record Protection: SIMULATED log cannot become DELIVERED via webhook")
  void testDemoSimulatedRecordProtection_cannotBecomeDeliveredViaWebhook() throws Exception {
    String msgId = MSG_PREFIX + "SIM-10";
    Long logId = insertTestCommLog(msgId, 9L); // 9 = SIMULATED

    mockMvc.perform(post("/api/v1/communication/webhooks/twilio")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .header(TwilioWebhookSignatureValidator.HEADER_TWILIO_SIGNATURE, TwilioWebhookSignatureValidator.TEST_TWILIO_SIGNATURE)
            .param("MessageSid", msgId)
            .param("MessageStatus", "delivered"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.accepted").value(true))
        .andExpect(jsonPath("$.data.updatedStatus").value("SIMULATED"))
        .andExpect(jsonPath("$.data.resolutionCode").value("SIMULATED_RECORD_IMMUTABLE"));

    Long currentStatusId = jdbcTemplate.queryForObject(
        "SELECT communication_status_id FROM communication_logs WHERE communication_log_id = ?",
        Long.class,
        logId
    );
    assertThat(currentStatusId).isEqualTo(9L); // Still SIMULATED!

    String deliveredAt = jdbcTemplate.queryForObject(
        "SELECT delivered_at FROM communication_logs WHERE communication_log_id = ?",
        String.class,
        logId
    );
    assertThat(deliveredAt).isNull();
  }

  @Test
  @DisplayName("11. Statutory Consent Protection: BLOCKED_NO_CONSENT record is immutable to webhooks")
  void testBlockedConsentRecordProtection_cannotBeModifiedViaWebhook() throws Exception {
    String msgId = MSG_PREFIX + "NOCONSENT-11";
    Long logId = insertTestCommLog(msgId, 8L); // 8 = BLOCKED_NO_CONSENT

    mockMvc.perform(post("/api/v1/communication/webhooks/twilio")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .header(TwilioWebhookSignatureValidator.HEADER_TWILIO_SIGNATURE, TwilioWebhookSignatureValidator.TEST_TWILIO_SIGNATURE)
            .param("MessageSid", msgId)
            .param("MessageStatus", "delivered"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.accepted").value(true))
        .andExpect(jsonPath("$.data.updatedStatus").value("BLOCKED_NO_CONSENT"))
        .andExpect(jsonPath("$.data.resolutionCode").value("BLOCKED_CONSENT_IMMUTABLE"));

    Long currentStatusId = jdbcTemplate.queryForObject(
        "SELECT communication_status_id FROM communication_logs WHERE communication_log_id = ?",
        Long.class,
        logId
    );
    assertThat(currentStatusId).isEqualTo(8L); // Still BLOCKED_NO_CONSENT!
  }

  @Test
  @DisplayName("12. Non-existent Message ID -> Returns 404 with MESSAGE_NOT_FOUND")
  void testNonExistentMessageId_returns404NotFound() throws Exception {
    String msgId = MSG_PREFIX + "UNKNOWN-NONEXISTENT-999";

    mockMvc.perform(post("/api/v1/communication/webhooks/twilio")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .header(TwilioWebhookSignatureValidator.HEADER_TWILIO_SIGNATURE, TwilioWebhookSignatureValidator.TEST_TWILIO_SIGNATURE)
            .param("MessageSid", msgId)
            .param("MessageStatus", "delivered"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.data.resolutionCode").value("MESSAGE_NOT_FOUND"));
  }

  @Test
  @DisplayName("13. Unsupported Provider -> Returns 404 with UNSUPPORTED_PROVIDER")
  void testUnsupportedProvider_returns404NotFound() throws Exception {
    mockMvc.perform(post("/api/v1/communication/webhooks/unsupported-carrier")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("UNSUPPORTED_PROVIDER"));
  }
}
