package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.PlacementRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlacementRecordRepository extends JpaRepository<PlacementRecord, Long> {

  Optional<PlacementRecord> findByPlacementNumber(String placementNumber);

  boolean existsByPlacementNumber(String placementNumber);

  Optional<PlacementRecord> findByJobApplicationId(Long jobApplicationId);

  Optional<PlacementRecord> findByEnrollmentIdAndJobPostingId(Long enrollmentId, Long jobPostingId);

  List<PlacementRecord> findByTraineeId(Long traineeId);

  List<PlacementRecord> findByEnrollmentId(Long enrollmentId);

  List<PlacementRecord> findByCourseId(Long courseId);

  List<PlacementRecord> findByProgramId(Long programId);

  List<PlacementRecord> findByTrainingProviderId(Long providerId);

  List<PlacementRecord> findByEmployerId(Long employerId);

  List<PlacementRecord> findByEmployerBranchId(Long employerBranchId);

  List<PlacementRecord> findByJobPostingId(Long jobPostingId);

  List<PlacementRecord> findByJobRoleId(Long jobRoleId);

  List<PlacementRecord> findByPlacementStatusId(Long placementStatusId);

  List<PlacementRecord> findByJoiningStatusId(Long joiningStatusId);

  List<PlacementRecord> findByEmployerIdAndPlacementStatusId(Long employerId, Long placementStatusId);

  List<PlacementRecord> findByCourseIdAndPlacementStatusId(Long courseId, Long placementStatusId);

  List<PlacementRecord> findByRecordVerificationStatusId(Long recordVerificationStatusId);

  List<PlacementRecord> findByDeletedAtIsNull();
}
