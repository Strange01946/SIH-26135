package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.ImportRecord;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImportRecordRepository extends JpaRepository<ImportRecord, Long> {

  Optional<ImportRecord> findByImportBatchIdAndSourceRowNumber(Long importBatchId,
      Integer sourceRowNumber);

  boolean existsByImportBatchIdAndSourceRowNumber(Long importBatchId, Integer sourceRowNumber);

  boolean existsByImportBatchId(Long importBatchId);

  List<ImportRecord> findByImportBatchId(Long importBatchId);

  Page<ImportRecord> findByImportBatchId(Long importBatchId, Pageable pageable);

  Page<ImportRecord> findByImportBatchIdAndImportRecordStatusStatusCodeIn(
      Long importBatchId,
      Collection<String> statusCodes,
      Pageable pageable);

  List<ImportRecord> findByImportRecordStatusId(Long statusId);

  Page<ImportRecord> findByImportRecordStatusId(Long statusId, Pageable pageable);

  List<ImportRecord> findByEntityTypeAndEntityId(String entityType, Long entityId);

  Page<ImportRecord> findByEntityTypeAndEntityId(String entityType, Long entityId, Pageable pageable);
}
