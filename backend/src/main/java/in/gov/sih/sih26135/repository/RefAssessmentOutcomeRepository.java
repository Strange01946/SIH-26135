package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.RefAssessmentOutcome;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefAssessmentOutcomeRepository extends JpaRepository<RefAssessmentOutcome, Long> {

  Optional<RefAssessmentOutcome> findByOutcomeCode(String outcomeCode);

  boolean existsByOutcomeCode(String outcomeCode);

  List<RefAssessmentOutcome> findAllByOrderBySortOrderAsc();
}
