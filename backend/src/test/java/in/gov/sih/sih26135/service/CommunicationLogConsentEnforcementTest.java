package in.gov.sih.sih26135.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.gov.sih.sih26135.dto.request.CreateCommunicationLogRequest;
import in.gov.sih.sih26135.dto.response.CommunicationLogResponse;
import in.gov.sih.sih26135.entity.CommunicationLog;
import in.gov.sih.sih26135.entity.RefCommunicationChannel;
import in.gov.sih.sih26135.entity.RefCommunicationDirection;
import in.gov.sih.sih26135.entity.RefCommunicationPurpose;
import in.gov.sih.sih26135.entity.RefCommunicationStatus;
import in.gov.sih.sih26135.entity.RefConsentType;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.mapper.CommunicationLogMapper;
import in.gov.sih.sih26135.repository.CommunicationLogRepository;
import in.gov.sih.sih26135.repository.FollowupTaskRepository;
import in.gov.sih.sih26135.repository.RefCommunicationChannelRepository;
import in.gov.sih.sih26135.repository.RefCommunicationDirectionRepository;
import in.gov.sih.sih26135.repository.RefCommunicationPurposeRepository;
import in.gov.sih.sih26135.repository.RefCommunicationStatusRepository;
import in.gov.sih.sih26135.repository.RefConsentTypeRepository;
import in.gov.sih.sih26135.repository.SurveyResponseRepository;
import in.gov.sih.sih26135.repository.TraineeRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import in.gov.sih.sih26135.service.impl.CommunicationLogServiceImpl;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommunicationLogConsentEnforcementTest {

  @Mock private CommunicationLogRepository communicationLogRepository;
  @Mock private TraineeRepository traineeRepository;
  @Mock private FollowupTaskRepository followupTaskRepository;
  @Mock private SurveyResponseRepository surveyResponseRepository;
  @Mock private RefCommunicationChannelRepository refCommunicationChannelRepository;
  @Mock private RefCommunicationDirectionRepository refCommunicationDirectionRepository;
  @Mock private RefCommunicationPurposeRepository refCommunicationPurposeRepository;
  @Mock private RefCommunicationStatusRepository refCommunicationStatusRepository;
  @Mock private RefConsentTypeRepository refConsentTypeRepository;
  @Mock private UserRepository userRepository;
  @Mock private TraineeConsentService traineeConsentService;

  private CommunicationLogMapper communicationLogMapper;
  private CommunicationLogServiceImpl communicationLogService;

  private static final Long TRAINEE_ID = 100L;
  private static final Long CHANNEL_ID = 1L;
  private static final Long DIRECTION_OUTBOUND_ID = 1L;
  private static final Long DIRECTION_INBOUND_ID = 2L;
  private static final Long PURPOSE_ID = 1L;
  private static final Long STATUS_ID = 1L;

  private Trainee trainee;
  private RefCommunicationChannel channelWithConsent;
  private RefCommunicationChannel channelWithoutConsent;
  private RefCommunicationDirection directionOutbound;
  private RefCommunicationDirection directionInbound;
  private RefCommunicationPurpose purposeWithConsent;
  private RefCommunicationPurpose purposeWithoutConsent;
  private RefCommunicationStatus communicationStatus;
  private RefConsentType smsConsentType;
  private RefConsentType followupConsentType;

  @BeforeEach
  void setUp() {
    communicationLogMapper = new CommunicationLogMapper();

    communicationLogService = new CommunicationLogServiceImpl(
        communicationLogRepository,
        traineeRepository,
        followupTaskRepository,
        surveyResponseRepository,
        refCommunicationChannelRepository,
        refCommunicationDirectionRepository,
        refCommunicationPurposeRepository,
        refCommunicationStatusRepository,
        refConsentTypeRepository,
        userRepository,
        communicationLogMapper,
        traineeConsentService
    );

    trainee = new Trainee();
    trainee.setId(TRAINEE_ID);

    smsConsentType = new RefConsentType();
    smsConsentType.setId(5L);
    smsConsentType.setConsentCode("COMMUNICATION_SMS");

    followupConsentType = new RefConsentType();
    followupConsentType.setId(2L);
    followupConsentType.setConsentCode("EMPLOYMENT_FOLLOW_UP");

    channelWithConsent = new RefCommunicationChannel();
    channelWithConsent.setId(CHANNEL_ID);
    channelWithConsent.setChannelCode("SMS");
    channelWithConsent.setRequiredConsentType(smsConsentType);

    channelWithoutConsent = new RefCommunicationChannel();
    channelWithoutConsent.setId(CHANNEL_ID);
    channelWithoutConsent.setChannelCode("INTERNAL_NOTE");
    channelWithoutConsent.setRequiredConsentType(null);

    directionOutbound = new RefCommunicationDirection();
    directionOutbound.setId(DIRECTION_OUTBOUND_ID);
    directionOutbound.setDirectionCode("OUTBOUND");

    directionInbound = new RefCommunicationDirection();
    directionInbound.setId(DIRECTION_INBOUND_ID);
    directionInbound.setDirectionCode("INBOUND");

    purposeWithConsent = new RefCommunicationPurpose();
    purposeWithConsent.setId(PURPOSE_ID);
    purposeWithConsent.setPurposeCode("FOLLOW_UP_SURVEY");
    purposeWithConsent.setRequiredConsentType(followupConsentType);

    purposeWithoutConsent = new RefCommunicationPurpose();
    purposeWithoutConsent.setId(PURPOSE_ID);
    purposeWithoutConsent.setPurposeCode("GENERAL_INFO");
    purposeWithoutConsent.setRequiredConsentType(null);

    communicationStatus = new RefCommunicationStatus();
    communicationStatus.setId(STATUS_ID);
    communicationStatus.setStatusCode("DELIVERED");
  }

  private CreateCommunicationLogRequest buildRequest() {
    CreateCommunicationLogRequest req = new CreateCommunicationLogRequest();
    req.setTraineeId(TRAINEE_ID);
    req.setCommunicationChannelId(CHANNEL_ID);
    req.setCommunicationDirectionId(DIRECTION_OUTBOUND_ID);
    req.setCommunicationPurposeId(PURPOSE_ID);
    req.setCommunicationStatusId(STATUS_ID);
    return req;
  }

  @Nested
  @DisplayName("Outbound communication consent enforcement")
  class OutboundConsentEnforcementTests {

    @Test
    @DisplayName("Succeeds and populates consentType and consentCheckedAt when channel consent is active")
    void succeedsWhenChannelConsentValid() {
      CreateCommunicationLogRequest request = buildRequest();

      when(traineeRepository.findById(TRAINEE_ID)).thenReturn(Optional.of(trainee));
      when(refCommunicationChannelRepository.findById(CHANNEL_ID)).thenReturn(Optional.of(channelWithConsent));
      when(refCommunicationDirectionRepository.findById(DIRECTION_OUTBOUND_ID)).thenReturn(Optional.of(directionOutbound));
      when(refCommunicationPurposeRepository.findById(PURPOSE_ID)).thenReturn(Optional.of(purposeWithoutConsent));
      when(refCommunicationStatusRepository.findById(STATUS_ID)).thenReturn(Optional.of(communicationStatus));

      doNothing().when(traineeConsentService).validateConsent(TRAINEE_ID, "COMMUNICATION_SMS");

      when(communicationLogRepository.save(any(CommunicationLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

      CommunicationLogResponse response = communicationLogService.logCommunication(request);

      assertThat(response).isNotNull();
      verify(traineeConsentService).validateConsent(TRAINEE_ID, "COMMUNICATION_SMS");

      ArgumentCaptor<CommunicationLog> captor = ArgumentCaptor.forClass(CommunicationLog.class);
      verify(communicationLogRepository).save(captor.capture());
      CommunicationLog saved = captor.getValue();
      assertThat(saved.getConsentType()).isEqualTo(smsConsentType);
      assertThat(saved.getConsentCheckedAt()).isNotNull();
    }

    @Test
    @DisplayName("Fails and aborts save when channel consent is missing or revoked")
    void failsWhenChannelConsentMissingOrRevoked() {
      CreateCommunicationLogRequest request = buildRequest();

      when(traineeRepository.findById(TRAINEE_ID)).thenReturn(Optional.of(trainee));
      when(refCommunicationChannelRepository.findById(CHANNEL_ID)).thenReturn(Optional.of(channelWithConsent));
      when(refCommunicationDirectionRepository.findById(DIRECTION_OUTBOUND_ID)).thenReturn(Optional.of(directionOutbound));
      when(refCommunicationPurposeRepository.findById(PURPOSE_ID)).thenReturn(Optional.of(purposeWithoutConsent));
      when(refCommunicationStatusRepository.findById(STATUS_ID)).thenReturn(Optional.of(communicationStatus));

      doThrow(new BadRequestException("Trainee consent for COMMUNICATION_SMS has been revoked", "CONSENT_REVOKED"))
          .when(traineeConsentService).validateConsent(TRAINEE_ID, "COMMUNICATION_SMS");

      assertThatThrownBy(() -> communicationLogService.logCommunication(request))
          .isInstanceOf(BadRequestException.class)
          .hasFieldOrPropertyWithValue("errorCode", "CONSENT_REVOKED");

      verify(traineeConsentService).validateConsent(TRAINEE_ID, "COMMUNICATION_SMS");
      verify(communicationLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("Succeeds and validates both when channel and purpose both require consent; records channel audit")
    void validatesBothChannelAndPurposeConsent() {
      CreateCommunicationLogRequest request = buildRequest();

      when(traineeRepository.findById(TRAINEE_ID)).thenReturn(Optional.of(trainee));
      when(refCommunicationChannelRepository.findById(CHANNEL_ID)).thenReturn(Optional.of(channelWithConsent));
      when(refCommunicationDirectionRepository.findById(DIRECTION_OUTBOUND_ID)).thenReturn(Optional.of(directionOutbound));
      when(refCommunicationPurposeRepository.findById(PURPOSE_ID)).thenReturn(Optional.of(purposeWithConsent));
      when(refCommunicationStatusRepository.findById(STATUS_ID)).thenReturn(Optional.of(communicationStatus));

      doNothing().when(traineeConsentService).validateConsent(TRAINEE_ID, "COMMUNICATION_SMS");
      doNothing().when(traineeConsentService).validateConsent(TRAINEE_ID, "EMPLOYMENT_FOLLOW_UP");

      when(communicationLogRepository.save(any(CommunicationLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

      CommunicationLogResponse response = communicationLogService.logCommunication(request);

      assertThat(response).isNotNull();
      verify(traineeConsentService).validateConsent(TRAINEE_ID, "COMMUNICATION_SMS");
      verify(traineeConsentService).validateConsent(TRAINEE_ID, "EMPLOYMENT_FOLLOW_UP");

      ArgumentCaptor<CommunicationLog> captor = ArgumentCaptor.forClass(CommunicationLog.class);
      verify(communicationLogRepository).save(captor.capture());
      CommunicationLog saved = captor.getValue();
      assertThat(saved.getConsentType()).isEqualTo(smsConsentType);
      assertThat(saved.getConsentCheckedAt()).isNotNull();
    }

    @Test
    @DisplayName("Succeeds and records purpose audit when purpose requires consent and channel does not")
    void succeedsWithPurposeConsentAuditWhenChannelHasNoRequirement() {
      CreateCommunicationLogRequest request = buildRequest();

      when(traineeRepository.findById(TRAINEE_ID)).thenReturn(Optional.of(trainee));
      when(refCommunicationChannelRepository.findById(CHANNEL_ID)).thenReturn(Optional.of(channelWithoutConsent));
      when(refCommunicationDirectionRepository.findById(DIRECTION_OUTBOUND_ID)).thenReturn(Optional.of(directionOutbound));
      when(refCommunicationPurposeRepository.findById(PURPOSE_ID)).thenReturn(Optional.of(purposeWithConsent));
      when(refCommunicationStatusRepository.findById(STATUS_ID)).thenReturn(Optional.of(communicationStatus));

      doNothing().when(traineeConsentService).validateConsent(TRAINEE_ID, "EMPLOYMENT_FOLLOW_UP");

      when(communicationLogRepository.save(any(CommunicationLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

      CommunicationLogResponse response = communicationLogService.logCommunication(request);

      assertThat(response).isNotNull();
      verify(traineeConsentService).validateConsent(TRAINEE_ID, "EMPLOYMENT_FOLLOW_UP");

      ArgumentCaptor<CommunicationLog> captor = ArgumentCaptor.forClass(CommunicationLog.class);
      verify(communicationLogRepository).save(captor.capture());
      CommunicationLog saved = captor.getValue();
      assertThat(saved.getConsentType()).isEqualTo(followupConsentType);
      assertThat(saved.getConsentCheckedAt()).isNotNull();
    }

    @Test
    @DisplayName("Fails if channel consent passes but purpose consent fails")
    void failsWhenPurposeConsentFails() {
      CreateCommunicationLogRequest request = buildRequest();

      when(traineeRepository.findById(TRAINEE_ID)).thenReturn(Optional.of(trainee));
      when(refCommunicationChannelRepository.findById(CHANNEL_ID)).thenReturn(Optional.of(channelWithConsent));
      when(refCommunicationDirectionRepository.findById(DIRECTION_OUTBOUND_ID)).thenReturn(Optional.of(directionOutbound));
      when(refCommunicationPurposeRepository.findById(PURPOSE_ID)).thenReturn(Optional.of(purposeWithConsent));
      when(refCommunicationStatusRepository.findById(STATUS_ID)).thenReturn(Optional.of(communicationStatus));

      doNothing().when(traineeConsentService).validateConsent(TRAINEE_ID, "COMMUNICATION_SMS");
      doThrow(new BadRequestException("Trainee consent for EMPLOYMENT_FOLLOW_UP has expired", "CONSENT_EXPIRED"))
          .when(traineeConsentService).validateConsent(TRAINEE_ID, "EMPLOYMENT_FOLLOW_UP");

      assertThatThrownBy(() -> communicationLogService.logCommunication(request))
          .isInstanceOf(BadRequestException.class)
          .hasFieldOrPropertyWithValue("errorCode", "CONSENT_EXPIRED");

      verify(communicationLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("Succeeds without consent audit fields when neither channel nor purpose requires consent")
    void succeedsWithoutAuditFieldsWhenNoConsentRequired() {
      CreateCommunicationLogRequest request = buildRequest();

      when(traineeRepository.findById(TRAINEE_ID)).thenReturn(Optional.of(trainee));
      when(refCommunicationChannelRepository.findById(CHANNEL_ID)).thenReturn(Optional.of(channelWithoutConsent));
      when(refCommunicationDirectionRepository.findById(DIRECTION_OUTBOUND_ID)).thenReturn(Optional.of(directionOutbound));
      when(refCommunicationPurposeRepository.findById(PURPOSE_ID)).thenReturn(Optional.of(purposeWithoutConsent));
      when(refCommunicationStatusRepository.findById(STATUS_ID)).thenReturn(Optional.of(communicationStatus));

      when(communicationLogRepository.save(any(CommunicationLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

      CommunicationLogResponse response = communicationLogService.logCommunication(request);

      assertThat(response).isNotNull();
      verify(traineeConsentService, never()).validateConsent(any(Long.class), any(String.class));

      ArgumentCaptor<CommunicationLog> captor = ArgumentCaptor.forClass(CommunicationLog.class);
      verify(communicationLogRepository).save(captor.capture());
      CommunicationLog saved = captor.getValue();
      assertThat(saved.getConsentType()).isNull();
      assertThat(saved.getConsentCheckedAt()).isNull();
    }
  }

  @Nested
  @DisplayName("Inbound communication consent handling")
  class InboundConsentHandlingTests {

    @Test
    @DisplayName("Inbound communication does not trigger outbound checks and leaves consent audit fields null")
    void doesNotValidateConsentForInboundAndLeavesAuditNull() {
      CreateCommunicationLogRequest request = buildRequest();
      request.setCommunicationDirectionId(DIRECTION_INBOUND_ID);

      when(traineeRepository.findById(TRAINEE_ID)).thenReturn(Optional.of(trainee));
      when(refCommunicationChannelRepository.findById(CHANNEL_ID)).thenReturn(Optional.of(channelWithConsent));
      when(refCommunicationDirectionRepository.findById(DIRECTION_INBOUND_ID)).thenReturn(Optional.of(directionInbound));
      when(refCommunicationPurposeRepository.findById(PURPOSE_ID)).thenReturn(Optional.of(purposeWithConsent));
      when(refCommunicationStatusRepository.findById(STATUS_ID)).thenReturn(Optional.of(communicationStatus));

      when(communicationLogRepository.save(any(CommunicationLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

      CommunicationLogResponse response = communicationLogService.logCommunication(request);

      assertThat(response).isNotNull();
      verify(traineeConsentService, never()).validateConsent(any(Long.class), any(String.class));
      verify(traineeConsentService, never()).validateConsent(any(Long.class), any(Long.class));

      ArgumentCaptor<CommunicationLog> captor = ArgumentCaptor.forClass(CommunicationLog.class);
      verify(communicationLogRepository).save(captor.capture());
      CommunicationLog saved = captor.getValue();
      assertThat(saved.getConsentType()).isNull();
      assertThat(saved.getConsentCheckedAt()).isNull();
    }
  }

  @Nested
  @DisplayName("Explicit consentTypeId handling")
  class ExplicitConsentTypeIdTests {

    @Test
    @DisplayName("Validates explicit consentTypeId for inbound message and populates audit fields upon success")
    void validatesExplicitConsentTypeIdForInbound() {
      CreateCommunicationLogRequest request = buildRequest();
      request.setCommunicationDirectionId(DIRECTION_INBOUND_ID);
      request.setConsentTypeId(5L);

      when(traineeRepository.findById(TRAINEE_ID)).thenReturn(Optional.of(trainee));
      when(refCommunicationChannelRepository.findById(CHANNEL_ID)).thenReturn(Optional.of(channelWithoutConsent));
      when(refCommunicationDirectionRepository.findById(DIRECTION_INBOUND_ID)).thenReturn(Optional.of(directionInbound));
      when(refCommunicationPurposeRepository.findById(PURPOSE_ID)).thenReturn(Optional.of(purposeWithoutConsent));
      when(refCommunicationStatusRepository.findById(STATUS_ID)).thenReturn(Optional.of(communicationStatus));

      when(refConsentTypeRepository.findById(5L)).thenReturn(Optional.of(smsConsentType));
      doNothing().when(traineeConsentService).validateConsent(TRAINEE_ID, "COMMUNICATION_SMS");

      when(communicationLogRepository.save(any(CommunicationLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

      CommunicationLogResponse response = communicationLogService.logCommunication(request);

      assertThat(response).isNotNull();
      verify(traineeConsentService).validateConsent(TRAINEE_ID, "COMMUNICATION_SMS");

      ArgumentCaptor<CommunicationLog> captor = ArgumentCaptor.forClass(CommunicationLog.class);
      verify(communicationLogRepository).save(captor.capture());
      CommunicationLog saved = captor.getValue();
      assertThat(saved.getConsentType()).isEqualTo(smsConsentType);
      assertThat(saved.getConsentCheckedAt()).isNotNull();
    }

    @Test
    @DisplayName("Rejects and aborts save when explicit consentTypeId validation fails")
    void rejectsWhenExplicitConsentTypeIdFails() {
      CreateCommunicationLogRequest request = buildRequest();
      request.setConsentTypeId(5L);

      when(traineeRepository.findById(TRAINEE_ID)).thenReturn(Optional.of(trainee));
      when(refCommunicationChannelRepository.findById(CHANNEL_ID)).thenReturn(Optional.of(channelWithoutConsent));
      when(refCommunicationDirectionRepository.findById(DIRECTION_OUTBOUND_ID)).thenReturn(Optional.of(directionOutbound));
      when(refCommunicationPurposeRepository.findById(PURPOSE_ID)).thenReturn(Optional.of(purposeWithoutConsent));
      when(refCommunicationStatusRepository.findById(STATUS_ID)).thenReturn(Optional.of(communicationStatus));

      when(refConsentTypeRepository.findById(5L)).thenReturn(Optional.of(smsConsentType));
      doThrow(new BadRequestException("Trainee consent for COMMUNICATION_SMS has been revoked", "CONSENT_REVOKED"))
          .when(traineeConsentService).validateConsent(TRAINEE_ID, "COMMUNICATION_SMS");

      assertThatThrownBy(() -> communicationLogService.logCommunication(request))
          .isInstanceOf(BadRequestException.class)
          .hasFieldOrPropertyWithValue("errorCode", "CONSENT_REVOKED");

      verify(traineeConsentService).validateConsent(TRAINEE_ID, "COMMUNICATION_SMS");
      verify(communicationLogRepository, never()).save(any());
    }
  }
}
