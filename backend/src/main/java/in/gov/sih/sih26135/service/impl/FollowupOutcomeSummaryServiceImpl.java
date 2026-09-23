package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.FollowupOutcomeSummaryResponse;
import in.gov.sih.sih26135.entity.analytics.FollowupOutcomeSummary;
import in.gov.sih.sih26135.entity.analytics.FollowupOutcomeSummaryId;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.FollowupOutcomeSummaryMapper;
import in.gov.sih.sih26135.repository.analytics.FollowupOutcomeSummaryRepository;
import in.gov.sih.sih26135.service.FollowupOutcomeSummaryService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FollowupOutcomeSummaryServiceImpl implements FollowupOutcomeSummaryService {

  private final FollowupOutcomeSummaryRepository repository;
  private final FollowupOutcomeSummaryMapper mapper;

  public FollowupOutcomeSummaryServiceImpl(
      FollowupOutcomeSummaryRepository repository,
      FollowupOutcomeSummaryMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public List<FollowupOutcomeSummaryResponse> getAllFollowupOutcomeSummaries() {
    return repository.findAll().stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<FollowupOutcomeSummaryResponse> getAllFollowupOutcomeSummariesOrderByOffsetMonthsAsc() {
    return repository.findAllByOrderByFollowupOffsetMonthsAsc().stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public FollowupOutcomeSummaryResponse getFollowupOutcomeSummaryById(String followupTypeCode, Integer followupOffsetMonths) {
    if (followupTypeCode == null || followupTypeCode.isBlank()) {
      throw new BadRequestException("Followup type code is required");
    }
    if (followupOffsetMonths == null) {
      throw new BadRequestException("Followup offset months is required");
    }
    FollowupOutcomeSummaryId id = new FollowupOutcomeSummaryId(followupTypeCode.trim(), followupOffsetMonths);
    FollowupOutcomeSummary entity = repository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("FollowupOutcomeSummary", "id"));
    return mapper.toResponse(entity);
  }

  @Override
  public List<FollowupOutcomeSummaryResponse> getFollowupOutcomeSummariesByTypeCode(String followupTypeCode) {
    if (followupTypeCode == null || followupTypeCode.isBlank()) {
      throw new BadRequestException("Followup type code is required");
    }
    return repository.findByFollowupTypeCode(followupTypeCode.trim()).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<FollowupOutcomeSummaryResponse> getFollowupOutcomeSummariesByOffsetMonths(Integer followupOffsetMonths) {
    if (followupOffsetMonths == null) {
      throw new BadRequestException("Followup offset months is required");
    }
    return repository.findByFollowupOffsetMonths(followupOffsetMonths).stream()
        .map(mapper::toResponse)
        .toList();
  }
}
