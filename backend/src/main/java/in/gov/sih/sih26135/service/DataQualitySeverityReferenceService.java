package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.DataQualitySeverityResponse;
import java.util.List;

public interface DataQualitySeverityReferenceService {

  List<DataQualitySeverityResponse> getAllSeverities();

  DataQualitySeverityResponse getById(Long id);

  DataQualitySeverityResponse getByCode(String severityCode);
}
