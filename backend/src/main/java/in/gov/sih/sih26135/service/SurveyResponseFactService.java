package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.SurveyResponseFactResponse;
import in.gov.sih.sih26135.response.PagedResponse;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface SurveyResponseFactService {

  PagedResponse<SurveyResponseFactResponse> getAllSurveyResponses(Pageable pageable);

  List<SurveyResponseFactResponse> getAllSurveyResponses();

  SurveyResponseFactResponse getSurveyResponseById(Long surveyResponseId);

  List<SurveyResponseFactResponse> getSurveyResponsesByTraineeId(Long traineeId);

  PagedResponse<SurveyResponseFactResponse> getSurveyResponsesByTraineeId(Long traineeId, Pageable pageable);

  List<SurveyResponseFactResponse> getSurveyResponsesBySurveyId(Long surveyId);

  PagedResponse<SurveyResponseFactResponse> getSurveyResponsesBySurveyId(Long surveyId, Pageable pageable);

  List<SurveyResponseFactResponse> getSurveyResponsesBySurveyPurposeId(Long surveyPurposeId);

  PagedResponse<SurveyResponseFactResponse> getSurveyResponsesBySurveyPurposeId(Long surveyPurposeId, Pageable pageable);

  List<SurveyResponseFactResponse> getSurveyResponsesBySurveyPurposeCode(String surveyPurposeCode);

  PagedResponse<SurveyResponseFactResponse> getSurveyResponsesBySurveyPurposeCode(
      String surveyPurposeCode, Pageable pageable);

  List<SurveyResponseFactResponse> getSurveyResponsesByProgramId(Long programId);

  PagedResponse<SurveyResponseFactResponse> getSurveyResponsesByProgramId(Long programId, Pageable pageable);

  List<SurveyResponseFactResponse> getSurveyResponsesByCourseId(Long courseId);

  PagedResponse<SurveyResponseFactResponse> getSurveyResponsesByCourseId(Long courseId, Pageable pageable);

  List<SurveyResponseFactResponse> getSurveyResponsesByBatchId(Long batchId);

  PagedResponse<SurveyResponseFactResponse> getSurveyResponsesByBatchId(Long batchId, Pageable pageable);

  List<SurveyResponseFactResponse> getSurveyResponsesByEnrollmentId(Long enrollmentId);

  PagedResponse<SurveyResponseFactResponse> getSurveyResponsesByEnrollmentId(Long enrollmentId, Pageable pageable);

  List<SurveyResponseFactResponse> getSurveyResponsesByFollowupTaskId(Long followupTaskId);

  PagedResponse<SurveyResponseFactResponse> getSurveyResponsesByFollowupTaskId(Long followupTaskId, Pageable pageable);

  List<SurveyResponseFactResponse> getSurveyResponsesByIsSubmittedFlag(Boolean isSubmittedFlag);

  PagedResponse<SurveyResponseFactResponse> getSurveyResponsesByIsSubmittedFlag(
      Boolean isSubmittedFlag, Pageable pageable);
}
