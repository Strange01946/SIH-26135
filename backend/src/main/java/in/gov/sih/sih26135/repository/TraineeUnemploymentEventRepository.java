package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.TraineeUnemploymentEvent;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TraineeUnemploymentEventRepository extends
    JpaRepository<TraineeUnemploymentEvent, Long> {

  Optional<TraineeUnemploymentEvent> findByTraineeIdAndPeriodNumber(Long traineeId,
      Integer periodNumber);

  boolean existsByTraineeIdAndPeriodNumber(Long traineeId, Integer periodNumber);

  Optional<TraineeUnemploymentEvent> findByTraineeIdAndStartDate(Long traineeId,
      LocalDate startDate);

  Optional<TraineeUnemploymentEvent> findByTraineeIdAndCurrentPeriodKey(Long traineeId,
      Integer currentPeriodKey);

  Optional<TraineeUnemploymentEvent> findByEmploymentExitEventId(Long exitEventId);

  boolean existsByEmploymentExitEventId(Long exitEventId);

  List<TraineeUnemploymentEvent> findByTraineeId(Long traineeId);

  List<TraineeUnemploymentEvent> findByTraineeIdAndIsCurrentTrue(Long traineeId);

  List<TraineeUnemploymentEvent> findByLabourStatusId(Long labourStatusId);

  List<TraineeUnemploymentEvent> findByUnemploymentReasonId(Long reasonId);

  List<TraineeUnemploymentEvent> findByPrecedingEmploymentId(Long employmentId);

  List<TraineeUnemploymentEvent> findBySucceedingEmploymentId(Long employmentId);

  List<TraineeUnemploymentEvent> findByEnrollmentId(Long enrollmentId);

  List<TraineeUnemploymentEvent> findByPlacementRecordId(Long placementId);

  List<TraineeUnemploymentEvent> findByFollowupTaskId(Long followupTaskId);

  List<TraineeUnemploymentEvent> findBySurveyResponseId(Long surveyResponseId);

  List<TraineeUnemploymentEvent> findByEmploymentInfoSourceId(Long sourceId);

  List<TraineeUnemploymentEvent> findByRecordVerificationStatusId(Long statusId);

  List<TraineeUnemploymentEvent> findByVerifiedByUserId(Long verifiedByUserId);

  List<TraineeUnemploymentEvent> findByStartDate(LocalDate startDate);
}
