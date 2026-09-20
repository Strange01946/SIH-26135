package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.RefSkillGapSeverity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefSkillGapSeverityRepository extends JpaRepository<RefSkillGapSeverity, Long> {

  Optional<RefSkillGapSeverity> findBySeverityCode(String severityCode);

  boolean existsBySeverityCode(String severityCode);

  Optional<RefSkillGapSeverity> findBySeverityRank(Integer severityRank);

  boolean existsBySeverityRank(Integer severityRank);
}
