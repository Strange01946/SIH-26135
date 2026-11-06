package in.gov.sih.sih26135.repository.analytics;

import in.gov.sih.sih26135.entity.analytics.FollowupFact;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FollowupFactRepository extends JpaRepository<FollowupFact, Long> {

  List<FollowupFact> findByTraineeId(Long traineeId);

  Page<FollowupFact> findByTraineeId(Long traineeId, Pageable pageable);

  List<FollowupFact> findByFollowupCampaignId(Long followupCampaignId);

  Page<FollowupFact> findByFollowupCampaignId(Long followupCampaignId, Pageable pageable);

  List<FollowupFact> findByFollowupTypeId(Long followupTypeId);

  Page<FollowupFact> findByFollowupTypeId(Long followupTypeId, Pageable pageable);

  List<FollowupFact> findByFollowupTypeCode(String followupTypeCode);

  Page<FollowupFact> findByFollowupTypeCode(String followupTypeCode, Pageable pageable);

  List<FollowupFact> findByFollowupStatusId(Long followupStatusId);

  Page<FollowupFact> findByFollowupStatusId(Long followupStatusId, Pageable pageable);

  List<FollowupFact> findByFollowupOutcomeId(Long followupOutcomeId);

  Page<FollowupFact> findByFollowupOutcomeId(Long followupOutcomeId, Pageable pageable);

  List<FollowupFact> findByIsOpenFlag(Boolean isOpenFlag);

  Page<FollowupFact> findByIsOpenFlag(Boolean isOpenFlag, Pageable pageable);
}
