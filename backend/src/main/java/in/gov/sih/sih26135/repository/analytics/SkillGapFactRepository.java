package in.gov.sih.sih26135.repository.analytics;

import in.gov.sih.sih26135.entity.analytics.SkillGapFact;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SkillGapFactRepository extends JpaRepository<SkillGapFact, Long> {

  Optional<SkillGapFact> findBySkillGapNumber(String skillGapNumber);

  List<SkillGapFact> findByTraineeId(Long traineeId);

  Page<SkillGapFact> findByTraineeId(Long traineeId, Pageable pageable);

  List<SkillGapFact> findBySkillId(Long skillId);

  Page<SkillGapFact> findBySkillId(Long skillId, Pageable pageable);

  List<SkillGapFact> findByCourseId(Long courseId);

  Page<SkillGapFact> findByCourseId(Long courseId, Pageable pageable);

  List<SkillGapFact> findByJobRoleId(Long jobRoleId);

  Page<SkillGapFact> findByJobRoleId(Long jobRoleId, Pageable pageable);

  List<SkillGapFact> findBySkillGapSeverityId(Long severityId);

  Page<SkillGapFact> findBySkillGapSeverityId(Long severityId, Pageable pageable);

  List<SkillGapFact> findBySkillGapStatusId(Long statusId);

  Page<SkillGapFact> findBySkillGapStatusId(Long statusId, Pageable pageable);

  List<SkillGapFact> findByIsCurrentTrue();

  Page<SkillGapFact> findByIsCurrentTrue(Pageable pageable);

  List<SkillGapFact> findByTraineeDistrictId(Long traineeDistrictId);

  Page<SkillGapFact> findByTraineeDistrictId(Long traineeDistrictId, Pageable pageable);
}
