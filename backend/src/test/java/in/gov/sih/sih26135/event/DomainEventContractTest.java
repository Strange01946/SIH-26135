package in.gov.sih.sih26135.event;

import static org.assertj.core.api.Assertions.assertThat;

import in.gov.sih.sih26135.config.AsyncConfig;
import in.gov.sih.sih26135.correlation.CorrelationContextHolder;
import in.gov.sih.sih26135.correlation.CorrelationIdFilter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

@DisplayName("Phase 29.1 — Domain Event Contract & Async Boundary Verification Test")
class DomainEventContractTest {

  private static final String SAMPLE_CORRELATION_ID = "CORR-291-TEST-0001";
  private static final String SAMPLE_REQUEST_ID = "REQ-291-TEST-0001";

  @BeforeEach
  void setUp() {
    CorrelationContextHolder.setCorrelationId(SAMPLE_CORRELATION_ID);
    CorrelationContextHolder.setRequestId(SAMPLE_REQUEST_ID);
    MDC.put(CorrelationIdFilter.MDC_CORRELATION_ID_KEY, SAMPLE_CORRELATION_ID);
    MDC.put(CorrelationIdFilter.MDC_REQUEST_ID_KEY, SAMPLE_REQUEST_ID);
  }

  @AfterEach
  void tearDown() {
    CorrelationContextHolder.clear();
    MDC.clear();
  }

  @Nested
  @DisplayName("1. Strongly Typed Domain Event Contract Conformance")
  class DomainEventContractConformanceTests {

    @Test
    @DisplayName("AuditTrailEvent conforms to DomainEvent interface with backward compatibility")
    void testAuditTrailEventConformsToDomainEvent() {
      AuditTrailEvent event = new AuditTrailEvent(
          "STATUS_CHANGE",
          "Placement",
          501L,
          99L,
          "Placement transitioned to employment",
          LocalDateTime.now(),
          SAMPLE_CORRELATION_ID
      );

      assertThat(event).isInstanceOf(DomainEvent.class);
      assertThat(event.getEventId()).isNotBlank();
      assertThat(event.getEventType()).isEqualTo("STATUS_CHANGE");
      assertThat(event.getEntityType()).isEqualTo("Placement");
      assertThat(event.getEntityId()).isEqualTo(501L);
      assertThat(event.getActorUserId()).isEqualTo(99L);
      assertThat(event.getCorrelationId()).isEqualTo(SAMPLE_CORRELATION_ID);
      assertThat(event.getOccurredAt()).isNotNull();

      // Verify original record getters remain 100% backward compatible
      assertThat(event.actionCode()).isEqualTo("STATUS_CHANGE");
      assertThat(event.entityType()).isEqualTo("Placement");
      assertThat(event.entityId()).isEqualTo(501L);
      assertThat(event.actorUserId()).isEqualTo(99L);
      assertThat(event.changeSummary()).isEqualTo("Placement transitioned to employment");
    }

    @Test
    @DisplayName("AuditTrailEvent eventId is stable per instance and unique across events sharing correlationId")
    void testAuditTrailEventEventIdentityStabilityAndUniqueness() {
      // 1. Repeated calls on the same event return the same ID
      AuditTrailEvent event1 = new AuditTrailEvent(
          "STATUS_CHANGE",
          "Placement",
          501L,
          99L,
          "Placement transitioned to employment",
          LocalDateTime.now(),
          SAMPLE_CORRELATION_ID
      );
      String id1a = event1.getEventId();
      String id1b = event1.getEventId();
      assertThat(id1a).isNotBlank();
      assertThat(id1a).isEqualTo(id1b);

      // 2. Different events with the same correlation ID receive distinct event IDs
      AuditTrailEvent event2 = new AuditTrailEvent(
          "STATUS_CHANGE",
          "Placement",
          502L,
          99L,
          "Second placement updated in same request",
          LocalDateTime.now(),
          SAMPLE_CORRELATION_ID
      );
      String id2 = event2.getEventId();
      assertThat(id2).isNotBlank();
      assertThat(id2).isNotEqualTo(id1a);

      // 3. Events without correlation IDs still have stable, unique IDs
      AuditTrailEvent eventNoCorr1 = new AuditTrailEvent(
          "CREATE",
          "Trainee",
          601L,
          99L,
          "Trainee created without correlation",
          LocalDateTime.now(),
          null
      );
      String noCorrId1a = eventNoCorr1.getEventId();
      String noCorrId1b = eventNoCorr1.getEventId();
      assertThat(noCorrId1a).isNotBlank();
      assertThat(noCorrId1a).isEqualTo(noCorrId1b);

      AuditTrailEvent eventNoCorr2 = new AuditTrailEvent(
          "CREATE",
          "Trainee",
          602L,
          99L,
          "Second trainee created without correlation",
          LocalDateTime.now(),
          null
      );
      String noCorrId2 = eventNoCorr2.getEventId();
      assertThat(noCorrId2).isNotBlank();
      assertThat(noCorrId2).isNotEqualTo(noCorrId1a);
    }

