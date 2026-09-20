package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.RefEmploymentVerificationRequestStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefEmploymentVerificationRequestStatusRepository extends
    JpaRepository<RefEmploymentVerificationRequestStatus, Long> {

  Optional<RefEmploymentVerificationRequestStatus> findByStatusCode(String statusCode);

  boolean existsByStatusCode(String statusCode);
}
