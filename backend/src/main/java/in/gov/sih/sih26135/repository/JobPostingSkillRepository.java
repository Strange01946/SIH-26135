package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.JobPostingSkill;
import in.gov.sih.sih26135.entity.JobPostingSkillId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobPostingSkillRepository extends JpaRepository<JobPostingSkill, JobPostingSkillId> {

  List<JobPostingSkill> findByJobPostingId(Long jobPostingId);

  List<JobPostingSkill> findBySkillId(Long skillId);

  boolean existsByJobPostingIdAndSkillId(Long jobPostingId, Long skillId);

  List<JobPostingSkill> findByRequiredSkillLevelId(Long requiredSkillLevelId);

  List<JobPostingSkill> findBySkillImportanceId(Long skillImportanceId);
}
