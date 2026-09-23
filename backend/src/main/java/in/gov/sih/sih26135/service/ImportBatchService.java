package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateImportBatchRequest;
import in.gov.sih.sih26135.dto.request.UpdateImportBatchRequest;
import in.gov.sih.sih26135.dto.response.ImportBatchResponse;
import java.util.List;

public interface ImportBatchService {

  ImportBatchResponse createImportBatch(CreateImportBatchRequest request);

  ImportBatchResponse updateImportBatch(Long id, UpdateImportBatchRequest request);

  ImportBatchResponse getBatchById(Long id);

  ImportBatchResponse getBatchByCode(String batchCode);

  List<ImportBatchResponse> getBatchesByStatusId(Long statusId);

  List<ImportBatchResponse> getBatchesByEntityType(String entityType);

  List<ImportBatchResponse> getBatchesBySourceSystem(String sourceSystem);

  List<ImportBatchResponse> getBatchesByInitiatedByUserId(Long initiatedByUserId);

  void deleteImportBatch(Long id);
}
