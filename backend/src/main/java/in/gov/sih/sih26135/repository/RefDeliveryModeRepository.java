package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.RefDeliveryMode;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefDeliveryModeRepository extends JpaRepository<RefDeliveryMode, Long> {

  Optional<RefDeliveryMode> findByModeCode(String modeCode);

  boolean existsByModeCode(String modeCode);
}
