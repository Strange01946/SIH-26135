package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.SkillGapAssessmentStatusResponse;
import in.gov.sih.sih26135.entity.RefSkillGapAssessmentStatus;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.SkillGapAssessmentStatusMapper;
import in.gov.sih.sih26135.repository.RefSkillGapAssessmentStatusRepository;
import in.gov.sih.sih26135.service.SkillGapAssessmentStatusReferenceService;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SkillGapAssessmentStatusReferenceServiceImpl
    implements SkillGapAssessmentStatusReferenceService {

  private final RefSkillGapAssessmentStatusRepository repository;
  private final SkillGapAssessmentStatusMapper mapper;

  public SkillGapAssessmentStatusReferenceServiceImpl(
      RefSkillGapAssessmentStatusRepository repository,
      SkillGapAssessmentStatusMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public List<SkillGapAssessmentStatusResponse> getAllStatuses() {
    return repository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder")).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public SkillGapAssessmentStatusResponse getById(Long id) {
    if (id == null) {
      throw new BadRequestException("Skill gap assessment status ID is required");
    }
    RefSkillGapAssessmentStatus entity = repository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("RefSkillGapAssessmentStatus", "id"));
    return mapper.toResponse(entity);
  }

  @Override
  public SkillGapAssessmentStatusResponse getByCode(String statusCode) {
    if (statusCode == null || statusCode.isBlank()) {
      throw new BadRequestException("Status code is required");
    }
    RefSkillGapAssessmentStatus entity = repository.findByStatusCode(statusCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("RefSkillGapAssessmentStatus", "statusCode"));
    return mapper.toResponse(entity);
  }
}
