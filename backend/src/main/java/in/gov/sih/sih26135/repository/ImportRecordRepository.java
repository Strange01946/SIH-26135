package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.ImportRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImportRecordRepository extends JpaRepository<ImportRecord, Long> {

  Optional<ImportRecord> findByImportBatchIdAndSourceRowNumber(Long importBatchId,
      Integer sourceRowNumber);

  boolean existsByImportBatchIdAndSourceRowNumber(Long importBatchId, Integer sourceRowNumber);

  List<ImportRecord> findByImportBatchId(Long importBatchId);

  List<ImportRecord> findByImportRecordStatusId(Long statusId);

  List<ImportRecord> findByEntityTypeAndEntityId(String entityType, Long entityId);
}
