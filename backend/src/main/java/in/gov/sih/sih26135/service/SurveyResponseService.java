package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateSurveyResponseRequest;
import in.gov.sih.sih26135.dto.request.UpdateSurveyResponseRequest;
import in.gov.sih.sih26135.dto.response.SurveyResponseResponse;
import java.util.List;

public interface SurveyResponseService {

  SurveyResponseResponse createSurveyResponse(CreateSurveyResponseRequest request);

  SurveyResponseResponse updateSurveyResponse(Long id, UpdateSurveyResponseRequest request);

  SurveyResponseResponse getSurveyResponseById(Long id);

  SurveyResponseResponse getSurveyResponseByAttempt(Long surveyId, Long traineeId, Integer attemptNumber);

  List<SurveyResponseResponse> getResponsesBySurvey(Long surveyId);

  List<SurveyResponseResponse> getResponsesByTrainee(Long traineeId);

  List<SurveyResponseResponse> getResponsesByFollowupTask(Long followupTaskId);

  List<SurveyResponseResponse> getResponsesByEnrollment(Long enrollmentId);

  List<SurveyResponseResponse> getResponsesByStatus(Long statusId);

  List<SurveyResponseResponse> getResponsesByTemplateVersion(Long versionId);

  void deleteSurveyResponse(Long id);
}
