package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.CreateSkillRequest;
import in.gov.sih.sih26135.dto.request.UpdateSkillRequest;
import in.gov.sih.sih26135.dto.response.SkillResponse;
import in.gov.sih.sih26135.entity.Skill;
import in.gov.sih.sih26135.entity.SkillCategory;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.SkillMapper;
import in.gov.sih.sih26135.repository.SkillCategoryRepository;
import in.gov.sih.sih26135.repository.SkillRepository;
import in.gov.sih.sih26135.service.SkillService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SkillServiceImpl implements SkillService {

  private final SkillRepository skillRepository;
  private final SkillCategoryRepository skillCategoryRepository;
  private final SkillMapper skillMapper;

  public SkillServiceImpl(
      SkillRepository skillRepository,
      SkillCategoryRepository skillCategoryRepository,
      SkillMapper skillMapper) {
    this.skillRepository = skillRepository;
    this.skillCategoryRepository = skillCategoryRepository;
    this.skillMapper = skillMapper;
  }

  @Override
  public SkillResponse getById(Long id) {
    if (id == null) {
      throw new BadRequestException("Skill ID is required");
    }
    Skill skill = skillRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Skill", "id"));
    return skillMapper.toResponse(skill);
  }

  @Override
  public SkillResponse getByCode(String skillCode) {
    if (skillCode == null || skillCode.isBlank()) {
      throw new BadRequestException("Skill code is required");
    }
    Skill skill = skillRepository.findBySkillCode(skillCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("Skill", "skillCode"));
    return skillMapper.toResponse(skill);
  }

  @Override
  public List<SkillResponse> getAllSkills() {
    return skillRepository.findAll().stream()
        .map(skillMapper::toResponse)
        .toList();
  }

  @Override
  public List<SkillResponse> getSkillsByCategoryId(Long categoryId) {
    if (categoryId == null) {
      throw new BadRequestException("Category ID is required");
    }
    return skillRepository.findBySkillCategoryId(categoryId).stream()
        .map(skillMapper::toResponse)
        .toList();
  }

  @Override
  public List<SkillResponse> getSkillsByLifecycleStatusId(Long lifecycleStatusId) {
    if (lifecycleStatusId == null) {
      throw new BadRequestException("Lifecycle status ID is required");
    }
    return skillRepository.findByLifecycleStatusId(lifecycleStatusId).stream()
        .map(skillMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public SkillResponse createSkill(CreateSkillRequest request) {
    if (request == null) {
      throw new BadRequestException("Skill creation request cannot be null");
    }
    if (request.getSkillCode() == null || request.getSkillCode().isBlank()) {
      throw new BadRequestException("Skill code is required");
    }
    if (request.getSkillName() == null || request.getSkillName().isBlank()) {
      throw new BadRequestException("Skill name is required");
    }
    if (request.getSkillCategoryId() == null) {
      throw new BadRequestException("Skill category ID is required");
    }
    if (request.getLifecycleStatusId() == null) {
      throw new BadRequestException("Lifecycle status ID is required");
    }

    String skillCode = request.getSkillCode().trim();
    if (skillRepository.existsBySkillCode(skillCode)) {
      throw new ConflictException("Skill code already exists", "SKILL_CODE_ALREADY_EXISTS");
    }

    String skillName = request.getSkillName().trim();
    if (skillRepository.existsBySkillName(skillName)) {
      throw new ConflictException("Skill name already exists", "SKILL_NAME_ALREADY_EXISTS");
    }

    SkillCategory category = skillCategoryRepository.findById(request.getSkillCategoryId())
        .orElseThrow(() -> new ResourceNotFoundException("SkillCategory", "skillCategoryId"));

    Skill entity = skillMapper.toEntity(request, category);
    entity.setSkillCode(skillCode);
    entity.setSkillName(skillName);
    if (request.getDescription() != null) {
      entity.setDescription(request.getDescription().trim());
    }

    LocalDateTime now = LocalDateTime.now();
    entity.setCreatedAt(now);
    entity.setUpdatedAt(now);

    Skill saved = skillRepository.save(entity);
    return skillMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public SkillResponse updateSkill(Long id, UpdateSkillRequest request) {
    if (id == null) {
      throw new BadRequestException("Skill ID is required");
    }
    if (request == null) {
      throw new BadRequestException("Skill update request cannot be null");
    }

    Skill skill = skillRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Skill", "id"));

    if (request.getSkillName() != null && !request.getSkillName().isBlank()) {
      String newName = request.getSkillName().trim();
      if (!newName.equalsIgnoreCase(skill.getSkillName())
          && skillRepository.existsBySkillName(newName)) {
        throw new ConflictException("Skill name already exists", "SKILL_NAME_ALREADY_EXISTS");
      }
      skill.setSkillName(newName);
    }

    if (request.getDescription() != null) {
      skill.setDescription(request.getDescription().trim());
    }

    if (request.getSkillCategoryId() != null) {
      SkillCategory category = skillCategoryRepository.findById(request.getSkillCategoryId())
          .orElseThrow(() -> new ResourceNotFoundException("SkillCategory", "skillCategoryId"));
      skill.setSkillCategory(category);
    }

    if (request.getLifecycleStatusId() != null) {
      skill.setLifecycleStatusId(request.getLifecycleStatusId());
    }

    LocalDateTime now = LocalDateTime.now();
    skill.setUpdatedAt(now);

    Skill saved = skillRepository.save(skill);
    return skillMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public void deleteSkill(Long id) {
    if (id == null) {
      throw new BadRequestException("Skill ID is required");
    }
    Skill skill = skillRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Skill", "id"));

    LocalDateTime now = LocalDateTime.now();
    skill.setDeletedAt(now);
    skill.setUpdatedAt(now);
    skillRepository.save(skill);
  }
}
