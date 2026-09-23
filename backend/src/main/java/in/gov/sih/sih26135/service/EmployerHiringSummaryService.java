package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.EmployerHiringSummaryResponse;
import java.util.List;

public interface EmployerHiringSummaryService {

  List<EmployerHiringSummaryResponse> getAllEmployerHiringSummaries();

  List<EmployerHiringSummaryResponse> getAllEmployerHiringSummariesOrderByJoinedCountDesc();

  List<EmployerHiringSummaryResponse> getAllEmployerHiringSummariesOrderByPlacementCountDesc();

  EmployerHiringSummaryResponse getEmployerHiringSummaryByEmployerId(Long employerId);
}
