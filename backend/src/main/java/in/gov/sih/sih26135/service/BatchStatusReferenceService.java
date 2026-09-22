package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.BatchStatusResponse;
import java.util.List;

public interface BatchStatusReferenceService {

  List<BatchStatusResponse> getAllBatchStatuses();

  BatchStatusResponse getById(Long id);

  BatchStatusResponse getByCode(String statusCode);
}
