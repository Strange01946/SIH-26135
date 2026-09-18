package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.TraineeConsent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TraineeConsentRepository extends JpaRepository<TraineeConsent, Long> {

  List<TraineeConsent> findByTraineeId(Long traineeId);

  List<TraineeConsent> findByTraineeIdAndConsentTypeId(Long traineeId, Long consentTypeId);

  List<TraineeConsent> findByTraineeIdAndConsentStatusId(Long traineeId, Long consentStatusId);
}
