package in.gov.sih.sih26135.event;

import in.gov.sih.sih26135.entity.AuditLog;
import in.gov.sih.sih26135.entity.RefAuditAction;
import in.gov.sih.sih26135.event.listener.AuditTrailEventListener;
import in.gov.sih.sih26135.repository.AuditLogRepository;
import in.gov.sih.sih26135.repository.RefAuditActionRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditTrailEventListener Unit & Resilience Tests")
class AuditTrailEventListenerTest {

  @Mock
  private AuditLogRepository auditLogRepository;

  @Mock
  private RefAuditActionRepository refAuditActionRepository;

  @Mock
  private UserRepository userRepository;

  private AuditTrailEventListener listener;

  private RefAuditAction statusChangeAction;

  @BeforeEach
  void setUp() {
    listener = new AuditTrailEventListener(auditLogRepository, refAuditActionRepository, userRepository);
    statusChangeAction = new RefAuditAction("STATUS_CHANGE", "Status change", 6);
    statusChangeAction.setId(6L);
  }

  @Test
  @DisplayName("Handles valid AuditTrailEvent by looking up action and persisting AuditLog record")
  void testHandleAuditTrailEventPersistsSuccessfully() {
    when(refAuditActionRepository.findByActionCode("STATUS_CHANGE"))
        .thenReturn(Optional.of(statusChangeAction));
    when(userRepository.existsById(101L)).thenReturn(true);

    LocalDateTime now = LocalDateTime.now();
    AuditTrailEvent event = new AuditTrailEvent(
        "STATUS_CHANGE",
        "Placement",
        7001L,
        101L,
        "Placement transitioned to employment",
        now,
        "CORR-EVT-999"
    );

    listener.handleAuditTrailEvent(event);

    ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
    verify(auditLogRepository, times(1)).save(captor.capture());

    AuditLog saved = captor.getValue();
    assertThat(saved.getAuditAction()).isEqualTo(statusChangeAction);
    assertThat(saved.getEntityType()).isEqualTo("Placement");
    assertThat(saved.getEntityId()).isEqualTo(7001L);
    assertThat(saved.getActorUserId()).isEqualTo(101L);
    assertThat(saved.getCorrelationId()).isEqualTo("CORR-EVT-999");
    assertThat(saved.getChangeSummary()).isEqualTo("Placement transitioned to employment");
    assertThat(saved.getOccurredAt()).isEqualTo(now);
    assertThat(saved.getCreatedAt()).isNotNull();
  }

  @Test
  @DisplayName("Sanitizes sensitive data (Aadhaar, PAN, secrets) in change summaries")
  void testSanitizesSensitiveDataInSummary() {
    when(refAuditActionRepository.findByActionCode("STATUS_CHANGE"))
        .thenReturn(Optional.of(statusChangeAction));

    AuditTrailEvent event = new AuditTrailEvent(
        "STATUS_CHANGE",
        "Verification",
        8002L,
        null,
        "Verified Aadhaar 2345 6789 0123 and PAN ABCDE1234F with password=superSecretPassword123",
        LocalDateTime.now(),
        "CORR-PII-TEST"
    );

    listener.handleAuditTrailEvent(event);

    ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
    verify(auditLogRepository).save(captor.capture());

    String sanitized = captor.getValue().getChangeSummary();
    assertThat(sanitized).doesNotContain("2345 6789 0123");
    assertThat(sanitized).contains("XXXX-XXXX-0123");
    assertThat(sanitized).doesNotContain("ABCDE1234F");
    assertThat(sanitized).contains("XXXXX1234F");
    assertThat(sanitized).doesNotContain("superSecretPassword123");
    assertThat(sanitized).contains("password=***REDACTED***");
  }

  @Test
  @DisplayName("Persistence failure is isolated and does not disrupt business transactions")
  void testPersistenceFailureIsIsolated() {
    when(refAuditActionRepository.findByActionCode("STATUS_CHANGE"))
        .thenReturn(Optional.of(statusChangeAction));
    when(auditLogRepository.save(any(AuditLog.class)))
        .thenThrow(new DataIntegrityViolationException("Database constraint violation"));

    AuditTrailEvent event = new AuditTrailEvent(
        "STATUS_CHANGE",
        "TrainingEnrollment",
        9003L,
        null,
        "Course completed",
        LocalDateTime.now(),
        "CORR-FAIL-SAFE"
    );

    assertThatCode(() -> listener.handleAuditTrailEvent(event))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("Non-existent actor user ID is attributed as null to prevent foreign key violation")
  void testNonExistentActorAttributedAsNull() {
    when(refAuditActionRepository.findByActionCode("STATUS_CHANGE"))
        .thenReturn(Optional.of(statusChangeAction));
    when(userRepository.existsById(99999L)).thenReturn(false);

    AuditTrailEvent event = new AuditTrailEvent(
        "STATUS_CHANGE",
        "FollowupTask",
        4004L,
        99999L,
        "Survey response received",
        LocalDateTime.now(),
        "CORR-FK-CHECK"
    );

    listener.handleAuditTrailEvent(event);

    ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
    verify(auditLogRepository).save(captor.capture());
    assertThat(captor.getValue().getActorUserId()).isNull();
  }

  @Test
  @DisplayName("Null event handled gracefully without exceptions or repository interactions")
  void testNullEventHandledGracefully() {
    assertThatCode(() -> listener.handleAuditTrailEvent(null))
        .doesNotThrowAnyException();

    verify(auditLogRepository, never()).save(any());
  }
}
