package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.ImportRecordStatusResponse;
import java.util.List;

public interface ImportRecordStatusReferenceService {

  List<ImportRecordStatusResponse> getAllStatuses();

  ImportRecordStatusResponse getById(Long id);

  ImportRecordStatusResponse getByCode(String statusCode);
}
