package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.SkillGapSummaryResponse;
import in.gov.sih.sih26135.entity.analytics.SkillGapSummary;
import in.gov.sih.sih26135.entity.analytics.SkillGapSummaryId;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.SkillGapSummaryMapper;
import in.gov.sih.sih26135.repository.analytics.SkillGapSummaryRepository;
import in.gov.sih.sih26135.service.SkillGapSummaryService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SkillGapSummaryServiceImpl implements SkillGapSummaryService {

  private final SkillGapSummaryRepository repository;
  private final SkillGapSummaryMapper mapper;

  public SkillGapSummaryServiceImpl(
      SkillGapSummaryRepository repository,
      SkillGapSummaryMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public List<SkillGapSummaryResponse> getAllSkillGapSummaries() {
    return repository.findAll().stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SkillGapSummaryResponse> getAllSkillGapSummariesOrderByGapCountDesc() {
    return repository.findAllByOrderByGapCountDesc().stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public SkillGapSummaryResponse getSkillGapSummaryById(Long skillId, Long skillGapSeverityId) {
    if (skillId == null) {
      throw new BadRequestException("Skill ID is required");
    }
    if (skillGapSeverityId == null) {
      throw new BadRequestException("Skill gap severity ID is required");
    }
    SkillGapSummaryId id = new SkillGapSummaryId(skillId, skillGapSeverityId);
    SkillGapSummary entity = repository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("SkillGapSummary", "id"));
    return mapper.toResponse(entity);
  }

  @Override
  public List<SkillGapSummaryResponse> getSkillGapSummariesBySkillId(Long skillId) {
    if (skillId == null) {
      throw new BadRequestException("Skill ID is required");
    }
    return repository.findBySkillId(skillId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SkillGapSummaryResponse> getSkillGapSummariesBySeverityId(Long skillGapSeverityId) {
    if (skillGapSeverityId == null) {
      throw new BadRequestException("Skill gap severity ID is required");
    }
    return repository.findBySkillGapSeverityId(skillGapSeverityId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SkillGapSummaryResponse> getSkillGapSummariesBySeverityCode(String severityCode) {
    if (severityCode == null || severityCode.isBlank()) {
      throw new BadRequestException("Severity code is required");
    }
    return repository.findBySeverityCode(severityCode.trim()).stream()
        .map(mapper::toResponse)
        .toList();
  }
}
