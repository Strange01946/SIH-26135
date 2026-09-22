package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.request.CreateSkillCategoryRequest;
import in.gov.sih.sih26135.dto.response.SkillCategoryResponse;
import in.gov.sih.sih26135.entity.SkillCategory;
import org.springframework.stereotype.Component;

@Component
public class SkillCategoryMapper {

  public SkillCategoryResponse toResponse(SkillCategory entity) {
    if (entity == null) {
      return null;
    }

    Long parentCategoryId = null;
    String parentCategoryCode = null;
    String parentCategoryName = null;
    if (entity.getParentCategory() != null) {
      parentCategoryId = entity.getParentCategory().getId();
      parentCategoryCode = entity.getParentCategory().getCategoryCode();
      parentCategoryName = entity.getParentCategory().getCategoryName();
    }

    return new SkillCategoryResponse(
        entity.getId(),
        entity.getCategoryCode(),
        entity.getCategoryName(),
        entity.getDescription(),
        parentCategoryId,
        parentCategoryCode,
        parentCategoryName,
        entity.getLifecycleStatusId(),
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        entity.getDeletedAt()
    );
  }

  public SkillCategory toEntity(CreateSkillCategoryRequest request, SkillCategory parentCategory) {
    if (request == null) {
      return null;
    }

    SkillCategory entity = new SkillCategory();
    entity.setCategoryCode(request.getCategoryCode());
    entity.setCategoryName(request.getCategoryName());
    entity.setDescription(request.getDescription());
    entity.setParentCategory(parentCategory);
    entity.setLifecycleStatusId(request.getLifecycleStatusId());
    return entity;
  }
}
