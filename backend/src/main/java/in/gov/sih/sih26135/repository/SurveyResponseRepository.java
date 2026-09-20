package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.SurveyResponse;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SurveyResponseRepository extends JpaRepository<SurveyResponse, Long> {

  Optional<SurveyResponse> findBySurveyIdAndTraineeIdAndAttemptNumber(Long surveyId, Long traineeId, Integer attemptNumber);

  boolean existsBySurveyIdAndTraineeIdAndAttemptNumber(Long surveyId, Long traineeId, Integer attemptNumber);

  List<SurveyResponse> findBySurveyId(Long surveyId);

  List<SurveyResponse> findByTraineeId(Long traineeId);

  List<SurveyResponse> findByFollowupTaskId(Long followupTaskId);

  List<SurveyResponse> findByEnrollmentId(Long enrollmentId);

  List<SurveyResponse> findBySurveyResponseStatusId(Long statusId);

  List<SurveyResponse> findBySurveyTemplateVersionId(Long versionId);
}