    @Test
    @DisplayName("TraineeLifecycleEvent captures immutable trajectory and auto-generates event context")
    void testTraineeLifecycleEventContract() {
      TraineeLifecycleEvent event = TraineeLifecycleEvent.of(
          "ENROLLED",
          1001L,
          88L,
          10L,
          20L,
          30L,
          "REGISTERED",
          "ENROLLED"
      );

      assertThat(event).isInstanceOf(DomainEvent.class);
      assertThat(event.getEventId()).isNotBlank();
      assertThat(event.getEventType()).isEqualTo("ENROLLED");
      assertThat(event.getEntityType()).isEqualTo("Trainee");
      assertThat(event.getEntityId()).isEqualTo(1001L);
      assertThat(event.getActorUserId()).isEqualTo(88L);
      assertThat(event.getCorrelationId()).isEqualTo(SAMPLE_CORRELATION_ID);
      assertThat(event.getOccurredAt()).isNotNull();
      assertThat(event.stateId()).isEqualTo(10L);
      assertThat(event.trainingProviderId()).isEqualTo(20L);
      assertThat(event.batchId()).isEqualTo(30L);
      assertThat(event.previousStatus()).isEqualTo("REGISTERED");
      assertThat(event.newStatus()).isEqualTo("ENROLLED");
    }

    @Test
    @DisplayName("PlacementLifecycleEvent transports placement parameters with salary and tenant scope")
    void testPlacementLifecycleEventContract() {
      BigDecimal offeredSalary = new BigDecimal("25000.00");
      PlacementLifecycleEvent event = PlacementLifecycleEvent.of(
          "ACCEPTED",
          2002L,
          1001L,
          88L,
          20L,
          300L,
          "ACCEPTED",
          "JOINED",
          offeredSalary
      );

      assertThat(event).isInstanceOf(DomainEvent.class);
      assertThat(event.getEventId()).isNotBlank();
      assertThat(event.getEventType()).isEqualTo("ACCEPTED");
      assertThat(event.getEntityType()).isEqualTo("Placement");
      assertThat(event.getEntityId()).isEqualTo(2002L);
      assertThat(event.traineeId()).isEqualTo(1001L);
      assertThat(event.trainingProviderId()).isEqualTo(20L);
      assertThat(event.employerId()).isEqualTo(300L);
      assertThat(event.offeredSalary()).isEqualByComparingTo("25000.00");
      assertThat(event.getCorrelationId()).isEqualTo(SAMPLE_CORRELATION_ID);
    }

    @Test
    @DisplayName("EmploymentVerificationEvent transports verification method, status and retention milestone")
    void testEmploymentVerificationEventContract() {
      BigDecimal verifiedSalary = new BigDecimal("27500.00");
      EmploymentVerificationEvent event = EmploymentVerificationEvent.of(
          "VERIFIED",
          4004L,
          3003L,
          1001L,
          88L,
          300L,
          "EMPLOYER_PORTAL",
          "VERIFIED",
          verifiedSalary,
          6
      );

      assertThat(event).isInstanceOf(DomainEvent.class);
      assertThat(event.getEventId()).isNotBlank();
      assertThat(event.getEventType()).isEqualTo("VERIFIED");
      assertThat(event.getEntityType()).isEqualTo("EmploymentVerification");
      assertThat(event.getEntityId()).isEqualTo(4004L);
      assertThat(event.employmentRecordId()).isEqualTo(3003L);
      assertThat(event.traineeId()).isEqualTo(1001L);
      assertThat(event.employerId()).isEqualTo(300L);
      assertThat(event.verificationMethod()).isEqualTo("EMPLOYER_PORTAL");
      assertThat(event.retentionMonthPeriod()).isEqualTo(6);
      assertThat(event.verifiedSalary()).isEqualByComparingTo("27500.00");
    }

