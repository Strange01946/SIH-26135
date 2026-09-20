package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.RefApplicationStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefApplicationStatusRepository extends JpaRepository<RefApplicationStatus, Long> {

  Optional<RefApplicationStatus> findByStatusCode(String statusCode);

  boolean existsByStatusCode(String statusCode);
}
