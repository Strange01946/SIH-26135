package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.FollowupOutcomeSummaryResponse;
import java.util.List;

public interface FollowupOutcomeSummaryService {

  List<FollowupOutcomeSummaryResponse> getAllFollowupOutcomeSummaries();

  List<FollowupOutcomeSummaryResponse> getAllFollowupOutcomeSummariesOrderByOffsetMonthsAsc();

  FollowupOutcomeSummaryResponse getFollowupOutcomeSummaryById(String followupTypeCode, Integer followupOffsetMonths);

  List<FollowupOutcomeSummaryResponse> getFollowupOutcomeSummariesByTypeCode(String followupTypeCode);

  List<FollowupOutcomeSummaryResponse> getFollowupOutcomeSummariesByOffsetMonths(Integer followupOffsetMonths);
}