    @Test
    @DisplayName("DataQualityAlertEvent transports rule violation details without sensitive leakage")
    void testDataQualityAlertEventContract() {
      DataQualityAlertEvent event = DataQualityAlertEvent.of(
          "ANOMALY_DETECTED",
          7007L,
          null, // system-generated
          "DQ-PLACEMENT-002",
          "HIGH",
          "Placement",
          2002L,
          20L,
          "OPEN",
          "Temporal incoherence detected: placement date precedes enrollment date"
      );

      assertThat(event).isInstanceOf(DomainEvent.class);
      assertThat(event.getEventId()).isNotBlank();
      assertThat(event.getEventType()).isEqualTo("ANOMALY_DETECTED");
      assertThat(event.getEntityType()).isEqualTo("DataQualityIssue");
      assertThat(event.getEntityId()).isEqualTo(7007L);
      assertThat(event.getActorUserId()).isNull();
      assertThat(event.ruleCode()).isEqualTo("DQ-PLACEMENT-002");
      assertThat(event.severity()).isEqualTo("HIGH");
      assertThat(event.targetEntityType()).isEqualTo("Placement");
      assertThat(event.targetEntityId()).isEqualTo(2002L);
      assertThat(event.trainingProviderId()).isEqualTo(20L);
      assertThat(event.summary()).contains("Temporal incoherence detected");
    }

    @Test
    @DisplayName("CommunicationTriggerEvent masks recipient contact information in audit payloads")
    void testCommunicationTriggerEventContract() {
      CommunicationTriggerEvent event = CommunicationTriggerEvent.of(
          "VERIFICATION_REQUEST",
          300L,
          88L,
          "EMAIL",
          "h***r@employer.com",
          "TPL-VERIF-003",
          20L,
          "HIGH",
          "Employment verification dispatched for trainee candidate"
      );

      assertThat(event).isInstanceOf(DomainEvent.class);
      assertThat(event.getEventId()).isNotBlank();
      assertThat(event.getEventType()).isEqualTo("VERIFICATION_REQUEST");
      assertThat(event.getEntityType()).isEqualTo("CommunicationTrigger");
      assertThat(event.channel()).isEqualTo("EMAIL");
      assertThat(event.recipientMasked()).isEqualTo("h***r@employer.com");
      assertThat(event.templateCode()).isEqualTo("TPL-VERIF-003");
      assertThat(event.priority()).isEqualTo("HIGH");
      assertThat(event.recipientMasked()).doesNotContain("plaintext_secret");
    }

