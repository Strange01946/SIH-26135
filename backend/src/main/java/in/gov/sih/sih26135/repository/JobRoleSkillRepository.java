package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.JobRoleSkill;
import in.gov.sih.sih26135.entity.JobRoleSkillId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobRoleSkillRepository extends JpaRepository<JobRoleSkill, JobRoleSkillId> {

  List<JobRoleSkill> findByJobRoleId(Long jobRoleId);

  List<JobRoleSkill> findBySkillId(Long skillId);

  boolean existsByJobRoleIdAndSkillId(Long jobRoleId, Long skillId);

  List<JobRoleSkill> findByRequiredSkillLevelId(Long requiredSkillLevelId);

  List<JobRoleSkill> findBySkillImportanceId(Long skillImportanceId);

  List<JobRoleSkill> findBySkillIdAndRequiredSkillLevelId(Long skillId, Long requiredSkillLevelId);
}
