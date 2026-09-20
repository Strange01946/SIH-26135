package in.gov.sih.sih26135.repository.analytics;

import in.gov.sih.sih26135.entity.analytics.FollowupOutcomeSummary;
import in.gov.sih.sih26135.entity.analytics.FollowupOutcomeSummaryId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FollowupOutcomeSummaryRepository extends
    JpaRepository<FollowupOutcomeSummary, FollowupOutcomeSummaryId> {

  List<FollowupOutcomeSummary> findByFollowupTypeCode(String followupTypeCode);

  List<FollowupOutcomeSummary> findByFollowupOffsetMonths(Integer followupOffsetMonths);

  List<FollowupOutcomeSummary> findAllByOrderByFollowupOffsetMonthsAsc();
}