    @Test
    @DisplayName("CommunicationTriggerEvent enforces PII masking on raw email and phone inputs and in toString()")
    void testCommunicationTriggerEventPIIProtectionWithUnmaskedInputs() {
      // 1. Unmasked email address
      CommunicationTriggerEvent emailEvent = CommunicationTriggerEvent.of(
          "VERIFICATION_REQUEST",
          301L,
          88L,
          "EMAIL",
          "aditya.sharma@example.com",
          "TPL-01",
          20L,
          "NORMAL",
          "Verification dispatched"
      );
      assertThat(emailEvent.recipientMasked()).isEqualTo("a***a@example.com");
      assertThat(emailEvent.recipientMasked()).doesNotContain("aditya.sharma");
      assertThat(emailEvent.toString()).doesNotContain("aditya.sharma@example.com");
      assertThat(emailEvent.toString()).contains("a***a@example.com");

      // 2. Unmasked 10-digit mobile phone
      CommunicationTriggerEvent phoneEvent = CommunicationTriggerEvent.of(
          "TRAINEE_ALERT",
          302L,
          88L,
          "SMS",
          "9876543210",
          "TPL-02",
          20L,
          "HIGH",
          "SMS reminder"
      );
      assertThat(phoneEvent.recipientMasked()).isEqualTo("XXXXXX3210");
      assertThat(phoneEvent.recipientMasked()).doesNotContain("987654");
      assertThat(phoneEvent.toString()).doesNotContain("9876543210");
      assertThat(phoneEvent.toString()).contains("XXXXXX3210");

      // 3. Unmasked international phone with country code and formatting
      CommunicationTriggerEvent intlPhoneEvent = CommunicationTriggerEvent.of(
          "TRAINEE_ALERT",
          303L,
          88L,
          "SMS",
          "+91-9876543210",
          "TPL-03",
          20L,
          "NORMAL",
          "SMS reminder"
      );
      assertThat(intlPhoneEvent.recipientMasked()).isEqualTo("+XX-XXXXXX3210");
      assertThat(intlPhoneEvent.recipientMasked()).doesNotContain("987654");
      assertThat(intlPhoneEvent.toString()).doesNotContain("9876543210");
      assertThat(intlPhoneEvent.toString()).contains("+XX-XXXXXX3210");

      // 4. Pre-masked inputs remain idempotent
      CommunicationTriggerEvent preMaskedEmail = CommunicationTriggerEvent.of(
          "VERIFICATION_REQUEST",
          304L,
          88L,
          "EMAIL",
          "h***r@employer.com",
          "TPL-04",
          20L,
          "NORMAL",
          "Pre-masked email"
      );
      assertThat(preMaskedEmail.recipientMasked()).isEqualTo("h***r@employer.com");

      CommunicationTriggerEvent preMaskedPhone = CommunicationTriggerEvent.of(
          "TRAINEE_ALERT",
          305L,
          88L,
          "SMS",
          "+91-XXXXX-XX123",
          "TPL-05",
          20L,
          "NORMAL",
          "Pre-masked phone"
      );
      assertThat(preMaskedPhone.recipientMasked()).isEqualTo("+91-XXXXX-XX123");

      // 5. Direct constructor invocation enforces masking
      CommunicationTriggerEvent directEvent = new CommunicationTriggerEvent(
          null,
          null,
          306L,
          88L,
          null,
          null,
          "EMAIL",
          "officer.contact@domain.gov.in",
          "TPL-06",
          null,
          null,
          null,
          "Direct instantiation payload"
      );
      assertThat(directEvent.recipientMasked()).isEqualTo("o***t@domain.gov.in");
      assertThat(directEvent.toString()).doesNotContain("officer.contact@domain.gov.in");
      assertThat(directEvent.toString()).contains("o***t@domain.gov.in");
    }

