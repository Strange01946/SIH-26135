package in.gov.sih.sih26135.repository.analytics;

import in.gov.sih.sih26135.entity.analytics.EnrollmentOutcomeFact;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EnrollmentOutcomeFactRepository extends
    JpaRepository<EnrollmentOutcomeFact, Long> {

  Optional<EnrollmentOutcomeFact> findByEnrollmentNumber(String enrollmentNumber);

  List<EnrollmentOutcomeFact> findByTraineeId(Long traineeId);

  List<EnrollmentOutcomeFact> findByProgramId(Long programId);

  List<EnrollmentOutcomeFact> findByCourseId(Long courseId);

  List<EnrollmentOutcomeFact> findByProviderId(Long providerId);

  List<EnrollmentOutcomeFact> findByBatchId(Long batchId);

  List<EnrollmentOutcomeFact> findByTraineeDistrictId(Long traineeDistrictId);

  List<EnrollmentOutcomeFact> findByTraineeStateId(Long traineeStateId);
}
