package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.RefImportRecordStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefImportRecordStatusRepository extends
    JpaRepository<RefImportRecordStatus, Long> {

  Optional<RefImportRecordStatus> findByStatusCode(String statusCode);

  boolean existsByStatusCode(String statusCode);
}
