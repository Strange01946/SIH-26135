package in.gov.sih.sih26135.repository.analytics;

import in.gov.sih.sih26135.entity.analytics.EnrollmentOutcomeFact;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EnrollmentOutcomeFactRepository extends
    JpaRepository<EnrollmentOutcomeFact, Long> {

  Optional<EnrollmentOutcomeFact> findByEnrollmentNumber(String enrollmentNumber);

  List<EnrollmentOutcomeFact> findByTraineeId(Long traineeId);

  Page<EnrollmentOutcomeFact> findByTraineeId(Long traineeId, Pageable pageable);

  List<EnrollmentOutcomeFact> findByProgramId(Long programId);

  Page<EnrollmentOutcomeFact> findByProgramId(Long programId, Pageable pageable);

  List<EnrollmentOutcomeFact> findByCourseId(Long courseId);

  Page<EnrollmentOutcomeFact> findByCourseId(Long courseId, Pageable pageable);

  List<EnrollmentOutcomeFact> findByProviderId(Long providerId);

  Page<EnrollmentOutcomeFact> findByProviderId(Long providerId, Pageable pageable);

  List<EnrollmentOutcomeFact> findByBatchId(Long batchId);

  Page<EnrollmentOutcomeFact> findByBatchId(Long batchId, Pageable pageable);

  List<EnrollmentOutcomeFact> findByTraineeDistrictId(Long traineeDistrictId);

  Page<EnrollmentOutcomeFact> findByTraineeDistrictId(Long traineeDistrictId, Pageable pageable);

  List<EnrollmentOutcomeFact> findByTraineeStateId(Long traineeStateId);

  Page<EnrollmentOutcomeFact> findByTraineeStateId(Long traineeStateId, Pageable pageable);
}
