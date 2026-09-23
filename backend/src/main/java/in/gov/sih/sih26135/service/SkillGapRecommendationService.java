package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateSkillGapRecommendationRequest;
import in.gov.sih.sih26135.dto.request.UpdateSkillGapRecommendationRequest;
import in.gov.sih.sih26135.dto.response.SkillGapRecommendationResponse;
import java.util.List;

public interface SkillGapRecommendationService {

  SkillGapRecommendationResponse createRecommendation(CreateSkillGapRecommendationRequest request);

  SkillGapRecommendationResponse updateRecommendation(Long id, UpdateSkillGapRecommendationRequest request);

  SkillGapRecommendationResponse getRecommendationById(Long id);

  SkillGapRecommendationResponse getRecommendationBySkillGapAndNumber(Long skillGapId, Integer recommendationNumber);

  List<SkillGapRecommendationResponse> getRecommendationsBySkillGap(Long skillGapId);

  List<SkillGapRecommendationResponse> getAcceptedRecommendationsBySkillGap(Long skillGapId);

  List<SkillGapRecommendationResponse> getRecommendationsByActionType(Long actionTypeId);

  List<SkillGapRecommendationResponse> getRecommendationsByCourse(Long courseId);

  List<SkillGapRecommendationResponse> getRecommendationsBySkill(Long skillId);

  void deleteRecommendation(Long id);
}
