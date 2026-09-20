package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.EmploymentVerification;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmploymentVerificationRepository extends
    JpaRepository<EmploymentVerification, Long> {

  Optional<EmploymentVerification> findByVerificationNumber(String verificationNumber);

  boolean existsByVerificationNumber(String verificationNumber);

  Optional<EmploymentVerification> findByEmploymentVerificationRequestId(Long requestId);

  boolean existsByEmploymentVerificationRequestId(Long requestId);

  Optional<EmploymentVerification> findByEmploymentRecordIdAndCycleNumber(Long employmentId,
      Integer cycleNumber);

  Optional<EmploymentVerification> findByEmploymentRecordIdAndCurrentVerificationKey(
      Long employmentId, Integer currentVerificationKey);

  List<EmploymentVerification> findByEmploymentRecordId(Long employmentId);

  List<EmploymentVerification> findByTraineeId(Long traineeId);

  List<EmploymentVerification> findByPlacementRecordId(Long placementId);

  List<EmploymentVerification> findByEmployerId(Long employerId);

  List<EmploymentVerification> findByRecordVerificationStatusId(Long statusId);

  List<EmploymentVerification> findByEmploymentVerificationMethodId(Long methodId);

  List<EmploymentVerification> findByEmploymentVerificationRejectionReasonId(Long reasonId);

  List<EmploymentVerification> findByVerifiedByUserId(Long verifiedByUserId);

  List<EmploymentVerification> findByEmploymentRecordIdAndIsCurrentTrue(Long employmentId);
}
