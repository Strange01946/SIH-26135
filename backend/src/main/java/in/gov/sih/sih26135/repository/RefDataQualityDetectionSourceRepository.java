package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.RefDataQualityDetectionSource;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefDataQualityDetectionSourceRepository extends
    JpaRepository<RefDataQualityDetectionSource, Long> {

  Optional<RefDataQualityDetectionSource> findBySourceCode(String sourceCode);

  boolean existsBySourceCode(String sourceCode);
}
