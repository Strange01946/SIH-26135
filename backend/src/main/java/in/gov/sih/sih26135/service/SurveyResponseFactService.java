package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.SurveyResponseFactResponse;
import java.util.List;

public interface SurveyResponseFactService {

  List<SurveyResponseFactResponse> getAllSurveyResponses();

  SurveyResponseFactResponse getSurveyResponseById(Long surveyResponseId);

  List<SurveyResponseFactResponse> getSurveyResponsesByTraineeId(Long traineeId);

  List<SurveyResponseFactResponse> getSurveyResponsesBySurveyId(Long surveyId);

  List<SurveyResponseFactResponse> getSurveyResponsesBySurveyPurposeId(Long surveyPurposeId);

  List<SurveyResponseFactResponse> getSurveyResponsesBySurveyPurposeCode(String surveyPurposeCode);

  List<SurveyResponseFactResponse> getSurveyResponsesByProgramId(Long programId);

  List<SurveyResponseFactResponse> getSurveyResponsesByCourseId(Long courseId);

  List<SurveyResponseFactResponse> getSurveyResponsesByBatchId(Long batchId);

  List<SurveyResponseFactResponse> getSurveyResponsesByEnrollmentId(Long enrollmentId);

  List<SurveyResponseFactResponse> getSurveyResponsesByFollowupTaskId(Long followupTaskId);

  List<SurveyResponseFactResponse> getSurveyResponsesByIsSubmittedFlag(Boolean isSubmittedFlag);
}
