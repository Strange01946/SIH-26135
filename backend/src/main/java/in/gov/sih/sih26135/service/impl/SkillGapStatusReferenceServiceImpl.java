package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.SkillGapStatusResponse;
import in.gov.sih.sih26135.entity.RefSkillGapStatus;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.SkillGapStatusMapper;
import in.gov.sih.sih26135.repository.RefSkillGapStatusRepository;
import in.gov.sih.sih26135.service.SkillGapStatusReferenceService;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SkillGapStatusReferenceServiceImpl implements SkillGapStatusReferenceService {

  private final RefSkillGapStatusRepository repository;
  private final SkillGapStatusMapper mapper;

  public SkillGapStatusReferenceServiceImpl(
      RefSkillGapStatusRepository repository,
      SkillGapStatusMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public List<SkillGapStatusResponse> getAllStatuses() {
    return repository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder")).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public SkillGapStatusResponse getById(Long id) {
    if (id == null) {
      throw new BadRequestException("Skill gap status ID is required");
    }
    RefSkillGapStatus entity = repository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("RefSkillGapStatus", "id"));
    return mapper.toResponse(entity);
  }

  @Override
  public SkillGapStatusResponse getByCode(String statusCode) {
    if (statusCode == null || statusCode.isBlank()) {
      throw new BadRequestException("Status code is required");
    }
    RefSkillGapStatus entity = repository.findByStatusCode(statusCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("RefSkillGapStatus", "statusCode"));
    return mapper.toResponse(entity);
  }
}
