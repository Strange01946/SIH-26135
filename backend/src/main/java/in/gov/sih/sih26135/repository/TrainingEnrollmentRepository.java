package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.TrainingEnrollment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrainingEnrollmentRepository extends JpaRepository<TrainingEnrollment, Long> {

  Optional<TrainingEnrollment> findByEnrollmentNumber(String enrollmentNumber);

  boolean existsByEnrollmentNumber(String enrollmentNumber);

  Optional<TrainingEnrollment> findByTraineeIdAndTrainingBatchId(Long traineeId, Long batchId);

  boolean existsByTraineeIdAndTrainingBatchId(Long traineeId, Long batchId);

  List<TrainingEnrollment> findByTraineeId(Long traineeId);

  List<TrainingEnrollment> findByTrainingBatchId(Long batchId);

  List<TrainingEnrollment> findByProgramId(Long programId);

  List<TrainingEnrollment> findByCourseId(Long courseId);

  List<TrainingEnrollment> findByTrainingProviderId(Long providerId);

  List<TrainingEnrollment> findByTrainingCenterId(Long centerId);

  List<TrainingEnrollment> findByEnrollmentStatusId(Long enrollmentStatusId);
}
