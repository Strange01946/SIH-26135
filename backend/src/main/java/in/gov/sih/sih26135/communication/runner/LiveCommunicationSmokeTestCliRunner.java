package in.gov.sih.sih26135.communication.runner;

import in.gov.sih.sih26135.communication.config.CommunicationProperties;
import in.gov.sih.sih26135.communication.dto.LiveSmokeTestRequest;
import in.gov.sih.sih26135.communication.dto.LiveSmokeTestResult;
import in.gov.sih.sih26135.communication.model.CommunicationChannel;
import in.gov.sih.sih26135.communication.service.NotificationDispatcherService;
import in.gov.sih.sih26135.event.CommunicationTriggerEvent;
import in.gov.sih.sih26135.security.InternalSecurityExecutionSupport;
import in.gov.sih.sih26135.security.InternalServicePrincipal;
import in.gov.sih.sih26135.security.TenantScopeType;
import java.sql.Connection;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/**
 * Opt-in Spring Boot Command Line Runner for executing live multi-channel smoke tests.
 *
 * <p>Activates when {@code --communication.live-smoke-test.enabled=true} (or legacy {@code --communication.smoke-test.enabled=true})
 * is supplied to Spring Boot.
 *
 * <p>Enforces:
 * <ul>
 *   <li>Strict database catalog guard asserting {@code SIH26135_TEST}.</li>
 *   <li>Explicit target trainee ID: never silently falls back to a default trainee.</li>
 *   <li>Mode guard: strictly rejects execution in {@code MOCK} mode; live delivery requires {@code --communication.mode=LIVE}.</li>
 *   <li>Fail-closed error contract: any delivery rejection or consent block causes process failure with non-zero exit.</li>
 *   <li>Authorized internal execution identityDerivation via {@link InternalSecurityExecutionSupport}.</li>
 *   <li>Fail-closed recipient override validation: requires channel-specific override when enabled.</li>
 * </ul>
 */
