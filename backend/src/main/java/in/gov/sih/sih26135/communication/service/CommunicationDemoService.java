package in.gov.sih.sih26135.communication.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.sih.sih26135.communication.config.CommunicationProperties;
import in.gov.sih.sih26135.communication.dto.CommunicationDispatchCommand;
import in.gov.sih.sih26135.communication.model.CommunicationChannel;
import in.gov.sih.sih26135.communication.model.CommunicationPurpose;
import in.gov.sih.sih26135.communication.service.CommunicationCenterQueryService.LogRow;
import in.gov.sih.sih26135.communication.webhook.MockWebhookSignatureValidator;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CommunicationDemoService {
  private static final String SAMPLE_REGISTRATION = "LOCAL-DEMO-COMM-CENTER";
  private static final String SAMPLE_EMAIL = "trainee@sih-demo.example";
  private static final String ALERT_TEMPLATE = "TPL-ALERT-005";

  private final ApplicationContext application;
  private final CommunicationProperties properties;
  private final JdbcTemplate jdbc;
  private final ObjectMapper json;
  private final CommunicationCenterQueryService queries;
  private final NotificationDispatcherService dispatcher;
  private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

  public CommunicationDemoService(ApplicationContext application, CommunicationProperties properties,
      JdbcTemplate jdbc, ObjectMapper json, CommunicationCenterQueryService queries,
      NotificationDispatcherService dispatcher) {
    this.application = application;
    this.properties = properties;
    this.jdbc = jdbc;
    this.json = json;
    this.queries = queries;
    this.dispatcher = dispatcher;
  }

  public boolean isEnabled() {
    var environment = application.getEnvironment();
    return environment.acceptsProfiles(Profiles.of("local-demo"))
        && "127.0.0.1".equals(environment.getProperty("server.address"))
        && properties.isDemoMode()
        && properties.isSyntheticTestSignatureAllowed();
  }

  public LogRow createReceipt(Long actorId) {
    requireDemo(actorId);
    long traineeId = sampleTrainee();
    String correlation = MockWebhookSignatureValidator.DEMO_RECEIPT_PREFIX + UUID.randomUUID();
    int written = jdbc.update("""
        INSERT INTO communication_logs
          (trainee_id, communication_channel_id, communication_direction_id, communication_purpose_id,
           communication_status_id, initiated_by_user_id, provider_message_id, message_template_code, sent_at)
        SELECT ?, ch.communication_channel_id, d.communication_direction_id, p.communication_purpose_id,
               s.communication_status_id, ?, ?, ?, CURRENT_TIMESTAMP
        FROM ref_communication_channel ch, ref_communication_direction d, ref_communication_purpose p,
             ref_communication_status s
        WHERE ch.channel_code = 'EMAIL' AND d.direction_code = 'OUTBOUND' AND p.purpose_code = 'OPERATIONAL'
          AND s.status_code = 'SENT'
        """, traineeId, actorId, correlation, ALERT_TEMPLATE);
    if (written != 1) {
      throw new IllegalStateException("Required communication reference data is missing");
    }
    Long id = jdbc.queryForObject("SELECT communication_log_id FROM communication_logs WHERE provider_message_id = ?",
        Long.class, correlation);
    return queries.log(id);
  }

  public LogRow deliverReceipt(long id, Long actorId) {
    requireDemo(actorId);
    String correlation = jdbc.query("""
        SELECT l.provider_message_id
        FROM communication_logs l JOIN trainees t USING (trainee_id)
        WHERE l.communication_log_id = ? AND l.initiated_by_user_id = ?
          AND t.registration_number = ? AND t.email = ? AND t.user_id IS NULL AND t.deleted_at IS NULL
        """, (rs, row) -> rs.getString(1), id, actorId, SAMPLE_REGISTRATION, SAMPLE_EMAIL).stream()
        .filter(value -> value != null && value.startsWith(MockWebhookSignatureValidator.DEMO_RECEIPT_PREFIX))
        .findFirst().orElseThrow(() -> new ResourceNotFoundException("Demo receipt", "id"));
    LogRow existing = queries.log(id);
    if ("DELIVERED".equals(existing.status())) {
      return existing;
    }
    if (!"SENT".equals(existing.status())) {
      throw new BadRequestException("Only sent demo messages can receive a delivery receipt", "DEMO_RECEIPT_NOT_SENT");
    }
    submitSignedReceipt(correlation);
    LogRow updated = queries.log(id);
    if (!"DELIVERED".equals(updated.status())) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "The webhook did not confirm delivery");
    }
    return updated;
  }

  public ConsentCheck checkWithdrawnConsent(Long actorId) {
    requireDemo(actorId);
    long traineeId = sampleTrainee();
    int written = jdbc.update("""
        INSERT INTO trainee_consents
          (trainee_id, consent_type_id, purpose, consent_status_id, policy_version, granted_at, revoked_at)
        SELECT ?, ct.consent_type_id, 'Local Communication Center consent demonstration', cs.consent_status_id,
               'LOCAL-DEMO', NOW() - INTERVAL 1 MINUTE, NOW()
        FROM ref_consent_type ct, ref_consent_status cs
        WHERE ct.consent_code = 'DATA_PROCESSING' AND cs.status_code = 'REVOKED'
        """, traineeId);
    if (written != 1) {
      throw new IllegalStateException("Required consent reference data is missing");
    }
    var result = dispatcher.dispatchNotification(CommunicationDispatchCommand.of(traineeId, CommunicationChannel.EMAIL,
        CommunicationPurpose.OPERATIONAL, ALERT_TEMPLATE,
        Map.of("alertType", "Consent demonstration", "actionRequired", "Review your communication settings",
            "referenceId", "LOCAL-CONSENT-DEMO"), actorId));
    return new ConsentCheck(result.status(), result.simulated());
  }

  private long sampleTrainee() {
    jdbc.update("""
        INSERT INTO trainees (registration_number, first_name, last_name, gender_id, email, state_id, district_id,
                              current_employment_status_id, profile_status_id)
        SELECT ?, 'Sample', 'Trainee', g.gender_id, ?, d.state_id, d.district_id, e.employment_status_id,
               p.profile_status_id
        FROM districts d, ref_gender g, ref_employment_status e, ref_profile_status p
        WHERE g.gender_code = 'PREFER_NOT_TO_SAY' AND e.status_code = 'UNKNOWN' AND p.status_code = 'DRAFT'
        ORDER BY d.district_id LIMIT 1
        ON DUPLICATE KEY UPDATE trainees.trainee_id = trainees.trainee_id
        """, SAMPLE_REGISTRATION, SAMPLE_EMAIL);
    return jdbc.query("""
        SELECT trainee_id FROM trainees
        WHERE registration_number = ? AND email = ? AND first_name = 'Sample' AND last_name = 'Trainee'
          AND user_id IS NULL AND deleted_at IS NULL
        """, (rs, row) -> rs.getLong(1), SAMPLE_REGISTRATION, SAMPLE_EMAIL).stream().findFirst()
        .orElseThrow(() -> new IllegalStateException("A dedicated sample trainee could not be prepared"));
  }

  private void submitSignedReceipt(String correlation) {
    if (!(application instanceof ServletWebServerApplicationContext server) || server.getWebServer() == null) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Local webhook server is unavailable");
    }
    try {
      byte[] body = json.writeValueAsBytes(Map.of("providerMessageId", correlation, "status", "delivered"));
      String signature = MockWebhookSignatureValidator.computeHmacSha256Hex(body, MockWebhookSignatureValidator.DEFAULT_MOCK_SECRET);
      URI url = URI.create("http://127.0.0.1:" + server.getWebServer().getPort() + server.getServletContext().getContextPath()
          + "/api/v1/communication/webhooks/mock");
      HttpRequest receipt = HttpRequest.newBuilder(url).timeout(Duration.ofSeconds(8))
          .header("Content-Type", "application/json")
          .header(MockWebhookSignatureValidator.HEADER_MOCK_SIGNATURE, signature)
          .POST(HttpRequest.BodyPublishers.ofByteArray(body)).build();
      if (client.send(receipt, HttpResponse.BodyHandlers.discarding()).statusCode() != 200) {
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "The signed demo receipt was rejected");
      }
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Local webhook request was interrupted");
    } catch (ResponseStatusException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Local webhook could not be reached");
    }
  }

  private void requireDemo(Long actorId) {
    if (actorId == null || !isEnabled()) {
      throw new AccessDeniedException("Communication demonstrations require the loopback local demo and test database");
    }
  }

  public record ConsentCheck(String status, boolean simulated) {}
}
