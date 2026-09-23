package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.SystemEventCategoryResponse;
import java.util.List;

public interface SystemEventCategoryReferenceService {

  List<SystemEventCategoryResponse> getAllCategories();

  SystemEventCategoryResponse getById(Long id);

  SystemEventCategoryResponse getByCode(String categoryCode);
}
