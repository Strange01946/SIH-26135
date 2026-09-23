package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.ProviderOutcomeSummaryResponse;
import java.util.List;

public interface ProviderOutcomeSummaryService {

  List<ProviderOutcomeSummaryResponse> getAllProviderOutcomes();

  List<ProviderOutcomeSummaryResponse> getAllProviderOutcomesOrderByEnrollmentCountDesc();

  ProviderOutcomeSummaryResponse getProviderOutcomeByProviderId(Long providerId);
}
