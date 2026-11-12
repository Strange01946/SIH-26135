package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateImportRecordRequest;
import in.gov.sih.sih26135.dto.request.UpdateImportRecordRequest;
import in.gov.sih.sih26135.dto.response.ImportRecordResponse;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ImportRecordService {

  ImportRecordResponse createImportRecord(CreateImportRecordRequest request);

  ImportRecordResponse updateImportRecord(Long id, UpdateImportRecordRequest request);

  ImportRecordResponse getRecordById(Long id);

  ImportRecordResponse getRecordByBatchIdAndSourceRowNumber(Long batchId, Integer sourceRowNumber);

  List<ImportRecordResponse> getRecordsByBatchId(Long batchId);

  Page<ImportRecordResponse> getRecordsByBatchId(Long batchId, Pageable pageable);

  List<ImportRecordResponse> getRecordsByStatusId(Long statusId);

  Page<ImportRecordResponse> getRecordsByStatusId(Long statusId, Pageable pageable);

  List<ImportRecordResponse> getRecordsByEntity(String entityType, Long entityId);

  Page<ImportRecordResponse> getRecordsByEntity(String entityType, Long entityId, Pageable pageable);

  void deleteImportRecord(Long id);
}
