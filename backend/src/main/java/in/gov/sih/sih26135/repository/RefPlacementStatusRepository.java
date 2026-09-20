package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.RefPlacementStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefPlacementStatusRepository extends JpaRepository<RefPlacementStatus, Long> {

  Optional<RefPlacementStatus> findByStatusCode(String statusCode);

  boolean existsByStatusCode(String statusCode);

  List<RefPlacementStatus> findByIsOfferFlag(Boolean isOfferFlag);

  List<RefPlacementStatus> findByIsJoinedFlag(Boolean isJoinedFlag);

  List<RefPlacementStatus> findByIsUnsuccessfulFlag(Boolean isUnsuccessfulFlag);
}
