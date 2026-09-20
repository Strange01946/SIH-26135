package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.RefEmploymentExitReason;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefEmploymentExitReasonRepository extends JpaRepository<RefEmploymentExitReason, Long> {

  Optional<RefEmploymentExitReason> findByReasonCode(String reasonCode);

  boolean existsByReasonCode(String reasonCode);
}
