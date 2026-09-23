package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.FollowupFactResponse;
import java.util.List;

public interface FollowupFactService {

  List<FollowupFactResponse> getAllFollowupFacts();

  FollowupFactResponse getFollowupFactById(Long followupTaskId);

  List<FollowupFactResponse> getFollowupFactsByTraineeId(Long traineeId);

  List<FollowupFactResponse> getFollowupFactsByCampaignId(Long followupCampaignId);

  List<FollowupFactResponse> getFollowupFactsByTypeId(Long followupTypeId);

  List<FollowupFactResponse> getFollowupFactsByTypeCode(String followupTypeCode);

  List<FollowupFactResponse> getFollowupFactsByStatusId(Long followupStatusId);

  List<FollowupFactResponse> getFollowupFactsByOutcomeId(Long followupOutcomeId);

  List<FollowupFactResponse> getFollowupFactsByIsOpenFlag(Boolean isOpenFlag);
}
