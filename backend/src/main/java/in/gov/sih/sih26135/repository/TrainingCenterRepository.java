package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.TrainingCenter;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TrainingCenterRepository extends JpaRepository<TrainingCenter, Long> {

  Optional<TrainingCenter> findByCenterCodeAndDeletedAtIsNull(String centerCode);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT c FROM TrainingCenter c WHERE c.id = :id")
  Optional<TrainingCenter> findByIdForUpdate(@Param("id") Long id);

  boolean existsByCenterCode(String centerCode);

  List<TrainingCenter> findByTrainingProviderId(Long providerId);

  List<TrainingCenter> findByDistrictId(Long districtId);

  List<TrainingCenter> findByStateId(Long stateId);

  Page<TrainingCenter> findByDeletedAtIsNull(Pageable pageable);

  Page<TrainingCenter> findByTrainingProviderIdAndDeletedAtIsNull(Long providerId, Pageable pageable);

  Page<TrainingCenter> findByDistrictIdAndDeletedAtIsNull(Long districtId, Pageable pageable);

  Page<TrainingCenter> findByStateIdAndDeletedAtIsNull(Long stateId, Pageable pageable);
}
