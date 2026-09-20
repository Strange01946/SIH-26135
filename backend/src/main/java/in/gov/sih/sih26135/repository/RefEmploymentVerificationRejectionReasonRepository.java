package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.RefEmploymentVerificationRejectionReason;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefEmploymentVerificationRejectionReasonRepository extends
    JpaRepository<RefEmploymentVerificationRejectionReason, Long> {

  Optional<RefEmploymentVerificationRejectionReason> findByReasonCode(String reasonCode);

  boolean existsByReasonCode(String reasonCode);
}
