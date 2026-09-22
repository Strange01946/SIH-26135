package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.CompanySizeResponse;
import java.util.List;

public interface CompanySizeReferenceService {

  List<CompanySizeResponse> getAllCompanySizes();

  CompanySizeResponse getById(Long id);

  CompanySizeResponse getByCode(String sizeCode);
}
