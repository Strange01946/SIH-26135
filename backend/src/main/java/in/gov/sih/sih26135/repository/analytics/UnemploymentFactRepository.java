package in.gov.sih.sih26135.repository.analytics;

import in.gov.sih.sih26135.entity.analytics.UnemploymentFact;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UnemploymentFactRepository extends JpaRepository<UnemploymentFact, Long> {

  List<UnemploymentFact> findByTraineeId(Long traineeId);

  List<UnemploymentFact> findByUnemploymentReasonId(Long unemploymentReasonId);

  List<UnemploymentFact> findByLabourStatusId(Long labourStatusId);

  List<UnemploymentFact> findByIsCurrentTrue();

  List<UnemploymentFact> findByTraineeDistrictId(Long traineeDistrictId);
}
