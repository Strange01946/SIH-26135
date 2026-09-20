package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.RefEmploymentVerificationAttemptStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefEmploymentVerificationAttemptStatusRepository extends
    JpaRepository<RefEmploymentVerificationAttemptStatus, Long> {

  Optional<RefEmploymentVerificationAttemptStatus> findByStatusCode(String statusCode);

  boolean existsByStatusCode(String statusCode);
}
