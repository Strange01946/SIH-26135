package in.gov.sih.sih26135.repository.analytics;

import in.gov.sih.sih26135.entity.analytics.FollowupFact;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FollowupFactRepository extends JpaRepository<FollowupFact, Long> {

  List<FollowupFact> findByTraineeId(Long traineeId);

  List<FollowupFact> findByFollowupCampaignId(Long followupCampaignId);

  List<FollowupFact> findByFollowupTypeId(Long followupTypeId);

  List<FollowupFact> findByFollowupTypeCode(String followupTypeCode);

  List<FollowupFact> findByFollowupStatusId(Long followupStatusId);

  List<FollowupFact> findByFollowupOutcomeId(Long followupOutcomeId);

  List<FollowupFact> findByIsOpenFlag(Boolean isOpenFlag);
}
