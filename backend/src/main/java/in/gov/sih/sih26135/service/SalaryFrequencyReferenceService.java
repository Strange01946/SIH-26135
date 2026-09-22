package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.SalaryFrequencyResponse;
import java.util.List;

public interface SalaryFrequencyReferenceService {

  List<SalaryFrequencyResponse> getAllSalaryFrequencies();

  SalaryFrequencyResponse getById(Long id);

  SalaryFrequencyResponse getByCode(String frequencyCode);
}
