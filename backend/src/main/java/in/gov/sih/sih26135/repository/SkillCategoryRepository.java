package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.SkillCategory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SkillCategoryRepository extends JpaRepository<SkillCategory, Long> {

  Optional<SkillCategory> findByCategoryCode(String categoryCode);

  boolean existsByCategoryCode(String categoryCode);

  Optional<SkillCategory> findByCategoryName(String categoryName);

  boolean existsByCategoryName(String categoryName);

  List<SkillCategory> findByParentCategoryId(Long parentCategoryId);

  List<SkillCategory> findByLifecycleStatusId(Long lifecycleStatusId);
}