@Component
@ConditionalOnExpression("${communication.live-smoke-test.enabled:false} || ${communication.smoke-test.enabled:false}")
public class LiveCommunicationSmokeTestCliRunner implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(LiveCommunicationSmokeTestCliRunner.class);

  private final NotificationDispatcherService notificationDispatcherService;
  private final CommunicationProperties properties;
  private final DataSource dataSource;
  private final InternalSecurityExecutionSupport internalSecurityExecutionSupport;

  @Value("${communication.live-smoke-test.channel:${communication.channel:${communication.smoke-test.channel:SMS}}}")
  private String channelName;

  @Value("${communication.live-smoke-test.target-trainee-id:${communication.target-trainee-id:${communication.smoke-test.trainee-id:#{null}}}}")
  private Long targetTraineeId;

  @Value("${communication.live-smoke-test.recipient-override:${communication.live-recipient-override:${communication.smoke-test.recipient-override:}}}")
  private String recipientOverride;

  @Value("${communication.live-smoke-test.message:${communication.message:${communication.smoke-test.message:SIH-26135 Live Smoke Test Verification}}}")
  private String testMessage;

  @Autowired
  public LiveCommunicationSmokeTestCliRunner(
      NotificationDispatcherService notificationDispatcherService,
      CommunicationProperties properties,
      DataSource dataSource,
      InternalSecurityExecutionSupport internalSecurityExecutionSupport) {
    this.notificationDispatcherService = notificationDispatcherService;
    this.properties = properties;
    this.dataSource = dataSource;
    this.internalSecurityExecutionSupport = internalSecurityExecutionSupport;
  }

  @Override
  public void run(String... args) throws Exception {
    log.info("================================================================================");
    log.info("[LiveSmokeTest] Initiating Opt-in Live Communication Smoke Test Runner");
    log.info("================================================================================");

    // 1. Strict Database Catalog Guard: Must be SIH26135_TEST
    try (Connection conn = dataSource.getConnection()) {
      String catalog = conn.getCatalog();
      log.info("[LiveSmokeTest] Database catalog: {}", catalog);
      if (catalog == null || !catalog.equalsIgnoreCase("SIH26135_TEST")) {
        log.error("[LiveSmokeTest] FATAL: Smoke test aborted! Database catalog is '{}'. Must be 'SIH26135_TEST'.", catalog);
        throw new IllegalStateException("Database catalog must be SIH26135_TEST but was: " + catalog);
      }
    }

    // 2. Strict Mode Guard: Must be LIVE
    if (!properties.isLiveMode()) {
      log.error("================================================================================");
      log.error("[LiveSmokeTest] *** FATAL CONFIGURATION ERROR ***");
      log.error("[LiveSmokeTest] Live smoke testing cannot be reported as successful in MOCK mode.");
      log.error("[LiveSmokeTest] Current communication.mode is 'MOCK'.");
      log.error("[LiveSmokeTest] Pass '--communication.mode=LIVE' to enable genuine carrier delivery.");
      log.error("================================================================================");
      throw new IllegalStateException("Live smoke testing requires --communication.mode=LIVE, but active mode is MOCK");
    }

    // 3. Trainee ID Requirement: No silent fallback
    if (targetTraineeId == null) {
      log.error("================================================================================");
      log.error("[LiveSmokeTest] *** FATAL CONFIGURATION ERROR ***");
      log.error("[LiveSmokeTest] Target trainee ID is required for live smoke testing.");
      log.error("[LiveSmokeTest] Specify '--communication.target-trainee-id=<id>' or '--communication.live-smoke-test.target-trainee-id=<id>'.");
      log.error("[LiveSmokeTest] Silent fallback to default trainee ID has been strictly eliminated.");
      log.error("================================================================================");
      throw new IllegalArgumentException("Target trainee ID is required for live smoke test");
    }

    // 4. Resolve Channel
    CommunicationChannel channel;
    try {
      channel = CommunicationChannel.valueOf(channelName.trim().toUpperCase());
    } catch (Exception ex) {
      log.error("[LiveSmokeTest] Unsupported channel: '{}'. Supported channels: SMS, WHATSAPP, EMAIL", channelName);
      throw new IllegalArgumentException("Unsupported channel for live smoke test: " + channelName);
    }

    // 5. Resolve Effective Recipient Override
    String effectiveRecipientOverride = recipientOverride != null && !recipientOverride.isBlank()
        ? recipientOverride.trim()
        : null;

    if (effectiveRecipientOverride == null && properties.isLiveRecipientOverrideEnabled()) {
      effectiveRecipientOverride = switch (channel) {
        case SMS -> properties.getTwilio().getLiveTestPhone();
        case WHATSAPP -> properties.getTwilio().getLiveTestWhatsapp();
        case EMAIL -> {
          String sg = properties.getSendGrid().getLiveTestEmail();
          yield (sg != null && !sg.isBlank()) ? sg : properties.getSmtp().getLiveTestEmail();
        }
        default -> null;
      };
    }

    if (properties.isLiveRecipientOverrideEnabled()) {
      if (effectiveRecipientOverride == null || effectiveRecipientOverride.isBlank()) {
        log.error("================================================================================");
        log.error("[LiveSmokeTest] *** FATAL CONFIGURATION ERROR ***");
        log.error("[LiveSmokeTest] Live recipient override is enabled, but required test recipient for channel {} is missing or blank.", channel);
        log.error("[LiveSmokeTest] Configure SIH_LIVE_TEST_PHONE (SMS), SIH_LIVE_TEST_WHATSAPP (WhatsApp), or SIH_LIVE_TEST_EMAIL (Email).");
        log.error("[LiveSmokeTest] Silent fallback to arbitrary candidate contacts is strictly prevented.");
        log.error("================================================================================");
        throw new IllegalStateException("Live recipient override is enabled, but required recipient for channel " + channel + " is missing or blank");
      }
      log.info("[LiveSmokeTest] Recipient Override:  {} (Live Safety Override Active)",
          CommunicationTriggerEvent.maskRecipient(effectiveRecipientOverride));
    } else {
      log.info("[LiveSmokeTest] Recipient Override:  (Disabled - using trainee registered profile)");
    }

    log.info("[LiveSmokeTest] Provider Mode:       {}", properties.getMode());
    log.info("[LiveSmokeTest] Channel:             {}", channel);
    log.info("[LiveSmokeTest] Target Trainee ID:   {}", targetTraineeId);

    LiveSmokeTestRequest request = new LiveSmokeTestRequest(
        channel,
        targetTraineeId,
        effectiveRecipientOverride,
        testMessage
    );

    // 6. Execute under Authorized Internal Service Execution Identity
    InternalServicePrincipal principal = new InternalServicePrincipal(
        "CLI_SMOKE_TEST",
        "Live Communication Smoke Test CLI Runner",
        null,
        TenantScopeType.NATIONWIDE
    );

    try {
      LiveSmokeTestResult result = internalSecurityExecutionSupport.runAsInternalService(
          principal,
          () -> notificationDispatcherService.executeLiveSmokeTest(request)
      );

      if (!result.deliveryAccepted() || "FAILED".equalsIgnoreCase(result.status())) {
        log.error("================================================================================");
        log.error("[LiveSmokeTest] *** LIVE SMOKE TEST FAILED ***");
        log.error("[LiveSmokeTest] Channel:           {}", result.channel());
        log.error("[LiveSmokeTest] Provider:          {}", result.providerName());
        log.error("[LiveSmokeTest] Mode:              {}", result.providerMode());
        log.error("[LiveSmokeTest] Status:            {}", result.status());
        log.error("[LiveSmokeTest] Delivery Accepted: {}", result.deliveryAccepted());
        log.error("[LiveSmokeTest] Proof Summary:     {}", result.deliveryProofSummary());
        log.error("================================================================================");
        throw new IllegalStateException("Live smoke test delivery failed: status=" + result.status());
      }

      log.info("================================================================================");
      log.info("[LiveSmokeTest] *** LIVE SMOKE TEST SUCCESSFUL ***");
      log.info("[LiveSmokeTest] Delivery Accepted: {}", result.deliveryAccepted());
      log.info("[LiveSmokeTest] Provider:          {}", result.providerName());
      log.info("[LiveSmokeTest] Provider Mode:     {}", result.providerMode());
      log.info("[LiveSmokeTest] Status:            {}", result.status());
      log.info("[LiveSmokeTest] Message SID/ID:    {}", result.providerMessageId());
      log.info("[LiveSmokeTest] Masked Recipient:  {}", result.recipientMasked());
      log.info("[LiveSmokeTest] Comm Log ID:       {}", result.communicationLogId());
      log.info("[LiveSmokeTest] Proof Summary:     {}", result.deliveryProofSummary());
      log.info("================================================================================");
    } catch (Exception ex) {
      log.error("================================================================================");
      log.error("[LiveSmokeTest] *** LIVE SMOKE TEST ENCOUNTERED AN ERROR ***");
      log.error("[LiveSmokeTest] Error: {}", ex.getMessage(), ex);
      log.error("================================================================================");
      throw ex;
    }
  }
}

