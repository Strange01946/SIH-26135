package in.gov.sih.sih26135.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.gov.sih.sih26135.dto.request.CreateSurveyResponseRequest;
import in.gov.sih.sih26135.dto.request.UpdateSurveyResponseRequest;
import in.gov.sih.sih26135.dto.response.SurveyResponseResponse;
import in.gov.sih.sih26135.entity.FollowupTask;
import in.gov.sih.sih26135.entity.SurveyResponse;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.mapper.SurveyResponseMapper;
import in.gov.sih.sih26135.repository.FollowupTaskRepository;
import in.gov.sih.sih26135.repository.SurveyResponseAnswerRepository;
import in.gov.sih.sih26135.repository.SurveyResponseRepository;
import in.gov.sih.sih26135.repository.TraineeRepository;
import in.gov.sih.sih26135.repository.TrainingEnrollmentRepository;
import in.gov.sih.sih26135.service.impl.SurveyResponseServiceImpl;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SurveyResponseConsentEnforcementTest {

  @Mock private SurveyResponseRepository surveyResponseRepository;
  @Mock private TraineeRepository traineeRepository;
  @Mock private TrainingEnrollmentRepository trainingEnrollmentRepository;
  @Mock private FollowupTaskRepository followupTaskRepository;
  @Mock private SurveyResponseAnswerRepository surveyResponseAnswerRepository;
  @Mock private TraineeConsentService traineeConsentService;

  private SurveyResponseMapper surveyResponseMapper;
  private SurveyResponseServiceImpl surveyResponseService;

  private static final Long TRAINEE_ID = 100L;
  private static final Long SURVEY_ID = 10L;
  private static final Long VERSION_ID = 1L;
  private static final Long STATUS_ID = 1L;
  private static final Long TASK_ID = 50L;
  private static final Long RESPONSE_ID = 500L;

  private Trainee trainee;
  private FollowupTask followupTask;

  @BeforeEach
  void setUp() {
    surveyResponseMapper = new SurveyResponseMapper();

    surveyResponseService = new SurveyResponseServiceImpl(
        surveyResponseRepository,
        traineeRepository,
        trainingEnrollmentRepository,
        followupTaskRepository,
        surveyResponseAnswerRepository,
        surveyResponseMapper,
        traineeConsentService
    );

    trainee = new Trainee();
    trainee.setId(TRAINEE_ID);

    followupTask = new FollowupTask();
    followupTask.setId(TASK_ID);
    followupTask.setTrainee(trainee);
    followupTask.setSurveyId(SURVEY_ID);
  }

  private CreateSurveyResponseRequest buildCreateRequest(Long followupTaskId) {
    CreateSurveyResponseRequest request = new CreateSurveyResponseRequest();
    request.setSurveyId(SURVEY_ID);
    request.setSurveyTemplateVersionId(VERSION_ID);
    request.setTraineeId(TRAINEE_ID);
    request.setSurveyResponseStatusId(STATUS_ID);
    request.setFollowupTaskId(followupTaskId);
    return request;
  }

  private SurveyResponse buildExistingResponse(FollowupTask attachedTask) {
    SurveyResponse response = new SurveyResponse();
    response.setId(RESPONSE_ID);
    response.setSurveyId(SURVEY_ID);
    response.setSurveyTemplateVersionId(VERSION_ID);
    response.setTrainee(trainee);
    response.setSurveyResponseStatusId(STATUS_ID);
    response.setFollowupTask(attachedTask);
    response.setStartedAt(LocalDateTime.now().minusHours(1));
    return response;
  }

  @Nested
  @DisplayName("Create path: follow-up task linked survey response consent enforcement")
  class CreateFollowupTaskLinkedSurveyResponseTests {

    @Test
    @DisplayName("Succeeds and saves when trainee has active EMPLOYMENT_FOLLOW_UP consent")
    void succeedsWhenConsentGranted() {
      CreateSurveyResponseRequest request = buildCreateRequest(TASK_ID);

      when(traineeRepository.findById(TRAINEE_ID)).thenReturn(Optional.of(trainee));
      when(followupTaskRepository.findById(TASK_ID)).thenReturn(Optional.of(followupTask));
      doNothing().when(traineeConsentService).validateConsent(TRAINEE_ID, "EMPLOYMENT_FOLLOW_UP");

      when(surveyResponseRepository.save(any(SurveyResponse.class))).thenAnswer(inv -> {
        SurveyResponse r = inv.getArgument(0);
        r.setId(999L);
        return r;
      });

      SurveyResponseResponse response = surveyResponseService.createSurveyResponse(request);

      assertThat(response).isNotNull();
      assertThat(response.getId()).isEqualTo(999L);
      assertThat(response.getFollowupTaskId()).isEqualTo(TASK_ID);

      verify(traineeConsentService).validateConsent(TRAINEE_ID, "EMPLOYMENT_FOLLOW_UP");
      verify(surveyResponseRepository).save(any(SurveyResponse.class));
    }

    @Test
    @DisplayName("Fails and aborts save when consent is missing (CONSENT_REQUIRED)")
    void failsWhenConsentMissing() {
      CreateSurveyResponseRequest request = buildCreateRequest(TASK_ID);

      when(traineeRepository.findById(TRAINEE_ID)).thenReturn(Optional.of(trainee));
      when(followupTaskRepository.findById(TASK_ID)).thenReturn(Optional.of(followupTask));
      doThrow(new BadRequestException("Trainee has not granted consent for EMPLOYMENT_FOLLOW_UP", "CONSENT_REQUIRED"))
          .when(traineeConsentService).validateConsent(TRAINEE_ID, "EMPLOYMENT_FOLLOW_UP");

      assertThatThrownBy(() -> surveyResponseService.createSurveyResponse(request))
          .isInstanceOf(BadRequestException.class)
          .hasFieldOrPropertyWithValue("errorCode", "CONSENT_REQUIRED");

      verify(traineeConsentService).validateConsent(TRAINEE_ID, "EMPLOYMENT_FOLLOW_UP");
      verify(surveyResponseRepository, never()).save(any());
    }

    @Test
    @DisplayName("Fails and aborts save when consent is revoked (CONSENT_REVOKED)")
    void failsWhenConsentRevoked() {
      CreateSurveyResponseRequest request = buildCreateRequest(TASK_ID);

      when(traineeRepository.findById(TRAINEE_ID)).thenReturn(Optional.of(trainee));
      when(followupTaskRepository.findById(TASK_ID)).thenReturn(Optional.of(followupTask));
      doThrow(new BadRequestException("Trainee consent for EMPLOYMENT_FOLLOW_UP has been revoked", "CONSENT_REVOKED"))
          .when(traineeConsentService).validateConsent(TRAINEE_ID, "EMPLOYMENT_FOLLOW_UP");

      assertThatThrownBy(() -> surveyResponseService.createSurveyResponse(request))
          .isInstanceOf(BadRequestException.class)
          .hasFieldOrPropertyWithValue("errorCode", "CONSENT_REVOKED");

      verify(traineeConsentService).validateConsent(TRAINEE_ID, "EMPLOYMENT_FOLLOW_UP");
      verify(surveyResponseRepository, never()).save(any());
    }

    @Test
    @DisplayName("Fails and aborts save when consent is expired (CONSENT_EXPIRED)")
    void failsWhenConsentExpired() {
      CreateSurveyResponseRequest request = buildCreateRequest(TASK_ID);

      when(traineeRepository.findById(TRAINEE_ID)).thenReturn(Optional.of(trainee));
      when(followupTaskRepository.findById(TASK_ID)).thenReturn(Optional.of(followupTask));
      doThrow(new BadRequestException("Trainee consent for EMPLOYMENT_FOLLOW_UP has expired", "CONSENT_EXPIRED"))
          .when(traineeConsentService).validateConsent(TRAINEE_ID, "EMPLOYMENT_FOLLOW_UP");

      assertThatThrownBy(() -> surveyResponseService.createSurveyResponse(request))
          .isInstanceOf(BadRequestException.class)
          .hasFieldOrPropertyWithValue("errorCode", "CONSENT_EXPIRED");

      verify(traineeConsentService).validateConsent(TRAINEE_ID, "EMPLOYMENT_FOLLOW_UP");
      verify(surveyResponseRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("Create path: general survey response without follow-up task")
  class CreateGeneralSurveyResponseTests {

    @Test
    @DisplayName("Does not validate follow-up consent when response is not linked to follow-up task")
    void succeedsWithoutConsentValidationForGeneralSurvey() {
      CreateSurveyResponseRequest request = buildCreateRequest(null);

      when(traineeRepository.findById(TRAINEE_ID)).thenReturn(Optional.of(trainee));
      when(surveyResponseRepository.save(any(SurveyResponse.class))).thenAnswer(inv -> {
        SurveyResponse r = inv.getArgument(0);
        r.setId(888L);
        return r;
      });

      SurveyResponseResponse response = surveyResponseService.createSurveyResponse(request);

      assertThat(response).isNotNull();
      assertThat(response.getId()).isEqualTo(888L);
      assertThat(response.getFollowupTaskId()).isNull();

      verify(traineeConsentService, never()).validateConsent(any(Long.class), any(String.class));
      verify(traineeConsentService, never()).validateConsent(any(Long.class), any(Long.class));
      verify(surveyResponseRepository).save(any(SurveyResponse.class));
    }
  }

  @Nested
  @DisplayName("Update path: survey response consent enforcement")
  class UpdateSurveyResponseConsentTests {

    @Test
    @DisplayName("Updating a response to attach a follow-up task with valid consent succeeds")
    void succeedsWhenAttachingFollowupTaskWithActiveConsent() {
      SurveyResponse existing = buildExistingResponse(null);
      UpdateSurveyResponseRequest request = new UpdateSurveyResponseRequest();
      request.setFollowupTaskId(TASK_ID);

      when(surveyResponseRepository.findById(RESPONSE_ID)).thenReturn(Optional.of(existing));
      when(followupTaskRepository.findById(TASK_ID)).thenReturn(Optional.of(followupTask));
      doNothing().when(traineeConsentService).validateConsent(TRAINEE_ID, "EMPLOYMENT_FOLLOW_UP");
      when(surveyResponseRepository.save(any(SurveyResponse.class))).thenAnswer(inv -> inv.getArgument(0));

      SurveyResponseResponse response = surveyResponseService.updateSurveyResponse(RESPONSE_ID, request);

      assertThat(response).isNotNull();
      assertThat(response.getFollowupTaskId()).isEqualTo(TASK_ID);

      verify(traineeConsentService).validateConsent(TRAINEE_ID, "EMPLOYMENT_FOLLOW_UP");
      verify(surveyResponseRepository).save(existing);
      assertThat(existing.getFollowupTask()).isEqualTo(followupTask);
    }

    @Test
    @DisplayName("Updating a response to attach a follow-up task without consent fails and aborts save")
    void failsWhenAttachingFollowupTaskWithoutConsent() {
      SurveyResponse existing = buildExistingResponse(null);
      UpdateSurveyResponseRequest request = new UpdateSurveyResponseRequest();
      request.setFollowupTaskId(TASK_ID);

      when(surveyResponseRepository.findById(RESPONSE_ID)).thenReturn(Optional.of(existing));
      when(followupTaskRepository.findById(TASK_ID)).thenReturn(Optional.of(followupTask));
      doThrow(new BadRequestException("Trainee has not granted consent for EMPLOYMENT_FOLLOW_UP", "CONSENT_REQUIRED"))
          .when(traineeConsentService).validateConsent(TRAINEE_ID, "EMPLOYMENT_FOLLOW_UP");

      assertThatThrownBy(() -> surveyResponseService.updateSurveyResponse(RESPONSE_ID, request))
          .isInstanceOf(BadRequestException.class)
          .hasFieldOrPropertyWithValue("errorCode", "CONSENT_REQUIRED");

      verify(traineeConsentService).validateConsent(TRAINEE_ID, "EMPLOYMENT_FOLLOW_UP");
      verify(surveyResponseRepository, never()).save(any());
    }

    @Test
    @DisplayName("Updating an already follow-up-linked response without consent is rejected, even if followupTaskId is omitted from request")
    void failsWhenUpdatingAlreadyLinkedResponseWithoutConsentEvenWhenTaskIdOmitted() {
      SurveyResponse existing = buildExistingResponse(followupTask);
      UpdateSurveyResponseRequest request = new UpdateSurveyResponseRequest();
      request.setSurveyResponseStatusId(2L); // followupTaskId omitted

      when(surveyResponseRepository.findById(RESPONSE_ID)).thenReturn(Optional.of(existing));
      doThrow(new BadRequestException("Trainee consent for EMPLOYMENT_FOLLOW_UP has been revoked", "CONSENT_REVOKED"))
          .when(traineeConsentService).validateConsent(TRAINEE_ID, "EMPLOYMENT_FOLLOW_UP");

      assertThatThrownBy(() -> surveyResponseService.updateSurveyResponse(RESPONSE_ID, request))
          .isInstanceOf(BadRequestException.class)
          .hasFieldOrPropertyWithValue("errorCode", "CONSENT_REVOKED");

      verify(traineeConsentService).validateConsent(TRAINEE_ID, "EMPLOYMENT_FOLLOW_UP");
      verify(surveyResponseRepository, never()).save(any());
    }

    @Test
    @DisplayName("Updating an already follow-up-linked response with active consent succeeds when followupTaskId is omitted")
    void succeedsWhenUpdatingAlreadyLinkedResponseWithActiveConsent() {
      SurveyResponse existing = buildExistingResponse(followupTask);
      UpdateSurveyResponseRequest request = new UpdateSurveyResponseRequest();
      request.setSurveyResponseStatusId(2L);

      when(surveyResponseRepository.findById(RESPONSE_ID)).thenReturn(Optional.of(existing));
      doNothing().when(traineeConsentService).validateConsent(TRAINEE_ID, "EMPLOYMENT_FOLLOW_UP");
      when(surveyResponseRepository.save(any(SurveyResponse.class))).thenAnswer(inv -> inv.getArgument(0));

      SurveyResponseResponse response = surveyResponseService.updateSurveyResponse(RESPONSE_ID, request);

      assertThat(response).isNotNull();
      assertThat(response.getSurveyResponseStatusId()).isEqualTo(2L);
      assertThat(response.getFollowupTaskId()).isEqualTo(TASK_ID);

      verify(traineeConsentService).validateConsent(TRAINEE_ID, "EMPLOYMENT_FOLLOW_UP");
      verify(surveyResponseRepository).save(existing);
    }

    @Test
    @DisplayName("An update to an unrelated survey response with no follow-up association does not require follow-up consent")
    void succeedsWithoutConsentWhenUpdatingUnrelatedResponse() {
      SurveyResponse existing = buildExistingResponse(null);
      UpdateSurveyResponseRequest request = new UpdateSurveyResponseRequest();
      request.setSurveyResponseStatusId(2L);

      when(surveyResponseRepository.findById(RESPONSE_ID)).thenReturn(Optional.of(existing));
      when(surveyResponseRepository.save(any(SurveyResponse.class))).thenAnswer(inv -> inv.getArgument(0));

      SurveyResponseResponse response = surveyResponseService.updateSurveyResponse(RESPONSE_ID, request);

      assertThat(response).isNotNull();
      assertThat(response.getSurveyResponseStatusId()).isEqualTo(2L);
      assertThat(response.getFollowupTaskId()).isNull();

      verify(traineeConsentService, never()).validateConsent(any(Long.class), any(String.class));
      verify(traineeConsentService, never()).validateConsent(any(Long.class), any(Long.class));
      verify(surveyResponseRepository).save(existing);
    }

    @Test
    @DisplayName("Existing trainee ownership check continues to reject mismatched follow-up task")
    void failsWhenFollowupTaskBelongsToDifferentTrainee() {
      SurveyResponse existing = buildExistingResponse(null);
      UpdateSurveyResponseRequest request = new UpdateSurveyResponseRequest();
      request.setFollowupTaskId(TASK_ID);

      Trainee otherTrainee = new Trainee();
      otherTrainee.setId(999L);
      FollowupTask mismatchedTask = new FollowupTask();
      mismatchedTask.setId(TASK_ID);
      mismatchedTask.setTrainee(otherTrainee);
      mismatchedTask.setSurveyId(SURVEY_ID);

      when(surveyResponseRepository.findById(RESPONSE_ID)).thenReturn(Optional.of(existing));
      when(followupTaskRepository.findById(TASK_ID)).thenReturn(Optional.of(mismatchedTask));

      assertThatThrownBy(() -> surveyResponseService.updateSurveyResponse(RESPONSE_ID, request))
          .isInstanceOf(BadRequestException.class)
          .hasFieldOrPropertyWithValue("errorCode", "TRAINEE_TASK_MISMATCH");

      verify(traineeConsentService, never()).validateConsent(any(Long.class), any(String.class));
      verify(traineeConsentService, never()).validateConsent(any(Long.class), any(Long.class));
      verify(surveyResponseRepository, never()).save(any());
    }

    @Test
    @DisplayName("Existing survey mismatch check continues to reject follow-up task with mismatched survey ID")
    void failsWhenFollowupTaskSurveyIdMismatches() {
      SurveyResponse existing = buildExistingResponse(null);
      UpdateSurveyResponseRequest request = new UpdateSurveyResponseRequest();
      request.setFollowupTaskId(TASK_ID);

      FollowupTask mismatchedSurveyTask = new FollowupTask();
      mismatchedSurveyTask.setId(TASK_ID);
      mismatchedSurveyTask.setTrainee(trainee);
      mismatchedSurveyTask.setSurveyId(999L); // Differs from existing.getSurveyId() (10L)

      when(surveyResponseRepository.findById(RESPONSE_ID)).thenReturn(Optional.of(existing));
      when(followupTaskRepository.findById(TASK_ID)).thenReturn(Optional.of(mismatchedSurveyTask));

      assertThatThrownBy(() -> surveyResponseService.updateSurveyResponse(RESPONSE_ID, request))
          .isInstanceOf(BadRequestException.class)
          .hasFieldOrPropertyWithValue("errorCode", "SURVEY_MISMATCH");

      verify(traineeConsentService, never()).validateConsent(any(Long.class), any(String.class));
      verify(traineeConsentService, never()).validateConsent(any(Long.class), any(Long.class));
      verify(surveyResponseRepository, never()).save(any());
    }
  }
}
