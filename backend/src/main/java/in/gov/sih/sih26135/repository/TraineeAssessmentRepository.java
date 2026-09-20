package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.TraineeAssessment;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TraineeAssessmentRepository extends JpaRepository<TraineeAssessment, Long> {

  Optional<TraineeAssessment> findByAssessmentIdAndTraineeIdAndAttemptNumber(
      Long assessmentId, Long traineeId, Integer attemptNumber);

  boolean existsByAssessmentIdAndTraineeIdAndAttemptNumber(
      Long assessmentId, Long traineeId, Integer attemptNumber);

  List<TraineeAssessment> findByAssessmentId(Long assessmentId);

  List<TraineeAssessment> findByTrainingEnrollmentId(Long enrollmentId);

  List<TraineeAssessment> findByTraineeId(Long traineeId);

  List<TraineeAssessment> findByAssessmentDate(LocalDate assessmentDate);
}