    @Test
    @DisplayName("CommunicationTriggerEvent prevents recipient masking bypass with X, x, or special characters")
    void testCommunicationTriggerEventRecipientMaskingBypassRegressions() {
      // 1. Local part starting with uppercase X (e.g. Xavier)
      CommunicationTriggerEvent xavierEvent = CommunicationTriggerEvent.of(
          "NOTIFY", 310L, 88L, "EMAIL", "Xavier@example.com", "TPL-07", 20L, "NORMAL", "Notice"
      );
      assertThat(xavierEvent.recipientMasked()).isEqualTo("X***r@example.com");
      assertThat(xavierEvent.recipientMasked()).doesNotContain("Xavier");
      assertThat(xavierEvent.toString()).doesNotContain("Xavier@example.com");

      // 2. Local part with lowercase x and internal x (e.g. xavier.kumar, alex.maxwell)
      CommunicationTriggerEvent alexEvent = CommunicationTriggerEvent.of(
          "NOTIFY", 311L, 88L, "EMAIL", "alex.maxwell@example.com", "TPL-08", 20L, "NORMAL", "Notice"
      );
      assertThat(alexEvent.recipientMasked()).isEqualTo("a***l@example.com");
      assertThat(alexEvent.recipientMasked()).doesNotContain("alex.maxwell");

      // 3. Local part with multiple x's (e.g. maxxxx)
      CommunicationTriggerEvent maxxxEvent = CommunicationTriggerEvent.of(
          "NOTIFY", 312L, 88L, "EMAIL", "maxxxx@example.com", "TPL-09", 20L, "NORMAL", "Notice"
      );
      assertThat(maxxxEvent.recipientMasked()).isEqualTo("m***x@example.com");
      assertThat(maxxxEvent.recipientMasked()).doesNotContain("maxxxx");

      // 4. Single-letter X email
      CommunicationTriggerEvent singleXEvent = CommunicationTriggerEvent.of(
          "NOTIFY", 313L, 88L, "EMAIL", "X@example.com", "TPL-10", 20L, "NORMAL", "Notice"
      );
      assertThat(singleXEvent.recipientMasked()).isEqualTo("X***@example.com");

      // 5. Already masked phone and email formats remain strictly idempotent
      assertThat(CommunicationTriggerEvent.maskRecipient("XXXXXX3210")).isEqualTo("XXXXXX3210");
      assertThat(CommunicationTriggerEvent.maskRecipient("+XX-XXXXXX3210")).isEqualTo("+XX-XXXXXX3210");
      assertThat(CommunicationTriggerEvent.maskRecipient("+91-XXXXX-XX123")).isEqualTo("+91-XXXXX-XX123");
      assertThat(CommunicationTriggerEvent.maskRecipient("(XXX) XXX-3210")).isEqualTo("(XXX) XXX-3210");
      assertThat(CommunicationTriggerEvent.maskRecipient("XXX.XXX.3210")).isEqualTo("XXX.XXX.3210");
      assertThat(CommunicationTriggerEvent.maskRecipient("h***r@employer.com")).isEqualTo("h***r@employer.com");
      assertThat(CommunicationTriggerEvent.maskRecipient("a***@example.com")).isEqualTo("a***@example.com");
      assertThat(CommunicationTriggerEvent.maskRecipient("[REDACTED]")).isEqualTo("[REDACTED]");

      // 6. Partially masked phone numbers are sanitized to protect sensitive digits
      assertThat(CommunicationTriggerEvent.maskRecipient("XXX9876543210")).isEqualTo("XXXXXXXXX3210");
      assertThat(CommunicationTriggerEvent.maskRecipient("9876543210XXX")).isEqualTo("XXXXXX3210XXX");

      // 7. Formatted recipient phone numbers are masked properly
      assertThat(CommunicationTriggerEvent.maskRecipient("(987) 654-3210")).isEqualTo("(XXX) XXX-3210");
      assertThat(CommunicationTriggerEvent.maskRecipient("987.654.3210")).isEqualTo("XXX.XXX.3210");
      assertThat(CommunicationTriggerEvent.maskRecipient("+91 (98765) 43210")).isEqualTo("+XX (XXXXX) X3210");

      // 8. Adjacent phone recipient formats are sanitized
      assertThat(CommunicationTriggerEvent.maskRecipient("9876543210/9123456780")).isEqualTo("XXXXXX3210/XXXXXX6780");
      assertThat(CommunicationTriggerEvent.maskRecipient("9876543210 - 9123456780")).isEqualTo("XXXXXX3210 - XXXXXX6780");
    }

