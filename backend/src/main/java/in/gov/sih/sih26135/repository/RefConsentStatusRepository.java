package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.RefConsentStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefConsentStatusRepository extends JpaRepository<RefConsentStatus, Long> {

  Optional<RefConsentStatus> findByStatusCode(String statusCode);

  boolean existsByStatusCode(String statusCode);
}
