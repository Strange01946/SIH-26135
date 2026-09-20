package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.RefSkillGapAssessmentStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefSkillGapAssessmentStatusRepository extends
    JpaRepository<RefSkillGapAssessmentStatus, Long> {

  Optional<RefSkillGapAssessmentStatus> findByStatusCode(String statusCode);

  boolean existsByStatusCode(String statusCode);
}
