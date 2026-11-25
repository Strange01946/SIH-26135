package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.TraineeConsent;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TraineeConsentRepository extends JpaRepository<TraineeConsent, Long> {

  @Query("select consent.trainee.id from TraineeConsent consent where consent.id = :id")
  Optional<Long> findOwnerTraineeId(@Param("id") Long id);

  List<TraineeConsent> findByTraineeId(Long traineeId);

  List<TraineeConsent> findByTraineeIdAndConsentTypeId(Long traineeId, Long consentTypeId);

  List<TraineeConsent> findByTraineeIdAndConsentStatusId(Long traineeId, Long consentStatusId);
}
