package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.ImportBatch;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImportBatchRepository extends JpaRepository<ImportBatch, Long> {

  Optional<ImportBatch> findByBatchCode(String batchCode);

  boolean existsByBatchCode(String batchCode);

  List<ImportBatch> findByImportBatchStatusId(Long statusId);

  List<ImportBatch> findByEntityType(String entityType);

  List<ImportBatch> findBySourceSystem(String sourceSystem);

  List<ImportBatch> findByInitiatedByUserId(Long initiatedByUserId);
}
