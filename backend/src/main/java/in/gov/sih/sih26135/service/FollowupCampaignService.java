package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateFollowupCampaignRequest;
import in.gov.sih.sih26135.dto.request.UpdateFollowupCampaignRequest;
import in.gov.sih.sih26135.dto.response.FollowupCampaignResponse;
import java.time.LocalDate;
import java.util.List;

public interface FollowupCampaignService {

  FollowupCampaignResponse createFollowupCampaign(CreateFollowupCampaignRequest request);

  FollowupCampaignResponse updateFollowupCampaign(Long id, UpdateFollowupCampaignRequest request);

  FollowupCampaignResponse getFollowupCampaignById(Long id);

  FollowupCampaignResponse getFollowupCampaignByCode(String campaignCode);

  List<FollowupCampaignResponse> getAllActiveFollowupCampaigns();

  List<FollowupCampaignResponse> getFollowupCampaignsByFollowupType(Long followupTypeId);

  List<FollowupCampaignResponse> getFollowupCampaignsBySurvey(Long surveyId);

  List<FollowupCampaignResponse> getFollowupCampaignsByProgram(Long programId);

  List<FollowupCampaignResponse> getFollowupCampaignsByCourse(Long courseId);

  List<FollowupCampaignResponse> getFollowupCampaignsByLifecycleStatus(Long lifecycleStatusId);

  List<FollowupCampaignResponse> getFollowupCampaignsByScheduledStartDate(LocalDate scheduledStartDate);

  void deleteFollowupCampaign(Long id);
}
