package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.AssessmentResult;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssessmentResultRepository extends JpaRepository<AssessmentResult, Long> {

  Optional<AssessmentResult> findByTraineeAssessmentId(Long traineeAssessmentId);

  boolean existsByTraineeAssessmentId(Long traineeAssessmentId);

  List<AssessmentResult> findByTraineeId(Long traineeId);

  List<AssessmentResult> findByAssessmentOutcomeId(Long assessmentOutcomeId);
}
