package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.RefSystemEventSeverity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefSystemEventSeverityRepository extends
    JpaRepository<RefSystemEventSeverity, Long> {

  Optional<RefSystemEventSeverity> findBySeverityCode(String severityCode);

  boolean existsBySeverityCode(String severityCode);
}
