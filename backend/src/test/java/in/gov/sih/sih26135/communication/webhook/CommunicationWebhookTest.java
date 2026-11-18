package in.gov.sih.sih26135.communication.webhook;

import in.gov.sih.sih26135.communication.config.CommunicationProperties;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;
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
    properties.setAllowTestWebhookSignatures(true);
    jdbcTemplate.update("DELETE FROM communication_logs WHERE provider_message_id LIKE '" + MSG_PREFIX + "%'");
  }

  private Long insertTestCommLog(String providerMessageId, long statusId) {
    return insertTestCommLog(providerMessageId, 1L, statusId); // 1 = SMS
  }

  private Long insertTestCommLog(String providerMessageId, long channelId, long statusId) {
    jdbcTemplate.update(
        "INSERT INTO communication_logs ("
            + "trainee_id, communication_channel_id, communication_direction_id, "
            + "communication_purpose_id, communication_status_id, provider_message_id, "
            + "message_template_code, sent_at, created_at, updated_at) "
            + "VALUES (?, ?, 1, 6, ?, ?, 'TPL-ENROLL-001', NOW(), NOW(), NOW())",
        AUTHORITATIVE_TEST_TRAINEE_ID,
        channelId,
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
    Long logId = insertTestCommLog(msgId, 2L, 3L); // 2 = WHATSAPP, 3 = SENT

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
    Long logId = insertTestCommLog(msgId, 3L, 3L); // 3 = EMAIL, 3 = SENT

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
  @DisplayName("7. SendGrid Genuine ECDSA (SHA256withECDSA on P-256) Signature Verification")
  void testSendGridWebhook_genuineEcdsaSignature_verifiesAndReconciles() throws Exception {
    String msgId = MSG_PREFIX + "SG-ECDSA-07";
    Long logId = insertTestCommLog(msgId, 3L, 3L); // 3 = EMAIL, 3 = SENT

    // Temporarily disable test signature bypass to prove real cryptographic ECDSA verification
    properties.setAllowTestWebhookSignatures(false);

    // Generate real EC key pair (P-256)
    KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
    kpg.initialize(256);
    KeyPair keyPair = kpg.generateKeyPair();

    String publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    properties.getSendgrid().setWebhookVerificationKey(publicKeyBase64);

    long nowEpoch = System.currentTimeMillis() / 1000L;
    String timestamp = String.valueOf(nowEpoch);
    String payload = "[{\"sg_message_id\":\"" + msgId + "\",\"event\":\"delivered\"}]";

    // Sign timestamp + payload with SHA256withECDSA
    Signature signer = Signature.getInstance("SHA256withECDSA");
    signer.initSign(keyPair.getPrivate());
    signer.update(timestamp.getBytes(StandardCharsets.UTF_8));
    signer.update(payload.getBytes(StandardCharsets.UTF_8));
    String realSignatureBase64 = Base64.getEncoder().encodeToString(signer.sign());

    try {
      mockMvc.perform(post("/api/v1/communication/webhooks/sendgrid")
              .contentType(MediaType.APPLICATION_JSON)
              .header(SendGridWebhookSignatureValidator.HEADER_SENDGRID_SIGNATURE, realSignatureBase64)
              .header(SendGridWebhookSignatureValidator.HEADER_SENDGRID_TIMESTAMP, timestamp)
              .content(payload))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.accepted").value(true))
          .andExpect(jsonPath("$.data.updatedStatus").value("DELIVERED"));

      Long currentStatusId = jdbcTemplate.queryForObject(
          "SELECT communication_status_id FROM communication_logs WHERE communication_log_id = ?",
          Long.class,
          logId
      );
      assertThat(currentStatusId).isEqualTo(4L);
    } finally {
      properties.setAllowTestWebhookSignatures(true);
      properties.getSendgrid().setWebhookVerificationKey(null);
    }
  }

  @Test
  @DisplayName("8. SendGrid Tampered ECDSA Signature -> Returns 401 Unauthorized")
  void testSendGridWebhook_tamperedEcdsaSignature_returns401() throws Exception {
    properties.setAllowTestWebhookSignatures(false);

    KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
    kpg.initialize(256);
    KeyPair keyPair = kpg.generateKeyPair();
    String publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    properties.getSendgrid().setWebhookVerificationKey(publicKeyBase64);

    long nowEpoch = System.currentTimeMillis() / 1000L;
    String payload = "[{\"sg_message_id\":\"fake\",\"event\":\"delivered\"}]";

    try {
      mockMvc.perform(post("/api/v1/communication/webhooks/sendgrid")
              .contentType(MediaType.APPLICATION_JSON)
              .header(SendGridWebhookSignatureValidator.HEADER_SENDGRID_SIGNATURE, "MEQCIBeFakeSignatureThatFailsCryptographicCheck12345678901234567890=")
              .header(SendGridWebhookSignatureValidator.HEADER_SENDGRID_TIMESTAMP, String.valueOf(nowEpoch))
              .content(payload))
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.code").value("INVALID_WEBHOOK_SIGNATURE"));
    } finally {
      properties.setAllowTestWebhookSignatures(true);
      properties.getSendgrid().setWebhookVerificationKey(null);
    }
  }

  @Test
  @DisplayName("9. Rejection of Unknown Status: Unknown raw status returns 400 and never manufactures SENT")
  void testWebhook_unknownStatus_returnsBadRequest_neverManufacturesSent() throws Exception {
    String msgId = MSG_PREFIX + "TW-UNKNOWN-09";
    Long logId = insertTestCommLog(msgId, 1L); // 1 = QUEUED

    mockMvc.perform(post("/api/v1/communication/webhooks/twilio")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .header(TwilioWebhookSignatureValidator.HEADER_TWILIO_SIGNATURE, TwilioWebhookSignatureValidator.TEST_TWILIO_SIGNATURE)
            .param("MessageSid", msgId)
            .param("MessageStatus", "unrecognized_carrier_state"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.data.resolutionCode").value("UNKNOWN_WEBHOOK_STATUS"));

    // Verify database record status is strictly unchanged (still QUEUED = 1L, NOT SENT = 3L)
    Long currentStatusId = jdbcTemplate.queryForObject(
        "SELECT communication_status_id FROM communication_logs WHERE communication_log_id = ?",
        Long.class,
        logId
    );
    assertThat(currentStatusId).isEqualTo(1L);
  }

  @Test
  @DisplayName("10. Legitimate Failure Progression: Delivery failure (FAILED) is accepted after SENT")
  void testWebhook_deliveryFailureAfterSent_updatesToFailed() throws Exception {
    String msgId = MSG_PREFIX + "TW-FAIL-10";
    Long logId = insertTestCommLog(msgId, 3L); // 3 = SENT

    mockMvc.perform(post("/api/v1/communication/webhooks/twilio")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .header(TwilioWebhookSignatureValidator.HEADER_TWILIO_SIGNATURE, TwilioWebhookSignatureValidator.TEST_TWILIO_SIGNATURE)
            .param("MessageSid", msgId)
            .param("MessageStatus", "undelivered")
            .param("ErrorCode", "30008")
            .param("ErrorMessage", "Unknown carrier delivery error"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.accepted").value(true))
        .andExpect(jsonPath("$.data.updatedStatus").value("FAILED"))
        .andExpect(jsonPath("$.data.resolutionCode").value("STATUS_UPDATED"));

    Long currentStatusId = jdbcTemplate.queryForObject(
        "SELECT communication_status_id FROM communication_logs WHERE communication_log_id = ?",
        Long.class,
        logId
    );
    assertThat(currentStatusId).isEqualTo(7L); // 7 = FAILED
  }

  @Test
  @DisplayName("11. Out-of-Order Safety: DELIVERED log cannot be downgraded by delayed SENT webhook")
  void testOutOfOrderProtection_deliveredLogCannotBeDowngraded() throws Exception {
    String msgId = MSG_PREFIX + "OO-11";
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
    assertThat(currentStatusId).isEqualTo(4L);
  }

  @Test
  @DisplayName("12. Out-of-Order Safety: DELIVERED log cannot be downgraded to FAILED by delayed failure webhook")
  void testOutOfOrderProtection_deliveredLogCannotTransitionToFailed() throws Exception {
    String msgId = MSG_PREFIX + "OO-FAIL-12";
    Long logId = insertTestCommLog(msgId, 4L); // 4 = DELIVERED

    mockMvc.perform(post("/api/v1/communication/webhooks/twilio")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .header(TwilioWebhookSignatureValidator.HEADER_TWILIO_SIGNATURE, TwilioWebhookSignatureValidator.TEST_TWILIO_SIGNATURE)
            .param("MessageSid", msgId)
            .param("MessageStatus", "failed"))
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
    assertThat(currentStatusId).isEqualTo(4L); // Still DELIVERED
  }

  @Test
  @DisplayName("13. SendGrid JSON Event Array: Processes all events in a batch array")
  void testSendGridWebhook_multiEventArray_processesAllEvents() throws Exception {
    String msgId1 = MSG_PREFIX + "SG-ARR-13A";
    String msgId2 = MSG_PREFIX + "SG-ARR-13B";
    Long logId1 = insertTestCommLog(msgId1, 3L, 3L); // EMAIL, SENT
    Long logId2 = insertTestCommLog(msgId2, 3L, 3L); // EMAIL, SENT

    long currentEpoch = System.currentTimeMillis() / 1000L;
    String payload = "["
        + "{\"sg_message_id\":\"" + msgId1 + "\",\"event\":\"delivered\"},"
        + "{\"sg_message_id\":\"" + msgId2 + "\",\"event\":\"open\"}"
        + "]";

    mockMvc.perform(post("/api/v1/communication/webhooks/sendgrid")
            .contentType(MediaType.APPLICATION_JSON)
            .header(SendGridWebhookSignatureValidator.HEADER_SENDGRID_SIGNATURE, SendGridWebhookSignatureValidator.TEST_SENDGRID_SIGNATURE)
            .header(SendGridWebhookSignatureValidator.HEADER_SENDGRID_TIMESTAMP, String.valueOf(currentEpoch))
            .content(payload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.accepted").value(true))
        .andExpect(jsonPath("$.data.resolutionCode").value("BATCH_PROCESSED"));

    Long statusId1 = jdbcTemplate.queryForObject(
        "SELECT communication_status_id FROM communication_logs WHERE communication_log_id = ?",
        Long.class,
        logId1
    );
    Long statusId2 = jdbcTemplate.queryForObject(
        "SELECT communication_status_id FROM communication_logs WHERE communication_log_id = ?",
        Long.class,
        logId2
    );
    assertThat(statusId1).isEqualTo(4L); // 4 = DELIVERED
    assertThat(statusId2).isEqualTo(5L); // 5 = READ
  }

  @Test
  @DisplayName("14. Provider/Channel Mismatch: Twilio webhook rejected when targeting EMAIL log")
  void testWebhook_providerChannelMismatch_rejected() throws Exception {
    String msgId = MSG_PREFIX + "MISMATCH-14";
    Long logId = insertTestCommLog(msgId, 3L, 3L); // 3 = EMAIL channel

    mockMvc.perform(post("/api/v1/communication/webhooks/twilio")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .header(TwilioWebhookSignatureValidator.HEADER_TWILIO_SIGNATURE, TwilioWebhookSignatureValidator.TEST_TWILIO_SIGNATURE)
            .param("MessageSid", msgId)
            .param("MessageStatus", "delivered"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.data.resolutionCode").value("PROVIDER_CHANNEL_MISMATCH"));
  }

  @Test
  @DisplayName("15. Mock Webhook HMAC-SHA256 Signature Authenticates and Reconciles")
  void testMockWebhook_hmacSha256Signature_authenticatesAndReconciles() throws Exception {
    String msgId = MSG_PREFIX + "MOCK-HMAC-15";
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
  @DisplayName("16. Mock Webhook Disabled when allowTestWebhookSignatures is false -> 401")
  void testMockWebhook_disabledOutsideTestEnvironments_returns401() throws Exception {
    properties.setAllowTestWebhookSignatures(false);
    String payload = "{\"providerMessageId\":\"any\",\"status\":\"delivered\"}";

    try {
      mockMvc.perform(post("/api/v1/communication/webhooks/mock")
              .contentType(MediaType.APPLICATION_JSON)
              .header(MockWebhookSignatureValidator.HEADER_MOCK_SIGNATURE, MockWebhookSignatureValidator.TEST_MOCK_VALID_SIGNATURE)
              .content(payload))
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.code").value("INVALID_WEBHOOK_SIGNATURE"));
    } finally {
      properties.setAllowTestWebhookSignatures(true);
    }
  }

  @Test
  @DisplayName("17. Duplicate Delivery Idempotency: Duplicate webhook returns DUPLICATE_IGNORED safely")
  void testDuplicateDeliveryIdempotency_ignoresDuplicateSafely() throws Exception {
    String msgId = MSG_PREFIX + "DUP-17";
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
  @DisplayName("18. DEMO/SIMULATED Record Protection: SIMULATED log cannot become DELIVERED via webhook")
  void testDemoSimulatedRecordProtection_cannotBecomeDeliveredViaWebhook() throws Exception {
    String msgId = MSG_PREFIX + "SIM-18";
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
    assertThat(currentStatusId).isEqualTo(9L);

    String deliveredAt = jdbcTemplate.queryForObject(
        "SELECT delivered_at FROM communication_logs WHERE communication_log_id = ?",
        String.class,
        logId
    );
    assertThat(deliveredAt).isNull();
  }

  @Test
  @DisplayName("19. Statutory Consent Protection: BLOCKED_NO_CONSENT record is immutable to webhooks")
  void testBlockedConsentRecordProtection_cannotBeModifiedViaWebhook() throws Exception {
    String msgId = MSG_PREFIX + "NOCONSENT-19";
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
    assertThat(currentStatusId).isEqualTo(8L);
  }

  @Test
  @DisplayName("20. Non-existent Message ID -> Returns 404 with MESSAGE_NOT_FOUND")
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
  @DisplayName("21. Unsupported Provider -> Returns 404 with UNSUPPORTED_PROVIDER")
  void testUnsupportedProvider_returns404NotFound() throws Exception {
    mockMvc.perform(post("/api/v1/communication/webhooks/unsupported-carrier")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("UNSUPPORTED_PROVIDER"));
  }
}
