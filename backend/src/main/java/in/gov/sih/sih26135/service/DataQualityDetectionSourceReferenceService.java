package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.DataQualityDetectionSourceResponse;
import java.util.List;

public interface DataQualityDetectionSourceReferenceService {

  List<DataQualityDetectionSourceResponse> getAllSources();

  DataQualityDetectionSourceResponse getById(Long id);

  DataQualityDetectionSourceResponse getByCode(String sourceCode);
}
