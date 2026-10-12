package in.gov.sih.sih26135.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import in.gov.sih.sih26135.entity.RefConsentStatus;
import in.gov.sih.sih26135.entity.RefConsentType;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.entity.TraineeConsent;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.TraineeConsentMapper;
import in.gov.sih.sih26135.repository.RefConsentStatusRepository;
import in.gov.sih.sih26135.repository.RefConsentTypeRepository;
import in.gov.sih.sih26135.repository.TraineeConsentRepository;
import in.gov.sih.sih26135.repository.TraineeRepository;
import in.gov.sih.sih26135.service.impl.TraineeConsentServiceImpl;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TraineeConsentValidationTest {

  @Mock private TraineeConsentRepository traineeConsentRepository;
  @Mock private TraineeRepository traineeRepository;
  @Mock private RefConsentTypeRepository refConsentTypeRepository;
  @Mock private RefConsentStatusRepository refConsentStatusRepository;
  @Mock private TraineeConsentMapper traineeConsentMapper;

  private TraineeConsentServiceImpl consentService;

  private static final Long TRAINEE_ID = 100L;
  private static final Long CONSENT_TYPE_ID = 2L;
  private static final String CONSENT_CODE = "EMPLOYMENT_FOLLOW_UP";

  private RefConsentType consentType;
  private RefConsentStatus statusGranted;
  private RefConsentStatus statusRevoked;
  private RefConsentStatus statusExpired;
  private RefConsentStatus statusDenied;

  @BeforeEach
  void setUp() {
    consentService = new TraineeConsentServiceImpl(
        traineeConsentRepository,
        traineeRepository,
        refConsentTypeRepository,
        refConsentStatusRepository,
        traineeConsentMapper
    );

    consentType = new RefConsentType();
    consentType.setId(CONSENT_TYPE_ID);
    consentType.setConsentCode(CONSENT_CODE);

    statusGranted = new RefConsentStatus();
    statusGranted.setId(1L);
    statusGranted.setStatusCode("GRANTED");

    statusRevoked = new RefConsentStatus();
    statusRevoked.setId(2L);
    statusRevoked.setStatusCode("REVOKED");

    statusExpired = new RefConsentStatus();
    statusExpired.setId(3L);
    statusExpired.setStatusCode("EXPIRED");

    statusDenied = new RefConsentStatus();
    statusDenied.setId(4L);
    statusDenied.setStatusCode("DENIED");
  }

  private TraineeConsent buildConsent(Long id, RefConsentStatus status, LocalDateTime grantedAt, LocalDateTime expiresAt, LocalDateTime revokedAt) {
    TraineeConsent consent = new TraineeConsent();
    consent.setId(id);
    consent.setConsentStatus(status);
    consent.setConsentType(consentType);
    consent.setGrantedAt(grantedAt);
    consent.setExpiresAt(expiresAt);
    consent.setRevokedAt(revokedAt);
    return consent;
  }

  @Nested
  @DisplayName("hasActiveConsent query tests")
  class HasActiveConsentTests {

    @Test
    @DisplayName("Returns false for null or blank parameters")
    void returnsFalseForInvalidParameters() {
      assertThat(consentService.hasActiveConsent(null, CONSENT_CODE)).isFalse();
      assertThat(consentService.hasActiveConsent(TRAINEE_ID, null)).isFalse();
      assertThat(consentService.hasActiveConsent(TRAINEE_ID, "   ")).isFalse();
    }

    @Test
    @DisplayName("Returns false when trainee does not exist")
    void returnsFalseWhenTraineeNotFound() {
      when(traineeRepository.existsById(TRAINEE_ID)).thenReturn(false);
      assertThat(consentService.hasActiveConsent(TRAINEE_ID, CONSENT_CODE)).isFalse();
    }

    @Test
    @DisplayName("Returns false when consent type does not exist")
    void returnsFalseWhenConsentTypeNotFound() {
      when(traineeRepository.existsById(TRAINEE_ID)).thenReturn(true);
      when(refConsentTypeRepository.findByConsentCode(CONSENT_CODE)).thenReturn(Optional.empty());

      assertThat(consentService.hasActiveConsent(TRAINEE_ID, CONSENT_CODE)).isFalse();
    }

    @Test
    @DisplayName("Returns false when no consent record exists")
    void returnsFalseWhenNoConsents() {
      when(traineeRepository.existsById(TRAINEE_ID)).thenReturn(true);
      when(refConsentTypeRepository.findByConsentCode(CONSENT_CODE)).thenReturn(Optional.of(consentType));
      when(traineeConsentRepository.findByTraineeIdAndConsentTypeId(TRAINEE_ID, CONSENT_TYPE_ID))
          .thenReturn(Collections.emptyList());

      assertThat(consentService.hasActiveConsent(TRAINEE_ID, CONSENT_CODE)).isFalse();
    }

    @Test
    @DisplayName("Returns true when valid GRANTED consent exists")
    void returnsTrueWhenValidConsentExists() {
      when(traineeRepository.existsById(TRAINEE_ID)).thenReturn(true);
      when(refConsentTypeRepository.findByConsentCode(CONSENT_CODE)).thenReturn(Optional.of(consentType));
      TraineeConsent validConsent = buildConsent(10L, statusGranted, LocalDateTime.now().minusDays(5), LocalDateTime.now().plusMonths(6), null);
      when(traineeConsentRepository.findByTraineeIdAndConsentTypeId(TRAINEE_ID, CONSENT_TYPE_ID))
          .thenReturn(List.of(validConsent));

      assertThat(consentService.hasActiveConsent(TRAINEE_ID, CONSENT_CODE)).isTrue();
    }

    @Test
    @DisplayName("Returns true when latest record is GRANTED even if earlier record was REVOKED")
    void returnsTrueWhenLatestIsGranted() {
      when(traineeRepository.existsById(TRAINEE_ID)).thenReturn(true);
      when(refConsentTypeRepository.findByConsentCode(CONSENT_CODE)).thenReturn(Optional.of(consentType));
      TraineeConsent oldRevoked = buildConsent(5L, statusRevoked, LocalDateTime.now().minusYears(1), null, LocalDateTime.now().minusMonths(6));
      TraineeConsent newGranted = buildConsent(15L, statusGranted, LocalDateTime.now().minusDays(1), LocalDateTime.now().plusMonths(6), null);
      when(traineeConsentRepository.findByTraineeIdAndConsentTypeId(TRAINEE_ID, CONSENT_TYPE_ID))
          .thenReturn(List.of(oldRevoked, newGranted));

      assertThat(consentService.hasActiveConsent(TRAINEE_ID, CONSENT_CODE)).isTrue();
    }

    @Test
    @DisplayName("Returns false when latest record is REVOKED even if earlier record was GRANTED")
    void returnsFalseWhenLatestIsRevoked() {
      when(traineeRepository.existsById(TRAINEE_ID)).thenReturn(true);
      when(refConsentTypeRepository.findByConsentCode(CONSENT_CODE)).thenReturn(Optional.of(consentType));
      TraineeConsent oldGranted = buildConsent(5L, statusGranted, LocalDateTime.now().minusMonths(6), null, null);
      TraineeConsent newRevoked = buildConsent(15L, statusRevoked, LocalDateTime.now().minusDays(2), null, LocalDateTime.now().minusDays(1));
      when(traineeConsentRepository.findByTraineeIdAndConsentTypeId(TRAINEE_ID, CONSENT_TYPE_ID))
          .thenReturn(List.of(oldGranted, newRevoked));

      assertThat(consentService.hasActiveConsent(TRAINEE_ID, CONSENT_CODE)).isFalse();
    }

    @Test
    @DisplayName("Returns false when consent has expired")
    void returnsFalseWhenExpired() {
      when(traineeRepository.existsById(TRAINEE_ID)).thenReturn(true);
      when(refConsentTypeRepository.findByConsentCode(CONSENT_CODE)).thenReturn(Optional.of(consentType));
      TraineeConsent expired = buildConsent(10L, statusGranted, LocalDateTime.now().minusMonths(6), LocalDateTime.now().minusDays(1), null);
      when(traineeConsentRepository.findByTraineeIdAndConsentTypeId(TRAINEE_ID, CONSENT_TYPE_ID))
          .thenReturn(List.of(expired));

      assertThat(consentService.hasActiveConsent(TRAINEE_ID, CONSENT_CODE)).isFalse();
    }

    @Test
    @DisplayName("Returns false when consent grantedAt is in future")
    void returnsFalseWhenFutureEffective() {
      when(traineeRepository.existsById(TRAINEE_ID)).thenReturn(true);
      when(refConsentTypeRepository.findByConsentCode(CONSENT_CODE)).thenReturn(Optional.of(consentType));
      TraineeConsent futureConsent = buildConsent(10L, statusGranted, LocalDateTime.now().plusDays(2), LocalDateTime.now().plusMonths(6), null);
      when(traineeConsentRepository.findByTraineeIdAndConsentTypeId(TRAINEE_ID, CONSENT_TYPE_ID))
          .thenReturn(List.of(futureConsent));

      assertThat(consentService.hasActiveConsent(TRAINEE_ID, CONSENT_CODE)).isFalse();
    }
  }

  @Nested
  @DisplayName("validateConsent(traineeId, consentCode) enforcement tests")
  class ValidateConsentByCodeTests {

    @Test
    @DisplayName("Throws BadRequestException when traineeId is null")
    void throwsWhenTraineeIdNull() {
      assertThatThrownBy(() -> consentService.validateConsent(null, CONSENT_CODE))
          .isInstanceOf(BadRequestException.class)
          .hasFieldOrPropertyWithValue("errorCode", "TRAINEE_ID_REQUIRED");
    }

    @Test
    @DisplayName("Throws BadRequestException when consentCode is blank")
    void throwsWhenConsentCodeBlank() {
      assertThatThrownBy(() -> consentService.validateConsent(TRAINEE_ID, "   "))
          .isInstanceOf(BadRequestException.class)
          .hasFieldOrPropertyWithValue("errorCode", "CONSENT_CODE_REQUIRED");
    }

    @Test
    @DisplayName("Throws ResourceNotFoundException when trainee does not exist")
    void throwsWhenTraineeNotFound() {
      when(traineeRepository.existsById(TRAINEE_ID)).thenReturn(false);

      assertThatThrownBy(() -> consentService.validateConsent(TRAINEE_ID, CONSENT_CODE))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Throws ResourceNotFoundException when consent type does not exist")
    void throwsWhenConsentTypeNotFound() {
      when(traineeRepository.existsById(TRAINEE_ID)).thenReturn(true);
      when(refConsentTypeRepository.findByConsentCode(CONSENT_CODE)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> consentService.validateConsent(TRAINEE_ID, CONSENT_CODE))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Throws CONSENT_REQUIRED when trainee has no consent record")
    void throwsConsentRequiredWhenNoRecord() {
      when(traineeRepository.existsById(TRAINEE_ID)).thenReturn(true);
      when(refConsentTypeRepository.findByConsentCode(CONSENT_CODE)).thenReturn(Optional.of(consentType));
      when(traineeConsentRepository.findByTraineeIdAndConsentTypeId(TRAINEE_ID, CONSENT_TYPE_ID))
          .thenReturn(Collections.emptyList());

      assertThatThrownBy(() -> consentService.validateConsent(TRAINEE_ID, CONSENT_CODE))
          .isInstanceOf(BadRequestException.class)
          .hasFieldOrPropertyWithValue("errorCode", "CONSENT_REQUIRED");
    }

    @Test
    @DisplayName("Throws CONSENT_REVOKED when consent status is REVOKED")
    void throwsConsentRevokedWhenStatusRevoked() {
      when(traineeRepository.existsById(TRAINEE_ID)).thenReturn(true);
      when(refConsentTypeRepository.findByConsentCode(CONSENT_CODE)).thenReturn(Optional.of(consentType));
      TraineeConsent revoked = buildConsent(10L, statusRevoked, LocalDateTime.now().minusDays(5), null, LocalDateTime.now().minusDays(1));
      when(traineeConsentRepository.findByTraineeIdAndConsentTypeId(TRAINEE_ID, CONSENT_TYPE_ID))
          .thenReturn(List.of(revoked));

      assertThatThrownBy(() -> consentService.validateConsent(TRAINEE_ID, CONSENT_CODE))
          .isInstanceOf(BadRequestException.class)
          .hasFieldOrPropertyWithValue("errorCode", "CONSENT_REVOKED");
    }

    @Test
    @DisplayName("Throws CONSENT_EXPIRED when consent status is EXPIRED")
    void throwsConsentExpiredWhenStatusExpired() {
      when(traineeRepository.existsById(TRAINEE_ID)).thenReturn(true);
      when(refConsentTypeRepository.findByConsentCode(CONSENT_CODE)).thenReturn(Optional.of(consentType));
      TraineeConsent expired = buildConsent(10L, statusExpired, LocalDateTime.now().minusMonths(6), LocalDateTime.now().minusDays(1), null);
      when(traineeConsentRepository.findByTraineeIdAndConsentTypeId(TRAINEE_ID, CONSENT_TYPE_ID))
          .thenReturn(List.of(expired));

      assertThatThrownBy(() -> consentService.validateConsent(TRAINEE_ID, CONSENT_CODE))
          .isInstanceOf(BadRequestException.class)
          .hasFieldOrPropertyWithValue("errorCode", "CONSENT_EXPIRED");
    }

    @Test
    @DisplayName("Throws CONSENT_NOT_GRANTED when consent status is DENIED")
    void throwsConsentNotGrantedWhenStatusDenied() {
      when(traineeRepository.existsById(TRAINEE_ID)).thenReturn(true);
      when(refConsentTypeRepository.findByConsentCode(CONSENT_CODE)).thenReturn(Optional.of(consentType));
      TraineeConsent denied = buildConsent(10L, statusDenied, LocalDateTime.now().minusDays(5), null, null);
      when(traineeConsentRepository.findByTraineeIdAndConsentTypeId(TRAINEE_ID, CONSENT_TYPE_ID))
          .thenReturn(List.of(denied));

      assertThatThrownBy(() -> consentService.validateConsent(TRAINEE_ID, CONSENT_CODE))
          .isInstanceOf(BadRequestException.class)
          .hasFieldOrPropertyWithValue("errorCode", "CONSENT_NOT_GRANTED");
    }

    @Test
    @DisplayName("Throws CONSENT_REVOKED when revokedAt timestamp has passed despite GRANTED status")
    void throwsConsentRevokedWhenRevokedAtPassed() {
      when(traineeRepository.existsById(TRAINEE_ID)).thenReturn(true);
      when(refConsentTypeRepository.findByConsentCode(CONSENT_CODE)).thenReturn(Optional.of(consentType));
      TraineeConsent consent = buildConsent(10L, statusGranted, LocalDateTime.now().minusDays(10), null, LocalDateTime.now().minusMinutes(5));
      when(traineeConsentRepository.findByTraineeIdAndConsentTypeId(TRAINEE_ID, CONSENT_TYPE_ID))
          .thenReturn(List.of(consent));

      assertThatThrownBy(() -> consentService.validateConsent(TRAINEE_ID, CONSENT_CODE))
          .isInstanceOf(BadRequestException.class)
          .hasFieldOrPropertyWithValue("errorCode", "CONSENT_REVOKED");
    }

    @Test
    @DisplayName("Throws CONSENT_EXPIRED when expiresAt timestamp has passed despite GRANTED status")
    void throwsConsentExpiredWhenExpiresAtPassed() {
      when(traineeRepository.existsById(TRAINEE_ID)).thenReturn(true);
      when(refConsentTypeRepository.findByConsentCode(CONSENT_CODE)).thenReturn(Optional.of(consentType));
      TraineeConsent consent = buildConsent(10L, statusGranted, LocalDateTime.now().minusDays(30), LocalDateTime.now().minusMinutes(1), null);
      when(traineeConsentRepository.findByTraineeIdAndConsentTypeId(TRAINEE_ID, CONSENT_TYPE_ID))
          .thenReturn(List.of(consent));

      assertThatThrownBy(() -> consentService.validateConsent(TRAINEE_ID, CONSENT_CODE))
          .isInstanceOf(BadRequestException.class)
          .hasFieldOrPropertyWithValue("errorCode", "CONSENT_EXPIRED");
    }

    @Test
    @DisplayName("Throws CONSENT_NOT_YET_EFFECTIVE when grantedAt is in future")
    void throwsConsentNotYetEffectiveWhenGrantedAtInFuture() {
      when(traineeRepository.existsById(TRAINEE_ID)).thenReturn(true);
      when(refConsentTypeRepository.findByConsentCode(CONSENT_CODE)).thenReturn(Optional.of(consentType));
      TraineeConsent consent = buildConsent(10L, statusGranted, LocalDateTime.now().plusDays(2), LocalDateTime.now().plusMonths(6), null);
      when(traineeConsentRepository.findByTraineeIdAndConsentTypeId(TRAINEE_ID, CONSENT_TYPE_ID))
          .thenReturn(List.of(consent));

      assertThatThrownBy(() -> consentService.validateConsent(TRAINEE_ID, CONSENT_CODE))
          .isInstanceOf(BadRequestException.class)
          .hasFieldOrPropertyWithValue("errorCode", "CONSENT_NOT_YET_EFFECTIVE");
    }

    @Test
    @DisplayName("Succeeds quietly when consent is fully valid and active")
    void succeedsWhenValidConsent() {
      when(traineeRepository.existsById(TRAINEE_ID)).thenReturn(true);
      when(refConsentTypeRepository.findByConsentCode(CONSENT_CODE)).thenReturn(Optional.of(consentType));
      TraineeConsent valid = buildConsent(10L, statusGranted, LocalDateTime.now().minusDays(5), LocalDateTime.now().plusMonths(6), null);
      when(traineeConsentRepository.findByTraineeIdAndConsentTypeId(TRAINEE_ID, CONSENT_TYPE_ID))
          .thenReturn(List.of(valid));

      // Should complete without throwing
      consentService.validateConsent(TRAINEE_ID, CONSENT_CODE);
    }
  }

  @Nested
  @DisplayName("validateConsent(traineeId, consentTypeId) validation tests")
  class ValidateConsentByIdTests {

    @Test
    @DisplayName("Throws BadRequestException when consentTypeId is null")
    void throwsWhenConsentTypeIdNull() {
      assertThatThrownBy(() -> consentService.validateConsent(TRAINEE_ID, (Long) null))
          .isInstanceOf(BadRequestException.class)
          .hasFieldOrPropertyWithValue("errorCode", "CONSENT_TYPE_ID_REQUIRED");
    }

    @Test
    @DisplayName("Throws ResourceNotFoundException when consentTypeId does not exist")
    void throwsWhenConsentTypeIdNotFound() {
      when(traineeRepository.existsById(TRAINEE_ID)).thenReturn(true);
      when(refConsentTypeRepository.findById(CONSENT_TYPE_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> consentService.validateConsent(TRAINEE_ID, CONSENT_TYPE_ID))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Succeeds when valid consent exists looked up by ID")
    void succeedsWhenValidConsentById() {
      when(traineeRepository.existsById(TRAINEE_ID)).thenReturn(true);
      when(refConsentTypeRepository.findById(CONSENT_TYPE_ID)).thenReturn(Optional.of(consentType));
      TraineeConsent valid = buildConsent(10L, statusGranted, LocalDateTime.now().minusDays(5), LocalDateTime.now().plusMonths(6), null);
      when(traineeConsentRepository.findByTraineeIdAndConsentTypeId(TRAINEE_ID, CONSENT_TYPE_ID))
          .thenReturn(List.of(valid));

      consentService.validateConsent(TRAINEE_ID, CONSENT_TYPE_ID);
    }
  }
}
