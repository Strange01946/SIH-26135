package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.RefTrainingDropoutReason;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefTrainingDropoutReasonRepository extends JpaRepository<RefTrainingDropoutReason, Long> {

  Optional<RefTrainingDropoutReason> findByReasonCode(String reasonCode);

  boolean existsByReasonCode(String reasonCode);

  List<RefTrainingDropoutReason> findAllByOrderBySortOrderAsc();
}
