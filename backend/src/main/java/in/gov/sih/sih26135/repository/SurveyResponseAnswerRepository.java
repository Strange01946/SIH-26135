package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.SurveyResponseAnswer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SurveyResponseAnswerRepository extends JpaRepository<SurveyResponseAnswer, Long> {

  Optional<SurveyResponseAnswer> findBySurveyResponseIdAndSurveyQuestionId(Long surveyResponseId, Long surveyQuestionId);

  boolean existsBySurveyResponseIdAndSurveyQuestionId(Long surveyResponseId, Long surveyQuestionId);

  List<SurveyResponseAnswer> findBySurveyResponseId(Long surveyResponseId);

  List<SurveyResponseAnswer> findBySurveyQuestionId(Long surveyQuestionId);
}
