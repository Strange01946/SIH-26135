package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.SkillImportanceResponse;
import in.gov.sih.sih26135.entity.RefSkillImportance;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.SkillImportanceMapper;
import in.gov.sih.sih26135.repository.RefSkillImportanceRepository;
import in.gov.sih.sih26135.service.RefSkillImportanceReferenceService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RefSkillImportanceReferenceServiceImpl implements RefSkillImportanceReferenceService {

  private final RefSkillImportanceRepository refSkillImportanceRepository;
  private final SkillImportanceMapper skillImportanceMapper;

  public RefSkillImportanceReferenceServiceImpl(
      RefSkillImportanceRepository refSkillImportanceRepository,
      SkillImportanceMapper skillImportanceMapper) {
    this.refSkillImportanceRepository = refSkillImportanceRepository;
    this.skillImportanceMapper = skillImportanceMapper;
  }

  @Override
  public List<SkillImportanceResponse> getAllSkillImportances() {
    return refSkillImportanceRepository.findAllByOrderBySortOrderAsc().stream()
        .map(skillImportanceMapper::toResponse)
        .toList();
  }

  @Override
  public SkillImportanceResponse getById(Long id) {
    if (id == null) {
      throw new BadRequestException("Skill importance ID is required");
    }
    RefSkillImportance entity = refSkillImportanceRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("RefSkillImportance", "id"));
    return skillImportanceMapper.toResponse(entity);
  }

  @Override
  public SkillImportanceResponse getByCode(String importanceCode) {
    if (importanceCode == null || importanceCode.isBlank()) {
      throw new BadRequestException("Importance code is required");
    }
    RefSkillImportance entity = refSkillImportanceRepository.findByImportanceCode(importanceCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("RefSkillImportance", "importanceCode"));
    return skillImportanceMapper.toResponse(entity);
  }
}
