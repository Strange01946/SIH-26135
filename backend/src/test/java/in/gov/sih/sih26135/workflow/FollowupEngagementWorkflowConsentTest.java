package in.gov.sih.sih26135.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.gov.sih.sih26135.dto.request.CreateCommunicationLogRequest;
import in.gov.sih.sih26135.dto.request.CreateSurveyResponseRequest;
import in.gov.sih.sih26135.dto.request.RecordEngagementAttemptRequest;
import in.gov.sih.sih26135.dto.request.RecordFollowupSurveyResponseRequest;
import in.gov.sih.sih26135.dto.response.CommunicationLogResponse;
import in.gov.sih.sih26135.dto.response.FollowupTaskResponse;
import in.gov.sih.sih26135.dto.response.RecordEngagementAttemptResponse;
import in.gov.sih.sih26135.dto.response.RecordFollowupSurveyResponseResponse;
import in.gov.sih.sih26135.dto.response.SurveyResponseResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.service.CommunicationLogService;
import in.gov.sih.sih26135.service.FollowupTaskService;
import in.gov.sih.sih26135.service.SurveyResponseService;
import in.gov.sih.sih26135.service.TraineeConsentService;
import in.gov.sih.sih26135.service.impl.FollowupEngagementWorkflowServiceImpl;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FollowupEngagementWorkflowConsentTest {

  @Mock private FollowupTaskService followupTaskService;
  @Mock private SurveyResponseService surveyResponseService;
  @Mock private CommunicationLogService communicationLogService;
  @Mock private TraineeConsentService traineeConsentService;

  private FollowupEngagementWorkflowServiceImpl workflowService;

  private static final Long TASK_ID = 50L;
  private static final Long TRAINEE_ID = 100L;
  private static final Long ENROLLMENT_ID = 200L;

  @BeforeEach
  void setUp() {
    workflowService = new FollowupEngagementWorkflowServiceImpl(
        followupTaskService,
        surveyResponseService,
        communicationLogService,
        traineeConsentService,
        mock(WorkflowSecurityScopeValidator.class)
    );
  }

  private FollowupTaskResponse buildFollowupTaskResponse() {
    FollowupTaskResponse response = new FollowupTaskResponse();
    response.setId(TASK_ID);
    response.setTraineeId(TRAINEE_ID);
    response.setEnrollmentId(ENROLLMENT_ID);
    return response;
  }

  @Nested
  @DisplayName("recordFollowupSurveyResponse consent validation tests")
  class RecordFollowupSurveyResponseConsentTests {

    @Test
    @DisplayName("Succeeds when trainee has active EMPLOYMENT_FOLLOW_UP consent")
    void succeedsWhenConsentGranted() {
      FollowupTaskResponse task = buildFollowupTaskResponse();
      when(followupTaskService.getFollowupTaskById(TASK_ID)).thenReturn(task);

      doNothing().when(traineeConsentService).validateConsent(TRAINEE_ID, "EMPLOYMENT_FOLLOW_UP");

      CreateSurveyResponseRequest surveyReq = new CreateSurveyResponseRequest();
      surveyReq.setSurveyId(1L);
      surveyReq.setSurveyTemplateVersionId(1L);

      RecordFollowupSurveyResponseRequest request = new RecordFollowupSurveyResponseRequest();
      request.setFollowupTaskId(TASK_ID);
      request.setSurveyResponseRequest(surveyReq);

      SurveyResponseResponse expectedResponse = new SurveyResponseResponse();
      expectedResponse.setId(10L);
      expectedResponse.setTraineeId(TRAINEE_ID);
      when(surveyResponseService.createSurveyResponse(any(CreateSurveyResponseRequest.class))).thenReturn(expectedResponse);

      RecordFollowupSurveyResponseResponse result = workflowService.recordFollowupSurveyResponse(request);

      assertThat(result).isNotNull();
      assertThat(result.surveyResponse()).isNotNull();
      assertThat(result.surveyResponse().getId()).isEqualTo(10L);

      verify(traineeConsentService).validateConsent(TRAINEE_ID, "EMPLOYMENT_FOLLOW_UP");
      verify(surveyResponseService).createSurveyResponse(surveyReq);
    }

    @Test
    @DisplayName("Fails and rejects survey creation when trainee consent is missing (CONSENT_REQUIRED)")
    void rejectsSurveyCreationWhenConsentMissing() {
      FollowupTaskResponse task = buildFollowupTaskResponse();
      when(followupTaskService.getFollowupTaskById(TASK_ID)).thenReturn(task);

      doThrow(new BadRequestException("Trainee has not granted consent for EMPLOYMENT_FOLLOW_UP", "CONSENT_REQUIRED"))
          .when(traineeConsentService).validateConsent(TRAINEE_ID, "EMPLOYMENT_FOLLOW_UP");

      CreateSurveyResponseRequest surveyReq = new CreateSurveyResponseRequest();
      RecordFollowupSurveyResponseRequest request = new RecordFollowupSurveyResponseRequest();
      request.setFollowupTaskId(TASK_ID);
      request.setSurveyResponseRequest(surveyReq);

      assertThatThrownBy(() -> workflowService.recordFollowupSurveyResponse(request))
          .isInstanceOf(BadRequestException.class)
          .hasFieldOrPropertyWithValue("errorCode", "CONSENT_REQUIRED");

      verify(traineeConsentService).validateConsent(TRAINEE_ID, "EMPLOYMENT_FOLLOW_UP");
      verify(surveyResponseService, never()).createSurveyResponse(any());
    }

    @Test
    @DisplayName("Fails and rejects survey creation when trainee consent is revoked (CONSENT_REVOKED)")
    void rejectsSurveyCreationWhenConsentRevoked() {
      FollowupTaskResponse task = buildFollowupTaskResponse();
      when(followupTaskService.getFollowupTaskById(TASK_ID)).thenReturn(task);

      doThrow(new BadRequestException("Trainee consent for EMPLOYMENT_FOLLOW_UP has been revoked", "CONSENT_REVOKED"))
          .when(traineeConsentService).validateConsent(TRAINEE_ID, "EMPLOYMENT_FOLLOW_UP");

      CreateSurveyResponseRequest surveyReq = new CreateSurveyResponseRequest();
      RecordFollowupSurveyResponseRequest request = new RecordFollowupSurveyResponseRequest();
      request.setFollowupTaskId(TASK_ID);
      request.setSurveyResponseRequest(surveyReq);

      assertThatThrownBy(() -> workflowService.recordFollowupSurveyResponse(request))
          .isInstanceOf(BadRequestException.class)
          .hasFieldOrPropertyWithValue("errorCode", "CONSENT_REVOKED");

      verify(traineeConsentService).validateConsent(TRAINEE_ID, "EMPLOYMENT_FOLLOW_UP");
      verify(surveyResponseService, never()).createSurveyResponse(any());
    }

    @Test
    @DisplayName("Fails and rejects survey creation when trainee consent has expired (CONSENT_EXPIRED)")
    void rejectsSurveyCreationWhenConsentExpired() {
      FollowupTaskResponse task = buildFollowupTaskResponse();
      when(followupTaskService.getFollowupTaskById(TASK_ID)).thenReturn(task);

      doThrow(new BadRequestException("Trainee consent for EMPLOYMENT_FOLLOW_UP has expired", "CONSENT_EXPIRED"))
          .when(traineeConsentService).validateConsent(TRAINEE_ID, "EMPLOYMENT_FOLLOW_UP");

      CreateSurveyResponseRequest surveyReq = new CreateSurveyResponseRequest();
      RecordFollowupSurveyResponseRequest request = new RecordFollowupSurveyResponseRequest();
      request.setFollowupTaskId(TASK_ID);
      request.setSurveyResponseRequest(surveyReq);

      assertThatThrownBy(() -> workflowService.recordFollowupSurveyResponse(request))
          .isInstanceOf(BadRequestException.class)
          .hasFieldOrPropertyWithValue("errorCode", "CONSENT_EXPIRED");

      verify(traineeConsentService).validateConsent(TRAINEE_ID, "EMPLOYMENT_FOLLOW_UP");
      verify(surveyResponseService, never()).createSurveyResponse(any());
    }
  }

  @Nested
  @DisplayName("recordEngagementAttempt consent validation tests")
  class RecordEngagementAttemptConsentTests {

    @Test
    @DisplayName("Succeeds when trainee has active EMPLOYMENT_FOLLOW_UP consent")
    void succeedsWhenConsentGranted() {
      FollowupTaskResponse task = buildFollowupTaskResponse();
      when(followupTaskService.getFollowupTaskById(TASK_ID)).thenReturn(task);

      doNothing().when(traineeConsentService).validateConsent(TRAINEE_ID, "EMPLOYMENT_FOLLOW_UP");

      CreateCommunicationLogRequest commReq = new CreateCommunicationLogRequest();
      commReq.setCommunicationChannelId(1L);

      RecordEngagementAttemptRequest request = new RecordEngagementAttemptRequest();
      request.setFollowupTaskId(TASK_ID);
      request.setCommunicationLogRequest(commReq);

      CommunicationLogResponse commResponse = new CommunicationLogResponse();
      commResponse.setId(20L);
      commResponse.setCommunicationChannelId(1L);
      when(communicationLogService.logCommunication(any(CreateCommunicationLogRequest.class))).thenReturn(commResponse);

      FollowupTaskResponse updatedTask = buildFollowupTaskResponse();
      when(followupTaskService.updateFollowupTask(eq(TASK_ID), any())).thenReturn(updatedTask);

      RecordEngagementAttemptResponse result = workflowService.recordEngagementAttempt(request);

      assertThat(result).isNotNull();
      assertThat(result.communicationLog()).isNotNull();
      assertThat(result.communicationLog().getId()).isEqualTo(20L);

      verify(traineeConsentService).validateConsent(TRAINEE_ID, "EMPLOYMENT_FOLLOW_UP");
      verify(communicationLogService).logCommunication(commReq);
    }

    @Test
    @DisplayName("Fails and rejects communication logging when consent is missing (CONSENT_REQUIRED)")
    void rejectsEngagementAttemptWhenConsentMissing() {
      FollowupTaskResponse task = buildFollowupTaskResponse();
      when(followupTaskService.getFollowupTaskById(TASK_ID)).thenReturn(task);

      doThrow(new BadRequestException("Trainee has not granted consent for EMPLOYMENT_FOLLOW_UP", "CONSENT_REQUIRED"))
          .when(traineeConsentService).validateConsent(TRAINEE_ID, "EMPLOYMENT_FOLLOW_UP");

      CreateCommunicationLogRequest commReq = new CreateCommunicationLogRequest();
      RecordEngagementAttemptRequest request = new RecordEngagementAttemptRequest();
      request.setFollowupTaskId(TASK_ID);
      request.setCommunicationLogRequest(commReq);

      assertThatThrownBy(() -> workflowService.recordEngagementAttempt(request))
          .isInstanceOf(BadRequestException.class)
          .hasFieldOrPropertyWithValue("errorCode", "CONSENT_REQUIRED");

      verify(traineeConsentService).validateConsent(TRAINEE_ID, "EMPLOYMENT_FOLLOW_UP");
      verify(communicationLogService, never()).logCommunication(any());
      verify(followupTaskService, never()).updateFollowupTask(any(), any());
    }

    @Test
    @DisplayName("Fails and rejects communication logging when consent is revoked (CONSENT_REVOKED)")
    void rejectsEngagementAttemptWhenConsentRevoked() {
      FollowupTaskResponse task = buildFollowupTaskResponse();
      when(followupTaskService.getFollowupTaskById(TASK_ID)).thenReturn(task);

      doThrow(new BadRequestException("Trainee consent for EMPLOYMENT_FOLLOW_UP has been revoked", "CONSENT_REVOKED"))
          .when(traineeConsentService).validateConsent(TRAINEE_ID, "EMPLOYMENT_FOLLOW_UP");

      CreateCommunicationLogRequest commReq = new CreateCommunicationLogRequest();
      RecordEngagementAttemptRequest request = new RecordEngagementAttemptRequest();
      request.setFollowupTaskId(TASK_ID);
      request.setCommunicationLogRequest(commReq);

      assertThatThrownBy(() -> workflowService.recordEngagementAttempt(request))
          .isInstanceOf(BadRequestException.class)
          .hasFieldOrPropertyWithValue("errorCode", "CONSENT_REVOKED");

      verify(traineeConsentService).validateConsent(TRAINEE_ID, "EMPLOYMENT_FOLLOW_UP");
      verify(communicationLogService, never()).logCommunication(any());
    }
  }
}
