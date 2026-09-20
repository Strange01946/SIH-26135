package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.Skill;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {

  Optional<Skill> findBySkillCode(String skillCode);

  boolean existsBySkillCode(String skillCode);

  Optional<Skill> findBySkillName(String skillName);

  boolean existsBySkillName(String skillName);

  List<Skill> findBySkillCategoryId(Long skillCategoryId);

  List<Skill> findByLifecycleStatusId(Long lifecycleStatusId);
}
