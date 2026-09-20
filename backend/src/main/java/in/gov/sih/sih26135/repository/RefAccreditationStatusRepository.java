package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.RefAccreditationStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefAccreditationStatusRepository extends JpaRepository<RefAccreditationStatus, Long> {

  Optional<RefAccreditationStatus> findByStatusCode(String statusCode);

  boolean existsByStatusCode(String statusCode);
}
