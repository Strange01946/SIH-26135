package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.UnemploymentReasonSummaryResponse;
import in.gov.sih.sih26135.entity.analytics.UnemploymentReasonSummary;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.UnemploymentReasonSummaryMapper;
import in.gov.sih.sih26135.repository.analytics.UnemploymentReasonSummaryRepository;
import in.gov.sih.sih26135.service.UnemploymentReasonSummaryService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UnemploymentReasonSummaryServiceImpl implements UnemploymentReasonSummaryService {

  private final UnemploymentReasonSummaryRepository repository;
  private final UnemploymentReasonSummaryMapper mapper;

  public UnemploymentReasonSummaryServiceImpl(
      UnemploymentReasonSummaryRepository repository,
      UnemploymentReasonSummaryMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public List<UnemploymentReasonSummaryResponse> getAllUnemploymentReasonSummaries() {
    return repository.findAll().stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<UnemploymentReasonSummaryResponse> getAllUnemploymentReasonSummariesOrderByPeriodCountDesc() {
    return repository.findAllByOrderByPeriodCountDesc().stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public UnemploymentReasonSummaryResponse getUnemploymentReasonSummaryById(Long unemploymentReasonId) {
    if (unemploymentReasonId == null) {
      throw new BadRequestException("Unemployment reason ID is required");
    }
    UnemploymentReasonSummary entity = repository.findById(unemploymentReasonId)
        .orElseThrow(() -> new ResourceNotFoundException("UnemploymentReasonSummary", "unemploymentReasonId"));
    return mapper.toResponse(entity);
  }

  @Override
  public UnemploymentReasonSummaryResponse getUnemploymentReasonSummaryByCode(String unemploymentReasonCode) {
    if (unemploymentReasonCode == null || unemploymentReasonCode.isBlank()) {
      throw new BadRequestException("Unemployment reason code is required");
    }
    UnemploymentReasonSummary entity = repository.findByUnemploymentReasonCode(unemploymentReasonCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("UnemploymentReasonSummary", "unemploymentReasonCode"));
    return mapper.toResponse(entity);
  }
}
