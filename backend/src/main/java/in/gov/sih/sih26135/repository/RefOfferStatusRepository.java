package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.RefOfferStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefOfferStatusRepository extends JpaRepository<RefOfferStatus, Long> {

  Optional<RefOfferStatus> findByStatusCode(String statusCode);

  boolean existsByStatusCode(String statusCode);
}
