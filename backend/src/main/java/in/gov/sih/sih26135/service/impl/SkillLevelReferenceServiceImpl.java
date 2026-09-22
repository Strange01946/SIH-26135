package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.SkillLevelResponse;
import in.gov.sih.sih26135.entity.SkillLevel;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.SkillLevelMapper;
import in.gov.sih.sih26135.repository.SkillLevelRepository;
import in.gov.sih.sih26135.service.SkillLevelReferenceService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SkillLevelReferenceServiceImpl implements SkillLevelReferenceService {

  private final SkillLevelRepository skillLevelRepository;
  private final SkillLevelMapper skillLevelMapper;

  public SkillLevelReferenceServiceImpl(
      SkillLevelRepository skillLevelRepository,
      SkillLevelMapper skillLevelMapper) {
    this.skillLevelRepository = skillLevelRepository;
    this.skillLevelMapper = skillLevelMapper;
  }

  @Override
  public List<SkillLevelResponse> getAllSkillLevels() {
    return skillLevelRepository.findAllByOrderBySortOrderAsc().stream()
        .map(skillLevelMapper::toResponse)
        .toList();
  }

  @Override
  public SkillLevelResponse getById(Long id) {
    if (id == null) {
      throw new BadRequestException("Skill level ID is required");
    }
    SkillLevel entity = skillLevelRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("SkillLevel", "id"));
    return skillLevelMapper.toResponse(entity);
  }

  @Override
  public SkillLevelResponse getByCode(String levelCode) {
    if (levelCode == null || levelCode.isBlank()) {
      throw new BadRequestException("Level code is required");
    }
    SkillLevel entity = skillLevelRepository.findByLevelCode(levelCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("SkillLevel", "levelCode"));
    return skillLevelMapper.toResponse(entity);
  }

  @Override
  public SkillLevelResponse getByRank(Integer levelRank) {
    if (levelRank == null) {
      throw new BadRequestException("Level rank is required");
    }
    SkillLevel entity = skillLevelRepository.findByLevelRank(levelRank)
        .orElseThrow(() -> new ResourceNotFoundException("SkillLevel", "levelRank"));
    return skillLevelMapper.toResponse(entity);
  }
}
