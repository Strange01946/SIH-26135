package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.IndustryResponse;
import java.util.List;

public interface IndustryReferenceService {

  List<IndustryResponse> getAllIndustries();

  IndustryResponse getById(Long id);

  IndustryResponse getByCode(String industryCode);

  List<IndustryResponse> getIndustriesBySectorId(Long sectorId);
}