    @Test
    @DisplayName("CommunicationTriggerEvent sanitizes embedded emails, formatted and partially masked phones in payloadSummary")
    void testCommunicationTriggerEventPayloadSummaryContactProtection() {
      // 1. Raw emails embedded in payloadSummary
      String emailSummary = "Verification reminder dispatched to Xavier@example.com and candidate.alert@domain.gov.in regarding cohort";
      CommunicationTriggerEvent emailEvent = CommunicationTriggerEvent.of(
          "VERIFICATION_REQUEST", 401L, 88L, "EMAIL", "officer@example.com", "TPL-01", 20L, "NORMAL", emailSummary
      );
      assertThat(emailEvent.payloadSummary()).doesNotContain("Xavier@example.com");
      assertThat(emailEvent.payloadSummary()).doesNotContain("candidate.alert@domain.gov.in");
      assertThat(emailEvent.payloadSummary()).contains("X***r@example.com");
      assertThat(emailEvent.payloadSummary()).contains("c***t@domain.gov.in");
      assertThat(emailEvent.toString()).doesNotContain("Xavier@example.com");
      assertThat(emailEvent.toString()).doesNotContain("candidate.alert@domain.gov.in");

      // 2. Formatted phone numbers embedded in payloadSummary: (987) 654-3210, 987.654.3210, +91 (98765) 43210
      String formattedPhoneSummary = "Dispatched notifications to (987) 654-3210, dot-formatted 987.654.3210, and international +91 (98765) 43210 successfully";
      CommunicationTriggerEvent formattedPhoneEvent = CommunicationTriggerEvent.of(
          "TRAINEE_ALERT", 402L, 88L, "SMS", "9876543210", "TPL-02", 20L, "HIGH", formattedPhoneSummary
      );
      assertThat(formattedPhoneEvent.payloadSummary()).doesNotContain("(987) 654-3210");
      assertThat(formattedPhoneEvent.payloadSummary()).doesNotContain("987.654.3210");
      assertThat(formattedPhoneEvent.payloadSummary()).doesNotContain("+91 (98765) 43210");
      assertThat(formattedPhoneEvent.payloadSummary()).contains("(XXX) XXX-3210");
      assertThat(formattedPhoneEvent.payloadSummary()).contains("XXX.XXX.3210");
      assertThat(formattedPhoneEvent.payloadSummary()).contains("+XX (XXXXX) X3210");
      assertThat(formattedPhoneEvent.toString()).doesNotContain("(987) 654-3210");
      assertThat(formattedPhoneEvent.toString()).doesNotContain("987.654.3210");
      assertThat(formattedPhoneEvent.toString()).doesNotContain("+91 (98765) 43210");

      // 3. Partially masked phone numbers embedded in payloadSummary (e.g. XXX9876543210)
      String partiallyMaskedSummary = "Alert queued for partially masked contact XXX9876543210 regarding status check";
      CommunicationTriggerEvent partialMaskEvent = CommunicationTriggerEvent.of(
          "TRAINEE_ALERT", 403L, 88L, "SMS", "XXX9876543210", "TPL-03", 20L, "HIGH", partiallyMaskedSummary
      );
      assertThat(partialMaskEvent.recipientMasked()).isEqualTo("XXXXXXXXX3210");
      assertThat(partialMaskEvent.recipientMasked()).doesNotContain("987654");
      assertThat(partialMaskEvent.payloadSummary()).doesNotContain("9876543210");
      assertThat(partialMaskEvent.payloadSummary()).contains("XXXXXXXXX3210");
      assertThat(partialMaskEvent.toString()).doesNotContain("9876543210");

      // 4. Mixed emails, phone numbers, and pre-masked tokens in payloadSummary
      String mixedSummary = "Sent email to john.doe@example.com (pre-masked: h***r@employer.com) and phone 9876543210 (pre-masked: +91-XXXXX-XX123)";
      CommunicationTriggerEvent mixedEvent = CommunicationTriggerEvent.of(
          "DISPATCH", 404L, 88L, "EMAIL", "j***e@example.com", "TPL-04", 20L, "NORMAL", mixedSummary
      );
      assertThat(mixedEvent.payloadSummary()).doesNotContain("john.doe@example.com");
      assertThat(mixedEvent.payloadSummary()).doesNotContain("9876543210");
      assertThat(mixedEvent.payloadSummary()).contains("j***e@example.com");
      assertThat(mixedEvent.payloadSummary()).contains("h***r@employer.com");
      assertThat(mixedEvent.payloadSummary()).contains("XXXXXX3210");
      assertThat(mixedEvent.payloadSummary()).contains("+91-XXXXX-XX123");
      assertThat(mixedEvent.toString()).doesNotContain("john.doe@example.com");
      assertThat(mixedEvent.toString()).doesNotContain("9876543210");

      // 5. Unrelated numeric values are preserved without corruption
      String nonContactSummary = "Batch 1001 scheduled on 2026-11-15 at server 192.168.1.100 with total stipend 12345.67 for entity 501";
      CommunicationTriggerEvent nonContactEvent = CommunicationTriggerEvent.of(
          "DISPATCH", 405L, 88L, "EMAIL", "officer@example.com", "TPL-05", 20L, "NORMAL", nonContactSummary
      );
      assertThat(nonContactEvent.payloadSummary()).contains("1001");
      assertThat(nonContactEvent.payloadSummary()).contains("2026-11-15");
      assertThat(nonContactEvent.payloadSummary()).contains("192.168.1.100");
      assertThat(nonContactEvent.payloadSummary()).contains("12345.67");
      assertThat(nonContactEvent.payloadSummary()).contains("501");

      // 6. Adjacent phone numbers separated by slash (e.g. Contact 9876543210/9123456780)
      String adjacentSlashSummary = "Contact 9876543210/9123456780 for verification support";
      CommunicationTriggerEvent adjacentSlashEvent = CommunicationTriggerEvent.of(
          "DISPATCH", 406L, 88L, "EMAIL", "officer@example.com", "TPL-06", 20L, "NORMAL", adjacentSlashSummary
      );
      assertThat(adjacentSlashEvent.payloadSummary()).doesNotContain("9876543210");
      assertThat(adjacentSlashEvent.payloadSummary()).doesNotContain("9123456780");
      assertThat(adjacentSlashEvent.payloadSummary()).contains("XXXXXX3210/XXXXXX6780");
      assertThat(adjacentSlashEvent.toString()).doesNotContain("9876543210");
      assertThat(adjacentSlashEvent.toString()).doesNotContain("9123456780");
      assertThat(adjacentSlashEvent.toString()).contains("XXXXXX3210/XXXXXX6780");

      // 7. Adjacent phone numbers separated by hyphen-space (e.g. Phone 9876543210 - 9123456780)
      String adjacentHyphenSummary = "Phone 9876543210 - 9123456780 alternate lines";
      CommunicationTriggerEvent adjacentHyphenEvent = CommunicationTriggerEvent.of(
          "DISPATCH", 407L, 88L, "SMS", "9876543210", "TPL-07", 20L, "NORMAL", adjacentHyphenSummary
      );
      assertThat(adjacentHyphenEvent.payloadSummary()).doesNotContain("9876543210");
      assertThat(adjacentHyphenEvent.payloadSummary()).doesNotContain("9123456780");
      assertThat(adjacentHyphenEvent.payloadSummary()).contains("XXXXXX3210 - XXXXXX6780");
      assertThat(adjacentHyphenEvent.toString()).doesNotContain("9876543210");
      assertThat(adjacentHyphenEvent.toString()).doesNotContain("9123456780");
      assertThat(adjacentHyphenEvent.toString()).contains("XXXXXX3210 - XXXXXX6780");

      // 8. Date adjacent to phone number (e.g. 2026-11-15 9876543210)
      String dateAdjacentSummary = "Batch scheduled on 2026-11-15 9876543210 for trainee alert";
      CommunicationTriggerEvent dateAdjacentEvent = CommunicationTriggerEvent.of(
          "DISPATCH", 408L, 88L, "SMS", "9876543210", "TPL-08", 20L, "NORMAL", dateAdjacentSummary
      );
      assertThat(dateAdjacentEvent.payloadSummary()).doesNotContain("9876543210");
      assertThat(dateAdjacentEvent.payloadSummary()).contains("2026-11-15 XXXXXX3210");
      assertThat(dateAdjacentEvent.payloadSummary()).contains("2026-11-15");
      assertThat(dateAdjacentEvent.toString()).doesNotContain("9876543210");
      assertThat(dateAdjacentEvent.toString()).contains("2026-11-15 XXXXXX3210");
      assertThat(dateAdjacentEvent.toString()).contains("2026-11-15");
    }
  }

