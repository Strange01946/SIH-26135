package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.SkillGapActionTypeResponse;
import in.gov.sih.sih26135.entity.RefSkillGapActionType;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.SkillGapActionTypeMapper;
import in.gov.sih.sih26135.repository.RefSkillGapActionTypeRepository;
import in.gov.sih.sih26135.service.SkillGapActionTypeReferenceService;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SkillGapActionTypeReferenceServiceImpl implements SkillGapActionTypeReferenceService {

  private final RefSkillGapActionTypeRepository repository;
  private final SkillGapActionTypeMapper mapper;

  public SkillGapActionTypeReferenceServiceImpl(
      RefSkillGapActionTypeRepository repository,
      SkillGapActionTypeMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public List<SkillGapActionTypeResponse> getAllActionTypes() {
    return repository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder")).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public SkillGapActionTypeResponse getById(Long id) {
    if (id == null) {
      throw new BadRequestException("Skill gap action type ID is required");
    }
    RefSkillGapActionType entity = repository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("RefSkillGapActionType", "id"));
    return mapper.toResponse(entity);
  }

  @Override
  public SkillGapActionTypeResponse getByCode(String actionCode) {
    if (actionCode == null || actionCode.isBlank()) {
      throw new BadRequestException("Action code is required");
    }
    RefSkillGapActionType entity = repository.findByActionCode(actionCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("RefSkillGapActionType", "actionCode"));
    return mapper.toResponse(entity);
  }
}
