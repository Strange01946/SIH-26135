package in.gov.sih.sih26135.repository.analytics;

import in.gov.sih.sih26135.entity.analytics.SurveyResponseFact;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SurveyResponseFactRepository extends
    JpaRepository<SurveyResponseFact, Long> {

  List<SurveyResponseFact> findByTraineeId(Long traineeId);

  List<SurveyResponseFact> findBySurveyId(Long surveyId);

  List<SurveyResponseFact> findBySurveyPurposeId(Long surveyPurposeId);

  List<SurveyResponseFact> findBySurveyPurposeCode(String surveyPurposeCode);

  List<SurveyResponseFact> findByProgramId(Long programId);

  List<SurveyResponseFact> findByCourseId(Long courseId);

  List<SurveyResponseFact> findByBatchId(Long batchId);

  List<SurveyResponseFact> findByEnrollmentId(Long enrollmentId);

  List<SurveyResponseFact> findByFollowupTaskId(Long followupTaskId);

  List<SurveyResponseFact> findByIsSubmittedFlag(Boolean isSubmittedFlag);
}
