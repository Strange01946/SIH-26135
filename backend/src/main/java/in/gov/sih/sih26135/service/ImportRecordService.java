package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateImportRecordRequest;
import in.gov.sih.sih26135.dto.request.UpdateImportRecordRequest;
import in.gov.sih.sih26135.dto.response.ImportRecordResponse;
import java.util.List;

public interface ImportRecordService {

  ImportRecordResponse createImportRecord(CreateImportRecordRequest request);

  ImportRecordResponse updateImportRecord(Long id, UpdateImportRecordRequest request);

  ImportRecordResponse getRecordById(Long id);

  ImportRecordResponse getRecordByBatchIdAndSourceRowNumber(Long batchId, Integer sourceRowNumber);

  List<ImportRecordResponse> getRecordsByBatchId(Long batchId);

  List<ImportRecordResponse> getRecordsByStatusId(Long statusId);

  List<ImportRecordResponse> getRecordsByEntity(String entityType, Long entityId);

  void deleteImportRecord(Long id);
}
