package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.RefRecordVerificationStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefRecordVerificationStatusRepository extends JpaRepository<RefRecordVerificationStatus, Long> {

  Optional<RefRecordVerificationStatus> findByStatusCode(String statusCode);

  boolean existsByStatusCode(String statusCode);

  List<RefRecordVerificationStatus> findByIsVerifiedFlag(Boolean isVerifiedFlag);
}
