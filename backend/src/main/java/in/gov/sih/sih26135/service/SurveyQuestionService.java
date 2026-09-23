package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateSurveyQuestionRequest;
import in.gov.sih.sih26135.dto.request.UpdateSurveyQuestionRequest;
import in.gov.sih.sih26135.dto.response.SurveyQuestionResponse;
import java.util.List;

public interface SurveyQuestionService {

  SurveyQuestionResponse createSurveyQuestion(CreateSurveyQuestionRequest request);

  SurveyQuestionResponse updateSurveyQuestion(Long id, UpdateSurveyQuestionRequest request);

  SurveyQuestionResponse getSurveyQuestionById(Long id);

  SurveyQuestionResponse getSurveyQuestionByVersionAndCode(Long versionId, String questionCode);

  SurveyQuestionResponse getSurveyQuestionByVersionAndDisplayOrder(Long versionId, Integer displayOrder);

  List<SurveyQuestionResponse> getQuestionsByVersion(Long versionId);

  List<SurveyQuestionResponse> getQuestionsByVersionOrdered(Long versionId);

  List<SurveyQuestionResponse> getQuestionsByType(Long questionTypeId);

  void deleteSurveyQuestion(Long id);
}
