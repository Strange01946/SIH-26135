package in.gov.sih.sih26135.repository.analytics;

import in.gov.sih.sih26135.entity.analytics.ProgramOutcomeSummary;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProgramOutcomeSummaryRepository extends
    JpaRepository<ProgramOutcomeSummary, Long> {

  List<ProgramOutcomeSummary> findBySchemeId(Long schemeId);

  List<ProgramOutcomeSummary> findAllByOrderByEnrollmentCountDesc();
}
