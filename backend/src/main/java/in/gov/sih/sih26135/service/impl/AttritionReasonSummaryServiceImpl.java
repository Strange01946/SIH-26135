package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.AttritionReasonSummaryResponse;
import in.gov.sih.sih26135.entity.analytics.AttritionReasonSummary;
import in.gov.sih.sih26135.entity.analytics.AttritionReasonSummaryId;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.AttritionReasonSummaryMapper;
import in.gov.sih.sih26135.repository.analytics.AttritionReasonSummaryRepository;
import in.gov.sih.sih26135.service.AttritionReasonSummaryService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AttritionReasonSummaryServiceImpl implements AttritionReasonSummaryService {

  private final AttritionReasonSummaryRepository repository;
  private final AttritionReasonSummaryMapper mapper;

  public AttritionReasonSummaryServiceImpl(
      AttritionReasonSummaryRepository repository,
      AttritionReasonSummaryMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public List<AttritionReasonSummaryResponse> getAllAttritionReasonSummaries() {
    return repository.findAll().stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<AttritionReasonSummaryResponse> getAllAttritionReasonSummariesOrderByExitCountDesc() {
    return repository.findAllByOrderByExitCountDesc().stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public AttritionReasonSummaryResponse getAttritionReasonSummaryById(Long employmentExitReasonId, Long separationNatureId) {
    if (employmentExitReasonId == null) {
      throw new BadRequestException("Employment exit reason ID is required");
    }
    if (separationNatureId == null) {
      throw new BadRequestException("Separation nature ID is required");
    }
    AttritionReasonSummaryId id = new AttritionReasonSummaryId(employmentExitReasonId, separationNatureId);
    AttritionReasonSummary entity = repository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("AttritionReasonSummary", "id"));
    return mapper.toResponse(entity);
  }

  @Override
  public List<AttritionReasonSummaryResponse> getAttritionReasonSummariesByExitReasonId(Long employmentExitReasonId) {
    if (employmentExitReasonId == null) {
      throw new BadRequestException("Employment exit reason ID is required");
    }
    return repository.findByEmploymentExitReasonId(employmentExitReasonId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<AttritionReasonSummaryResponse> getAttritionReasonSummariesBySeparationNatureId(Long separationNatureId) {
    if (separationNatureId == null) {
      throw new BadRequestException("Separation nature ID is required");
    }
    return repository.findBySeparationNatureId(separationNatureId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<AttritionReasonSummaryResponse> getAttritionReasonSummariesByIsVoluntaryFlag(Boolean isVoluntaryFlag) {
    if (isVoluntaryFlag == null) {
      throw new BadRequestException("isVoluntaryFlag is required");
    }
    return repository.findByIsVoluntaryFlag(isVoluntaryFlag).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<AttritionReasonSummaryResponse> getAttritionReasonSummariesByIsInvoluntaryFlag(Boolean isInvoluntaryFlag) {
    if (isInvoluntaryFlag == null) {
      throw new BadRequestException("isInvoluntaryFlag is required");
    }
    return repository.findByIsInvoluntaryFlag(isInvoluntaryFlag).stream()
        .map(mapper::toResponse)
        .toList();
  }
}
