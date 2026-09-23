package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.DistrictOutcomeSummaryResponse;
import java.util.List;

public interface DistrictOutcomeSummaryService {

  List<DistrictOutcomeSummaryResponse> getAllDistrictOutcomes();

  List<DistrictOutcomeSummaryResponse> getAllDistrictOutcomesOrderByEnrollmentCountDesc();

  DistrictOutcomeSummaryResponse getDistrictOutcomeByDistrictId(Long districtId);
}
