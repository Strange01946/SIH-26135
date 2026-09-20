package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.RefUnemploymentReason;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefUnemploymentReasonRepository extends JpaRepository<RefUnemploymentReason, Long> {

  Optional<RefUnemploymentReason> findByReasonCode(String reasonCode);

  boolean existsByReasonCode(String reasonCode);
}
