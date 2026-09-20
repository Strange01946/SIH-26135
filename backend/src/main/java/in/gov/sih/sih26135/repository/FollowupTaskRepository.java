package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.FollowupTask;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FollowupTaskRepository extends JpaRepository<FollowupTask, Long> {

  Optional<FollowupTask> findByFollowupCampaignIdAndTraineeId(Long campaignId, Long traineeId);

  boolean existsByFollowupCampaignIdAndTraineeId(Long campaignId, Long traineeId);

  List<FollowupTask> findByFollowupCampaignId(Long campaignId);

  List<FollowupTask> findByTraineeId(Long traineeId);

  List<FollowupTask> findByEnrollmentId(Long enrollmentId);

  List<FollowupTask> findByPlacementRecordId(Long placementId);

  List<FollowupTask> findByEmploymentRecordId(Long employmentId);

  List<FollowupTask> findBySurveyId(Long surveyId);

  List<FollowupTask> findByFollowupStatusId(Long statusId);

  List<FollowupTask> findByFollowupOutcomeId(Long outcomeId);

  List<FollowupTask> findByAssignedUserId(Long assignedUserId);

  List<FollowupTask> findByScheduledDate(LocalDate scheduledDate);
}
