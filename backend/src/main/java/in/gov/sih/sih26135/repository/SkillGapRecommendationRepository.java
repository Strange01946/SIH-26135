package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.SkillGapRecommendation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SkillGapRecommendationRepository extends
    JpaRepository<SkillGapRecommendation, Long> {

  Optional<SkillGapRecommendation> findBySkillGapIdAndRecommendationNumber(Long skillGapId,
      Integer recommendationNumber);

  boolean existsBySkillGapIdAndRecommendationNumber(Long skillGapId, Integer recommendationNumber);

  Optional<SkillGapRecommendation> findBySkillGapIdAndSkillGapActionTypeIdAndRecommendedCourseId(
      Long skillGapId, Long actionTypeId, Long courseId);

  List<SkillGapRecommendation> findBySkillGapId(Long skillGapId);

  List<SkillGapRecommendation> findBySkillGapActionTypeId(Long actionTypeId);

  List<SkillGapRecommendation> findByRecommendedCourseId(Long courseId);

  List<SkillGapRecommendation> findByRecommendedSkillId(Long skillId);

  List<SkillGapRecommendation> findBySkillGapIdAndIsAcceptedFlagTrue(Long skillGapId);
}
