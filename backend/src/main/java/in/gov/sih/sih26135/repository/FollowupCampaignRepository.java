package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.FollowupCampaign;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FollowupCampaignRepository extends JpaRepository<FollowupCampaign, Long> {

  Optional<FollowupCampaign> findByCampaignCode(String campaignCode);

  boolean existsByCampaignCode(String campaignCode);

  List<FollowupCampaign> findByFollowupTypeId(Long followupTypeId);

  List<FollowupCampaign> findBySurveyId(Long surveyId);

  List<FollowupCampaign> findByProgramId(Long programId);

  List<FollowupCampaign> findByCourseId(Long courseId);

  List<FollowupCampaign> findByLifecycleStatusId(Long lifecycleStatusId);

  List<FollowupCampaign> findByScheduledStartDate(LocalDate scheduledStartDate);

  List<FollowupCampaign> findByDeletedAtIsNull();
}