  @Nested
  @DisplayName("2. Asynchronous Context Propagation & Boundary Isolation")
  class AsyncBoundaryIsolationTests {

    @Test
    @DisplayName("CorrelationTaskDecorator propagates correlation ID and distinct async request ID across thread boundary")
    void testCorrelationTaskDecoratorPropagation() throws InterruptedException {
      AsyncConfig asyncConfig = new AsyncConfig();
      TaskDecorator decorator = asyncConfig.correlationTaskDecorator();

      AtomicReference<String> workerCorrelationId = new AtomicReference<>();
      AtomicReference<String> workerRequestId = new AtomicReference<>();
      AtomicReference<String> workerMdcCorrelationId = new AtomicReference<>();
      AtomicReference<String> workerMdcRequestId = new AtomicReference<>();

      CountDownLatch latch = new CountDownLatch(1);

      Runnable decoratedTask = decorator.decorate(() -> {
        try {
          workerCorrelationId.set(CorrelationContextHolder.getCorrelationId());
          workerRequestId.set(CorrelationContextHolder.getExplicitRequestId());
          workerMdcCorrelationId.set(MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY));
          workerMdcRequestId.set(MDC.get(CorrelationIdFilter.MDC_REQUEST_ID_KEY));
        } finally {
          latch.countDown();
        }
      });

