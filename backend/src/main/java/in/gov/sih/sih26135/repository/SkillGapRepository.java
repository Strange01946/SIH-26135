package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.SkillGap;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SkillGapRepository extends JpaRepository<SkillGap, Long> {

  Optional<SkillGap> findBySkillGapNumber(String skillGapNumber);

  boolean existsBySkillGapNumber(String skillGapNumber);

  Optional<SkillGap> findBySkillGapAssessmentIdAndSkillId(Long skillGapAssessmentId, Long skillId);

  boolean existsBySkillGapAssessmentIdAndSkillId(Long skillGapAssessmentId, Long skillId);

  Optional<SkillGap> findByTraineeIdAndSkillIdAndCurrentGapKey(Long traineeId, Long skillId,
      Integer currentGapKey);

  List<SkillGap> findBySkillGapAssessmentId(Long skillGapAssessmentId);

  List<SkillGap> findByTraineeId(Long traineeId);

  List<SkillGap> findBySkillId(Long skillId);

  List<SkillGap> findBySkillGapSeverityId(Long severityId);

  List<SkillGap> findBySkillGapStatusId(Long statusId);

  List<SkillGap> findBySkillGapSourceId(Long sourceId);

  List<SkillGap> findByTraineeIdAndIsCurrentTrue(Long traineeId);

  List<SkillGap> findBySkillIdAndIsCurrentTrue(Long skillId);
}
