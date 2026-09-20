package in.gov.sih.sih26135.repository.analytics;

import in.gov.sih.sih26135.entity.analytics.TraineeOutcomeSummary;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TraineeOutcomeSummaryRepository extends
    JpaRepository<TraineeOutcomeSummary, Long> {

  List<TraineeOutcomeSummary> findByStateId(Long stateId);

  List<TraineeOutcomeSummary> findByDistrictId(Long districtId);

  List<TraineeOutcomeSummary> findByCurrentEmploymentStatusId(Long currentEmploymentStatusId);

  List<TraineeOutcomeSummary> findBySnapshotIsEmployedFlag(Boolean snapshotIsEmployedFlag);
}
