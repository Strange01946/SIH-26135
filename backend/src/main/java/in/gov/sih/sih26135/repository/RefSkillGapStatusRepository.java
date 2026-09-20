package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.RefSkillGapStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefSkillGapStatusRepository extends JpaRepository<RefSkillGapStatus, Long> {

  Optional<RefSkillGapStatus> findByStatusCode(String statusCode);

  boolean existsByStatusCode(String statusCode);
}
