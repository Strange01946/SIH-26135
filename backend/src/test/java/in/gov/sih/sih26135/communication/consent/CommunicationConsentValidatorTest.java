package in.gov.sih.sih26135.communication.consent;

import in.gov.sih.sih26135.communication.exception.CommunicationConsentException;
import in.gov.sih.sih26135.entity.RefCommunicationChannel;
import in.gov.sih.sih26135.entity.RefCommunicationPurpose;
import in.gov.sih.sih26135.entity.RefConsentStatus;
import in.gov.sih.sih26135.entity.RefConsentType;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.entity.TraineeConsent;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.repository.RefCommunicationChannelRepository;
import in.gov.sih.sih26135.repository.RefCommunicationPurposeRepository;
import in.gov.sih.sih26135.repository.RefConsentTypeRepository;
import in.gov.sih.sih26135.repository.TraineeConsentRepository;
import in.gov.sih.sih26135.repository.TraineeRepository;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Communication Consent Validator Unit Tests")
class CommunicationConsentValidatorTest {

  @Mock
  private TraineeRepository traineeRepository;

  @Mock
  private TraineeConsentRepository traineeConsentRepository;

  @Mock
  private RefConsentTypeRepository refConsentTypeRepository;

  @Mock
  private RefCommunicationChannelRepository refCommunicationChannelRepository;

  @Mock
  private RefCommunicationPurposeRepository refCommunicationPurposeRepository;

  @Mock
  private WorkflowSecurityScopeValidator workflowSecurityScopeValidator;

  private CommunicationConsentValidatorImpl validator;

  private Trainee testTrainee;
  private RefConsentType smsConsentType;
  private RefConsentType whatsappConsentType;
  private RefConsentType emailConsentType;
  private RefConsentType followupConsentType;
  private RefConsentStatus grantedStatus;
  private RefConsentStatus revokedStatus;
  private RefConsentStatus expiredStatus;
  private RefCommunicationChannel smsChannel;
  private RefCommunicationPurpose followupPurpose;

  @BeforeEach
  void setUp() {
    validator = new CommunicationConsentValidatorImpl(
        traineeRepository,
        traineeConsentRepository,
        refConsentTypeRepository,
        refCommunicationChannelRepository,
        refCommunicationPurposeRepository,
        workflowSecurityScopeValidator
    );

    testTrainee = new Trainee();
    testTrainee.setFirstName("Rahul");
    testTrainee.setLastName("Sharma");
    testTrainee.setPhone("+919876543210");
    testTrainee.setEmail("rahul.sharma@example.com");

    smsConsentType = new RefConsentType("COMMUNICATION_SMS", "SMS Communication", "SMS", false, true);
    smsConsentType.setId(5L);

    whatsappConsentType = new RefConsentType("COMMUNICATION_WHATSAPP", "WhatsApp Communication", "WhatsApp", false, false);
    whatsappConsentType.setId(6L);

    emailConsentType = new RefConsentType("COMMUNICATION_EMAIL", "Email Communication", "Email", false, false);
    emailConsentType.setId(7L);

    followupConsentType = new RefConsentType("EMPLOYMENT_FOLLOW_UP", "Employment Follow-up", "Follow-up", true, false);
    followupConsentType.setId(2L);

    grantedStatus = new RefConsentStatus("GRANTED", "Granted", 1);
    grantedStatus.setId(1L);

    revokedStatus = new RefConsentStatus("REVOKED", "Revoked", 2);
    revokedStatus.setId(2L);

    expiredStatus = new RefConsentStatus("EXPIRED", "Expired", 3);
    expiredStatus.setId(3L);

    smsChannel = new RefCommunicationChannel("SMS", "SMS", 1);
    smsChannel.setRequiredConsentType(smsConsentType);

    followupPurpose = new RefCommunicationPurpose("FOLLOWUP_REMINDER", "Follow-up Reminder", 1);
    followupPurpose.setRequiredConsentType(followupConsentType);
  }

  @Test
  @DisplayName("Valid SMS consent permits outbound SMS dispatch")
  void testValidSmsConsentPermitsDispatch() {
    when(traineeRepository.existsById(101L)).thenReturn(true);
    when(refCommunicationChannelRepository.findByChannelCode("SMS")).thenReturn(Optional.of(smsChannel));
    when(refConsentTypeRepository.findByConsentCode("COMMUNICATION_SMS")).thenReturn(Optional.of(smsConsentType));

    TraineeConsent consent = new TraineeConsent(testTrainee, smsConsentType, "Outbound SMS", grantedStatus, "v1.0");
    consent.setGrantedAt(LocalDateTime.now().minusDays(10));
    consent.setExpiresAt(LocalDateTime.now().plusYears(1));
    when(traineeConsentRepository.findByTraineeIdAndConsentTypeId(101L, 5L))
        .thenReturn(List.of(consent));

    // Must execute without exception
    validator.validateCommunicationConsent(101L, "SMS", "OPERATIONAL");

    verify(workflowSecurityScopeValidator).validateTraineeScope(101L);
  }

