package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.SurveyResponse;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

  Page<SurveyResponse> findBySurveyId(Long surveyId, Pageable pageable);

  Page<SurveyResponse> findByTraineeId(Long traineeId, Pageable pageable);

  Page<SurveyResponse> findByFollowupTaskId(Long followupTaskId, Pageable pageable);

  Page<SurveyResponse> findByEnrollmentId(Long enrollmentId, Pageable pageable);

  Page<SurveyResponse> findBySurveyResponseStatusId(Long statusId, Pageable pageable);

  Page<SurveyResponse> findBySurveyTemplateVersionId(Long versionId, Pageable pageable);
}
