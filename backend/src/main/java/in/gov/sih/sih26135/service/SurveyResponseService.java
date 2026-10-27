package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateSurveyResponseRequest;
import in.gov.sih.sih26135.dto.request.UpdateSurveyResponseRequest;
import in.gov.sih.sih26135.dto.response.SurveyResponseResponse;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SurveyResponseService {

  SurveyResponseResponse createSurveyResponse(CreateSurveyResponseRequest request);

  SurveyResponseResponse updateSurveyResponse(Long id, UpdateSurveyResponseRequest request);

  SurveyResponseResponse getSurveyResponseById(Long id);

  SurveyResponseResponse getSurveyResponseByAttempt(Long surveyId, Long traineeId, Integer attemptNumber);

  List<SurveyResponseResponse> getResponsesBySurvey(Long surveyId);

  Page<SurveyResponseResponse> getResponsesBySurvey(Long surveyId, Pageable pageable);

  List<SurveyResponseResponse> getResponsesByTrainee(Long traineeId);

  Page<SurveyResponseResponse> getResponsesByTrainee(Long traineeId, Pageable pageable);

  List<SurveyResponseResponse> getResponsesByFollowupTask(Long followupTaskId);

  Page<SurveyResponseResponse> getResponsesByFollowupTask(Long followupTaskId, Pageable pageable);

  List<SurveyResponseResponse> getResponsesByEnrollment(Long enrollmentId);

  Page<SurveyResponseResponse> getResponsesByEnrollment(Long enrollmentId, Pageable pageable);

  List<SurveyResponseResponse> getResponsesByStatus(Long statusId);

  Page<SurveyResponseResponse> getResponsesByStatus(Long statusId, Pageable pageable);

  List<SurveyResponseResponse> getResponsesByTemplateVersion(Long versionId);

  Page<SurveyResponseResponse> getResponsesByTemplateVersion(Long versionId, Pageable pageable);

  void deleteSurveyResponse(Long id);
}
