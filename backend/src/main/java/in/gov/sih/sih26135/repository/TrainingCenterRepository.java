package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.TrainingCenter;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrainingCenterRepository extends JpaRepository<TrainingCenter, Long> {

  Optional<TrainingCenter> findByCenterCode(String centerCode);

  boolean existsByCenterCode(String centerCode);

  List<TrainingCenter> findByTrainingProviderId(Long providerId);

  List<TrainingCenter> findByDistrictId(Long districtId);

  List<TrainingCenter> findByStateId(Long stateId);
}
