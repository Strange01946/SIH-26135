package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.PlacementOffer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlacementOfferRepository extends JpaRepository<PlacementOffer, Long> {

  Optional<PlacementOffer> findByOfferNumber(String offerNumber);

  boolean existsByOfferNumber(String offerNumber);

  List<PlacementOffer> findByPlacementRecordId(Long placementId);

  List<PlacementOffer> findByEmployerId(Long employerId);

  List<PlacementOffer> findByJobRoleId(Long jobRoleId);

  List<PlacementOffer> findByOfferStatusId(Long offerStatusId);

  List<PlacementOffer> findByPlacementRecordIdAndOfferStatusId(Long placementId, Long offerStatusId);
}
