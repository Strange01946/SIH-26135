package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.DataQualityRuleResponse;
import in.gov.sih.sih26135.entity.DataQualityRule;
import org.springframework.stereotype.Component;

@Component
public class DataQualityRuleMapper {

  public DataQualityRuleResponse toResponse(DataQualityRule entity) {
    if (entity == null) {
      return null;
    }

    Long categoryId = null;
    String categoryCode = null;
    String categoryName = null;
    if (entity.getDataQualityCategory() != null) {
      categoryId = entity.getDataQualityCategory().getId();
      categoryCode = entity.getDataQualityCategory().getCategoryCode();
      categoryName = entity.getDataQualityCategory().getCategoryName();
    }

    Long severityId = null;
    String severityCode = null;
    String severityName = null;
    if (entity.getDataQualitySeverity() != null) {
      severityId = entity.getDataQualitySeverity().getId();
      severityCode = entity.getDataQualitySeverity().getSeverityCode();
      severityName = entity.getDataQualitySeverity().getSeverityName();
    }

    return new DataQualityRuleResponse(
        entity.getId(),
        entity.getRuleCode(),
        entity.getRuleName(),
        entity.getDescription(),
        entity.getTargetEntityType(),
        categoryId,
        categoryCode,
        categoryName,
        severityId,
        severityCode,
        severityName,
        entity.getLifecycleStatusId(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
