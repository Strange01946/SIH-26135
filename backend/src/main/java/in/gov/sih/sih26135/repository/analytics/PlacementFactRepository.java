package in.gov.sih.sih26135.repository.analytics;

import in.gov.sih.sih26135.entity.analytics.PlacementFact;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlacementFactRepository extends JpaRepository<PlacementFact, Long> {

  Optional<PlacementFact> findByPlacementNumber(String placementNumber);

  List<PlacementFact> findByTraineeId(Long traineeId);

  Page<PlacementFact> findByTraineeId(Long traineeId, Pageable pageable);

  List<PlacementFact> findByEnrollmentId(Long enrollmentId);

  Page<PlacementFact> findByEnrollmentId(Long enrollmentId, Pageable pageable);

  List<PlacementFact> findByEmployerId(Long employerId);

  Page<PlacementFact> findByEmployerId(Long employerId, Pageable pageable);

  List<PlacementFact> findByCourseId(Long courseId);

  Page<PlacementFact> findByCourseId(Long courseId, Pageable pageable);

  List<PlacementFact> findByProgramId(Long programId);

  Page<PlacementFact> findByProgramId(Long programId, Pageable pageable);

  List<PlacementFact> findByProviderId(Long providerId);

  Page<PlacementFact> findByProviderId(Long providerId, Pageable pageable);

  List<PlacementFact> findByJobRoleId(Long jobRoleId);

  Page<PlacementFact> findByJobRoleId(Long jobRoleId, Pageable pageable);

  List<PlacementFact> findByTraineeDistrictId(Long traineeDistrictId);

  Page<PlacementFact> findByTraineeDistrictId(Long traineeDistrictId, Pageable pageable);
}
