package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.request.CreateSkillRequest;
import in.gov.sih.sih26135.dto.response.SkillResponse;
import in.gov.sih.sih26135.entity.Skill;
import in.gov.sih.sih26135.entity.SkillCategory;
import org.springframework.stereotype.Component;

@Component
public class SkillMapper {

  public SkillResponse toResponse(Skill entity) {
    if (entity == null) {
      return null;
    }

    Long skillCategoryId = null;
    String categoryCode = null;
    String categoryName = null;
    if (entity.getSkillCategory() != null) {
      skillCategoryId = entity.getSkillCategory().getId();
      categoryCode = entity.getSkillCategory().getCategoryCode();
      categoryName = entity.getSkillCategory().getCategoryName();
    }

    return new SkillResponse(
        entity.getId(),
        entity.getSkillCode(),
        entity.getSkillName(),
        entity.getDescription(),
        skillCategoryId,
        categoryCode,
        categoryName,
        entity.getLifecycleStatusId(),
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        entity.getDeletedAt()
    );
  }

  public Skill toEntity(CreateSkillRequest request, SkillCategory skillCategory) {
    if (request == null) {
      return null;
    }

    Skill entity = new Skill();
    entity.setSkillCode(request.getSkillCode());
    entity.setSkillName(request.getSkillName());
    entity.setDescription(request.getDescription());
    entity.setSkillCategory(skillCategory);
    entity.setLifecycleStatusId(request.getLifecycleStatusId());
    return entity;
  }
}
