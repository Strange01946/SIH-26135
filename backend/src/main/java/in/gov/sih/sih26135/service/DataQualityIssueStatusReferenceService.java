package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.DataQualityIssueStatusResponse;
import java.util.List;

public interface DataQualityIssueStatusReferenceService {

  List<DataQualityIssueStatusResponse> getAllStatuses();

  List<DataQualityIssueStatusResponse> getOpenStatuses();

  DataQualityIssueStatusResponse getById(Long id);

  DataQualityIssueStatusResponse getByCode(String statusCode);
}
