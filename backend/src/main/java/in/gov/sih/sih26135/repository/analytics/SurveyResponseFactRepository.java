package in.gov.sih.sih26135.repository.analytics;

import in.gov.sih.sih26135.entity.analytics.SurveyResponseFact;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SurveyResponseFactRepository extends
    JpaRepository<SurveyResponseFact, Long> {

  List<SurveyResponseFact> findByTraineeId(Long traineeId);

  Page<SurveyResponseFact> findByTraineeId(Long traineeId, Pageable pageable);

  List<SurveyResponseFact> findBySurveyId(Long surveyId);

  Page<SurveyResponseFact> findBySurveyId(Long surveyId, Pageable pageable);

  List<SurveyResponseFact> findBySurveyPurposeId(Long surveyPurposeId);

  Page<SurveyResponseFact> findBySurveyPurposeId(Long surveyPurposeId, Pageable pageable);

  List<SurveyResponseFact> findBySurveyPurposeCode(String surveyPurposeCode);

  Page<SurveyResponseFact> findBySurveyPurposeCode(String surveyPurposeCode, Pageable pageable);

  List<SurveyResponseFact> findByProgramId(Long programId);

  Page<SurveyResponseFact> findByProgramId(Long programId, Pageable pageable);

  List<SurveyResponseFact> findByCourseId(Long courseId);

  Page<SurveyResponseFact> findByCourseId(Long courseId, Pageable pageable);

  List<SurveyResponseFact> findByBatchId(Long batchId);

  Page<SurveyResponseFact> findByBatchId(Long batchId, Pageable pageable);

  List<SurveyResponseFact> findByEnrollmentId(Long enrollmentId);

  Page<SurveyResponseFact> findByEnrollmentId(Long enrollmentId, Pageable pageable);

  List<SurveyResponseFact> findByFollowupTaskId(Long followupTaskId);

  Page<SurveyResponseFact> findByFollowupTaskId(Long followupTaskId, Pageable pageable);

  List<SurveyResponseFact> findByIsSubmittedFlag(Boolean isSubmittedFlag);

  Page<SurveyResponseFact> findByIsSubmittedFlag(Boolean isSubmittedFlag, Pageable pageable);
}
