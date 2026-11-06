package in.gov.sih.sih26135.repository.analytics;

import in.gov.sih.sih26135.entity.analytics.EmploymentVerificationFact;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmploymentVerificationFactRepository extends
    JpaRepository<EmploymentVerificationFact, Long> {

  Optional<EmploymentVerificationFact> findByVerificationNumber(String verificationNumber);

  List<EmploymentVerificationFact> findByTraineeId(Long traineeId);

  Page<EmploymentVerificationFact> findByTraineeId(Long traineeId, Pageable pageable);

  List<EmploymentVerificationFact> findByEmploymentId(Long employmentId);

  Page<EmploymentVerificationFact> findByEmploymentId(Long employmentId, Pageable pageable);

  List<EmploymentVerificationFact> findByPlacementId(Long placementId);

  Page<EmploymentVerificationFact> findByPlacementId(Long placementId, Pageable pageable);

  List<EmploymentVerificationFact> findByEmployerId(Long employerId);

  Page<EmploymentVerificationFact> findByEmployerId(Long employerId, Pageable pageable);

  List<EmploymentVerificationFact> findByRecordVerificationStatusId(Long statusId);

  Page<EmploymentVerificationFact> findByRecordVerificationStatusId(Long statusId, Pageable pageable);

  List<EmploymentVerificationFact> findByEmploymentVerificationMethodId(Long methodId);

  Page<EmploymentVerificationFact> findByEmploymentVerificationMethodId(Long methodId, Pageable pageable);

  List<EmploymentVerificationFact> findByIsCurrentTrue();

  Page<EmploymentVerificationFact> findByIsCurrentTrue(Pageable pageable);

  List<EmploymentVerificationFact> findByIsVerifiedFlag(Boolean isVerifiedFlag);

  Page<EmploymentVerificationFact> findByIsVerifiedFlag(Boolean isVerifiedFlag, Pageable pageable);
}