      Thread workerThread = new Thread(decoratedTask, "test-async-worker");
      workerThread.start();

      boolean completed = latch.await(5, TimeUnit.SECONDS);
      assertThat(completed).isTrue();

      // 1. Parent correlation ID must be preserved exactly
      assertThat(workerCorrelationId.get()).isEqualTo(SAMPLE_CORRELATION_ID);
      assertThat(workerMdcCorrelationId.get()).isEqualTo(SAMPLE_CORRELATION_ID);

      // 2. Worker thread must receive a distinct non-null async request ID
      assertThat(workerRequestId.get()).isNotNull();
      assertThat(workerRequestId.get()).isNotEqualTo(SAMPLE_REQUEST_ID);
      assertThat(workerMdcRequestId.get()).isEqualTo(workerRequestId.get());

      // 3. Parent calling thread context must remain untouched
      assertThat(CorrelationContextHolder.getCorrelationId()).isEqualTo(SAMPLE_CORRELATION_ID);
      assertThat(CorrelationContextHolder.getExplicitRequestId()).isEqualTo(SAMPLE_REQUEST_ID);
    }

    @Test
    @DisplayName("ObservableAuditTaskRejectionHandler counts saturation and protects calling thread")
    void testObservableAuditTaskRejectionHandlerUnderSaturation() {
      AsyncConfig.ObservableAuditTaskRejectionHandler handler = new AsyncConfig.ObservableAuditTaskRejectionHandler();
      handler.reset();

      ThreadPoolExecutor executor = new ThreadPoolExecutor(
          1, 1, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1)
      );

      Runnable dummyTask = () -> {};

      // Simulate rejection under saturation
      handler.rejectedExecution(dummyTask, executor);
      handler.rejectedExecution(dummyTask, executor);

      assertThat(handler.getRejectedTaskCount()).isEqualTo(2L);

      executor.shutdownNow();
    }
  }

  @Nested
  @DisplayName("3. Polymorphic Event Dispatch & Processing Safety")
  class PolymorphicEventDispatchTests {

    @Test
    @DisplayName("Polymorphic listener can accept heterogeneous DomainEvent instances safely")
    void testHeterogeneousEventDispatch() {
      List<DomainEvent> capturedEvents = new ArrayList<>();

      DomainEvent e1 = TraineeLifecycleEvent.of("ENROLLED", 1L, 10L, 100L, 200L, 300L, "PENDING", "ACTIVE");
      DomainEvent e2 = PlacementLifecycleEvent.of("OFFERED", 2L, 1L, 10L, 200L, 500L, "OFFERED", null, new BigDecimal("30000.00"));
      DomainEvent e3 = EmploymentVerificationEvent.of("VERIFIED", 3L, 20L, 1L, 10L, 500L, "PORTAL", "VERIFIED", new BigDecimal("30000.00"), 3);
      DomainEvent e4 = DataQualityAlertEvent.of("ANOMALY", 4L, null, "DQ-001", "WARN", "Trainee", 1L, 200L, "OPEN", "Anomaly flagged");
      DomainEvent e5 = CommunicationTriggerEvent.of("DISPATCH", 1L, 10L, "SMS", "+91-XXXXX-XX123", "TPL-01", 200L, "NORMAL", "Reminder");
      DomainEvent e6 = new AuditTrailEvent("MUTATE", "Course", 50L, 10L, "Course created", LocalDateTime.now(), SAMPLE_CORRELATION_ID);

      capturedEvents.add(e1);
      capturedEvents.add(e2);
      capturedEvents.add(e3);
      capturedEvents.add(e4);
      capturedEvents.add(e5);
      capturedEvents.add(e6);

      assertThat(capturedEvents).hasSize(6);
      assertThat(capturedEvents).allSatisfy(event -> {
        assertThat(event.getEventId()).isNotBlank();
        assertThat(event.getEventType()).isNotBlank();
        assertThat(event.getEntityType()).isNotBlank();
        assertThat(event.getEntityId()).isNotNull();
        assertThat(event.getOccurredAt()).isNotNull();
      });
    }
  }
}
