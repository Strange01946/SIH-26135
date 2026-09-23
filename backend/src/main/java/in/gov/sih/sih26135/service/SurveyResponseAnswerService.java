package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateSurveyResponseAnswerRequest;
import in.gov.sih.sih26135.dto.request.UpdateSurveyResponseAnswerRequest;
import in.gov.sih.sih26135.dto.response.SurveyResponseAnswerResponse;
import java.util.List;

public interface SurveyResponseAnswerService {

  SurveyResponseAnswerResponse createSurveyResponseAnswer(CreateSurveyResponseAnswerRequest request);

  SurveyResponseAnswerResponse updateSurveyResponseAnswer(Long id, UpdateSurveyResponseAnswerRequest request);

  SurveyResponseAnswerResponse getSurveyResponseAnswerById(Long id);

  SurveyResponseAnswerResponse getAnswerByResponseAndQuestion(Long surveyResponseId, Long surveyQuestionId);

  List<SurveyResponseAnswerResponse> getAnswersByResponse(Long surveyResponseId);

  List<SurveyResponseAnswerResponse> getAnswersByQuestion(Long surveyQuestionId);

  void deleteSurveyResponseAnswer(Long id);
}
