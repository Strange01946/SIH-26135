package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.RefImportBatchStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefImportBatchStatusRepository extends
    JpaRepository<RefImportBatchStatus, Long> {

  Optional<RefImportBatchStatus> findByStatusCode(String statusCode);

  boolean existsByStatusCode(String statusCode);
}
