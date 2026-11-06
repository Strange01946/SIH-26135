package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.FollowupFactResponse;
import in.gov.sih.sih26135.response.PagedResponse;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface FollowupFactService {

  PagedResponse<FollowupFactResponse> getAllFollowupFacts(Pageable pageable);

  List<FollowupFactResponse> getAllFollowupFacts();

  FollowupFactResponse getFollowupFactById(Long followupTaskId);

  List<FollowupFactResponse> getFollowupFactsByTraineeId(Long traineeId);

  PagedResponse<FollowupFactResponse> getFollowupFactsByTraineeId(Long traineeId, Pageable pageable);

  List<FollowupFactResponse> getFollowupFactsByCampaignId(Long followupCampaignId);

  PagedResponse<FollowupFactResponse> getFollowupFactsByCampaignId(Long followupCampaignId, Pageable pageable);

  List<FollowupFactResponse> getFollowupFactsByTypeId(Long followupTypeId);

  PagedResponse<FollowupFactResponse> getFollowupFactsByTypeId(Long followupTypeId, Pageable pageable);

  List<FollowupFactResponse> getFollowupFactsByTypeCode(String followupTypeCode);

  PagedResponse<FollowupFactResponse> getFollowupFactsByTypeCode(String followupTypeCode, Pageable pageable);

  List<FollowupFactResponse> getFollowupFactsByStatusId(Long followupStatusId);

  PagedResponse<FollowupFactResponse> getFollowupFactsByStatusId(Long followupStatusId, Pageable pageable);

  List<FollowupFactResponse> getFollowupFactsByOutcomeId(Long followupOutcomeId);

  PagedResponse<FollowupFactResponse> getFollowupFactsByOutcomeId(Long followupOutcomeId, Pageable pageable);

  List<FollowupFactResponse> getFollowupFactsByIsOpenFlag(Boolean isOpenFlag);

  PagedResponse<FollowupFactResponse> getFollowupFactsByIsOpenFlag(Boolean isOpenFlag, Pageable pageable);
}
