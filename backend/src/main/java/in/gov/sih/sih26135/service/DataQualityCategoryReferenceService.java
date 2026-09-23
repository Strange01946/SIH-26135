package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.DataQualityCategoryResponse;
import java.util.List;

public interface DataQualityCategoryReferenceService {

  List<DataQualityCategoryResponse> getAllCategories();

  DataQualityCategoryResponse getById(Long id);

  DataQualityCategoryResponse getByCode(String categoryCode);
}
