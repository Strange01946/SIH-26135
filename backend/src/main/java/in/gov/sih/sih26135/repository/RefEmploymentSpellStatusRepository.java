package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.RefEmploymentSpellStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefEmploymentSpellStatusRepository extends JpaRepository<RefEmploymentSpellStatus, Long> {

  Optional<RefEmploymentSpellStatus> findByStatusCode(String statusCode);

  boolean existsByStatusCode(String statusCode);

  List<RefEmploymentSpellStatus> findByIsActiveFlag(Boolean isActiveFlag);
}
