package in.gov.sih.sih26135.repository.analytics;

import in.gov.sih.sih26135.entity.analytics.UnemploymentFact;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UnemploymentFactRepository extends JpaRepository<UnemploymentFact, Long> {

  List<UnemploymentFact> findByTraineeId(Long traineeId);

  Page<UnemploymentFact> findByTraineeId(Long traineeId, Pageable pageable);

  List<UnemploymentFact> findByUnemploymentReasonId(Long unemploymentReasonId);

  Page<UnemploymentFact> findByUnemploymentReasonId(Long unemploymentReasonId, Pageable pageable);

  List<UnemploymentFact> findByLabourStatusId(Long labourStatusId);

  Page<UnemploymentFact> findByLabourStatusId(Long labourStatusId, Pageable pageable);

  List<UnemploymentFact> findByIsCurrentTrue();

  Page<UnemploymentFact> findByIsCurrentTrue(Pageable pageable);

  List<UnemploymentFact> findByTraineeDistrictId(Long traineeDistrictId);

  Page<UnemploymentFact> findByTraineeDistrictId(Long traineeDistrictId, Pageable pageable);
}
