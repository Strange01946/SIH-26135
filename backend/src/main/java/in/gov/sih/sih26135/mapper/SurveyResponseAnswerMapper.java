package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.request.CreateSurveyResponseAnswerRequest;
import in.gov.sih.sih26135.dto.response.SurveyResponseAnswerResponse;
import in.gov.sih.sih26135.entity.SurveyQuestion;
import in.gov.sih.sih26135.entity.SurveyResponse;
import in.gov.sih.sih26135.entity.SurveyResponseAnswer;
import org.springframework.stereotype.Component;

@Component
public class SurveyResponseAnswerMapper {

  public SurveyResponseAnswerResponse toResponse(SurveyResponseAnswer entity) {
    if (entity == null) {
      return null;
    }

    Long responseId = null;
    if (entity.getSurveyResponse() != null) {
      responseId = entity.getSurveyResponse().getId();
    }

    Long questionId = null;
    String questionCode = null;
    String questionText = null;
    Long questionTypeId = null;
    if (entity.getSurveyQuestion() != null) {
      questionId = entity.getSurveyQuestion().getId();
      questionCode = entity.getSurveyQuestion().getQuestionCode();
      questionText = entity.getSurveyQuestion().getQuestionText();
      questionTypeId = entity.getSurveyQuestion().getQuestionTypeId();
    }

    return new SurveyResponseAnswerResponse(
        entity.getId(),
        responseId,
        questionId,
        questionCode,
        questionText,
        questionTypeId,
        entity.getSelectedOptionId(),
        entity.getAnswerText(),
        entity.getNumericValue(),
        entity.getBooleanValue(),
        entity.getDateValue(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }

  public SurveyResponseAnswer toEntity(
      CreateSurveyResponseAnswerRequest request,
      SurveyResponse surveyResponse,
      SurveyQuestion surveyQuestion) {
    if (request == null) {
      return null;
    }

    SurveyResponseAnswer entity = new SurveyResponseAnswer();
    entity.setSurveyResponse(surveyResponse);
    entity.setSurveyQuestion(surveyQuestion);
    entity.setSelectedOptionId(request.getSelectedOptionId());
    entity.setAnswerText(request.getAnswerText());
    entity.setNumericValue(request.getNumericValue());
    entity.setBooleanValue(request.getBooleanValue());
    entity.setDateValue(request.getDateValue());
    return entity;
  }
}
