package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.ProviderOutcomeSummaryResponse;
import in.gov.sih.sih26135.entity.analytics.ProviderOutcomeSummary;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.ProviderOutcomeSummaryMapper;
import in.gov.sih.sih26135.repository.analytics.ProviderOutcomeSummaryRepository;
import in.gov.sih.sih26135.service.ProviderOutcomeSummaryService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProviderOutcomeSummaryServiceImpl implements ProviderOutcomeSummaryService {

  private final ProviderOutcomeSummaryRepository repository;
  private final ProviderOutcomeSummaryMapper mapper;

  public ProviderOutcomeSummaryServiceImpl(
      ProviderOutcomeSummaryRepository repository,
      ProviderOutcomeSummaryMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public List<ProviderOutcomeSummaryResponse> getAllProviderOutcomes() {
    return repository.findAll().stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<ProviderOutcomeSummaryResponse> getAllProviderOutcomesOrderByEnrollmentCountDesc() {
    return repository.findAllByOrderByEnrollmentCountDesc().stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public ProviderOutcomeSummaryResponse getProviderOutcomeByProviderId(Long providerId) {
    if (providerId == null) {
      throw new BadRequestException("Provider ID is required");
    }
    ProviderOutcomeSummary entity = repository.findById(providerId)
        .orElseThrow(() -> new ResourceNotFoundException("ProviderOutcomeSummary", "providerId"));
    return mapper.toResponse(entity);
  }
}
