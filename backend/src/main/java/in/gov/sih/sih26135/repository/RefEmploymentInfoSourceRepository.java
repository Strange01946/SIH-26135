package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.RefEmploymentInfoSource;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefEmploymentInfoSourceRepository extends JpaRepository<RefEmploymentInfoSource, Long> {

  Optional<RefEmploymentInfoSource> findBySourceCode(String sourceCode);

  boolean existsBySourceCode(String sourceCode);

  List<RefEmploymentInfoSource> findByIsSelfReportedFlag(Boolean isSelfReportedFlag);
}
