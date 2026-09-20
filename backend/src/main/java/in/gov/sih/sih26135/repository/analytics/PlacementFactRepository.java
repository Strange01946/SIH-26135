package in.gov.sih.sih26135.repository.analytics;

import in.gov.sih.sih26135.entity.analytics.PlacementFact;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlacementFactRepository extends JpaRepository<PlacementFact, Long> {

  Optional<PlacementFact> findByPlacementNumber(String placementNumber);

  List<PlacementFact> findByTraineeId(Long traineeId);

  List<PlacementFact> findByEnrollmentId(Long enrollmentId);

  List<PlacementFact> findByEmployerId(Long employerId);

  List<PlacementFact> findByCourseId(Long courseId);

  List<PlacementFact> findByProgramId(Long programId);

  List<PlacementFact> findByProviderId(Long providerId);

  List<PlacementFact> findByJobRoleId(Long jobRoleId);

  List<PlacementFact> findByTraineeDistrictId(Long traineeDistrictId);
}
