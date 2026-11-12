package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.ImportBatchProgressResponse;
import in.gov.sih.sih26135.dto.response.ImportBatchResponse;
import in.gov.sih.sih26135.dto.response.ImportRecordResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

/**
 * Orchestrates staged bulk data uploads, streaming parsing, schema/business validation,
 * row-level quarantine, and chunked transactional persistence for SIH-26135 imports.
 */
public interface ImportBatchProcessingWorkflow {

  /**
   * Validates, stages durably to server-controlled storage without buffering full file bytes in memory,
   * and registers an {@code ImportBatch} in {@code PENDING} status under the verified caller/tenant.
   */
  ImportBatchResponse stageUpload(MultipartFile file, String entityType, String sourceSystem);

  /**
   * Atomically claims a staged {@code PENDING} batch, streams parsed rows in bounded chunks,
   * isolates chunk failures per row, persists terminal row outcomes, and reconciles batch totals.
   */
  ImportBatchProgressResponse processBatch(Long batchId);

  /**
   * Retrieves reconciled batch status and row outcome progress for an authorized caller.
   */
  ImportBatchProgressResponse getBatchProgress(Long batchId);

  /**
   * Retrieves paginated import records for a batch, optionally filtered by status code.
   */
  Page<ImportRecordResponse> getBatchRecords(Long batchId, String statusCode, Pageable pageable);

  /**
   * Retrieves paginated quarantined import records ({@code FAILED}, and optionally {@code DUPLICATE}).
   */
  Page<ImportRecordResponse> getQuarantinedRecords(Long batchId, boolean includeDuplicates, Pageable pageable);
}
