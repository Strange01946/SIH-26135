package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.TrainingBatch;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrainingBatchRepository extends JpaRepository<TrainingBatch, Long> {

  Optional<TrainingBatch> findByBatchCode(String batchCode);

  boolean existsByBatchCode(String batchCode);

  List<TrainingBatch> findByCourseId(Long courseId);

  List<TrainingBatch> findByTrainingProviderId(Long providerId);

  List<TrainingBatch> findByTrainingCenterId(Long centerId);

  List<TrainingBatch> findByProgramId(Long programId);

  List<TrainingBatch> findByBatchStatusId(Long batchStatusId);
}
