package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.CreateSkillCategoryRequest;
import in.gov.sih.sih26135.dto.request.UpdateSkillCategoryRequest;
import in.gov.sih.sih26135.dto.response.SkillCategoryResponse;
import in.gov.sih.sih26135.entity.SkillCategory;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.SkillCategoryMapper;
import in.gov.sih.sih26135.repository.SkillCategoryRepository;
import in.gov.sih.sih26135.service.SkillCategoryService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SkillCategoryServiceImpl implements SkillCategoryService {

  private final SkillCategoryRepository skillCategoryRepository;
  private final SkillCategoryMapper skillCategoryMapper;

  public SkillCategoryServiceImpl(
      SkillCategoryRepository skillCategoryRepository,
      SkillCategoryMapper skillCategoryMapper) {
    this.skillCategoryRepository = skillCategoryRepository;
    this.skillCategoryMapper = skillCategoryMapper;
  }

  @Override
  public SkillCategoryResponse getById(Long id) {
    if (id == null) {
      throw new BadRequestException("Category ID is required");
    }
    SkillCategory category = skillCategoryRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("SkillCategory", "id"));
    return skillCategoryMapper.toResponse(category);
  }

  @Override
  public SkillCategoryResponse getByCode(String categoryCode) {
    if (categoryCode == null || categoryCode.isBlank()) {
      throw new BadRequestException("Category code is required");
    }
    SkillCategory category = skillCategoryRepository.findByCategoryCode(categoryCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("SkillCategory", "categoryCode"));
    return skillCategoryMapper.toResponse(category);
  }

  @Override
  public List<SkillCategoryResponse> getAllSkillCategories() {
    return skillCategoryRepository.findAll().stream()
        .map(skillCategoryMapper::toResponse)
        .toList();
  }

  @Override
  public List<SkillCategoryResponse> getSkillCategoriesByParentId(Long parentCategoryId) {
    if (parentCategoryId == null) {
      throw new BadRequestException("Parent category ID is required");
    }
    return skillCategoryRepository.findByParentCategoryId(parentCategoryId).stream()
        .map(skillCategoryMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public SkillCategoryResponse createSkillCategory(CreateSkillCategoryRequest request) {
    if (request == null) {
      throw new BadRequestException("Skill category creation request cannot be null");
    }
    if (request.getCategoryCode() == null || request.getCategoryCode().isBlank()) {
      throw new BadRequestException("Category code is required");
    }
    if (request.getCategoryName() == null || request.getCategoryName().isBlank()) {
      throw new BadRequestException("Category name is required");
    }
    if (request.getLifecycleStatusId() == null) {
      throw new BadRequestException("Lifecycle status ID is required");
    }

    String categoryCode = request.getCategoryCode().trim();
    if (skillCategoryRepository.existsByCategoryCode(categoryCode)) {
      throw new ConflictException("Category code already exists", "CATEGORY_CODE_ALREADY_EXISTS");
    }

    String categoryName = request.getCategoryName().trim();
    if (skillCategoryRepository.existsByCategoryName(categoryName)) {
      throw new ConflictException("Category name already exists", "CATEGORY_NAME_ALREADY_EXISTS");
    }

    SkillCategory parentCategory = null;
    if (request.getParentCategoryId() != null) {
      parentCategory = skillCategoryRepository.findById(request.getParentCategoryId())
          .orElseThrow(() -> new ResourceNotFoundException("SkillCategory", "parentCategoryId"));
    }

    SkillCategory entity = skillCategoryMapper.toEntity(request, parentCategory);
    entity.setCategoryCode(categoryCode);
    entity.setCategoryName(categoryName);
    if (request.getDescription() != null) {
      entity.setDescription(request.getDescription().trim());
    }

    LocalDateTime now = LocalDateTime.now();
    entity.setCreatedAt(now);
    entity.setUpdatedAt(now);

    SkillCategory saved = skillCategoryRepository.save(entity);
    return skillCategoryMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public SkillCategoryResponse updateSkillCategory(Long id, UpdateSkillCategoryRequest request) {
    if (id == null) {
      throw new BadRequestException("Category ID is required");
    }
    if (request == null) {
      throw new BadRequestException("Skill category update request cannot be null");
    }

    SkillCategory category = skillCategoryRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("SkillCategory", "id"));

    if (request.getCategoryName() != null && !request.getCategoryName().isBlank()) {
      String newName = request.getCategoryName().trim();
      if (!newName.equalsIgnoreCase(category.getCategoryName())
          && skillCategoryRepository.existsByCategoryName(newName)) {
        throw new ConflictException("Category name already exists", "CATEGORY_NAME_ALREADY_EXISTS");
      }
      category.setCategoryName(newName);
    }

    if (request.getDescription() != null) {
      category.setDescription(request.getDescription().trim());
    }

    if (request.getLifecycleStatusId() != null) {
      category.setLifecycleStatusId(request.getLifecycleStatusId());
    }

    if (request.getParentCategoryId() != null) {
      Long parentId = request.getParentCategoryId();
      if (parentId.equals(id)) {
        throw new BadRequestException("Category cannot be its own parent", "INVALID_PARENT_CATEGORY");
      }

      SkillCategory parentCategory = skillCategoryRepository.findById(parentId)
          .orElseThrow(() -> new ResourceNotFoundException("SkillCategory", "parentCategoryId"));

      // Check circular hierarchy
      SkillCategory currentAncestor = parentCategory;
      while (currentAncestor != null) {
        if (currentAncestor.getId().equals(id)) {
          throw new BadRequestException("Cyclic category hierarchy detected", "CYCLIC_CATEGORY_HIERARCHY");
        }
        currentAncestor = currentAncestor.getParentCategory();
      }

      category.setParentCategory(parentCategory);
    }

    LocalDateTime now = LocalDateTime.now();
    category.setUpdatedAt(now);

    SkillCategory saved = skillCategoryRepository.save(category);
    return skillCategoryMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public void deleteSkillCategory(Long id) {
    if (id == null) {
      throw new BadRequestException("Category ID is required");
    }
    SkillCategory category = skillCategoryRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("SkillCategory", "id"));

    LocalDateTime now = LocalDateTime.now();
    category.setDeletedAt(now);
    category.setUpdatedAt(now);
    skillCategoryRepository.save(category);
  }
}
