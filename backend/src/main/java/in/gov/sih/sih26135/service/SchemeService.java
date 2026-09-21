package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateSchemeRequest;
import in.gov.sih.sih26135.dto.request.UpdateSchemeRequest;
import in.gov.sih.sih26135.dto.response.SchemeResponse;
import java.util.List;

public interface SchemeService {

  SchemeResponse getById(Long id);

  SchemeResponse getByCode(String schemeCode);

  List<SchemeResponse> getAllSchemes();

  List<SchemeResponse> getSchemesByDepartmentId(Long departmentId);

  SchemeResponse createScheme(CreateSchemeRequest request);

  SchemeResponse updateScheme(Long id, UpdateSchemeRequest request);

  void deleteScheme(Long id);
}
