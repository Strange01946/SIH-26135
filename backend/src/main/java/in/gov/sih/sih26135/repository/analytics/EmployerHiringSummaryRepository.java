package in.gov.sih.sih26135.repository.analytics;

import in.gov.sih.sih26135.entity.analytics.EmployerHiringSummary;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployerHiringSummaryRepository extends
    JpaRepository<EmployerHiringSummary, Long> {

  List<EmployerHiringSummary> findAllByOrderByJoinedCountDesc();

  List<EmployerHiringSummary> findAllByOrderByPlacementCountDesc();
}
