package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.SeparationNatureResponse;
import java.util.List;

public interface SeparationNatureReferenceService {

  List<SeparationNatureResponse> getAllNatures();

  SeparationNatureResponse getById(Long id);

  SeparationNatureResponse getByCode(String natureCode);
}
