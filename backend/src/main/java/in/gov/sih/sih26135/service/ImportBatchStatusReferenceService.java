package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.ImportBatchStatusResponse;
import java.util.List;

public interface ImportBatchStatusReferenceService {

  List<ImportBatchStatusResponse> getAllStatuses();

  ImportBatchStatusResponse getById(Long id);

  ImportBatchStatusResponse getByCode(String statusCode);
}
