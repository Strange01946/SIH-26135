package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.RefCommunicationDirection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefCommunicationDirectionRepository extends JpaRepository<RefCommunicationDirection, Long> {

  Optional<RefCommunicationDirection> findByDirectionCode(String directionCode);

  boolean existsByDirectionCode(String directionCode);
}
