package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.RefBatchStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefBatchStatusRepository extends JpaRepository<RefBatchStatus, Long> {

  Optional<RefBatchStatus> findByStatusCode(String statusCode);

  boolean existsByStatusCode(String statusCode);
}
