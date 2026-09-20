package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.SkillLevel;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SkillLevelRepository extends JpaRepository<SkillLevel, Long> {

  Optional<SkillLevel> findByLevelCode(String levelCode);

  boolean existsByLevelCode(String levelCode);

  Optional<SkillLevel> findByLevelRank(Integer levelRank);

  boolean existsByLevelRank(Integer levelRank);

  List<SkillLevel> findAllByOrderBySortOrderAsc();
}
