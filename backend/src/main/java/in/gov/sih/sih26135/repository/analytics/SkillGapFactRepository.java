package in.gov.sih.sih26135.repository.analytics;

import in.gov.sih.sih26135.entity.analytics.SkillGapFact;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SkillGapFactRepository extends JpaRepository<SkillGapFact, Long> {

  Optional<SkillGapFact> findBySkillGapNumber(String skillGapNumber);

  List<SkillGapFact> findByTraineeId(Long traineeId);

  List<SkillGapFact> findBySkillId(Long skillId);

  List<SkillGapFact> findByCourseId(Long courseId);

  List<SkillGapFact> findByJobRoleId(Long jobRoleId);

  List<SkillGapFact> findBySkillGapSeverityId(Long severityId);

  List<SkillGapFact> findBySkillGapStatusId(Long statusId);

  List<SkillGapFact> findByIsCurrentTrue();

  List<SkillGapFact> findByTraineeDistrictId(Long traineeDistrictId);
}
