package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateFollowupTaskRequest;
import in.gov.sih.sih26135.dto.request.UpdateFollowupTaskRequest;
import in.gov.sih.sih26135.dto.response.FollowupTaskResponse;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FollowupTaskService {

  FollowupTaskResponse createFollowupTask(CreateFollowupTaskRequest request);

  FollowupTaskResponse updateFollowupTask(Long id, UpdateFollowupTaskRequest request);

  FollowupTaskResponse getFollowupTaskById(Long id);

  FollowupTaskResponse getFollowupTaskByCampaignAndTrainee(Long campaignId, Long traineeId);

  List<FollowupTaskResponse> getFollowupTasksByCampaign(Long campaignId);

  Page<FollowupTaskResponse> getFollowupTasksByCampaign(Long campaignId, Pageable pageable);

  List<FollowupTaskResponse> getFollowupTasksByTrainee(Long traineeId);

  Page<FollowupTaskResponse> getFollowupTasksByTrainee(Long traineeId, Pageable pageable);

  List<FollowupTaskResponse> getFollowupTasksByEnrollment(Long enrollmentId);

  Page<FollowupTaskResponse> getFollowupTasksByEnrollment(Long enrollmentId, Pageable pageable);

  List<FollowupTaskResponse> getFollowupTasksByPlacement(Long placementId);

  Page<FollowupTaskResponse> getFollowupTasksByPlacement(Long placementId, Pageable pageable);

  List<FollowupTaskResponse> getFollowupTasksByEmployment(Long employmentId);

  Page<FollowupTaskResponse> getFollowupTasksByEmployment(Long employmentId, Pageable pageable);

  List<FollowupTaskResponse> getFollowupTasksBySurvey(Long surveyId);

  Page<FollowupTaskResponse> getFollowupTasksBySurvey(Long surveyId, Pageable pageable);

  List<FollowupTaskResponse> getFollowupTasksByStatus(Long statusId);

  Page<FollowupTaskResponse> getFollowupTasksByStatus(Long statusId, Pageable pageable);

  List<FollowupTaskResponse> getFollowupTasksByOutcome(Long outcomeId);

  Page<FollowupTaskResponse> getFollowupTasksByOutcome(Long outcomeId, Pageable pageable);

  List<FollowupTaskResponse> getFollowupTasksByAssignedUser(Long assignedUserId);

  Page<FollowupTaskResponse> getFollowupTasksByAssignedUser(Long assignedUserId, Pageable pageable);

  List<FollowupTaskResponse> getFollowupTasksByScheduledDate(LocalDate scheduledDate);

  Page<FollowupTaskResponse> getFollowupTasksByScheduledDate(LocalDate scheduledDate, Pageable pageable);

  void deleteFollowupTask(Long id);
}
