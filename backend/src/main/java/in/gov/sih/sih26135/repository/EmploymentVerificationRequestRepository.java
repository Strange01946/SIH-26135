package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.EmploymentVerificationRequest;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmploymentVerificationRequestRepository extends
    JpaRepository<EmploymentVerificationRequest, Long> {

  Optional<EmploymentVerificationRequest> findByRequestNumber(String requestNumber);

  boolean existsByRequestNumber(String requestNumber);

  Optional<EmploymentVerificationRequest> findByEmploymentRecordIdAndCycleNumber(Long employmentId,
      Integer cycleNumber);

  Optional<EmploymentVerificationRequest> findByEmploymentRecordIdAndOpenRequestKey(Long employmentId,
      Integer openRequestKey);

  List<EmploymentVerificationRequest> findByEmploymentRecordId(Long employmentId);

  List<EmploymentVerificationRequest> findByTraineeId(Long traineeId);

  List<EmploymentVerificationRequest> findByPlacementRecordId(Long placementId);

  List<EmploymentVerificationRequest> findByEmployerId(Long employerId);

  List<EmploymentVerificationRequest> findByEmploymentVerificationRequestStatusId(Long statusId);

  List<EmploymentVerificationRequest> findByAssignedVerifierUserId(Long assignedVerifierUserId);

  List<EmploymentVerificationRequest> findByFollowupTaskId(Long followupTaskId);

  List<EmploymentVerificationRequest> findBySurveyResponseId(Long surveyResponseId);
}
