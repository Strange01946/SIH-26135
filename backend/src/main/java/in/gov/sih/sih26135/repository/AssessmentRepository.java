package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.Assessment;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssessmentRepository extends JpaRepository<Assessment, Long> {

  Optional<Assessment> findByAssessmentCode(String assessmentCode);

  boolean existsByAssessmentCode(String assessmentCode);

  List<Assessment> findByCourseId(Long courseId);

  List<Assessment> findByTrainingBatchId(Long batchId);

  List<Assessment> findByProgramId(Long programId);

  List<Assessment> findByAssessmentTypeId(Long assessmentTypeId);

  List<Assessment> findByAssessmentDate(LocalDate assessmentDate);

  List<Assessment> findByLifecycleStatusId(Long lifecycleStatusId);
}
