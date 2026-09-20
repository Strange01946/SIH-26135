package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.RefNonSelectionReason;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefNonSelectionReasonRepository extends JpaRepository<RefNonSelectionReason, Long> {

  Optional<RefNonSelectionReason> findByReasonCode(String reasonCode);

  boolean existsByReasonCode(String reasonCode);
}
