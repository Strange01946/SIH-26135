package in.gov.sih.sih26135.repository.analytics;

import in.gov.sih.sih26135.entity.analytics.UnemploymentReasonSummary;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UnemploymentReasonSummaryRepository extends
    JpaRepository<UnemploymentReasonSummary, Long> {

  Optional<UnemploymentReasonSummary> findByUnemploymentReasonCode(String unemploymentReasonCode);

  List<UnemploymentReasonSummary> findAllByOrderByPeriodCountDesc();
}
