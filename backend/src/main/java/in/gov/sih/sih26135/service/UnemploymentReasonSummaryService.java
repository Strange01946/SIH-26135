package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.UnemploymentReasonSummaryResponse;
import java.util.List;

public interface UnemploymentReasonSummaryService {

  List<UnemploymentReasonSummaryResponse> getAllUnemploymentReasonSummaries();

  List<UnemploymentReasonSummaryResponse> getAllUnemploymentReasonSummariesOrderByPeriodCountDesc();

  UnemploymentReasonSummaryResponse getUnemploymentReasonSummaryById(Long unemploymentReasonId);

  UnemploymentReasonSummaryResponse getUnemploymentReasonSummaryByCode(String unemploymentReasonCode);
}
