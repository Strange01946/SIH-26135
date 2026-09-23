package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.SkillGapSeverityResponse;
import in.gov.sih.sih26135.entity.RefSkillGapSeverity;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.SkillGapSeverityMapper;
import in.gov.sih.sih26135.repository.RefSkillGapSeverityRepository;
import in.gov.sih.sih26135.service.SkillGapSeverityReferenceService;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SkillGapSeverityReferenceServiceImpl implements SkillGapSeverityReferenceService {

  private final RefSkillGapSeverityRepository repository;
  private final SkillGapSeverityMapper mapper;

  public SkillGapSeverityReferenceServiceImpl(
      RefSkillGapSeverityRepository repository,
      SkillGapSeverityMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public List<SkillGapSeverityResponse> getAllSeverities() {
    return repository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder")).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public SkillGapSeverityResponse getById(Long id) {
    if (id == null) {
      throw new BadRequestException("Skill gap severity ID is required");
    }
    RefSkillGapSeverity entity = repository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("RefSkillGapSeverity", "id"));
    return mapper.toResponse(entity);
  }

  @Override
  public SkillGapSeverityResponse getByCode(String severityCode) {
    if (severityCode == null || severityCode.isBlank()) {
      throw new BadRequestException("Severity code is required");
    }
    RefSkillGapSeverity entity = repository.findBySeverityCode(severityCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("RefSkillGapSeverity", "severityCode"));
    return mapper.toResponse(entity);
  }

  @Override
  public SkillGapSeverityResponse getByRank(Integer severityRank) {
    if (severityRank == null) {
      throw new BadRequestException("Severity rank is required");
    }
    RefSkillGapSeverity entity = repository.findBySeverityRank(severityRank)
        .orElseThrow(() -> new ResourceNotFoundException("RefSkillGapSeverity", "severityRank"));
    return mapper.toResponse(entity);
  }
}
