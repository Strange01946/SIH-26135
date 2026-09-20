package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.RefSkillImportance;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefSkillImportanceRepository extends JpaRepository<RefSkillImportance, Long> {

  Optional<RefSkillImportance> findByImportanceCode(String importanceCode);

  boolean existsByImportanceCode(String importanceCode);

  List<RefSkillImportance> findAllByOrderBySortOrderAsc();
}
