package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.RefSkillGapSource;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefSkillGapSourceRepository extends JpaRepository<RefSkillGapSource, Long> {

  Optional<RefSkillGapSource> findBySourceCode(String sourceCode);

  boolean existsBySourceCode(String sourceCode);
}
