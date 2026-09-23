package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.request.CreateSurveyQuestionRequest;
import in.gov.sih.sih26135.dto.response.SurveyQuestionResponse;
import in.gov.sih.sih26135.entity.SurveyQuestion;
import org.springframework.stereotype.Component;

@Component
public class SurveyQuestionMapper {

  public SurveyQuestionResponse toResponse(SurveyQuestion entity) {
    if (entity == null) {
      return null;
    }

    return new SurveyQuestionResponse(
        entity.getId(),
        entity.getSurveyTemplateVersionId(),
        entity.getQuestionCode(),
        entity.getQuestionText(),
        entity.getQuestionTypeId(),
        entity.getDisplayOrder(),
        entity.getIsRequired(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }

  public SurveyQuestion toEntity(CreateSurveyQuestionRequest request) {
    if (request == null) {
      return null;
    }

    SurveyQuestion entity = new SurveyQuestion();
    entity.setSurveyTemplateVersionId(request.getSurveyTemplateVersionId());
    entity.setQuestionCode(request.getQuestionCode());
    entity.setQuestionText(request.getQuestionText());
    entity.setQuestionTypeId(request.getQuestionTypeId());
    entity.setDisplayOrder(request.getDisplayOrder());
    entity.setIsRequired(request.getIsRequired() != null ? request.getIsRequired() : true);
    return entity;
  }
}
