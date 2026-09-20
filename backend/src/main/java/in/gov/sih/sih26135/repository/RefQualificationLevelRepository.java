package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.RefQualificationLevel;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefQualificationLevelRepository extends JpaRepository<RefQualificationLevel, Long> {

  Optional<RefQualificationLevel> findByLevelCode(String levelCode);

  boolean existsByLevelCode(String levelCode);
}
