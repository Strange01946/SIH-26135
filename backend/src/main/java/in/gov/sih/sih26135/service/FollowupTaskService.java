package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateFollowupTaskRequest;
import in.gov.sih.sih26135.dto.request.UpdateFollowupTaskRequest;
import in.gov.sih.sih26135.dto.response.FollowupTaskResponse;
import java.time.LocalDate;
import java.util.List;

public interface FollowupTaskService {

  FollowupTaskResponse createFollowupTask(CreateFollowupTaskRequest request);

  FollowupTaskResponse updateFollowupTask(Long id, UpdateFollowupTaskRequest request);

  FollowupTaskResponse getFollowupTaskById(Long id);

  FollowupTaskResponse getFollowupTaskByCampaignAndTrainee(Long campaignId, Long traineeId);

  List<FollowupTaskResponse> getFollowupTasksByCampaign(Long campaignId);

  List<FollowupTaskResponse> getFollowupTasksByTrainee(Long traineeId);

  List<FollowupTaskResponse> getFollowupTasksByEnrollment(Long enrollmentId);

  List<FollowupTaskResponse> getFollowupTasksByPlacement(Long placementId);

  List<FollowupTaskResponse> getFollowupTasksByEmployment(Long employmentId);

  List<FollowupTaskResponse> getFollowupTasksBySurvey(Long surveyId);

  List<FollowupTaskResponse> getFollowupTasksByStatus(Long statusId);

  List<FollowupTaskResponse> getFollowupTasksByOutcome(Long outcomeId);

  List<FollowupTaskResponse> getFollowupTasksByAssignedUser(Long assignedUserId);

  List<FollowupTaskResponse> getFollowupTasksByScheduledDate(LocalDate scheduledDate);

  void deleteFollowupTask(Long id);
}
