package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.CreateCommunicationLogRequest;
import in.gov.sih.sih26135.dto.request.CreateSurveyResponseRequest;
import in.gov.sih.sih26135.dto.request.RecordEngagementAttemptRequest;
import in.gov.sih.sih26135.dto.request.RecordFollowupSurveyResponseRequest;
import in.gov.sih.sih26135.dto.request.UpdateFollowupTaskRequest;
import in.gov.sih.sih26135.dto.response.CommunicationLogResponse;
import in.gov.sih.sih26135.dto.response.FollowupTaskResponse;
import in.gov.sih.sih26135.dto.response.RecordEngagementAttemptResponse;
import in.gov.sih.sih26135.dto.response.RecordFollowupSurveyResponseResponse;
import in.gov.sih.sih26135.dto.response.SurveyResponseResponse;
import in.gov.sih.sih26135.dto.response.TraineeEngagementHistoryResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.service.CommunicationLogService;
import in.gov.sih.sih26135.service.FollowupEngagementWorkflowService;
import in.gov.sih.sih26135.service.FollowupTaskService;
import in.gov.sih.sih26135.service.SurveyResponseService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FollowupEngagementWorkflowServiceImpl implements FollowupEngagementWorkflowService {

  private final FollowupTaskService followupTaskService;
  private final SurveyResponseService surveyResponseService;
  private final CommunicationLogService communicationLogService;

  public FollowupEngagementWorkflowServiceImpl(
      FollowupTaskService followupTaskService,
      SurveyResponseService surveyResponseService,
      CommunicationLogService communicationLogService) {
    this.followupTaskService = followupTaskService;
    this.surveyResponseService = surveyResponseService;
    this.communicationLogService = communicationLogService;
  }

  @Override
  @Transactional
  public RecordFollowupSurveyResponseResponse recordFollowupSurveyResponse(
      RecordFollowupSurveyResponseRequest request) {
    if (request == null) {
      throw new BadRequestException("Request body cannot be null", "REQUEST_BODY_NULL");
    }
    if (request.getFollowupTaskId() == null) {
      throw new BadRequestException("Followup task ID is required", "FOLLOWUP_TASK_ID_REQUIRED");
    }
    if (request.getSurveyResponseRequest() == null) {
      throw new BadRequestException("Survey response request is required", "SURVEY_RESPONSE_REQUEST_REQUIRED");
    }

    FollowupTaskResponse task = followupTaskService.getFollowupTaskById(request.getFollowupTaskId());
    CreateSurveyResponseRequest surveyReq = request.getSurveyResponseRequest();

    if (surveyReq.getTraineeId() == null) {
      surveyReq.setTraineeId(task.getTraineeId());
    } else if (!surveyReq.getTraineeId().equals(task.getTraineeId())) {
      throw new BadRequestException(
          "Survey response trainee does not match followup task trainee",
          "TRAINEE_TASK_MISMATCH"
      );
    }

    surveyReq.setFollowupTaskId(task.getId());
    if (surveyReq.getEnrollmentId() == null) {
      surveyReq.setEnrollmentId(task.getEnrollmentId());
    }

    SurveyResponseResponse surveyResponse = surveyResponseService.createSurveyResponse(surveyReq);

    FollowupTaskResponse updatedTask = null;
    if (request.getFollowupTaskStatusId() != null || request.getFollowupTaskOutcomeId() != null) {
      UpdateFollowupTaskRequest updateTask = new UpdateFollowupTaskRequest();
      updateTask.setEnrollmentId(task.getEnrollmentId());
      updateTask.setPlacementId(task.getPlacementId());
      updateTask.setEmploymentId(task.getEmploymentId());
      updateTask.setSurveyId(task.getSurveyId());
      updateTask.setScheduledDate(task.getScheduledDate());
      updateTask.setNextFollowupDate(task.getNextFollowupDate());
      updateTask.setFollowupStatusId(
          request.getFollowupTaskStatusId() != null ? request.getFollowupTaskStatusId() : task.getFollowupStatusId());
      updateTask.setFollowupOutcomeId(
          request.getFollowupTaskOutcomeId() != null ? request.getFollowupTaskOutcomeId() : task.getFollowupOutcomeId());
      updateTask.setNonResponseReasonId(task.getNonResponseReasonId());
      updateTask.setAssignedUserId(task.getAssignedUserId());
      updateTask.setLastChannelId(task.getLastChannelId());
      updateTask.setRemarks(task.getRemarks());
      updateTask.setCompletedAt(LocalDateTime.now());

      updatedTask = followupTaskService.updateFollowupTask(task.getId(), updateTask);
    }

    return new RecordFollowupSurveyResponseResponse(surveyResponse, updatedTask);
  }

  @Override
  @Transactional
  public RecordEngagementAttemptResponse recordEngagementAttempt(RecordEngagementAttemptRequest request) {
    if (request == null) {
      throw new BadRequestException("Request body cannot be null", "REQUEST_BODY_NULL");
    }
    if (request.getFollowupTaskId() == null) {
      throw new BadRequestException("Followup task ID is required", "FOLLOWUP_TASK_ID_REQUIRED");
    }
    if (request.getCommunicationLogRequest() == null) {
      throw new BadRequestException("Communication log request is required", "COMMUNICATION_LOG_REQUEST_REQUIRED");
    }

    FollowupTaskResponse task = followupTaskService.getFollowupTaskById(request.getFollowupTaskId());
    CreateCommunicationLogRequest commReq = request.getCommunicationLogRequest();

    if (commReq.getTraineeId() == null) {
      commReq.setTraineeId(task.getTraineeId());
    } else if (!commReq.getTraineeId().equals(task.getTraineeId())) {
      throw new BadRequestException(
          "Communication log trainee does not match followup task trainee",
          "TRAINEE_TASK_MISMATCH"
      );
    }

    commReq.setFollowupTaskId(task.getId());
    CommunicationLogResponse commLog = communicationLogService.logCommunication(commReq);

    UpdateFollowupTaskRequest updateTask = new UpdateFollowupTaskRequest();
    updateTask.setEnrollmentId(task.getEnrollmentId());
    updateTask.setPlacementId(task.getPlacementId());
    updateTask.setEmploymentId(task.getEmploymentId());
    updateTask.setSurveyId(task.getSurveyId());
    updateTask.setScheduledDate(task.getScheduledDate());
    updateTask.setNextFollowupDate(task.getNextFollowupDate());
    updateTask.setFollowupStatusId(
        request.getFollowupTaskStatusId() != null ? request.getFollowupTaskStatusId() : task.getFollowupStatusId());
    updateTask.setFollowupOutcomeId(
        request.getFollowupTaskOutcomeId() != null ? request.getFollowupTaskOutcomeId() : task.getFollowupOutcomeId());
    updateTask.setNonResponseReasonId(task.getNonResponseReasonId());
    updateTask.setAssignedUserId(task.getAssignedUserId());
    updateTask.setLastChannelId(commLog.getCommunicationChannelId());
    updateTask.setRemarks(task.getRemarks());
    updateTask.setCompletedAt(task.getCompletedAt());

    FollowupTaskResponse updatedTask = followupTaskService.updateFollowupTask(task.getId(), updateTask);

    return new RecordEngagementAttemptResponse(commLog, updatedTask);
  }

  @Override
  public TraineeEngagementHistoryResponse getTraineeEngagementHistory(Long traineeId) {
    if (traineeId == null) {
      throw new BadRequestException("Trainee ID is required", "TRAINEE_ID_REQUIRED");
    }

    List<FollowupTaskResponse> tasks = followupTaskService.getFollowupTasksByTrainee(traineeId);
    List<SurveyResponseResponse> responses = surveyResponseService.getResponsesByTrainee(traineeId);

    return new TraineeEngagementHistoryResponse(traineeId, tasks, responses);
  }
}
