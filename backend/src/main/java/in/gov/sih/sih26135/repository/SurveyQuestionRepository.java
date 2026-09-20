package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.SurveyQuestion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SurveyQuestionRepository extends JpaRepository<SurveyQuestion, Long> {

  Optional<SurveyQuestion> findBySurveyTemplateVersionIdAndQuestionCode(Long versionId, String questionCode);

  Optional<SurveyQuestion> findBySurveyTemplateVersionIdAndDisplayOrder(Long versionId, Integer displayOrder);

  List<SurveyQuestion> findBySurveyTemplateVersionId(Long versionId);

  List<SurveyQuestion> findBySurveyTemplateVersionIdOrderByDisplayOrderAsc(Long versionId);

  List<SurveyQuestion> findByQuestionTypeId(Long questionTypeId);
}