  @Test
  @DisplayName("Missing consent throws CommunicationConsentException with CONSENT_NOT_GRANTED and HTTP 403")
  void testMissingConsentThrowsConsentNotGranted() {
    when(traineeRepository.existsById(101L)).thenReturn(true);
    when(refCommunicationChannelRepository.findByChannelCode("SMS")).thenReturn(Optional.of(smsChannel));
    when(refConsentTypeRepository.findByConsentCode("COMMUNICATION_SMS")).thenReturn(Optional.of(smsConsentType));
    when(traineeConsentRepository.findByTraineeIdAndConsentTypeId(101L, 5L))
        .thenReturn(Collections.emptyList());

    assertThatThrownBy(() -> validator.validateCommunicationConsent(101L, "SMS", "OPERATIONAL"))
        .isInstanceOf(CommunicationConsentException.class)
        .satisfies(ex -> {
          CommunicationConsentException cce = (CommunicationConsentException) ex;
          assertThat(cce.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
          assertThat(cce.getErrorCode()).isEqualTo("CONSENT_NOT_GRANTED");
          assertThat(cce.getMessage()).contains("Trainee has not granted communication consent for COMMUNICATION_SMS");
        });
  }

  @Test
  @DisplayName("Revoked consent throws CommunicationConsentException with CONSENT_WITHDRAWN and HTTP 403")
  void testRevokedConsentThrowsConsentWithdrawn() {
    when(traineeRepository.existsById(101L)).thenReturn(true);
    when(refCommunicationChannelRepository.findByChannelCode("SMS")).thenReturn(Optional.of(smsChannel));
    when(refConsentTypeRepository.findByConsentCode("COMMUNICATION_SMS")).thenReturn(Optional.of(smsConsentType));

    TraineeConsent revokedConsent = new TraineeConsent(testTrainee, smsConsentType, "Outbound SMS", revokedStatus, "v1.0");
    revokedConsent.setGrantedAt(LocalDateTime.now().minusDays(30));
    revokedConsent.setRevokedAt(LocalDateTime.now().minusDays(2));
    when(traineeConsentRepository.findByTraineeIdAndConsentTypeId(101L, 5L))
        .thenReturn(List.of(revokedConsent));

    assertThatThrownBy(() -> validator.validateCommunicationConsent(101L, "SMS", "OPERATIONAL"))
        .isInstanceOf(CommunicationConsentException.class)
        .satisfies(ex -> {
          CommunicationConsentException cce = (CommunicationConsentException) ex;
          assertThat(cce.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
          assertThat(cce.getErrorCode()).isEqualTo("CONSENT_WITHDRAWN");
          assertThat(cce.getMessage()).contains("has been revoked");
        });
  }

  @Test
  @DisplayName("Expired consent throws CommunicationConsentException with CONSENT_NOT_GRANTED and HTTP 403")
  void testExpiredConsentThrowsConsentNotGranted() {
    when(traineeRepository.existsById(101L)).thenReturn(true);
    when(refCommunicationChannelRepository.findByChannelCode("SMS")).thenReturn(Optional.of(smsChannel));
    when(refConsentTypeRepository.findByConsentCode("COMMUNICATION_SMS")).thenReturn(Optional.of(smsConsentType));

    TraineeConsent expiredConsent = new TraineeConsent(testTrainee, smsConsentType, "Outbound SMS", grantedStatus, "v1.0");
    expiredConsent.setGrantedAt(LocalDateTime.now().minusDays(365));
    expiredConsent.setExpiresAt(LocalDateTime.now().minusDays(1)); // Expired yesterday
    when(traineeConsentRepository.findByTraineeIdAndConsentTypeId(101L, 5L))
        .thenReturn(List.of(expiredConsent));

    assertThatThrownBy(() -> validator.validateCommunicationConsent(101L, "SMS", "OPERATIONAL"))
        .isInstanceOf(CommunicationConsentException.class)
        .satisfies(ex -> {
          CommunicationConsentException cce = (CommunicationConsentException) ex;
          assertThat(cce.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
          assertThat(cce.getErrorCode()).isEqualTo("CONSENT_NOT_GRANTED");
          assertThat(cce.getMessage()).contains("has expired");
        });
  }

  @Test
  @DisplayName("Future/not-yet-effective consent throws CONSENT_NOT_GRANTED")
  void testNotYetEffectiveConsentThrowsConsentNotGranted() {
    when(traineeRepository.existsById(101L)).thenReturn(true);
    when(refCommunicationChannelRepository.findByChannelCode("SMS")).thenReturn(Optional.of(smsChannel));
    when(refConsentTypeRepository.findByConsentCode("COMMUNICATION_SMS")).thenReturn(Optional.of(smsConsentType));

    TraineeConsent futureConsent = new TraineeConsent(testTrainee, smsConsentType, "Outbound SMS", grantedStatus, "v1.0");
    futureConsent.setGrantedAt(LocalDateTime.now().plusDays(2)); // Effective in 2 days
    futureConsent.setExpiresAt(LocalDateTime.now().plusYears(1));
    when(traineeConsentRepository.findByTraineeIdAndConsentTypeId(101L, 5L))
        .thenReturn(List.of(futureConsent));

    assertThatThrownBy(() -> validator.validateCommunicationConsent(101L, "SMS", "OPERATIONAL"))
        .isInstanceOf(CommunicationConsentException.class)
        .satisfies(ex -> {
          CommunicationConsentException cce = (CommunicationConsentException) ex;
          assertThat(cce.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
          assertThat(cce.getErrorCode()).isEqualTo("CONSENT_NOT_GRANTED");
          assertThat(cce.getMessage()).contains("is not yet effective");
        });
  }

  @Test
  @DisplayName("Cross-channel consent substitution is strictly rejected: SMS consent cannot authorize WhatsApp")
  void testCrossChannelConsentSubstitutionRejected() {
    when(traineeRepository.existsById(101L)).thenReturn(true);
    RefCommunicationChannel waChannel = new RefCommunicationChannel("WHATSAPP", "WhatsApp", 2);
    waChannel.setRequiredConsentType(whatsappConsentType);
    when(refCommunicationChannelRepository.findByChannelCode("WHATSAPP")).thenReturn(Optional.of(waChannel));
    when(refConsentTypeRepository.findByConsentCode("COMMUNICATION_WHATSAPP")).thenReturn(Optional.of(whatsappConsentType));

    // Trainee only has SMS consent, no WhatsApp consent
    when(traineeConsentRepository.findByTraineeIdAndConsentTypeId(101L, 6L))
        .thenReturn(Collections.emptyList());

    assertThatThrownBy(() -> validator.validateCommunicationConsent(101L, "WHATSAPP", "OPERATIONAL"))
        .isInstanceOf(CommunicationConsentException.class)
        .satisfies(ex -> {
          CommunicationConsentException cce = (CommunicationConsentException) ex;
          assertThat(cce.getErrorCode()).isEqualTo("CONSENT_NOT_GRANTED");
          assertThat(cce.getMessage()).contains("COMMUNICATION_WHATSAPP");
        });

    // Proves SMS consent was not checked to substitute for WhatsApp
    verify(traineeConsentRepository, never()).findByTraineeIdAndConsentTypeId(eq(101L), eq(5L));
  }

  @Test
  @DisplayName("Purpose-specific consent is enforced: FOLLOWUP_REMINDER mandates EMPLOYMENT_FOLLOW_UP consent")
  void testPurposeSpecificConsentEnforced() {
    when(traineeRepository.existsById(101L)).thenReturn(true);
    when(refCommunicationChannelRepository.findByChannelCode("SMS")).thenReturn(Optional.of(smsChannel));
    when(refConsentTypeRepository.findByConsentCode("COMMUNICATION_SMS")).thenReturn(Optional.of(smsConsentType));
    when(refCommunicationPurposeRepository.findByPurposeCode("FOLLOWUP_REMINDER")).thenReturn(Optional.of(followupPurpose));
    when(refConsentTypeRepository.findByConsentCode("EMPLOYMENT_FOLLOW_UP")).thenReturn(Optional.of(followupConsentType));

    // SMS consent is GRANTED
    TraineeConsent smsConsent = new TraineeConsent(testTrainee, smsConsentType, "Outbound SMS", grantedStatus, "v1.0");
    smsConsent.setGrantedAt(LocalDateTime.now().minusDays(10));
    smsConsent.setExpiresAt(LocalDateTime.now().plusYears(1));
    when(traineeConsentRepository.findByTraineeIdAndConsentTypeId(101L, 5L)).thenReturn(List.of(smsConsent));

    // But EMPLOYMENT_FOLLOW_UP consent is missing
    when(traineeConsentRepository.findByTraineeIdAndConsentTypeId(101L, 2L)).thenReturn(Collections.emptyList());

    assertThatThrownBy(() -> validator.validateCommunicationConsent(101L, "SMS", "FOLLOWUP_REMINDER"))
        .isInstanceOf(CommunicationConsentException.class)
        .satisfies(ex -> {
          CommunicationConsentException cce = (CommunicationConsentException) ex;
          assertThat(cce.getErrorCode()).isEqualTo("CONSENT_NOT_GRANTED");
          assertThat(cce.getMessage()).contains("EMPLOYMENT_FOLLOW_UP");
        });
  }

  @Test
  @DisplayName("Non-existent trainee throws ResourceNotFoundException")
  void testNonExistentTraineeThrowsResourceNotFound() {
    when(traineeRepository.existsById(999L)).thenReturn(false);

    assertThatThrownBy(() -> validator.validateCommunicationConsent(999L, "SMS", "OPERATIONAL"))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  @DisplayName("Null trainee ID or channel code throws BadRequestException")
  void testNullParametersThrowBadRequest() {
    assertThatThrownBy(() -> validator.validateCommunicationConsent(null, "SMS", "OPERATIONAL"))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Trainee ID is required");

    assertThatThrownBy(() -> validator.validateCommunicationConsent(101L, null, "OPERATIONAL"))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("channel code is required");
  }
}
