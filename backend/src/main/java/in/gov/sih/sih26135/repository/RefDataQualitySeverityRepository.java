package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.RefDataQualitySeverity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefDataQualitySeverityRepository extends
    JpaRepository<RefDataQualitySeverity, Long> {

  Optional<RefDataQualitySeverity> findBySeverityCode(String severityCode);

  boolean existsBySeverityCode(String severityCode);
}
